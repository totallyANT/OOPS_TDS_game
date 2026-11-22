package com.project.tds.engine;

import com.project.tds.model.GameException;
import com.project.tds.model.enemy.Boss;
import com.project.tds.model.enemy.Enemy;
import com.project.tds.model.enemy.FrostGolem;
import com.project.tds.model.enemy.Goblin;
import com.project.tds.model.enemy.Orc;
import com.project.tds.model.enemy.Slime;
import com.project.tds.model.tower.Archer;
import com.project.tds.model.tower.Cannon;
import com.project.tds.model.tower.Mage;
import com.project.tds.model.tower.Tower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Holds the whole game: grid, towers, enemies, gold and lives.
 * Methods are synchronized because the game loop thread and the JavaFX thread both use this object.
 */
public class GameState {
    public static final int SIZE = 7;
    private static final double SPAWN_GAP = 0.8; // seconds between spawns

    // Snake-shaped path, {row, col}. Enters top at column 1, leaves bottom at column 5.
    private static final int[][] PATH_CELLS = {
            {0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1},
            {5, 2}, {5, 3}, {4, 3}, {3, 3}, {2, 3}, {1, 3},
            {1, 4}, {1, 5}, {2, 5}, {3, 5}, {4, 5}, {5, 5}, {6, 5}
    };

    private final List<int[]> path = new ArrayList<>();
    private final boolean[][] pathTiles = new boolean[SIZE][SIZE];
    private final Tower[][] towers = new Tower[SIZE][SIZE];
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Enemy> spawnQueue = new ArrayList<>();

    public static final int MAX_WAVES = 10;           // wave 10 = horde, then the Boss
    private static final double HORDE_SPAWN_GAP = 0.45;
    private static final double HEALTH_GROWTH = 0.25; // +25% enemy health per wave

    private double spawnGap = SPAWN_GAP;
    private int gold = 130;
    private int lives = 20;
    private int wave = 0;
    private double spawnTimer = 0;

    public GameState() {
        for (int[] cell : PATH_CELLS) {
            path.add(cell);
            pathTiles[cell[0]][cell[1]] = true;
        }
    }

    public synchronized void placeTower(String type, int row, int col) throws GameException {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            throw new GameException("That's outside the grid");
        }
        if (pathTiles[row][col]) {
            throw new GameException("Can't build on the path");
        }
        if (towers[row][col] != null) {
            throw new GameException("There's already a tower here");
        }
        Tower tower;
        switch (type) {
            case "Archer": tower = new Archer(row, col); break;
            case "Cannon": tower = new Cannon(row, col); break;
            case "Mage":   tower = new Mage(row, col); break;
            default: throw new GameException("Unknown tower: " + type);
        }
        if (gold < tower.getCost()) {
            throw new GameException("Not enough gold (need " + tower.getCost() + ")");
        }
        gold -= tower.getCost();
        towers[row][col] = tower;
    }

    public synchronized void upgradeTower(int row, int col) throws GameException {
        Tower tower = towers[row][col];
        if (tower == null) {
            throw new GameException("No tower selected");
        }
        if (tower.getLevel() >= Tower.MAX_LEVEL) {
            throw new GameException(tower.getName() + " is already at max level");
        }
        if (gold < tower.getUpgradeCost()) {
            throw new GameException("Not enough gold (need " + tower.getUpgradeCost() + ")");
        }
        tower.upgrade();
        gold -= tower.getUpgradeCost();
    }

    public synchronized void startWave() throws GameException {
        if (isGameOver()) {
            throw new GameException("Game over!");
        }
        if (!enemies.isEmpty() || !spawnQueue.isEmpty()) {
            throw new GameException("Wave still in progress");
        }
        if (wave >= MAX_WAVES) {
            throw new GameException("All waves cleared!");
        }
        wave++;
        spawnGap = SPAWN_GAP;
        if (wave == MAX_WAVES) {
            // final wave: a fast horde of everything, then the Boss walks in last
            for (int i = 0; i < 14; i++) spawn(new Goblin(path));
            for (int i = 0; i < 10; i++) spawn(new Orc(path));
            for (int i = 0; i < 8; i++) spawn(new Slime(path));
            for (int i = 0; i < 2; i++) spawn(new FrostGolem(path));
            Collections.shuffle(spawnQueue);
            spawn(new Boss(path));
            spawnGap = HORDE_SPAWN_GAP;
            return;
        }
        for (int i = 0; i < 4 + 2 * wave; i++) spawn(new Goblin(path));
        for (int i = 0; i < wave; i++) spawn(new Orc(path));
        for (int i = 0; i < wave / 2; i++) spawn(new Slime(path));
        for (int i = 0; i < wave / 3; i++) spawn(new FrostGolem(path)); // from wave 3
    }

    /** Queue an enemy, with extra health the later the wave. */
    private void spawn(Enemy e) {
        e.scaleHealth(1 + HEALTH_GROWTH * (wave - 1));
        spawnQueue.add(e);
    }

    /** One tick of the game. dt = seconds since last tick. */
    public synchronized void update(double dt) {
        if (isGameOver()) {
            return;
        }

        spawnTimer -= dt;
        if (!spawnQueue.isEmpty() && spawnTimer <= 0) {
            enemies.add(spawnQueue.remove(0));
            spawnTimer = spawnGap;
        }

        for (Enemy e : enemies) {
            e.move(dt);
        }
        for (Tower[] row : towers) {
            for (Tower t : row) {
                if (t != null) t.update(dt, enemies);
            }
        }

        ArrayList<Enemy> newcomers = new ArrayList<>(); // e.g. small slimes from a split
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            if (!e.isAlive()) {
                gold += e.getReward();
                newcomers.addAll(e.onDeath());
                it.remove();
            } else if (e.reachedEnd()) {
                lives = Math.max(0, lives - e.getLivesCost());
                it.remove();
            }
        }
        enemies.addAll(newcomers); // added after the loop so we don't change the list while iterating
    }

    public synchronized boolean isGameOver() { return lives <= 0; }
    /** Won = final wave started and fully cleared while still alive. */
    public synchronized boolean isVictory() { return wave == MAX_WAVES && !isWaveRunning() && lives > 0; }
    public synchronized boolean isWaveRunning() { return !enemies.isEmpty() || !spawnQueue.isEmpty(); }
    public synchronized int getGold() { return gold; }
    public synchronized int getLives() { return lives; }
    public synchronized int getWave() { return wave; }
    public synchronized Tower getTower(int row, int col) { return towers[row][col]; }
    public boolean isPath(int row, int col) { return pathTiles[row][col]; }
    public List<int[]> getPath() { return path; }

    /** Copy so the UI can draw without holding the lock or tripping over removals. */
    public synchronized List<Enemy> getEnemies() { return new ArrayList<>(enemies); }
}

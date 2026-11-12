package td.service;

import td.exception.InsufficientGoldException;
import td.exception.InvalidPlacementException;
import td.model.ArcherTower;
import td.model.BossEnemy;
import td.model.CannonTower;
import td.model.Damageable;
import td.model.Enemy;
import td.model.GameRecord;
import td.model.GoblinEnemy;
import td.model.MageTower;
import td.model.Tower;
import td.model.Upgradable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Central game engine managing battlefield state, collection models,
 * game loop progression, and thread-safe interactions.
 */
public class GameEngine {
    private final List<Tower> towers = new ArrayList<>();
    private final List<Enemy> activeEnemies = new ArrayList<>();
    private final List<GameRecord> gameRecords = new ArrayList<>();
    private final List<String> notifications = new ArrayList<>();

    private String playerName = "Commander";
    private String difficulty = "Normal";
    private int gold = 150;
    private int lives = 5;
    private int currentWave = 0;
    private int score = 0;
    private boolean waveInProgress = false;

    private int towerCounter = 1;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public GameEngine() {
        addNotification("Game initialized. Ready for battle!");
    }

    /**
     * Validates and places a new tower on the grid.
     *
     * @param towerType type of tower (Archer, Cannon, Mage)
     * @param row       grid row (0-4, row 2 is path)
     * @param col       grid column (0-4)
     * @return the created Tower instance
     * @throws InsufficientGoldException if player cannot afford the tower
     * @throws InvalidPlacementException if location is invalid, occupied, or during active wave
     */
    public synchronized Tower placeTower(String towerType, int row, int col)
            throws InsufficientGoldException, InvalidPlacementException {
        if (waveInProgress) {
            throw new InvalidPlacementException("Cannot place towers while a wave is in progress!");
        }

        if (row == 2) {
            throw new InvalidPlacementException("Cannot build on Row 2! This is the fixed enemy path.");
        }

        if (row < 0 || row > 4 || col < 0 || col > 4) {
            throw new InvalidPlacementException(String.format("Coordinates (%d, %d) out of grid bounds (5x5).", row, col));
        }

        if (getTowerAt(row, col) != null) {
            throw new InvalidPlacementException(String.format("Tile (%d, %d) already contains a tower!", row, col));
        }

        int cost;
        String normalizedType = towerType.trim().toLowerCase();
        Tower newTower;
        String id = "T" + (towerCounter++);

        if (normalizedType.contains("archer")) {
            cost = 50;
            if (gold < cost) {
                throw new InsufficientGoldException(String.format("Need %dg to build Archer Tower, but only have %dg!", cost, gold));
            }
            newTower = new ArcherTower(id, row, col);
        } else if (normalizedType.contains("cannon")) {
            cost = 90;
            if (gold < cost) {
                throw new InsufficientGoldException(String.format("Need %dg to build Cannon Tower, but only have %dg!", cost, gold));
            }
            newTower = new CannonTower(id, row, col);
        } else if (normalizedType.contains("mage")) {
            cost = 75;
            if (gold < cost) {
                throw new InsufficientGoldException(String.format("Need %dg to build Mage Tower, but only have %dg!", cost, gold));
            }
            newTower = new MageTower(id, row, col);
        } else {
            throw new InvalidPlacementException("Unknown tower type: " + towerType);
        }

        gold -= cost;
        towers.add(newTower);
        addNotification(String.format("Constructed %s at (%d, %d) for %dg. Remaining gold: %dg.",
                newTower.getName(), row, col, cost, gold));
        return newTower;
    }

    /**
     * Upgrades an existing tower at the specified grid position using the Upgradable interface.
     *
     * @param row grid row
     * @param col grid column
     * @throws InsufficientGoldException if player cannot afford upgrade
     * @throws InvalidPlacementException if no tower exists or during active wave
     */
    public synchronized void upgradeTowerAt(int row, int col)
            throws InsufficientGoldException, InvalidPlacementException {
        if (waveInProgress) {
            throw new InvalidPlacementException("Cannot upgrade towers while a wave is in progress!");
        }

        Tower tower = getTowerAt(row, col);
        if (tower == null) {
            throw new InvalidPlacementException(String.format("No tower found at (%d, %d) to upgrade!", row, col));
        }

        // Interface reference polymorphism
        Upgradable upgradable = tower;
        int upgradeCost = upgradable.getUpgradeCost();

        if (gold < upgradeCost) {
            throw new InsufficientGoldException(String.format("Upgrade costs %dg, but you only have %dg!", upgradeCost, gold));
        }

        gold -= upgradeCost;
        upgradable.upgrade();

        addNotification(String.format("Upgraded %s to Level %d for %dg! Remaining gold: %dg.",
                tower.getName(), upgradable.getLevel(), upgradeCost, gold));
    }

    /**
     * Finds the tower located at given row and column.
     *
     * @param row grid row
     * @param col grid col
     * @return Tower at position, or null if none
     */
    public synchronized Tower getTowerAt(int row, int col) {
        for (Tower tower : towers) {
            if (tower.getRow() == row && tower.getCol() == col) {
                return tower;
            }
        }
        return null;
    }

    /**
     * Executes one step of the battle simulation:
     * 1. Towers attack eligible enemies in range (polymorphically).
     * 2. Defeated enemies yield bounty and are removed.
     * 3. Surviving enemies move forward along path.
     * 4. Enemies reaching base inflict damage on lives.
     *
     * @return combat log strings for this tick
     */
    public synchronized List<String> executeSimulationTick() {
        List<String> tickLogs = new ArrayList<>();

        // 1. Tower attack phase (polymorphic invocation via Tower and Enemy references)
        for (Tower tower : towers) {
            for (Enemy enemy : activeEnemies) {
                if (!enemy.isDead() && tower.isInRange(enemy)) {
                    String attackMsg = tower.attack(enemy);
                    tickLogs.add(attackMsg);
                    addNotification(attackMsg);
                    break; // Tower focuses on first enemy in range
                }
            }
        }

        // 2. Process defeated enemies
        Iterator<Enemy> enemyIterator = activeEnemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            // Checking Damageable state
            Damageable damageable = enemy;
            if (damageable.isDead()) {
                gold += enemy.getRewardGold();
                score += enemy.getRewardGold() * 2;
                String killMsg = String.format("Eliminated %s! +%dg awarded. (Score: %d)",
                        enemy.getType(), enemy.getRewardGold(), score);
                tickLogs.add(killMsg);
                addNotification(killMsg);
                enemyIterator.remove();
            }
        }

        // 3. Movement phase for remaining enemies
        enemyIterator = activeEnemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            enemy.move();
            if (enemy.hasReachedBase()) {
                lives = Math.max(0, lives - 1);
                String breachMsg = String.format("WARNING: %s breached the base perimeter! Lives remaining: %d.",
                        enemy.getType(), lives);
                tickLogs.add(breachMsg);
                addNotification(breachMsg);
                enemyIterator.remove();
            }
        }

        return tickLogs;
    }

    /**
     * Searches placed towers by ID, name, or coordinate string.
     *
     * @param keyword search keyword
     * @return list of matching towers
     */
    public synchronized List<Tower> searchTowers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>(towers);
        }
        String lower = keyword.trim().toLowerCase();
        List<Tower> results = new ArrayList<>();
        for (Tower tower : towers) {
            if (tower.getId().toLowerCase().contains(lower)
                    || tower.getName().toLowerCase().contains(lower)
                    || tower.getCoordinates().contains(lower)) {
                results.add(tower);
            }
        }
        return results;
    }

    /**
     * Spawns an enemy into the active enemies list.
     *
     * @param enemy enemy to spawn
     */
    public synchronized void spawnEnemy(Enemy enemy) {
        activeEnemies.add(enemy);
        addNotification(String.format("Wave %d: %s appeared on the road! (HP: %d)",
                currentWave, enemy.getType(), enemy.getHealth()));
    }

    /**
     * Records the outcome of a completed wave into match history.
     *
     * @param result "CLEARED" or "BASE BREACHED"
     */
    public synchronized void recordWaveResult(String result) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        GameRecord record = new GameRecord(currentWave, playerName, difficulty, score, lives, result, timestamp);
        gameRecords.add(record);
        addNotification(String.format("Wave %d completed with result: %s. Total Score: %d.", currentWave, result, score));
    }

    /**
     * Resets game state to starting values.
     */
    public synchronized void resetGame() {
        towers.clear();
        activeEnemies.clear();
        notifications.clear();
        gold = 150;
        lives = 5;
        currentWave = 0;
        score = 0;
        waveInProgress = false;
        towerCounter = 1;
        addNotification("Game reset. Starting fresh with 150g and 5 lives.");
    }

    public synchronized void addNotification(String msg) {
        String timeTagged = "[" + LocalDateTime.now().format(TIME_FORMATTER) + "] " + msg;
        notifications.add(timeTagged);
        // Keep list bounded for memory
        if (notifications.size() > 200) {
            notifications.remove(0);
        }
    }

    // Synchronized Getters & Setters

    public synchronized List<Tower> getTowers() {
        return Collections.unmodifiableList(new ArrayList<>(towers));
    }

    public synchronized List<Enemy> getActiveEnemies() {
        return Collections.unmodifiableList(new ArrayList<>(activeEnemies));
    }

    public synchronized List<GameRecord> getGameRecords() {
        return Collections.unmodifiableList(new ArrayList<>(gameRecords));
    }

    public synchronized List<String> getNotifications() {
        return Collections.unmodifiableList(new ArrayList<>(notifications));
    }

    public synchronized String getPlayerName() {
        return playerName;
    }

    public synchronized void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public synchronized String getDifficulty() {
        return difficulty;
    }

    public synchronized void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public synchronized int getGold() {
        return gold;
    }

    public synchronized void setGold(int gold) {
        this.gold = gold;
    }

    public synchronized int getLives() {
        return lives;
    }

    public synchronized void setLives(int lives) {
        this.lives = lives;
    }

    public synchronized int getCurrentWave() {
        return currentWave;
    }

    public synchronized void setCurrentWave(int currentWave) {
        this.currentWave = currentWave;
    }

    public synchronized int getScore() {
        return score;
    }

    public synchronized void setScore(int score) {
        this.score = score;
    }

    public synchronized boolean isWaveInProgress() {
        return waveInProgress;
    }

    public synchronized void setWaveInProgress(boolean waveInProgress) {
        this.waveInProgress = waveInProgress;
    }
}

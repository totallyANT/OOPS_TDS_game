package td.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import td.exception.InsufficientGoldException;
import td.exception.InvalidPlacementException;
import td.model.ArcherTower;
import td.model.CannonTower;
import td.model.Enemy;
import td.model.GameRecord;
import td.model.MageTower;
import td.model.Tower;
import td.model.Upgradable;

/**
 * Central model facade. Every mutation is synchronized because the JavaFX
 * thread and the two background worker threads read and update this state.
 */
public class GameEngine {

    private static final int BOARD_SIZE = 5;
    private static final int PATH_ROW = 2;
    private static final int MAX_NOTIFICATIONS = 150;

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

    /**
     * Builds a tower on a valid grass tile and deducts its cost.
     */
    public synchronized Tower placeTower(String towerType, int row, int col)
            throws InsufficientGoldException, InvalidPlacementException {
        if (waveInProgress) {
            throw new InvalidPlacementException("You cannot build while a wave is in progress.");
        }
        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
            throw new InvalidPlacementException("Cell (" + row + ", " + col + ") is outside the battlefield.");
        }
        if (row == PATH_ROW) {
            throw new InvalidPlacementException("Row 2 is the enemy path; towers cannot be built there.");
        }
        if (getTowerAt(row, col) != null) {
            throw new InvalidPlacementException(
                    "Cell (" + row + ", " + col + ") already contains a tower.");
        }

        Tower tower = createTower(towerType, row, col);
        if (gold < tower.getCost()) {
            throw new InsufficientGoldException(
                    "Not enough gold for " + tower.getName() + ": need " + tower.getCost()
                            + "g but only have " + gold + "g.");
        }

        gold -= tower.getCost();
        towers.add(tower);
        addNotification("Built " + tower.getName() + " L1 at (" + row + ", " + col
                + ") for " + tower.getCost() + "g. Gold left: " + gold + "g.");
        return tower;
    }

    /**
     * Upgrades the tower on the given cell through an {@link Upgradable} reference.
     */
    public synchronized void upgradeTowerAt(int row, int col)
            throws InsufficientGoldException, InvalidPlacementException {
        if (waveInProgress) {
            throw new InvalidPlacementException("You cannot upgrade while a wave is in progress.");
        }

        Tower tower = getTowerAt(row, col);
        if (tower == null) {
            throw new InvalidPlacementException(
                    "There is no tower at (" + row + ", " + col + ") to upgrade.");
        }

        Upgradable target = tower;
        int cost = target.getUpgradeCost();
        if (gold < cost) {
            throw new InsufficientGoldException(
                    "Upgrade of " + tower.getName() + " costs " + cost
                            + "g but only " + gold + "g is available.");
        }

        gold -= cost;
        target.upgrade();
        addNotification("Upgraded " + tower.getName() + " at (" + row + ", " + col
                + ") to L" + target.getLevel() + " for " + cost + "g.");
    }

    /**
     * Advances the battle by one tick: towers attack, dead enemies are
     * removed and rewarded, survivors move, and breaches cost a life.
     *
     * @return human-readable combat log lines for this tick
     */
    public synchronized List<String> executeSimulationTick() {
        List<String> tickLog = new ArrayList<>();

        for (Tower tower : towers) {
            Enemy target = null;
            for (Enemy enemy : activeEnemies) {
                if (!enemy.isDead() && tower.isInRange(enemy)) {
                    target = enemy;
                    break;
                }
            }
            if (target != null) {
                String message = tower.attack(target);
                tickLog.add(message);
                addNotification(message);
            }
        }

        Iterator<Enemy> iterator = activeEnemies.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            if (enemy.isDead()) {
                gold += enemy.getRewardGold();
                score += enemy.getRewardGold();
                String message = enemy.getType() + " destroyed! +" + enemy.getRewardGold()
                        + "g, +" + enemy.getRewardGold() + " score.";
                tickLog.add(message);
                addNotification(message);
                iterator.remove();
                continue;
            }

            enemy.move();
            if (enemy.hasReachedBase()) {
                lives = Math.max(0, lives - 1);
                String message = enemy.getType() + " reached the base! Lives left: " + lives + ".";
                tickLog.add(message);
                addNotification(message);
                iterator.remove();
            }
        }

        return tickLog;
    }

    /**
     * Filters placed towers by name, id, or "row,col" coordinates.
     * A blank keyword returns every tower.
     */
    public synchronized List<Tower> searchTowers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new ArrayList<>(towers);
        }
        String needle = keyword.trim().toLowerCase(Locale.ROOT);
        List<Tower> matches = new ArrayList<>();
        for (Tower tower : towers) {
            String coordinates = tower.getRow() + "," + tower.getCol();
            if (tower.getName().toLowerCase(Locale.ROOT).contains(needle)
                    || tower.getId().toLowerCase(Locale.ROOT).contains(needle)
                    || coordinates.contains(needle)) {
                matches.add(tower);
            }
        }
        return matches;
    }

    /** Starts a new wave and reserves the battlefield for the wave thread. */
    public synchronized void beginWave() throws InvalidPlacementException {
        if (waveInProgress) {
            throw new InvalidPlacementException("A wave is already running. Wait for it to finish.");
        }
        if (lives <= 0) {
            throw new InvalidPlacementException("The base has fallen. Reset the game to play again.");
        }
        currentWave++;
        waveInProgress = true;
        addNotification("Wave " + currentWave + " incoming on " + difficulty + " difficulty!");
    }

    /** Adds a freshly spawned enemy to the active battlefield. */
    public synchronized void spawnEnemy(Enemy enemy) {
        activeEnemies.add(enemy);
        addNotification("Spawned " + enemy.getType() + " at the start of the path.");
    }

    /** Stores a completed wave and releases the battlefield lock. */
    public synchronized void finishWave(GameRecord record) {
        if (record != null) {
            gameRecords.add(record);
            addNotification("Wave " + record.getWaveNumber() + " finished: "
                    + record.getResult() + ".");
        }
        waveInProgress = false;
    }

    /** Removes every active enemy, used when the base falls. */
    public synchronized void clearActiveEnemies() {
        activeEnemies.clear();
    }

    /** Clears the board and counters but keeps the commander name. */
    public synchronized void resetGame() {
        towers.clear();
        activeEnemies.clear();
        gameRecords.clear();
        notifications.clear();
        gold = 150;
        lives = 5;
        currentWave = 0;
        score = 0;
        waveInProgress = false;
        addNotification("Game reset. Gold: 150g, Lives: 5. Build your defenses!");
    }

    public synchronized void addNotification(String message) {
        notifications.add(message);
        while (notifications.size() > MAX_NOTIFICATIONS) {
            notifications.remove(0);
        }
    }

    private Tower createTower(String towerType, int row, int col) throws InvalidPlacementException {
        if (towerType == null) {
            throw new InvalidPlacementException("Choose a tower type before building.");
        }
        String normalized = towerType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "archer tower", "archer" -> new ArcherTower(row, col);
            case "mage tower", "mage" -> new MageTower(row, col);
            case "cannon tower", "cannon" -> new CannonTower(row, col);
            default -> throw new InvalidPlacementException("Unknown tower type: " + towerType);
        };
    }

    public synchronized Tower getTowerAt(int row, int col) {
        for (Tower tower : towers) {
            if (tower.getRow() == row && tower.getCol() == col) {
                return tower;
            }
        }
        return null;
    }

    public synchronized List<Tower> getTowers() {
        return new ArrayList<>(towers);
    }

    public synchronized List<Enemy> getActiveEnemies() {
        return new ArrayList<>(activeEnemies);
    }

    public synchronized List<GameRecord> getGameRecords() {
        return new ArrayList<>(gameRecords);
    }

    public synchronized List<String> getNotifications() {
        return new ArrayList<>(notifications);
    }

    public synchronized String getPlayerName() {
        return playerName;
    }

    public synchronized void setPlayerName(String playerName) throws InvalidPlacementException {
        if (playerName == null || playerName.isBlank()) {
            throw new InvalidPlacementException("Commander name cannot be empty.");
        }
        this.playerName = playerName.trim();
        addNotification("Commander " + this.playerName + " reported for duty.");
    }

    public synchronized String getDifficulty() {
        return difficulty;
    }

    public synchronized void setDifficulty(String difficulty) {
        if ("Easy".equals(difficulty) || "Normal".equals(difficulty) || "Hard".equals(difficulty)) {
            this.difficulty = difficulty;
        }
    }

    public synchronized int getGold() {
        return gold;
    }

    public synchronized int getLives() {
        return lives;
    }

    public synchronized int getCurrentWave() {
        return currentWave;
    }

    public synchronized int getScore() {
        return score;
    }

    public synchronized boolean isWaveInProgress() {
        return waveInProgress;
    }
}

package com.project.tds.engine;

import com.project.tds.exception.InsufficientGoldException;
import com.project.tds.model.Targetable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe container managing the central state of the game:
 * gold, player lives, wave progression, score, and active entities.
 *
 * Demonstrates:
 * - Encapsulation with defensive copying and unmodifiable collections
 * - Method-level synchronization for multithreaded safety
 * - Custom checked exception integration (InsufficientGoldException)
 */
public class GameState {
    public static final int INITIAL_GOLD = 150;
    public static final int INITIAL_LIVES = 5;
    public static final int GRID_SIZE = 7; // 7x7 grid per team design notes
    // Integration: was 5. Waves 1-5 come from WaveData, waves 6-10 from ExtraWaves.
    public static final int TOTAL_WAVES = 10;

    private int gold;
    private int lives;
    private int waveNumber;
    private int score;
    private GameStatus status;

    // CopyOnWriteArrayList allows safe concurrent reading by UI/Engine threads
    // while the spawner or combat resolution threads add/remove entities.
    private final List<Targetable> activeEnemies;
    private final List<Object> activeTowers;

    public GameState() {
        this(INITIAL_GOLD, INITIAL_LIVES);
    }

    public GameState(int initialGold, int initialLives) {
        this.gold = initialGold;
        this.lives = initialLives;
        this.waveNumber = 0;
        this.score = 0;
        this.status = GameStatus.NOT_STARTED;
        this.activeEnemies = new CopyOnWriteArrayList<>();
        this.activeTowers = new CopyOnWriteArrayList<>();
    }

    /**
     * Deducts gold from the player's balance.
     * Synchronized to prevent race conditions during rapid tower placement.
     *
     * @param amount amount to deduct
     * @throws InsufficientGoldException if amount exceeds current gold
     * @throws IllegalArgumentException if amount is negative
     */
    public synchronized void deductGold(int amount) throws InsufficientGoldException {
        if (amount < 0) {
            throw new IllegalArgumentException("Deduction amount cannot be negative: " + amount);
        }
        if (amount > this.gold) {
            throw new InsufficientGoldException(amount, this.gold);
        }
        this.gold -= amount;
    }

    public synchronized void addGold(int amount) {
        if (amount > 0) {
            this.gold += amount;
        }
    }

    public synchronized void loseLives(int amount) {
        if (amount > 0) {
            this.lives = Math.max(0, this.lives - amount);
            if (this.lives == 0) {
                this.status = GameStatus.GAME_OVER;
            }
        }
    }

    public synchronized void addScore(int points) {
        if (points > 0) {
            this.score += points;
        }
    }

    public synchronized void nextWave() {
        this.waveNumber++;
    }

    public synchronized void setStatus(GameStatus newStatus) {
        this.status = newStatus;
    }

    public synchronized GameStatus getStatus() {
        return this.status;
    }

    public synchronized boolean isGameOver() {
        return this.lives <= 0 || this.status == GameStatus.GAME_OVER;
    }

    public synchronized boolean isVictory() {
        return this.status == GameStatus.VICTORY;
    }

    // Entity Management (Thread-safe via CopyOnWriteArrayList)
    public void addEnemy(Targetable enemy) {
        if (enemy != null) {
            activeEnemies.add(enemy);
        }
    }

    public void removeEnemy(Targetable enemy) {
        activeEnemies.remove(enemy);
    }

    public void addTower(Object tower) {
        if (tower != null) {
            activeTowers.add(tower);
        }
    }

    public void removeTower(Object tower) {
        activeTowers.remove(tower);
    }

    public void clearEnemies() {
        activeEnemies.clear();
    }

    /**
     * Resets the game state back to default values for a new match.
     */
    public synchronized void reset() {
        this.gold = INITIAL_GOLD;
        this.lives = INITIAL_LIVES;
        this.waveNumber = 0;
        this.score = 0;
        this.status = GameStatus.NOT_STARTED;
        this.activeEnemies.clear();
        this.activeTowers.clear();
    }

    // Defensive read-only views for external callers (e.g. UI render loop)
    public List<Targetable> getActiveEnemies() {
        return Collections.unmodifiableList(activeEnemies);
    }

    public List<Object> getActiveTowers() {
        return Collections.unmodifiableList(activeTowers);
    }

    public synchronized int getGold() {
        return gold;
    }

    public synchronized int getLives() {
        return lives;
    }

    public synchronized int getWaveNumber() {
        return waveNumber;
    }

    public synchronized int getScore() {
        return score;
    }
}

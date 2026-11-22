package com.project.tds.engine;

import com.project.tds.model.Targetable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Background worker thread responsible for pacing enemy spawns during each wave.
 * Instead of dumping an entire wave at once, enemies are introduced onto the
 * grid at configurable time intervals (e.g., 800ms).
 *
 * Demonstrates:
 * - Thread creation, lifecycle, and daemon status
 * - Graceful interruption handling with Thread.sleep
 * - State synchronization during pause/resume
 */
public class WaveSpawner implements Runnable {
    public static final long DEFAULT_SPAWN_INTERVAL_MS = 800;

    private final GameState gameState;
    private final GameEventListener listener;
    private final List<Targetable> queue;
    private final long spawnIntervalMs;

    private Thread spawnerThread;
    private volatile boolean running;
    private volatile boolean paused;

    public WaveSpawner(GameState gameState, GameEventListener listener) {
        this(gameState, listener, DEFAULT_SPAWN_INTERVAL_MS);
    }

    public WaveSpawner(GameState gameState, GameEventListener listener, long spawnIntervalMs) {
        this.gameState = gameState;
        this.listener = listener;
        this.spawnIntervalMs = spawnIntervalMs;
        this.queue = Collections.synchronizedList(new ArrayList<>());
        this.running = false;
        this.paused = false;
    }

    /**
     * Starts spawning a new wave of enemies on a dedicated thread.
     *
     * @param waveNumber current wave sequence
     * @param enemies list of enemies scheduled for this wave
     */
    public synchronized void startWave(int waveNumber, List<Targetable> enemies) {
        stop(); // Cancel any existing spawner thread

        this.queue.clear();
        if (enemies != null) {
            this.queue.addAll(enemies);
        }

        this.running = true;
        this.paused = false;

        if (listener != null) {
            listener.onWaveStarted(waveNumber);
            listener.onGameLog(String.format("Wave %d incoming! %d enemies approaching.", waveNumber, queue.size()));
        }

        spawnerThread = new Thread(this, "WaveSpawner-Thread-" + waveNumber);
        spawnerThread.setDaemon(true); // Auto-terminate if application closes
        spawnerThread.start();
    }

    public synchronized void pause() {
        this.paused = true;
    }

    public synchronized void resume() {
        this.paused = false;
        this.notifyAll(); // Wake up thread if waiting
    }

    public synchronized void stop() {
        this.running = false;
        if (spawnerThread != null && spawnerThread.isAlive()) {
            spawnerThread.interrupt();
        }
    }

    public boolean isSpawning() {
        return running && !queue.isEmpty();
    }

    @Override
    public void run() {
        while (running && !queue.isEmpty()) {
            // Handle pause condition
            synchronized (this) {
                while (paused && running) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }

            if (!running) break;

            // Spawn next enemy into game state
            Targetable nextEnemy = queue.remove(0);
            gameState.addEnemy(nextEnemy);

            // Wait before spawning the next enemy
            try {
                Thread.sleep(spawnIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
                break;
            }
        }
        this.running = false;
    }
}

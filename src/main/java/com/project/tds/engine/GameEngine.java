package com.project.tds.engine;

import com.project.tds.model.Targetable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Main game engine controller that executes the simulation loop on a dedicated thread.
 *
 * Demonstrates:
 * - Multithreading: decoupled loop thread keeping JavaFX UI responsive
 * - Controlled lifecycle state machine (start, pause, resume, restart, stop)
 * - Safe thread interruption and daemon thread management
 * - Observer pattern for UI event notification
 */
public class GameEngine implements Runnable {
    public static final int TARGET_TPS = 60; // 60 simulation ticks per second
    public static final long TICK_DURATION_MS = 1000 / TARGET_TPS; // ~16.6ms

    private final GameState gameState;
    private final WaveSpawner waveSpawner;
    private final List<GameEventListener> listeners;

    private Thread engineThread;
    private volatile boolean running;
    private volatile boolean paused;

    public GameEngine(GameState gameState) {
        this.gameState = gameState;
        this.listeners = new CopyOnWriteArrayList<>();
        this.running = false;
        this.paused = false;

        // Internal listener to forward spawner events to registered engine listeners
        this.waveSpawner = new WaveSpawner(gameState, new GameEventListener() {
            @Override
            public void onGameStateUpdated(GameState state) {}
            @Override
            public void onWaveStarted(int wave) { notifyWaveStarted(wave); }
            @Override
            public void onWaveCompleted(int wave) { notifyWaveCompleted(wave); }
            @Override
            public void onGameOver() { notifyGameOver(); }
            @Override
            public void onVictory() { notifyVictory(); }
            @Override
            public void onGameLog(String message) { notifyGameLog(message); }
        });
    }

    public void addListener(GameEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(GameEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Starts the main game loop thread.
     */
    public synchronized void start() {
        if (running) return;

        this.running = true;
        this.paused = false;
        gameState.setStatus(GameStatus.RUNNING);

        engineThread = new Thread(this, "GameEngine-Loop-Thread");
        engineThread.setDaemon(true); // Automatically terminates if JavaFX window closes
        engineThread.start();

        notifyGameLog("Game engine started.");
    }

    /**
     * Pauses the game loop and the wave spawner.
     */
    public synchronized void pause() {
        if (!running || paused) return;

        this.paused = true;
        gameState.setStatus(GameStatus.PAUSED);
        waveSpawner.pause();

        notifyGameLog("Game paused.");
    }

    /**
     * Resumes the game loop and the wave spawner.
     */
    public synchronized void resume() {
        if (!running || !paused) return;

        this.paused = false;
        gameState.setStatus(GameStatus.RUNNING);
        waveSpawner.resume();

        notifyGameLog("Game resumed.");
    }

    /**
     * Stops the engine and cleanly shuts down background threads.
     */
    public synchronized void stop() {
        this.running = false;
        waveSpawner.stop();

        if (engineThread != null && engineThread.isAlive()) {
            engineThread.interrupt(); // Interrupt sleep to exit cleanly
        }

        notifyGameLog("Game engine stopped.");
    }

    /**
     * Resets the game state and restarts the engine for a fresh match.
     */
    public synchronized void restart() {
        stop();
        gameState.reset();
        start();
        notifyGameLog("Game restarted.");
    }

    /**
     * Initiates a new wave through the WaveSpawner.
     *
     * @param enemies list of enemies for this wave
     */
    public void launchWave(List<Targetable> enemies) {
        if (gameState.getStatus() != GameStatus.RUNNING) {
            start();
        }
        gameState.nextWave();
        waveSpawner.startWave(gameState.getWaveNumber(), enemies);
    }

    public WaveSpawner getWaveSpawner() {
        return waveSpawner;
    }

    public GameState getGameState() {
        return gameState;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    @Override
    public void run() {
        while (running) {
            long frameStart = System.currentTimeMillis();

            if (!paused && gameState.getStatus() == GameStatus.RUNNING) {
                tick();
            }

            long elapsed = System.currentTimeMillis() - frameStart;
            long sleepTime = TICK_DURATION_MS - elapsed;

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    break; // Cleanly exit loop
                }
            }
        }
    }

    /**
     * Executes one atomic simulation tick.
     */
    private void tick() {
        // 1. Clean up any enemies that have died
        for (Targetable enemy : gameState.getActiveEnemies()) {
            if (!enemy.isAlive()) {
                gameState.removeEnemy(enemy);
            }
        }

        // 2. Check loss condition (lives depleted)
        if (gameState.getLives() <= 0) {
            gameState.setStatus(GameStatus.GAME_OVER);
            stop();
            notifyGameOver();
            return;
        }

        // 3. Check victory condition (all waves cleared and no remaining enemies)
        if (gameState.getWaveNumber() >= GameState.TOTAL_WAVES
                && !waveSpawner.isSpawning()
                && gameState.getActiveEnemies().isEmpty()) {
            gameState.setStatus(GameStatus.VICTORY);
            stop();
            notifyVictory();
            return;
        }

        // 4. Notify UI listeners of state update
        notifyGameStateUpdated(gameState);
    }

    // Listener Dispatchers
    private void notifyGameStateUpdated(GameState state) {
        for (GameEventListener l : listeners) l.onGameStateUpdated(state);
    }

    private void notifyWaveStarted(int wave) {
        for (GameEventListener l : listeners) l.onWaveStarted(wave);
    }

    private void notifyWaveCompleted(int wave) {
        for (GameEventListener l : listeners) l.onWaveCompleted(wave);
    }

    private void notifyGameOver() {
        for (GameEventListener l : listeners) l.onGameOver();
    }

    private void notifyVictory() {
        for (GameEventListener l : listeners) l.onVictory();
    }

    private void notifyGameLog(String msg) {
        for (GameEventListener l : listeners) l.onGameLog(msg);
    }
}

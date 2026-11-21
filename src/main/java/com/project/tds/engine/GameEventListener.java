package com.project.tds.engine;

/**
 * Observer interface allowing the UI layer (JavaFX) or audio system
 * to receive decoupled events from the background GameEngine.
 */
public interface GameEventListener {
    /**
     * Fired on each simulation tick (e.g. 60 FPS).
     * @param state the current snapshot of the game state
     */
    void onGameStateUpdated(GameState state);

    /**
     * Fired when a new wave starts spawning.
     * @param waveNumber current wave index
     */
    void onWaveStarted(int waveNumber);

    /**
     * Fired when all enemies in the active wave have been defeated.
     * @param waveNumber completed wave index
     */
    void onWaveCompleted(int waveNumber);

    /** Fired when player lives drop to zero. */
    void onGameOver();

    /** Fired when all waves are cleared. */
    void onVictory();

    /**
     * Fired for combat or general notification events.
     * @param message descriptive log text
     */
    void onGameLog(String message);
}

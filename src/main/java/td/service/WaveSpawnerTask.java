package td.service;

import javafx.application.Platform;
import td.model.BossEnemy;
import td.model.Enemy;
import td.model.GoblinEnemy;

import java.util.ArrayList;
import java.util.List;

/**
 * Background worker thread (Thread 1) managing the real-time wave lifecycle,
 * spawning sequential enemy waves, ticking the game simulation, and updating
 * the JavaFX user interface asynchronously without freezing the GUI.
 */
public class WaveSpawnerTask implements Runnable {
    private final GameEngine gameEngine;
    private final Runnable uiRefreshCallback;
    private final Runnable waveCompleteCallback;

    public WaveSpawnerTask(GameEngine gameEngine, Runnable uiRefreshCallback, Runnable waveCompleteCallback) {
        this.gameEngine = gameEngine;
        this.uiRefreshCallback = uiRefreshCallback;
        this.waveCompleteCallback = waveCompleteCallback;
    }

    @Override
    public void run() {
        gameEngine.setWaveInProgress(true);
        int waveNum = gameEngine.getCurrentWave() + 1;
        gameEngine.setCurrentWave(waveNum);
        gameEngine.addNotification(String.format("=== WAVE %d COMMENCED ===", waveNum));

        // Queue of 3 to 5 enemies for this wave
        List<Enemy> spawnQueue = new ArrayList<>();
        int goblinCount = Math.min(2 + waveNum, 4);
        for (int i = 1; i <= goblinCount; i++) {
            spawnQueue.add(new GoblinEnemy("G" + waveNum + "-" + i));
        }
        // At least one BossEnemy per wave
        spawnQueue.add(new BossEnemy("Boss-" + waveNum));

        int initialLives = gameEngine.getLives();

        try {
            // Simulation loop: continues until spawnQueue empty and all activeEnemies dead or exited
            while (!spawnQueue.isEmpty() || !gameEngine.getActiveEnemies().isEmpty()) {
                // Spawn one enemy if queue has remaining
                if (!spawnQueue.isEmpty()) {
                    Enemy nextEnemy = spawnQueue.remove(0);
                    gameEngine.spawnEnemy(nextEnemy);
                }

                // Advance battle simulation tick
                gameEngine.executeSimulationTick();

                // Dispatch UI updates to JavaFX Application Thread
                dispatchToFx(uiRefreshCallback);

                // Check for game over (0 lives)
                if (gameEngine.getLives() <= 0) {
                    gameEngine.addNotification("DEFENSE FAILED: Commander lives reached 0!");
                    break;
                }

                // Tick delay for smooth grid animation and thread cooperation
                Thread.sleep(700);
            }
        } catch (InterruptedException e) {
            gameEngine.addNotification("Wave interrupted prematurely.");
            Thread.currentThread().interrupt();
        } finally {
            String result = (gameEngine.getLives() == initialLives) ? "CLEARED" : "BASE BREACHED";
            if (gameEngine.getLives() <= 0) {
                result = "DEFEATED";
            }
            gameEngine.recordWaveResult(result);
            gameEngine.setWaveInProgress(false);

            // Final UI refresh and completion notification
            dispatchToFx(uiRefreshCallback);
            if (waveCompleteCallback != null) {
                dispatchToFx(waveCompleteCallback);
            }
        }
    }

    /**
     * Safely executes a runnable on the JavaFX Application Thread or runs directly
     * if the JavaFX platform toolkit is not yet initialized (e.g. in test suites).
     *
     * @param action runnable to execute
     */
    private void dispatchToFx(Runnable action) {
        if (action == null) return;
        try {
            Platform.runLater(action);
        } catch (IllegalStateException | NoClassDefFoundError ex) {
            // Fallback for headless tests where JavaFX toolkit is not running
            action.run();
        }
    }
}

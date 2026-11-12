package td.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import td.model.BossEnemy;
import td.model.Enemy;
import td.model.GameRecord;
import td.model.GoblinEnemy;

/**
 * Thread 1: runs one complete wave lifecycle off the JavaFX application thread.
 * All GUI updates are dispatched back with {@link Platform#runLater(Runnable)}.
 */
public class WaveSpawnerTask implements Runnable {

    private static final long STEP_DELAY_MS = 700;
    private static final int MAX_TICKS = 60;
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GameEngine gameEngine;
    private final Runnable onUiRefresh;

    public WaveSpawnerTask(GameEngine gameEngine, Runnable onUiRefresh) {
        this.gameEngine = gameEngine;
        this.onUiRefresh = onUiRefresh;
    }

    @Override
    public void run() {
        List<Enemy> waveQueue = buildWave();
        boolean baseBreached = false;
        int spawned = 0;

        try {
            for (int tick = 0; tick < MAX_TICKS; tick++) {
                int livesBeforeTick = gameEngine.getLives();

                if (spawned < waveQueue.size()) {
                    gameEngine.spawnEnemy(waveQueue.get(spawned));
                    spawned++;
                }

                gameEngine.executeSimulationTick();
                if (gameEngine.getLives() < livesBeforeTick) {
                    baseBreached = true;
                }

                Platform.runLater(onUiRefresh);

                if (gameEngine.getLives() <= 0) {
                    break;
                }
                if (spawned >= waveQueue.size() && gameEngine.getActiveEnemies().isEmpty()) {
                    break;
                }

                Thread.sleep(STEP_DELAY_MS);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            gameEngine.addNotification("Wave thread was interrupted.");
        } finally {
            if (gameEngine.getLives() <= 0) {
                gameEngine.clearActiveEnemies();
                gameEngine.addNotification("The base has fallen. Reset the game to try again.");
            }

            String result = baseBreached || gameEngine.getLives() <= 0
                    ? "BASE BREACHED" : "CLEARED";
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            GameRecord record = new GameRecord(
                    gameEngine.getCurrentWave(),
                    gameEngine.getPlayerName(),
                    gameEngine.getDifficulty(),
                    gameEngine.getScore(),
                    gameEngine.getLives(),
                    result,
                    timestamp);
            gameEngine.finishWave(record);

            Platform.runLater(onUiRefresh);
        }
    }

    /**
     * Easy sends 3 enemies, Normal 4, Hard 5. Every wave includes a boss.
     */
    private List<Enemy> buildWave() {
        int enemyCount = switch (gameEngine.getDifficulty()) {
            case "Easy" -> 3;
            case "Hard" -> 5;
            default -> 4;
        };

        List<Enemy> wave = new ArrayList<>();
        for (int i = 0; i < enemyCount - 1; i++) {
            wave.add(new GoblinEnemy());
        }
        wave.add(new BossEnemy());
        return wave;
    }
}

package td.service;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.application.Platform;
import td.model.GameRecord;
import td.model.Tower;

/**
 * Thread 2: aggregates a system and combat report without blocking JavaFX.
 * Results and status changes are pushed back through consumers on the FX thread.
 */
public class ReportGeneratorTask implements Runnable {

    private static final long COMPILE_DELAY_MS = 600;

    private final GameEngine gameEngine;
    private final Consumer<String> reportConsumer;
    private final Consumer<String> statusConsumer;

    public ReportGeneratorTask(GameEngine gameEngine,
                               Consumer<String> reportConsumer,
                               Consumer<String> statusConsumer) {
        this.gameEngine = gameEngine;
        this.reportConsumer = reportConsumer;
        this.statusConsumer = statusConsumer;
    }

    @Override
    public void run() {
        Platform.runLater(() -> statusConsumer.accept("Generating report in background..."));

        try {
            Thread.sleep(COMPILE_DELAY_MS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            Platform.runLater(() -> statusConsumer.accept("Report generation was interrupted."));
            return;
        }

        String report;
        synchronized (gameEngine) {
            List<Tower> towers = gameEngine.getTowers();
            List<GameRecord> records = gameEngine.getGameRecords();

            int totalUpgradeLevels = 0;
            StringBuilder towerLines = new StringBuilder();
            for (Tower tower : towers) {
                totalUpgradeLevels += tower.getLevel();
                towerLines.append(String.format(Locale.US,
                        "  - %s %s L%d | DMG %d | RNG %d | cell (%d,%d)%n",
                        tower.getId(), tower.getName(), tower.getLevel(), tower.getDamage(),
                        tower.getRange(), tower.getRow(), tower.getCol()));
            }
            if (towers.isEmpty()) {
                towerLines.append("  (no towers built yet)\n");
            }

            int totalRecordedScore = 0;
            StringBuilder waveLines = new StringBuilder();
            for (GameRecord record : records) {
                totalRecordedScore += record.getScore();
                waveLines.append(String.format(Locale.US,
                        "  - Wave %d | %s | %s | Score %d | Lives %d | %s%n",
                        record.getWaveNumber(), record.getPlayerName(), record.getDifficulty(),
                        record.getScore(), record.getRemainingLives(), record.getResult()));
            }
            if (records.isEmpty()) {
                waveLines.append("  (no completed waves yet)\n");
            }
            double averageScore = records.isEmpty() ? 0.0
                    : (double) totalRecordedScore / records.size();

            report = String.format(Locale.US, """
                    ==============================================
                      TOWER DEFENSE - SYSTEM & COMBAT REPORT
                    ==============================================
                    Generated at      : %s
                    Commander         : %s
                    Difficulty        : %s
                    Current wave      : %d
                    Wave in progress  : %s
                    Gold / Lives      : %d g / %d
                    Active enemies    : %d
                    Cumulative score  : %d

                    --- DEFENSE ROSTER ---------------------------
                    Towers built      : %d
                    Sum of levels     : %d
                    %s
                    --- WAVE HISTORY -----------------------------
                    Waves recorded    : %d
                    Average score     : %.2f
                    %s
                    --- THREAD NOTES -----------------------------
                    Compiled on a daemon background thread while the
                    JavaFX application thread stayed responsive.
                    ==============================================
                    """,
                    java.time.LocalDateTime.now(),
                    gameEngine.getPlayerName(),
                    gameEngine.getDifficulty(),
                    gameEngine.getCurrentWave(),
                    gameEngine.isWaveInProgress() ? "yes" : "no",
                    gameEngine.getGold(),
                    gameEngine.getLives(),
                    gameEngine.getActiveEnemies().size(),
                    gameEngine.getScore(),
                    towers.size(),
                    totalUpgradeLevels,
                    towerLines,
                    records.size(),
                    averageScore,
                    waveLines);
        }

        String finalReport = report;
        Platform.runLater(() -> {
            reportConsumer.accept(finalReport);
            statusConsumer.accept("Report ready - generated on a background thread.");
        });
    }
}

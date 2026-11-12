package td.service;

import javafx.application.Platform;
import td.model.GameRecord;
import td.model.Tower;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Background analytics worker (Thread 2) that aggregates game statistics,
 * combat metrics, and match records into a formatted report asynchronously
 * without blocking or freezing the JavaFX application GUI.
 */
public class ReportGeneratorTask implements Runnable {
    private final GameEngine gameEngine;
    private final Consumer<String> statusConsumer;
    private final Consumer<String> contentConsumer;

    public ReportGeneratorTask(GameEngine gameEngine, Consumer<String> statusConsumer, Consumer<String> contentConsumer) {
        this.gameEngine = gameEngine;
        this.statusConsumer = statusConsumer;
        this.contentConsumer = contentConsumer;
    }

    @Override
    public void run() {
        dispatchStatus("Generating report in background...");

        try {
            // Simulate complex analytics and compilation time
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            dispatchStatus("Report generation aborted.");
            return;
        }

        // Thread-safe data extraction
        String report;
        synchronized (gameEngine) {
            List<Tower> towers = gameEngine.getTowers();
            List<GameRecord> records = gameEngine.getGameRecords();

            int archerCount = 0;
            int cannonCount = 0;
            int mageCount = 0;
            int totalUpgrades = 0;
            int totalInvestedGold = 0;

            for (Tower t : towers) {
                String name = t.getName().toLowerCase();
                if (name.contains("archer")) archerCount++;
                else if (name.contains("cannon")) cannonCount++;
                else if (name.contains("mage")) mageCount++;

                totalUpgrades += (t.getLevel() - 1);
                totalInvestedGold += t.getCost();
            }

            int totalScore = gameEngine.getScore();
            int wavesCompleted = records.size();
            long clearedWaves = records.stream()
                    .filter(r -> "CLEARED".equalsIgnoreCase(r.getResult()))
                    .count();

            StringBuilder sb = new StringBuilder();
            sb.append("============================================================\n");
            sb.append("          TOWER DEFENSE COMBAT & SYSTEM ANALYTICS           \n");
            sb.append("============================================================\n");
            sb.append(String.format("Generated At:       %s\n", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            sb.append(String.format("Commander Name:     %s\n", gameEngine.getPlayerName()));
            sb.append(String.format("Difficulty Setting: %s\n", gameEngine.getDifficulty()));
            sb.append(String.format("Current Gold:       %dg\n", gameEngine.getGold()));
            sb.append(String.format("Remaining Lives:    %d\n", gameEngine.getLives()));
            sb.append(String.format("Total Score:        %d\n", totalScore));
            sb.append(String.format("Current Wave:       Wave %d\n", gameEngine.getCurrentWave()));
            sb.append("------------------------------------------------------------\n");
            sb.append("DEFENSE FORCES BREAKDOWN:\n");
            sb.append(String.format(" - Total Active Towers:     %d\n", towers.size()));
            sb.append(String.format("   * Archer Towers (50g):   %d\n", archerCount));
            sb.append(String.format("   * Cannon Towers (90g):   %d\n", cannonCount));
            sb.append(String.format("   * Mage Towers (75g):     %d\n", mageCount));
            sb.append(String.format(" - Total Upgrades Applied:  %d levels\n", totalUpgrades));
            sb.append(String.format(" - Total Capital Invested:  %dg\n", totalInvestedGold));
            sb.append("------------------------------------------------------------\n");
            sb.append("WAVE HISTORICAL PERFORMANCE:\n");
            sb.append(String.format(" - Waves Documented:        %d\n", wavesCompleted));
            sb.append(String.format(" - Waves Flawlessly Cleared:%d\n", clearedWaves));
            if (wavesCompleted > 0) {
                sb.append(String.format(" - Clear Efficiency:        %.1f%%\n", (clearedWaves * 100.0) / wavesCompleted));
                sb.append("\nRecent Match Log:\n");
                for (GameRecord r : records) {
                    sb.append(String.format("   * Wave %d | Status: %-13s | Score: %-5d | Lives: %d | Time: %s\n",
                            r.getWaveNumber(), r.getResult(), r.getScore(), r.getRemainingLives(), r.getTimestamp()));
                }
            } else {
                sb.append(" - (No completed waves recorded yet)\n");
            }
            sb.append("============================================================\n");
            sb.append("Status: THREAD_EXECUTION_SUCCESS [ReportGeneratorTask]\n");

            report = sb.toString();
        }

        // Dispatch completed report to UI
        String finalReport = report;
        dispatch(() -> {
            if (contentConsumer != null) {
                contentConsumer.accept(finalReport);
            }
            if (statusConsumer != null) {
                statusConsumer.accept("Report generated successfully!");
            }
        });
    }

    private void dispatchStatus(String message) {
        dispatch(() -> {
            if (statusConsumer != null) {
                statusConsumer.accept(message);
            }
        });
    }

    private void dispatch(Runnable action) {
        if (action == null) return;
        try {
            Platform.runLater(action);
        } catch (IllegalStateException | NoClassDefFoundError ex) {
            action.run();
        }
    }
}

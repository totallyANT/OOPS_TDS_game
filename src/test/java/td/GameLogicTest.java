package td;

import td.exception.InsufficientGoldException;
import td.exception.InvalidPlacementException;
import td.model.ArcherTower;
import td.model.BossEnemy;
import td.model.CannonTower;
import td.model.Enemy;
import td.model.GoblinEnemy;
import td.model.MageTower;
import td.model.Tower;
import td.model.Upgradable;
import td.service.GameEngine;
import td.service.ReportGeneratorTask;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Comprehensive automated test suite validating all core game logic,
 * exception handling, OOP polymorphism, and multithreading behavior without external dependencies.
 */
public class GameLogicTest {

    private static int passedTests = 0;
    private static int totalTests = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("     RUNNING TOWER DEFENSE VERIFICATION SUITE     ");
        System.out.println("==================================================");

        testCase1_TowerPlacementAndGoldDeduction();
        testCase2_TowerUpgradeViaUpgradableInterface();
        testCase3_NormalWaveCombatAndGoblinBounty();
        testCase4_BossArmorDamageMitigation();
        testCase5_MageTowerSlowMechanic();
        testCase6_ArmorySearchFiltering();
        testCase7_RejectPlacementOnEnemyPath();
        testCase8_RejectPlacementOnOccupiedTile();
        testCase9_RejectInsufficientGoldForBuildAndUpgrade();
        testCase10_BackgroundReportGenerationThread();
        testCase11_WaveSpawnerTaskExecution();

        System.out.println("==================================================");
        System.out.printf("Test Suite Finished: %d/%d tests PASSED.%n", passedTests, totalTests);
        System.out.println("==================================================");

        if (passedTests != totalTests) {
            System.err.println("FAILURE: Some tests did not pass!");
            System.exit(1);
        } else {
            System.out.println("SUCCESS: All system specifications verified!");
        }
    }

    private static void assertTrue(String testName, boolean condition, String errorDetail) {
        totalTests++;
        if (condition) {
            passedTests++;
            System.out.printf(" [PASS] TC%02d: %s%n", totalTests, testName);
        } else {
            System.err.printf(" [FAIL] TC%02d: %s -> %s%n", totalTests, testName, errorDetail);
        }
    }

    // TC01: Building Towers & Gold Calculation
    private static void testCase1_TowerPlacementAndGoldDeduction() {
        GameEngine engine = new GameEngine();
        try {
            int initialGold = engine.getGold(); // 150
            Tower tower = engine.placeTower("Archer Tower", 0, 0);

            boolean valid = tower instanceof ArcherTower
                    && engine.getGold() == (initialGold - 50)
                    && engine.getTowers().size() == 1
                    && engine.getTowerAt(0, 0) == tower;

            assertTrue("Normal Tower Placement & Gold Deduction", valid, "Expected ArcherTower placed and 50g deducted");
        } catch (Exception e) {
            assertTrue("Normal Tower Placement & Gold Deduction", false, "Exception thrown: " + e.getMessage());
        }
    }

    // TC02: Upgrading Tower via Upgradable Interface
    private static void testCase2_TowerUpgradeViaUpgradableInterface() {
        GameEngine engine = new GameEngine();
        try {
            Tower tower = engine.placeTower("Archer Tower", 1, 1); // 150 - 50 = 100g left
            int upgradeCost = tower.getUpgradeCost(); // 30 * 1 = 30g
            int initialDamage = tower.getDamage(); // 20

            engine.upgradeTowerAt(1, 1); // 100 - 30 = 70g left

            Upgradable up = tower;
            boolean valid = up.getLevel() == 2
                    && tower.getDamage() == (initialDamage + 10)
                    && engine.getGold() == 70
                    && up.getUpgradeCost() == 60; // 30 * 2 = 60

            assertTrue("Tower Upgrade via Upgradable Interface", valid, "Tower stats or gold incorrect after upgrade");
        } catch (Exception e) {
            assertTrue("Tower Upgrade via Upgradable Interface", false, "Exception: " + e.getMessage());
        }
    }

    // TC03: Simulation Tick, Combat & Goblin Bounty
    private static void testCase3_NormalWaveCombatAndGoblinBounty() {
        GameEngine engine = new GameEngine();
        try {
            // Place Archer Tower at (1, 0) - adjacent to (2, 0) -> distance = |1-2| + |0-0| = 1 <= range 2
            engine.placeTower("Archer Tower", 1, 0); // 100g left
            GoblinEnemy goblin = new GoblinEnemy("G1");
            engine.spawnEnemy(goblin);

            // Goblin HP = 50. Archer DMG = 20.
            engine.executeSimulationTick(); // DMG = 20 -> HP = 30, goblin moves to col 1
            boolean hit1 = goblin.getHealth() == 30;

            engine.executeSimulationTick(); // col 1, dist = |1-2| + |0-1| = 2 <= range 2 -> DMG = 20 -> HP = 10, goblin moves to col 2
            boolean hit2 = goblin.getHealth() == 10;

            assertTrue("Wave Combat & Damage Application", hit1 && hit2,
                    "Goblin health expected 30 then 10, but got: " + goblin.getHealth());
        } catch (Exception e) {
            assertTrue("Wave Combat & Damage Application", false, "Exception: " + e.getMessage());
        }
    }

    // TC04: Boss Armor Damage Mitigation
    private static void testCase4_BossArmorDamageMitigation() {
        BossEnemy boss = new BossEnemy("Boss1"); // armor = 8, HP = 140
        // Attack with 20 damage: effective damage = 20 - 8 = 12 -> health = 140 - 12 = 128
        boss.takeDamage(20);
        boolean hit1 = boss.getHealth() == 128;

        // Attack with small damage 10: effective damage = Math.max(5, 10 - 8) = 5 -> health = 128 - 5 = 123
        boss.takeDamage(10);
        boolean hit2 = boss.getHealth() == 123;

        assertTrue("Boss Armor Damage Mitigation (Min 5 Dmg)", hit1 && hit2,
                "Expected HP 128 then 123, but got: " + boss.getHealth());
    }

    // TC05: Mage Tower Slow Mechanic
    private static void testCase5_MageTowerSlowMechanic() {
        GameEngine engine = new GameEngine();
        try {
            Tower mage = engine.placeTower("Mage Tower", 1, 0); // 75g
            GoblinEnemy goblin = new GoblinEnemy("G_Slow"); // col = 0
            engine.spawnEnemy(goblin);

            // Mage attacks goblin, slows it
            List<String> logs = engine.executeSimulationTick();
            // Since it was slowed, move() should clear slow and NOT advance col
            boolean wasSlowedAndStoodStill = goblin.getCol() == 0 && !goblin.isSlowed();

            assertTrue("Mage Tower Slow Mechanic Prevents Move For 1 Tick", wasSlowedAndStoodStill,
                    "Expected goblin col to stay 0 after slow, actual col: " + goblin.getCol());
        } catch (Exception e) {
            assertTrue("Mage Tower Slow Mechanic Prevents Move For 1 Tick", false, "Exception: " + e.getMessage());
        }
    }

    // TC06: Armory Search Filtering
    private static void testCase6_ArmorySearchFiltering() {
        GameEngine engine = new GameEngine();
        try {
            engine.placeTower("Archer Tower", 0, 0);
            engine.placeTower("Cannon Tower", 0, 1);

            List<Tower> resultsArcher = engine.searchTowers("Archer");
            List<Tower> resultsCannon = engine.searchTowers("Cannon");
            List<Tower> resultsCoords = engine.searchTowers("(0, 1)");

            boolean valid = resultsArcher.size() == 1
                    && resultsCannon.size() == 1
                    && resultsCoords.size() == 1
                    && resultsCoords.get(0).getName().equals("Cannon Tower");

            assertTrue("Armory Search Filtering", valid, "Search did not return expected towers");
        } catch (Exception e) {
            assertTrue("Armory Search Filtering", false, "Exception: " + e.getMessage());
        }
    }

    // TC07: Reject Placement on Enemy Path (Row 2)
    private static void testCase7_RejectPlacementOnEnemyPath() {
        GameEngine engine = new GameEngine();
        boolean threw = false;
        try {
            engine.placeTower("Archer Tower", 2, 2);
        } catch (InvalidPlacementException ex) {
            threw = true;
        } catch (Exception ex) {
            // unexpected
        }
        assertTrue("Reject Placement on Enemy Path (Row 2)", threw, "Did not throw InvalidPlacementException on row 2");
    }

    // TC08: Reject Placement on Occupied Tile
    private static void testCase8_RejectPlacementOnOccupiedTile() {
        GameEngine engine = new GameEngine();
        boolean threw = false;
        try {
            engine.placeTower("Archer Tower", 0, 0);
            engine.placeTower("Mage Tower", 0, 0); // duplicate
        } catch (InvalidPlacementException ex) {
            threw = true;
        } catch (Exception ex) {
            // unexpected
        }
        assertTrue("Reject Placement on Occupied Tile", threw, "Did not throw InvalidPlacementException on occupied tile");
    }

    // TC09: Reject Insufficient Gold for Build and Upgrade
    private static void testCase9_RejectInsufficientGoldForBuildAndUpgrade() {
        GameEngine engine = new GameEngine();
        boolean threwBuild = false;
        boolean threwUpgrade = false;
        try {
            engine.setGold(40); // Archer costs 50
            engine.placeTower("Archer Tower", 0, 0);
        } catch (InsufficientGoldException ex) {
            threwBuild = true;
        } catch (Exception ex) {
            // unexpected
        }

        try {
            engine.setGold(50);
            Tower t = engine.placeTower("Archer Tower", 0, 0); // 0 gold left
            engine.upgradeTowerAt(0, 0); // upgrade costs 30g, but have 0g
        } catch (InsufficientGoldException ex) {
            threwUpgrade = true;
        } catch (Exception ex) {
            // unexpected
        }

        assertTrue("Reject Insufficient Gold for Build and Upgrade", threwBuild && threwUpgrade,
                "Failed to enforce InsufficientGoldException on build or upgrade");
    }

    // TC10: Multithreaded Background Report Generation
    private static void testCase10_BackgroundReportGenerationThread() {
        GameEngine engine = new GameEngine();
        try {
            engine.placeTower("Archer Tower", 0, 0);
            engine.placeTower("Cannon Tower", 0, 1);
            engine.recordWaveResult("CLEARED");

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> reportResult = new AtomicReference<>();
            AtomicReference<String> statusResult = new AtomicReference<>();

            ReportGeneratorTask task = new ReportGeneratorTask(
                    engine,
                    statusResult::set,
                    report -> {
                        reportResult.set(report);
                        latch.countDown();
                    }
            );

            Thread worker = new Thread(task);
            worker.setDaemon(true);
            worker.start();

            boolean finished = latch.await(2000, TimeUnit.MILLISECONDS);
            boolean valid = finished
                    && reportResult.get() != null
                    && reportResult.get().contains("TOWER DEFENSE COMBAT & SYSTEM ANALYTICS")
                    && reportResult.get().contains("Archer Towers (50g):   1")
                    && reportResult.get().contains("Cannon Towers (90g):   1")
                    && reportResult.get().contains("Waves Flawlessly Cleared:1");

            assertTrue("Multithreaded Background Report Generation Task", valid, "Report generator thread did not produce expected output");
        } catch (Exception ex) {
            assertTrue("Multithreaded Background Report Generation Task", false, "Exception: " + ex.getMessage());
        }
    }

    // TC11: Multithreaded Background Wave Spawner Task
    private static void testCase11_WaveSpawnerTaskExecution() {
        GameEngine engine = new GameEngine();
        try {
            // Provide ample gold to build testing defense line
            engine.setGold(400);
            engine.placeTower("Archer Tower", 1, 0);
            engine.placeTower("Cannon Tower", 1, 1);
            engine.placeTower("Mage Tower", 3, 0);

            CountDownLatch completeLatch = new CountDownLatch(1);
            td.service.WaveSpawnerTask waveTask = new td.service.WaveSpawnerTask(
                    engine,
                    () -> { /* tick refresh */ },
                    completeLatch::countDown
            );

            Thread waveWorker = new Thread(waveTask);
            waveWorker.setDaemon(true);
            waveWorker.start();

            boolean finished = completeLatch.await(10, TimeUnit.SECONDS);

            boolean valid = finished
                    && !engine.isWaveInProgress()
                    && engine.getCurrentWave() == 1
                    && engine.getGameRecords().size() == 1;

            assertTrue("Multithreaded Background Wave Spawner Task", valid,
                    "Wave spawner did not finish cleanly or did not record outcome");
        } catch (Exception ex) {
            assertTrue("Multithreaded Background Wave Spawner Task", false, "Exception: " + ex.getMessage());
        }
    }
}

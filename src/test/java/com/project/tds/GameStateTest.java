package com.project.tds;

import com.project.tds.engine.GameState;
import com.project.tds.model.GameException;
import com.project.tds.model.enemy.Enemy;
import com.project.tds.model.enemy.FrostGolem;
import com.project.tds.model.enemy.Goblin;
import com.project.tds.model.enemy.Slime;
import com.project.tds.model.tower.Archer;
import com.project.tds.model.tower.Cannon;
import com.project.tds.model.tower.Mage;
import com.project.tds.model.tower.Tower;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    @Test
    void buildingRules() throws GameException {
        GameState state = new GameState();
        int startGold = state.getGold();
        assertThrows(GameException.class, () -> state.placeTower("Archer", 0, 1)); // path tile
        state.placeTower("Archer", 0, 0);
        assertEquals(startGold - Archer.COST, state.getGold());
        assertThrows(GameException.class, () -> state.placeTower("Mage", 0, 0)); // occupied
    }

    @Test
    void upgradeNeedsGold() throws GameException {
        GameState state = new GameState();
        state.placeTower("Cannon", 0, 0);
        while (state.getGold() >= state.getTower(0, 0).getUpgradeCost()) {
            state.upgradeTower(0, 0);
        }
        int level = state.getTower(0, 0).getLevel();
        assertThrows(GameException.class, () -> state.upgradeTower(0, 0)); // can't afford
        assertEquals(level, state.getTower(0, 0).getLevel());
    }

    @Test
    void upgradeStopsAtMaxLevel() throws GameException {
        Tower archer = new Archer(0, 0);
        for (int i = 0; i < Tower.MAX_LEVEL; i++) {
            archer.upgrade();
        }
        assertThrows(GameException.class, archer::upgrade);
    }

    @Test
    void cannonSplashHitsNearbyEnemiesForHalfDamage() {
        List<int[]> path = new GameState().getPath();
        List<Enemy> enemies = new ArrayList<>();
        enemies.add(new Goblin(path)); // both goblins stand on the first path tile
        enemies.add(new Goblin(path));
        Tower cannon = new Cannon(1, 2);
        cannon.update(1.0, enemies);   // one shot
        assertEquals(0, enemies.get(0).getHealth());                       // target: full damage
        assertEquals(enemies.get(1).getMaxHealth() - 15, enemies.get(1).getHealth()); // neighbour: half of 30
    }

    @Test
    void mageSpreadsItsSlowToEnemiesThatArentSlowedYet() {
        List<int[]> path = new GameState().getPath();
        Enemy front = new Goblin(path);
        Enemy behind = new Goblin(path);
        front.move(0.3);          // front goblin is a bit further along the path
        front.applySlow(10);      // and already slowed
        List<Enemy> enemies = new ArrayList<>(List.of(front, behind));
        new Mage(1, 2).update(1.0, enemies);
        assertTrue(behind.isSlowed(), "Mage should pick the goblin that wasn't slowed");
        assertEquals(behind.getMaxHealth() - 15, behind.getHealth());
        assertEquals(front.getMaxHealth(), front.getHealth());
    }

    @Test
    void frostGolemIgnoresSlowAndSpeedsUpWhenHurt() {
        FrostGolem golem = new FrostGolem(new GameState().getPath());
        golem.applySlow(10);
        assertFalse(golem.isSlowed(), "slows should do nothing to a Frost Golem");
        double startSpeed = golem.getSpeed();
        golem.takeDamage(golem.getMaxHealth() / 2);   // half health -> +25% speed
        assertEquals(startSpeed * 1.25, golem.getSpeed(), 0.001);
    }

    @Test
    void resetStartsAFreshGame() throws GameException {
        GameState state = new GameState();
        state.placeTower("Cannon", 1, 2);
        state.startWave();
        for (int i = 0; i < 100; i++) {
            state.update(0.02);
        }
        state.reset();
        assertEquals(GameState.START_GOLD, state.getGold());
        assertEquals(GameState.START_LIVES, state.getLives());
        assertEquals(0, state.getWave());
        assertNull(state.getTower(1, 2));
        assertFalse(state.isWaveRunning());
    }

    @Test
    void enemiesSpawnAndTowersKillThem() throws GameException {
        GameState state = new GameState();
        state.placeTower("Cannon", 1, 2);
        int goldAfterBuilding = state.getGold();
        state.startWave();
        assertThrows(GameException.class, state::startWave); // wave already running
        for (int i = 0; i < 2000 && state.isWaveRunning(); i++) {
            state.update(0.02);
        }
        assertFalse(state.isWaveRunning());
        assertTrue(state.getGold() > goldAfterBuilding, "should have earned gold from kills");
    }

    @Test
    void allWavesThenBossThenVictory() throws GameException {
        GameState state = new GameState();
        boolean sawBoss = false;
        for (int w = 1; w <= GameState.MAX_WAVES; w++) {
            buildAndUpgrade(state);
            state.startWave();
            for (int i = 0; i < 5000 && state.isWaveRunning(); i++) {
                state.update(0.02);
                for (Enemy e : state.getEnemies()) {
                    if (e.getName().equals("Boss")) {
                        assertEquals(GameState.MAX_WAVES, w, "Boss should only appear in the last wave");
                        sawBoss = true;
                    }
                }
            }
        }
        assertTrue(sawBoss);
        assertTrue(state.isVictory());
        assertThrows(GameException.class, state::startWave); // no wave after the last one
    }

    @Test
    void slimeSplitsIntoTwoSmallSlimesThatDontSplitAgain() {
        Slime slime = new Slime(new GameState().getPath());
        assertEquals(2, slime.onDeath().size());
        for (Enemy piece : slime.onDeath()) {
            assertEquals("Small Slime", piece.getName());
            assertTrue(piece.onDeath().isEmpty());
        }
    }

    // Test helper: a sensible player. Alternates Cannons and Mages next to the path, then upgrades them.
    private static final int[][] SPOTS = {{2, 2}, {4, 2}, {3, 4}, {2, 4}, {4, 4}, {0, 2}, {6, 4}, {3, 2}, {3, 0}, {2, 6}};
    private static final String[] TYPES = {"Cannon", "Mage"};

    private void buildAndUpgrade(GameState state) {
        for (int i = 0; i < SPOTS.length; i++) {
            try {
                state.placeTower(TYPES[i % TYPES.length], SPOTS[i][0], SPOTS[i][1]);
            } catch (GameException ignored) {
                // occupied or not enough gold: skip
            }
        }
        for (int[] spot : SPOTS) {
            for (int u = 0; u < Tower.MAX_LEVEL; u++) {
                try {
                    state.upgradeTower(spot[0], spot[1]);
                } catch (GameException ignored) {
                    // no tower, max level or not enough gold: skip
                }
            }
        }
    }
}

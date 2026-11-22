package com.project.tds;

import com.project.tds.engine.GameState;
import com.project.tds.model.GameException;
import com.project.tds.model.enemy.Enemy;
import com.project.tds.model.enemy.Slime;
import com.project.tds.model.tower.Tower;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    @Test
    void buildingRules() throws GameException {
        GameState state = new GameState();
        assertThrows(GameException.class, () -> state.placeTower("Archer", 0, 1)); // path tile
        state.placeTower("Archer", 0, 0);
        assertEquals(100, state.getGold());
        assertThrows(GameException.class, () -> state.placeTower("Mage", 0, 0)); // occupied
    }

    @Test
    void upgradeStopsAtMaxLevel() throws GameException {
        GameState state = new GameState();
        state.placeTower("Archer", 0, 0); // 150 -> 100 gold
        state.upgradeTower(0, 0);         // 100 -> 60
        state.upgradeTower(0, 0);         // 60 -> 20
        assertThrows(GameException.class, () -> state.upgradeTower(0, 0)); // can't afford
        assertEquals(2, state.getTower(0, 0).getLevel());
    }

    @Test
    void enemiesSpawnAndTowersKillThem() throws GameException {
        GameState state = new GameState();
        state.placeTower("Cannon", 1, 2);
        state.startWave();
        assertThrows(GameException.class, state::startWave); // wave already running
        for (int i = 0; i < 2000 && state.isWaveRunning(); i++) {
            state.update(0.02);
        }
        assertFalse(state.isWaveRunning());
        assertTrue(state.getGold() > 70, "should have earned gold from kills");
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

    // Test helper: a sensible player. Builds a mix of towers on tiles next to the path, then upgrades them.
    private static final int[][] SPOTS = {{2, 2}, {4, 2}, {3, 4}, {2, 4}, {4, 4}, {0, 2}, {6, 4}, {3, 2}, {3, 0}, {2, 6}};
    private static final String[] TYPES = {"Archer", "Cannon", "Mage"};

    private void buildAndUpgrade(GameState state) {
        for (int i = 0; i < SPOTS.length; i++) {
            try {
                state.placeTower(TYPES[i % 3], SPOTS[i][0], SPOTS[i][1]);
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

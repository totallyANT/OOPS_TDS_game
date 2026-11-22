package com.project.tds;

import com.project.tds.engine.GameState;
import com.project.tds.model.GameException;
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
    void tenWavesThenBossThenVictory() throws GameException {
        GameState state = new GameState();
        for (int w = 1; w <= GameState.MAX_WAVES; w++) {
            buildWhateverWeCanAfford(state);
            state.startWave();
            if (w == GameState.MAX_WAVES) {
                state.update(0.02); // let the first enemy spawn
                assertEquals(1, state.getEnemies().size());
                assertEquals("Boss", state.getEnemies().get(0).getName());
            }
            for (int i = 0; i < 5000 && state.isWaveRunning(); i++) {
                state.update(0.02);
            }
        }
        assertTrue(state.isVictory());
        assertThrows(GameException.class, state::startWave); // no wave 11
    }

    // Test helper: fill empty tiles next to the path with Cannons while gold lasts
    private void buildWhateverWeCanAfford(GameState state) {
        for (int r = 0; r < GameState.SIZE; r++) {
            for (int c = 0; c < GameState.SIZE; c++) {
                try {
                    state.placeTower("Cannon", r, c);
                } catch (GameException ignored) {
                    // path tile, occupied or not enough gold: just skip it
                }
            }
        }
    }
}

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
}

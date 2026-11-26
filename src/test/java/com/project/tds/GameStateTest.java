package com.project.tds;

import com.project.tds.engine.GameEngine;
import com.project.tds.engine.GameState;
import com.project.tds.exception.GameException;
import com.project.tds.exception.InsufficientGoldException;
import com.project.tds.exception.InvalidPlacementException;
import com.project.tds.exception.MaxUpgradeReachedException;
import com.project.tds.model.Targetable;
import com.project.tds.model.enemy.Boss;
import com.project.tds.model.enemy.Enemy;
import com.project.tds.model.enemy.ExtraWaves;
import com.project.tds.model.enemy.FrostGolem;
import com.project.tds.model.enemy.GamePath;
import com.project.tds.model.enemy.Goblin;
import com.project.tds.model.enemy.SplittingSlime;
import com.project.tds.model.tower.ArcherTower;
import com.project.tds.model.tower.CannonTower;
import com.project.tds.model.tower.SplashCannonTower;
import com.project.tds.model.tower.Tower;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {
    private final GamePath path = GamePath.createDefaultPath();

    @Test
    void buildingRules() throws GameException {
        GameState state = new GameState();
        GameEngine engine = new GameEngine(state);
        int startGold = state.getGold();
        assertThrows(InvalidPlacementException.class, () -> engine.placeTower(new ArcherTower(1, 0))); // path tile
        engine.placeTower(new ArcherTower(0, 0));
        assertEquals(startGold - ArcherTower.BASE_COST, state.getGold());
        assertThrows(InvalidPlacementException.class, () -> engine.placeTower(new ArcherTower(0, 0))); // occupied
        assertThrows(InvalidPlacementException.class, () -> new ArcherTower(7, 7)); // off the grid
    }

    @Test
    void notEnoughGold() {
        GameEngine engine = new GameEngine(new GameState(10, 5));
        assertThrows(InsufficientGoldException.class, () -> engine.placeTower(new ArcherTower(0, 0)));
    }

    @Test
    void upgradeStopsAtMaxLevel() throws GameException {
        GameEngine engine = new GameEngine(new GameState(10_000, 5));
        Tower archer = new ArcherTower(0, 0);
        engine.placeTower(archer);
        while (!archer.isMaxLevel()) {
            engine.upgradeTower(archer);
        }
        assertThrows(MaxUpgradeReachedException.class, () -> engine.upgradeTower(archer));
    }

    @Test
    void cannonSplashHitsNearbyEnemiesForHalfDamage() throws GameException {
        List<Targetable> enemies = new ArrayList<>();
        enemies.add(new Goblin(path)); // both goblins stand on the first path tile (1, 0)
        enemies.add(new Goblin(path));
        new SplashCannonTower(0, 0).tryAttack(enemies, 0);
        int full = Goblin.MAX_HEALTH;
        int hits = (enemies.get(0).getHealth() < full ? 1 : 0) + (enemies.get(1).getHealth() < full ? 1 : 0);
        assertEquals(2, hits);
        assertNotEquals(enemies.get(0).getHealth(), enemies.get(1).getHealth()); // one full hit, one half
        assertTrue(new SplashCannonTower(0, 0) instanceof CannonTower);
    }

    @Test
    void frostGolemCantBeSlowed() {
        FrostGolem golem = new FrostGolem(path);
        golem.applySlow(0.5, 2000);
        assertFalse(golem.isSlowed());
    }

    @Test
    void slimeSplitsIntoTwoSmallSlimes() {
        SplittingSlime slime = new SplittingSlime(path);
        slime.move(3.0);   // walk a few tiles in
        List<Enemy> pieces = slime.split();
        assertEquals(2, pieces.size());
        for (Enemy piece : pieces) {
            assertEquals("Small Slime", piece.getName());
            assertTrue(piece.getPath().getTotalDistance() < path.getTotalDistance()); // starts part-way along
        }
    }

    @Test
    void finalWaveEndsWithTheBoss() {
        List<Targetable> wave = ExtraWaves.build(GameState.TOTAL_WAVES, path);
        assertTrue(wave.get(wave.size() - 1) instanceof Boss);
        assertThrows(IllegalArgumentException.class, () -> ExtraWaves.build(GameState.TOTAL_WAVES + 1, path));
    }
}

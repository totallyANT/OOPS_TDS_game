package com.project.tds.model.enemy;

import com.project.tds.engine.GameState;
import com.project.tds.model.Targetable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Waves 6 to 10, played after the 5 waves in {@link WaveData}.
 * Wave n (6-9) has 4 + 2n Goblins, n Orcs, n / 2 Splitting Slimes and n / 4 Frost Golems.
 * Wave 10 is a shuffled horde of everything, with the Boss walking in last.
 */
public final class ExtraWaves {
    public static final int FIRST_WAVE = WaveData.getTotalWaves() + 1;

    private ExtraWaves() {
    }

    public static List<Targetable> build(int wave, GamePath path) {
        if (wave < FIRST_WAVE || wave > GameState.TOTAL_WAVES) {
            throw new IllegalArgumentException("ExtraWaves has no wave " + wave);
        }
        List<Targetable> enemies = new ArrayList<>();
        if (wave == GameState.TOTAL_WAVES) {
            for (int i = 0; i < 14; i++) enemies.add(new Goblin(path));
            for (int i = 0; i < 10; i++) enemies.add(new Orc(path));
            for (int i = 0; i < 8; i++) enemies.add(new SplittingSlime(path));
            enemies.add(new FrostGolem(path));
            Collections.shuffle(enemies);
            enemies.add(new Boss(path));
            return enemies;
        }
        for (int i = 0; i < 4 + 2 * wave; i++) enemies.add(new Goblin(path));
        for (int i = 0; i < wave; i++) enemies.add(new Orc(path));
        for (int i = 0; i < wave / 2; i++) enemies.add(new SplittingSlime(path));
        for (int i = 0; i < wave / 4; i++) enemies.add(new FrostGolem(path));
        return enemies;
    }
}

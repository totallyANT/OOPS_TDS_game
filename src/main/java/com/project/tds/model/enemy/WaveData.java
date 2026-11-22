package com.project.tds.model.enemy;

import com.project.tds.engine.GameState;
import com.project.tds.model.Targetable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Central catalog of wave compositions for the game (matching {@link GameState#TOTAL_WAVES}).
 *
 * Designed for direct consumption by Anirudh's {@code GameEngine} and {@code WaveSpawner}:
 * <pre>
 *     GamePath path = GamePath.createDefaultPath();
 *     List&lt;Targetable&gt; waveEnemies = WaveData.buildWaveForSpawner(waveNumber, path);
 *     gameEngine.launchWave(waveEnemies);
 * </pre>
 */
public final class WaveData {
    private static final List<WaveDefinition> DEFAULT_WAVES = createDefaultWaves();

    private WaveData() {
        // Utility class
    }

    private static List<WaveDefinition> createDefaultWaves() {
        List<WaveDefinition> waves = new ArrayList<>(GameState.TOTAL_WAVES);

        // Wave 1: Intro wave — 6 Goblins (total 270 HP, 72g reward)
        waves.add(new WaveDefinition(
                1,
                "Goblin Scout Party",
                Arrays.asList(
                        EnemyType.GOBLIN, EnemyType.GOBLIN, EnemyType.GOBLIN,
                        EnemyType.GOBLIN, EnemyType.GOBLIN, EnemyType.GOBLIN
                ),
                900L
        ));

        // Wave 2: Fast flank — 5 Goblins + 4 Slimes interleaved (total 505 HP, 132g reward)
        waves.add(new WaveDefinition(
                2,
                "Slime Rush",
                Arrays.asList(
                        EnemyType.GOBLIN, EnemyType.GOBLIN, EnemyType.SLIME,
                        EnemyType.GOBLIN, EnemyType.SLIME, EnemyType.GOBLIN,
                        EnemyType.SLIME, EnemyType.GOBLIN, EnemyType.SLIME
                ),
                850L
        ));

        // Wave 3: Armored push — 4 Goblins, 4 Orcs, 2 Slimes (total 800 HP, 184g reward)
        waves.add(new WaveDefinition(
                3,
                "Orc Vanguard",
                Arrays.asList(
                        EnemyType.GOBLIN, EnemyType.ORC, EnemyType.GOBLIN,
                        EnemyType.SLIME, EnemyType.ORC, EnemyType.GOBLIN,
                        EnemyType.ORC, EnemyType.SLIME, EnemyType.GOBLIN,
                        EnemyType.ORC
                ),
                800L
        ));

        // Wave 4: Mixed assault — 4 Slimes, 5 Orcs, 4 Goblins (total 1060 HP, 245g reward)
        waves.add(new WaveDefinition(
                4,
                "Relentless Onslaught",
                Arrays.asList(
                        EnemyType.SLIME, EnemyType.ORC, EnemyType.GOBLIN,
                        EnemyType.SLIME, EnemyType.ORC, EnemyType.GOBLIN,
                        EnemyType.ORC, EnemyType.SLIME, EnemyType.GOBLIN,
                        EnemyType.ORC, EnemyType.SLIME, EnemyType.GOBLIN,
                        EnemyType.ORC
                ),
                700L
        ));

        // Wave 5: Finale — escort, Boss, and rearguard (total 1515 HP, 326g reward)
        waves.add(new WaveDefinition(
                5,
                "Boss Siege",
                Arrays.asList(
                        EnemyType.GOBLIN, EnemyType.GOBLIN, EnemyType.GOBLIN,
                        EnemyType.SLIME, EnemyType.SLIME,
                        EnemyType.ORC, EnemyType.ORC,
                        EnemyType.BOSS,
                        EnemyType.ORC, EnemyType.ORC,
                        EnemyType.SLIME, EnemyType.SLIME, EnemyType.SLIME
                ),
                750L
        ));

        return Collections.unmodifiableList(waves);
    }

    /** @return unmodifiable list of all default wave definitions in order */
    public static List<WaveDefinition> getDefaultWaves() {
        return DEFAULT_WAVES;
    }

    /** @return total number of configured waves */
    public static int getTotalWaves() {
        return DEFAULT_WAVES.size();
    }

    /**
     * Looks up a wave definition by its 1-based wave number.
     *
     * @param waveNumber 1-based wave number (1 .. {@link #getTotalWaves()})
     * @return matching {@link WaveDefinition}
     * @throws IllegalArgumentException if {@code waveNumber} is out of range
     */
    public static WaveDefinition getWave(int waveNumber) {
        if (waveNumber < 1 || waveNumber > DEFAULT_WAVES.size()) {
            throw new IllegalArgumentException(
                    "Invalid wave number " + waveNumber + "; expected 1.." + DEFAULT_WAVES.size());
        }
        return DEFAULT_WAVES.get(waveNumber - 1);
    }

    /**
     * Instantiates all {@link Enemy} units for the specified wave along {@code path}.
     *
     * @param waveNumber 1-based wave number
     * @param path       the path the spawned enemies will follow
     * @return ordered list of {@link Enemy} instances
     */
    public static List<Enemy> buildWaveEnemies(int waveNumber, GamePath path) {
        return getWave(waveNumber).createEnemies(path);
    }

    /**
     * Instantiates all enemies for the specified wave as {@link Targetable} instances,
     * ready to pass directly into {@code GameEngine.launchWave(List<Targetable>)} or
     * {@code WaveSpawner.startWave(int, List<Targetable>)}.
     *
     * @param waveNumber 1-based wave number
     * @param path       the path the spawned enemies will follow
     * @return ordered list of {@link Targetable} enemy instances
     */
    public static List<Targetable> buildWaveForSpawner(int waveNumber, GamePath path) {
        return getWave(waveNumber).createTargetableEnemies(path);
    }
}

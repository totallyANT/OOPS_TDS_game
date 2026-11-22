package com.project.tds.model.enemy;

import com.project.tds.model.Targetable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable definition of a single enemy wave: wave number, title, ordered enemy spawn
 * sequence, and recommended spawn pacing interval for {@code WaveSpawner}.
 */
public final class WaveDefinition {
    private final int waveNumber;
    private final String name;
    private final List<EnemyType> spawnOrder;
    private final long spawnIntervalMs;

    /**
     * Creates a wave definition.
     *
     * @param waveNumber      1-based wave index (must be >= 1)
     * @param name            descriptive wave label for HUD/logs
     * @param spawnOrder      exact ordered list of enemy types to spawn (non-empty)
     * @param spawnIntervalMs delay in milliseconds between consecutive spawns (must be > 0)
     */
    public WaveDefinition(int waveNumber, String name, List<EnemyType> spawnOrder, long spawnIntervalMs) {
        if (waveNumber < 1) {
            throw new IllegalArgumentException("waveNumber must be >= 1: " + waveNumber);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Wave name must not be null or blank.");
        }
        if (spawnOrder == null || spawnOrder.isEmpty()) {
            throw new IllegalArgumentException("Wave spawnOrder must not be null or empty.");
        }
        if (spawnIntervalMs <= 0) {
            throw new IllegalArgumentException("spawnIntervalMs must be positive: " + spawnIntervalMs);
        }

        List<EnemyType> copy = new ArrayList<>(spawnOrder.size());
        for (int i = 0; i < spawnOrder.size(); i++) {
            EnemyType type = spawnOrder.get(i);
            if (type == null) {
                throw new IllegalArgumentException("EnemyType at index " + i + " must not be null.");
            }
            copy.add(type);
        }

        this.waveNumber = waveNumber;
        this.name = name;
        this.spawnOrder = Collections.unmodifiableList(copy);
        this.spawnIntervalMs = spawnIntervalMs;
    }

    public int getWaveNumber() {
        return waveNumber;
    }

    public String getName() {
        return name;
    }

    /** @return unmodifiable ordered list of enemy types spawned in this wave */
    public List<EnemyType> getSpawnOrder() {
        return spawnOrder;
    }

    public long getSpawnIntervalMs() {
        return spawnIntervalMs;
    }

    public int getEnemyCount() {
        return spawnOrder.size();
    }

    /**
     * Summarizes how many enemies of each {@link EnemyType} appear in this wave.
     * Useful for displaying upcoming wave previews in the HUD.
     *
     * @return unmodifiable map from enemy type to count
     */
    public Map<EnemyType, Integer> getEnemyCounts() {
        Map<EnemyType, Integer> counts = new EnumMap<>(EnemyType.class);
        for (EnemyType type : spawnOrder) {
            counts.merge(type, 1, Integer::sum);
        }
        return Collections.unmodifiableMap(counts);
    }

    /**
     * Instantiates fresh {@link Enemy} objects in exact wave spawn order.
     *
     * @param path the path all enemies in this wave will follow
     * @return mutable {@link ArrayList} of newly created {@link Enemy} instances
     */
    public List<Enemy> createEnemies(GamePath path) {
        Objects.requireNonNull(path, "path must not be null");
        List<Enemy> enemies = new ArrayList<>(spawnOrder.size());
        for (EnemyType type : spawnOrder) {
            enemies.add(type.createEnemy(path));
        }
        return enemies;
    }

    /**
     * Instantiates fresh enemies typed as {@link Targetable} for direct handoff to
     * {@code GameEngine.launchWave(List<Targetable>)} and {@code WaveSpawner.startWave(...)}.
     *
     * @param path the path all enemies in this wave will follow
     * @return mutable {@link ArrayList} of {@link Targetable} enemies in spawn order
     */
    public List<Targetable> createTargetableEnemies(GamePath path) {
        Objects.requireNonNull(path, "path must not be null");
        List<Targetable> targets = new ArrayList<>(spawnOrder.size());
        for (EnemyType type : spawnOrder) {
            targets.add(type.createEnemy(path));
        }
        return targets;
    }

    @Override
    public String toString() {
        return String.format("Wave %d (%s): %d enemies, %dms interval",
                waveNumber, name, spawnOrder.size(), spawnIntervalMs);
    }
}

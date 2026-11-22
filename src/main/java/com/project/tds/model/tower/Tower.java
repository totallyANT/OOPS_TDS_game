package com.project.tds.model.tower;

import com.project.tds.engine.GameState;
import com.project.tds.exception.InvalidPlacementException;
import com.project.tds.exception.MaxUpgradeReachedException;
import com.project.tds.model.Targetable;

import java.util.List;

public abstract class Tower {
    public static final int MAX_LEVEL = 3;

    private final String name;
    private final int row;
    private final int col;
    private final int cost;

    protected int level;
    protected int upgradeCost;
    protected int damage;
    protected double range;
    protected double fireRate;
    private long lastShotTimeMs;
    private TargetingStrategy targetingStrategy;

    public Tower(String name, int row, int col, int cost, int upgradeCost,
                 int damage, double range, double fireRate) throws InvalidPlacementException {
        if (row < 0 || row >= GameState.GRID_SIZE || col < 0 || col >= GameState.GRID_SIZE) {
            throw new InvalidPlacementException(row, col, "Position is outside the 7x7 grid.");
        }
        this.name = name;
        this.row = row;
        this.col = col;
        this.cost = cost;
        this.upgradeCost = upgradeCost;
        this.damage = damage;
        this.range = range;
        this.fireRate = fireRate;
        this.level = 1;
        this.lastShotTimeMs = -1L;
        this.targetingStrategy = TargetingStrategy.NEAREST;
    }

    public boolean canFire(long currentTimeMs) {
        if (lastShotTimeMs < 0) {
            return true;
        }
        long cooldownMs = (long) (1000.0 / fireRate);
        return (currentTimeMs - lastShotTimeMs) >= cooldownMs;
    }

    public Targetable tryAttack(List<Targetable> enemies, long currentTimeMs) {
        if (enemies == null || enemies.isEmpty() || !canFire(currentTimeMs)) {
            return null;
        }
        Targetable target = selectTarget(enemies);
        if (target != null) {
            attack(target);
            lastShotTimeMs = currentTimeMs;
        }
        return target;
    }

    public Targetable selectTarget(List<Targetable> enemies) {
        Targetable best = null;
        double bestMetric = Double.MAX_VALUE;

        for (Targetable enemy : enemies) {
            if (enemy == null || !enemy.isAlive()) {
                continue;
            }
            double dist = distanceTo(enemy);
            if (dist > range) {
                continue;
            }

            if (best == null) {
                best = enemy;
                bestMetric = metricFor(enemy, dist);
                continue;
            }

            double metric = metricFor(enemy, dist);
            if (targetingStrategy == TargetingStrategy.HIGHEST_HEALTH) {
                if (metric > bestMetric) {
                    best = enemy;
                    bestMetric = metric;
                }
            } else {
                if (metric < bestMetric) {
                    best = enemy;
                    bestMetric = metric;
                }
            }
        }
        return best;
    }

    private double metricFor(Targetable enemy, double dist) {
        switch (targetingStrategy) {
            case LOWEST_HEALTH:
            case HIGHEST_HEALTH:
                return enemy.getHealth();
            case NEAREST:
            default:
                return dist;
        }
    }

    public double distanceTo(Targetable target) {
        return Math.hypot(target.getX() - col, target.getY() - row);
    }

    public boolean isInRange(Targetable target) {
        return target != null && target.isAlive() && distanceTo(target) <= range;
    }

    protected abstract void attack(Targetable target);

    protected abstract void applyUpgradeStats();

    public void upgrade() throws MaxUpgradeReachedException {
        if (level >= MAX_LEVEL) {
            throw new MaxUpgradeReachedException(level, MAX_LEVEL);
        }
        level++;
        applyUpgradeStats();
        upgradeCost = (int) Math.round(upgradeCost * 1.5);
    }

    public boolean isMaxLevel() {
        return level >= MAX_LEVEL;
    }

    public String getName() { return name; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public int getCost() { return cost; }
    public int getLevel() { return level; }
    public int getUpgradeCost() { return upgradeCost; }
    public int getDamage() { return damage; }
    public double getRange() { return range; }
    public double getFireRate() { return fireRate; }
    public TargetingStrategy getTargetingStrategy() { return targetingStrategy; }

    public void setTargetingStrategy(TargetingStrategy targetingStrategy) {
        if (targetingStrategy != null) {
            this.targetingStrategy = targetingStrategy;
        }
    }
}

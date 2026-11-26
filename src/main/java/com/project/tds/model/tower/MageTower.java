package com.project.tds.model.tower;

import com.project.tds.exception.InvalidPlacementException;
import com.project.tds.model.Targetable;

public class MageTower extends Tower {
    public static final int BASE_COST = 65;
    public static final int BASE_UPGRADE_COST = 50;
    private static final int BASE_DAMAGE = 25;
    private static final double BASE_RANGE = 2.2;
    private static final double BASE_FIRE_RATE = 1.0;

    private final double slowFactor;
    private long slowDurationMs;
    private static final long SLOW_DURATION_INCREMENT_MS = 1000L;

    public MageTower(int row, int col) throws InvalidPlacementException {
        super("Mage Tower", row, col, BASE_COST, BASE_UPGRADE_COST,
                BASE_DAMAGE, BASE_RANGE, BASE_FIRE_RATE);
        this.slowFactor = 0.5;
        this.slowDurationMs = 2000L;
    }

    @Override
    protected void attack(Targetable target) {
        target.takeDamage(damage);
        if (target instanceof Slowable) {
            ((Slowable) target).applySlow(slowFactor, slowDurationMs);
        }
    }

    @Override
    protected void applyUpgradeStats() {
        slowDurationMs += SLOW_DURATION_INCREMENT_MS;
    }

    public double getSlowFactor() {
        return slowFactor;
    }

    public long getSlowDurationMs() {
        return slowDurationMs;
    }
}

package com.project.tds.model.tower;

import com.project.tds.model.enemy.Enemy;

/** Medium damage and slows the target. Upgrades extend the slow only. */
public class Mage extends Tower {
    public static final int COST = 70;
    private double slowDuration = 1.5;

    public Mage(int row, int col) {
        super("Mage", row, col, COST, 50, 15, 1.0, 1.8);
    }

    @Override
    protected void attack(Enemy target) {
        super.attack(target);
        target.applySlow(slowDuration);
    }

    @Override
    protected void applyUpgrade() {
        slowDuration += 1.0;
    }

    @Override
    public String getStats() {
        return String.format("Damage %d, slow %.1fs", damage, slowDuration);
    }
}

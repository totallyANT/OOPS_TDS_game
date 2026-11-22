package com.project.tds.model.tower;

/** Slow, heavy damage. Upgrades raise damage only. */
public class Cannon extends Tower {
    public static final int COST = 80;

    public Cannon(int row, int col) {
        super("Cannon", row, col, COST, 60, 30, 0.6, 1.6);
    }

    @Override
    protected void applyUpgrade() {
        damage += 15;
    }

    @Override
    public String getStats() {
        return String.format("Damage %d, %.2f shots/s", damage, fireRate);
    }
}

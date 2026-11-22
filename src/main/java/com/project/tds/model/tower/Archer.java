package com.project.tds.model.tower;

/** Fast, light damage. Upgrades raise fire rate only. */
public class Archer extends Tower {
    public static final int COST = 50;

    public Archer(int row, int col) {
        super("Archer", row, col, COST, 40, 8, 2.0, 2.0);
    }

    @Override
    protected void applyUpgrade() {
        fireRate += 0.75;
    }

    @Override
    public String getStats() {
        return String.format("Damage %d, %.2f shots/s", damage, fireRate);
    }
}

package com.project.tds.model.tower;

/** Fast, light damage. Upgrades raise fire rate only. Weak against armour (Boss). */
public class Archer extends Tower {
    public static final int COST = 50;

    public Archer(int row, int col) {
        super("Archer", row, col, COST, 45, 7, 2.0, 1.8);
    }

    @Override
    protected void applyUpgrade() {
        fireRate += 0.5;
    }

    @Override
    public String getStats() {
        return String.format("Damage %d, %.2f shots/s", damage, fireRate);
    }
}

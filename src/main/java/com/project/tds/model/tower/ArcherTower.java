package com.project.tds.model.tower;

import com.project.tds.exception.InvalidPlacementException;
import com.project.tds.model.Targetable;

public class ArcherTower extends Tower {
    public static final int BASE_COST = 50;
    public static final int BASE_UPGRADE_COST = 40;
    private static final int BASE_DAMAGE = 15;
    private static final double BASE_RANGE = 2.5;
    private static final double BASE_FIRE_RATE = 1.8;
    private static final double FIRE_RATE_INCREMENT = 0.6;

    public ArcherTower(int row, int col) throws InvalidPlacementException {
        super("Archer Tower", row, col, BASE_COST, BASE_UPGRADE_COST,
                BASE_DAMAGE, BASE_RANGE, BASE_FIRE_RATE);
    }

    @Override
    protected void attack(Targetable target) {
        target.takeDamage(damage);
    }

    @Override
    protected void applyUpgradeStats() {
        fireRate += FIRE_RATE_INCREMENT;
    }
}

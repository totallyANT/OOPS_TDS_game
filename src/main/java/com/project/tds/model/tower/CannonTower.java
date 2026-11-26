package com.project.tds.model.tower;

import com.project.tds.exception.InvalidPlacementException;
import com.project.tds.model.Targetable;

public class CannonTower extends Tower {
    public static final int BASE_COST = 75;
    public static final int BASE_UPGRADE_COST = 60;
    private static final int BASE_DAMAGE = 45;
    private static final double BASE_RANGE = 2.0;
    private static final double BASE_FIRE_RATE = 0.6;
    private static final int DAMAGE_INCREMENT = 25;

    public CannonTower(int row, int col) throws InvalidPlacementException {
        super("Cannon Tower", row, col, BASE_COST, BASE_UPGRADE_COST,
                BASE_DAMAGE, BASE_RANGE, BASE_FIRE_RATE);
    }

    @Override
    protected void attack(Targetable target) {
        target.takeDamage(damage);
    }

    @Override
    protected void applyUpgradeStats() {
        damage += DAMAGE_INCREMENT;
    }
}

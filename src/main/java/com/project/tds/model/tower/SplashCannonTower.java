package com.project.tds.model.tower;

import com.project.tds.exception.InvalidPlacementException;
import com.project.tds.model.Targetable;

import java.util.List;

/** A Cannon Tower whose shots also hit every enemy near the target for half damage. */
public class SplashCannonTower extends CannonTower {
    public static final double SPLASH_RADIUS = 0.9; // in cells

    public SplashCannonTower(int row, int col) throws InvalidPlacementException {
        super(row, col);
    }

    @Override
    public Targetable tryAttack(List<Targetable> enemies, long currentTimeMs) {
        Targetable target = super.tryAttack(enemies, currentTimeMs);
        if (target != null) {
            for (Targetable e : enemies) {
                double dist = Math.hypot(e.getX() - target.getX(), e.getY() - target.getY());
                if (e != target && e.isAlive() && dist <= SPLASH_RADIUS) {
                    e.takeDamage(damage / 2);
                }
            }
        }
        return target;
    }
}

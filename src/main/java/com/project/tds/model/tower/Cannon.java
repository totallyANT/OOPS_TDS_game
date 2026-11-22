package com.project.tds.model.tower;

import com.project.tds.model.enemy.Enemy;

import java.util.List;

/** Slow, heavy splash damage. Upgrades raise damage only. */
public class Cannon extends Tower {
    public static final int COST = 80;
    public static final double SPLASH_RADIUS = 0.9; // cells around the target that also get hit

    public Cannon(int row, int col) {
        super("Cannon", row, col, COST, 60, 30, 0.6, 1.8);
    }

    /** Full damage to the target, half damage to every other enemy close to it (splash). */
    @Override
    protected void attack(Enemy target, List<Enemy> enemies) {
        double tx = target.getX();
        double ty = target.getY();
        for (Enemy e : enemies) {
            double dx = e.getX() - tx;
            double dy = e.getY() - ty;
            if (e != target && e.isAlive() && Math.sqrt(dx * dx + dy * dy) <= SPLASH_RADIUS) {
                e.takeDamage(damage / 2);
            }
        }
        target.takeDamage(damage);
    }

    @Override
    protected void applyUpgrade() {
        damage += 15;
    }

    @Override
    public String getStats() {
        return String.format("Damage %d (splash), %.2f shots/s", damage, fireRate);
    }
}

package com.project.tds.model.tower;

import com.project.tds.model.enemy.Enemy;

import java.util.List;

/** Medium damage and slows the target. Upgrades extend the slow only. */
public class Mage extends Tower {
    public static final int COST = 70;
    private double slowDuration = 2.5;

    public Mage(int row, int col) {
        super("Mage", row, col, COST, 50, 15, 1.0, 1.8);
    }

    /**
     * Spread the slow around: aim at the furthest enemy that is NOT slowed yet.
     * Only if every enemy in range is already slowed, fall back to the normal choice.
     */
    @Override
    protected Enemy chooseTarget(List<Enemy> enemies) {
        Enemy target = null;
        for (Enemy e : enemies) {
            if (e.isAlive() && !e.isSlowed() && inRange(e)
                    && (target == null || e.getProgress() > target.getProgress())) {
                target = e;
            }
        }
        return target != null ? target : super.chooseTarget(enemies);
    }

    @Override
    protected void attack(Enemy target, List<Enemy> enemies) {
        super.attack(target, enemies);
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

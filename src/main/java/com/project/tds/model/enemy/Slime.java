package com.project.tds.model.enemy;

/**
 * Fast, resilient ooze: quickest enemy on the grid and passively regenerates
 * health while moving along the path, rewarding sustained fire and Mage slows.
 */
public class Slime extends Enemy {
    public static final int MAX_HEALTH = 70;
    public static final double BASE_SPEED = 1.45;
    public static final int GOLD_REWARD = 18;
    public static final int SCORE_REWARD = 20;
    public static final int LIFE_DAMAGE = 1;
    public static final double REGEN_PER_SECOND = 3.0;

    private double regenAccumulator;

    public Slime(GamePath path) {
        super("Slime", MAX_HEALTH, BASE_SPEED, GOLD_REWARD, SCORE_REWARD, LIFE_DAMAGE, path);
        this.regenAccumulator = 0.0;
    }

    public double getRegenPerSecond() {
        return REGEN_PER_SECOND;
    }

    @Override
    protected void onTick(double deltaSeconds) {
        if (getHealth() >= getMaxHealth()) {
            regenAccumulator = 0.0;
            return;
        }
        regenAccumulator += REGEN_PER_SECOND * deltaSeconds;
        int wholeHeal = (int) regenAccumulator;
        if (wholeHeal > 0) {
            heal(wholeHeal);
            regenAccumulator -= wholeHeal;
            if (getHealth() >= getMaxHealth()) {
                regenAccumulator = 0.0;
            }
        }
    }

    @Override
    public EnemyType getType() {
        return EnemyType.SLIME;
    }

    @Override
    public String getDescription() {
        return "Fast enemy that regenerates " + (int) REGEN_PER_SECOND + " HP per second.";
    }
}

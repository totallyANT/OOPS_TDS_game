package com.project.tds.model.enemy;

/**
 * Finale juggernaut enemy: massive health pool, heavy armor, partial slow resistance
 * (caps slow intensity and halves slow duration), and costs 3 lives if it reaches the exit.
 */
public class Boss extends Enemy {
    public static final int MAX_HEALTH = 550;
    public static final double BASE_SPEED = 0.55;
    public static final int GOLD_REWARD = 100;
    public static final int SCORE_REWARD = 150;
    public static final int LIFE_DAMAGE = 3;
    public static final int ARMOR = 8;
    public static final double MIN_SLOW_FACTOR = 0.70;
    public static final double SLOW_DURATION_MULTIPLIER = 0.50;

    public Boss(GamePath path) {
        super("Boss", MAX_HEALTH, BASE_SPEED, GOLD_REWARD, SCORE_REWARD, LIFE_DAMAGE, path);
    }

    public int getArmor() {
        return ARMOR;
    }

    @Override
    protected int computeIncomingDamage(int rawDamage) {
        return Math.max(1, rawDamage - ARMOR);
    }

    @Override
    protected double adjustSlowFactor(double requestedSlowFactor) {
        return Math.max(requestedSlowFactor, MIN_SLOW_FACTOR);
    }

    @Override
    protected long adjustSlowDuration(long requestedDurationMs) {
        return Math.max(1L, Math.round(requestedDurationMs * SLOW_DURATION_MULTIPLIER));
    }

    @Override
    public EnemyType getType() {
        return EnemyType.BOSS;
    }

    @Override
    public String getDescription() {
        return "Massive armored boss with partial slow resistance that deals "
                + LIFE_DAMAGE + " life damage if it escapes.";
    }
}

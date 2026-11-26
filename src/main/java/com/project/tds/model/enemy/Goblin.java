package com.project.tds.model.enemy;

/**
 * Base enemy unit: small health, brisk movement speed, modest gold reward.
 * Appears in early waves and as fast fodder alongside tougher units.
 */
public class Goblin extends Enemy {
    public static final int MAX_HEALTH = 45;
    public static final double BASE_SPEED = 1.25;
    public static final int GOLD_REWARD = 12;
    public static final int SCORE_REWARD = 10;
    public static final int LIFE_DAMAGE = 1;

    public Goblin(GamePath path) {
        super("Goblin", MAX_HEALTH, BASE_SPEED, GOLD_REWARD, SCORE_REWARD, LIFE_DAMAGE, path);
    }

    @Override
    public EnemyType getType() {
        return EnemyType.GOBLIN;
    }

    @Override
    public String getDescription() {
        return "Small-health base enemy with brisk speed.";
    }
}

package com.project.tds.model.enemy;

/**
 * Heavy armored bruiser: high health, slower march, flat armor that reduces
 * each incoming hit (making Cannon towers much more effective than light Archer arrows).
 */
public class Orc extends Enemy {
    public static final int MAX_HEALTH = 120;
    public static final double BASE_SPEED = 0.75;
    public static final int GOLD_REWARD = 25;
    public static final int SCORE_REWARD = 25;
    public static final int LIFE_DAMAGE = 1;
    public static final int ARMOR = 5;

    public Orc(GamePath path) {
        super("Orc", MAX_HEALTH, BASE_SPEED, GOLD_REWARD, SCORE_REWARD, LIFE_DAMAGE, path);
    }

    public int getArmor() {
        return ARMOR;
    }

    @Override
    protected int computeIncomingDamage(int rawDamage) {
        return Math.max(1, rawDamage - ARMOR);
    }

    @Override
    public EnemyType getType() {
        return EnemyType.ORC;
    }

    @Override
    public String getDescription() {
        return "Armored heavy unit that reduces each incoming hit by " + ARMOR + " damage.";
    }
}

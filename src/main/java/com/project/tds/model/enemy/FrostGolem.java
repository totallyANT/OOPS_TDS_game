package com.project.tds.model.enemy;

/**
 * Ice enemy that slows can't affect. It also gets faster the more hurt it is
 * (up to 1.5x speed when almost dead), so finish it off quickly.
 */
public class FrostGolem extends Enemy {
    public static final int MAX_HEALTH = 120;
    public static final double BASE_SPEED = 0.8;
    private static final double MAX_ENRAGE = 0.5;   // +50% speed at (almost) zero health

    public FrostGolem(GamePath path) {
        super("Frost Golem", MAX_HEALTH, BASE_SPEED, 20, 30, 1, path);
    }

    /** Ice armour: a slow factor of 1.0 means "no slow", so Mage slows do nothing. */
    @Override
    protected double adjustSlowFactor(double requestedSlowFactor) {
        return 1.0;
    }

    /** Enrage: walk further each tick the more hurt we are (1x at full health, 1.5x nearly dead). */
    @Override
    public synchronized boolean move(double deltaSeconds) {
        double hurt = 1.0 - getHealthPercentage();
        return super.move(deltaSeconds * (1 + MAX_ENRAGE * hurt));
    }

    @Override
    public EnemyType getType() {
        return EnemyType.FROST_GOLEM;
    }

    @Override
    public String getDescription() {
        return "Can't be slowed, and speeds up as it gets hurt.";
    }
}

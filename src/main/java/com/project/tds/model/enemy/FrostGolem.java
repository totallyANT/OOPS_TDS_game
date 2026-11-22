package com.project.tds.model.enemy;

import java.util.List;

/**
 * Ice enemy that slows can't affect. It also gets a bit faster the more hurt it is
 * (up to 1.5x speed when almost dead), so finish it off quickly.
 */
public class FrostGolem extends Enemy {
    private static final double START_SPEED = 0.8;
    private static final double MAX_ENRAGE = 0.5;   // +50% speed at (almost) zero health

    public FrostGolem(List<int[]> path) {
        super("Frost Golem", 120, START_SPEED, 8, path);
    }

    /** Ice armour: slows have no effect at all. */
    @Override
    public void applySlow(double seconds) {
        // do nothing
    }

    /** Enrage: speed goes from 1x (full health) up to 1.5x (nearly dead). */
    @Override
    public void takeDamage(int amount) {
        super.takeDamage(amount);
        double hurt = 1.0 - (double) getHealth() / getMaxHealth();
        setSpeed(START_SPEED * (1 + MAX_ENRAGE * hurt));
    }
}

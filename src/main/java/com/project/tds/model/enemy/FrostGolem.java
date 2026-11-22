package com.project.tds.model.enemy;

import java.util.List;

/**
 * Tanky ice giant. Slows don't work on it, and it gets faster the more hurt it is
 * (up to double speed when almost dead). Mages can't stop it, so you need raw damage.
 */
public class FrostGolem extends Enemy {
    private static final double START_SPEED = 0.7;

    public FrostGolem(List<int[]> path) {
        super("Frost Golem", 250, START_SPEED, 10, path);
    }

    /** Ice armour: slows have no effect at all. */
    @Override
    public void applySlow(double seconds) {
        // do nothing
    }

    /** Enrage: speed goes from 1x (full health) up to 2x (nearly dead). */
    @Override
    public void takeDamage(int amount) {
        super.takeDamage(amount);
        double hurt = 1.0 - (double) getHealth() / getMaxHealth();
        setSpeed(START_SPEED * (1 + hurt));
    }
}

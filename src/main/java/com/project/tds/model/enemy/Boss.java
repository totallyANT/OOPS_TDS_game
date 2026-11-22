package com.project.tds.model.enemy;

import java.util.List;

/** Huge health, armour on every hit, and costs 5 lives if it escapes. */
public class Boss extends Enemy {
    private static final int ARMOR = 5;

    public Boss(List<int[]> path) {
        super("Boss", 400, 0.6, 50, path);
    }

    @Override
    public void takeDamage(int amount) {
        super.takeDamage(Math.max(1, amount - ARMOR));
    }

    @Override
    public int getLivesCost() {
        return 5;
    }
}

package com.project.tds.model.enemy;

import java.util.List;

/** Medium enemy that shrugs off half of any slow effect. */
public class Slime extends Enemy {
    public Slime(List<int[]> path) {
        super("Slime", 50, 1.2, 8, path);
    }

    @Override
    public void applySlow(double seconds) {
        super.applySlow(seconds / 2);
    }
}

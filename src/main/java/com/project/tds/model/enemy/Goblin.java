package com.project.tds.model.enemy;

import java.util.List;

/** Basic enemy: low health, quick. */
public class Goblin extends Enemy {
    public Goblin(List<int[]> path) {
        super("Goblin", 30, 1.5, 5, path);
    }
}

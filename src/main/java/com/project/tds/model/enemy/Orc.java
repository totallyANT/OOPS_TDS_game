package com.project.tds.model.enemy;

import java.util.List;

/** Tanky and slow. */
public class Orc extends Enemy {
    public Orc(List<int[]> path) {
        super("Orc", 80, 0.9, 10, path);
    }
}

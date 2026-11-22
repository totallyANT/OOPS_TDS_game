package com.project.tds.model;

/** Anything a tower can aim at. Positions are in grid cells (column = x, row = y). */
public interface Targetable {
    double getX();
    double getY();
    boolean isAlive();
    void takeDamage(int amount);
}

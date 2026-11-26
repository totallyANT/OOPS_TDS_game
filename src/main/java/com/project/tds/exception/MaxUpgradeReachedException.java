package com.project.tds.exception;

/**
 * Thrown when attempting to upgrade a tower that has already reached its maximum level.
 */
public class MaxUpgradeReachedException extends GameException {
    private final int currentLevel;
    private final int maxLevel;

    public MaxUpgradeReachedException(int currentLevel, int maxLevel) {
        super(String.format("Cannot upgrade: tower is already at max level %d (current: %d).", maxLevel, currentLevel));
        this.currentLevel = currentLevel;
        this.maxLevel = maxLevel;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }
}

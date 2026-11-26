package com.project.tds.exception;

/**
 * Thrown when an action requires more gold than the player currently possesses.
 */
public class InsufficientGoldException extends GameException {
    private final int requiredGold;
    private final int currentGold;

    public InsufficientGoldException(int requiredGold, int currentGold) {
        super(String.format("Insufficient gold: required %d, but only have %d.", requiredGold, currentGold));
        this.requiredGold = requiredGold;
        this.currentGold = currentGold;
    }

    public int getRequiredGold() {
        return requiredGold;
    }

    public int getCurrentGold() {
        return currentGold;
    }
}

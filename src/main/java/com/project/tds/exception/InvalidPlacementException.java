package com.project.tds.exception;

/**
 * Thrown when attempting to place a tower on an invalid grid location
 */
public class InvalidPlacementException extends GameException {
    private final int row;
    private final int col;

    public InvalidPlacementException(int row, int col, String reason) {
        super(String.format("Invalid placement at (%d, %d): %s", row, col, reason));
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }
}

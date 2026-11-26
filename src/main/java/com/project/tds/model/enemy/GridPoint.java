package com.project.tds.model.enemy;

import com.project.tds.engine.GameState;

import java.util.Objects;

/**
 * Immutable coordinate on the 7x7 game grid.
 *
 * Coordinate convention (aligned with Targetable and Tower):
 * - x corresponds to column index (0 .. GRID_SIZE - 1, left to right)
 * - y corresponds to row index (0 .. GRID_SIZE - 1, top to bottom)
 */
public final class GridPoint {
    private final int row;
    private final int col;

    /**
     * Creates a validated grid point within the 7x7 board.
     *
     * @param row row index in [0, GameState.GRID_SIZE - 1]
     * @param col column index in [0, GameState.GRID_SIZE - 1]
     * @throws IllegalArgumentException if row or col is out of grid bounds
     */
    public GridPoint(int row, int col) {
        if (row < 0 || row >= GameState.GRID_SIZE || col < 0 || col >= GameState.GRID_SIZE) {
            throw new IllegalArgumentException(
                    String.format("GridPoint (%d, %d) is outside the %dx%d grid.",
                            row, col, GameState.GRID_SIZE, GameState.GRID_SIZE));
        }
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    /** @return X coordinate (column) for distance and rendering calculations */
    public double getX() {
        return col;
    }

    /** @return Y coordinate (row) for distance and rendering calculations */
    public double getY() {
        return row;
    }

    /**
     * Computes Euclidean distance in grid units to another grid point.
     *
     * @param other target point
     * @return Euclidean distance
     */
    public double distanceTo(GridPoint other) {
        Objects.requireNonNull(other, "other must not be null");
        return Math.hypot(other.col - this.col, other.row - this.row);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GridPoint)) return false;
        GridPoint other = (GridPoint) o;
        return row == other.row && col == other.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }
}

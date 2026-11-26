package com.project.tds.model.enemy;

import com.project.tds.engine.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the enemy traversal path on the 7x7 game grid.
 *
 * Default layout (19 path cells, 30 buildable tower cells, 18.0 grid units long):
 *
 *      c0  c1  c2  c3  c4  c5  c6
 *  r0   .   .   .   .   .   .   .
 *  r1   S > * > * > * > * > *   .
 *  r2   .   .   .   .   .   v   .
 *  r3   .   * < * < * < * < *   .
 *  r4   .   v   .   .   .   .   .
 *  r5   .   * > * > * > * > * > E
 *  r6   .   .   .   .   .   .   .
 *
 * The winding S-curve creates two strategic kill-boxes (row 2 and row 4) where towers
 * can cover enemies on two lanes while leaving 30 open tiles for placement.
 */
public final class GamePath {
    private final List<GridPoint> waypoints;
    private final Set<GridPoint> pathCells;
    private final double totalDistance;

    /**
     * Constructs a path from an ordered list of grid waypoints.
     *
     * @param waypoints ordered list containing at least 2 waypoints (start to exit)
     * @throws IllegalArgumentException if waypoints is null, has fewer than 2 points,
     *                                  contains null, or has identical consecutive points
     */
    public GamePath(List<GridPoint> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            throw new IllegalArgumentException("A GamePath requires at least 2 waypoints.");
        }

        List<GridPoint> copy = new ArrayList<>(waypoints.size());
        Set<GridPoint> cells = new LinkedHashSet<>();
        double distance = 0.0;

        for (int i = 0; i < waypoints.size(); i++) {
            GridPoint point = waypoints.get(i);
            if (point == null) {
                throw new IllegalArgumentException("Waypoint at index " + i + " must not be null.");
            }
            copy.add(point);
            cells.add(point);

            if (i > 0) {
                GridPoint prev = waypoints.get(i - 1);
                if (prev.equals(point)) {
                    throw new IllegalArgumentException("Consecutive waypoints must be distinct: " + point);
                }
                distance += prev.distanceTo(point);
                addSegmentCells(cells, prev, point);
            }
        }

        this.waypoints = Collections.unmodifiableList(copy);
        this.pathCells = Collections.unmodifiableSet(cells);
        this.totalDistance = distance;
    }

    /**
     * Creates the team's canonical S-curve path across the 7x7 grid.
     *
     * @return default 7x7 {@link GamePath}
     */
    public static GamePath createDefaultPath() {
        List<GridPoint> points = new ArrayList<>();
        // Top lane: (1,0) -> (1,5)
        for (int col = 0; col <= 5; col++) {
            points.add(new GridPoint(1, col));
        }
        // First turn down: (2,5) -> (3,5)
        points.add(new GridPoint(2, 5));
        points.add(new GridPoint(3, 5));
        // Middle lane: (3,4) -> (3,1)
        for (int col = 4; col >= 1; col--) {
            points.add(new GridPoint(3, col));
        }
        // Second turn down: (4,1) -> (5,1)
        points.add(new GridPoint(4, 1));
        points.add(new GridPoint(5, 1));
        // Bottom lane to exit: (5,2) -> (5,6)
        for (int col = 2; col <= 6; col++) {
            points.add(new GridPoint(5, col));
        }
        return new GamePath(points);
    }

    private static void addSegmentCells(Set<GridPoint> cells, GridPoint from, GridPoint to) {
        if (from.getRow() == to.getRow()) {
            int step = Integer.compare(to.getCol(), from.getCol());
            for (int c = from.getCol(); c != to.getCol(); c += step) {
                cells.add(new GridPoint(from.getRow(), c));
            }
        } else if (from.getCol() == to.getCol()) {
            int step = Integer.compare(to.getRow(), from.getRow());
            for (int r = from.getRow(); r != to.getRow(); r += step) {
                cells.add(new GridPoint(r, from.getCol()));
            }
        }
    }

    /** @return unmodifiable ordered list of waypoints from spawn to exit */
    public List<GridPoint> getWaypoints() {
        return waypoints;
    }

    /** @return unmodifiable list of key corner/turn vertices (including start and end) */
    public List<GridPoint> getCornerWaypoints() {
        List<GridPoint> corners = new ArrayList<>();
        corners.add(waypoints.get(0));
        for (int i = 1; i < waypoints.size() - 1; i++) {
            GridPoint prev = waypoints.get(i - 1);
            GridPoint curr = waypoints.get(i);
            GridPoint next = waypoints.get(i + 1);
            int dRow1 = Integer.compare(curr.getRow(), prev.getRow());
            int dCol1 = Integer.compare(curr.getCol(), prev.getCol());
            int dRow2 = Integer.compare(next.getRow(), curr.getRow());
            int dCol2 = Integer.compare(next.getCol(), curr.getCol());
            if (dRow1 != dRow2 || dCol1 != dCol2) {
                corners.add(curr);
            }
        }
        corners.add(waypoints.get(waypoints.size() - 1));
        return Collections.unmodifiableList(corners);
    }

    public GridPoint getStart() {
        return waypoints.get(0);
    }

    public GridPoint getEnd() {
        return waypoints.get(waypoints.size() - 1);
    }

    public GridPoint getWaypoint(int index) {
        return waypoints.get(index);
    }

    public int getWaypointCount() {
        return waypoints.size();
    }

    /** @return total path length in grid units */
    public double getTotalDistance() {
        return totalDistance;
    }

    /** @return unmodifiable set of all grid cells occupied by the path */
    public Set<GridPoint> getPathCells() {
        return pathCells;
    }

    /**
     * Checks whether the given cell lies on the enemy path.
     *
     * @param row grid row
     * @param col grid column
     * @return true if the cell is part of the path
     */
    public boolean isPathCell(int row, int col) {
        if (row < 0 || row >= GameState.GRID_SIZE || col < 0 || col >= GameState.GRID_SIZE) {
            return false;
        }
        return pathCells.contains(new GridPoint(row, col));
    }

    /**
     * Checks whether a tower can legally be placed on the given cell
     * (inside grid bounds and not on the enemy path).
     *
     * @param row grid row
     * @param col grid column
     * @return true if buildable
     */
    public boolean isBuildable(int row, int col) {
        if (row < 0 || row >= GameState.GRID_SIZE || col < 0 || col >= GameState.GRID_SIZE) {
            return false;
        }
        return !isPathCell(row, col);
    }

    /**
     * Builds a 7x7 boolean matrix where {@code grid[row][col] == true} marks a path tile.
     * Provided for convenient grid rendering in the JavaFX UI layer.
     *
     * @return newly allocated 7x7 boolean array
     */
    public boolean[][] toPathGrid() {
        boolean[][] grid = new boolean[GameState.GRID_SIZE][GameState.GRID_SIZE];
        for (GridPoint cell : pathCells) {
            grid[cell.getRow()][cell.getCol()] = true;
        }
        return grid;
    }
}

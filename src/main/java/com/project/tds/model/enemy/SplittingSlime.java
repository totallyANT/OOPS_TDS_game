package com.project.tds.model.enemy;

import java.util.ArrayList;
import java.util.List;

/**
 * A Slime (same stats and regeneration) that splits into 2 Small Slimes when it dies.
 * Slows also only last half as long on it.
 */
public class SplittingSlime extends Slime {

    public SplittingSlime(GamePath path) {
        super(path);
    }

    @Override
    protected long adjustSlowDuration(long requestedDurationMs) {
        return requestedDurationMs / 2;
    }

    /**
     * The two pieces that appear when this slime dies. They start on the last path tiles it walked over
     * (the second one tile further back), and walk the rest of the path from there.
     */
    public List<Enemy> split() {
        List<Enemy> pieces = new ArrayList<>();
        List<GridPoint> waypoints = getPath().getWaypoints();
        int lastPassed = getCurrentWaypointIndex() - 1;
        for (int i = 0; i < 2; i++) {
            int start = Math.max(0, lastPassed - i);
            GamePath rest = new GamePath(waypoints.subList(start, waypoints.size()));
            pieces.add(new SmallSlime(rest));
        }
        return pieces;
    }

    /** What a SplittingSlime leaves behind: weak, and doesn't split again. */
    public static class SmallSlime extends Enemy {
        public SmallSlime(GamePath path) {
            super("Small Slime", 20, Slime.BASE_SPEED, 4, 5, 1, path);
        }

        @Override
        public EnemyType getType() {
            return EnemyType.SLIME;
        }

        @Override
        public String getDescription() {
            return "Small piece of a split slime.";
        }
    }
}

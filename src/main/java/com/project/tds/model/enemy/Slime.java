package com.project.tds.model.enemy;

import java.util.ArrayList;
import java.util.List;

/** Medium enemy that shrugs off half of any slow effect, and splits into 2 small slimes when it dies. */
public class Slime extends Enemy {
    private static final int BASE_HEALTH = 50;
    private final boolean small;

    public Slime(List<int[]> path) {
        this(path, false);
    }

    /** Small slimes have much less health, give less gold and don't split again. */
    private Slime(List<int[]> path, boolean small) {
        super(small ? "Small Slime" : "Slime", small ? 15 : BASE_HEALTH,
                1.2, small ? 3 : 8, path);
        this.small = small;
    }

    @Override
    public void applySlow(double seconds) {
        super.applySlow(seconds / 2);
    }

    @Override
    public List<Enemy> onDeath() {
        List<Enemy> pieces = new ArrayList<>();
        if (small) {
            return pieces;
        }
        double waveFactor = (double) getMaxHealth() / BASE_HEALTH; // keep the same wave scaling as the parent
        for (int i = 0; i < 2; i++) {
            Slime piece = new Slime(getPath(), true);
            piece.scaleHealth(waveFactor);
            piece.setProgress(getProgress() - 0.3 * i); // second piece a little behind the first
            pieces.add(piece);
        }
        return pieces;
    }
}

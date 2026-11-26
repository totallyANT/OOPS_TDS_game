package com.project.tds.model.tower;

import com.project.tds.exception.InvalidPlacementException;
import com.project.tds.model.Targetable;
import com.project.tds.model.enemy.Enemy;

import java.util.ArrayList;
import java.util.List;

/**
 * A Mage Tower that spreads its slow around: it only aims at enemies that aren't slowed yet
 * (still using the tower's targeting strategy). If every enemy in range is slowed, it aims normally.
 */
public class SpreadMageTower extends MageTower {

    public SpreadMageTower(int row, int col) throws InvalidPlacementException {
        super(row, col);
    }

    @Override
    public Targetable selectTarget(List<Targetable> enemies) {
        List<Targetable> notSlowed = new ArrayList<>();
        for (Targetable t : enemies) {
            if (!(t instanceof Enemy) || !((Enemy) t).isSlowed()) {
                notSlowed.add(t);
            }
        }
        Targetable target = super.selectTarget(notSlowed);
        return target != null ? target : super.selectTarget(enemies);
    }
}

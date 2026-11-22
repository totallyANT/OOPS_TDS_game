package com.project.tds.model.tower;

public interface Slowable {
    void applySlow(double slowFactor, long durationMs);
}

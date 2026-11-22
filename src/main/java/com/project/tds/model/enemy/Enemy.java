package com.project.tds.model.enemy;

import com.project.tds.model.Targetable;

import java.util.ArrayList;
import java.util.List;

/** Base class for every enemy. Walks along the path; subclasses only change stats or special rules. */
public abstract class Enemy implements Targetable {
    private final String name;
    private int maxHealth;
    private double baseSpeed;         // cells per second
    private final int reward;         // gold on kill
    private final List<int[]> path;   // each entry is {row, col}

    private int health;
    private double progress;          // how far along the path (0 = first cell)
    private double slowTimer;         // seconds of slow left

    protected Enemy(String name, int maxHealth, double baseSpeed, int reward, List<int[]> path) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.baseSpeed = baseSpeed;
        this.reward = reward;
        this.path = path;
    }

    /** Makes this enemy tougher. Used to scale enemies up in later waves. */
    public void scaleHealth(double factor) {
        maxHealth = (int) Math.round(maxHealth * factor);
        health = maxHealth;
    }

    public void move(double dt) {
        double speed = baseSpeed;
        if (slowTimer > 0) {
            speed = baseSpeed * 0.5;
            slowTimer -= dt;
        }
        progress += speed * dt;
    }

    public boolean reachedEnd() {
        return progress >= path.size() - 1;
    }

    @Override
    public void takeDamage(int amount) {
        health = Math.max(0, health - amount);
    }

    public void applySlow(double seconds) {
        slowTimer = Math.max(slowTimer, seconds);
    }

    /** Enemies that appear when this one dies. Most leave nothing; Slime overrides this to split. */
    public List<Enemy> onDeath() {
        return new ArrayList<>();
    }

    /** Puts this enemy at a given point on the path (used when spawning split pieces). */
    protected void setProgress(double progress) {
        this.progress = Math.max(0, progress);
    }

    protected List<int[]> getPath() {
        return path;
    }

    /** How many lives the player loses if this enemy gets through. */
    public int getLivesCost() {
        return 1;
    }

    @Override
    public boolean isAlive() {
        return health > 0;
    }

    @Override
    public double getX() {
        return interpolate(1);
    }

    @Override
    public double getY() {
        return interpolate(0);
    }

    // index 0 = row, 1 = col; blends between the current cell and the next one
    private double interpolate(int index) {
        int i = (int) progress;
        if (i >= path.size() - 1) {
            return path.get(path.size() - 1)[index];
        }
        double t = progress - i;
        return path.get(i)[index] * (1 - t) + path.get(i + 1)[index] * t;
    }

    public String getName() { return name; }
    public double getSpeed() { return baseSpeed; }

    /** Lets a subclass speed itself up (Frost Golem gets faster when hit). */
    protected void setSpeed(double speed) {
        this.baseSpeed = speed;
    }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getReward() { return reward; }
    public double getProgress() { return progress; }
    public boolean isSlowed() { return slowTimer > 0; }
}

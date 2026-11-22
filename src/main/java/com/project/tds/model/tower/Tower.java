package com.project.tds.model.tower;

import com.project.tds.model.GameException;
import com.project.tds.model.Targetable;
import com.project.tds.model.enemy.Enemy;

import java.util.List;

/** Base class for all towers. Handles aiming and cooldown; subclasses decide how upgrades work. */
public abstract class Tower {
    public static final int MAX_LEVEL = 3;

    private final String name;
    private final int row;
    private final int col;
    private final int cost;
    private final int upgradeCost;
    private final double range;       // in cells

    protected int damage;
    protected double fireRate;        // shots per second
    private int level;
    private double cooldown;

    protected Tower(String name, int row, int col, int cost, int upgradeCost,
                    int damage, double fireRate, double range) {
        this.name = name;
        this.row = row;
        this.col = col;
        this.cost = cost;
        this.upgradeCost = upgradeCost;
        this.damage = damage;
        this.fireRate = fireRate;
        this.range = range;
    }

    /** Called every tick: count down, then shoot the enemy furthest along the path that's in range. */
    public void update(double dt, List<Enemy> enemies) {
        cooldown -= dt;
        if (cooldown > 0) {
            return;
        }
        Enemy target = null;
        for (Enemy e : enemies) {
            if (e.isAlive() && inRange(e) && (target == null || e.getProgress() > target.getProgress())) {
                target = e;
            }
        }
        if (target != null) {
            attack(target);
            cooldown = 1.0 / fireRate;
        }
    }

    /** Default attack is plain damage. Mage overrides this to also slow. */
    protected void attack(Enemy target) {
        target.takeDamage(damage);
    }

    public boolean inRange(Targetable t) {
        double dx = t.getX() - col;
        double dy = t.getY() - row;
        return Math.sqrt(dx * dx + dy * dy) <= range;
    }

    public final void upgrade() throws GameException {
        if (level >= MAX_LEVEL) {
            throw new GameException(name + " is already at max level");
        }
        level++;
        applyUpgrade();
    }

    /** What one upgrade level changes. */
    protected abstract void applyUpgrade();

    /** Short text for the side panel. */
    public abstract String getStats();

    public String getName() { return name; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public int getCost() { return cost; }
    public int getUpgradeCost() { return upgradeCost; }
    public double getRange() { return range; }
    public int getLevel() { return level; }
}

package td.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class for every defensive structure on the 5x5 grid.
 * Concrete towers decide how an attack is resolved and how upgrades scale.
 */
public abstract class Tower implements Upgradable {

    private static final AtomicInteger ID_SEQUENCE = new AtomicInteger(0);

    private final String id;
    private String name;
    private int cost;
    private int damage;
    private int range;
    private int level;
    private final int row;
    private final int col;

    protected Tower(String name, int cost, int damage, int range, int row, int col) {
        this.id = "TWR-" + ID_SEQUENCE.incrementAndGet();
        this.name = name;
        this.cost = cost;
        this.damage = damage;
        this.range = range;
        this.level = 1;
        this.row = row;
        this.col = col;
    }

    /** Applies this tower's attack to the target and returns a combat log line. */
    public abstract String attack(Enemy target);

    /** Human-readable description of the tower's special behaviour. */
    public abstract String getSpecialEffectDescription();

    /**
     * Manhattan distance from this tower to an enemy walking along row 2.
     */
    public boolean isInRange(Enemy enemy) {
        return Math.abs(this.row - 2) + Math.abs(this.col - enemy.getCol()) <= this.range;
    }

    @Override
    public String getUpgradeSummary() {
        return name + " L" + level + " (DMG " + damage + ", RNG " + range
                + ") - next upgrade " + getUpgradeCost() + "g";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCost() {
        return cost;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = Math.max(0, damage);
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = Math.max(0, range);
    }

    @Override
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }
}

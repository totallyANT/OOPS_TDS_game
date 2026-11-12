package td.model;

/**
 * Abstract superclass representing a defensive tower placed on the battlefield grid.
 * Implements Upgradable interface for leveling up towers.
 */
public abstract class Tower implements Upgradable {
    private final String id;
    private final String name;
    private final int cost;
    private int damage;
    private int range;
    private int level;
    private final int row;
    private final int col;

    public Tower(String id, String name, int cost, int damage, int range, int row, int col) {
        this.id = id;
        this.name = name;
        this.cost = cost;
        this.damage = damage;
        this.range = range;
        this.row = row;
        this.col = col;
        this.level = 1;
    }

    /**
     * Executes an attack against the specified target enemy.
     *
     * @param target enemy to attack
     * @return short combat narrative log
     */
    public abstract String attack(Enemy target);

    /**
     * Describes the unique combat attribute or bonus of this tower type.
     *
     * @return special effect description
     */
    public abstract String getSpecialEffectDescription();

    /**
     * Determines whether the given enemy is within Manhattan attack distance of this tower.
     * Enemy is located along fixed row 2 at enemy.getCol().
     *
     * @param enemy target enemy
     * @return true if Manhattan distance <= range
     */
    public boolean isInRange(Enemy enemy) {
        int distance = Math.abs(this.row - 2) + Math.abs(this.col - enemy.getCol());
        return distance <= this.range;
    }

    @Override
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public String getUpgradeSummary() {
        return String.format("%s Lv.%d -> Lv.%d (Next Cost: %dg, +DMG)",
                name, level, level + 1, getUpgradeCost());
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCost() {
        return cost;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public String getCoordinates() {
        return "(" + row + ", " + col + ")";
    }

    @Override
    public String toString() {
        return String.format("%s [ID: %s, Lv.%d, DMG: %d, RNG: %d] at (%d,%d)",
                name, id, level, damage, range, row, col);
    }
}

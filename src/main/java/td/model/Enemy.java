package td.model;

/**
 * Abstract superclass representing an enemy traversing the fixed path (Row 2).
 * Implements Damageable interface for polymorphic damage handling.
 */
public abstract class Enemy implements Damageable {
    private final String id;
    private final String type;
    private int health;
    private final int maxHealth;
    private int col;
    private final int rewardGold;
    private boolean slowed;

    public Enemy(String id, String type, int health, int maxHealth, int rewardGold) {
        this.id = id;
        this.type = type;
        this.health = health;
        this.maxHealth = maxHealth;
        this.rewardGold = rewardGold;
        this.col = 0;
        this.slowed = false;
    }

    @Override
    public abstract void takeDamage(int amount);

    /**
     * Describes the unique trait or combat behavior of the enemy subclass.
     *
     * @return trait description string
     */
    public abstract String getTraitDescription();

    /**
     * Advances the enemy along the path. If slowed, clears slow status and delays
     * advancement by one tick.
     */
    public void move() {
        if (slowed) {
            this.slowed = false;
        } else {
            this.col++;
        }
    }

    /**
     * Applies a slow effect to the enemy.
     */
    public void applySlow() {
        this.slowed = true;
    }

    /**
     * Checks if the enemy has marched past the last column of the defense perimeter.
     *
     * @return true if base breached (col > 4)
     */
    public boolean hasReachedBase() {
        return this.col > 4;
    }

    @Override
    public boolean isDead() {
        return this.health <= 0;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    @Override
    public int getHealth() {
        return health;
    }

    protected void setHealth(int health) {
        this.health = health;
    }

    @Override
    public int getMaxHealth() {
        return maxHealth;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return 2; // Fixed path is always row index 2
    }

    public int getRewardGold() {
        return rewardGold;
    }

    public boolean isSlowed() {
        return slowed;
    }

    @Override
    public String toString() {
        return String.format("%s [%d/%d HP] @ (2,%d)", type, health, maxHealth, col);
    }
}

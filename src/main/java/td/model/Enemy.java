package td.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class for every hostile unit. Enemies walk from column 0
 * towards column 4 along the fixed middle row of the battlefield.
 */
public abstract class Enemy implements Damageable {

    private static final AtomicInteger ID_SEQUENCE = new AtomicInteger(0);

    private final String id;
    private final String type;
    private int health;
    private final int maxHealth;
    private int col;
    private final int rewardGold;
    private boolean slowed;

    protected Enemy(String type, int health, int rewardGold) {
        this.id = "ENM-" + ID_SEQUENCE.incrementAndGet();
        this.type = type;
        this.health = health;
        this.maxHealth = health;
        this.col = 0;
        this.rewardGold = rewardGold;
        this.slowed = false;
    }

    /** Each subclass resolves incoming damage in its own way. */
    @Override
    public abstract void takeDamage(int amount);

    /** Describes the enemy's defining trait for the bestiary. */
    public abstract String getTraitDescription();

    /**
     * Advances one column. A slowed enemy spends this tick recovering
     * instead of moving.
     */
    public void move() {
        if (slowed) {
            slowed = false;
            return;
        }
        col++;
    }

    public void applySlow() {
        this.slowed = true;
    }

    public boolean hasReachedBase() {
        return col > 4;
    }

    @Override
    public boolean isDead() {
        return health <= 0;
    }

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

    @Override
    public int getMaxHealth() {
        return maxHealth;
    }

    protected void setHealth(int health) {
        this.health = Math.max(0, health);
    }

    public int getCol() {
        return col;
    }

    public int getRewardGold() {
        return rewardGold;
    }

    public boolean isSlowed() {
        return slowed;
    }
}

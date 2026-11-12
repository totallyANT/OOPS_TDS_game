package td.model;

/**
 * Interface representing any game entity capable of receiving damage.
 */
public interface Damageable {
    /**
     * Applies damage to the entity.
     *
     * @param amount raw damage value
     */
    void takeDamage(int amount);

    /**
     * Checks whether the entity has been destroyed or defeated.
     *
     * @return true if health <= 0, false otherwise
     */
    boolean isDead();

    /**
     * Gets current health of the entity.
     *
     * @return current health
     */
    int getHealth();

    /**
     * Gets maximum possible health of the entity.
     *
     * @return maximum health
     */
    int getMaxHealth();
}

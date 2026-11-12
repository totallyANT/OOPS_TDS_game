package td.model;

/**
 * Contract for entities that can receive damage.
 * Used through an interface reference when towers attack enemies.
 */
public interface Damageable {

    void takeDamage(int amount);

    boolean isDead();

    int getHealth();

    int getMaxHealth();
}

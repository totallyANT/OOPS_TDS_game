package com.project.tds.model;

/**
 * Interface contract for any entity on the grid that can be targeted, positioned,
 * and take damage from towers. Typically implemented by Enemy units.
 */
public interface Targetable {
    /** @return Current X coordinate (or column position) */
    double getX();

    /** @return Current Y coordinate (or row position) */
    double getY();

    /** @return Current remaining health points */
    int getHealth();

    /** @return Maximum health points */
    int getMaxHealth();

    /**
     * Applies damage to the target.
     * @param damage amount of damage points to inflict
     */
    void takeDamage(int damage);

    /** @return true if health > 0, false otherwise */
    boolean isAlive();
}

package td.model;

/**
 * Interface representing any game entity that can be upgraded.
 */
public interface Upgradable {
    /**
     * Upgrades the entity to its next level, improving stats.
     */
    void upgrade();

    /**
     * Returns the gold cost required for the next upgrade.
     *
     * @return upgrade cost in gold
     */
    int getUpgradeCost();

    /**
     * Returns the current upgrade level of the entity.
     *
     * @return current level
     */
    int getLevel();

    /**
     * Returns a human-readable summary of the entity's upgrade state.
     *
     * @return upgrade summary string
     */
    String getUpgradeSummary();
}

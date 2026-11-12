package td.model;

/**
 * Contract for anything that can be improved by spending gold.
 * Used through an interface reference in {@code GameEngine.upgradeTowerAt}.
 */
public interface Upgradable {

    void upgrade();

    int getUpgradeCost();

    int getLevel();

    String getUpgradeSummary();
}

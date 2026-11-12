package td.model;

/**
 * Cheap, reliable single-target tower.
 */
public class ArcherTower extends Tower {

    public ArcherTower(int row, int col) {
        super("Archer", 50, 20, 2, row, col);
    }

    @Override
    public String attack(Enemy target) {
        target.takeDamage(getDamage());
        return getName() + " L" + getLevel() + " shot " + target.getType()
                + " for " + getDamage() + " damage. "
                + target.getType() + " HP: " + target.getHealth();
    }

    @Override
    public void upgrade() {
        setLevel(getLevel() + 1);
        setDamage(getDamage() + 10);
    }

    @Override
    public int getUpgradeCost() {
        return 30 * getLevel();
    }

    @Override
    public String getSpecialEffectDescription() {
        return "Reliable single-target arrows.";
    }
}

package td.model;

/**
 * Arcane tower that damages and slows its target.
 */
public class MageTower extends Tower {

    public MageTower(int row, int col) {
        super("Mage", 75, 25, 2, row, col);
    }

    @Override
    public String attack(Enemy target) {
        target.takeDamage(getDamage());
        target.applySlow();
        return getName() + " L" + getLevel() + " zapped and slowed " + target.getType()
                + " for " + getDamage() + " damage. "
                + target.getType() + " HP: " + target.getHealth();
    }

    @Override
    public void upgrade() {
        setLevel(getLevel() + 1);
        setDamage(getDamage() + 12);
        if (getLevel() == 3) {
            setRange(getRange() + 1);
        }
    }

    @Override
    public int getUpgradeCost() {
        return 40 * getLevel();
    }

    @Override
    public String getSpecialEffectDescription() {
        return "Arcane bolt slows the target for one tick; range grows at level 3.";
    }
}

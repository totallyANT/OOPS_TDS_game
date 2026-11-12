package td.model;

/**
 * Arcane defense tower dealing magical damage and applying a chilling slow effect to targets.
 */
public class MageTower extends Tower {

    public MageTower(String id, int row, int col) {
        super(id, "Mage Tower", 75, 25, 2, row, col);
    }

    @Override
    public String attack(Enemy target) {
        int initialHealth = target.getHealth();
        target.takeDamage(getDamage());
        target.applySlow();
        int damageDealt = initialHealth - target.getHealth();
        return String.format("[%s] casts frost bolt at %s dealing %d damage and SLOWING movement! (%d HP left)",
                getId(), target.getType(), damageDealt, target.getHealth());
    }

    @Override
    public void upgrade() {
        int newLevel = getLevel() + 1;
        setLevel(newLevel);
        setDamage(getDamage() + 12);
        if (newLevel == 3) {
            setRange(getRange() + 1);
        }
    }

    @Override
    public int getUpgradeCost() {
        return 40 * getLevel();
    }

    @Override
    public String getSpecialEffectDescription() {
        return "Frost ray chills target (freezes movement for 1 turn); +12 damage per level; +1 range at Level 3.";
    }
}

package td.model;

/**
 * Rapid single-target physical defense tower with balanced cost and range.
 */
public class ArcherTower extends Tower {

    public ArcherTower(String id, int row, int col) {
        super(id, "Archer Tower", 50, 20, 2, row, col);
    }

    @Override
    public String attack(Enemy target) {
        int initialHealth = target.getHealth();
        target.takeDamage(getDamage());
        int damageDealt = initialHealth - target.getHealth();
        return String.format("[%s] fires an arrow at %s dealing %d damage! (%d HP left)",
                getId(), target.getType(), damageDealt, target.getHealth());
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
        return "Rapid physical arrows; +10 damage per level.";
    }
}

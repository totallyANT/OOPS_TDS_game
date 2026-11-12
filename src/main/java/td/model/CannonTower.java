package td.model;

/**
 * Heavy explosive defense tower dealing high short-range burst damage with explosive bonus.
 */
public class CannonTower extends Tower {

    public CannonTower(String id, int row, int col) {
        super(id, "Cannon Tower", 90, 45, 1, row, col);
    }

    @Override
    public String attack(Enemy target) {
        int initialHealth = target.getHealth();
        int totalDamage = getDamage() + 10; // explosive bonus
        target.takeDamage(totalDamage);
        int damageDealt = initialHealth - target.getHealth();
        return String.format("[%s] blasts %s with explosive shell dealing %d damage! (%d HP left)",
                getId(), target.getType(), damageDealt, target.getHealth());
    }

    @Override
    public void upgrade() {
        setLevel(getLevel() + 1);
        setDamage(getDamage() + 20);
    }

    @Override
    public int getUpgradeCost() {
        return 50 * getLevel();
    }

    @Override
    public String getSpecialEffectDescription() {
        return "+10 explosive bonus damage on every hit; +20 base damage per level.";
    }
}

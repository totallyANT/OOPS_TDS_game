package td.model;

/**
 * Short-range tower that deals bonus explosive damage.
 */
public class CannonTower extends Tower {

    public CannonTower(int row, int col) {
        super("Cannon", 90, 45, 1, row, col);
    }

    @Override
    public String attack(Enemy target) {
        int totalDamage = getDamage() + 10;
        target.takeDamage(totalDamage);
        return getName() + " L" + getLevel() + " blasted " + target.getType()
                + " for " + totalDamage + " damage. "
                + target.getType() + " HP: " + target.getHealth();
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
        return "Explosive shells: +10 bonus damage per hit.";
    }
}

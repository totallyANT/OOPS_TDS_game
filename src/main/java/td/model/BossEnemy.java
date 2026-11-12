package td.model;

/**
 * Heavy enemy with flat armor reduction, demonstrating overridden damage logic.
 */
public class BossEnemy extends Enemy {

    private final int armor = 8;

    public BossEnemy() {
        super("Armored Ogre", 140, 60);
    }

    @Override
    public void takeDamage(int amount) {
        int effectiveDamage = Math.max(5, amount - armor);
        setHealth(getHealth() - effectiveDamage);
    }

    @Override
    public String getTraitDescription() {
        return "Armored Ogre: reduces every hit by " + armor + " armor (minimum 5 damage).";
    }

    public int getArmor() {
        return armor;
    }
}

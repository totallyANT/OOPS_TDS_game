package td.model;

/**
 * High-durability boss enemy with passive armor that mitigates incoming damage.
 */
public class BossEnemy extends Enemy {
    private final int armor;

    public BossEnemy(String id) {
        super(id, "Armored Ogre", 140, 140, 60);
        this.armor = 8;
    }

    public BossEnemy(String id, int armor) {
        super(id, "Armored Ogre", 140, 140, 60);
        this.armor = armor;
    }

    @Override
    public void takeDamage(int amount) {
        // Armor mitigates damage, but each successful strike deals at least 5 damage
        int effectiveDamage = Math.max(5, amount - armor);
        int newHealth = Math.max(0, getHealth() - effectiveDamage);
        setHealth(newHealth);
    }

    @Override
    public String getTraitDescription() {
        return String.format("Heavy armor reduces all incoming damage by %d (minimum 5 damage taken per hit).", armor);
    }

    public int getArmor() {
        return armor;
    }
}

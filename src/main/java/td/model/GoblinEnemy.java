package td.model;

/**
 * Fast-moving standard enemy unit that takes full damage from attacks.
 */
public class GoblinEnemy extends Enemy {

    public GoblinEnemy(String id) {
        super(id, "Goblin", 50, 50, 20);
    }

    @Override
    public void takeDamage(int amount) {
        int newHealth = Math.max(0, getHealth() - amount);
        setHealth(newHealth);
    }

    @Override
    public String getTraitDescription() {
        return "Fast raider; vulnerable to all standard tower attacks.";
    }
}

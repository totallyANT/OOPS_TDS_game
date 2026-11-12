package td.model;

/**
 * Basic fast-moving enemy with no damage reduction.
 */
public class GoblinEnemy extends Enemy {

    public GoblinEnemy() {
        super("Goblin", 50, 20);
    }

    @Override
    public void takeDamage(int amount) {
        setHealth(Math.max(0, getHealth() - amount));
    }

    @Override
    public String getTraitDescription() {
        return "Fragile scout: takes full damage from every tower.";
    }
}

package com.project.tds.model.enemy;

/**
 * Enumerates the four enemy archetypes in the game and provides a factory method
 * to instantiate each concrete subclass on a given {@link GamePath}.
 */
public enum EnemyType {
    GOBLIN("Goblin"),
    ORC("Orc"),
    SLIME("Slime"),
    BOSS("Boss"),
    FROST_GOLEM("Frost Golem"); // Integration: added for the FrostGolem enemy

    private final String displayName;

    EnemyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Creates a new enemy instance of this type positioned at the start of {@code path}.
     *
     * @param path the path the enemy will traverse
     * @return newly created concrete {@link Enemy}
     */
    public Enemy createEnemy(GamePath path) {
        switch (this) {
            case GOBLIN:
                return new Goblin(path);
            case ORC:
                return new Orc(path);
            case SLIME:
                return new Slime(path);
            case BOSS:
                return new Boss(path);
            case FROST_GOLEM: // Integration
                return new FrostGolem(path);
            default:
                throw new IllegalStateException("Unhandled enemy type: " + this);
        }
    }
}

package com.project.tds.model.enemy;

import com.project.tds.engine.GameState;
import com.project.tds.model.Targetable;
import com.project.tds.model.tower.Slowable;

import java.util.Objects;

/**
 * Abstract base class for all enemy units traversing the 7x7 path.
 *
 * Implements {@link Targetable} (for towers and the engine) and {@link Slowable}
 * (for Mage Tower slow effects). Subclasses define concrete stats and may override
 * polymorphic hooks such as {@link #computeIncomingDamage(int)}, {@link #onTick(double)},
 * {@link #adjustSlowFactor(double)}, and {@link #adjustSlowDuration(long)}.
 *
 * All mutable state is private and synchronized for thread-safe access across the
 * game loop, wave spawner, and JavaFX UI threads.
 */
public abstract class Enemy implements Targetable, Slowable {
    private final String name;
    private final int maxHealth;
    private final double baseSpeed;
    private final int goldReward;
    private final int scoreReward;
    private final int lifeDamage;
    private final GamePath path;

    private int health;
    private double x;
    private double y;
    private int currentWaypointIndex;
    private double distanceTraveled;
    private boolean reachedEnd;
    private boolean rewardClaimed;

    // Slow status effect state (applied by MageTower via Slowable)
    private double slowFactor;
    private double remainingSlowSeconds;

    /**
     * Constructs an enemy placed at the start of the given {@link GamePath}.
     *
     * @param name        display name of the enemy
     * @param maxHealth   initial and maximum hit points (must be > 0)
     * @param baseSpeed   movement speed in grid cells per second (must be > 0)
     * @param goldReward  gold awarded when defeated (must be >= 0)
     * @param scoreReward score awarded when defeated (must be >= 0)
     * @param lifeDamage  player lives lost if this enemy reaches the exit (must be > 0)
     * @param path        path this enemy follows (must not be null)
     */
    protected Enemy(String name, int maxHealth, double baseSpeed,
                    int goldReward, int scoreReward, int lifeDamage, GamePath path) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Enemy name must not be null or blank.");
        }
        if (maxHealth <= 0) {
            throw new IllegalArgumentException("Enemy maxHealth must be positive: " + maxHealth);
        }
        if (baseSpeed <= 0.0 || Double.isNaN(baseSpeed) || Double.isInfinite(baseSpeed)) {
            throw new IllegalArgumentException("Enemy baseSpeed must be finite and positive: " + baseSpeed);
        }
        if (goldReward < 0) {
            throw new IllegalArgumentException("Enemy goldReward cannot be negative: " + goldReward);
        }
        if (scoreReward < 0) {
            throw new IllegalArgumentException("Enemy scoreReward cannot be negative: " + scoreReward);
        }
        if (lifeDamage <= 0) {
            throw new IllegalArgumentException("Enemy lifeDamage must be positive: " + lifeDamage);
        }
        this.path = Objects.requireNonNull(path, "Enemy path must not be null.");

        this.name = name;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.baseSpeed = baseSpeed;
        this.goldReward = goldReward;
        this.scoreReward = scoreReward;
        this.lifeDamage = lifeDamage;

        GridPoint start = path.getStart();
        this.x = start.getX();
        this.y = start.getY();
        this.currentWaypointIndex = 1;
        this.distanceTraveled = 0.0;
        this.reachedEnd = false;
        this.rewardClaimed = false;

        this.slowFactor = 1.0;
        this.remainingSlowSeconds = 0.0;
    }

    /** @return the concrete {@link EnemyType} enum for this enemy */
    public abstract EnemyType getType();

    /** @return short summary of this enemy's role and special traits */
    public abstract String getDescription();

    /**
     * Polymorphic hook allowing subclasses (e.g. armored enemies) to modify incoming damage.
     *
     * @param rawDamage positive incoming damage
     * @return effective damage to apply (at least 1)
     */
    protected int computeIncomingDamage(int rawDamage) {
        return rawDamage;
    }

    /**
     * Polymorphic hook invoked on each simulation tick while the enemy is alive
     * (e.g. for passive regeneration).
     *
     * @param deltaSeconds elapsed simulation time in seconds
     */
    protected void onTick(double deltaSeconds) {
        // Default: no per-tick passive behaviour
    }

    /**
     * Polymorphic hook invoked when the enemy's health reaches zero.
     */
    protected void onDeath() {
        // Default: no special on-death behaviour
    }

    /**
     * Polymorphic hook allowing subclasses to resist slow intensity.
     *
     * @param requestedSlowFactor speed multiplier in (0.0, 1.0]
     * @return effective speed multiplier in (0.0, 1.0]
     */
    protected double adjustSlowFactor(double requestedSlowFactor) {
        return requestedSlowFactor;
    }

    /**
     * Polymorphic hook allowing subclasses to resist slow duration.
     *
     * @param requestedDurationMs duration in milliseconds
     * @return effective duration in milliseconds
     */
    protected long adjustSlowDuration(long requestedDurationMs) {
        return requestedDurationMs;
    }

    /**
     * Heals this enemy up to {@code maxHealth}. Intended for subclass abilities such as regeneration.
     *
     * @param amount positive hit points to restore
     */
    protected synchronized void heal(int amount) {
        if (amount > 0 && isAlive()) {
            this.health = Math.min(this.maxHealth, this.health + amount);
        }
    }

    // --- Targetable implementation ---

    @Override
    public synchronized double getX() {
        return x;
    }

    @Override
    public synchronized double getY() {
        return y;
    }

    @Override
    public synchronized int getHealth() {
        return health;
    }

    @Override
    public int getMaxHealth() {
        return maxHealth;
    }

    @Override
    public synchronized void takeDamage(int damage) {
        if (damage <= 0 || !isAlive()) {
            return;
        }
        int effectiveDamage = Math.max(1, computeIncomingDamage(damage));
        this.health = Math.max(0, this.health - effectiveDamage);
        if (this.health == 0) {
            onDeath();
        }
    }

    /**
     * Returns true while the enemy is still active on the path (has health > 0
     * and has not yet exited the end of the path).
     */
    @Override
    public synchronized boolean isAlive() {
        return health > 0 && !reachedEnd;
    }

    // --- Slowable implementation (Mage Tower integration) ---

    /**
     * Applies a movement slow effect from a Mage tower.
     * If already slowed, retains the stronger slow factor and refreshes the remaining
     * duration to at least the new duration.
     *
     * @param slowFactor multiplier applied to {@code baseSpeed}, in (0.0, 1.0]
     * @param durationMs duration of the slow in milliseconds
     * @throws IllegalArgumentException if {@code slowFactor} is not in (0.0, 1.0]
     */
    @Override
    public synchronized void applySlow(double slowFactor, long durationMs) {
        if (Double.isNaN(slowFactor) || slowFactor <= 0.0 || slowFactor > 1.0) {
            throw new IllegalArgumentException("slowFactor must be in (0.0, 1.0]: " + slowFactor);
        }
        if (durationMs <= 0 || !isAlive() || slowFactor == 1.0) {
            return;
        }

        double effectiveFactor = Math.min(1.0, Math.max(0.05, adjustSlowFactor(slowFactor)));
        long effectiveDurationMs = adjustSlowDuration(durationMs);
        if (effectiveFactor >= 1.0 || effectiveDurationMs <= 0) {
            return;
        }

        double durationSeconds = effectiveDurationMs / 1000.0;
        if (this.remainingSlowSeconds <= 0.0) {
            this.slowFactor = effectiveFactor;
            this.remainingSlowSeconds = durationSeconds;
        } else {
            this.slowFactor = Math.min(this.slowFactor, effectiveFactor);
            this.remainingSlowSeconds = Math.max(this.remainingSlowSeconds, durationSeconds);
        }
    }

    /**
     * Advances the slow timer by {@code deltaSeconds}, clearing the slow once expired.
     *
     * @param deltaSeconds elapsed simulation time in seconds
     */
    public synchronized void updateSlow(double deltaSeconds) {
        if (deltaSeconds <= 0.0 || remainingSlowSeconds <= 0.0) {
            return;
        }
        remainingSlowSeconds = Math.max(0.0, remainingSlowSeconds - deltaSeconds);
        if (remainingSlowSeconds == 0.0) {
            slowFactor = 1.0;
        }
    }

    // --- Movement along the path ---

    /**
     * Advances this enemy along its {@link GamePath} for one simulation step.
     * Handles passive tick effects, slow speed modification and exact slow expiration,
     * waypoint transitions, and reaching the end of the path.
     *
     * @param deltaSeconds elapsed simulation time in seconds (e.g., 1.0 / 60.0)
     * @return {@code true} on the step when the enemy reaches the exit, {@code false} otherwise
     */
    public synchronized boolean move(double deltaSeconds) {
        if (deltaSeconds <= 0.0 || !isAlive()) {
            return false;
        }

        onTick(deltaSeconds);

        double remainingDistance = computeStepDistanceAndUpdateSlow(deltaSeconds);

        while (remainingDistance > 0.0 && currentWaypointIndex < path.getWaypointCount()) {
            GridPoint target = path.getWaypoint(currentWaypointIndex);
            double dx = target.getX() - x;
            double dy = target.getY() - y;
            double distToTarget = Math.hypot(dx, dy);

            if (distToTarget <= 1e-9 || remainingDistance >= distToTarget) {
                x = target.getX();
                y = target.getY();
                distanceTraveled += distToTarget;
                remainingDistance -= distToTarget;
                currentWaypointIndex++;
            } else {
                double ratio = remainingDistance / distToTarget;
                x += dx * ratio;
                y += dy * ratio;
                distanceTraveled += remainingDistance;
                remainingDistance = 0.0;
            }
        }

        if (currentWaypointIndex >= path.getWaypointCount()) {
            reachedEnd = true;
            return true;
        }
        return false;
    }

    /**
     * Moves this enemy along the path and automatically deducts lives from {@code gameState}
     * if it reaches the exit on this step.
     *
     * @param deltaSeconds elapsed simulation time in seconds
     * @param gameState    shared game state to penalize on exit (may be null)
     * @return {@code true} if the enemy reached the exit on this step
     */
    public synchronized boolean move(double deltaSeconds, GameState gameState) {
        boolean justReachedEnd = move(deltaSeconds);
        if (justReachedEnd && gameState != null) {
            gameState.loseLives(lifeDamage);
        }
        return justReachedEnd;
    }

    private double computeStepDistanceAndUpdateSlow(double deltaSeconds) {
        if (remainingSlowSeconds <= 0.0) {
            return deltaSeconds * baseSpeed;
        }

        if (deltaSeconds <= remainingSlowSeconds) {
            double distance = deltaSeconds * baseSpeed * slowFactor;
            updateSlow(deltaSeconds);
            return distance;
        }

        // Slow expires mid-step: split slowed and normal movement accurately
        double slowedPart = remainingSlowSeconds * baseSpeed * slowFactor;
        double normalPart = (deltaSeconds - remainingSlowSeconds) * baseSpeed;
        remainingSlowSeconds = 0.0;
        slowFactor = 1.0;
        return slowedPart + normalPart;
    }

    /**
     * Awards this enemy's gold and score rewards to {@code gameState} once when defeated.
     * Safe to call repeatedly; rewards are never granted more than once or if the enemy escaped.
     *
     * @param gameState shared game state to credit
     * @return {@code true} if the reward was claimed on this call
     */
    public synchronized boolean claimReward(GameState gameState) {
        if (gameState == null || !isDefeated() || rewardClaimed) {
            return false;
        }
        rewardClaimed = true;
        gameState.addGold(goldReward);
        gameState.addScore(scoreReward);
        return true;
    }

    // --- Accessors ---

    public String getName() {
        return name;
    }

    public double getBaseSpeed() {
        return baseSpeed;
    }

    public synchronized double getEffectiveSpeed() {
        return isSlowed() ? baseSpeed * slowFactor : baseSpeed;
    }

    public int getGoldReward() {
        return goldReward;
    }

    public int getScoreReward() {
        return scoreReward;
    }

    public int getLifeDamage() {
        return lifeDamage;
    }

    public GamePath getPath() {
        return path;
    }

    public synchronized boolean isDefeated() {
        return health <= 0;
    }

    public synchronized boolean hasReachedEnd() {
        return reachedEnd;
    }

    public synchronized boolean isRewardClaimed() {
        return rewardClaimed;
    }

    public synchronized boolean isSlowed() {
        return remainingSlowSeconds > 0.0 && slowFactor < 1.0;
    }

    public synchronized double getSlowFactor() {
        return isSlowed() ? slowFactor : 1.0;
    }

    public synchronized double getRemainingSlowSeconds() {
        return remainingSlowSeconds;
    }

    public synchronized double getHealthPercentage() {
        return (double) health / maxHealth;
    }

    public synchronized double getDistanceTraveled() {
        return distanceTraveled;
    }

    public synchronized double getPathProgress() {
        double total = path.getTotalDistance();
        return total > 0.0 ? Math.min(1.0, distanceTraveled / total) : 1.0;
    }

    public synchronized int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }

    @Override
    public synchronized String toString() {
        return String.format("%s[hp=%d/%d, pos=(%.2f,%.2f), speed=%.2f%s]",
                name, health, maxHealth, x, y, getEffectiveSpeed(),
                isSlowed() ? " (SLOWED)" : "");
    }
}

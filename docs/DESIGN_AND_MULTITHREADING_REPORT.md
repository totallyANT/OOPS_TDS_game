# Design and Multithreading Technical Report

## 1. Object-Oriented Programming (OOP) Pillars Implementation

This project was engineered from the ground up to rigorously embody all core pillars of Object-Oriented Software Design without extraneous third-party bloat.

### 1.1 Encapsulation
- **Data Hiding & State Protection**: All fields across models (`Tower`, `Enemy`, `GameRecord`) and service components (`GameEngine`) are strictly marked `private` (or `protected` where subclass mutation is controlled, e.g., `Enemy.setHealth()`).
- **Controlled Access**: External components access attributes exclusively via standard getters and controlled setters that validate internal constraints (e.g. non-negative health clamping, level and damage progression).
- **Defensive Copying**: `GameEngine` returns unmodifiable or copied lists (`Collections.unmodifiableList(...)`) for its internal collections (`towers`, `activeEnemies`, `gameRecords`, `notifications`) to prevent external callers from mutating engine state without going through sanctioned methods.

### 1.2 Inheritance
- **Base Tower Hierarchy**: The abstract base class `Tower` encapsulates shared spatial properties (`row`, `col`), common attributes (`id`, `name`, `cost`, `damage`, `range`, `level`), and universal range calculation logic (`isInRange()`). Concrete specializations (`ArcherTower`, `CannonTower`, `MageTower`) extend `Tower` to provide specialized stats and upgrades.
- **Base Enemy Hierarchy**: The abstract base class `Enemy` defines core movement, tracking, and combat state (`id`, `type`, `health`, `maxHealth`, `col`, `rewardGold`, `slowed`). Concrete classes (`GoblinEnemy`, `BossEnemy`) inherit shared pathing mechanics while customizing damage mitigation.

### 1.3 Polymorphism (Dynamic Method Dispatch)
- **Combat Resolution**: During each simulation step in `GameEngine.executeSimulationTick()`, the engine iterates over superclass references `Tower` and `Enemy`:
  ```java
  for (Tower tower : towers) {
      for (Enemy enemy : activeEnemies) {
          if (!enemy.isDead() && tower.isInRange(enemy)) {
              String attackMsg = tower.attack(enemy); // Dynamic dispatch to Archer, Cannon, or Mage
              ...
          }
      }
  }
  ```
- **Damage Mitigation**: When `tower.attack(enemy)` invokes `target.takeDamage(...)`, the runtime polymorphically executes either `GoblinEnemy.takeDamage()` (full direct damage) or `BossEnemy.takeDamage()` (incoming damage reduced by armor with a minimum floor of 5).

### 1.4 Abstraction
- Abstract classes `Tower` and `Enemy` extract common state and abstract away implementation details through pure abstract methods:
  - `Tower.attack(Enemy target)` and `Tower.getSpecialEffectDescription()`.
  - `Enemy.takeDamage(int amount)` and `Enemy.getTraitDescription()`.
- Callers (like the simulation engine) do not need to know the specific subclass mechanics; they interact purely through the abstract contract.

### 1.5 Interfaces
- **`Upgradable`**: Defines contract methods `upgrade()`, `getUpgradeCost()`, `getLevel()`, and `getUpgradeSummary()`. When `GameEngine.upgradeTowerAt(row, col)` upgrades a tower, it references the target via an `Upgradable` interface handle:
  ```java
  Upgradable upgradable = tower;
  if (gold < upgradable.getUpgradeCost()) throw new InsufficientGoldException(...);
  gold -= upgradable.getUpgradeCost();
  upgradable.upgrade();
  ```
- **`Damageable`**: Declares `takeDamage(int amount)`, `isDead()`, `getHealth()`, and `getMaxHealth()`. Implemented by `Enemy` to standardize health management and damage checks.

### 1.6 Custom Exception Handling
- **`InsufficientGoldException`**: A custom checked exception thrown when a player attempts to construct or upgrade a tower without sufficient gold reserves.
- **`InvalidPlacementException`**: A custom checked exception thrown when attempting to build on the enemy road (Row 2), placing on an already occupied cell, building out of grid bounds, or issuing commands during an active wave.
- **GUI Integration**: Caught directly within the JavaFX UI controller handlers, prompting contextual visual feedback via `javafx.scene.control.Alert` dialogs without application crashes.

---

## 2. Multithreading Architecture & Concurrency Strategy

The application employs a responsive multithreaded architecture featuring two dedicated background worker tasks (`WaveSpawnerTask` and `ReportGeneratorTask`) decoupled from the JavaFX Application Thread.

```
       +-------------------------------------------------+
       |           JavaFX Application Thread             |
       |  - Renders UI / 5x5 GridPane / TableView        |
       |  - Dispatches User Button Clicks & Form Inputs  |
       +-------------------------------------------------+
                  |                            |
       spawns new Thread             spawns new Thread
                  |                            |
                  v                            v
    +---------------------------+  +----------------------------+
    |    WaveSpawnerTask        |  |    ReportGeneratorTask     |
    |  - Thread 1 (Daemon)      |  |  - Thread 2 (Daemon)       |
    |  - Spawns Enemy Waves     |  |  - Aggregates Match Data   |
    |  - Ticks Simulation       |  |  - Compiles Statistics     |
    |  - Thread.sleep(700ms)    |  |  - Thread.sleep(600ms)     |
    +---------------------------+  +----------------------------+
                  |                            |
                  +-------------+--------------+
                                |
                   Platform.runLater(Runnable)
                                |
                                v
                   [ Safely Redraws JavaFX UI ]
```

### 2.1 Thread 1: `WaveSpawnerTask` (Real-Time Wave Simulation)
- **Role**: Drives the active combat round. Sequentially spawns 3 to 5 enemies, triggers step-by-step simulation ticks via `gameEngine.executeSimulationTick()`, and inserts pacing delays (`Thread.sleep(700)`).
- **Non-Blocking Execution**: Runs as a daemon background thread (`setDaemon(true)`). The JavaFX GUI remains fully interactive (users can switch tabs, inspect the Armory, or adjust views while combat unfolds).
- **Asynchronous UI Redraws**: Any visual changes (updating the 5x5 grid button labels/colors, changing health bars, adding combat events to the `ListView`) are dispatched safely through `Platform.runLater(...)`.

### 2.2 Thread 2: `ReportGeneratorTask` (Asynchronous Analytics Compiler)
- **Role**: Simulates heavy analytics aggregation and reporting over all match records, investment totals, wave outcomes, and defense distributions.
- **Background Aggregation**: Simulates intensive calculation latency (`Thread.sleep(600)`) while reading `GameEngine` statistics.
- **Decoupled Notification**: Dispatches thread progress messages ("Generating report in background...") and completion callbacks to the UI via `Platform.runLater(...)`, writing the compiled report into the Reports `TextArea`.

### 2.3 Thread Safety & Mutual Exclusion (`synchronized`)
- **Shared State Protection**: Both background threads interact with the single `GameEngine` instance.
- **Intrinsic Lock Synchronization**: All state mutations and queries across `GameEngine` (`placeTower`, `upgradeTowerAt`, `executeSimulationTick`, `spawnEnemy`, `recordWaveResult`, and state getters/setters) are declared with the `synchronized` keyword.
- **Race Condition Prevention**: Prevents concurrent modification exceptions on internal lists (`towers`, `activeEnemies`, `gameRecords`) when the UI queries the collection while `WaveSpawnerTask` ticks combat.

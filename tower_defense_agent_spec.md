# Coding Agent Specification: Simple JavaFX Tower Defense (OOP Project)

## 1. Project Overview & Goal

Build a minimal, self-contained **JavaFX Tower Defense Game** in Java that satisfies every mandatory requirement of the Object-Oriented Programming (OOP) course rubric while keeping the codebase small, readable, and free of external third-party libraries.

### Core Design Philosophy
* **Zero External Libraries**: Use only standard JDK packages (`java.util.*`, `java.io.*`, `java.time.*`) and JavaFX (`javafx.*`).
* **Grid-Based Simplicity**: Instead of continuous 2D vector physics, pixel collision, or sprite animations, represent the battlefield as a **5x5 `GridPane`** where Row 2 (index 2, middle row, columns 0 to 4) is the fixed enemy path and all other rows (0, 1, 3, 4) are buildable tower slots.
* **Strict MVC / Separation of Concerns**: Keep model classes, custom exceptions, and background services separate from JavaFX event-handler code.

---

## 2. Rubric & Technical Requirements Checklist

| Rubric Category | Weight | Mandatory Requirement | Exact Implementation in This Project |
| :--- | :--- | :--- | :--- |
| **OOP Design** | 20% | At least 8 meaningful classes, private fields with getters/setters, constructors, inheritance, and runtime polymorphism | 11 classes across `model`, `exception`, `service`, and `ui` packages; overridden `attack()` and `takeDamage()` invoked polymorphically via superclass references |
| **Abstract Classes & Interfaces** | 10% | At least 1 abstract class and at least 2 interfaces used via interface references | 2 abstract classes (`Tower`, `Enemy`) and 2 interfaces (`Upgradable`, `Damageable`) |
| **Collections & Method Design** | 10% | `ArrayList` of objects and modular helper methods outside GUI handlers | `ArrayList<Tower>`, `ArrayList<Enemy>`, `ArrayList<GameRecord>` managed by `GameEngine` methods |
| **Exception Handling** | 10% | Meaningful `try/catch/throw/throws` and custom user-defined exceptions | `InsufficientGoldException` and `InvalidPlacementException` thrown by `GameEngine` and shown via JavaFX `Alert` |
| **JavaFX GUI & Event Handling** | 20% | Login/role screen, at least 3 functional views/modules, standard controls (`ComboBox`, `TextField`, `Button`, `TableView`, `ListView`, `Alert`, `TabPane`), and event handlers | Role/player setup bar + 3-tab `TabPane` (Battlefield, Armory & Bestiary `TableView`, Match History & Reports `TableView`/`ListView`) |
| **Multithreading** | 10% | At least 2 independent background threads/tasks using `Thread`/`Runnable` and `Platform.runLater()` without freezing the GUI | `WaveSpawnerTask` (spawns/steps wave in background) and `ReportGeneratorTask` (compiles match analytics report asynchronously) |
| **Documentation & Testing** | 20% | UML class diagram, design doc, multithreading report, test cases, and `README.md` | Included as required markdown deliverables in `docs/` and project root |

---

## 3. Target Directory & Package Structure

Generate the project with the following directory layout:

```text
tower-defense-oop/
├── README.md
├── docs/
│   ├── DESIGN_AND_MULTITHREADING_REPORT.md
│   ├── TEST_CASES.md
│   └── UML_CLASS_DIAGRAM.md
└── src/
    └── main/
        └── java/
            └── td/
                ├── TowerDefenseApp.java
                ├── model/
                │   ├── Upgradable.java
                │   ├── Damageable.java
                │   ├── Tower.java
                │   ├── ArcherTower.java
                │   ├── CannonTower.java
                │   ├── MageTower.java
                │   ├── Enemy.java
                │   ├── GoblinEnemy.java
                │   ├── BossEnemy.java
                │   └── GameRecord.java
                ├── exception/
                │   ├── InsufficientGoldException.java
                │   └── InvalidPlacementException.java
                └── service/
                    ├── GameEngine.java
                    ├── WaveSpawnerTask.java
                    └── ReportGeneratorTask.java
```

---

## 4. Detailed Class-by-Class Implementation Specs

### 4.1 Interfaces (`td.model`)

#### `Upgradable.java`
* Methods:
  * `void upgrade()`
  * `int getUpgradeCost()`
  * `int getLevel()`
  * `String getUpgradeSummary()`
* Requirement: Must be referenced via an `Upgradable` variable when upgrading a selected tower in `GameEngine.upgradeTower(Upgradable target)`.

#### `Damageable.java`
* Methods:
  * `void takeDamage(int amount)`
  * `boolean isDead()`
  * `int getHealth()`
  * `int getMaxHealth()`
* Requirement: Must be referenced via a `Damageable` parameter or collection reference when applying damage.

---

### 4.2 Custom Exceptions (`td.exception`)

#### `InsufficientGoldException.java`
* Extends `Exception`.
* Constructor: `public InsufficientGoldException(String message)`
* Thrown when player gold is less than tower build cost or tower upgrade cost.

#### `InvalidPlacementException.java`
* Extends `Exception`.
* Constructor: `public InvalidPlacementException(String message)`
* Thrown when:
  * A player clicks a path cell (`row == 2`) to build a tower.
  * A player clicks a cell that already contains a tower (`!isUpgradeMode`).
  * A player attempts an invalid action while a wave is active or with an empty player name.

---

### 4.3 Abstract Classes & Subclasses (`td.model`)

#### `Tower.java` (Abstract Class)
* Implements `Upgradable`.
* Private fields:
  * `String id`
  * `String name`
  * `int cost`
  * `int damage`
  * `int range` (measured in Manhattan grid distance: `Math.abs(row - enemyRow) + Math.abs(col - enemyCol)`)
  * `int level`
  * `int row`
  * `int col`
* Constructors:
  * Parameterized constructor initializing all fields (`level` defaults to `1`).
* Abstract methods:
  * `public abstract String attack(Enemy target);` — Applies tower-specific attack logic to `target` and returns a short combat log message.
  * `public abstract String getSpecialEffectDescription();`
* Concrete methods:
  * `public boolean isInRange(Enemy enemy)` — Returns `true` if `Math.abs(this.row - 2) + Math.abs(this.col - enemy.getCol()) <= this.range`.
  * Standard getters and controlled setters (`setDamage`, `setRange`, `setLevel`).

#### `ArcherTower.java`
* Extends `Tower`.
* Stats: `cost = 50`, `damage = 20`, `range = 2`.
* `attack(Enemy target)`: Deals `getDamage()` to `target`.
* `upgrade()`: Increments `level` by 1, increases `damage` by 10.
* `getUpgradeCost()`: Returns `30 * getLevel()`.

#### `CannonTower.java`
* Extends `Tower`.
* Stats: `cost = 90`, `damage = 45`, `range = 1`.
* `attack(Enemy target)`: Deals `getDamage() + 10` bonus explosive damage to `target`.
* `upgrade()`: Increments `level` by 1, increases `damage` by 20.
* `getUpgradeCost()`: Returns `50 * getLevel()`.

#### `MageTower.java`
* Extends `Tower`.
* Stats: `cost = 75`, `damage = 25`, `range = 2`.
* `attack(Enemy target)`: Deals `getDamage()` and slows the enemy (`target.applySlow()`).
* `upgrade()`: Increments `level` by 1, increases `damage` by 12, increases `range` by 1 on level 3.
* `getUpgradeCost()`: Returns `40 * getLevel()`.

---

#### `Enemy.java` (Abstract Class)
* Implements `Damageable`.
* Private fields:
  * `String id`
  * `String type`
  * `int health`
  * `int maxHealth`
  * `int col` (0 to 4 along row 2)
  * `int rewardGold`
  * `boolean slowed`
* Constructor: Parameterized constructor setting `col = 0` and `slowed = false`.
* Abstract methods:
  * `public abstract void takeDamage(int amount);`
  * `public abstract String getTraitDescription();`
* Concrete methods:
  * `public void move()`: If `slowed` is true, clears `slowed` and stays in current column for 1 tick; otherwise increments `col` by 1.
  * `public void applySlow()`: Sets `slowed = true`.
  * `public boolean hasReachedBase()`: Returns `col > 4`.
  * `public boolean isDead()`: Returns `health <= 0`.
  * Standard getters and protected `setHealth(int health)`.

#### `GoblinEnemy.java`
* Extends `Enemy`.
* Stats: `type = "Goblin"`, `health = 50`, `rewardGold = 20`.
* `takeDamage(int amount)`: Reduces health by `amount` (`Math.max(0, getHealth() - amount)`).

#### `BossEnemy.java`
* Extends `Enemy`.
* Stats: `type = "Armored Ogre"`, `health = 140`, `rewardGold = 60`.
* Private field: `int armor = 8`.
* `takeDamage(int amount)`: Reduces incoming damage by `armor` (minimum 5 damage taken per hit) to clearly demonstrate runtime method overriding.

---

#### `GameRecord.java`
* Encapsulated data class for completed waves/matches.
* Private fields:
  * `int waveNumber`
  * `String playerName`
  * `String difficulty`
  * `int score`
  * `int remainingLives`
  * `String result` (`"CLEARED"` or `"BASE BREACHED"`)
  * `String timestamp`
* Constructor + Getters for JavaFX `TableView` property binding.

---

### 4.4 Core Logic & Background Threads (`td.service`)

#### `GameEngine.java`
* Holds the core collections:
  * `private final List<Tower> towers = new ArrayList<>();`
  * `private final List<Enemy> activeEnemies = new ArrayList<>();`
  * `private final List<GameRecord> gameRecords = new ArrayList<>();`
  * `private final List<String> notifications = new ArrayList<>();`
* State fields (synchronized access where read/updated by background threads):
  * `private String playerName = "Commander";`
  * `private String difficulty = "Normal";`
  * `private int gold = 150;`
  * `private int lives = 5;`
  * `private int currentWave = 0;`
  * `private int score = 0;`
  * `private boolean waveInProgress = false;`
* Key Modular Methods:
  * `public synchronized Tower placeTower(String towerType, int row, int col) throws InsufficientGoldException, InvalidPlacementException`
    * Validates `row != 2` (cannot place on enemy path).
    * Validates no existing tower at `(row, col)`.
    * Validates `gold >= tower.getCost()`.
    * Deducts gold, adds to `towers`, logs notification, and returns the placed `Tower`.
  * `public synchronized void upgradeTowerAt(int row, int col) throws InsufficientGoldException, InvalidPlacementException`
    * Finds the tower at `(row, col)` (throws `InvalidPlacementException` if none exists).
    * Uses an `Upgradable` interface reference to check `getUpgradeCost()`, deduct gold, and invoke `upgrade()`.
  * `public synchronized List<String> executeSimulationTick()`
    * Called by the background wave thread once per step.
    * Polymorphically iterates over `Tower` objects in `towers` and `Enemy` objects in `activeEnemies`.
    * If an enemy is in range, invokes `tower.attack(enemy)`.
    * Removes dead enemies, awards `rewardGold` and `score`, and advances surviving enemies via `enemy.move()`.
    * If `enemy.hasReachedBase()`, decrements `lives` and removes the enemy.
    * Returns tick log strings for UI display.
  * `public synchronized List<Tower> searchTowers(String keyword)`
    * Filters `towers` by name, ID, or coordinates so the user can search placed towers in the Armory tab.

#### `WaveSpawnerTask.java` (Thread 1 — Background Wave & Notification Worker)
* Implements `Runnable`.
* Purpose: Runs a full wave lifecycle on a background `Thread` (`setDaemon(true)`) so the JavaFX GUI remains completely responsive.
* Behavior:
  1. Spawns 3 to 5 `Enemy` instances (`GoblinEnemy` and at least one `BossEnemy`) across sequential steps using `Thread.sleep(700)`.
  2. Calls `gameEngine.executeSimulationTick()` on each step.
  3. Dispatches UI grid redraws, HUD label updates, and `ListView` notification updates inside `Platform.runLater(...)`.
  4. When all enemies in the wave are defeated or reach the base, records a `GameRecord`, marks `waveInProgress = false`, and triggers a summary update via `Platform.runLater(...)`.

#### `ReportGeneratorTask.java` (Thread 2 — Background Analytics & Summary Generator)
* Implements `Runnable`.
* Purpose: Generates a formatted multi-line system & combat summary report in a separate background `Thread` (`setDaemon(true)`).
* Behavior:
  1. Updates a status label to `"Generating report in background..."` via `Platform.runLater(...)`.
  2. Simulates report compilation (`Thread.sleep(600)`) while aggregating total towers built, total upgrade levels, wave history, average score, and current gold/lives from `GameEngine` in a thread-safe (`synchronized`) block.
  3. Writes the formatted report into the Reports tab `TextArea` and shows a completion status via `Platform.runLater(...)`.

---

### 4.5 JavaFX GUI Specification (`td.TowerDefenseApp`)

Build the entire UI programmatically in `TowerDefenseApp.java` (no fragile `.fxml` path resolution issues).

#### Top Bar (Login / Commander Setup & Live HUD)
* **Controls**:
  * `TextField` for Commander Name (with "Set Player" `Button` that validates non-empty input and throws/handles invalid input via `Alert`).
  * `ComboBox<String>` for Difficulty (`"Easy"`, `"Normal"`, `"Hard"`).
  * Live status `Label`s for **Gold**, **Lives**, **Wave #**, **Score**, and **Thread Status**.

#### Main Navigation (`TabPane` with 3 Functional Modules)
1. **Tab 1: Battlefield (Core Game Module)**
   * **Left Control Panel (`VBox`)**:
     * `ComboBox<String>` to select tower type (`"Archer Tower (50g)"`, `"Mage Tower (75g)"`, `"Cannon Tower (90g)"`).
     * `Button` ("Start Next Wave") — starts `new Thread(new WaveSpawnerTask(...)).start()`.
     * `Button` ("Upgrade Mode Toggle" or "Upgrade Selected Cell") — upgrades an existing tower on click.
     * `Button` ("Reset Game").
   * **Center Grid (`GridPane`)**:
     * 5x5 grid of `Button` cells (minimum `95x75` px each).
     * Row 2 cells are styled as the **Enemy Path** (amber/sand background) and display any `Enemy` currently on that column (`"Goblin [50 HP]"` or `"Ogre [140 HP]"`).
     * Rows 0, 1, 3, 4 are styled as **Buildable Grass** (green/slate background) and display placed tower details (`"Archer L1\nDMG: 20"`).
     * Clicking a cell triggers event handling wrapped in `try / catch (InsufficientGoldException | InvalidPlacementException ex)` which displays a ` javafx.scene.control.Alert(Alert.AlertType.WARNING)`.
   * **Right Notification Center (`ListView<String>`)**:
     * Displays real-time combat events, wave alerts, and background thread notifications.
2. **Tab 2: Armory & Tower Directory (Search & `TableView` Module)**
   * **Top Search Bar**: `TextField` + `"Search"` `Button` + `"Show All"` `Button`.
   * **Center `TableView<Tower>`**:
     * Columns: Tower ID, Name, Level, Damage, Range, Upgrade Cost, Coordinates `(row, col)`, Special Effect.
     * Populated from `gameEngine.getTowers()` (and filtered via `gameEngine.searchTowers(...)`).
3. **Tab 3: Match History & Background Reports (`TableView` + Threaded Report Module)**
   * **Top Section**: `TableView<GameRecord>` showing completed waves (Wave #, Commander, Difficulty, Score, Remaining Lives, Status, Timestamp).
   * **Bottom Section**:
     * `Button` ("Generate System & Combat Report (Background Thread)") — launches `new Thread(new ReportGeneratorTask(...)).start()`.
     * `TextArea` displaying the generated summary report.

---

## 5. Required Non-Code Deliverables to Generate

In addition to the Java source code, generate these 4 markdown files so the submission package is 100% complete according to Section 15 of the project statement:

1. **`README.md`**:
   * Project overview, Java/JavaFX compilation and execution commands (`javac --module-path ...` and `mvn javafx:run` / IDE instructions), and controls guide.
2. **`docs/UML_CLASS_DIAGRAM.md`**:
   * A Mermaid `classDiagram` showing `Upgradable`, `Damageable`, `Tower` (`ArcherTower`, `CannonTower`, `MageTower`), `Enemy` (`GoblinEnemy`, `BossEnemy`), `GameRecord`, `GameEngine`, `WaveSpawnerTask`, `ReportGeneratorTask`, and `TowerDefenseApp`.
3. **`docs/DESIGN_AND_MULTITHREADING_REPORT.md`**:
   * Concise explanation of how each OOP pillar (Encapsulation, Inheritance, Polymorphism, Abstraction, Interfaces, Exception Handling) is implemented, plus why `WaveSpawnerTask` and `ReportGeneratorTask` run on separate threads with `Platform.runLater()` and `synchronized` state access.
4. **`docs/TEST_CASES.md`**:
   * A structured table of 10 test cases covering normal operations (building a tower, upgrading a tower, running a wave, searching towers, generating a background report) and exceptional situations (building on the enemy path, building on an occupied tile, building/upgrading with insufficient gold, empty player name, clicking "Start Wave" while a wave is already running).

---

## 6. Explicit Non-Goals (Do NOT Implement)

To prevent unnecessary complexity or scope creep, **do not** implement any of the following:
* **No External Dependencies**: Do not add JUnit, Jackson, Gson, SQLite, ControlsFX, or FXGL.
* **No Continuous 2D Canvas Physics or Trigonometry**: Do not build vector-based projectile movement, sprite sheets, or `AnimationTimer` shaders. The 5x5 `GridPane` of buttons is simpler, visually clear, and directly uses standard JavaFX controls and event handlers.
* **No External Database or File Persistence**: Keep all state in `ArrayList` collections in memory (file/DB persistence is listed as optional in Section 19 of the prompt and adds unnecessary I/O failure points).
* **No FXML or CSS Files**: Define layouts and inline styles directly in Java code so the project compiles and runs without classpath resource-loading issues.

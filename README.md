# Tower Defense OOP - JavaFX Strategy Game

A lightweight, robust, self-contained **JavaFX Tower Defense Game** engineered in Java demonstrating core Object-Oriented Programming (OOP) concepts, custom exception handling, clean MVC separation, and multithreading.

Developed strictly in accordance with course requirements: **zero external third-party dependencies** (no external game engines, databases, or test runners).

---

## 1. Key Features & Highlights

* **Pure Object-Oriented Architecture**:
  * **Inheritance & Polymorphism**: Abstract classes `Tower` and `Enemy` dynamically dispatched during combat.
  * **Interface Contracts**: `Upgradable` (for leveling towers) and `Damageable` (for taking damage).
  * **Encapsulation**: Private state fields, unmodifiable collection getters, defensive copies, and controlled setters.
  * **Custom Checked Exceptions**: `InsufficientGoldException` and `InvalidPlacementException` for robust error boundaries.
* **Responsive 3-Tab JavaFX GUI**:
  * **Battlefield Tab**: 5x5 tactical grid with fixed Enemy Path (Row 2), color-coded defense tiles, real-time wave combat, and live event notification stream.
  * **Armory Tab**: `TableView<Tower>` with live Manhattan range, level, damage, and dynamic multi-criteria search filtering.
  * **Match History & Reports Tab**: `TableView<GameRecord>` documenting wave outcomes and an asynchronous system & combat report generator.
* **Multithreading & Concurrency**:
  * **`WaveSpawnerTask` (Thread 1)**: Runs wave progression, stepping, and enemy advancement on a background daemon thread without freezing the GUI.
  * **`ReportGeneratorTask` (Thread 2)**: Gathers and compiles game analytics and stats asynchronously.
  * **Thread Safety**: Synchronized engine methods and `Platform.runLater(...)` UI state dispatching.

---

## 2. Directory Structure

```text
Java_TDS_antigravity/
├── pom.xml
├── README.md
├── tower_defense_agent_spec.md
├── docs/
│   ├── DESIGN_AND_MULTITHREADING_REPORT.md
│   ├── TEST_CASES.md
│   └── UML_CLASS_DIAGRAM.md
└── src/
    ├── main/
    │   └── java/
    │       └── td/
    │           ├── TowerDefenseApp.java
    │           ├── model/
    │           │   ├── Upgradable.java
    │           │   ├── Damageable.java
    │           │   ├── Tower.java
    │           │   ├── ArcherTower.java
    │           │   ├── CannonTower.java
    │           │   ├── MageTower.java
    │           │   ├── Enemy.java
    │           │   ├── GoblinEnemy.java
    │           │   ├── BossEnemy.java
    │           │   └── GameRecord.java
    │           ├── exception/
    │           │   ├── InsufficientGoldException.java
    │           │   └── InvalidPlacementException.java
    │           └── service/
    │               ├── GameEngine.java
    │               ├── WaveSpawnerTask.java
    │               └── ReportGeneratorTask.java
    └── test/
        └── java/
            └── td/
                └── GameLogicTest.java
```

---

## 3. How to Build & Run

### Prerequisites
* **Java Development Kit (JDK)**: JDK 17 or higher (tested with JDK 27).
* **Apache Maven** or **JavaFX SDK** (if building with raw `javac`).

### Option A: Using Maven (Recommended)

1. **Compile the Project**:
   ```bash
   mvn compile
   ```

2. **Run Automated Test Suite**:
   ```bash
   mvn test-compile
   java -cp "target/classes;target/test-classes" td.GameLogicTest
   ```

3. **Launch the JavaFX Application**:
   ```bash
   mvn javafx:run
   ```

### Option B: Using Standalone JavaFX SDK (`javac` / `java`)

If running without Maven using a downloaded JavaFX SDK:
```bash
# Set JavaFX lib path
set PATH_TO_FX="path\to\javafx-sdk-21\lib"

# Compile all source files
javac --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.base -d out src\main\java\td\*.java src\main\java\td\model\*.java src\main\java\td\exception\*.java src\main\java\td\service\*.java

# Launch the Application
java --module-path %PATH_TO_FX% --add-modules javafx.controls,javafx.base -cp out td.TowerDefenseApp
```

### Option C: In IntelliJ IDEA / NetBeans / Eclipse
1. Open this directory as a Maven project.
2. The IDE will automatically detect `pom.xml` and download the JavaFX dependencies.
3. Run `TowerDefenseApp.java` (or run Maven goal `javafx:run`).

---

## 4. Gameplay & Controls Guide

1. **Top Bar (Commander Setup & HUD)**:
   * Enter your Commander Name and click **"Set Player"**.
   * Select a difficulty level (**Easy**, **Normal**, **Hard**).
   * Monitor live metrics: Gold (starts at 150g), Lives (starts at 5), Current Wave, Score, and Background Thread Status.
2. **Battlefield Controls**:
   * **Place Tower**: Select a tower type from the dropdown (**Archer: 50g**, **Mage: 75g**, **Cannon: 90g**), ensure mode is `BUILD`, and click an open grass tile (Rows 0, 1, 3, or 4).
   * **Upgrade Tower**: Click the **"Mode: BUILD"** toggle button to switch to **"Mode: UPGRADE"**, then click any existing tower to upgrade its stats and damage.
   * **Start Wave**: Click **"▶ Start Next Wave"** to initiate the background wave thread. Watch the enemies traverse along Row 2 and towers retaliate!
   * **Combat Log**: Real-time battle events appear in the notification panel on the right.
3. **Armory Directory**:
   * Inspect all placed towers, their stats, Manhattan ranges, upgrade costs, and special effects.
   * Use the search bar to filter towers by name, ID (e.g. `T1`), or coordinates `(row, col)`.
4. **Reports & History**:
   * Review all past waves and outcomes in the match history table.
   * Click **"Generate System & Combat Report"** to run an asynchronous background thread that compiles analytics into the summary display.

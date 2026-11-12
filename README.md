# Tower Defense - OOP Edition

A small, self-contained **JavaFX tower defense game** built for the Object-Oriented
Programming course rubric. The battlefield is a 5x5 grid: the middle row is the enemy
path, every other cell is buildable grass. Place towers, upgrade them, survive waves,
inspect your armory, and generate background reports.

The project uses **only the JDK and JavaFX**. There are no FXML files, CSS files,
databases, file persistence, or third-party libraries.

---

## Features

- Commander setup with name validation and Easy / Normal / Hard difficulty.
- Three tower types (Archer, Mage, Cannon) with distinct attacks and upgrades.
- Two enemy types (Goblin, Armored Ogre) with different damage handling.
- 5x5 button grid battlefield with a fixed enemy path on row 2.
- Live HUD: gold, lives, wave, score, and wave-thread status.
- Real-time notification center.
- Armory with a searchable `TableView<Tower>`.
- Match history `TableView<GameRecord>` and a background-generated report.
- Two daemon background threads using `Thread`, `Runnable`, `Platform.runLater`,
  and `synchronized` engine access.
- Custom checked exceptions surfaced through JavaFX `Alert` dialogs.

---

## Requirements

| Tool | Version used to build and test |
| :--- | :--- |
| JDK | 17 or newer (built and tested with JDK 27) |
| Maven | **Not required** - the Maven wrapper (`mvnw`) is included |
| JavaFX | 21.0.8 (downloaded automatically by Maven) |
| OS | Windows 11 (the game is cross-platform) |

You only need a JDK installed and an internet connection for the first run.
The Maven wrapper downloads Maven and the JavaFX libraries for your operating
system automatically, so there is nothing else to install.

---

## Project Structure

```text
Java_TDS_deepseek/
├── README.md
├── pom.xml
├── mvnw / mvnw.cmd          # Maven wrapper (no Maven install needed)
├── .mvn/wrapper/            # Wrapper configuration
├── tower_defense_agent_spec.md
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

## Run the Game (Recommended)

### Windows (PowerShell)

```powershell
git clone https://github.com/totallyANT/Java_TDS_deepseek.git
cd Java_TDS_deepseek
.\mvnw.cmd javafx:run
```

### Linux / macOS

```bash
git clone https://github.com/totallyANT/Java_TDS_deepseek.git
cd Java_TDS_deepseek
chmod +x mvnw        # only needed once, if git did not preserve the executable bit
./mvnw javafx:run
```

The first run downloads Maven and the JavaFX libraries into your local Maven
cache (`~/.m2`). Later runs start immediately. To compile without launching the
game, run `.\mvnw.cmd clean compile` (Windows) or `./mvnw clean compile`.

### IntelliJ IDEA / Eclipse

Open the folder as a **Maven project** (IntelliJ: *File > Open*, select `pom.xml`).
The IDE resolves JavaFX automatically. Run the `javafx:run` Maven goal, or run
`td.TowerDefenseApp` directly.

### Manual compile without Maven (optional)

If you prefer plain `javac`, download the JavaFX SDK from
[Gluon](https://gluonhq.com/products/javafx/), extract it, and point `JAVAFX_LIB`
at its `lib` folder.

```powershell
$env:JAVAFX_LIB = "C:\path\to\javafx-sdk-21.0.8\lib"

New-Item -ItemType Directory -Force build\classes | Out-Null

javac --module-path "$env:JAVAFX_LIB" --add-modules javafx.controls `
  -d build\classes (Get-ChildItem -Path src\main\java -Recurse -Filter *.java).FullName

java --module-path "$env:JAVAFX_LIB" --add-modules javafx.controls `
  -cp build\classes td.TowerDefenseApp
```

```bash
JAVAFX_LIB="/path/to/javafx-sdk-21.0.8/lib"

mkdir -p build/classes

javac --module-path "$JAVAFX_LIB" --add-modules javafx.controls \
  -d build/classes $(find src/main/java -name '*.java')

java --module-path "$JAVAFX_LIB" --add-modules javafx.controls \
  -cp build/classes td.TowerDefenseApp
```

---

## How to Play

1. Enter a commander name and click **Set Player**. Empty or whitespace-only names
   are rejected with a warning.
2. Choose a difficulty. Easy sends 3 enemies, Normal 4, Hard 5; every wave includes
   an Armored Ogre.
3. Select a tower in **Battlefield > BUILD MENU** and click a green grass cell.
4. To upgrade, click **Upgrade Mode: OFF** so it reads **ON**, then click a placed
   tower. Upgrade costs scale with the tower's current level.
5. Click **Start Next Wave**. The wave runs on a daemon background thread; the HUD
   and grid update through `Platform.runLater`.
6. Open **Armory & Bestiary** to search towers by name, ID, or `row,col`.
7. Open **Match History & Reports** to see completed waves and click
   **Generate System & Combat Report (Background Thread)**.
8. **Reset Game** clears the board, records, and counters, restoring 150 gold and
   5 lives. Resetting during a wave is blocked.

### Battlefield rules

| Cell | Behaviour |
| :--- | :--- |
| Row 2 (amber) | Enemy path. Towers cannot be placed here. Displays enemies currently on that column. |
| Rows 0, 1, 3, 4 (green) | Buildable grass. Displays tower name, level, damage, and range. |

Towers use Manhattan range:
`|towerRow - 2| + |towerCol - enemyCol| <= towerRange`.
Each tower attacks at most one in-range enemy per simulation tick.

### Tower and enemy stats

| Tower | Cost | Damage | Range | Upgrade effect |
| :--- | ---: | ---: | ---: | :--- |
| Archer | 50g | 20 | 2 | +10 damage per level; cost `30 * level` |
| Mage | 75g | 25 | 2 | +12 damage per level, range +1 at level 3; cost `40 * level` |
| Cannon | 90g | 45 | 1 | +10 bonus damage per hit; +20 damage per level; cost `50 * level` |

| Enemy | Health | Reward | Trait |
| :--- | ---: | ---: | :--- |
| Goblin | 50 | 20g | Takes full damage. |
| Armored Ogre | 140 | 60g | Reduces each hit by 8 armor, minimum 5 damage. |

Killing an enemy also adds its reward to the score. An enemy that reaches the base
costs one life. At zero lives, waves can no longer be started until the game is reset.

---

## Rubric Mapping

| Requirement | Where it lives |
| :--- | :--- |
| 8+ classes, encapsulation, inheritance, polymorphism | `td.model`, `td.service`, `td.exception` (16 source files) |
| 1+ abstract class and 2+ interfaces via interface references | `Tower`, `Enemy`; `Upgradable`, `Damageable` |
| `ArrayList` collections and modular helpers | `GameEngine` tower/enemy/record/notification lists |
| Custom checked exceptions | `InsufficientGoldException`, `InvalidPlacementException` |
| JavaFX views and controls | `TowerDefenseApp` with 3 tabs, table views, dialogs |
| 2 background threads | `WaveSpawnerTask`, `ReportGeneratorTask` |
| Documentation and tests | `docs/` and `docs/TEST_CASES.md` |

---

## Documentation

- [UML class diagram](docs/UML_CLASS_DIAGRAM.md)
- [Design and multithreading report](docs/DESIGN_AND_MULTITHREADING_REPORT.md)
- [Test cases and execution results](docs/TEST_CASES.md)

## Verification Performed

The sources were originally compiled with **no warnings** using JDK 27 and
JavaFX 27.0.1. The Maven build pins JavaFX 21.0.8 (LTS) for wider compatibility
and was compiled and launched successfully with `.\mvnw.cmd javafx:run` on JDK 27.
An 80-check engine test and a 35-check JavaFX UI smoke test were executed; all
checks passed. Details and results are recorded in
[docs/TEST_CASES.md](docs/TEST_CASES.md).

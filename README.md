# OOPS_TDS_game

A tower defence game written in Java with JavaFX, made for our Object Oriented Programming course (CSD2004).
(We'll figure out a name soon.)

## The game

Enemies walk along a winding path on a 7x7 grid. You build towers on the empty tiles beside the path to stop them before they reach the end.

- You start with **130 gold** and **20 lives**.
- There are **10 waves**. Waves 1 to 9 get bigger and tougher each time. **Wave 10 is the finale**: a fast horde of 34 enemies, with the **Boss** walking in last.
- Killing an enemy gives you gold. An enemy that reaches the end takes away lives (the Boss takes 5).
- Clear all 10 waves to win. Hit 0 lives and you lose.

### How to play

1. Pick a tower on the right panel (Archer, Cannon or Mage).
2. Click an empty grey tile to build it. You can't build on the dark path.
3. Click **Start next wave**.
4. Click one of your towers to see its range (orange circle) and click **Upgrade selected** to upgrade it (3 times max).

### Towers

| Tower  | Cost | Strength                         | Each upgrade (3 max)   |
|--------|------|----------------------------------|------------------------|
| Archer | 50   | Fast shots, light damage. Nearly useless against the Boss's armour | Shoots faster          |
| Cannon | 80   | Slow shots, heavy damage. **Splash**: enemies near the target take half damage | More damage            |
| Mage   | 70   | Medium damage, slows the enemy for 2.5s (half speed). Aims at enemies that aren't slowed yet | Slow lasts 1s longer   |

Towers shoot the enemy furthest along the path, since it is closest to escaping. The Mage is the exception: it aims at the furthest enemy that isn't slowed yet, so it spreads its slow across the group. Every shot is drawn briefly on the board (arrow line, cannon blast circle, magic beam), and slowed enemies get a violet ring.

No single tower type can win on its own. You need a mix, and Cannons with Mages work especially well together: Mages slow enemies so they bunch up, and Cannons splash the whole group. Watch out for Frost Golems, though. Mages can't slow them, so you need enough raw damage to bring them down before they speed up.

### Enemies

| Enemy  | Health | Speed     | Special                                               |
|--------|--------|-----------|-------------------------------------------------------|
| Goblin | 30     | Fast      | The basic enemy                                       |
| Orc    | 80     | Slow      | Tanky                                                 |
| Slime  | 50     | Medium    | Slows only last half as long on it. **Splits into 2 Small Slimes** (15 health, don't split again) when it dies |
| Frost Golem | 250 | Slow, speeds up when hurt | **Can't be slowed.** Gets faster the more hurt it is, up to double speed. Appears from wave 3 |
| Boss   | 400    | Very slow | Armour (5 less damage per hit), costs 5 lives, final wave only |

Wave *n* (for 1 to 9) has `4 + 2n` Goblins, `n` Orcs and `n / 2` Slimes. Waves from 3 onwards also have `n / 3` Frost Golems. Wave 10 is a shuffled horde of 14 Goblins, 10 Orcs, 8 Slimes and 2 Frost Golems, spawning almost twice as fast, followed by the Boss. Every enemy gets **+25% health per wave**, so the Boss has 1300 health. The health numbers in the table are wave-1 values.

## How to run

Needs Java 17+ and Maven. From this folder:

```bash
mvn javafx:run
```

Run the tests with:

```bash
mvn test
```

## Project structure

```
src/main/java/com/project/tds
├── Main.java               starts the JavaFX app
├── model/
│   ├── Targetable.java     interface: anything a tower can aim at
│   ├── GameException.java  our own exception for rule breaks
│   ├── enemy/              Enemy (abstract), Goblin, Orc, Slime, FrostGolem, Boss
│   └── tower/              Tower (abstract), Archer, Cannon, Mage
├── engine/
│   ├── GameState.java      the whole game: grid, towers, enemies, gold, lives, waves
│   └── GameLoop.java       background thread that updates the game ~60 times a second
└── ui/
    └── GameApp.java        JavaFX window: draws the board, handles clicks and buttons
src/test/java/.../GameStateTest.java   JUnit tests for building, upgrades, waves and winning
```

## How it works

Two threads run at the same time:

- **Game loop thread** (`GameLoop`) calls `GameState.update()` about 60 times a second. Each update spawns the next enemy, moves all enemies, lets every tower shoot, then removes dead enemies (adding gold) and escaped ones (taking lives).
- **JavaFX thread** (`GameApp`) redraws the board every frame and reacts to the player's clicks.

Both threads share one `GameState`, so its methods are `synchronized`. That way only one thread changes it at a time.

## OOP concepts used

| Concept | Where |
|---|---|
| Classes and objects | Every tower, enemy and the game state is an object, e.g. `new Archer(row, col)` |
| Encapsulation and constructors | Fields are `private` and change only through methods like `takeDamage()` and `placeTower()`. Subclasses pass their stats up through `super(...)` constructors |
| Inheritance | `Goblin`, `Orc`, `Slime`, `FrostGolem`, `Boss` extend `Enemy`. `Archer`, `Cannon`, `Mage` extend `Tower` |
| Polymorphism | The game calls `tower.attack(...)` without knowing the tower type. `Mage` overrides it to also slow, and `Cannon` overrides it to splash nearby enemies. `Mage` also overrides `chooseTarget()` to pick enemies that aren't slowed yet. `Boss` overrides `takeDamage()` (armour) and `getLivesCost()`. `FrostGolem` overrides `applySlow()` (does nothing) and `takeDamage()` (speeds up when hurt). `Slime` overrides `applySlow()` and `onDeath()` (to split into two) |
| Arrays / ArrayList of objects | The grid is a `Tower[7][7]` array, and the path is a `boolean[][]`. Enemies and the spawn queue are `ArrayList<Enemy>` because their size keeps changing |
| Method design | Small methods that each do one job: `placeTower`, `upgradeTower`, `startWave`, `update`, `inRange` |
| Abstract classes and interfaces | `Enemy` and `Tower` are abstract (`Tower.applyUpgrade()` has no body, so each tower must define it). `Targetable` is an interface that `Enemy` implements |
| Exception handling | `GameException` is thrown for illegal moves: building on the path or an occupied tile, not enough gold, max level, a wave already running, or no waves left. The UI catches it and shows the message. `GameLoop` handles `InterruptedException` |
| JavaFX and event handling | `GameApp` uses a `Canvas`, buttons, radio buttons, mouse-click handlers and an `AnimationTimer` |
| Multithreading | `GameLoop` runs on its own thread alongside the JavaFX thread, with `synchronized` methods in `GameState` and a `volatile` stop flag |

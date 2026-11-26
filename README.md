# OOPS_TDS_game

A tower defence game written in Java with JavaFX, made for our Object Oriented Programming course (CSD2004).
(We'll figure out a name soon.)

## Who wrote what

| Part | Author | Files |
|---|---|---|
| Game engine and threads | Aniruddha | `engine/GameEngine`, `WaveSpawner`, `GameEventListener`, `GameStatus`, `GameState` |
| Exceptions | Aniruddha | `exception/GameException`, `InsufficientGoldException`, `InvalidPlacementException`, `MaxUpgradeReachedException` |
| Towers | Indrani | `model/tower/Tower`, `ArcherTower`, `CannonTower`, `MageTower`, `Slowable`, `TargetingStrategy` |
| Enemies, path and waves 1-5 | dostroh | `model/enemy/Enemy`, `Goblin`, `Orc`, `Slime`, `Boss`, `EnemyType`, `GamePath`, `GridPoint`, `WaveData`, `WaveDefinition` |
| JavaFX UI, extras, build, tests | Shaurya | `ui/GameApp`, `style.css`, `FrostGolem`, `SplittingSlime`, `ExtraWaves`, `SplashCannonTower`, `SpreadMageTower`, `pom.xml`, tests |

Everyone's files are kept as they wrote them. The only changes made while putting the pieces together are marked with an `Integration` comment:

- `GameEngine`: `fight()` (enemies move, towers shoot, rewards are paid, slimes split), plus `placeTower`, `upgradeTower`, `launchNextWave` and `getTowerAt` for the UI.
- `GameState`: `TOTAL_WAVES` is 10 instead of 5.
- `EnemyType`: added `FROST_GOLEM`.

## The game

Enemies walk along an S-shaped path on a 7x7 grid. You build towers on the empty tiles beside the path to stop them before they reach the end.

- You start with **150 gold** and **5 lives**.
- There are **10 waves**. Waves 1-5 are the hand-made waves in `WaveData` (wave 5 is the "Boss Siege"). Waves 6-9 come from `ExtraWaves` and keep growing. **Wave 10 is the finale**: a horde of 33 enemies with the Boss walking in last.
- Killing an enemy gives you gold and score. An enemy that reaches the end takes away lives (the Boss takes 3).
- Clear all 10 waves to win. Hit 0 lives and you lose.

### How to play

1. Pick a tower on the right panel (Archer, Cannon or Mage).
2. Click an empty grey tile to build it. You can't build on the dark path.
3. Click **Start next wave**.
4. Click one of your towers to see its range (orange circle), change its **targeting** (nearest, lowest health, highest health) and **upgrade** it (up to level 3).
5. **Pause** stops the game, **New game** starts over.

### Towers

| Tower  | Cost | Strength | Each upgrade |
|--------|------|----------|--------------|
| Archer | 50 | Fast shots, light damage | Shoots faster |
| Cannon | 75 | Slow shots, heavy damage. **Splash**: enemies near the target take half damage | More damage |
| Mage   | 65 | Medium damage, slows the enemy to half speed for 2s. Aims at enemies that aren't slowed yet | Slow lasts 1s longer |

Every shot is drawn briefly on the board (arrow line, cannon blast circle, magic beam), and slowed enemies get a violet ring.

### Enemies

| Enemy  | Health | Speed | Special |
|--------|--------|-------|---------|
| Goblin | 45  | Fast      | The basic enemy |
| Orc    | 120 | Slow      | Armour: 5 less damage per hit |
| Slime  | 70  | Very fast | Regenerates 3 health per second |
| Splitting Slime | 70 | Very fast | Like a Slime, but slows last half as long, and it **splits into 2 Small Slimes** when it dies. Waves 6-10 |
| Frost Golem | 120 | Medium | **Can't be slowed.** Gets faster the more hurt it is (up to 1.5x). Waves 6-10 |
| Boss   | 550 | Very slow | Armour 8, resists slows, costs 3 lives |

## How to run

Needs Java 17+ and Maven. From this folder:

```bash
mvn javafx:run
```

Run the tests with:

```bash
mvn test
```

## How to read the code

Read the files in this order. Each one builds on the previous ones.

| # | File | What it does | OOP idea to point at |
|---|---|---|---|
| 1 | `Main` | Starts the JavaFX app | — |
| 2 | `ui/GameApp` | The window: side panel, clicks and buttons, draws the board every frame | JavaFX, event handling |
| 3 | `engine/GameState` | Gold, lives, score, wave number, the lists of towers and enemies | Encapsulation, `synchronized` |
| 4 | `engine/GameEngine` | Background thread running 60 ticks a second; one tick = `fight()` then win/loss checks | Multithreading, observer pattern |
| 5 | `engine/WaveSpawner` | Background thread that lets enemies in one at a time | Threads, `wait`/`notifyAll` |
| 6 | `model/tower/Tower` | Parent of all towers: range, cooldown, targeting, upgrades | Abstract class |
| 7 | `ArcherTower`, `CannonTower`, `MageTower`, `SplashCannonTower`, `SpreadMageTower` | Each tower's stats and special attack | Inheritance, overriding |
| 8 | `model/enemy/Enemy` | Parent of all enemies: health, walking the path, slows | Abstract class, hook methods |
| 9 | `Goblin`, `Orc`, `Slime`, `SplittingSlime`, `FrostGolem`, `Boss` | Each enemy's stats and special rule | Polymorphism |
| 10 | `GamePath`, `WaveData`, `ExtraWaves` | The path and what each wave contains | Immutable classes, enums |
| 11 | `exception/*` | Our own exceptions for illegal moves | Custom checked exceptions |

**Example: what happens when you click a tile**
1. `GameApp.onTileClicked(row, col)` runs because of the mouse-click handler set up in `start()`.
2. It creates the tower (`new ArcherTower(row, col)`) and calls `engine.placeTower(tower)`.
3. If the tile is on the path or taken, an `InvalidPlacementException` is thrown. If you can't afford it, `GameState.deductGold` throws an `InsufficientGoldException`.
4. `GameApp` catches the exception and shows its message in red. Otherwise the tower is added to the game state.
5. On the next frame, `draw()` sees the new tower and draws it.

## Project structure

```
src/main/java/com/project/tds
├── Main.java
├── engine/       GameEngine, WaveSpawner, GameEventListener, GameState, GameStatus
├── exception/    GameException and its three subclasses
├── model/
│   ├── Targetable.java
│   ├── enemy/    Enemy + subclasses, EnemyType, GamePath, GridPoint, WaveData, WaveDefinition, ExtraWaves
│   └── tower/    Tower + subclasses, Slowable, TargetingStrategy
└── ui/           GameApp (JavaFX)
src/main/resources/style.css
src/test/java/com/project/tds/GameStateTest.java
```

## How it works

Three threads run at the same time:

- **Engine thread** (`GameEngine`) ticks 60 times a second: enemies move, towers shoot, rewards are paid, dead and escaped enemies are removed, and it checks for a win or loss.
- **Spawner thread** (`WaveSpawner`) adds the wave's enemies one at a time, 0.8s apart.
- **JavaFX thread** (`GameApp`) redraws the board every frame and handles clicks. It hears about game over, victory and log messages through `GameEventListener`, and uses `Platform.runLater` to update the screen safely.

`GameState` uses `synchronized` methods and `CopyOnWriteArrayList` so the threads can share it safely.

## OOP concepts used

| Concept | Where |
|---|---|
| Classes and objects | Every tower, enemy, path and the game state is an object |
| Encapsulation | Private fields with getters; `GameState` hands out read-only lists |
| Inheritance | Enemies extend `Enemy`, towers extend `Tower`. `SplittingSlime` extends `Slime`, `SplashCannonTower` extends `CannonTower`, `SpreadMageTower` extends `MageTower` |
| Polymorphism | `Tower.attack()` and `applyUpgradeStats()` differ per tower. Enemies override hooks like `computeIncomingDamage()` (armour), `onTick()` (regeneration), `adjustSlowFactor()` / `adjustSlowDuration()` (slow resistance) and `move()` (Frost Golem enrage) |
| Abstract classes and interfaces | `Enemy`, `Tower` are abstract. `Targetable`, `Slowable` and `GameEventListener` are interfaces |
| Enums | `EnemyType`, `TargetingStrategy`, `GameStatus` |
| Exception handling | Custom checked exceptions for bad placement, not enough gold and max upgrade level; the UI catches them and shows the message |
| Multithreading | Engine and spawner threads, `volatile` flags, `synchronized`, `wait`/`notifyAll`, daemon threads |
| JavaFX and events | `Canvas`, buttons, radio buttons, a combo box, mouse handlers, `AnimationTimer` |

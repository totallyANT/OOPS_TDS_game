# Test Cases and Execution Results

## 1. Test Environment

| Item | Value |
| :--- | :--- |
| Date | 2026-11-10 |
| OS | Windows 11 |
| JDK | Oracle JDK 27 (build 27+35-2325) |
| JavaFX | OpenJFX SDK 27.0.1 |
| Build command | `javac -Xlint:unchecked --module-path <javafx>/lib --add-modules javafx.controls -d build/classes <all main sources>` |
| Compile result | **PASS** — 16 source files, zero errors, zero warnings |
| Engine test harness | **80 passed, 0 failed** |
| JavaFX UI smoke test | **35 passed, 0 failed** (real window, real button clicks) |
| Visual check | Scene snapshot rendered successfully at 1320x880 |

The engine harness exercised model classes, exceptions, placement validation,
upgrades, search, combat ticks, base breaches, wave lifecycle, difficulty, and reset.
The UI harness launched the application, filled the commander name, changed
difficulty, clicked grid cells, toggled upgrade mode, used all three tabs, closed
modal alerts, ran a complete wave, generated a background report, and reset the game.

---

## 2. Main Test Cases

| ID | Category | Preconditions and Steps | Expected Result | Actual Result | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-01** | Normal — build tower | New game (150g). Select `Archer Tower (50g)`, click grass cell `(0,0)`. | One Archer at `(0,0)`, level 1, DMG 20, RNG 2; gold becomes 100g; armory and notifications update. | Archer created; gold 100g; DMG 20 / RNG 2; armory row shown; HUD updated to 100g. | **PASS** |
| **TC-02** | Normal — upgrade tower | Archer at `(0,0)`. Enable upgrade mode and click the tower. | Level 2, DMG 30, gold 70g, next upgrade cost 60g. | Tower became L2, DMG 30, gold 70g, `getUpgradeCost()` returned 60g. | **PASS** |
| **TC-03** | Normal — run wave | Two towers placed. Difficulty Easy. Click **Start Next Wave**. | Wave 1 starts on a background thread; HUD shows RUNNING; goblins and one Armored Ogre spawn and step; one `GameRecord` is added; HUD returns to IDLE; GUI stays responsive. | Wave 1 ran asynchronously, enemies spawned/advanced, exactly one history row appeared with a result, thread status returned to IDLE, UI clicks remained responsive. | **PASS** |
| **TC-04** | Normal — search towers | Towers at `(0,0)` and `(1,0)`. Open Armory. Search `Archer`, then `1,0`, then click **Show All**. | `Archer` returns 2 rows; `1,0` returns 1 row; Show All restores 2 rows. | Results were 2, 1, and 2 respectively. Search was case-insensitive in additional checks. | **PASS** |
| **TC-05** | Normal — background report | At least one completed wave. Open Match History & Reports, click **Generate System & Combat Report (Background Thread)**. | Status shows “Generating report in background…”; UI remains responsive; report appears in `TextArea` with title, commander, tower roster, and wave history; status confirms completion. | Status updated, report arrived after the background delay, contained “TOWER DEFENSE”, “Ada”, “Archer”, and “Wave 1”; completion status shown. | **PASS** |
| **TC-06** | Exceptional — build on path | Select a tower, click amber cell `(2,0)`. | `InvalidPlacementException`: “Row 2 is the enemy path; towers cannot be built there.” Warning `Alert` shown; no tower added; gold unchanged. | Warning dialog displayed the message; tower count stayed 2; gold stayed 20g in the UI test. | **PASS** |
| **TC-07** | Exceptional — occupied cell | Tower at `(0,0)`, upgrade mode off, click `(0,0)` again. | `InvalidPlacementException`: “Cell (0, 0) already contains a tower.” No second tower and no gold deduction. | Exception thrown with the expected message; tower count and gold unchanged. | **PASS** |
| **TC-08** | Exceptional — insufficient gold | Build Cannon (90g), leaving 60g. Attempt Mage (75g). Also attempt a 90g upgrade with only 10g. | `InsufficientGoldException`: build message reports “need 75g but only 60g”; upgrade message reports “costs 90g but only 10g is available.” State unchanged in both cases. | Both exceptions thrown with those messages; no tower added, no level/damage change, no gold deducted. | **PASS** |
| **TC-09** | Exceptional — empty player name | Enter three spaces in the commander field and click **Set Player**. | `InvalidPlacementException`: “Commander name cannot be empty.” Warning `Alert`; previous commander name retained. | Dialog shown and closed; engine name remained “Ada”; engine-level blank/whitespace test also rejected input. | **PASS** |
| **TC-10** | Exceptional — double wave start | While a wave is running, click **Start Next Wave** again. | `InvalidPlacementException`: “A wave is already running. Wait for it to finish.” Warning `Alert`; wave number does not increase; no second thread starts. | Engine check threw the expected exception and wave count did not advance. The UI uses the same catch-and-`Alert` path verified in TC-06/TC-09. | **PASS** |

---

## 3. Additional Checks Executed (80-Check Harness)

These checks are beyond the ten required cases and were all executed in the engine
harness:

| Area | Checks |
| :--- | :--- |
| Initial state | 150 gold, 5 lives, wave 0, score 0, no towers, wave idle. |
| Placement validation | Out-of-bounds `(5,0)` rejected; unknown tower type rejected; failed builds leave gold and tower list unchanged. |
| Tower stats | Archer 50g/20 DMG/2 RNG; Cannon 90g/45 DMG/1 RNG; Mage 75g/25 DMG/2 RNG. |
| Archers | +10 damage per upgrade; cost `30 * level`; L3 = 40 damage; unaffordable third upgrade rejected. |
| Mages | +12 damage per upgrade; L3 range increases from 2 to 3; attacks apply slow. |
| Cannons | +10 bonus damage on every attack; +20 damage per upgrade. |
| Boss | 140 HP, 8 armor: 20 damage becomes 12; 1 damage becomes 5; health never below zero. |
| Goblin | 50 HP, full damage, reaches base after six moves. |
| Slow | Slowed enemy stays in place for one tick and then moves. |
| Range | Archer at `(0,0)` can hit column 0 but a Cannon at `(0,4)` cannot. |
| Combat tick | Towers attack once, dead goblin is removed, +20 gold and +20 score awarded, notification logged, survivor advances. |
| Base breach | Enemy removed at `col > 4`, lives decrease by one, breach notification logged. |
| Wave lifecycle | Wave number increments, flag set, second wave blocked, build/upgrade blocked during wave, record stored, flag cleared. |
| Game over | Five breaches set lives to 0; starting another wave is rejected. |
| Search | Name, ID, coordinates, blank keyword, no-match, case-insensitivity. |
| Difficulty | Valid values accepted; invalid value ignored. |
| Reset | Towers, enemies, records, wave, score restored; gold 150g; lives 5; flag cleared. |
| UI | Window shows; 3 tabs; 5x5 grid; commander update; difficulty update; build; upgrade; warning dialog; search; wave; record; report; reset. |

---

## 4. Requirement Coverage

| Required test area | Covered by |
| :--- | :--- |
| Building a tower | TC-01 |
| Upgrading a tower | TC-02 |
| Running a wave | TC-03 |
| Searching towers | TC-04 |
| Generating a background report | TC-05 |
| Building on the enemy path | TC-06 |
| Building on an occupied tile | TC-07 |
| Building/upgrading with insufficient gold | TC-08 |
| Empty player name | TC-09 |
| Starting a wave while one is active | TC-10 |

All ten required cases passed, along with every additional engine and UI check.

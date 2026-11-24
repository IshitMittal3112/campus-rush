# Campus Rush — Time-Based Campus Strategy Game (JavaFX + JDBC)

Race the clock across a 37-location campus map. Missions open and close in **time windows**, the campus
**changes through the day**, roads carry **hurdle risk**, and the highest score at the end wins.

```
TIME -> MISSION -> ROUTE -> HURDLE -> CHALLENGE -> ARRIVE -> POINTS -> NEXT TIME EVENT
```

## How it plays

* **Simulated clock.** Time only advances when you act (walk a road, wait, fail a hurdle). No threads, fully deterministic.
* **Local hot-seat multiplayer.** Choose one to four players on the start screen. Everyone shares the campus map, chooses a character, and competes for the highest score. The player whose simulated clock is earliest takes the next turn.
* **Missions** (`missions.csv`): be at a location inside a window, e.g. *Library 09:55–10:10*. Three missions open at
  distant places in each time slot, so **you cannot do everything — you must choose**. Arrive early? Press *Wait*.
  Arrive late? 0 points.
* **Routes** (Dijkstra): *Fastest*, *Safest* and an *Alternative*, each with ETA vs. the mission window and hurdle chance.
  Shortcut roads (`SHORTCUT` in `campus_map.csv`) are much faster but have a high hurdle risk.
* **Time of day** (`phases.csv`): Morning (all open) → Afternoon (lunch crowds, +40% hurdle risk) → Evening (library, LT,
  labs locked) → Night (slower paths, sports facilities also closed). The open campus stays connected in every phase.
* **Hurdles**: quiz hurdles from `questions.csv` plus live-generated math / pattern / campus-map questions, and keyboard
  reflex tasks (GUI). Fail = lose time and stay where you are. Pass = cross and score.

### Scoring (all values in `game_config.properties`)

| Action | Points |
|---|---:|
| Complete a mission | its reward (40–70; special missions 70–100) |
| Arrive before the deadline | +20 |
| Solve a hurdle | +10 (Scholar ×2) |
| Cross a shortcut | +5 |
| Miss a deadline | 0 |

### Characters — each differs by ONE number + one ability that really changes the next step

| Class | Passive | Ability (2 charges) |
|---|---|---|
| Sprinter | walks 20% faster | Campus Shortcut: next step half time, no hurdle |
| Scholar | hurdle points ×2 | Time Freeze: next step costs no time |
| Negotiator | 30% fewer hurdles | Diplomatic Bypass: next hurdle auto-solved |

## Run on Windows

Open PowerShell in the folder containing `pom.xml` (for example `OneDrive\Desktop\All`) and launch with:

```powershell
.\run.bat
```

The launcher uses Maven from `PATH`, or looks for a standard extracted Maven folder under your Downloads folder. It runs `clean` before launching so stale class/resource files cannot shadow the current source. You can also use `mvn clean javafx:run` directly. If Maven is not installed, download the binary ZIP from [Apache Maven](https://maven.apache.org/download.cgi), extract it, then use `run.bat` or add its `bin` folder to PATH.

Other launcher options: `run.bat console`, `run.bat test`, and `run.bat sim`.
## Run

```bash
mvn javafx:run                                              # GUI
mvn exec:java -Dexec.mainClass=com.campusrush.console.ConsoleMain   # text version, same engine
mvn exec:java -Dexec.mainClass=com.campusrush.sim.SelfTest          # 31 regression checks
mvn exec:java -Dexec.mainClass=com.campusrush.sim.BotSimulator      # class-balance simulation
```
(`run.sh` / `run.bat` wrap these.) GUI: choose 1–4 players, pick a mission → *Show Route Guide* → choose a route to highlight on the map (if you wander off it, a new route to the same mission is suggested automatically), then **you move the token
yourself**, road by road, with WASD/arrows/1-4 (it never moves on its own). *Wait* passes game time. Java 17+, Maven 3.8+.

## Architecture

`core` (GameEngine, GameClock, StepPlan/StepResult — **no JavaFX**) · `map` (CampusMap graph, PathFinder = Dijkstra,
CampusSchedule = time-of-day rules) · `mission` · `player` · `ability` · `hurdle` · `challenge` · `score` · `db` (SQLite DAOs)
· `ui` (JavaFX) · `console` · `sim`. Because the engine is UI-independent, the GUI, console and bot all drive the same code.

## Syllabus map

Inheritance/polymorphism (`Character`, `Hurdle`, `Ability`, `MultipleChoiceChallenge`) · interfaces (`Ability`, `Challenge`, `Dao<T,ID>`)
· collections/generics/Comparator/Optional/custom Iterator · custom checked exceptions · file I/O (CSV loaders, `.properties`,
binary `.sav` serialization) · `java.util.logging` · JDBC with transactions (`ScoreEventDao.insertBatchWithSessionUpdate`)
· JavaFX scene graph, CSS, animations · plain-Java tests.

## Data-driven content (no code changes needed)

`campus_map.csv` (nodes, roads, shortcuts) · `missions.csv` · `phases.csv` · `questions.csv` · `game_config.properties`.

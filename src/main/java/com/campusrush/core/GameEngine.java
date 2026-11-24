package com.campusrush.core;

import com.campusrush.ability.Ability;
import com.campusrush.ability.AbilityEffect;
import com.campusrush.challenge.CheckpointTask;
import com.campusrush.core.exceptions.*;
import com.campusrush.db.*;
import com.campusrush.hurdle.Hurdle;
import com.campusrush.hurdle.HurdleFactory;
import com.campusrush.map.*;
import com.campusrush.mission.Mission;
import com.campusrush.mission.MissionManager;
import com.campusrush.mission.MissionState;
import com.campusrush.player.Player;
import com.campusrush.score.ScoreBoard;
import com.campusrush.score.ScoreEvent;
import com.campusrush.score.ScoreType;
import com.campusrush.util.AppLogger;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Central, UI-independent game engine (no JavaFX imports - it runs the same under the GUI, the
 * console version and the bot simulator).
 *
 * TIME MODEL
 *  - The clock is simulated: time only moves when a player acts (walk a path, wait, fail a hurdle).
 *  - Every player has their OWN clock over the same mission schedule. The player whose clock is
 *    earliest acts next ("who is furthest behind goes next"), so efficient play is rewarded and
 *    nobody gets extra turns. A player is done when their clock reaches the end of the game.
 *
 * TURN FLOW
 *  planStep() -> (UI shows hurdle, player answers) -> resolveStep() -> missions are checked.
 */
public class GameEngine implements Serializable {
    private static final long serialVersionUID = 2L;

    private final List<Player> players = new ArrayList<>();
    private final CampusMap campusMap;
    private final CampusSchedule schedule;
    private final MissionManager missionManager;
    private final ScoreBoard scoreBoard = new ScoreBoard();
    private final Random rng;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final boolean persist;

    private boolean reflexHurdlesEnabled = true;
    private boolean ended = false;
    private GameState currentState = GameState.MISSION_SELECT;
    private int dbSessionId = 1;
    private long turnCounter = 0;

    /** Normal game: loads phases.csv + missions.csv and records the session in SQLite. */
    public GameEngine(List<Player> players, CampusMap campusMap) {
        this(players, campusMap, CampusSchedule.loadFromCSV("phases.csv"), null, GameConfig.RANDOM_SEED, true);
    }

    /**
     * Full constructor.
     * @param missions null = load missions.csv
     * @param seed     0 = random seed
     * @param persist  false = never touch the database (tests, simulations)
     */
    public GameEngine(List<Player> players, CampusMap campusMap, CampusSchedule schedule,
                      MissionManager missions, long seed, boolean persist) {
        Set<String> names = new HashSet<>();
        for (Player p : players) {
            if (!names.add(p.getName().toLowerCase())) {
                throw new IllegalArgumentException("Player names must be unique: " + p.getName());
            }
        }
        this.players.addAll(players);
        this.campusMap = campusMap;
        this.schedule = schedule;
        this.persist = persist;
        this.rng = seed == 0 ? new Random() : new Random(seed);

        LocalDate today = LocalDate.now();
        this.startTime = LocalDateTime.of(today, GameConfig.START_TIME);
        this.endTime = startTime.plusMinutes(GameConfig.GAME_DURATION_MINUTES);
        this.missionManager = missions != null ? missions
                : MissionManager.loadFromCSV("missions.csv", campusMap, today);

        int nextId = 1;
        for (Player p : this.players) {
            p.setClock(new GameClock(startTime, GameConfig.GAME_DURATION_MINUTES));
            if (!persist) p.setId(nextId++);
        }

        if (persist) registerInDatabase();
        for (String problem : validateMissions()) AppLogger.warning("Mission check: " + problem);
    }

    private void registerInDatabase() {
        try {
            PlayerDao playerDao = new PlayerDao();
            for (Player p : players) playerDao.insert(p);
            dbSessionId = new GameSessionDao().createSession(startTime, GameConfig.GAME_DURATION_MINUTES);
            MissionDao missionDao = new MissionDao();
            for (Mission m : missionManager.getAllMissions()) missionDao.insertForSession(m, dbSessionId);
        } catch (DataAccessException e) {
            AppLogger.severe("Could not initialize DB session: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ turn order

    /** The player who acts next: unfinished player with the earliest clock (least recently acted on ties). */
    public Player getCurrentPlayer() {
        Player best = null;
        for (Player p : players) {
            if (p.isFinished()) continue;
            if (best == null) { best = p; continue; }
            LocalDateTime a = p.getClock().getCurrentGameTime();
            LocalDateTime b = best.getClock().getCurrentGameTime();
            if (a.isBefore(b) || (a.isEqual(b) && p.getLastActedSeq() < best.getLastActedSeq())) best = p;
        }
        return best != null ? best : players.get(0);
    }

    public boolean isGameOver() { return ended; }

    // ------------------------------------------------------------------ time helpers

    public LocalDateTime getCurrentTime() { return getCurrentPlayer().getClock().getCurrentGameTime(); }
    public long getRemainingMinutes() { return getCurrentPlayer().getClock().getRemainingMinutes(); }
    public CampusSchedule.Phase getCurrentPhase() { return schedule.phaseAt(getCurrentTime()); }
    public CampusSchedule getSchedule() { return schedule; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }

    public TravelContext travelContextFor(Player p) {
        return new TravelContext(schedule, p.getClock().getCurrentGameTime(),
                p.getCharacter().getSpeedModifier(), p.getCharacter().getHurdleChanceModifier());
    }

    public void setReflexHurdlesEnabled(boolean enabled) { this.reflexHurdlesEnabled = enabled; }

    // ------------------------------------------------------------------ missions & routes

    /** Marks a mission as the player's target and returns candidate routes (fastest / safest / alternative). */
    public List<Route> selectMissionAndFindRoutes(Mission mission) throws CampusRushException {
        Player player = getCurrentPlayer();
        MissionState state = missionManager.stateFor(player, mission);
        if (state == MissionState.EXPIRED || state == MissionState.COMPLETED) {
            throw new MissionExpiredException("That mission is no longer available (" + state + ").");
        }
        missionManager.setTarget(player, mission);
        currentState = GameState.ROUTE_SELECT;
        return findRoutes(mission.getDestination());
    }

    public List<Route> findRoutes(Location destination) throws InvalidRouteException {
        Player p = getCurrentPlayer();
        return campusMap.findRoutes(p.getCurrentLocation(), destination, travelContextFor(p));
    }

    /**
     * Sanity check of the mission data: can a perfect player starting at the start location
     * reach each mission in time if they head there up to 45 minutes before it opens
     * (closures and time-of-day travel costs included)? Returns human-readable problems.
     */
    public List<String> validateMissions() {
        List<String> problems = new ArrayList<>();
        if (players.isEmpty()) return problems;
        Location start = players.get(0).getCurrentLocation();
        for (Mission m : missionManager.getAllMissions()) {
            LocalDateTime early = m.getAvailableFrom().minusMinutes(45);
            LocalDateTime leave = early.isBefore(startTime) ? startTime : early;
            TravelContext ctx = new TravelContext(schedule, leave, 1.0, 1.0);
            double mins = PathFinder.fastestMinutes(campusMap, start, m.getDestination(), ctx);
            if (mins < 0) {
                problems.add("Mission '" + m.getTitle() + "' cannot be reached (closed or disconnected).");
            } else if (leave.plusSeconds(Math.round(mins * 60)).truncatedTo(java.time.temporal.ChronoUnit.MINUTES)
                    .isAfter(m.getDeadline())) {
                problems.add("Mission '" + m.getTitle() + "' is impossible: needs " + Math.round(mins)
                        + " min but the window is " + m.windowText() + ".");
            }
        }
        return problems;
    }

    // ------------------------------------------------------------------ abilities

    /** Arms the current player's special ability for their next step. */
    public Ability useAbility() throws InsufficientAbilityChargeException {
        Player p = getCurrentPlayer();
        Ability ability = p.getCharacter().getSpecialAbility();
        ability.use(p);
        return ability;
    }

    // ------------------------------------------------------------------ stepping

    public StepPlan planStep(Location next) throws InvalidRouteException {
        return planStepFor(getCurrentPlayer(), next);
    }

    public StepPlan planStepFor(Player p, Location next) throws InvalidRouteException {
        if (ended || p.isFinished()) throw new InvalidRouteException("The game is over for " + p.getName() + ".");
        Location from = p.getCurrentLocation();
        RouteEdge edge = campusMap.getEdge(from, next);
        if (edge == null) {
            throw new InvalidRouteException(next.getName() + " is not directly connected to " + from.getName() + ".");
        }

        LocalDateTime now = p.getClock().getCurrentGameTime();
        TravelContext ctx = travelContextFor(p);
        AbilityEffect effect = p.getArmedEffect();

        double secs = ctx.edgeSeconds(edge, now);
        if (effect == AbilityEffect.HALF_TIME_NO_HURDLE) secs *= 0.5;
        if (effect == AbilityEffect.FREE_STEP) secs = 0;
        long seconds = Math.round(secs);

        String closed = schedule.closedReason(next, now.plusSeconds(seconds));
        if (closed != null) throw new InvalidRouteException(closed);

        Hurdle hurdle = null;
        CheckpointTask task = null;
        boolean auto = false;
        if (effect != AbilityEffect.HALF_TIME_NO_HURDLE && rng.nextDouble() < ctx.hurdleProbability(edge, now)) {
            if (reflexHurdlesEnabled && rng.nextDouble() < GameConfig.REFLEX_HURDLE_CHANCE) {
                task = CheckpointTask.generateRandomTask(from.getName(), next.getName(), rng);
            } else {
                hurdle = HurdleFactory.random(campusMap, rng);
            }
            auto = effect == AbilityEffect.AUTO_SOLVE_HURDLE;
        }
        return new StepPlan(p, from, next, edge, seconds, hurdle, task, auto, effect,
                schedule.phaseAt(now), p.getClock().canAfford(seconds));
    }

    /**
     * Applies a planned step. If the plan needs a challenge, {@code outcome} must say how it went
     * (null is accepted when no challenge was required). A failed hurdle costs its delay and
     * the player STAYS where they are; a passed hurdle lets them cross and scores points.
     */
    public StepResult resolveStep(StepPlan plan, HurdleOutcome outcome) {
        Player p = plan.getPlayer();
        StepResult res = new StepResult();
        if (ended || p.isFinished()) {
            res.addMessage(p.getName() + " has no time left.");
            return res;
        }

        if (!plan.isAffordable()) {
            p.getClock().expire();
            res.setType(StepResult.Type.TIME_UP);
            res.addMessage("Time ran out before " + p.getName() + " could reach " + plan.getTo().getName() + ".");
            afterAction(p, res);
            return res;
        }

        if (plan.requiresChallenge()) {
            if (outcome == null) throw new IllegalArgumentException("A hurdle outcome is required for this step.");
            if (outcome == HurdleOutcome.FAILED) {
                int delay = plan.getFailDelayMinutes();
                p.getClock().advanceMinutes(delay);
                p.addTimePenalty(delay);
                p.incrementHurdlesFailed();
                res.setType(StepResult.Type.BLOCKED);
                res.setMinutesSpent(delay);
                res.addMessage("Failed '" + plan.getHurdleName() + "': " + delay + " min lost, you stay at "
                        + plan.getFrom().getName() + ".");
                AppLogger.info(p.getName() + " failed hurdle " + plan.getHurdleName());
                afterAction(p, res);
                return res;
            }
        }

        // The step succeeds: pay the time, move.
        p.getClock().advanceSeconds(plan.getSeconds());
        p.setCurrentLocation(plan.getTo());
        res.setType(StepResult.Type.MOVED);
        res.setMinutesSpent(plan.getMinutes());

        AbilityEffect eff = plan.getAppliedEffect();
        if (eff == AbilityEffect.HALF_TIME_NO_HURDLE || eff == AbilityEffect.FREE_STEP) {
            p.clearArmedEffect();
            res.addMessage("Ability used up on this step.");
        }

        if (plan.hasHurdle()) {
            int pts = (int) Math.round(GameConfig.HURDLE_SOLVED_POINTS * p.getCharacter().getHurdleBonusMultiplier());
            award(p, ScoreType.HURDLE, pts,
                    (plan.isAutoSolved() ? "Bypassed hurdle: " : "Solved hurdle: ") + plan.getHurdleName(), res);
            p.incrementHurdlesSolved();
            if (plan.isAutoSolved()) {
                p.clearArmedEffect();
                res.addMessage("Diplomatic Bypass cleared the hurdle automatically.");
            }
        }
        if (plan.getEdge().isShortcut()) {
            award(p, ScoreType.SHORTCUT, GameConfig.SHORTCUT_POINTS,
                    "Took shortcut " + plan.getFrom().getName() + " -> " + plan.getTo().getName(), res);
            p.incrementShortcutsTaken();
        }
        afterAction(p, res);
        return res;
    }

    /** Spend time standing still (e.g. arrive early and wait for a mission window to open). */
    public StepResult waitMinutes(int minutes) {
        Player p = getCurrentPlayer();
        StepResult res = new StepResult();
        if (ended || p.isFinished()) return res;
        int m = minutes > 0 ? minutes : GameConfig.WAIT_MINUTES;
        m = (int) Math.min(m, Math.max(1, p.getClock().getRemainingMinutes()));
        p.getClock().advanceMinutes(m);
        res.setType(StepResult.Type.WAITED);
        res.setMinutesSpent(m);
        res.addMessage(p.getName() + " waits " + m + " min at " + p.getCurrentLocation().getName() + ".");
        afterAction(p, res);
        return res;
    }

    /** The current player stops playing and keeps their score (their remaining time is forfeited). */
    public void finishCurrentPlayer() {
        Player p = getCurrentPlayer();
        if (ended || p.isFinished()) return;
        StepResult res = new StepResult();
        p.getClock().expire();
        afterAction(p, res);
    }

    // ------------------------------------------------------------------ bookkeeping

    private void award(Player p, ScoreType type, int points, String reason, StepResult res) {
        ScoreEvent ev = new ScoreEvent(p.getId(), points, reason, p.getClock().getCurrentGameTime(), type);
        scoreBoard.addScoreEvent(p, ev);
        res.addPoints(points);
        res.addMessage((points >= 0 ? "+" : "") + points + " pts: " + reason);
    }

    private void afterAction(Player p, StepResult res) {
        processMissions(p, res);
        p.setLastActedSeq(++turnCounter);
        currentState = GameState.MISSION_SELECT;

        if (p.getClock().isTimeExpired() && !p.isFinished()) {
            p.setFinished(true);
            res.addMessage(p.getName() + "'s day is over.");
            AppLogger.info(p.getName() + " finished with " + p.getScore() + " pts");
        }
        boolean allDone = true;
        for (Player q : players) {
            if (!q.isFinished()) { allDone = false; break; }
        }
        if (allDone) endSession();
    }

    /** Complete every open mission at the player's location, then expire overdue ones. */
    private void processMissions(Player p, StepResult res) {
        LocalDateTime now = p.getClock().getCurrentGameTime();
        for (Mission m : missionManager.getOpenMissionsAt(p, p.getCurrentLocation())) {
            award(p, m.isSpecial() ? ScoreType.SPECIAL_MISSION : ScoreType.MISSION, m.getRewardPoints(),
                    (m.isSpecial() ? "Special mission: " : "Mission: ") + m.getTitle(), res);
            award(p, ScoreType.ON_TIME, GameConfig.ON_TIME_BONUS, "Arrived before the deadline: " + m.getTitle(), res);
            missionManager.completeMission(p, m, now);
            res.addCompleted(m);
            AppLogger.info(p.getName() + " completed " + m.getTitle());
        }
        for (Mission m : missionManager.expireOverdue(p)) {
            scoreBoard.addScoreEvent(p, new ScoreEvent(p.getId(), 0, "Missed deadline: " + m.getTitle(), now, ScoreType.MISSED));
            res.addMissed(m);
            res.addMessage("Missed: " + m.getTitle() + " (0 pts)");
        }
    }

    // ------------------------------------------------------------------ end of game

    public void endSession() {
        if (ended) return;
        ended = true;
        currentState = GameState.END_SESSION;

        // First player (by game time) to complete a mission earns a bonus - only meaningful with 2+ players.
        if (players.size() > 1) {
            for (Mission m : missionManager.getAllMissions()) {
                Player first = null;
                LocalDateTime best = null;
                boolean tie = false;
                for (Player p : players) {
                    LocalDateTime t = p.getCompletionTimes().get(m.getId());
                    if (t == null) continue;
                    if (best == null || t.isBefore(best)) { best = t; first = p; tie = false; }
                    else if (t.isEqual(best)) tie = true;
                }
                if (first != null && !tie) {
                    scoreBoard.addScoreEvent(first, new ScoreEvent(first.getId(), GameConfig.FIRST_ARRIVAL_BONUS,
                            "First to complete: " + m.getTitle(), best, ScoreType.FIRST_ARRIVAL));
                }
            }
        }

        if (!persist) return;
        try {
            new GameSessionDao().endSession(dbSessionId, endTime);
            List<ScoreEvent> allEvents = new ArrayList<>();
            for (Player p : players) allEvents.addAll(scoreBoard.getEventsFor(p));
            new ScoreEventDao().insertBatchWithSessionUpdate(allEvents, dbSessionId);
        } catch (DataAccessException e) {
            AppLogger.severe("Failed to persist final session stats: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ save / resume

    public void saveGameSession(File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(this);
            AppLogger.info("Saved game session to: " + file.getAbsolutePath());
        }
    }

    public static GameEngine loadGameSession(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            GameEngine engine = (GameEngine) ois.readObject();
            AppLogger.info("Loaded saved game session from: " + file.getAbsolutePath());
            return engine;
        }
    }

    // ------------------------------------------------------------------ getters

    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }
    public CampusMap getCampusMap() { return campusMap; }
    public MissionManager getMissionManager() { return missionManager; }
    public ScoreBoard getScoreBoard() { return scoreBoard; }
    public GameState getCurrentState() { return currentState; }
    public int getDbSessionId() { return dbSessionId; }
}

package com.campusrush.sim;

import com.campusrush.ability.AbilityEffect;
import com.campusrush.challenge.*;
import com.campusrush.core.*;
import com.campusrush.core.exceptions.InvalidRouteException;
import com.campusrush.map.*;
import com.campusrush.mission.Mission;
import com.campusrush.player.*;
import com.campusrush.score.ScoreType;
import com.campusrush.util.AppLogger;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Plain-Java regression tests (no JUnit needed):  java -cp target/classes com.campusrush.sim.SelfTest
 * Exits with status 1 if any check fails.
 */
public final class SelfTest {
    private static int passed = 0, failed = 0;

    private static void check(boolean ok, String name) {
        if (ok) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }

    private static CampusMap map() { return MapLoader.loadMapFromCSV("campus_map.csv"); }

    private static GameEngine engine(String... classes) {
        CampusMap m = map();
        List<Player> ps = new ArrayList<>();
        for (int i = 0; i < classes.length; i++) {
            ps.add(new Player(i + 1, "P" + (i + 1), Characters.byName(classes[i]), m.getLocation("MAIN_GATE")));
        }
        GameEngine e = new GameEngine(ps, m, CampusSchedule.loadFromCSV("phases.csv"), null, 42, false);
        e.setReflexHurdlesEnabled(false);
        return e;
    }

    /** Walks a path, answering every hurdle correctly. */
    private static void walk(GameEngine e, String... ids) throws Exception {
        for (String id : ids) {
            StepPlan plan = e.planStep(e.getCampusMap().getLocation(id));
            e.resolveStep(plan, plan.requiresChallenge() ? HurdleOutcome.PASSED : null);
        }
    }

    public static void main(String[] args) throws Exception {
        AppLogger.setQuiet(true);

        System.out.println("Map & pathfinding");
        CampusMap map = map();
        List<Location> locs = new ArrayList<>(map.getAllLocations());
        int n = locs.size();
        double[][] d = new double[n][n];
        for (double[] row : d) Arrays.fill(row, 1e9);
        for (int i = 0; i < n; i++) {
            d[i][i] = 0;
            for (RouteEdge e : map.getEdgesFrom(locs.get(i))) d[i][locs.indexOf(e.getToLocation())] = e.getDistanceMinutes();
        }
        for (int k = 0; k < n; k++) for (int i = 0; i < n; i++) for (int j = 0; j < n; j++)
            if (d[i][k] + d[k][j] < d[i][j]) d[i][j] = d[i][k] + d[k][j];
        boolean dijkstraOk = true;
        TravelContext neutral = TravelContext.neutral();
        for (int i = 0; i < n && dijkstraOk; i++) for (int j = 0; j < n; j++) {
            if (i == j) continue;
            Route r = PathFinder.fastest(map, locs.get(i), locs.get(j), neutral);
            if (r == null || Math.abs(r.getEstimatedMinutes() - d[i][j]) > 1e-6) { dijkstraOk = false; break; }
        }
        check(dijkstraOk, "Dijkstra fastest route equals Floyd-Warshall for all " + n * (n - 1) + " pairs");

        Location lib = map.getLocation("LIBRARY"), canteen = map.getLocation("CANTEEN");
        List<Route> routes = map.findRoutes(lib, canteen);
        check(routes.size() >= 2, "Library -> Canteen offers at least 2 routes (" + routes.size() + ")");
        boolean distinct = true;
        for (int i = 0; i < routes.size(); i++) for (int j = i + 1; j < routes.size(); j++)
            if (routes.get(i).samePathAs(routes.get(j))) distinct = false;
        check(distinct, "candidate routes are distinct paths");
        Route fastest = routes.get(0);
        boolean fastestOk = true;
        for (Route r : routes) if (r.getEstimatedMinutes() < fastest.getEstimatedMinutes() - 1e-9) fastestOk = false;
        check(fastestOk, "first route is the fastest");
        check(fastest.usesShortcut() && fastest.getHurdleProbability() > 0.3, "fastest Library->Canteen uses a risky shortcut");

        CampusSchedule sched = CampusSchedule.loadFromCSV("phases.csv");
        boolean connected = true;
        for (CampusSchedule.Phase ph : sched.getPhases()) {
            Set<String> seen = new HashSet<>();
            Deque<Location> stack = new ArrayDeque<>();
            Location root = null;
            int open = 0;
            for (Location l : locs) if (!ph.isClosed(l)) { open++; if (root == null) root = l; }
            stack.push(root); seen.add(root.getId());
            while (!stack.isEmpty()) {
                Location u = stack.pop();
                for (RouteEdge e : map.getEdgesFrom(u))
                    if (!ph.isClosed(e.getToLocation()) && seen.add(e.getToLocation().getId())) stack.push(e.getToLocation());
            }
            if (seen.size() != open) connected = false;
        }
        check(connected, "open campus stays connected in every time-of-day phase");

        System.out.println("Time-of-day rules");
        GameEngine e = engine("Scholar");
        Player p = e.getCurrentPlayer();
        check(e.validateMissions().isEmpty(), "every mission is reachable by a perfect player: " + e.validateMissions());
        p.setCurrentLocation(map.getLocation("MECHANICAL_WORKSHOP"));
        p.getClock().advanceMinutes(5 * 60 + 10);          // 14:10 -> EVENING
        check(e.getCurrentPhase().getName().equals("EVENING"), "clock reaches EVENING at 14:00");
        boolean closedBlocked = false;
        try { e.planStep(map.getLocation("LIBRARY")); } catch (InvalidRouteException ex) { closedBlocked = ex.getMessage().contains("closed"); }
        check(closedBlocked, "Library cannot be entered in the evening");

        System.out.println("Missions, clock & scoring");
        e = engine("Scholar");
        p = e.getCurrentPlayer();
        p.getClock().advanceMinutes(40);                                   // 09:40 (slot-1 missions are gone)
        walk(e, "TSLAS_GATE", "MECHANICAL_WORKSHOP", "LIBRARY");           // 10 min -> 09:50, library window opens 09:55
        check(p.getCompletedMissionsCount() == 0, "arriving before the window opens does not complete the mission");
        e.waitMinutes(5);
        Mission csMission = e.getMissionManager().getAllMissions().stream().filter(m -> m.getTitle().equals("Return Reference Book")).findFirst().get();
        check(p.getCompletedMissionsCount() >= 1 && p.getResolvedMissions().containsKey(csMission.getId()),
                "waiting until the window opens completes the mission");
        Map<ScoreType, Integer> bd = e.getScoreBoard().getBreakdown(p);
        check(bd.getOrDefault(ScoreType.MISSION, 0) == csMission.getRewardPoints() && bd.getOrDefault(ScoreType.ON_TIME, 0) == GameConfig.ON_TIME_BONUS,
                "score = mission reward + on-time bonus " + bd);
        check(p.getScore() == bd.values().stream().mapToInt(Integer::intValue).sum(), "player score equals sum of score events");

        e = engine("Scholar");
        p = e.getCurrentPlayer();
        p.getClock().advanceMinutes(60);
        StepResult late = e.waitMinutes(1);
        check(!late.getMissed().isEmpty() && p.getScore() == 0, "missing a deadline scores 0 and is recorded");

        System.out.println("Hurdles");
        e = engine("Negotiator");
        p = e.getCurrentPlayer();
        p.setCurrentLocation(map.getLocation("LIBRARY"));
        Location target = map.getLocation("STREET_CAFE");     // risky shortcut edge
        StepPlan hp = null;
        for (int i = 0; i < 500 && (hp == null || !hp.requiresChallenge()); i++) hp = e.planStep(target);
        check(hp != null && hp.requiresChallenge(), "risky shortcut eventually triggers a hurdle");
        LocalDateTime before = p.getClock().getCurrentGameTime();
        StepResult failedRes = e.resolveStep(hp, HurdleOutcome.FAILED);
        check(failedRes.blocked() && p.getCurrentLocation().getId().equals("LIBRARY"), "failed hurdle blocks movement");
        check(p.getClock().getCurrentGameTime().isEqual(before.plusMinutes(hp.getFailDelayMinutes())), "failed hurdle costs its delay in game time");
        check(p.getTotalTimePenalties() == hp.getFailDelayMinutes() && p.getHurdlesFailed() == 1, "penalty counters updated");

        e = engine("Scholar");
        p = e.getCurrentPlayer();
        p.setCurrentLocation(map.getLocation("LIBRARY"));
        hp = null;
        for (int i = 0; i < 500 && (hp == null || !hp.requiresChallenge()); i++) hp = e.planStep(target);
        e.resolveStep(hp, HurdleOutcome.PASSED);
        Map<ScoreType, Integer> hbd = e.getScoreBoard().getBreakdown(p);
        check(hbd.getOrDefault(ScoreType.HURDLE, 0) == (int) Math.round(GameConfig.HURDLE_SOLVED_POINTS * GameConfig.SCHOLAR_HURDLE_BONUS)
                && hbd.getOrDefault(ScoreType.SHORTCUT, 0) == GameConfig.SHORTCUT_POINTS, "Scholar hurdle bonus + shortcut points awarded " + hbd);

        System.out.println("Abilities");
        e = engine("Sprinter");
        p = e.getCurrentPlayer();
        p.setCurrentLocation(map.getLocation("LIBRARY"));
        long base = e.planStep(map.getLocation("LT")).getSeconds();
        e.useAbility();
        boolean half = true;
        for (int i = 0; i < 100; i++) {
            StepPlan sp = e.planStep(map.getLocation("LT"));
            if (sp.hasHurdle() || sp.getSeconds() != Math.round(base / 2.0)) half = false;
        }
        check(p.getArmedEffect() == AbilityEffect.HALF_TIME_NO_HURDLE && half, "Campus Shortcut halves time and removes hurdles");
        e.resolveStep(e.planStep(map.getLocation("LT")), null);
        check(p.getArmedEffect() == null && p.getAbilityCharges() == GameConfig.ABILITY_CHARGES - 1, "ability is consumed after the step");

        e = engine("Scholar");
        p = e.getCurrentPlayer();
        e.useAbility();
        check(e.planStep(map.getLocation("MAIN_AUDI")).getSeconds() == 0, "Time Freeze makes the next step free");

        System.out.println("Turn order (own clocks)");
        e = engine("Sprinter", "Scholar");
        Player a = e.getPlayers().get(0), b = e.getPlayers().get(1);
        check(e.getCurrentPlayer() == a, "first player starts");
        e.waitMinutes(5);
        check(e.getCurrentPlayer() == b, "player furthest behind in time acts next");

        System.out.println("Challenges & data");
        Random rng = new Random(1);
        boolean oneCorrect = true;
        for (int i = 0; i < 300; i++) {
            Challenge[] cs = {new MathChallenge(rng), new PatternSequenceChallenge(rng), new CampusTriviaChallenge(map, rng), TriviaChallenge.fromCategory("JAVA", rng)};
            for (Challenge c : cs) {
                int correct = 0;
                for (String o : c.getOptions()) if (c.validateAnswer(o)) correct++;
                if (c.getOptions().size() != 4 || correct != 1 || !c.validateAnswer(c.getCorrectAnswerDisplay())) oneCorrect = false;
            }
        }
        check(oneCorrect, "every challenge has 4 options and exactly one correct answer");
        check(QuestionBank.size("JAVA") >= 10 && QuestionBank.size("LOGIC") >= 5 && QuestionBank.size("CAMPUS") >= 3, "question bank loaded from questions.csv");

        System.out.println("Save / load");
        e = engine("Sprinter", "Negotiator");
        e.waitMinutes(5);
        File tmp = File.createTempFile("campusrush", ".sav");
        e.saveGameSession(tmp);
        GameEngine loaded = GameEngine.loadGameSession(tmp);
        tmp.delete();
        check(loaded.getCurrentPlayer().getName().equals(e.getCurrentPlayer().getName())
                && loaded.getCurrentTime().isEqual(e.getCurrentTime()), "engine survives serialization round-trip");

        System.out.println("Full games");
        GameEngine g1 = BotSimulator.playGame(Arrays.asList("Sprinter", "Scholar", "Negotiator"), 7, 0.7);
        GameEngine g2 = BotSimulator.playGame(Arrays.asList("Sprinter", "Scholar", "Negotiator"), 7, 0.7);
        boolean same = true;
        for (int i = 0; i < 3; i++) if (g1.getPlayers().get(i).getScore() != g2.getPlayers().get(i).getScore()) same = false;
        check(g1.isGameOver() && same, "seeded games finish and are reproducible");
        check(g1.getPlayers().stream().allMatch(Player::isFinished), "all players reach the end of the day");

        Map<String, Double> avg = BotSimulator.averageScores(Collections.singletonList("Sprinter"), 60, 0.7);
        double sp = avg.get("Sprinter");
        double sc = BotSimulator.averageScores(Collections.singletonList("Scholar"), 60, 0.7).get("Scholar");
        double ne = BotSimulator.averageScores(Collections.singletonList("Negotiator"), 60, 0.7).get("Negotiator");
        double max = Math.max(sp, Math.max(sc, ne)), min = Math.min(sp, Math.min(sc, ne));
        check(max / min < 1.12, String.format("class balance: Sprinter %.0f / Scholar %.0f / Negotiator %.0f (spread %.1f%%)", sp, sc, ne, (max / min - 1) * 100));
        check(g1.getPlayers().get(0).getCompletedMissionsCount() < g1.getMissionManager().getAllMissions().size(), "no one can complete every mission (real choices)");

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}

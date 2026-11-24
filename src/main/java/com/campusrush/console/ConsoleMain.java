package com.campusrush.console;

import com.campusrush.core.*;
import com.campusrush.core.exceptions.CampusRushException;
import com.campusrush.db.DatabaseManager;
import com.campusrush.challenge.Challenge;
import com.campusrush.hurdle.Hurdle;
import com.campusrush.map.*;
import com.campusrush.mission.Mission;
import com.campusrush.mission.MissionState;
import com.campusrush.player.*;
import com.campusrush.sim.BotSimulator;
import com.campusrush.util.AppLogger;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Text version of Campus Rush on the same engine as the GUI.
 *   mvn exec:java -Dexec.mainClass=com.campusrush.console.ConsoleMain
 *   ... ConsoleMain --sim [games] [accuracy]   (bot balance simulation)
 */
public class ConsoleMain {
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private final Scanner in = new Scanner(System.in);
    private GameEngine engine;
    private boolean quit = false;

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--sim")) {
            BotSimulator.main(Arrays.copyOfRange(args, 1, args.length));
            return;
        }
        new ConsoleMain().run();
    }

    private String ask(String prompt) {
        System.out.print(prompt);
        if (!in.hasNextLine()) { quit = true; return ""; }
        return in.nextLine().trim();
    }

    private void run() {
        AppLogger.setQuiet(true);
        System.out.println("=== CAMPUS RUSH (console) ===");
        boolean persist = true;
        try { DatabaseManager.initializeDatabase(); } catch (Exception e) { persist = false; }

        CampusMap map = MapLoader.loadMapFromCSV("campus_map.csv");
        Location start = map.getLocation("MAIN_GATE");
        List<Player> players = new ArrayList<>();
        String typed = ask("Your name: ");
        String name = typed.isEmpty() ? "Player" : typed;
        String cls = ask("Class 1) Sprinter  2) Scholar  3) Negotiator [1]: ");
        int idx = cls.matches("[123]") ? Integer.parseInt(cls) - 1 : 0;
        players.add(new Player(1, name, Characters.byName(Characters.NAMES[idx]), start));
        if (quit) return;
        engine = new GameEngine(players, map, CampusSchedule.loadFromCSV("phases.csv"), null, GameConfig.RANDOM_SEED, persist);
        engine.setReflexHurdlesEnabled(false);
        System.out.println("Type 'help' for commands.");

        while (!engine.isGameOver() && !quit) {
            showStatus();
            handle(ask("> "));
        }
        if (engine.isGameOver()) showResults();
    }

    private void showStatus() {
        Player p = engine.getCurrentPlayer();
        CampusSchedule.Phase ph = engine.getCurrentPhase();
        System.out.println("\n--------------------------------------------------");
        System.out.printf("%s (%s) | %s | %s phase | %d min left | score %d | charges %d%s%n",
                p.getName(), p.getCharacter().getName(), engine.getCurrentTime().format(HM), ph.getName(),
                p.getClock().getRemainingMinutes(), p.getScore(), p.getAbilityCharges(),
                p.getArmedEffect() != null ? " | ARMED: " + p.getArmedEffect() : "");
        System.out.println("Phase: " + ph.getDescription());
        System.out.println("At: " + p.getCurrentLocation().getName());
        List<RouteEdge> edges = engine.getCampusMap().getEdgesFrom(p.getCurrentLocation());
        for (int i = 0; i < edges.size(); i++) {
            RouteEdge e = edges.get(i);
            boolean closed = !engine.getSchedule().isOpen(e.getToLocation(), engine.getCurrentTime());
            System.out.printf("  go %d : %s (%d min, risk %d%%)%s%s%n", i + 1, e.getToLocation().getName(), e.getDistanceMinutes(),
                    Math.round(e.getHurdleChance() * 100), e.isShortcut() ? " [SHORTCUT]" : "", closed ? " [CLOSED]" : "");
        }
        System.out.println("Missions:");
        printMissions(p);
    }

    private void printMissions(Player p) {
        for (Mission m : engine.getMissionManager().getVisibleMissions(p, 4)) {
            MissionState s = engine.getMissionManager().stateFor(p, m);
            System.out.printf("  #%-2d %-8s %s%n", m.getId(), s == MissionState.LOCKED ? "(soon)" : "OPEN", m);
        }
    }

    private void handle(String line) {
        if (line.isEmpty()) return;
        String[] t = line.split("\\s+");
        Player p = engine.getCurrentPlayer();
        try {
            switch (t[0].toLowerCase()) {
                case "help":
                    System.out.println("go <n> | route <missionId> (shows route guides) | wait [min] | ability | missions | score | finish | quit");
                    break;
                case "go": {
                    List<RouteEdge> edges = engine.getCampusMap().getEdgesFrom(p.getCurrentLocation());
                    int n = Integer.parseInt(t[1]) - 1;
                    step(edges.get(n).getToLocation());
                    break;
                }
                case "route": routeCommand(Integer.parseInt(t[1])); break;
                case "wait": printResult(engine.waitMinutes(t.length > 1 ? Integer.parseInt(t[1]) : 0)); break;
                case "ability": System.out.println("Armed: " + engine.useAbility().getDescription()); break;
                case "missions": printMissions(p); break;
                case "score": showScores(); break;
                case "finish": engine.finishCurrentPlayer(); break;
                case "quit": quit = true; break;
                default: System.out.println("Unknown command. Type 'help'.");
            }
        } catch (CampusRushException e) {
            System.out.println("! " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("! Could not understand that (" + e.getClass().getSimpleName() + "). Type 'help'.");
        }
    }

    private void routeCommand(int missionId) throws CampusRushException {
        Mission m = engine.getMissionManager().findById(missionId).orElseThrow(() -> new CampusRushException("No such mission."));
        List<Route> routes = engine.selectMissionAndFindRoutes(m);
        System.out.println("Routes to " + m.getDestination().getName() + " (window " + m.windowText() + "):");
        for (int i = 0; i < routes.size(); i++) System.out.println("  " + (i + 1) + ") " + routes.get(i));
        System.out.println("(Routes are only a guide - move yourself with 'go <n>' one road at a time.)");
    }

    /** Plans and resolves one step; returns false if the walk should stop (blocked, error). */
    private boolean step(Location next) throws CampusRushException {
        StepPlan plan = engine.planStep(next);
        HurdleOutcome outcome = null;
        if (plan.requiresChallenge()) {
            Hurdle h = plan.getHurdle();
            System.out.println("\n!! HURDLE: " + h.getName() + " - " + h.getDescription());
            Challenge c = h.getChallenge();
            System.out.println(c.getQuestionText());
            for (int i = 0; i < c.getOptions().size(); i++) System.out.println("   " + (i + 1) + ") " + c.getOptions().get(i));
            outcome = c.validateAnswer(ask("Answer: ")) ? HurdleOutcome.PASSED : HurdleOutcome.FAILED;
            if (outcome == HurdleOutcome.FAILED) System.out.println("Wrong! Correct answer: " + c.getCorrectAnswerDisplay());
        }
        StepResult r = engine.resolveStep(plan, outcome);
        printResult(r);
        return r.moved();
    }

    private void printResult(StepResult r) {
        for (String m : r.getMessages()) System.out.println("   " + m);
        for (Mission m : r.getCompleted()) System.out.println("   *** MISSION COMPLETE: " + m.getTitle() + " ***");
    }

    private void showScores() {
        for (Player q : engine.getScoreBoard().getLeaderboard(engine.getPlayers())) {
            System.out.printf("  %-12s %5d pts | %d missions | %s%n", q.getName(), q.getScore(), q.getCompletedMissionsCount(),
                    q.isFinished() ? "finished" : q.getClock().getCurrentGameTime().format(HM));
        }
    }

    private void showResults() {
        System.out.println("\n=== FINAL RESULTS ===");
        showScores();
        System.out.println("Winner: " + engine.getScoreBoard().getLeaderboard(engine.getPlayers()).get(0).getName());
    }
}

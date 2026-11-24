package com.campusrush.sim;

import com.campusrush.core.*;
import com.campusrush.core.exceptions.InsufficientAbilityChargeException;
import com.campusrush.core.exceptions.InvalidRouteException;
import com.campusrush.map.*;
import com.campusrush.mission.Mission;
import com.campusrush.player.Characters;
import com.campusrush.player.Player;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Greedy bot that plays full games through the SAME engine API the GUI and console use.
 * Used for (a) balance testing - do all three character classes score comparably? - and
 * (b) regression tests. Every bot answers hurdles correctly with the same probability, so any
 * score difference between classes comes from the classes themselves, not from player skill.
 */
public final class BotSimulator {
    private BotSimulator() {}

    /** Plays one complete game and returns the finished engine. */
    public static GameEngine playGame(List<String> characters, long seed, double accuracy) {
        CampusMap map = MapLoader.loadMapFromCSV("campus_map.csv");
        Location start = map.getLocation("MAIN_GATE");
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < characters.size(); i++) {
            players.add(new Player(i + 1, "Bot" + (i + 1) + "-" + characters.get(i),
                    Characters.byName(characters.get(i)), start));
        }
        GameEngine engine = new GameEngine(players, map, CampusSchedule.loadFromCSV("phases.csv"), null, seed, false);
        engine.setReflexHurdlesEnabled(false);
        Random rng = new Random(seed * 31 + 7);

        int guard = 0;
        while (!engine.isGameOver() && guard++ < 200000) {
            act(engine, engine.getCurrentPlayer(), rng, accuracy);
        }
        return engine;
    }

    /** One bot decision = one step, one wait, or finishing. */
    static void act(GameEngine engine, Player p, Random rng, double accuracy) {
        LocalDateTime now = p.getClock().getCurrentGameTime();
        TravelContext ctx = engine.travelContextFor(p);
        double bestRate = 0;
        Route bestRoute = null;
        Mission bestMission = null;
        double bestWait = 0;

        List<Mission> candidates = new ArrayList<>(engine.getMissionManager().getOpenMissions(p));
        for (Mission m : engine.getMissionManager().getUpcomingMissions(p)) {
            if (m.getAvailableFrom().isBefore(now.plusMinutes(60))) candidates.add(m);
        }
        for (Mission m : candidates) {
            double eta;
            Route route = null;
            if (m.getDestination().equals(p.getCurrentLocation())) {
                eta = 0;
            } else {
                route = PathFinder.fastest(engine.getCampusMap(), p.getCurrentLocation(), m.getDestination(), ctx);
                if (route == null) continue;
                eta = route.getEstimatedMinutes() + route.getExpectedHurdles() * (1 - accuracy) * 4.0;
            }
            LocalDateTime arrive = now.plusSeconds(Math.round(eta * 60));
            LocalDateTime done = arrive.isBefore(m.getAvailableFrom()) ? m.getAvailableFrom() : arrive;
            if (m.isPastDeadline(done) || !engine.getSchedule().isOpen(m.getDestination(), done)) continue;
            double total = Math.max(1.0, ChronoUnit.SECONDS.between(now, done) / 60.0);
            double rate = (m.getRewardPoints() + com.campusrush.core.GameConfig.ON_TIME_BONUS) / total;
            if (rate > bestRate) {
                bestRate = rate;
                bestRoute = route;
                bestMission = m;
                bestWait = ChronoUnit.SECONDS.between(arrive, done) / 60.0;
            }
        }

        if (bestMission == null) {
            boolean laterMissions = !engine.getMissionManager().getUpcomingMissions(p).isEmpty();
            if (laterMissions && p.getClock().getRemainingMinutes() > 10) {
                engine.waitMinutes(GameConfig.WAIT_MINUTES);
            } else {
                engine.finishCurrentPlayer();
            }
            return;
        }

        if (bestRoute == null) {   // already at the destination, window not yet open
            long mins = Math.max(1, (long) Math.ceil(ChronoUnit.SECONDS.between(now, bestMission.getAvailableFrom()) / 60.0));
            engine.waitMinutes((int) mins);
            return;
        }

        Location next = bestRoute.getPathNodes().get(1);
        try {
            // Use the special ability when time is tight or the next path is risky.
            double slackMin = ChronoUnit.SECONDS.between(now.plusSeconds(Math.round(bestRoute.getEstimatedMinutes() * 60)),
                    bestMission.getDeadline().plusMinutes(1)) / 60.0;
            RouteEdge edge = engine.getCampusMap().getEdge(p.getCurrentLocation(), next);
            if (p.getAbilityCharges() > 0 && p.getArmedEffect() == null
                    && (slackMin <= 4 || ctx.hurdleProbability(edge, now) >= 0.3)) {
                try { engine.useAbility(); } catch (InsufficientAbilityChargeException ignored) { }
            }
            StepPlan plan = engine.planStepFor(p, next);
            HurdleOutcome outcome = null;
            if (plan.requiresChallenge()) {
                outcome = rng.nextDouble() < accuracy ? HurdleOutcome.PASSED : HurdleOutcome.FAILED;
            }
            engine.resolveStep(plan, outcome);
        } catch (InvalidRouteException e) {
            engine.waitMinutes(1);   // e.g. destination just closed - lose a minute and re-plan
        }
    }

    /** Average final score per character class over many games with the given line-up. */
    public static Map<String, Double> averageScores(List<String> lineup, int games, double accuracy) {
        Map<String, Double> sum = new LinkedHashMap<>();
        Map<String, Integer> count = new HashMap<>();
        for (int g = 1; g <= games; g++) {
            GameEngine e = playGame(lineup, 1000L + g, accuracy);
            for (Player p : e.getPlayers()) {
                String cls = p.getCharacter().getName();
                sum.merge(cls, (double) p.getScore(), Double::sum);
                count.merge(cls, 1, Integer::sum);
            }
        }
        Map<String, Double> avg = new LinkedHashMap<>();
        for (String cls : sum.keySet()) avg.put(cls, sum.get(cls) / count.get(cls));
        return avg;
    }

    public static void main(String[] args) {
        com.campusrush.util.AppLogger.setQuiet(true);
        int games = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        double acc = args.length > 1 ? Double.parseDouble(args[1]) : 0.7;
        System.out.printf("Balance simulation: %d games per scenario, hurdle accuracy %.0f%%%n", games, acc * 100);
        for (String cls : new String[]{"Sprinter", "Scholar", "Negotiator"}) {
            System.out.printf("  solo %-10s -> avg score %.1f%n", cls, averageScores(Collections.singletonList(cls), games, acc).get(cls));
        }
        Map<String, Double> three = averageScores(Arrays.asList("Sprinter", "Scholar", "Negotiator"), games, acc);
        System.out.println("  3-player mix     -> " + three);
    }
}

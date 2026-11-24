package com.campusrush.map;

import com.campusrush.core.exceptions.InvalidRouteException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Time-aware shortest-path search (Dijkstra with a PriorityQueue).
 *
 * Replaces the old depth-first enumeration, which could return routes that were not actually
 * the fastest. Edge costs depend on the time of day (travel multipliers), closed locations
 * cannot be entered, and each route is priced both in minutes and in expected hurdle delay.
 *
 *  - FASTEST: minimises travel time only.
 *  - SAFEST : minimises travel time + expected hurdle delay (risk priced in minutes).
 *  - ALTERNATIVE: a genuinely different path found by penalising edges of the earlier routes.
 */
public final class PathFinder {
    private PathFinder() {}

    private enum Mode { FASTEST, SAFEST }

    public static Route fastest(CampusMap map, Location start, Location end, TravelContext ctx) {
        return dijkstra(map, start, end, ctx, Mode.FASTEST, Collections.emptyMap(), "Fastest");
    }

    public static Route safest(CampusMap map, Location start, Location end, TravelContext ctx) {
        return dijkstra(map, start, end, ctx, Mode.SAFEST, Collections.emptyMap(), "Safest");
    }

    /** Fastest possible travel time in minutes, or -1 when the target cannot be reached. */
    public static double fastestMinutes(CampusMap map, Location start, Location end, TravelContext ctx) {
        if (start.equals(end)) return 0;
        Route r = fastest(map, start, end, ctx);
        return r == null ? -1 : r.getEstimatedMinutes();
    }

    /** Up to three distinct candidate routes: fastest, safest, and an alternative. */
    public static List<Route> findRoutes(CampusMap map, Location start, Location end, TravelContext ctx)
            throws InvalidRouteException {
        if (start == null || end == null) {
            throw new InvalidRouteException("Start or End location cannot be null.");
        }
        if (map.getLocation(start.getId()) == null || map.getLocation(end.getId()) == null) {
            throw new InvalidRouteException("Start or End location does not exist in campus map.");
        }
        if (start.equals(end)) {
            return Collections.singletonList(new Route(Collections.singletonList(start), Collections.emptyList(),
                    "Already here", 0, 0, 0));
        }
        Route fastest = fastest(map, start, end, ctx);
        if (fastest == null) {
            String why = ctx.getSchedule().closedReason(end, ctx.getDeparture());
            throw new InvalidRouteException(why != null ? why
                    : "No valid route found between " + start.getName() + " and " + end.getName());
        }
        List<Route> result = new ArrayList<>();
        Route safest = safest(map, start, end, ctx);
        if (safest != null && safest.samePathAs(fastest)) {
            result.add(fastest.withLabel("Fastest & Safest"));
        } else {
            result.add(fastest);
            if (safest != null) result.add(safest);
        }

        // Alternative: penalise edges already used, then keep the first genuinely new path.
        Map<String, Double> penalty = new HashMap<>();
        for (Route r : result) addPenalty(penalty, r, 1.8);
        for (int attempt = 0; attempt < 4 && result.size() < 3; attempt++) {
            Route alt = dijkstra(map, start, end, ctx, Mode.SAFEST, penalty, "Alternative");
            if (alt == null) break;
            boolean duplicate = false;
            for (Route r : result) {
                if (r.samePathAs(alt)) duplicate = true;
            }
            if (!duplicate && alt.getEstimatedMinutes() <= fastest.getEstimatedMinutes() * 2.5 + 10) {
                result.add(alt);
                break;
            }
            addPenalty(penalty, alt, 1.8);
        }
        return result;
    }

    private static void addPenalty(Map<String, Double> penalty, Route route, double factor) {
        for (RouteEdge e : route.getEdges()) {
            penalty.merge(edgeKey(e.getFromLocation(), e.getToLocation()), factor, (a, b) -> a * b);
        }
    }

    private static String edgeKey(Location a, Location b) {
        return a.getId().compareTo(b.getId()) < 0 ? a.getId() + "|" + b.getId() : b.getId() + "|" + a.getId();
    }

    private static final class QueueEntry {
        final Location node;
        final double weight;
        QueueEntry(Location node, double weight) { this.node = node; this.weight = weight; }
    }

    private static Route dijkstra(CampusMap map, Location start, Location end, TravelContext ctx,
                                  Mode mode, Map<String, Double> penalty, String label) {
        Map<String, Double> weight = new HashMap<>();
        Map<String, Double> travelSeconds = new HashMap<>();
        Map<String, RouteEdge> cameBy = new HashMap<>();
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(Comparator.comparingDouble(q -> q.weight));

        weight.put(start.getId(), 0.0);
        travelSeconds.put(start.getId(), 0.0);
        queue.add(new QueueEntry(start, 0.0));

        while (!queue.isEmpty()) {
            QueueEntry cur = queue.poll();
            Location u = cur.node;
            if (cur.weight > weight.getOrDefault(u.getId(), Double.MAX_VALUE)) continue;
            if (u.equals(end)) break;

            LocalDateTime depart = ctx.getDeparture().plusSeconds(Math.round(travelSeconds.get(u.getId())));
            for (RouteEdge e : map.getEdgesFrom(u)) {
                Location v = e.getToLocation();
                double secs = ctx.edgeSeconds(e, depart);
                LocalDateTime arrive = depart.plusSeconds(Math.round(secs));
                if (!ctx.getSchedule().isOpen(v, arrive)) continue; // closed: cannot enter

                double cost = secs;
                if (mode == Mode.SAFEST) {
                    cost += ctx.hurdleProbability(e, depart) * ctx.expectedDelaySeconds();
                }
                cost *= penalty.getOrDefault(edgeKey(u, v), 1.0);

                double newWeight = cur.weight + cost;
                if (newWeight < weight.getOrDefault(v.getId(), Double.MAX_VALUE)) {
                    weight.put(v.getId(), newWeight);
                    travelSeconds.put(v.getId(), travelSeconds.get(u.getId()) + secs);
                    cameBy.put(v.getId(), e);
                    queue.add(new QueueEntry(v, newWeight));
                }
            }
        }

        if (!cameBy.containsKey(end.getId())) return null;

        LinkedList<RouteEdge> edges = new LinkedList<>();
        Location at = end;
        while (!at.equals(start)) {
            RouteEdge e = cameBy.get(at.getId());
            edges.addFirst(e);
            at = e.getFromLocation();
        }
        return buildRoute(edges, ctx, label);
    }

    /** Prices a fixed list of edges (walked in order) under the given travel context. */
    public static Route buildRoute(List<RouteEdge> edges, TravelContext ctx, String label) {
        List<Location> nodes = new ArrayList<>();
        double seconds = 0;
        double noHurdle = 1.0;
        double expected = 0;
        for (RouteEdge e : edges) {
            if (nodes.isEmpty()) nodes.add(e.getFromLocation());
            LocalDateTime depart = ctx.getDeparture().plusSeconds(Math.round(seconds));
            seconds += ctx.edgeSeconds(e, depart);
            double p = ctx.hurdleProbability(e, depart);
            noHurdle *= (1.0 - p);
            expected += p;
            nodes.add(e.getToLocation());
        }
        return new Route(nodes, edges, label, seconds / 60.0, 1.0 - noHurdle, expected);
    }
}

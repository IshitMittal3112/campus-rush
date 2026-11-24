package com.campusrush.map;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A complete path across campus nodes and edges. Implements Iterable&lt;Location&gt;.
 * Besides the raw map distance it carries the time-aware estimate produced by the pathfinder
 * (phase multipliers and character speed already applied).
 */
public class Route implements Iterable<Location>, Serializable {
    private static final long serialVersionUID = 2L;

    private final List<Location> pathNodes;
    private final List<RouteEdge> edges;
    private final int totalDistanceMinutes;
    private final double averageHurdleRisk;

    private final String label;
    private final double estimatedMinutes;
    private final double hurdleProbability;   // P(at least one hurdle on the route)
    private final double expectedHurdles;     // expected number of hurdles

    public Route(List<Location> pathNodes, List<RouteEdge> edges) {
        this(pathNodes, edges, "Route", Double.NaN, Double.NaN, Double.NaN);
    }

    public Route(List<Location> pathNodes, List<RouteEdge> edges, String label,
                 double estimatedMinutes, double hurdleProbability, double expectedHurdles) {
        this.pathNodes = new ArrayList<>(pathNodes);
        this.edges = new ArrayList<>(edges);
        this.label = label;

        int dist = 0;
        double riskSum = 0.0;
        double noHurdle = 1.0;
        double expected = 0.0;
        for (RouteEdge edge : edges) {
            dist += edge.getDistanceMinutes();
            riskSum += edge.getHurdleChance();
            noHurdle *= (1.0 - edge.getHurdleChance());
            expected += edge.getHurdleChance();
        }
        this.totalDistanceMinutes = dist;
        this.averageHurdleRisk = edges.isEmpty() ? 0.0 : (riskSum / edges.size());
        this.estimatedMinutes = Double.isNaN(estimatedMinutes) ? dist : estimatedMinutes;
        this.hurdleProbability = Double.isNaN(hurdleProbability) ? 1.0 - noHurdle : hurdleProbability;
        this.expectedHurdles = Double.isNaN(expectedHurdles) ? expected : expectedHurdles;
    }

    /** Same path, different label. */
    public Route withLabel(String newLabel) {
        return new Route(pathNodes, edges, newLabel, estimatedMinutes, hurdleProbability, expectedHurdles);
    }

    public List<Location> getPathNodes() { return Collections.unmodifiableList(pathNodes); }
    public List<RouteEdge> getEdges() { return Collections.unmodifiableList(edges); }
    public int getTotalDistanceMinutes() { return totalDistanceMinutes; }
    public double getAverageHurdleRisk() { return averageHurdleRisk; }
    public String getLabel() { return label; }
    public double getEstimatedMinutes() { return estimatedMinutes; }
    public double getHurdleProbability() { return hurdleProbability; }
    public double getExpectedHurdles() { return expectedHurdles; }

    public boolean usesShortcut() {
        for (RouteEdge e : edges) {
            if (e.isShortcut()) return true;
        }
        return false;
    }

    public Location getStart() { return pathNodes.isEmpty() ? null : pathNodes.get(0); }
    public Location getEnd() { return pathNodes.isEmpty() ? null : pathNodes.get(pathNodes.size() - 1); }

    /** True when both routes visit the same locations in the same order. */
    public boolean samePathAs(Route other) {
        return other != null && pathNodes.equals(other.pathNodes);
    }

    @Override
    public Iterator<Location> iterator() {
        return new RouteIterator(pathNodes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(label).append(": ");
        for (int i = 0; i < pathNodes.size(); i++) {
            sb.append(pathNodes.get(i).getName());
            if (i < pathNodes.size() - 1) sb.append(" -> ");
        }
        sb.append(String.format(" [~%.0f min, hurdle chance %d%%]", estimatedMinutes, Math.round(hurdleProbability * 100)));
        return sb.toString();
    }
}

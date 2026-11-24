package com.campusrush.map;

import com.campusrush.core.exceptions.InvalidRouteException;

import java.io.Serializable;
import java.util.*;

/**
 * CampusMap is the graph of campus nodes and weighted route edges
 * (HashMap of nodes + adjacency lists). Route search lives in {@link PathFinder}.
 */
public class CampusMap implements Serializable {
    private static final long serialVersionUID = 2L;

    private final Map<String, Location> nodes = new LinkedHashMap<>();
    private final Map<String, List<RouteEdge>> adjacencyList = new HashMap<>();

    public void addLocation(Location location) {
        nodes.put(location.getId(), location);
        adjacencyList.putIfAbsent(location.getId(), new ArrayList<>());
    }

    public void addEdge(Location from, Location to, int distanceMinutes, double hurdleChance) {
        addEdge(from, to, distanceMinutes, hurdleChance, RouteEdge.EdgeType.ROAD);
    }

    public void addEdge(Location from, Location to, int distanceMinutes, double hurdleChance, RouteEdge.EdgeType type) {
        addLocation(from);
        addLocation(to);
        adjacencyList.get(from.getId()).add(new RouteEdge(from, to, distanceMinutes, hurdleChance, type));
        adjacencyList.get(to.getId()).add(new RouteEdge(to, from, distanceMinutes, hurdleChance, type));
    }

    public Location getLocation(String id) {
        return nodes.get(id);
    }

    public Collection<Location> getAllLocations() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public List<RouteEdge> getEdgesFrom(Location location) {
        return adjacencyList.getOrDefault(location.getId(), Collections.emptyList());
    }

    /** The direct edge between two locations, or null if they are not adjacent. */
    public RouteEdge getEdge(Location from, Location to) {
        for (RouteEdge e : getEdgesFrom(from)) {
            if (e.getToLocation().equals(to)) return e;
        }
        return null;
    }

    /** Candidate routes using plain map distances (no time of day, speed 1.0). */
    public List<Route> findRoutes(Location start, Location end) throws InvalidRouteException {
        return PathFinder.findRoutes(this, start, end, TravelContext.neutral());
    }

    /** Candidate routes priced for a specific traveller and time of day. */
    public List<Route> findRoutes(Location start, Location end, TravelContext ctx) throws InvalidRouteException {
        return PathFinder.findRoutes(this, start, end, ctx);
    }
}

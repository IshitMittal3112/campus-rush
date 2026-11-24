package com.campusrush.map;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a weighted connection between two campus locations.
 * A SHORTCUT is much faster than the road network but carries a high hurdle risk.
 */
public class RouteEdge implements Serializable {
    private static final long serialVersionUID = 2L;

    public enum EdgeType { ROAD, SHORTCUT }

    private final Location fromLocation;
    private final Location toLocation;
    private final int distanceMinutes;
    private final double hurdleChance;
    private final EdgeType type;

    public RouteEdge(Location fromLocation, Location toLocation, int distanceMinutes, double hurdleChance) {
        this(fromLocation, toLocation, distanceMinutes, hurdleChance, EdgeType.ROAD);
    }

    public RouteEdge(Location fromLocation, Location toLocation, int distanceMinutes, double hurdleChance, EdgeType type) {
        this.fromLocation = Objects.requireNonNull(fromLocation);
        this.toLocation = Objects.requireNonNull(toLocation);
        this.distanceMinutes = distanceMinutes;
        this.hurdleChance = hurdleChance;
        this.type = type == null ? EdgeType.ROAD : type;
    }

    public Location getFromLocation() { return fromLocation; }
    public Location getToLocation() { return toLocation; }
    public int getDistanceMinutes() { return distanceMinutes; }
    public double getHurdleChance() { return hurdleChance; }
    public EdgeType getType() { return type; }
    public boolean isShortcut() { return type == EdgeType.SHORTCUT; }

    @Override
    public String toString() {
        return fromLocation.getName() + " -> " + toLocation.getName() + " (" + distanceMinutes
                + " mins, risk: " + Math.round(hurdleChance * 100) + "%" + (isShortcut() ? ", shortcut" : "") + ")";
    }
}

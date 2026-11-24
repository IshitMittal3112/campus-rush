package com.campusrush.map;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a location/building node on the campus map graph.
 */
public class Location implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final double x;
    private final double y;
    private final LocationCategory category;

    public Location(String id, String name, double x, double y) {
        this.id = Objects.requireNonNull(id, "Location ID cannot be null");
        this.name = Objects.requireNonNull(name, "Location name cannot be null");
        this.x = x;
        this.y = y;
        this.category = LocationCategory.fromId(id);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public LocationCategory getCategory() {
        return category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Objects.equals(id, location.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }
}

package com.campusrush.map;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Custom Iterator implementation for stepping through a Route's sequential locations.
 * Demonstrates Unit 2 custom Iterator requirement.
 */
public class RouteIterator implements Iterator<Location> {

    private final List<Location> path;
    private int currentIndex = 0;

    public RouteIterator(List<Location> path) {
        this.path = path;
    }

    @Override
    public boolean hasNext() {
        return currentIndex < path.size();
    }

    @Override
    public Location next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more locations along this route.");
        }
        return path.get(currentIndex++);
    }
}

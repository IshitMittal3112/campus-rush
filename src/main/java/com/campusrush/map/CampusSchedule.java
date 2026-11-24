package com.campusrush.map;

import com.campusrush.util.AppLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * Time-of-day rules for the campus: the same board behaves differently as the day goes on.
 * Each Phase can slow travel, raise hurdle risk and close whole categories of locations.
 * Loaded from phases.csv so designers can change the day without touching code.
 */
public class CampusSchedule implements Serializable {
    private static final long serialVersionUID = 1L;

    /** One stretch of the day (e.g. MORNING, EVENING). */
    public static class Phase implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final LocalTime start;
        private final double travelMultiplier;
        private final double hurdleMultiplier;
        private final Set<LocationCategory> closed;
        private final Set<String> closedIds;
        private final String description;

        public Phase(String name, LocalTime start, double travelMultiplier, double hurdleMultiplier,
                     Set<LocationCategory> closed, Set<String> closedIds, String description) {
            this.name = name;
            this.start = start;
            this.travelMultiplier = travelMultiplier;
            this.hurdleMultiplier = hurdleMultiplier;
            this.closed = closed.isEmpty() ? EnumSet.noneOf(LocationCategory.class) : EnumSet.copyOf(closed);
            this.closedIds = new HashSet<>(closedIds);
            this.description = description;
        }

        public String getName() { return name; }
        public LocalTime getStart() { return start; }
        public double getTravelMultiplier() { return travelMultiplier; }
        public double getHurdleMultiplier() { return hurdleMultiplier; }
        public Set<LocationCategory> getClosedCategories() { return Collections.unmodifiableSet(closed); }
        public String getDescription() { return description; }
        public Set<String> getClosedLocationIds() { return Collections.unmodifiableSet(closedIds); }
        public boolean isClosed(Location location) {
            return closed.contains(location.getCategory()) || closedIds.contains(location.getId());
        }

        @Override
        public String toString() { return name; }
    }

    private final List<Phase> phases = new ArrayList<>();

    public CampusSchedule(List<Phase> phases) {
        this.phases.addAll(phases);
        this.phases.sort(Comparator.comparing(Phase::getStart));
        if (this.phases.isEmpty()) {
            this.phases.add(new Phase("DAY", LocalTime.MIDNIGHT, 1.0, 1.0,
                    EnumSet.noneOf(LocationCategory.class), Collections.emptySet(), "Everything is open."));
        }
    }

    /** A schedule in which nothing ever changes (used for plain map tests). */
    public static CampusSchedule alwaysOpen() {
        return new CampusSchedule(Collections.emptyList());
    }

    public List<Phase> getPhases() {
        return Collections.unmodifiableList(phases);
    }

    /** The phase in force at the given moment (the last phase that has started). */
    public Phase phaseAt(LocalDateTime time) {
        LocalTime t = time.toLocalTime();
        Phase current = phases.get(0);
        for (Phase p : phases) {
            if (!p.getStart().isAfter(t)) current = p;
        }
        return current;
    }

    public boolean isOpen(Location location, LocalDateTime time) {
        return !phaseAt(time).isClosed(location);
    }

    /** Human-readable reason a location is closed, or null when it is open. */
    public String closedReason(Location location, LocalDateTime time) {
        Phase p = phaseAt(time);
        if (!p.isClosed(location)) return null;
        return location.getName() + " is closed during " + p.getName() + " (" + p.getDescription() + ")";
    }

    public static CampusSchedule loadFromCSV(String resourcePath) {
        List<Phase> list = new ArrayList<>();
        InputStream is = CampusSchedule.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            AppLogger.warning("Phase schedule not found (" + resourcePath + "); campus stays open all day.");
            return alwaysOpen();
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(",", 6);
                if (p.length < 5) continue;
                Set<LocationCategory> closed = EnumSet.noneOf(LocationCategory.class);
                Set<String> closedIds = new HashSet<>();
                for (String c : p[4].split(";")) {
                    String token = c.trim().toUpperCase();
                    if (token.isEmpty()) continue;
                    try {
                        closed.add(LocationCategory.valueOf(token));
                    } catch (IllegalArgumentException notACategory) {
                        closedIds.add(token);   // otherwise it is a location id
                    }
                }
                list.add(new Phase(p[0].trim(), LocalTime.parse(p[1].trim()),
                        Double.parseDouble(p[2].trim()), Double.parseDouble(p[3].trim()),
                        closed, closedIds, p.length > 5 ? p[5].trim() : ""));
            }
        } catch (Exception e) {
            AppLogger.severe("Failed to parse phases.csv: " + e.getMessage(), e);
            return alwaysOpen();
        }
        AppLogger.info("Loaded " + list.size() + " campus phases.");
        return new CampusSchedule(list);
    }
}

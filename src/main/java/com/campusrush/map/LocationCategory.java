package com.campusrush.map;

import java.util.Arrays;
import java.util.List;

/**
 * Category of a campus location. Used for map colouring AND for time-of-day closures
 * (e.g. academic blocks lock in the evening).
 */
public enum LocationCategory {
    GATE("Campus Entrance / Gate"),
    HOSTEL("Student Hostel / Residential"),
    DINING("Dining & Student Hangout"),
    OUTDOOR("Sports & Outdoor Arena"),
    ACADEMIC("Academic Block / Department / Lab");

    private final String displayName;

    LocationCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    private static final List<String> HOSTEL_TOKENS = Arrays.asList("HALL", "HOSTEL", "QUARTERS");
    private static final List<String> DINING_TOKENS = Arrays.asList("CAFE", "CANTEEN", "FOOD");
    private static final List<String> OUTDOOR_TOKENS =
            Arrays.asList("PARK", "TRACK", "FIELD", "SPORTS", "OAT", "POOL");

    /** Derives the category from a location id such as FOOD_COURT or WESTERN_GATE (token based). */
    public static LocationCategory fromId(String id) {
        List<String> tokens = Arrays.asList(id.toUpperCase().split("_"));
        if (tokens.contains("GATE")) return GATE;
        if (containsAny(tokens, HOSTEL_TOKENS)) return HOSTEL;
        if (containsAny(tokens, DINING_TOKENS)) return DINING;
        if (containsAny(tokens, OUTDOOR_TOKENS)) return OUTDOOR;
        return ACADEMIC;
    }

    private static boolean containsAny(List<String> tokens, List<String> wanted) {
        for (String w : wanted) {
            if (tokens.contains(w)) return true;
        }
        return false;
    }
}

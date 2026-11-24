package com.campusrush.player;

/** Small factory so the GUI, the database layer and the console all create characters the same way. */
public final class Characters {
    private Characters() {}

    public static final String[] NAMES = {"Sprinter", "Scholar", "Negotiator"};

    public static Character byName(String name) {
        if ("Scholar".equalsIgnoreCase(name)) return new ScholarCharacter();
        if ("Negotiator".equalsIgnoreCase(name)) return new NegotiatorCharacter();
        return new SprinterCharacter();
    }
}

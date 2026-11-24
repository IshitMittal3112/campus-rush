package com.campusrush.player;

import com.campusrush.ability.Ability;

import java.io.Serializable;

/**
 * Abstract Character class demonstrating inheritance and polymorphism.
 * Each concrete class differs by ONE passive number (kept in game_config.properties) plus
 * one ability, which keeps the classes easy to balance.
 */
public abstract class Character implements Serializable {
    private static final long serialVersionUID = 2L;

    private final String name;
    private final String description;

    protected Character(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }

    /** &gt;1 means faster walking (travel time is divided by this). */
    public double getSpeedModifier() { return 1.0; }

    /** Multiplies the chance that a path triggers a hurdle (&lt;1 = fewer hurdles). */
    public double getHurdleChanceModifier() { return 1.0; }

    /** Multiplies the points earned for solving a hurdle. */
    public double getHurdleBonusMultiplier() { return 1.0; }

    public abstract Ability getSpecialAbility();

    @Override
    public String toString() {
        return name + " (" + description + ")";
    }
}

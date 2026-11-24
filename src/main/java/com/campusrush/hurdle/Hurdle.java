package com.campusrush.hurdle;

import com.campusrush.challenge.Challenge;

import java.io.Serializable;

/**
 * Abstract Hurdle: an obstacle met while crossing a path. Each concrete hurdle brings its own
 * flavour text, delay and challenge type (polymorphism). Solve it to pass and score;
 * fail it and you lose {@link #getTimeDelayMinutes()} minutes and stay where you are.
 */
public abstract class Hurdle implements Serializable {
    private static final long serialVersionUID = 2L;

    private final String name;
    private final String description;
    private final Challenge challenge;

    protected Hurdle(String name, String description, Challenge challenge) {
        this.name = name;
        this.description = description;
        this.challenge = challenge;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Challenge getChallenge() { return challenge; }

    /** Minutes lost when the challenge is failed or times out. */
    public abstract int getTimeDelayMinutes();

    @Override
    public String toString() {
        return name + ": " + description;
    }
}

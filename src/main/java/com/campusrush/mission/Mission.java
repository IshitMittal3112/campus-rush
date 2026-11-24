package com.campusrush.mission;

import com.campusrush.map.Location;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * A campus mission: be at a location inside a time window. A Mission is an immutable catalogue
 * entry - whether it is open, done or missed depends on each player's own clock
 * (see {@link MissionManager#stateFor}).
 */
public class Mission implements Serializable {
    private static final long serialVersionUID = 2L;
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private int id;
    private final String title;
    private final String description;
    private final Location destination;
    private final LocalDateTime availableFrom;
    private final LocalDateTime deadline;
    private final int rewardPoints;
    private final MissionType type;

    public Mission(int id, String title, String description, Location destination,
                   LocalDateTime availableFrom, LocalDateTime deadline, int rewardPoints) {
        this(id, title, description, destination, availableFrom, deadline, rewardPoints, MissionType.NORMAL);
    }

    public Mission(int id, String title, String description, Location destination,
                   LocalDateTime availableFrom, LocalDateTime deadline, int rewardPoints, MissionType type) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.destination = destination;
        this.availableFrom = availableFrom;
        this.deadline = deadline;
        this.rewardPoints = rewardPoints;
        this.type = type == null ? MissionType.NORMAL : type;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Location getDestination() { return destination; }
    public LocalDateTime getAvailableFrom() { return availableFrom; }
    public LocalDateTime getDeadline() { return deadline; }
    public int getRewardPoints() { return rewardPoints; }
    public MissionType getType() { return type; }
    public boolean isSpecial() { return type == MissionType.SPECIAL; }

    /** Is the time window open at this moment? Deadlines are inclusive of the whole deadline minute. */
    public boolean isWindowOpenAt(LocalDateTime t) {
        return !t.isBefore(availableFrom) && !isPastDeadline(t);
    }

    public boolean isPastDeadline(LocalDateTime t) {
        return t.truncatedTo(ChronoUnit.MINUTES).isAfter(deadline);
    }

    public String windowText() {
        return availableFrom.format(HM) + " - " + deadline.format(HM);
    }

    @Override
    public String toString() {
        return (isSpecial() ? "[SPECIAL] " : "") + title + " @ " + destination.getName()
                + " (" + windowText() + ", " + rewardPoints + " pts)";
    }
}

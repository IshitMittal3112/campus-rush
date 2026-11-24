package com.campusrush.core;

import com.campusrush.mission.Mission;

import java.util.ArrayList;
import java.util.List;

/** What happened when a step (or a wait) was resolved. */
public final class StepResult {
    public enum Type { MOVED, BLOCKED, WAITED, TIME_UP, NONE }

    private Type type = Type.NONE;
    private double minutesSpent;
    private int pointsEarned;
    private final List<String> messages = new ArrayList<>();
    private final List<Mission> completed = new ArrayList<>();
    private final List<Mission> missed = new ArrayList<>();

    void setType(Type type) { this.type = type; }
    void setMinutesSpent(double m) { this.minutesSpent = m; }
    void addPoints(int p) { this.pointsEarned += p; }
    void addMessage(String m) { messages.add(m); }
    void addCompleted(Mission m) { completed.add(m); }
    void addMissed(Mission m) { missed.add(m); }

    public Type getType() { return type; }
    public double getMinutesSpent() { return minutesSpent; }
    public int getPointsEarned() { return pointsEarned; }
    public List<String> getMessages() { return messages; }
    public List<Mission> getCompleted() { return completed; }
    public List<Mission> getMissed() { return missed; }
    public boolean moved() { return type == Type.MOVED; }
    public boolean blocked() { return type == Type.BLOCKED; }
    public boolean completedAnyMission() { return !completed.isEmpty(); }
}

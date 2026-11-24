package com.campusrush.player;

import com.campusrush.ability.AbilityEffect;
import com.campusrush.core.GameClock;
import com.campusrush.map.Location;
import com.campusrush.mission.MissionState;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

/**
 * An active player in a Campus Rush session. Every player owns their own simulated clock,
 * mission progress and ability state. Implements Serializable for binary save/resume.
 */
public class Player implements Serializable {
    private static final long serialVersionUID = 2L;

    private int id;
    private final String name;
    private final Character character;
    private Location currentLocation;

    private int score;
    private int abilityCharges;
    private AbilityEffect armedEffect;

    private int completedMissionsCount;
    private int totalTimePenalties;      // minutes lost to failed hurdles
    private int hurdlesSolved;
    private int hurdlesFailed;
    private int shortcutsTaken;

    private GameClock clock;
    private boolean finished;
    private long lastActedSeq;           // tie-break: who acted least recently goes first

    private int targetMissionId = -1;
    private final Map<Integer, MissionState> resolvedMissions = new HashMap<>();
    private final Map<Integer, LocalDateTime> completionTimes = new HashMap<>();

    public Player(int id, String name, Character character, Location startingLocation) {
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.character = Objects.requireNonNull(character);
        this.currentLocation = Objects.requireNonNull(startingLocation);
        this.abilityCharges = character.getSpecialAbility().getMaxCharges();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public Character getCharacter() { return character; }

    public Location getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(Location currentLocation) { this.currentLocation = currentLocation; }

    public int getScore() { return score; }
    public void addScore(int points) { this.score += points; }

    public int getAbilityCharges() { return abilityCharges; }
    public void consumeAbilityCharge() { if (abilityCharges > 0) abilityCharges--; }
    public void addAbilityCharge() { abilityCharges++; }

    public AbilityEffect getArmedEffect() { return armedEffect; }
    public void armEffect(AbilityEffect effect) { this.armedEffect = effect; }
    public void clearArmedEffect() { this.armedEffect = null; }

    public int getCompletedMissionsCount() { return completedMissionsCount; }
    public void incrementCompletedMissions() { this.completedMissionsCount++; }

    public int getTotalTimePenalties() { return totalTimePenalties; }
    public void addTimePenalty(int minutes) { this.totalTimePenalties += minutes; }

    public int getHurdlesSolved() { return hurdlesSolved; }
    public void incrementHurdlesSolved() { hurdlesSolved++; }
    public int getHurdlesFailed() { return hurdlesFailed; }
    public void incrementHurdlesFailed() { hurdlesFailed++; }
    public int getShortcutsTaken() { return shortcutsTaken; }
    public void incrementShortcutsTaken() { shortcutsTaken++; }

    public GameClock getClock() { return clock; }
    public void setClock(GameClock clock) { this.clock = clock; }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
    public long getLastActedSeq() { return lastActedSeq; }
    public void setLastActedSeq(long seq) { this.lastActedSeq = seq; }

    public int getTargetMissionId() { return targetMissionId; }
    public void setTargetMissionId(int id) { this.targetMissionId = id; }

    public Map<Integer, MissionState> getResolvedMissions() { return resolvedMissions; }
    public Map<Integer, LocalDateTime> getCompletionTimes() { return completionTimes; }

    public void resolveMission(int missionId, MissionState state, LocalDateTime when) {
        resolvedMissions.put(missionId, state);
        if (state == MissionState.COMPLETED && when != null) completionTimes.put(missionId, when);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(name, ((Player) o).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name + " (" + character.getName() + ") - Score: " + score + " @ " + currentLocation.getName();
    }
}

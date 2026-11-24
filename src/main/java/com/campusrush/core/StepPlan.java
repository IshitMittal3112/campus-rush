package com.campusrush.core;

import com.campusrush.ability.AbilityEffect;
import com.campusrush.challenge.CheckpointTask;
import com.campusrush.hurdle.Hurdle;
import com.campusrush.map.CampusSchedule;
import com.campusrush.map.Location;
import com.campusrush.map.RouteEdge;
import com.campusrush.player.Player;

/**
 * Everything known about ONE step (a single edge) before the player commits to it:
 * its time cost under the current phase and character, and the hurdle (if any) that was rolled.
 * The UI/console shows the hurdle, then passes the outcome to {@link GameEngine#resolveStep}.
 */
public final class StepPlan {
    private final Player player;
    private final Location from;
    private final Location to;
    private final RouteEdge edge;
    private final long seconds;
    private final Hurdle hurdle;
    private final CheckpointTask reflexTask;
    private final boolean autoSolved;
    private final AbilityEffect appliedEffect;
    private final CampusSchedule.Phase phase;
    private final boolean affordable;

    StepPlan(Player player, Location from, Location to, RouteEdge edge, long seconds, Hurdle hurdle,
             CheckpointTask reflexTask, boolean autoSolved, AbilityEffect appliedEffect,
             CampusSchedule.Phase phase, boolean affordable) {
        this.player = player;
        this.from = from;
        this.to = to;
        this.edge = edge;
        this.seconds = seconds;
        this.hurdle = hurdle;
        this.reflexTask = reflexTask;
        this.autoSolved = autoSolved;
        this.appliedEffect = appliedEffect;
        this.phase = phase;
        this.affordable = affordable;
    }

    public Player getPlayer() { return player; }
    public Location getFrom() { return from; }
    public Location getTo() { return to; }
    public RouteEdge getEdge() { return edge; }
    public long getSeconds() { return seconds; }
    public double getMinutes() { return seconds / 60.0; }
    public Hurdle getHurdle() { return hurdle; }
    public CheckpointTask getReflexTask() { return reflexTask; }
    public AbilityEffect getAppliedEffect() { return appliedEffect; }
    public CampusSchedule.Phase getPhase() { return phase; }
    public boolean isAffordable() { return affordable; }

    /** A hurdle (quiz or reflex task) was triggered on this step. */
    public boolean hasHurdle() { return hurdle != null || reflexTask != null; }

    /** The Negotiator's ability already solved the hurdle - nothing to show the player. */
    public boolean isAutoSolved() { return autoSolved; }

    /** The player must actually attempt something before moving. */
    public boolean requiresChallenge() { return hasHurdle() && !autoSolved; }

    public String getHurdleName() {
        return hurdle != null ? hurdle.getName() : (reflexTask != null ? reflexTask.getTitle() : "");
    }

    /** Minutes lost if the challenge is failed. */
    public int getFailDelayMinutes() {
        if (hurdle != null) return hurdle.getTimeDelayMinutes();
        return reflexTask != null ? reflexTask.getTimePenaltyMinutes() : 0;
    }
}

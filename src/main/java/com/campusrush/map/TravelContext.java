package com.campusrush.map;

import com.campusrush.core.GameConfig;

import java.time.LocalDateTime;

/**
 * Everything the pathfinder needs to price a trip: when you leave, how fast you walk,
 * how likely you are to meet hurdles, and what the campus looks like at that time of day.
 */
public final class TravelContext {
    private final CampusSchedule schedule;
    private final LocalDateTime departure;
    private final double speed;
    private final double hurdleModifier;

    public TravelContext(CampusSchedule schedule, LocalDateTime departure, double speed, double hurdleModifier) {
        this.schedule = schedule == null ? CampusSchedule.alwaysOpen() : schedule;
        this.departure = departure == null ? LocalDateTime.of(2000, 1, 1, 12, 0) : departure;
        this.speed = speed <= 0 ? 1.0 : speed;
        this.hurdleModifier = hurdleModifier;
    }

    /** Plain map distances: no phases, speed 1.0, no character modifiers. */
    public static TravelContext neutral() {
        return new TravelContext(CampusSchedule.alwaysOpen(), null, 1.0, 1.0);
    }

    public CampusSchedule getSchedule() { return schedule; }
    public LocalDateTime getDeparture() { return departure; }
    public double getSpeed() { return speed; }
    public double getHurdleModifier() { return hurdleModifier; }

    /** Seconds needed to cross an edge when leaving at {@code at}. */
    public double edgeSeconds(RouteEdge edge, LocalDateTime at) {
        double mult = schedule.phaseAt(at).getTravelMultiplier();
        return edge.getDistanceMinutes() * 60.0 * mult / speed;
    }

    /** Probability that crossing the edge at {@code at} triggers a hurdle. */
    public double hurdleProbability(RouteEdge edge, LocalDateTime at) {
        double p = edge.getHurdleChance() * schedule.phaseAt(at).getHurdleMultiplier() * hurdleModifier;
        return Math.max(0.0, Math.min(0.95, p));
    }

    public double expectedDelaySeconds() {
        return GameConfig.EXPECTED_HURDLE_DELAY_MINUTES * 60.0;
    }
}

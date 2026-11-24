package com.campusrush.core;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class GameClock implements Serializable {
    private static final long serialVersionUID = 2L;

    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private LocalDateTime current;

    public GameClock(LocalDateTime startTime, int totalMinutes) {
        this.startTime = startTime;
        this.current = startTime;
        this.endTime = startTime.plusMinutes(totalMinutes);
    }

    public void advanceSeconds(long seconds) {
        if (seconds <= 0) return;
        current = current.plusSeconds(seconds);
        if (current.isAfter(endTime)) current = endTime;
    }

    public void advanceMinutes(int minutes) {
        advanceSeconds(minutes * 60L);
    }

    /** Would an action taking this many seconds still finish inside the game? */
    public boolean canAfford(long seconds) {
        return !current.plusSeconds(seconds).isAfter(endTime);
    }

    public void expire() {
        current = endTime;
    }

    public boolean isTimeExpired() {
        return !current.isBefore(endTime);
    }

    public LocalDateTime getCurrentGameTime() { return current; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }

    public long getRemainingMinutes() {
        return isTimeExpired() ? 0 : Duration.between(current, endTime).toMinutes();
    }

    public long getRemainingSeconds() {
        return isTimeExpired() ? 0 : Duration.between(current, endTime).getSeconds();
    }

    public long getElapsedMinutes() {
        return ChronoUnit.MINUTES.between(startTime, current);
    }
}

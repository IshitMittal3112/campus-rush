package com.campusrush.score;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a point-earning or penalty event recorded during a game session.
 */
public class ScoreEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int playerId;
    private int sessionId;
    private final int points;
    private final String reason;
    private final LocalDateTime recordedAt;
    private final ScoreType type;

    public ScoreEvent(int playerId, int points, String reason, LocalDateTime recordedAt) {
        this(playerId, points, reason, recordedAt, ScoreType.OTHER);
    }

    public ScoreEvent(int playerId, int points, String reason, LocalDateTime recordedAt, ScoreType type) {
        this.playerId = playerId;
        this.points = points;
        this.reason = reason;
        this.recordedAt = recordedAt;
        this.type = type == null ? ScoreType.OTHER : type;
    }

    public ScoreType getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public int getPoints() {
        return points;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    @Override
    public String toString() {
        return (points >= 0 ? "+" : "") + points + " pts: " + reason + " (" + recordedAt.toLocalTime() + ")";
    }
}

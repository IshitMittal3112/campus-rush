package com.campusrush.db;

import com.campusrush.core.exceptions.DataAccessException;
import com.campusrush.util.AppLogger;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists and retrieves historical game sessions from SQLite database.
 */
public class GameSessionDao {

    public static class SessionRecord {
        private final int id;
        private final String startedAt;
        private final String endedAt;
        private final int durationMinutes;
        private final String winner;
        private final int topScore;

        public SessionRecord(int id, String startedAt, String endedAt, int durationMinutes) {
            this(id, startedAt, endedAt, durationMinutes, null, 0);
        }

        public SessionRecord(int id, String startedAt, String endedAt, int durationMinutes, String winner, int topScore) {
            this.winner = winner;
            this.topScore = topScore;
            this.id = id;
            this.startedAt = startedAt;
            this.endedAt = endedAt;
            this.durationMinutes = durationMinutes;
        }

        public int getId() { return id; }
        public String getStartedAt() { return startedAt; }
        public String getEndedAt() { return endedAt; }
        public int getDurationMinutes() { return durationMinutes; }
        public String getWinner() { return winner == null ? "-" : winner; }
        public int getTopScore() { return topScore; }
    }

    public int createSession(LocalDateTime startedAt, int durationMinutes) throws DataAccessException {
        String sql = "INSERT INTO game_sessions (started_at, duration_minutes) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, startedAt.toString());
            ps.setInt(2, durationMinutes);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 1;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to record new game session", e);
        }
    }

    public void endSession(int sessionId, LocalDateTime endedAt) throws DataAccessException {
        String sql = "UPDATE game_sessions SET ended_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, endedAt.toString());
            ps.setInt(2, sessionId);
            ps.executeUpdate();
            AppLogger.info("Session " + sessionId + " ended at " + endedAt);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update ended_at for session: " + sessionId, e);
        }
    }

    public List<SessionRecord> findAllSessions() throws DataAccessException {
        String sql = "SELECT gs.id, gs.started_at, gs.ended_at, gs.duration_minutes, "
                + "(SELECT p.name FROM score_events se JOIN players p ON p.id = se.player_id WHERE se.session_id = gs.id "
                + " GROUP BY se.player_id ORDER BY SUM(se.points) DESC LIMIT 1) AS winner, "
                + "(SELECT SUM(se.points) FROM score_events se WHERE se.session_id = gs.id "
                + " GROUP BY se.player_id ORDER BY SUM(se.points) DESC LIMIT 1) AS top_score "
                + "FROM game_sessions gs ORDER BY gs.id DESC";
        List<SessionRecord> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String start = rs.getString("started_at");
                String end = rs.getString("ended_at");
                if (end == null) end = "In Progress";
                int dur = rs.getInt("duration_minutes");
                list.add(new SessionRecord(id, start, end, dur, rs.getString("winner"), rs.getInt("top_score")));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load historical session records", e);
        }
    }
}

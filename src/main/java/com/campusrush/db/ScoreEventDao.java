package com.campusrush.db;

import com.campusrush.core.exceptions.DataAccessException;
import com.campusrush.score.ScoreEvent;
import com.campusrush.util.AppLogger;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScoreEventDao implements Dao<ScoreEvent, Integer> {

    @Override
    public ScoreEvent findById(Integer id) throws DataAccessException {
        String sql = "SELECT id, player_id, session_id, points, reason, recorded_at FROM score_events WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToScoreEvent(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch score event with ID: " + id, e);
        }
    }

    @Override
    public List<ScoreEvent> findAll() throws DataAccessException {
        String sql = "SELECT id, player_id, session_id, points, reason, recorded_at FROM score_events";
        List<ScoreEvent> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRowToScoreEvent(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch all score events", e);
        }
    }

    @Override
    public Integer insert(ScoreEvent event) throws DataAccessException {
        String sql = "INSERT INTO score_events (player_id, session_id, points, reason, recorded_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, event.getPlayerId());
            ps.setInt(2, event.getSessionId());
            ps.setInt(3, event.getPoints());
            ps.setString(4, event.getReason());
            ps.setString(5, event.getRecordedAt().toString());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    event.setId(generatedId);
                    return generatedId;
                }
            }
            return event.getId();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert score event", e);
        }
    }

    /**
     * Demonstrates explicit transaction handling:
     * Disables auto-commit, inserts batch score events and updates session record,
     * commits transaction on success, and rollbacks on SQLException.
     */
    public void insertBatchWithSessionUpdate(List<ScoreEvent> events, int sessionId) throws DataAccessException {
        String sql = "INSERT INTO score_events (player_id, session_id, points, reason, recorded_at) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Turn off auto-commit to begin transaction

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (ScoreEvent event : events) {
                    ps.setInt(1, event.getPlayerId());
                    ps.setInt(2, sessionId);
                    ps.setInt(3, event.getPoints());
                    ps.setString(4, event.getReason());
                    ps.setString(5, event.getRecordedAt().toString());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit(); // Commit transaction
            AppLogger.info("Successfully committed transaction for " + events.size() + " score events in session " + sessionId);
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback transaction on failure
                    AppLogger.warning("Transaction rolled back for session " + sessionId + " due to error: " + e.getMessage());
                } catch (SQLException ex) {
                    AppLogger.severe("Failed to rollback transaction", ex);
                }
            }
            throw new DataAccessException("Failed batch score event insertion transaction", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit state
                    conn.close();
                } catch (SQLException e) {
                    AppLogger.severe("Failed to close connection after transaction", e);
                }
            }
        }
    }

    @Override
    public void update(ScoreEvent entity) throws DataAccessException {
        // Not used
    }

    @Override
    public void delete(Integer id) throws DataAccessException {
        String sql = "DELETE FROM score_events WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete score event", e);
        }
    }

    private ScoreEvent mapRowToScoreEvent(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int playerId = rs.getInt("player_id");
        int sessionId = rs.getInt("session_id");
        int points = rs.getInt("points");
        String reason = rs.getString("reason");
        LocalDateTime recordedAt = LocalDateTime.parse(rs.getString("recorded_at"));

        ScoreEvent event = new ScoreEvent(playerId, points, reason, recordedAt);
        event.setId(id);
        event.setSessionId(sessionId);
        return event;
    }
}

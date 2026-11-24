package com.campusrush.db;

import com.campusrush.core.exceptions.DataAccessException;
import com.campusrush.map.Location;
import com.campusrush.mission.Mission;
import com.campusrush.mission.MissionType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MissionDao implements Dao<Mission, Integer> {

    @Override
    public Mission findById(Integer id) throws DataAccessException {
        String sql = "SELECT id, session_id, location_id, available_from, deadline, reward_points, state FROM missions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMission(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch mission with ID: " + id, e);
        }
    }

    @Override
    public List<Mission> findAll() throws DataAccessException {
        String sql = "SELECT id, session_id, location_id, available_from, deadline, reward_points, state FROM missions";
        List<Mission> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRowToMission(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch all missions", e);
        }
    }

    public Integer insertForSession(Mission mission, int sessionId) throws DataAccessException {
        String sql = "INSERT INTO missions (session_id, location_id, available_from, deadline, reward_points, state) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sessionId);
            ps.setString(2, mission.getDestination().getId());
            ps.setString(3, mission.getAvailableFrom().toString());
            ps.setString(4, mission.getDeadline().toString());
            ps.setInt(5, mission.getRewardPoints());
            ps.setString(6, mission.getType() == MissionType.SPECIAL ? "SPECIAL" : "SCHEDULED");
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);   // the catalogue id (from missions.csv) is kept on the Mission itself
                }
            }
            return mission.getId();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert mission: " + mission.getTitle(), e);
        }
    }

    @Override
    public Integer insert(Mission mission) throws DataAccessException {
        return insertForSession(mission, 1);
    }

    @Override
    public void update(Mission mission) throws DataAccessException {
        String sql = "UPDATE missions SET reward_points = ?, deadline = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, mission.getRewardPoints());
            ps.setString(2, mission.getDeadline().toString());
            ps.setInt(3, mission.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update mission state", e);
        }
    }

    @Override
    public void delete(Integer id) throws DataAccessException {
        String sql = "DELETE FROM missions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete mission", e);
        }
    }

    private Mission mapRowToMission(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String locationId = rs.getString("location_id");
        String availFromStr = rs.getString("available_from");
        String deadlineStr = rs.getString("deadline");
        int points = rs.getInt("reward_points");
        String stateStr = rs.getString("state");

        Location loc = new Location(locationId, locationId, 100, 100);
        LocalDateTime avail = LocalDateTime.parse(availFromStr);
        LocalDateTime deadline = LocalDateTime.parse(deadlineStr);

        MissionType type = "SPECIAL".equals(stateStr) ? MissionType.SPECIAL : MissionType.NORMAL;
        return new Mission(id, "Mission #" + id, "Persisted Mission", loc, avail, deadline, points, type);
    }
}

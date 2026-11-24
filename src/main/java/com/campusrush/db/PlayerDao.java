package com.campusrush.db;

import com.campusrush.core.exceptions.DataAccessException;
import com.campusrush.map.Location;
import com.campusrush.player.*;
import com.campusrush.player.Character;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlayerDao implements Dao<Player, Integer> {

    @Override
    public Player findById(Integer id) throws DataAccessException {
        String sql = "SELECT id, name, character_type FROM players WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPlayer(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch player with ID: " + id, e);
        }
    }

    @Override
    public List<Player> findAll() throws DataAccessException {
        String sql = "SELECT id, name, character_type FROM players";
        List<Player> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRowToPlayer(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to fetch all players", e);
        }
    }

    @Override
    public Integer insert(Player player) throws DataAccessException {
        String sql = "INSERT INTO players (name, character_type) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, player.getName());
            ps.setString(2, player.getCharacter().getName());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    player.setId(generatedId);
                    return generatedId;
                }
            }
            return player.getId();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert player: " + player.getName(), e);
        }
    }

    @Override
    public void update(Player player) throws DataAccessException {
        String sql = "UPDATE players SET name = ?, character_type = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.getName());
            ps.setString(2, player.getCharacter().getName());
            ps.setInt(3, player.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update player: " + player.getName(), e);
        }
    }

    @Override
    public void delete(Integer id) throws DataAccessException {
        String sql = "DELETE FROM players WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete player with ID: " + id, e);
        }
    }

    private Player mapRowToPlayer(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String charType = rs.getString("character_type");

        Character character = Characters.byName(charType);

        Location defaultStart = new Location("MAIN_GATE", "Main Entrance", 80, 520);
        return new Player(id, name, character, defaultStart);
    }
}

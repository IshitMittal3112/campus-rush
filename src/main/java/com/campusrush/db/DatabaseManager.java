package com.campusrush.db;

import com.campusrush.core.exceptions.DataAccessException;
import com.campusrush.util.AppLogger;
import com.campusrush.util.PropertiesConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite database connection lifecycle and schema table creation DDL.
 */
public class DatabaseManager {

    private static String url;

    static {
        url = PropertiesConfig.getDbProp("db.url", "jdbc:sqlite:campusrush.db");
        try {
            Class.forName(PropertiesConfig.getDbProp("db.driver", "org.sqlite.JDBC"));
        } catch (ClassNotFoundException e) {
            AppLogger.severe("SQLite JDBC Driver not found!", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    public static void initializeDatabase() throws DataAccessException {
        String createPlayers = """
            CREATE TABLE IF NOT EXISTS players (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              name TEXT NOT NULL,
              character_type TEXT NOT NULL
            );
            """;

        String createSessions = """
            CREATE TABLE IF NOT EXISTS game_sessions (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              started_at TEXT NOT NULL,
              ended_at TEXT,
              duration_minutes INTEGER NOT NULL
            );
            """;

        String createMissions = """
            CREATE TABLE IF NOT EXISTS missions (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              session_id INTEGER REFERENCES game_sessions(id),
              location_id TEXT NOT NULL,
              available_from TEXT NOT NULL,
              deadline TEXT NOT NULL,
              reward_points INTEGER NOT NULL,
              state TEXT NOT NULL
            );
            """;

        String createScoreEvents = """
            CREATE TABLE IF NOT EXISTS score_events (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              player_id INTEGER REFERENCES players(id),
              session_id INTEGER REFERENCES game_sessions(id),
              points INTEGER NOT NULL,
              reason TEXT NOT NULL,
              recorded_at TEXT NOT NULL
            );
            """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createPlayers);
            stmt.execute(createSessions);
            stmt.execute(createMissions);
            stmt.execute(createScoreEvents);
            AppLogger.info("Database schema initialized successfully.");
        } catch (SQLException e) {
            throw new DataAccessException("Failed to initialize database tables", e);
        }
    }
}

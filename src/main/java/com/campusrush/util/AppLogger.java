package com.campusrush.util;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * AppLogger wraps java.util.logging.Logger and initializes logging configuration
 * from logging.properties file.
 */
public class AppLogger {

    private static final Logger LOGGER = Logger.getLogger("CampusRush");

    static {
        try (InputStream is = AppLogger.class.getClassLoader().getResourceAsStream("logging.properties")) {
            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
            }
        } catch (Exception e) {
            System.err.println("Could not load logging.properties: " + e.getMessage());
        }
    }

    /** Silences console/file logging below WARNING (used by simulations and self-tests). */
    public static void setQuiet(boolean quiet) {
        Level level = quiet ? Level.WARNING : Level.INFO;
        LOGGER.setLevel(level);
        for (java.util.logging.Handler h : LOGGER.getHandlers()) h.setLevel(level);
        for (java.util.logging.Handler h : Logger.getLogger("").getHandlers()) h.setLevel(quiet ? Level.WARNING : Level.ALL);
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warning(String message) {
        LOGGER.warning(message);
    }

    public static void severe(String message, Throwable throwable) {
        LOGGER.log(Level.SEVERE, message, throwable);
    }

    public static void severe(String message) {
        LOGGER.severe(message);
    }

    public static void fine(String message) {
        LOGGER.fine(message);
    }
}

package com.campusrush.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * Utility for loading configuration properties from game_config.properties and db.properties.
 */
public class PropertiesConfig {

    private static final Properties gameProps = new Properties();
    private static final Properties dbProps = new Properties();

    static {
        loadProperties("game_config.properties", gameProps);
        loadProperties("db.properties", dbProps);
    }

    private static void loadProperties(String resourceName, Properties props) {
        try (InputStream is = PropertiesConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is != null) {
                props.load(is);
            } else {
                AppLogger.warning("Property file not found: " + resourceName);
            }
        } catch (Exception e) {
            AppLogger.severe("Failed to load properties file: " + resourceName, e);
        }
    }

    public static String getGameProp(String key, String defaultValue) {
        return gameProps.getProperty(key, defaultValue);
    }

    public static int getGamePropInt(String key, int defaultValue) {
        try {
            String val = gameProps.getProperty(key);
            return val != null ? Integer.parseInt(val.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static double getGamePropDouble(String key, double defaultValue) {
        try {
            String val = gameProps.getProperty(key);
            return val != null ? Double.parseDouble(val.trim()) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getDbProp(String key, String defaultValue) {
        return dbProps.getProperty(key, defaultValue);
    }
}

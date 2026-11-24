package com.campusrush.map;

import com.campusrush.util.AppLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Loads campus map node and edge data from campus_map.csv resource file
 * using Java File I/O character streams.
 */
public class MapLoader {

    public static CampusMap loadMapFromCSV(String resourcePath) {
        CampusMap map = new CampusMap();
        InputStream is = MapLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            AppLogger.severe("Map CSV file not found in resources: " + resourcePath);
            return map;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split(",");
                if (parts[0].equalsIgnoreCase("NODE") && parts.length >= 5) {
                    String id = parts[1].trim();
                    String name = parts[2].trim();
                    double x = Double.parseDouble(parts[3].trim());
                    double y = Double.parseDouble(parts[4].trim());
                    map.addLocation(new Location(id, name, x, y));
                } else if (parts[0].equalsIgnoreCase("EDGE") && parts.length >= 5) {
                    String fromId = parts[1].trim();
                    String toId = parts[2].trim();
                    int dist = Integer.parseInt(parts[3].trim());
                    double chance = Double.parseDouble(parts[4].trim());
                    RouteEdge.EdgeType type = parts.length >= 6 && parts[5].trim().equalsIgnoreCase("SHORTCUT")
                            ? RouteEdge.EdgeType.SHORTCUT : RouteEdge.EdgeType.ROAD;

                    Location fromLoc = map.getLocation(fromId);
                    Location toLoc = map.getLocation(toId);
                    if (fromLoc != null && toLoc != null) {
                        map.addEdge(fromLoc, toLoc, dist, chance, type);
                    } else {
                        AppLogger.warning("Skipping edge with unknown location: " + line);
                    }
                }
            }
            AppLogger.info("Campus map successfully loaded with " + map.getAllLocations().size() + " locations.");
        } catch (Exception e) {
            AppLogger.severe("Failed to parse campus map CSV: " + e.getMessage(), e);
        }

        return map;
    }
}

package com.campusrush.mission;

import com.campusrush.map.CampusMap;
import com.campusrush.map.Location;
import com.campusrush.player.Player;
import com.campusrush.util.AppLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Mission catalogue (loaded from missions.csv) plus per-player mission queries.
 * Several missions can be open at once, so players must choose which to chase - that is the
 * strategy layer. Demonstrates Optional&lt;T&gt; return types.
 */
public class MissionManager implements Serializable {
    private static final long serialVersionUID = 2L;

    private final List<Mission> allMissions = new ArrayList<>();

    public static MissionManager loadFromCSV(String resourcePath, CampusMap map, LocalDate date) {
        MissionManager mm = new MissionManager();
        InputStream is = MissionManager.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            AppLogger.severe("Mission file not found: " + resourcePath);
            return mm;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split(",");
                try {
                    if (p.length < 7) throw new IllegalArgumentException("expected at least 7 fields");
                    Location dest = map.getLocation(p[3].trim());
                    if (dest == null) throw new IllegalArgumentException("unknown location " + p[3]);
                    LocalDateTime from = LocalDateTime.of(date, LocalTime.parse(p[4].trim()));
                    LocalDateTime deadline = LocalDateTime.of(date, LocalTime.parse(p[5].trim()));
                    MissionType type = p.length > 7 ? MissionType.valueOf(p[7].trim().toUpperCase()) : MissionType.NORMAL;
                    mm.allMissions.add(new Mission(Integer.parseInt(p[0].trim()), p[1].trim(), p[2].trim(),
                            dest, from, deadline, Integer.parseInt(p[6].trim()), type));
                } catch (RuntimeException ex) {
                    AppLogger.warning("Skipping bad mission on line " + lineNo + ": " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            AppLogger.severe("Failed to read missions.csv: " + e.getMessage(), e);
        }
        mm.allMissions.sort(Comparator.comparing(Mission::getAvailableFrom).thenComparingInt(Mission::getId));
        AppLogger.info("Loaded " + mm.allMissions.size() + " missions.");
        return mm;
    }

    public void addMission(Mission m) {
        allMissions.add(m);
        allMissions.sort(Comparator.comparing(Mission::getAvailableFrom).thenComparingInt(Mission::getId));
    }

    public List<Mission> getAllMissions() {
        return Collections.unmodifiableList(allMissions);
    }

    public Optional<Mission> findById(int id) {
        for (Mission m : allMissions) if (m.getId() == id) return Optional.of(m);
        return Optional.empty();
    }

    /** State of a mission from ONE player's point of view (their own clock decides). */
    public MissionState stateFor(Player player, Mission mission) {
        MissionState done = player.getResolvedMissions().get(mission.getId());
        if (done != null) return done;
        LocalDateTime now = player.getClock().getCurrentGameTime();
        if (mission.isPastDeadline(now)) return MissionState.EXPIRED;
        if (now.isBefore(mission.getAvailableFrom())) return MissionState.LOCKED;
        return player.getTargetMissionId() == mission.getId() ? MissionState.IN_PROGRESS : MissionState.AVAILABLE;
    }

    /** Missions whose window is open right now for this player, soonest deadline first. */
    public List<Mission> getOpenMissions(Player player) {
        List<Mission> out = new ArrayList<>();
        for (Mission m : allMissions) {
            MissionState s = stateFor(player, m);
            if (s == MissionState.AVAILABLE || s == MissionState.IN_PROGRESS) out.add(m);
        }
        out.sort(Comparator.comparing(Mission::getDeadline));
        return out;
    }

    /** Missions that have not opened yet for this player, soonest first. */
    public List<Mission> getUpcomingMissions(Player player) {
        List<Mission> out = new ArrayList<>();
        for (Mission m : allMissions) {
            if (stateFor(player, m) == MissionState.LOCKED) out.add(m);
        }
        out.sort(Comparator.comparing(Mission::getAvailableFrom));
        return out;
    }

    /** Open missions plus the next few upcoming ones - what the mission panel shows. */
    public List<Mission> getVisibleMissions(Player player, int upcomingCount) {
        List<Mission> out = new ArrayList<>(getOpenMissions(player));
        List<Mission> up = getUpcomingMissions(player);
        out.addAll(up.subList(0, Math.min(upcomingCount, up.size())));
        return out;
    }

    /** Backwards-compatible name used by the map view: every mission the player can act on now. */
    public List<Mission> getAvailableMissions(Player player) {
        return getOpenMissions(player);
    }

    public List<Mission> getOpenMissionsAt(Player player, Location location) {
        List<Mission> out = new ArrayList<>();
        for (Mission m : getOpenMissions(player)) {
            if (m.getDestination().equals(location)) out.add(m);
        }
        return out;
    }

    public Optional<Mission> getTargetMission(Player player) {
        int id = player.getTargetMissionId();
        return id < 0 ? Optional.empty() : findById(id);
    }

    public void setTarget(Player player, Mission mission) {
        player.setTargetMissionId(mission == null ? -1 : mission.getId());
    }

    public void completeMission(Player player, Mission mission, LocalDateTime when) {
        player.resolveMission(mission.getId(), MissionState.COMPLETED, when);
        if (player.getTargetMissionId() == mission.getId()) player.setTargetMissionId(-1);
        player.incrementCompletedMissions();
    }

    /** Marks every unresolved mission whose deadline has passed as EXPIRED and returns them. */
    public List<Mission> expireOverdue(Player player) {
        List<Mission> expired = new ArrayList<>();
        LocalDateTime now = player.getClock().getCurrentGameTime();
        for (Mission m : allMissions) {
            if (!player.getResolvedMissions().containsKey(m.getId()) && m.isPastDeadline(now)) {
                player.resolveMission(m.getId(), MissionState.EXPIRED, null);
                if (player.getTargetMissionId() == m.getId()) player.setTargetMissionId(-1);
                expired.add(m);
            }
        }
        return expired;
    }
}

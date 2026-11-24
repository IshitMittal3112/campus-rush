package com.campusrush.score;

import com.campusrush.player.Player;

import java.io.Serializable;
import java.util.*;

/**
 * Manages player score events and computes sorted leaderboards.
 * Demonstrates Comparator chaining with reversed order and tie-breaking.
 */
public class ScoreBoard implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Player, List<ScoreEvent>> playerEvents = new HashMap<>();

    public void addScoreEvent(Player player, ScoreEvent event) {
        playerEvents.computeIfAbsent(player, k -> new ArrayList<>()).add(event);
        player.addScore(event.getPoints());
    }

    /** Points per category for one player (mission, on-time, hurdle, shortcut ...). */
    public Map<ScoreType, Integer> getBreakdown(Player player) {
        Map<ScoreType, Integer> out = new EnumMap<>(ScoreType.class);
        for (ScoreEvent e : getEventsFor(player)) {
            out.merge(e.getType(), e.getPoints(), Integer::sum);
        }
        return out;
    }

    public List<ScoreEvent> getEventsFor(Player player) {
        return Collections.unmodifiableList(playerEvents.getOrDefault(player, Collections.emptyList()));
    }

    /**
     * Computes the live leaderboard.
     * Rule: Highest total score wins! Ties are broken by completed missions, then lowest time penalty.
     */
    public List<Player> getLeaderboard(List<Player> players) {
        List<Player> sorted = new ArrayList<>(players);

        Comparator<Player> comparator = Comparator
                .comparingInt(Player::getScore).reversed()
                .thenComparing(Comparator.comparingInt(Player::getCompletedMissionsCount).reversed())
                .thenComparingInt(Player::getTotalTimePenalties);

        sorted.sort(comparator);
        return sorted;
    }
}

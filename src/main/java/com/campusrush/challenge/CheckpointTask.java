package com.campusrush.challenge;

import com.campusrush.core.GameConfig;
import java.io.Serializable;
import java.util.Random;
public class CheckpointTask implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum TaskType {
        KEY_COMBO,      // Press sequence of directional keys (e.g. W-D-S-A) within time
        RAPID_SPACEBAR, // Mash Spacebar 10 times to power through crowd / monsoon puddles
        GATE_PIN,       // Memorize and enter a 4-digit electronic gate code
        CAMPUS_MATH,    // Fast mental arithmetic under tight time limit
        GREEN_LIGHT     // Wait for traffic light to turn GREEN, then tap SPACE instantly
    }
    private final TaskType type;
    private final String title;
    private final String description;
    private final String targetData;
    private final int timeLimitSeconds;
    private final int timePenaltyMinutes;
    private final int bonusScore;
    public CheckpointTask(TaskType type, String title, String description, String targetData,
                          int timeLimitSeconds, int timePenaltyMinutes, int bonusScore) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.targetData = targetData;
        this.timeLimitSeconds = timeLimitSeconds;
        this.timePenaltyMinutes = timePenaltyMinutes;
        this.bonusScore = bonusScore;
    }
    public static CheckpointTask generateRandomTask(String fromName, String toName) {
        return generateRandomTask(fromName, toName, new Random());
    }
    /** Reflex hurdle (keyboard task). A failed task costs {@code timePenaltyMinutes}; a solved one scores like any hurdle. */
    public static CheckpointTask generateRandomTask(String fromName, String toName, Random rand) {
        int pick = rand.nextInt(5);
        int bonus = GameConfig.HURDLE_SOLVED_POINTS;
        switch (pick) {
            case 0: {
                // Key Combo Dash
                String[] keys = {"W", "A", "S", "D"};
                StringBuilder combo = new StringBuilder();
                for (int i = 0; i < 4; i++) {
                    combo.append(keys[rand.nextInt(keys.length)]);
                }
                String seq = combo.toString();
                return new CheckpointTask(
                        TaskType.KEY_COMBO,
                        "🏃 Campus Corridor Sprint!",
                        "Sprint from " + fromName + " to " + toName + "! Type this key sequence quickly: " + seq,
                        seq,
                        5, 3, bonus
                );
            }
            case 1: {
                // Rapid Spacebar Tap
                return new CheckpointTask(
                        TaskType.RAPID_SPACEBAR,
                        "🌊 High-Traffic Crowd Rush!",
                        "Crowded walkway between " + fromName + " and " + toName + "! Tap [SPACE] 10 times rapidly to power through!",
                        "10",
                        4, 4, bonus
                );
            }
            case 2: {
                // Gate PIN Memory
                int pin = 1000 + rand.nextInt(9000);
                return new CheckpointTask(
                        TaskType.GATE_PIN,
                        "🔐 Turnstile Electronic Keypad!",
                        "Security gate checkpoint at " + toName + "! Memorize the PIN and enter it: " + pin,
                        String.valueOf(pin),
                        6, 3, bonus
                );
            }
            case 3: {
                // Fast Campus Math
                int a = 12 + rand.nextInt(38);
                int b = 15 + rand.nextInt(45);
                int sum = a + b;
                return new CheckpointTask(
                        TaskType.CAMPUS_MATH,
                        "🧠 Professor Surprise Checkpoint Quiz!",
                        "Quick test on the way to " + toName + "! Calculate: " + a + " + " + b + " = ?",
                        String.valueOf(sum),
                        6, 3, bonus
                );
            }
            default: {
                // Green Light Reflex
                return new CheckpointTask(
                        TaskType.GREEN_LIGHT,
                        "⚡ Electric Campus Shuttle Crossing!",
                        "Road crossing ahead! Wait for the light to turn GREEN, then press [SPACE] immediately!",
                        "SPACE",
                        5, 3, bonus
                );
            }
        }
    }

    public TaskType getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getTargetData() { return targetData; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public int getTimePenaltyMinutes() { return timePenaltyMinutes; }
    public int getBonusScore() { return bonusScore; }
}

package com.campusrush.core;

import com.campusrush.util.PropertiesConfig;

import java.time.LocalTime;

/**
 * Central, read-only view of game_config.properties. Every balance number lives in the
 * properties file so the game can be tuned without recompiling.
 */
public final class GameConfig {
    private GameConfig() {}

    public static final String TITLE = PropertiesConfig.getGameProp("game.title", "Campus Rush");

    public static final LocalTime START_TIME =
            LocalTime.parse(PropertiesConfig.getGameProp("game.startTime", "09:00"));
    public static final int GAME_DURATION_MINUTES = PropertiesConfig.getGamePropInt("game.durationMinutes", 480);
    public static final int WAIT_MINUTES = PropertiesConfig.getGamePropInt("game.waitMinutes", 5);

    public static final int ON_TIME_BONUS = PropertiesConfig.getGamePropInt("game.score.onTimeBonus", 20);
    public static final int HURDLE_SOLVED_POINTS = PropertiesConfig.getGamePropInt("game.score.hurdleSolved", 10);
    public static final int SHORTCUT_POINTS = PropertiesConfig.getGamePropInt("game.score.shortcut", 5);
    public static final int FIRST_ARRIVAL_BONUS = PropertiesConfig.getGamePropInt("game.score.firstArrival", 10);

    public static final int EXPECTED_HURDLE_DELAY_MINUTES =
            PropertiesConfig.getGamePropInt("game.hurdle.expectedDelayMinutes", 4);
    public static final double REFLEX_HURDLE_CHANCE =
            PropertiesConfig.getGamePropDouble("game.hurdle.reflexChance", 0.35);
    public static final int QUIZ_SECONDS = PropertiesConfig.getGamePropInt("game.hurdle.quizSeconds", 15);

    public static final double SPRINTER_SPEED = PropertiesConfig.getGamePropDouble("game.speedMultiplier.sprinter", 1.25);
    public static final double NEGOTIATOR_HURDLE_CHANCE =
            PropertiesConfig.getGamePropDouble("game.hurdleChanceMultiplier.negotiator", 0.70);
    public static final double SCHOLAR_HURDLE_BONUS =
            PropertiesConfig.getGamePropDouble("game.hurdleBonusMultiplier.scholar", 2.0);

    public static final int ABILITY_CHARGES = PropertiesConfig.getGamePropInt("game.abilityCharges", 2);
    public static final long RANDOM_SEED = PropertiesConfig.getGamePropInt("game.randomSeed", 0);
}

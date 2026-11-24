package com.campusrush.ability;

/** What an armed ability does to the player's NEXT step. */
public enum AbilityEffect {
    /** Next step costs half the time and skips the hurdle roll (Sprinter). */
    HALF_TIME_NO_HURDLE,
    /** Next step costs no time at all (Scholar - "time freeze"). */
    FREE_STEP,
    /** If the next step triggers a hurdle, it is solved automatically with full points (Negotiator). */
    AUTO_SOLVE_HURDLE
}

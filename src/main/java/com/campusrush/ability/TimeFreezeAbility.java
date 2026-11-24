package com.campusrush.ability;

public class TimeFreezeAbility extends AbstractAbility {
    private static final long serialVersionUID = 2L;
    @Override public String getName() { return "Time Freeze"; }
    @Override public String getDescription() { return "Your next step costs no game time at all."; }
    @Override public AbilityEffect getEffect() { return AbilityEffect.FREE_STEP; }
}

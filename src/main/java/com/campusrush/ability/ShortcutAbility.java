package com.campusrush.ability;

public class ShortcutAbility extends AbstractAbility {
    private static final long serialVersionUID = 2L;
    @Override public String getName() { return "Campus Shortcut"; }
    @Override public String getDescription() { return "Your next step costs half the time and skips any hurdle."; }
    @Override public AbilityEffect getEffect() { return AbilityEffect.HALF_TIME_NO_HURDLE; }
}

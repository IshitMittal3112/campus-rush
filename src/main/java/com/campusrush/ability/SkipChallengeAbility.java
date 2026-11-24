package com.campusrush.ability;

public class SkipChallengeAbility extends AbstractAbility {
    private static final long serialVersionUID = 2L;
    @Override public String getName() { return "Diplomatic Bypass"; }
    @Override public String getDescription() { return "The next hurdle you meet is solved automatically (full points, no delay)."; }
    @Override public AbilityEffect getEffect() { return AbilityEffect.AUTO_SOLVE_HURDLE; }
}

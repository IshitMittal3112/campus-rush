package com.campusrush.player;

import com.campusrush.ability.Ability;
import com.campusrush.ability.SkipChallengeAbility;
import com.campusrush.core.GameConfig;

public class NegotiatorCharacter extends Character {
    private static final long serialVersionUID = 2L;

    public NegotiatorCharacter() {
        super("Negotiator", "Meets " + Math.round((1 - GameConfig.NEGOTIATOR_HURDLE_CHANCE) * 100) + "% fewer hurdles. Ability: Diplomatic Bypass.");
    }

    @Override public double getHurdleChanceModifier() { return GameConfig.NEGOTIATOR_HURDLE_CHANCE; }
    @Override public Ability getSpecialAbility() { return new SkipChallengeAbility(); }
}

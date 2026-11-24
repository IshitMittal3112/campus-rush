package com.campusrush.player;

import com.campusrush.ability.Ability;
import com.campusrush.ability.TimeFreezeAbility;
import com.campusrush.core.GameConfig;

public class ScholarCharacter extends Character {
    private static final long serialVersionUID = 2L;

    public ScholarCharacter() {
        super("Scholar", "Hurdle questions are worth x" + GameConfig.SCHOLAR_HURDLE_BONUS + " points. Ability: Time Freeze.");
    }

    @Override public double getHurdleBonusMultiplier() { return GameConfig.SCHOLAR_HURDLE_BONUS; }
    @Override public Ability getSpecialAbility() { return new TimeFreezeAbility(); }
}

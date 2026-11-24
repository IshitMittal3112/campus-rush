package com.campusrush.player;

import com.campusrush.ability.Ability;
import com.campusrush.ability.ShortcutAbility;
import com.campusrush.core.GameConfig;

public class SprinterCharacter extends Character {
    private static final long serialVersionUID = 2L;

    public SprinterCharacter() {
        super("Sprinter", "Walks " + Math.round((GameConfig.SPRINTER_SPEED - 1) * 100) + "% faster. Ability: Campus Shortcut.");
    }

    @Override public double getSpeedModifier() { return GameConfig.SPRINTER_SPEED; }
    @Override public Ability getSpecialAbility() { return new ShortcutAbility(); }
}

package com.campusrush.ability;

import com.campusrush.core.exceptions.InsufficientAbilityChargeException;
import com.campusrush.player.Player;

import java.io.Serializable;

/**
 * Special ability of a character class. Using it ARMS an effect that the engine applies to the
 * player's next step - so the ability genuinely changes the game, not just a counter.
 */
public interface Ability extends Serializable {
    String getName();
    String getDescription();
    int getMaxCharges();
    AbilityEffect getEffect();

    /** Spends one charge and arms the effect. */
    void use(Player player) throws InsufficientAbilityChargeException;
}

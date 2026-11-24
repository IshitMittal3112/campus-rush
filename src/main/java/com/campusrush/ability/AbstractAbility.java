package com.campusrush.ability;

import com.campusrush.core.GameConfig;
import com.campusrush.core.exceptions.InsufficientAbilityChargeException;
import com.campusrush.player.Player;
import com.campusrush.util.AppLogger;

/** Shared charge handling so each concrete ability only declares name, text and effect. */
public abstract class AbstractAbility implements Ability {
    private static final long serialVersionUID = 1L;

    @Override
    public int getMaxCharges() {
        return GameConfig.ABILITY_CHARGES;
    }

    @Override
    public void use(Player player) throws InsufficientAbilityChargeException {
        if (player.getAbilityCharges() <= 0) {
            throw new InsufficientAbilityChargeException("No charges remaining for " + getName());
        }
        if (player.getArmedEffect() != null) {
            throw new InsufficientAbilityChargeException(getName() + " is already armed for your next step.");
        }
        player.consumeAbilityCharge();
        player.armEffect(getEffect());
        AppLogger.info(player.getName() + " armed " + getName());
    }
}

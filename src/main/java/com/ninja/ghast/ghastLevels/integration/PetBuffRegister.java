package com.ninja.ghast.ghastLevels.integration;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

/**
 * Abstract class for RivalPets buff registration
 *
 * This is a minimal implementation representing the base class that would be provided by RivalPets.
 * In a real implementation, this would be provided by the RivalPets plugin.
 */
public abstract class PetBuffRegister implements Listener {

    /**
     * Get the name of the buff
     *
     * @return The buff name
     */
    public abstract String getBuffName();

    /**
     * Called when a pet with this buff is activated
     *
     * @param player The player activating the pet
     */
    public abstract void onActivatePet(Player player);

    /**
     * Called when a pet with this buff is deactivated
     *
     * @param player The player deactivating the pet
     */
    public abstract void onDeactivatePet(Player player);
}
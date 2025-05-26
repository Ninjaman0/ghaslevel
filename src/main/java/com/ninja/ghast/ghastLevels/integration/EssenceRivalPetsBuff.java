package com.ninja.ghast.ghastLevels.integration;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.utils.MessageUtils;
import me.rivaldev.rivalpets.api.RivalPetsAPI;
import me.rivaldev.rivalpets.buffs.PetBuffRegister;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EssenceRivalPetsBuff extends PetBuffRegister {

    private final LevelsPlugin plugin;
    private final Map<UUID, Double> playerMultipliers = new HashMap<>();

    public EssenceRivalPetsBuff(LevelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getBuffName() {
        return "glevel_multiplier";
    }

    @Override
    public void onActivatePet(Player player) {
        if (player == null) return;

        // Use a default starting multiplier or get existing multiplier (whichever is higher)
        double defaultMultiplier = 1.5;
        double existingMultiplier = plugin.getLevelManager().getPetMultiplier(player.getUniqueId());
        double multiplier = Math.max(defaultMultiplier, existingMultiplier);


        playerMultipliers.put(player.getUniqueId(), multiplier);

        // Update the player data in our plugin (with fromRivalPets=true to avoid circular updates)
        plugin.getLevelManager().setPetMultiplier(player.getUniqueId(), multiplier, true);

        // Add experience to the RivalPets buff system
        RivalPetsAPI.getApi().addExperience(player, getBuffName());

        // Update displays
        plugin.getDisplayManager().updateDisplays(player);
    }

    @Override
    public void onDeactivatePet(Player player) {
        if (player == null) return;



        // Clear the stored multiplier
        playerMultipliers.remove(player.getUniqueId());

        // Update plugin state (with fromRivalPets=true to avoid circular updates)
        plugin.getLevelManager().clearPetMultiplier(player.getUniqueId(), true);

        // Update displays
        plugin.getDisplayManager().updateDisplays(player);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        try {
            // Get the plugin's stored multiplier data
            boolean isPetActive = plugin.getLevelManager().getPlayerData(player.getUniqueId()).isPetActive();
            double storedMultiplier = plugin.getLevelManager().getPetMultiplier(player.getUniqueId());

            plugin.getLogger().info("Join sync - Player: " + player.getName() +
                    ", Stored pet active: " + isPetActive +
                    ", Stored multiplier: " + storedMultiplier);

            // Check if player has the buff in RivalPets
            boolean hasRivalPetsBuff = RivalPetsAPI.getApi().hasBuff(player, getBuffName());

            if (hasRivalPetsBuff) {
                // Get the current multiplier from RivalPets
                double buffMultiplier = RivalPetsAPI.getApi().getBuffBoost(player, getBuffName());
                plugin.getLogger().info("RivalPets reports buff active with multiplier: " + buffMultiplier);

                // If the buff multiplier is valid, use it
                if (buffMultiplier > 1.0) {
                    playerMultipliers.put(player.getUniqueId(), buffMultiplier);

                    // Update our plugin's player data to match RivalPets (true = from RivalPets)
                    plugin.getLevelManager().setPetMultiplier(player.getUniqueId(), buffMultiplier, true);

                }
                // Buff exists but has invalid multiplier, and we have a valid stored multiplier
                else if (isPetActive && storedMultiplier > 1.0) {
                    playerMultipliers.put(player.getUniqueId(), storedMultiplier);

                    // Update RivalPets with our stored value
                    RivalPetsAPI.getApi().addExperience(player, getBuffName());

                }
            }
            // Player doesn't have RivalPets buff, but our plugin says pet is active
            else if (isPetActive && storedMultiplier > 1.0) {
                playerMultipliers.put(player.getUniqueId(), storedMultiplier);

                // Activate the buff in RivalPets with our stored multiplier
                RivalPetsAPI.getApi().addExperience(player, getBuffName());
                plugin.getLogger().info("Activated RivalPets buff with our stored multiplier: " + storedMultiplier);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error syncing RivalPets buff on join for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save data and clean up when player leaves
        Player player = event.getPlayer();
        if (player != null) {
            UUID uuid = player.getUniqueId();
            // If we have a multiplier stored, make sure it's saved to persistent storage
            if (playerMultipliers.containsKey(uuid)) {
                double multiplier = playerMultipliers.get(uuid);
                if (multiplier > 1.0) {
                    plugin.getLevelManager().setPetMultiplier(uuid, multiplier, true);
                    plugin.getLogger().info("Saved multiplier on quit for " + player.getName() + ": " + multiplier);
                }
            }
            playerMultipliers.remove(uuid);
        }
    }

    /**
     * Get the current multiplier for a player
     *
     * @param player The player to check
     * @return The current multiplier (1.0 if none)
     */
    public double getPlayerMultiplier(Player player) {
        if (player == null) return 1.0;

        UUID uuid = player.getUniqueId();
        double multiplier = 1.0;

        try {
            // Check if player has the buff in RivalPets
            if (RivalPetsAPI.getApi().hasBuff(player, getBuffName())) {
                // Get the multiplier from RivalPets
                double buffBoost = RivalPetsAPI.getApi().getBuffBoost(player, getBuffName());

                // Only use if it's a valid multiplier (greater than 1.0)
                if (buffBoost > 1.0) {
                    multiplier = buffBoost;

                } else {
                    plugin.getLogger().warning("Invalid RivalPets multiplier: " + buffBoost + ", using fallback");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting RivalPets multiplier: " + e.getMessage());
        }

        // If RivalPets didn't provide a valid multiplier, use our cached or stored value
        if (multiplier <= 1.0) {
            // First check our cache
            multiplier = playerMultipliers.getOrDefault(uuid, 1.0);

            // If cache doesn't have a valid value, check persistent storage
            if (multiplier <= 1.0 && plugin.getLevelManager().getPlayerData(uuid).isPetActive()) {
                multiplier = plugin.getLevelManager().getPlayerData(uuid).getPetMultiplier();
            }

            if (multiplier > 1.0) {
            }
        }

        return multiplier;
    }

    /**
     * Set a new multiplier for a player
     *
     * @param player The player to update
     * @param multiplier The new multiplier value
     */
    public void setPlayerMultiplier(Player player, double multiplier) {
        if (player == null) return;
        if (multiplier <= 1.0) multiplier = 1.0;

        UUID uuid = player.getUniqueId();

        // Store in our local cache
        playerMultipliers.put(uuid, multiplier);

        try {
            // Make sure the player has the buff in RivalPets
            if (!RivalPetsAPI.getApi().hasBuff(player, getBuffName())) {
                RivalPetsAPI.getApi().addExperience(player, getBuffName());
            } else {
                // Update experience to refresh the buff
                RivalPetsAPI.getApi().addExperience(player, getBuffName());
            }

            // Update the plugin's persistent storage
            plugin.getLevelManager().setPetMultiplier(uuid, multiplier, true);
        } catch (Exception e) {
            plugin.getLogger().warning("Error setting RivalPets multiplier: " + e.getMessage());
        }

        // Update displays
        plugin.getDisplayManager().updateDisplays(player);
    }

    /**
     * Apply the essence multiplier boost to an amount
     *
     * @param player The player
     * @param amount The base amount
     * @return The boosted amount
     */
    public int applyMultiplier(Player player, int amount) {
        if (player == null || amount <= 0) return amount;

        // Get the current multiplier
        double multiplier = getPlayerMultiplier(player);

        // Apply and return
        return (int) Math.round(amount * multiplier);
    }
}
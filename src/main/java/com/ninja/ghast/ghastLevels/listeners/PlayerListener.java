package com.ninja.ghast.ghastLevels.listeners;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.utils.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerListener implements Listener {

    private final LevelsPlugin plugin;

    public PlayerListener(LevelsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Load player data
        plugin.getLevelManager().loadPlayerData(uuid);

        // Check and apply booster
        long remaining = plugin.getBoosterManager().getBoosterTimeRemaining(uuid);
        if (remaining > 0) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("multiplier", String.format("%.1f",
                    plugin.getBoosterManager().getBoosterMultiplier(uuid)));
            placeholders.put("time", MessageUtils.formatTime(remaining));
            MessageUtils.sendMessage(player, "booster.active", placeholders);

            // Reschedule expiry task
            plugin.getBoosterManager().scheduleExpiryTask(uuid,
                    plugin.getLevelManager().getPlayerData(uuid).getBoosterExpiry());
        }

        // Check and notify about pet
        if (plugin.getLevelManager().getPlayerData(uuid).isPetActive()) {
            MessageUtils.sendMessage(player, "pet.active", Map.of(
                    "multiplier", String.format("%.1f",
                            plugin.getLevelManager().getPetMultiplier(uuid))
            ));
        }

        // Update displays
        plugin.getDisplayManager().updateDisplays(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            // Save player data
            plugin.getLevelManager().savePlayerData(uuid);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during player quit event", e);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        // Check world access restrictions
        if (!plugin.getWorldAccessManager().canAccessWorld(player, worldName)) {
            // Teleport back to previous world or spawn
            player.teleport(event.getFrom().getSpawnLocation());
            plugin.getWorldAccessManager().notifyInsufficientLevel(player, worldName);
            return;
        }

        // Update displays in case world has different settings
        plugin.getDisplayManager().updateDisplays(player);
    }

    // Detect when a player levels up in our custom system
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLevelUp(PlayerLevelChangeEvent event) {
        Player player = event.getPlayer();
        int oldLevel = event.getOldLevel();
        int newLevel = event.getNewLevel();

        // Make sure it's an increase
        if (newLevel > oldLevel) {
            // Play level up animations and sounds
            plugin.getLevelAnimationManager().playLevelUpAnimation(player);
        }
    }
}
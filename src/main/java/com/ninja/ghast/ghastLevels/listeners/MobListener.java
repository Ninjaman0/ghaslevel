package com.ninja.ghast.ghastLevels.listeners;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashMap;
import java.util.Map;

public class MobListener implements Listener {
    private final LevelsPlugin plugin;
    private final Map<EntityType, Integer> mobPoints;

    public MobListener(LevelsPlugin plugin) {
        this.plugin = plugin;
        this.mobPoints = new HashMap<>();
        reload();
    }

    public void reload() {
        mobPoints.clear();
        ConfigurationSection mobs = plugin.getConfig().getConfigurationSection("mobs");

        if (mobs != null) {
            for (String key : mobs.getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(key.toUpperCase());
                    int points = mobs.getInt(key);
                    if (points > 0) {
                        mobPoints.put(type, points);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid entity type in mobs config: " + key);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        // Skip if not killed by a player
        if (killer == null) {
            return;
        }

        // Skip if player is in creative or spectator mode
        if (killer.getGameMode() == GameMode.CREATIVE || killer.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // Check if levels are enabled in this world
        if (!plugin.getLevelManager().isWorldAllowed(entity.getWorld().getName())) {
            return;
        }

        // Check if levels are enabled in this region
        if (plugin.hasWorldGuard() && !plugin.getWorldGuardManager().isAllowedInRegion(entity.getLocation())) {
            return;
        }

        // Check if entity gives points
        Integer points = mobPoints.get(entity.getType());
        if (points != null && points > 0) {
            plugin.getLevelManager().givePoints(killer, points);
        }
    }
}
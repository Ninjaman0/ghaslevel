package com.ninja.ghast.ghastLevels.managers;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;

import java.util.logging.Level;

public class WorldGuardManager {

    private final LevelsPlugin plugin;
    private WorldGuardPlugin worldGuard;
    private StateFlag levelsFlag;
    private boolean defaultAllowed;

    public WorldGuardManager(LevelsPlugin plugin) {
        this.plugin = plugin;
        this.worldGuard = WorldGuardPlugin.inst();

        if (worldGuard == null) {
            plugin.getLogger().warning("WorldGuard not found or not enabled!");
            return;
        }

        // Retrieve the flag registered by onLoad
        String flagName = plugin.getConfig().getString("worldguard.flag-name", "levels-allowed");
        levelsFlag = (StateFlag) WorldGuard.getInstance().getFlagRegistry().get(flagName);
        if (levelsFlag == null) {
            plugin.getLogger().warning("WorldGuard flag '" + flagName + "' not found! Using default behavior.");
        }

        reload();
    }

    public void reload() {
        defaultAllowed = plugin.getConfig().getBoolean("worldguard.default-allowed", true);
    }

    public boolean isAllowedInRegion(Location location) {
        if (levelsFlag == null) {
            return defaultAllowed;
        }

        try {
            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(location);
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();

            // Query for the flag's value
            return query.queryValue(loc, null, levelsFlag) != StateFlag.State.DENY;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking WorldGuard flag", e);
            return defaultAllowed;
        }
    }

    // Static method to register flags during onLoad
    public static void registerFlags(LevelsPlugin plugin) {
        try {
            String flagName = plugin.getConfig().getString("worldguard.flag-name", "levels-allowed");
            boolean defaultAllowed = plugin.getConfig().getBoolean("worldguard.default-allowed", true);

            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            StateFlag levelsFlag = new StateFlag(flagName, defaultAllowed);

            try {
                registry.register(levelsFlag);
                plugin.getLogger().info("Successfully registered WorldGuard flag: " + flagName);
            } catch (FlagConflictException e) {
                // Flag already exists, use the existing one
                plugin.getLogger().log(Level.WARNING, "Flag '" + flagName + "' already exists, using existing flag", e);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to register WorldGuard flag", e);
        }
    }
}
package com.ninja.ghast.ghastLevels.managers;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

import java.util.logging.Level;

public class WorldGuardCustomFlags {

    public static void registerFlags(LevelsPlugin plugin) {
        try {
            String flagName = plugin.getConfig().getString("worldguard.flag-name", "essence-allowed");
            boolean defaultAllowed = plugin.getConfig().getBoolean("worldguard.default-allowed", true);

            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            StateFlag essenceFlag = new StateFlag(flagName, defaultAllowed);

            try {
                registry.register(essenceFlag);
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
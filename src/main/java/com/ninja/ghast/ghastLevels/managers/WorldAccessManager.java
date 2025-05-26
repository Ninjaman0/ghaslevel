package com.ninja.ghast.ghastLevels.managers;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.utils.MessageUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class WorldAccessManager {
    private final LevelsPlugin plugin;
    private final Map<String, Integer> worldLevelRequirements;

    public WorldAccessManager(LevelsPlugin plugin) {
        this.plugin = plugin;
        this.worldLevelRequirements = new HashMap<>();
        reload();
    }

    public void reload() {
        worldLevelRequirements.clear();
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("world-access");
        if (config == null) return;

        for (String worldName : config.getKeys(false)) {
            int requiredLevel = config.getInt(worldName, 0);
            if (requiredLevel > 0) {
                worldLevelRequirements.put(worldName.toLowerCase(), requiredLevel);
                plugin.getLogger().info("World access requirement loaded: " + worldName + " - Level " + requiredLevel);
            }
        }
    }

    public boolean canAccessWorld(Player player, String worldName) {
        if (worldName == null) return true;

        Integer requiredLevel = worldLevelRequirements.get(worldName.toLowerCase());
        if (requiredLevel == null) return true;

        if (player.hasPermission("levels.world.bypass")) return true;

        int playerLevel = plugin.getLevelManager().getLevel(player.getUniqueId());
        return playerLevel >= requiredLevel;
    }

    public void notifyInsufficientLevel(Player player, String worldName) {
        Integer requiredLevel = worldLevelRequirements.get(worldName.toLowerCase());
        if (requiredLevel == null) return;

        Map<String, String> placeholders = MessageUtils.placeholders();
        placeholders.put("world", worldName);
        placeholders.put("required_level", String.valueOf(requiredLevel));
        MessageUtils.sendMessage(player, "world.level-required", placeholders);
    }

    public int getRequiredLevel(String worldName) {
        if (worldName == null) return 0;
        return worldLevelRequirements.getOrDefault(worldName.toLowerCase(), 0);
    }

    public Map<String, Integer> getAllWorldRequirements() {
        return new HashMap<>(worldLevelRequirements);
    }
}
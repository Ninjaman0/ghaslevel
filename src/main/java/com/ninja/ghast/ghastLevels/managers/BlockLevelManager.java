package com.ninja.ghast.ghastLevels.managers;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class BlockLevelManager {
    private final LevelsPlugin plugin;
    private final Map<Material, Integer> blockLevelRequirements;

    public BlockLevelManager(LevelsPlugin plugin) {
        this.plugin = plugin;
        this.blockLevelRequirements = new HashMap<>();
        reload();
    }

    public void reload() {
        blockLevelRequirements.clear();
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("block-levels");
        if (config == null) return;

        for (String materialName : config.getKeys(false)) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                int requiredLevel = config.getInt(materialName, 0);
                if (requiredLevel > 0) {
                    blockLevelRequirements.put(material, requiredLevel);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in block-levels config: " + materialName);
            }
        }
    }

    public boolean canBreakBlock(Player player, Material material) {
        Integer requiredLevel = blockLevelRequirements.get(material);
        if (requiredLevel == null) return true;

        if (player.hasPermission("levels.block.bypass")) return true;

        int playerLevel = plugin.getLevelManager().getLevel(player.getUniqueId());
        return playerLevel >= requiredLevel;
    }

    public void notifyInsufficientLevel(Player player, Material material) {
        Integer requiredLevel = blockLevelRequirements.get(material);
        if (requiredLevel == null) return;

        Map<String, String> placeholders = MessageUtils.placeholders();
        placeholders.put("block", material.name().toLowerCase().replace("_", " "));
        placeholders.put("required_level", String.valueOf(requiredLevel));
        MessageUtils.sendMessage(player, "block.level-required", placeholders);
    }
}
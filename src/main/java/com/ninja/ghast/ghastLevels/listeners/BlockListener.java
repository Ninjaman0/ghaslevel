package com.ninja.ghast.ghastLevels.listeners;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.utils.MessageUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.Map;

public class BlockListener implements Listener {
    private final LevelsPlugin plugin;
    private final Map<Material, Integer> blockPoints;

    public BlockListener(LevelsPlugin plugin) {
        this.plugin = plugin;
        this.blockPoints = new HashMap<>();
        reload();
    }

    public void reload() {
        blockPoints.clear();
        ConfigurationSection blocks = plugin.getConfig().getConfigurationSection("blocks");

        if (blocks != null) {
            for (String key : blocks.getKeys(false)) {
                try {
                    Material material = Material.matchMaterial(key);
                    if (material != null) {
                        int points = blocks.getInt(key);
                        if (points > 0) {
                            blockPoints.put(material, points);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid material in blocks config: " + key);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Skip if player is in creative or spectator mode
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // Check if levels are enabled in this world
        if (!plugin.getLevelManager().isWorldAllowed(player.getWorld().getName())) {
            return;
        }

        // Check if levels are enabled in this region
        if (plugin.hasWorldGuard() && !plugin.getWorldGuardManager().isAllowedInRegion(player.getLocation())) {
            return;
        }

        // Check block level requirements
        if (!checkBlockLevelRequirements(player, block.getType())) {
            event.setCancelled(true);
            return;
        }

        // Check if block gives points
        Integer points = blockPoints.get(block.getType());
        if (points != null && points > 0) {
            plugin.getLevelManager().givePoints(player, points);
        }
    }

    /**
     * Check if player meets the level requirement for a block
     */
    private boolean checkBlockLevelRequirements(Player player, Material material) {
        if (player.hasPermission("levels.block.bypass")) {
            return true;
        }

        if (!plugin.getBlockLevelManager().canBreakBlock(player, material)) {
            plugin.getBlockLevelManager().notifyInsufficientLevel(player, material);
            return false;
        }

        return true;
    }
}
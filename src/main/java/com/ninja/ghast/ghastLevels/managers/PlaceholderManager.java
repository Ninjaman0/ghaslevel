package com.ninja.ghast.ghastLevels.managers;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.utils.MessageUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PlaceholderManager extends PlaceholderExpansion {

    private final LevelsPlugin plugin;

    public PlaceholderManager(LevelsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "glevels";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        LevelManager levelManager = plugin.getLevelManager();

        // Basic placeholders
        switch (identifier) {
            case "current_level":
                return String.valueOf(levelManager.getLevel(player.getUniqueId()));

            case "current_points":
                return String.valueOf(levelManager.getPoints(player.getUniqueId()));

            case "current_exp":
                return String.valueOf(levelManager.getCurrentLevelXp(player.getUniqueId()));

            case "required_exp":
                return String.valueOf(levelManager.getXpForNextLevel(player.getUniqueId()));

            case "max_level":
                return String.valueOf(levelManager.getMaxLevel());

            case "progress_percent": {
                float progress = levelManager.getLevelProgress(player.getUniqueId());
                return String.format("%.1f", progress * 100);
            }

            case "progress_bar": {
                float progress = levelManager.getLevelProgress(player.getUniqueId());
                return MessageUtils.createProgressBar(progress, 10, "■", "□");
            }

            case "progress_bar_long": {
                float progress = levelManager.getLevelProgress(player.getUniqueId());
                return MessageUtils.createProgressBar(progress, 20, "■", "□");
            }

            case "tab_progress_bar": {
                float progress = levelManager.getLevelProgress(player.getUniqueId());
                String filledChar = plugin.getConfig().getString("display.tablist.progress_filled", "■");
                String emptyChar = plugin.getConfig().getString("display.tablist.progress_empty", "□");
                int length = plugin.getConfig().getInt("display.tablist.progress_length", 20);
                return MessageUtils.createProgressBar(progress, length, filledChar, emptyChar);
            }

            case "booster_active":
                return String.valueOf(plugin.getBoosterManager().hasActiveBooster(player.getUniqueId()));

            case "booster_multiplier":
                return String.format("%.1f", plugin.getBoosterManager().getBoosterMultiplier(player.getUniqueId()));

            case "booster_time_left":
                return String.valueOf(plugin.getBoosterManager().getBoosterTimeRemaining(player.getUniqueId()));

            case "total_multiplier":
                return String.format("%.2f", levelManager.getTotalMultiplier(player));

            case "bar_enabled":
                return String.valueOf(levelManager.isActionBarEnabled(player.getUniqueId()));

            case "event_name":
                return plugin.getEventManager().isEventRunning() ?
                        plugin.getEventManager().getCurrentEventName() : "None";

            case "event_multiplier":
                return String.format("%.1f", plugin.getEventManager().getCurrentMultiplier());

            case "event_time_left":
                return String.valueOf(plugin.getEventManager().getCurrentEventTimeRemaining());

            case "armor_multiplier":
                return String.format("%.2f", plugin.getArmorListener().getArmorMultiplier(player));

            case "pet_multiplier":
                return String.format("%.1f", plugin.getLevelManager().getPetMultiplier(player.getUniqueId()));
        }

        // Top player placeholders
        if (identifier.startsWith("top_")) {
            try {
                // Format: top_<position>_<name|level>
                String[] parts = identifier.split("_");
                if (parts.length == 3) {
                    int position = Integer.parseInt(parts[1]);
                    String type = parts[2];

                    if (type.equals("name")) {
                        return plugin.getTopPlayersManager().getTopPlayerName(position);
                    } else if (type.equals("level")) {
                        return String.valueOf(plugin.getTopPlayersManager().getTopPlayerLevel(position));
                    }
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }
}
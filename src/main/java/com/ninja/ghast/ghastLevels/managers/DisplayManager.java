package com.ninja.ghast.ghastLevels.managers;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.utils.MessageUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class DisplayManager {
    private final LevelsPlugin plugin;
    private BukkitTask displayTask;

    // Configuration options
    private boolean actionBarEnabled;
    private String actionBarFormat;
    private int progressLength;
    private String progressFilled;
    private String progressEmpty;

    private boolean tablistEnabled;
    private String tablistHeader;
    private String tablistFooter;
    private int tablistProgressLength;
    private String tablistProgressFilled;
    private String tablistProgressEmpty;

    public DisplayManager(LevelsPlugin plugin) {
        this.plugin = plugin;
        reload();
        startDisplayTask();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();

        // Load action bar settings
        this.actionBarEnabled = config.getBoolean("display.actionbar.enabled", true);
        this.actionBarFormat = config.getString("display.actionbar.format",
                "&bCustom Level: {level} &7[&a{progress_bar}&7] &b{current_exp}/{required_exp} XP");
        this.progressLength = config.getInt("display.actionbar.progress_length", 20);
        this.progressFilled = config.getString("display.actionbar.progress_filled", "■");
        this.progressEmpty = config.getString("display.actionbar.progress_empty", "□");

        // Load tablist settings
        this.tablistEnabled = config.getBoolean("display.tablist.enabled", true);
        this.tablistHeader = config.getString("display.tablist.header",
                "&b&lCustom Levels\n&7Progress: {progress_bar} &f({current_exp}/{required_exp})");
        this.tablistFooter = config.getString("display.tablist.footer",
                "&7Level: &b{level} &7| Multiplier: &b{multiplier}x");
        this.tablistProgressLength = config.getInt("display.tablist.progress_length", 20);
        this.tablistProgressFilled = config.getString("display.tablist.progress_filled", "■");
        this.tablistProgressEmpty = config.getString("display.tablist.progress_empty", "□");

        // Restart display task if needed
        if (displayTask != null) {
            displayTask.cancel();
            startDisplayTask();
        }
    }

    private void startDisplayTask() {
        // Task runs every 10 ticks (0.5 seconds) to update displays
        displayTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (!actionBarEnabled && !tablistEnabled) {
                return; // Skip if both displays are disabled
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                updateDisplays(player);
            }
        }, 20, 10);
    }

    public void updateDisplays(Player player) {
        if (player == null) return;

        UUID uuid = player.getUniqueId();

        // Update action bar if enabled globally and for this player
        if (actionBarEnabled && plugin.getLevelManager().isActionBarEnabled(uuid)) {
            sendActionBar(player);
        }

        // Update tablist if enabled
        if (tablistEnabled) {
            updateTablist(player);
        }
    }

    private void sendActionBar(Player player) {
        UUID uuid = player.getUniqueId();
        LevelManager levelManager = plugin.getLevelManager();

        int level = levelManager.getLevel(uuid);
        int currentExp = levelManager.getCurrentLevelXp(uuid);
        int requiredExp = levelManager.getXpForNextLevel(uuid);
        float progress = levelManager.getLevelProgress(uuid);
        double multiplier = levelManager.getTotalMultiplier(player);

        String progressBar = MessageUtils.createProgressBar(
                progress, progressLength, progressFilled, progressEmpty);

        // Replace placeholders
        String message = actionBarFormat
                .replace("{level}", String.valueOf(level))
                .replace("{current_exp}", String.valueOf(currentExp))
                .replace("{required_exp}", String.valueOf(requiredExp))
                .replace("{progress_bar}", progressBar)
                .replace("{multiplier}", String.format("%.1f", multiplier));

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(MessageUtils.translateColors(message)));
    }

    private void updateTablist(Player player) {
        UUID uuid = player.getUniqueId();
        LevelManager levelManager = plugin.getLevelManager();

        int level = levelManager.getLevel(uuid);
        int currentExp = levelManager.getCurrentLevelXp(uuid);
        int requiredExp = levelManager.getXpForNextLevel(uuid);
        float progress = levelManager.getLevelProgress(uuid);
        double multiplier = levelManager.getTotalMultiplier(player);

        String progressBar = MessageUtils.createProgressBar(
                progress, tablistProgressLength, tablistProgressFilled, tablistProgressEmpty);

        // Replace placeholders in header and footer
        String header = tablistHeader
                .replace("{level}", String.valueOf(level))
                .replace("{current_exp}", String.valueOf(currentExp))
                .replace("{required_exp}", String.valueOf(requiredExp))
                .replace("{progress_bar}", progressBar)
                .replace("{multiplier}", String.format("%.1f", multiplier));

        String footer = tablistFooter
                .replace("{level}", String.valueOf(level))
                .replace("{current_exp}", String.valueOf(currentExp))
                .replace("{required_exp}", String.valueOf(requiredExp))
                .replace("{progress_bar}", progressBar)
                .replace("{multiplier}", String.format("%.1f", multiplier));

        player.setPlayerListHeader(MessageUtils.translateColors(header));
        player.setPlayerListFooter(MessageUtils.translateColors(footer));
    }
}
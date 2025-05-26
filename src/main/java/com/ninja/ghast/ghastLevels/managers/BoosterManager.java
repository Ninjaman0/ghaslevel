package com.ninja.ghast.ghastLevels.managers;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.model.PlayerData;
import com.ninja.ghast.ghastLevels.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoosterManager {
    private final LevelsPlugin plugin;
    private final Map<UUID, BukkitTask> expiryTasks = new HashMap<>();

    private double maxMultiplier;
    private long maxDuration;

    public BoosterManager(LevelsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        maxMultiplier = config.getDouble("boosters.max-multiplier", 5.0);
        maxDuration = config.getLong("boosters.max-duration", 86400);
    }

    public boolean addBooster(Player player, double multiplier, long durationSeconds) {
        UUID uuid = player.getUniqueId();

        // Validate inputs first
        if (multiplier <= 1.0) {
            MessageUtils.sendMessage(player, "booster.invalid-multiplier");
            return false;
        }

        if (durationSeconds <= 0) {
            MessageUtils.sendMessage(player, "booster.invalid-duration");
            return false;
        }

        // Check for active booster
        if (hasActiveBooster(uuid)) {
            sendActiveBoosterMessage(player);
            return false;
        }

        // Cap values
        multiplier = Math.min(multiplier, maxMultiplier);
        durationSeconds = Math.min(durationSeconds, maxDuration);

        // Apply booster
        long expiryTime = System.currentTimeMillis() + (durationSeconds * 1000);
        PlayerData data = plugin.getLevelManager().getPlayerData(uuid);
        data.setBoosterActive(true);
        data.setBoosterMultiplier(multiplier);
        data.setBoosterExpiry(expiryTime);
        plugin.getLevelManager().savePlayerData(uuid);

        // Manage expiry task
        cancelExpiryTask(uuid);
        scheduleExpiryTask(uuid, expiryTime);

        // Send success message
        Map<String, String> placeholders = MessageUtils.placeholders();
        placeholders.put("multiplier", String.format("%.1f", multiplier));
        placeholders.put("duration", MessageUtils.formatTime(durationSeconds));
        MessageUtils.sendMessage(player, "booster.applied", placeholders);

        return true;
    }

    public boolean hasActiveBooster(UUID uuid) {
        PlayerData data = plugin.getLevelManager().getPlayerData(uuid);
        return data != null &&
                data.isBoosterActive() &&
                System.currentTimeMillis() < data.getBoosterExpiry();
    }

    private void sendActiveBoosterMessage(Player player) {
        long remaining = getBoosterTimeRemaining(player.getUniqueId());
        double currentMultiplier = getBoosterMultiplier(player.getUniqueId());

        Map<String, String> placeholders = MessageUtils.placeholders();
        placeholders.put("multiplier", String.format("%.1f", currentMultiplier));
        placeholders.put("time", MessageUtils.formatTime(remaining));
        MessageUtils.sendMessage(player, "booster.already-active", placeholders);
    }

    public double getBoosterMultiplier(UUID uuid) {
        if (!hasActiveBooster(uuid)) return 1.0;
        return plugin.getLevelManager().getPlayerData(uuid).getBoosterMultiplier();
    }

    public long getBoosterTimeRemaining(UUID uuid) {
        if (!hasActiveBooster(uuid)) return 0;
        return (plugin.getLevelManager().getPlayerData(uuid).getBoosterExpiry() - System.currentTimeMillis()) / 1000;
    }

    public void scheduleExpiryTask(UUID uuid, long expiryTime) {
        cancelExpiryTask(uuid);

        long ticksRemaining = Math.max(1, (expiryTime - System.currentTimeMillis()) / 50);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PlayerData data = plugin.getLevelManager().getPlayerData(uuid);
            data.setBoosterActive(false);
            data.setBoosterMultiplier(1.0);
            data.setBoosterExpiry(0);
            plugin.getLevelManager().savePlayerData(uuid);

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                MessageUtils.sendMessage(player, "booster.expired");
            }
            expiryTasks.remove(uuid);
        }, ticksRemaining);

        expiryTasks.put(uuid, task);
    }

    private void cancelExpiryTask(UUID uuid) {
        BukkitTask task = expiryTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
}

package com.ninja.ghast.ghastLevels.storage;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.model.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class YamlStorage implements StorageProvider {
    private final LevelsPlugin plugin;
    private final File dataFolder;
    private boolean initialized = false;

    public YamlStorage(LevelsPlugin plugin) {
        this.plugin = plugin;

        // Get custom directory from config if specified
        String customDir = plugin.getConfig().getString("storage.yaml.directory", "playerdata");
        this.dataFolder = new File(plugin.getDataFolder(), customDir);

        if (!dataFolder.exists()) {
            if (dataFolder.mkdirs()) {
                plugin.getLogger().info("Created directory: " + dataFolder.getPath());
            } else {
                plugin.getLogger().severe("Failed to create directory: " + dataFolder.getPath());
            }
        }
    }

    @Override
    public void initialize() {
        plugin.getLogger().info("Initialized YAML storage in: " + dataFolder.getPath());
        initialized = true;
    }

    @Override
    public void shutdown() {
        // Nothing special needed for YAML
        plugin.getLogger().info("YAML storage shutdown complete");
    }

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        if (!initialized) {
            plugin.getLogger().warning("Attempted to load player data before initialization");
        }

        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        PlayerData data = new PlayerData();
        data.setUuid(uuid);

        if (playerFile.exists()) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
                // Use player name from config, but default to their actual name from server if known
                String playerName = config.getString("name", null);
                if (playerName == null || playerName.equalsIgnoreCase("Unknown")) {
                    // Try to get the actual player name from the server
                    playerName = plugin.getServer().getOfflinePlayer(uuid).getName();
                    // If still null, use "Unknown" as a fallback
                    if (playerName == null) {
                        playerName = "Unknown";
                    }
                }
                data.setName(playerName);
                data.setLevelPoints(config.getInt("levelPoints", 0));
                data.setLevel(config.getInt("level", 0));
                data.setPetActive(config.getBoolean("pet.active", false));
                data.setPetMultiplier(config.getDouble("pet.multiplier", 1.0));
                data.setBoosterActive(config.getBoolean("booster.active", false));
                data.setBoosterMultiplier(config.getDouble("booster.multiplier", 1.0));
                data.setBoosterExpiry(config.getLong("booster.expiry", 0));
                data.setActionBarEnabled(config.getBoolean("display.actionBar", true));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading player data for " + uuid, e);
            }
        } else {
            // For new player data, try to get their actual name from the server
            String playerName = plugin.getServer().getOfflinePlayer(uuid).getName();
            if (playerName != null) {
                data.setName(playerName);
            } else {
                data.setName("Unknown");
            }
        }

        return data;
    }

    @Override
    public void savePlayerData(PlayerData playerData) {
        if (!initialized) {
            plugin.getLogger().warning("Attempted to save player data before initialization");
        }

        File playerFile = new File(dataFolder, playerData.getUuid().toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        try {
            // Before saving, ensure we have the most up-to-date player name
            if (playerData.getName() == null || playerData.getName().equalsIgnoreCase("Unknown")) {
                String playerName = plugin.getServer().getOfflinePlayer(playerData.getUuid()).getName();
                if (playerName != null) {
                    playerData.setName(playerName);
                }
            }

            config.set("name", playerData.getName());
            config.set("levelPoints", playerData.getLevelPoints());
            config.set("level", playerData.getLevel());
            config.set("pet.active", playerData.isPetActive());
            config.set("pet.multiplier", playerData.getPetMultiplier());
            config.set("booster.active", playerData.isBoosterActive());
            config.set("booster.multiplier", playerData.getBoosterMultiplier());
            config.set("booster.expiry", playerData.getBoosterExpiry());
            config.set("display.actionBar", playerData.isActionBarEnabled());

            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save player data for " + playerData.getUuid(), e);
        }
    }

    @Override
    public void removePlayerData(UUID uuid) {
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        if (playerFile.exists()) {
            if (playerFile.delete()) {
                plugin.getLogger().info("Removed player data for " + uuid);
            } else {
                plugin.getLogger().warning("Failed to delete player file for " + uuid);
            }
        }
    }

    @Override
    public boolean validateConnection() {
        // No connection to validate for YAML storage
        return true;
    }
}
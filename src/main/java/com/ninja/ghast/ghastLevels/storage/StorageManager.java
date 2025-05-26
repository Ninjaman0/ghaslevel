package com.ninja.ghast.ghastLevels.storage;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.model.PlayerData;

import java.util.UUID;
import java.util.logging.Level;

public class StorageManager {
    private final LevelsPlugin plugin;
    private StorageProvider storageProvider;
    private StorageType currentStorageType;

    public enum StorageType {
        YAML,
        SQLITE
    }

    public StorageManager(LevelsPlugin plugin) {
        this.plugin = plugin;
        initializeStorage();
    }

    private void initializeStorage() {
        String storageType = plugin.getConfig().getString("storage.type", "yaml").toLowerCase();
        StorageType newStorageType;

        switch (storageType) {
            case "sqlite":
                newStorageType = StorageType.SQLITE;
                storageProvider = new SQLiteStorage(plugin);
                plugin.getLogger().info("Using SQLite storage");
                break;
            case "yaml":
            default:
                newStorageType = StorageType.YAML;
                storageProvider = new YamlStorage(plugin);
                plugin.getLogger().info("Using YAML storage");
                break;
        }

        try {
            storageProvider.initialize();
            currentStorageType = newStorageType;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize " + newStorageType + " storage", e);

            // Fallback to YAML if SQLite fails
            if (newStorageType == StorageType.SQLITE) {
                plugin.getLogger().warning("Falling back to YAML storage due to SQLite initialization failure");
                storageProvider = new YamlStorage(plugin);
                try {
                    storageProvider.initialize();
                    currentStorageType = StorageType.YAML;
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to initialize fallback YAML storage", ex);
                }
            }
        }
    }

    public PlayerData loadPlayerData(UUID uuid) {
        if (storageProvider == null) {
            plugin.getLogger().severe("Storage provider is null! Creating empty player data");
            PlayerData data = new PlayerData();
            data.setUuid(uuid);
            return data;
        }

        try {
            return storageProvider.loadPlayerData(uuid);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading player data for " + uuid, e);
            PlayerData data = new PlayerData();
            data.setUuid(uuid);
            return data;
        }
    }

    public void savePlayerData(PlayerData playerData) {
        if (storageProvider == null) {
            plugin.getLogger().severe("Storage provider is null! Cannot save player data");
            return;
        }

        try {
            storageProvider.savePlayerData(playerData);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving player data for " + playerData.getUuid(), e);
        }
    }

    public void removePlayerData(UUID uuid) {
        if (storageProvider == null) {
            plugin.getLogger().severe("Storage provider is null! Cannot remove player data");
            return;
        }

        try {
            storageProvider.removePlayerData(uuid);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error removing player data for " + uuid, e);
        }
    }

    public void shutdown() {
        if (storageProvider != null) {
            try {
                storageProvider.shutdown();
                plugin.getLogger().info("Storage provider shutdown complete");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error during storage provider shutdown", e);
            }
        }
    }

    public void runMaintenance() {
        if (storageProvider != null) {
            try {
                boolean valid = storageProvider.validateConnection();
                if (!valid) {
                    plugin.getLogger().warning("Storage validation failed during maintenance, attempting to reinitialize...");
                    shutdown();
                    initializeStorage();
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error during storage maintenance", e);
            }
        }
    }

    public StorageType getCurrentStorageType() {
        return currentStorageType;
    }
}
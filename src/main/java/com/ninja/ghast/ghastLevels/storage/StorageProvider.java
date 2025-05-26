package com.ninja.ghast.ghastLevels.storage;

import com.ninja.ghast.ghastLevels.model.PlayerData;
import java.util.UUID;

public interface StorageProvider {
    void initialize();
    void shutdown();
    PlayerData loadPlayerData(UUID uuid);
    void savePlayerData(PlayerData playerData);
    void removePlayerData(UUID uuid);
    boolean validateConnection();
}
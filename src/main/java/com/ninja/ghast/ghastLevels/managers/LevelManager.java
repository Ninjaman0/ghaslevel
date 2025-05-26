package com.ninja.ghast.ghastLevels.managers;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.listeners.ArmorListener;
import com.ninja.ghast.ghastLevels.model.PlayerData;
import com.ninja.ghast.ghastLevels.storage.StorageManager;
import com.ninja.ghast.ghastLevels.utils.MessageUtils;
import me.rivaldev.rivalpets.api.RivalPetsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLevelChangeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LevelManager {
    private final LevelsPlugin plugin;
    private final StorageManager storageManager;
    private final Map<UUID, PlayerData> playerDataCache;
    private final ArmorListener armorListener;

    private int basePerLevel;
    private int maxLevel;
    private Particle gainParticle;
    private int particleCount;
    private Set<String> worldWhitelist;
    private Set<String> worldBlacklist;
    private Map<Integer, Integer> levelRequirements;

    public LevelManager(LevelsPlugin plugin, ArmorListener armorListener) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.storageManager = Objects.requireNonNull(plugin.getStorageManager(), "StorageManager cannot be null");
        this.armorListener = armorListener;
        this.playerDataCache = new ConcurrentHashMap<>();
        this.levelRequirements = new HashMap<>();
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();

        basePerLevel = Math.max(1, config.getInt("levels.base-per-level", 100));
        maxLevel = Math.max(1, config.getInt("levels.max-level", 1000));

        String particleName = config.getString("levels.gain-particle", "WITCH");
        try {
            gainParticle = particleName != null && !particleName.equalsIgnoreCase("NONE") ?
                    Particle.valueOf(particleName.toUpperCase()) : null;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type: " + particleName + ". Using WITCH instead.");
            gainParticle = Particle.WITCH;
        }

        particleCount = Math.max(1, config.getInt("levels.gain-particle-count", 10));
        worldWhitelist = new HashSet<>(config.getStringList("worlds.whitelist"));
        worldBlacklist = new HashSet<>(config.getStringList("worlds.blacklist"));

        // Load custom level requirements
        loadLevelRequirements();

        // Load data for online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null) {
                loadPlayerData(player.getUniqueId());
            }
        }
    }

    private void loadLevelRequirements() {
        levelRequirements.clear();
        FileConfiguration levelConfig = plugin.getLevelConfig();

        for (String key : levelConfig.getKeys(false)) {
            try {
                if (key.startsWith("#")) continue; // Skip comment lines

                int level = Integer.parseInt(key);
                int xp = levelConfig.getInt(key);

                if (level > 0 && xp > 0) {
                    levelRequirements.put(level, xp);
                }
            } catch (NumberFormatException ignored) {
                // Skip non-integer keys
            }
        }

        plugin.getLogger().info("Loaded " + levelRequirements.size() + " custom level requirements");
    }

    public PlayerData getPlayerData(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }

        return playerDataCache.computeIfAbsent(uuid, id -> {
            PlayerData data = storageManager.loadPlayerData(id);
            if (data == null) {
                data = new PlayerData();
                data.setUuid(id);
            }
            return data;
        });
    }

    public void loadPlayerData(UUID uuid) {
        if (uuid == null) return;

        PlayerData data = storageManager.loadPlayerData(uuid);
        if (data != null) {
            playerDataCache.put(uuid, data);
        }
    }

    public void savePlayerData(UUID uuid) {
        if (uuid == null) return;

        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            storageManager.savePlayerData(data);
        }
    }

    public void saveAllPlayerData() {
        playerDataCache.forEach((uuid, data) -> {
            if (uuid != null && data != null) {
                storageManager.savePlayerData(data);
            }
        });
    }

    public int getPoints(UUID uuid) {
        return uuid != null ? getPlayerData(uuid).getLevelPoints() : 0;
    }

    public void setPoints(UUID uuid, int amount) {
        if (uuid == null) return;

        PlayerData data = getPlayerData(uuid);
        int oldLevel = data.getLevel();
        data.setLevelPoints(Math.max(0, amount));

        // Recalculate level based on points
        recalculatePlayerLevel(data);

        savePlayerData(uuid);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            // Check if level changed
            int newLevel = data.getLevel();
            if (newLevel != oldLevel) {
                // Call level change event
                Bukkit.getPluginManager().callEvent(new PlayerLevelChangeEvent(player, oldLevel, newLevel));
            }

            plugin.getDisplayManager().updateDisplays(player);
        }
    }

    private void recalculatePlayerLevel(PlayerData data) {
        int points = data.getLevelPoints();
        int level = 0;
        int remainingPoints = points;

        // Try to reach as high a level as possible with the points
        while (level < maxLevel) {
            int requiredForNextLevel = getXpForLevel(level + 1);
            if (remainingPoints >= requiredForNextLevel) {
                level++;
                remainingPoints -= requiredForNextLevel;
            } else {
                break;
            }
        }

        data.setLevel(level);
    }

    public boolean givePoints(Player player, int amount) {
        if (player == null || amount <= 0) return false;

        UUID uuid = player.getUniqueId();
        PlayerData data = getPlayerData(uuid);
        int currentPoints = data.getLevelPoints();
        int currentLevel = data.getLevel();

        if (currentLevel >= maxLevel) {
            MessageUtils.sendMessage(player, "levels.max-level");
            return false;
        }

        // Calculate max points based on reaching max level
        int maxPoints = calculateMaxPoints();
        int newPoints = Math.min(currentPoints + amount, maxPoints);
        int actualAmount = newPoints - currentPoints;

        if (actualAmount <= 0) {
            MessageUtils.sendMessage(player, "levels.max-level");
            return false;
        }

        double multiplier = getTotalMultiplier(player);
        int finalAmount = (int) Math.round(actualAmount * multiplier);

        int oldLevel = getLevel(uuid);
        setPoints(uuid, currentPoints + finalAmount);
        int newLevel = getLevel(uuid);

        if (gainParticle != null) {
            Location loc = player.getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(gainParticle, loc, particleCount, 0.5, 0.5, 0.5, 0.05);
        }

        // Check if player leveled up and play animation if needed
        if (newLevel > oldLevel) {
            // Calling the event here will make the animation play via PlayerListener
            Bukkit.getPluginManager().callEvent(new PlayerLevelChangeEvent(player, oldLevel, newLevel));
        }

        // Update display for player
        plugin.getDisplayManager().updateDisplays(player);

        return true;
    }

    private int calculateMaxPoints() {
        int total = 0;
        for (int level = 1; level <= maxLevel; level++) {
            total += getXpForLevel(level);
        }
        return total;
    }

    public boolean takePoints(Player player, int amount) {
        if (player == null || amount <= 0) return false;

        UUID uuid = player.getUniqueId();
        int currentPoints = getPoints(uuid);

        if (currentPoints < amount) {
            MessageUtils.sendMessage(player, "levels.not-enough");
            return false;
        }

        setPoints(uuid, currentPoints - amount);

        // Update display for player
        plugin.getDisplayManager().updateDisplays(player);

        return true;
    }

    public int getLevel(UUID uuid) {
        return uuid != null ? getPlayerData(uuid).getLevel() : 0;
    }

    public boolean setLevel(UUID uuid, int level) {
        if (uuid == null) return false;

        // Cap at max level
        level = Math.min(level, maxLevel);

        // Calculate points required to reach this level
        int totalPoints = 0;
        for (int i = 1; i <= level; i++) {
            totalPoints += getXpForLevel(i);
        }

        PlayerData data = getPlayerData(uuid);
        int oldLevel = data.getLevel();
        data.setLevel(level);
        data.setLevelPoints(totalPoints);
        savePlayerData(uuid);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            // Fire the level change event if needed
            if (level != oldLevel) {
                Bukkit.getPluginManager().callEvent(new PlayerLevelChangeEvent(player, oldLevel, level));
            }

            plugin.getDisplayManager().updateDisplays(player);
        }

        return true;
    }

    public int getXpForLevel(int level) {
        // Check if a custom XP requirement exists for this level
        Integer customXp = levelRequirements.get(level);
        if (customXp != null) {
            return customXp;
        }

        // Otherwise use the default formula
        return level * basePerLevel;
    }

    public int getCurrentLevelXp(UUID uuid) {
        if (uuid == null) return 0;

        PlayerData data = getPlayerData(uuid);
        int currentLevel = data.getLevel();
        int totalForCurrentLevel = 0;

        // Calculate total XP required to reach current level
        for (int i = 1; i <= currentLevel; i++) {
            totalForCurrentLevel += getXpForLevel(i);
        }

        return data.getLevelPoints() - totalForCurrentLevel;
    }

    public int getXpForNextLevel(UUID uuid) {
        if (uuid == null) return 0;

        int currentLevel = getLevel(uuid);
        return currentLevel >= maxLevel ? 0 : getXpForLevel(currentLevel + 1);
    }

    public float getLevelProgress(UUID uuid) {
        if (uuid == null) return 0.0f;

        int currentLevel = getLevel(uuid);

        if (currentLevel >= maxLevel) {
            return 1.0f;
        }

        int currentLevelXp = getCurrentLevelXp(uuid);
        int nextLevelRequirement = getXpForLevel(currentLevel + 1);

        return (float) currentLevelXp / nextLevelRequirement;
    }

    public double getTotalMultiplier(Player player) {
        if (player == null) return 1.0;

        PlayerData data = getPlayerData(player.getUniqueId());
        if (data == null) return 1.0;

        double totalMultiplier = 1.0;

        // Add event bonus if event manager exists
        if (plugin.getEventManager() != null) {
            double eventMultiplier = plugin.getEventManager().getCurrentMultiplier();
            totalMultiplier += (eventMultiplier - 1.0);
        }

        // Add booster bonus if active and not expired
        if (data.isBoosterActive() && System.currentTimeMillis() < data.getBoosterExpiry()) {
            totalMultiplier += (data.getBoosterMultiplier() - 1.0);
        }

        // Add armor bonus if enabled and armor listener exists
        if (plugin.getConfig().getBoolean("armor-multipliers.enabled", true) &&
                armorListener != null) {
            totalMultiplier += (armorListener.getArmorMultiplier(player) - 1.0);
        }

        // Check if RivalPets integration is active
        if (plugin.hasRivalPets()) {
            // Get multiplier from RivalPets buff instead of local data
            double petMultiplier = plugin.getRivalPetsBuff().getPlayerMultiplier(player);
            totalMultiplier += (petMultiplier - 1.0);
        } else {
            // Fallback to local pet multiplier data
            if (data.isPetActive()) {
                totalMultiplier += (data.getPetMultiplier() - 1.0);
            }
        }

        return Math.max(1.0, totalMultiplier);
    }

    /**
     * Set pet multiplier with option to specify if it's coming from RivalPets
     * to prevent circular references
     */
    public void setPetMultiplier(UUID uuid, double multiplier, boolean fromRivalPets) {
        if (uuid == null) return;


        PlayerData data = getPlayerData(uuid);
        data.setPetActive(true);
        data.setPetMultiplier(Math.max(1.0, multiplier));
        savePlayerData(uuid);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            // If RivalPets is available and this update isn't from RivalPets already,
            // update the buff there too
            if (plugin.hasRivalPets() && !fromRivalPets) {
                plugin.getRivalPetsBuff().setPlayerMultiplier(player, multiplier);
            }

            plugin.getDisplayManager().updateDisplays(player);

        }
    }

    // Original method for backward compatibility
    public void setPetMultiplier(UUID uuid, double multiplier) {
        setPetMultiplier(uuid, multiplier, false);
    }

    /**
     * Clear pet multiplier with option to specify if it's coming from RivalPets
     */
    public void clearPetMultiplier(UUID uuid, boolean fromRivalPets) {
        if (uuid == null) return;

        plugin.getLogger().info("Clearing pet multiplier for " + uuid +
                " (fromRivalPets=" + fromRivalPets + ")");

        PlayerData data = getPlayerData(uuid);
        data.setPetActive(false);
        data.setPetMultiplier(1.0);
        savePlayerData(uuid);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            plugin.getDisplayManager().updateDisplays(player);

        }
    }

    // Original method for backward compatibility
    public void clearPetMultiplier(UUID uuid) {
        clearPetMultiplier(uuid, false);
    }

    public double getPetMultiplier(UUID uuid) {
        if (uuid == null) return 1.0;

        PlayerData data = getPlayerData(uuid);
        return data.isPetActive() ? data.getPetMultiplier() : 1.0;
    }

    public boolean isWorldAllowed(String worldName) {
        if (worldName == null) return false;

        if (worldWhitelist.isEmpty()) {
            return !worldBlacklist.contains(worldName);
        }
        return worldWhitelist.contains(worldName);
    }

    public int getBasePerLevel() {
        return basePerLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean toggleActionBar(UUID uuid) {
        if (uuid == null) return false;

        PlayerData data = getPlayerData(uuid);
        boolean newState = !data.isActionBarEnabled();
        data.setActionBarEnabled(newState);
        savePlayerData(uuid);

        return newState;
    }

    public boolean isActionBarEnabled(UUID uuid) {
        if (uuid == null) return true;
        return getPlayerData(uuid).isActionBarEnabled();
    }

    /**
     * Get all player data for leaderboards
     */
    public Map<UUID, PlayerData> getAllPlayerData() {
        return new HashMap<>(playerDataCache);
    }
}
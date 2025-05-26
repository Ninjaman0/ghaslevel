package com.ninja.ghast.ghastLevels.model;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class PlayerData {
    private UUID uuid;
    private String name;

    // Level and XP
    private int levelPoints;
    private int level;
    private int currentLevelXp;
    private int totalXp;

    // Booster data
    private boolean boosterActive;
    private double boosterMultiplier;
    private long boosterExpiry;
    private Map<String, Long> boosterHistory;

    // Pet data
    private boolean petActive;
    private double petMultiplier;
    private String activePetId;

    // Display preferences
    private boolean actionBarEnabled;
    private boolean levelUpAnimationsEnabled;
    private boolean levelUpSoundsEnabled;

    // Statistics
    private int blocksMinedTotal;
    private int mobsKilledTotal;
    private long playTimeSeconds;
    private long lastLoginTime;
    private long lastLogoutTime;

    // Achievement tracking
    private Map<String, Boolean> unlockedAchievements;
    private Map<String, Integer> achievementProgress;

    public PlayerData() {
        // Initialize with default values
        this.levelPoints = 0;
        this.level = 0;
        this.currentLevelXp = 0;
        this.totalXp = 0;

        this.boosterActive = false;
        this.boosterMultiplier = 1.0;
        this.boosterExpiry = 0;
        this.boosterHistory = new HashMap<>();

        this.petActive = false;
        this.petMultiplier = 1.0;
        this.activePetId = null;

        this.actionBarEnabled = true;
        this.levelUpAnimationsEnabled = true;
        this.levelUpSoundsEnabled = true;

        this.blocksMinedTotal = 0;
        this.mobsKilledTotal = 0;
        this.playTimeSeconds = 0;
        this.lastLoginTime = 0;
        this.lastLogoutTime = 0;

        this.unlockedAchievements = new HashMap<>();
        this.achievementProgress = new HashMap<>();
    }

    // Getters and setters for all fields
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getLevelPoints() { return levelPoints; }
    public void setLevelPoints(int levelPoints) { this.levelPoints = levelPoints; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getCurrentLevelXp() { return currentLevelXp; }
    public void setCurrentLevelXp(int currentLevelXp) { this.currentLevelXp = currentLevelXp; }

    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int totalXp) { this.totalXp = totalXp; }

    public boolean isBoosterActive() { return boosterActive; }
    public void setBoosterActive(boolean boosterActive) { this.boosterActive = boosterActive; }

    public double getBoosterMultiplier() { return boosterMultiplier; }
    public void setBoosterMultiplier(double boosterMultiplier) { this.boosterMultiplier = boosterMultiplier; }

    public long getBoosterExpiry() { return boosterExpiry; }
    public void setBoosterExpiry(long boosterExpiry) { this.boosterExpiry = boosterExpiry; }

    public Map<String, Long> getBoosterHistory() { return boosterHistory; }
    public void setBoosterHistory(Map<String, Long> boosterHistory) { this.boosterHistory = boosterHistory; }

    public boolean isPetActive() { return petActive; }
    public void setPetActive(boolean petActive) { this.petActive = petActive; }

    public double getPetMultiplier() { return petMultiplier; }
    public void setPetMultiplier(double petMultiplier) { this.petMultiplier = petMultiplier; }

    public String getActivePetId() { return activePetId; }
    public void setActivePetId(String activePetId) { this.activePetId = activePetId; }

    public boolean isActionBarEnabled() { return actionBarEnabled; }
    public void setActionBarEnabled(boolean actionBarEnabled) { this.actionBarEnabled = actionBarEnabled; }

    public boolean isLevelUpAnimationsEnabled() { return levelUpAnimationsEnabled; }
    public void setLevelUpAnimationsEnabled(boolean enabled) { this.levelUpAnimationsEnabled = enabled; }

    public boolean isLevelUpSoundsEnabled() { return levelUpSoundsEnabled; }
    public void setLevelUpSoundsEnabled(boolean enabled) { this.levelUpSoundsEnabled = enabled; }

    public int getBlocksMinedTotal() { return blocksMinedTotal; }
    public void setBlocksMinedTotal(int blocksMinedTotal) { this.blocksMinedTotal = blocksMinedTotal; }
    public void incrementBlocksMined() { this.blocksMinedTotal++; }

    public int getMobsKilledTotal() { return mobsKilledTotal; }
    public void setMobsKilledTotal(int mobsKilledTotal) { this.mobsKilledTotal = mobsKilledTotal; }
    public void incrementMobsKilled() { this.mobsKilledTotal++; }

    public long getPlayTimeSeconds() { return playTimeSeconds; }
    public void setPlayTimeSeconds(long playTimeSeconds) { this.playTimeSeconds = playTimeSeconds; }

    public long getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(long lastLoginTime) { this.lastLoginTime = lastLoginTime; }

    public long getLastLogoutTime() { return lastLogoutTime; }
    public void setLastLogoutTime(long lastLogoutTime) { this.lastLogoutTime = lastLogoutTime; }

    public Map<String, Boolean> getUnlockedAchievements() { return unlockedAchievements; }
    public void setUnlockedAchievements(Map<String, Boolean> unlockedAchievements) {
        this.unlockedAchievements = unlockedAchievements;
    }

    public Map<String, Integer> getAchievementProgress() { return achievementProgress; }
    public void setAchievementProgress(Map<String, Integer> achievementProgress) {
        this.achievementProgress = achievementProgress;
    }

    // Utility methods
    public boolean hasBoosterExpired() {
        return !boosterActive || System.currentTimeMillis() >= boosterExpiry;
    }

    public void updatePlayTime() {
        if (lastLoginTime > 0) {
            long currentTime = System.currentTimeMillis();
            playTimeSeconds += (currentTime - lastLoginTime) / 1000;
            lastLoginTime = currentTime;
        }
    }

    public void unlockAchievement(String achievementId) {
        unlockedAchievements.put(achievementId, true);
    }

    public boolean hasAchievement(String achievementId) {
        return unlockedAchievements.getOrDefault(achievementId, false);
    }

    public void updateAchievementProgress(String achievementId, int progress) {
        achievementProgress.put(achievementId, progress);
    }

    public int getAchievementProgress(String achievementId) {
        return achievementProgress.getOrDefault(achievementId, 0);
    }
}
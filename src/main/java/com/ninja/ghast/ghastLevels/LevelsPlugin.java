package com.ninja.ghast.ghastLevels;

import com.ninja.ghast.ghastLevels.commands.LevelsCommand;
import com.ninja.ghast.ghastLevels.commands.LevelsTabCompleter;
import com.ninja.ghast.ghastLevels.integration.EssenceRivalPetsBuff;
import com.ninja.ghast.ghastLevels.listeners.*;
import com.ninja.ghast.ghastLevels.managers.*;
import com.ninja.ghast.ghastLevels.storage.StorageManager;
import me.rivaldev.rivalpets.api.RivalPetsAPI;
import com.ninja.ghast.ghastLevels.integration.EssenceRivalPetsBuff;
import com.ninja.ghast.ghastLevels.utils.MessageUtils;
import me.rivaldev.rivalpets.api.RivalPetsAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class LevelsPlugin extends JavaPlugin {
    private BoosterManager boosterManager;
    private StorageManager storageManager;
    private static LevelsPlugin instance;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private FileConfiguration levelConfig;
    private File levelFile;
    private BukkitTask maintenanceTask;

    private LevelManager levelManager;
    private DisplayManager displayManager;
    private EventManager eventManager;
    private WorldGuardManager worldGuardManager;
    private PlaceholderManager placeholderHook;
    private ArmorManager armorManager;
    private ArmorListener armorListener;
    private BlockListener blockListener;
    private MobListener mobListener;

    // New Managers
    private LevelAnimationManager levelAnimationManager;
    private WorldAccessManager worldAccessManager;
    private BlockLevelManager blockLevelManager;
    private TopPlayersManager topPlayersManager;
    private EssenceRivalPetsBuff rivalPetsBuff;
    @Override
    public void onLoad() {
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            WorldGuardManager.registerFlags(this);
        }
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveDefaultMessagesConfig();
        saveDefaultLevelConfig();

        // Initialize storage first to ensure data is ready
        storageManager = new StorageManager(this);

        // Initialize managers
        armorManager = new ArmorManager(this);
        armorListener = new ArmorListener(this, armorManager);
        levelManager = new LevelManager(this, armorListener);
        displayManager = new DisplayManager(this);
        boosterManager = new BoosterManager(this);
        eventManager = new EventManager(this);

        // Initialize block and mob listeners
        blockListener = new BlockListener(this);
        mobListener = new MobListener(this);

        // Initialize new managers
        levelAnimationManager = new LevelAnimationManager(this);
        worldAccessManager = new WorldAccessManager(this);
        blockLevelManager = new BlockLevelManager(this);
        topPlayersManager = new TopPlayersManager(this);

        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardManager = new WorldGuardManager(this);
            getLogger().info("WorldGuard found! Region protection enabled.");
        }

        // Set up maintenance task for storage
        maintenanceTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> storageManager.runMaintenance(),
                6000, 6000); // 5 minutes (6000 ticks)

        getCommand("levels").setExecutor(new LevelsCommand(this));
        getCommand("levels").setTabCompleter(new LevelsTabCompleter(this));

        // Register event listeners
        getServer().getPluginManager().registerEvents(blockListener, this);
        getServer().getPluginManager().registerEvents(mobListener, this);
        getServer().getPluginManager().registerEvents(armorListener, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderHook = new PlaceholderManager(this);
            placeholderHook.register();
            getLogger().info("PlaceholderAPI found! Placeholders registered.");
        }
        // Initialize RivalPets integration if available
        if (getServer().getPluginManager().getPlugin("RivalPets") != null) {
            setupRivalPetsIntegration();
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (getConfig().getBoolean("events.schedule.enabled", false)) {
                eventManager.setupScheduledEvents();
            }
        }, 100L);

        getLogger().info("LevelsPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all player data before shutdown
        if (levelManager != null) {
            getLogger().info("Saving all player data...");
            levelManager.saveAllPlayerData();
        }

        // Shutdown storage properly
        if (storageManager != null) {
            getLogger().info("Shutting down storage manager...");
            storageManager.shutdown();
        }

        // Shutdown TopPlayersManager
        if (topPlayersManager != null) {
            topPlayersManager.shutdown();
        }

        // Cancel all tasks
        getLogger().info("Cancelling scheduled tasks...");
        Bukkit.getScheduler().cancelTasks(this);

        // Unregister PlaceholderAPI if needed
        if (placeholderHook != null) {
            getLogger().info("Unregistering PlaceholderAPI hook...");
            placeholderHook.unregister();
        }

        getLogger().info("LevelsPlugin has been disabled!");
    }
    private void setupRivalPetsIntegration() {
        getLogger().info("RivalPets found! Setting up integration...");

        try {
            // Initialize our buff
            rivalPetsBuff = new EssenceRivalPetsBuff(this);

            // Register the buff with RivalPets
            RivalPetsAPI.getApi().registerBuff(rivalPetsBuff, "GhastEssence");

            getLogger().info("Successfully registered essence multiplier buff with RivalPets!");
        } catch (Exception e) {
            getLogger().severe("Failed to register with RivalPets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public EssenceRivalPetsBuff getRivalPetsBuff() {
        return rivalPetsBuff;
    }

    public boolean hasRivalPets() {
        return rivalPetsBuff != null && Bukkit.getPluginManager().getPlugin("RivalPets") != null;
    }
    public void reload() {
        getLogger().info("Reloading LevelsPlugin configuration...");

        reloadConfig();
        reloadMessagesConfig();
        reloadLevelConfig();

        // Check if storage type has changed
        String newStorageType = getConfig().getString("storage.type", "yaml").toLowerCase();
        StorageManager.StorageType currentType = storageManager.getCurrentStorageType();

        boolean storageChanged = (newStorageType.equals("sqlite") && currentType != StorageManager.StorageType.SQLITE) ||
                (!newStorageType.equals("sqlite") && currentType != StorageManager.StorageType.YAML);

        if (storageChanged) {
            getLogger().info("Storage type changed, reinitializing storage...");

            // Save all player data first
            if (levelManager != null) {
                levelManager.saveAllPlayerData();
            }

            // Shutdown current storage
            storageManager.shutdown();

            // Reinitialize storage with new type
            storageManager = new StorageManager(this);
        }

        // Reload each manager
        if (armorManager != null) armorManager.reload();
        if (armorListener != null) armorListener.reload();
        if (levelManager != null) levelManager.reload();
        if (displayManager != null) displayManager.reload();
        if (boosterManager != null) boosterManager.reload();
        if (eventManager != null) eventManager.reload();
        if (worldGuardManager != null) worldGuardManager.reload();

        // Reload new managers
        if (levelAnimationManager != null) levelAnimationManager.reload();
        if (worldAccessManager != null) worldAccessManager.reload();
        if (blockLevelManager != null) blockLevelManager.reload();
        if (topPlayersManager != null) {
            topPlayersManager.updateTopPlayers();
        }

        // Reload listeners to update their block and mob mappings
        if (blockListener != null) blockListener.reload();
        if (mobListener != null) mobListener.reload();

        getLogger().info("LevelsPlugin reload complete!");
    }

    private void saveDefaultMessagesConfig() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void saveDefaultLevelConfig() {
        levelFile = new File(getDataFolder(), "level.yml");
        if (!levelFile.exists()) {
            saveResource("level.yml", false);
        }
        levelConfig = YamlConfiguration.loadConfiguration(levelFile);
    }

    public void reloadMessagesConfig() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reloadLevelConfig() {
        levelConfig = YamlConfiguration.loadConfiguration(levelFile);
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getLevelConfig() {
        return levelConfig;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public void saveMessagesConfig() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save messages.yml", e);
        }
    }

    public void saveLevelConfig() {
        try {
            levelConfig.save(levelFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save level.yml", e);
        }
    }

    public static LevelsPlugin getInstance() {
        return instance;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public DisplayManager getDisplayManager() {
        return displayManager;
    }

    public BoosterManager getBoosterManager() {
        return boosterManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public WorldGuardManager getWorldGuardManager() {
        return worldGuardManager;
    }

    public ArmorManager getArmorManager() {
        return armorManager;
    }

    public ArmorListener getArmorListener() {
        return armorListener;
    }

    public LevelAnimationManager getLevelAnimationManager() {
        return levelAnimationManager;
    }

    public WorldAccessManager getWorldAccessManager() {
        return worldAccessManager;
    }

    public BlockLevelManager getBlockLevelManager() {
        return blockLevelManager;
    }

    public TopPlayersManager getTopPlayersManager() {
        return topPlayersManager;
    }

    public boolean hasWorldGuard() {
        return worldGuardManager != null;
    }

    public boolean hasPlaceholderAPI() {
        return placeholderHook != null;
    }
}
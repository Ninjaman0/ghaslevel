package com.ninja.ghast.ghastLevels.storage;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.model.PlayerData;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class SQLiteStorage implements StorageProvider {
    private final LevelsPlugin plugin;
    private final SQLiteConnectionPool connectionPool;
    private boolean initialized = false;
    private final String dbPath;

    public SQLiteStorage(LevelsPlugin plugin) {
        this.plugin = plugin;

        // Get custom filename from config if specified
        String fileName = plugin.getConfig().getString("storage.sqlite.filename", "data.db");
        this.dbPath = plugin.getDataFolder().getAbsolutePath() + File.separator + fileName;

        // Ensure parent directory exists
        File dbFile = new File(dbPath);
        if (!dbFile.getParentFile().exists()) {
            if (dbFile.getParentFile().mkdirs()) {
                plugin.getLogger().info("Created directory: " + dbFile.getParent());
            } else {
                plugin.getLogger().severe("Failed to create directory: " + dbFile.getParent());
            }
        }

        this.connectionPool = new SQLiteConnectionPool(dbPath);
    }

    @Override
    public void initialize() {
        plugin.getLogger().info("Initializing SQLite storage at: " + dbPath);
        int retries = 3;
        while (retries-- > 0) {
            try (Connection connection = connectionPool.getConnection();
                 Statement stmt = connection.createStatement()) {

                // Create tables with proper schema
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_data (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "name TEXT NOT NULL, " +
                        "level_points INTEGER NOT NULL DEFAULT 0, " +
                        "level INTEGER NOT NULL DEFAULT 0, " +
                        "pet_active BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "pet_multiplier REAL NOT NULL DEFAULT 1.0, " +
                        "booster_active BOOLEAN NOT NULL DEFAULT FALSE, " +
                        "booster_multiplier REAL NOT NULL DEFAULT 1.0, " +
                        "booster_expiry INTEGER NOT NULL DEFAULT 0, " +
                        "action_bar_enabled BOOLEAN NOT NULL DEFAULT TRUE)");

                // Create indexes for better performance
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_name ON player_data(name)");

                initialized = true;
                plugin.getLogger().info("SQLite storage initialized successfully");
                return;

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite storage (retries left: " + retries + ")", e);

                if (retries > 0) {
                    try {
                        Thread.sleep(1000); // Wait before retry
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        plugin.getLogger().severe("Failed to initialize SQLite storage after multiple attempts");
    }

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        if (!initialized) {
            plugin.getLogger().warning("Attempted to load player data before initialization. Creating empty data.");
            PlayerData data = new PlayerData();
            data.setUuid(uuid);
            return data;
        }

        String sql = "SELECT * FROM player_data WHERE uuid = ?";
        PlayerData data = new PlayerData();
        data.setUuid(uuid);

        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String playerName = rs.getString("name");
                        // If the name is "Unknown", try to get the actual name
                        if (playerName == null || playerName.equalsIgnoreCase("Unknown")) {
                            String actualName = plugin.getServer().getOfflinePlayer(uuid).getName();
                            if (actualName != null) {
                                playerName = actualName;
                                // Update the name in the database
                                updatePlayerName(connection, uuid, actualName);
                            } else {
                                playerName = "Unknown";
                            }
                        }
                        data.setName(playerName);
                        data.setLevelPoints(rs.getInt("level_points"));
                        data.setLevel(rs.getInt("level"));
                        data.setPetActive(rs.getBoolean("pet_active"));
                        data.setPetMultiplier(rs.getDouble("pet_multiplier"));
                        data.setBoosterActive(rs.getBoolean("booster_active"));
                        data.setBoosterMultiplier(rs.getDouble("booster_multiplier"));
                        data.setBoosterExpiry(rs.getLong("booster_expiry"));
                        data.setActionBarEnabled(rs.getBoolean("action_bar_enabled"));
                    } else {
                        // New player, try to get their name
                        String playerName = plugin.getServer().getOfflinePlayer(uuid).getName();
                        if (playerName != null) {
                            data.setName(playerName);
                        } else {
                            data.setName("Unknown");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, e);
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }

        return data;
    }

    private void updatePlayerName(Connection connection, UUID uuid, String name) {
        if (connection == null || name == null) return;

        String sql = "UPDATE player_data SET name = ? WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update player name for " + uuid, e);
        }
    }

    @Override
    public void savePlayerData(PlayerData playerData) {
        if (!initialized) {
            plugin.getLogger().warning("Attempted to save player data before initialization");
            return;
        }

        // Before saving, ensure we have the most up-to-date player name
        if (playerData.getName() == null || playerData.getName().equalsIgnoreCase("Unknown")) {
            String playerName = plugin.getServer().getOfflinePlayer(playerData.getUuid()).getName();
            if (playerName != null) {
                playerData.setName(playerName);
            }
        }

        String sql = "INSERT OR REPLACE INTO player_data " +
                "(uuid, name, level_points, level, " +
                "pet_active, pet_multiplier, booster_active, booster_multiplier, booster_expiry, action_bar_enabled) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerData.getUuid().toString());
                stmt.setString(2, playerData.getName() != null ? playerData.getName() : "Unknown");
                stmt.setInt(3, playerData.getLevelPoints());
                stmt.setInt(4, playerData.getLevel());
                stmt.setBoolean(5, playerData.isPetActive());
                stmt.setDouble(6, playerData.getPetMultiplier());
                stmt.setBoolean(7, playerData.isBoosterActive());
                stmt.setDouble(8, playerData.getBoosterMultiplier());
                stmt.setLong(9, playerData.getBoosterExpiry());
                stmt.setBoolean(10, playerData.isActionBarEnabled());

                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + playerData.getUuid(), e);
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
    }

    @Override
    public void removePlayerData(UUID uuid) {
        if (!initialized) {
            plugin.getLogger().warning("Attempted to remove player data before initialization");
            return;
        }

        String sql = "DELETE FROM player_data WHERE uuid = ?";

        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
                plugin.getLogger().info("Removed player data for " + uuid);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to remove player data for " + uuid, e);
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
    }

    @Override
    public void shutdown() {
        try {
            connectionPool.closeAllConnections();
            plugin.getLogger().info("Closed all SQLite connections");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error closing SQLite connections", e);
        }
    }

    @Override
    public boolean validateConnection() {
        if (!initialized) {
            return false;
        }

        Connection connection = null;
        try {
            connection = connectionPool.getConnection();
            try (Statement stmt = connection.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT 1")) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "SQLite connection validation failed", e);
            return false;
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
        }
    }
}
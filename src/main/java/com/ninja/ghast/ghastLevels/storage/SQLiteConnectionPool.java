package com.ninja.ghast.ghastLevels.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLiteConnectionPool {
    private final String connectionUrl;
    private final BlockingQueue<Connection> connectionPool;
    private final int maxPoolSize;
    private int currentPoolSize;
    private static final Logger logger = Logger.getLogger(SQLiteConnectionPool.class.getName());

    public SQLiteConnectionPool(String databasePath) {
        this.connectionUrl = "jdbc:sqlite:" + databasePath;
        this.maxPoolSize = 10;
        this.connectionPool = new LinkedBlockingQueue<>(maxPoolSize);
        this.currentPoolSize = 0;
    }

    public synchronized Connection getConnection() throws SQLException {
        Connection connection = null;

        // First try to get a connection from the pool
        while (!connectionPool.isEmpty()) {
            try {
                connection = connectionPool.poll();
                if (isValidConnection(connection)) {
                    return connection;
                } else {
                    // Close invalid connection
                    try {
                        if (connection != null && !connection.isClosed()) {
                            connection.close();
                        }
                    } catch (SQLException e) {
                        logger.log(Level.WARNING, "Error closing invalid connection", e);
                    }
                    currentPoolSize--;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error retrieving connection from pool", e);
                currentPoolSize--;
            }
        }

        // If no valid connection in pool, create a new one if under max size
        if (currentPoolSize < maxPoolSize) {
            currentPoolSize++;
            return createNewConnection();
        }

        // If we reach here, pool is exhausted and at max size
        // Wait for a connection to become available
        try {
            connection = connectionPool.poll(30, TimeUnit.SECONDS);
            if (connection != null && isValidConnection(connection)) {
                return connection;
            } else {
                // Force create a new connection if timeout occurs
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        logger.log(Level.WARNING, "Error closing timed out connection", e);
                    }
                }
                return createNewConnection();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a database connection", e);
        }
    }

    public void releaseConnection(Connection connection) {
        if (connection != null) {
            try {
                // Only return valid connections to the pool
                if (!connection.isClosed() && connection.isValid(1)) {
                    // Clear any leftover transactions
                    if (!connection.getAutoCommit()) {
                        connection.setAutoCommit(true);
                    }

                    // Offer to pool or close if pool is full
                    if (!connectionPool.offer(connection)) {
                        connection.close();
                        currentPoolSize--;
                    }
                } else {
                    // Connection is invalid, close it
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        logger.log(Level.WARNING, "Error closing invalid connection during release", e);
                    }
                    currentPoolSize--;
                }
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Error releasing connection to pool", e);
                try {
                    connection.close();
                } catch (SQLException ex) {
                    logger.log(Level.WARNING, "Error closing invalid connection", ex);
                }
                currentPoolSize--;
            }
        }
    }

    private Connection createNewConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection connection = DriverManager.getConnection(connectionUrl);

            // Test the connection immediately
            try (Statement testStmt = connection.createStatement()) {
                testStmt.execute("PRAGMA journal_mode=WAL");  // Use WAL mode for better concurrency
                testStmt.execute("PRAGMA synchronous=NORMAL"); // Balance durability and performance
                testStmt.execute("PRAGMA foreign_keys=ON");   // Enable foreign key constraints
                testStmt.execute("SELECT 1");                 // Test query
            }

            connection.setAutoCommit(true);
            return connection;
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
    }

    private boolean isValidConnection(Connection connection) {
        if (connection == null) return false;

        try {
            return !connection.isClosed() && connection.isValid(1); // 1 second timeout
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error validating connection", e);
            return false;
        }
    }

    public synchronized void closeAllConnections() {
        for (Connection connection : connectionPool) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Error closing connection during shutdown", e);
            }
        }
        connectionPool.clear();
        currentPoolSize = 0;
    }
}
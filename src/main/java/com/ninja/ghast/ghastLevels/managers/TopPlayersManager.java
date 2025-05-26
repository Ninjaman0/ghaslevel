    package com.ninja.ghast.ghastLevels.managers;

    import com.ninja.ghast.ghastLevels.LevelsPlugin;
    import com.ninja.ghast.ghastLevels.model.PlayerData;
    import org.bukkit.Bukkit;
    import org.bukkit.scheduler.BukkitTask;

    import java.util.*;
    import java.util.concurrent.ConcurrentSkipListMap;

    public class TopPlayersManager {
        private final LevelsPlugin plugin;
        private final NavigableMap<Integer, UUID> topPlayers;
        private BukkitTask updateTask;
        private static final int UPDATE_INTERVAL = 300; // 5 minutes in seconds
        private static final int MAX_TOP_PLAYERS = 10;

        public TopPlayersManager(LevelsPlugin plugin) {
            this.plugin = plugin;
            this.topPlayers = new ConcurrentSkipListMap<>(Collections.reverseOrder());
            startUpdateTask();
        }

        private void startUpdateTask() {
            updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::updateTopPlayers, 20L, UPDATE_INTERVAL * 20L);
        }

        public void updateTopPlayers() {
            Map<UUID, PlayerData> allPlayers = plugin.getLevelManager().getAllPlayerData();
            NavigableMap<Integer, UUID> newTopPlayers = new TreeMap<>(Collections.reverseOrder());

            allPlayers.forEach((uuid, data) -> {
                newTopPlayers.put(data.getLevel(), uuid);
                if (newTopPlayers.size() > MAX_TOP_PLAYERS) {
                    newTopPlayers.remove(newTopPlayers.lastKey());
                }
            });

            topPlayers.clear();
            topPlayers.putAll(newTopPlayers);
        }

        public List<Map.Entry<Integer, UUID>> getTopPlayers() {
            List<Map.Entry<Integer, UUID>> top = new ArrayList<>(topPlayers.entrySet());
            return top.subList(0, Math.min(top.size(), MAX_TOP_PLAYERS));
        }

        public String getTopPlayerName(int position) {
            if (position < 1 || position > MAX_TOP_PLAYERS) return "None";

            List<Map.Entry<Integer, UUID>> top = getTopPlayers();
            if (position > top.size()) return "None";

            UUID uuid = top.get(position - 1).getValue();
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            return name != null ? name : "Unknown";
        }

        public int getTopPlayerLevel(int position) {
            if (position < 1 || position > MAX_TOP_PLAYERS) return 0;

            List<Map.Entry<Integer, UUID>> top = getTopPlayers();
            if (position > top.size()) return 0;

            return top.get(position - 1).getKey();
        }

        public void shutdown() {
            if (updateTask != null) {
                updateTask.cancel();
            }
        }
    }
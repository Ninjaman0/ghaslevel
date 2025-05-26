package com.ninja.ghast.ghastLevels.managers;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EventManager {

    private final LevelsPlugin plugin;
    private final Map<String, EventData> events = new HashMap<>();

    private String currentEvent = null;
    private BukkitTask eventTask = null;
    private BukkitTask bossBarUpdateTask = null;
    private BossBar eventBar = null;
    private long eventStartTime;

    private final Map<String, BukkitTask> scheduledEvents = new HashMap<>();

    public EventManager(LevelsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        // Clear existing events
        events.clear();

        // Cancel current event if running
        if (currentEvent != null) {
            stopEvent();
        }

        // Cancel all scheduled events
        for (BukkitTask task : scheduledEvents.values()) {
            task.cancel();
        }
        scheduledEvents.clear();

        // Load events from config
        ConfigurationSection eventsSection = plugin.getConfig().getConfigurationSection("events");
        if (eventsSection == null) return;

        for (String eventId : eventsSection.getKeys(false)) {
            // Skip schedule section
            if (eventId.equals("schedule")) continue;

            ConfigurationSection eventSection = eventsSection.getConfigurationSection(eventId);
            if (eventSection == null) continue;

            String name = eventSection.getString("name", eventId);
            double multiplier = eventSection.getDouble("multiplier", 2.0);
            long duration = eventSection.getLong("duration", 3600);

            events.put(eventId, new EventData(name, multiplier, duration));
        }
    }

    public void setupScheduledEvents() {
        ConfigurationSection scheduleSection = plugin.getConfig().getConfigurationSection("events.schedule");
        if (scheduleSection == null || !scheduleSection.getBoolean("enabled", false)) return;

        ConfigurationSection eventsSection = scheduleSection.getConfigurationSection("events");
        if (eventsSection == null) return;

        for (String key : eventsSection.getKeys(false)) {
            ConfigurationSection eventSchedule = eventsSection.getConfigurationSection(key);
            if (eventSchedule == null) continue;

            String eventId = eventSchedule.getString("event");
            if (eventId == null || !events.containsKey(eventId)) {
                plugin.getLogger().warning("Invalid event ID in schedule: " + eventId);
                continue;
            }

            List<String> dayStrings = eventSchedule.getStringList("days");
            Set<DayOfWeek> days = new HashSet<>();

            for (String dayString : dayStrings) {
                try {
                    days.add(DayOfWeek.valueOf(dayString.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid day in schedule: " + dayString);
                }
            }

            if (days.isEmpty()) {
                plugin.getLogger().warning("No valid days for scheduled event: " + eventId);
                continue;
            }

            String timeString = eventSchedule.getString("time", "12:00");
            LocalTime time;
            try {
                time = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid time format for scheduled event: " + timeString);
                continue;
            }

            scheduleEvent(eventId, days, time);
        }
    }

    private void scheduleEvent(String eventId, Set<DayOfWeek> days, LocalTime time) {
        // Calculate delay until next occurrence
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = null;

        // Check today
        if (now.toLocalTime().isBefore(time) && days.contains(now.getDayOfWeek())) {
            nextRun = now.toLocalDate().atTime(time);
        }

        // If not today, find next day
        if (nextRun == null) {
            for (int i = 1; i <= 7; i++) {
                LocalDateTime checkDate = now.plusDays(i);
                if (days.contains(checkDate.getDayOfWeek())) {
                    nextRun = checkDate.toLocalDate().atTime(time);
                    break;
                }
            }
        }

        if (nextRun == null) {
            plugin.getLogger().warning("Could not schedule event: " + eventId);
            return;
        }

        // Calculate seconds until next run
        long secondsUntil = now.until(nextRun, java.time.temporal.ChronoUnit.SECONDS);

        // Schedule task
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Start the event
            startEvent(eventId);

            // Reschedule for next week
            scheduleEvent(eventId, days, time);
        }, secondsUntil * 20); // Convert to ticks

        scheduledEvents.put(eventId, task);

        plugin.getLogger().info("Scheduled event " + eventId + " to run in " +
                MessageUtils.formatTime(secondsUntil) + " at " + time);
    }

    public boolean startEvent(String eventId) {
        // Check if event exists
        if (!events.containsKey(eventId)) {
            return false;
        }

        // Check if an event is already running
        if (currentEvent != null) {
            return false;
        }

        EventData event = events.get(eventId);
        currentEvent = eventId;
        eventStartTime = System.currentTimeMillis();

        // Create boss bar
        eventBar = Bukkit.createBossBar(
                MessageUtils.translateColors("&6" + event.name + " &7- &6" + event.multiplier + "x &7essence"),
                BarColor.YELLOW,
                BarStyle.SOLID
        );

        // Add all online players to the boss bar
        for (Player player : Bukkit.getOnlinePlayers()) {
            eventBar.addPlayer(player);
        }

        // Schedule event end
        eventTask = Bukkit.getScheduler().runTaskLater(plugin,
                this::stopEvent,
                event.duration * 20 // Convert to ticks
        );

        // Schedule boss bar updates
        bossBarUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (currentEvent == null || eventBar == null) {
                if (bossBarUpdateTask != null) {
                    bossBarUpdateTask.cancel();
                    bossBarUpdateTask = null;
                }
                return;
            }

            long timeLeft = getCurrentEventTimeRemaining();
            if (timeLeft <= 0) {
                if (bossBarUpdateTask != null) {
                    bossBarUpdateTask.cancel();
                    bossBarUpdateTask = null;
                }
                return;
            }

            // Update progress
            eventBar.setProgress(Math.max(0, Math.min(1, timeLeft / (double) event.duration)));

            // Update title
            eventBar.setTitle(MessageUtils.translateColors(
                    "&6" + event.name + " &7- &6" + event.multiplier + "x &7- " +
                            MessageUtils.formatTime(timeLeft)
            ));
        }, 0L, 20L);

        // Broadcast event start
        String message = MessageUtils.getMessageNoPrefix("event.started")
                .replace("{name}", event.name)
                .replace("{multiplier}", String.format("%.1f", event.multiplier))
                .replace("{duration}", MessageUtils.formatTime(event.duration));

        Bukkit.broadcastMessage(MessageUtils.translateColors(plugin.getMessagesConfig().getString("prefix", "&8[&bLevels&8] ") + message));

        return true;
    }

    public void stopEvent() {
        if (currentEvent == null) return;

        // Get event data
        EventData event = events.get(currentEvent);

        // Remove boss bar
        if (eventBar != null) {
            eventBar.removeAll();
            eventBar = null;
        }

        // Cancel tasks
        if (eventTask != null) {
            eventTask.cancel();
            eventTask = null;
        }

        if (bossBarUpdateTask != null) {
            bossBarUpdateTask.cancel();
            bossBarUpdateTask = null;
        }

        // Broadcast event end
        if (event != null) {
            String message = MessageUtils.getMessageNoPrefix("event.ended").replace("{name}", event.name);
            Bukkit.broadcastMessage(MessageUtils.translateColors(plugin.getMessagesConfig().getString("prefix", "&8[&bLevels&8] ") + message));
        }

        // Reset current event
        currentEvent = null;
    }

    public double getCurrentMultiplier() {
        if (currentEvent == null) return 1.0;

        EventData event = events.get(currentEvent);
        return event != null ? event.multiplier : 1.0;
    }

    public boolean isEventRunning() {
        return currentEvent != null;
    }

    public String getCurrentEventName() {
        if (currentEvent == null) return null;

        EventData event = events.get(currentEvent);
        return event != null ? event.name : null;
    }

    public long getCurrentEventTimeRemaining() {
        if (currentEvent == null || !plugin.isEnabled()) return 0;

        EventData event = events.get(currentEvent);
        if (event == null) return 0;

        long elapsed = System.currentTimeMillis() - eventStartTime;
        long remaining = event.duration - (elapsed / 1000);
        return Math.max(0, remaining);
    }

    public Set<String> getEventIds() {
        return events.keySet();
    }

    public Map<String, String> getEventNames() {
        Map<String, String> eventNames = new HashMap<>();
        for (Map.Entry<String, EventData> entry : events.entrySet()) {
            eventNames.put(entry.getKey(), entry.getValue().name);
        }
        return eventNames;
    }

    private static class EventData {
        private final String name;
        private final double multiplier;
        private final long duration;

        public EventData(String name, double multiplier, long duration) {
            this.name = name;
            this.multiplier = multiplier;
            this.duration = duration;
        }
    }
}
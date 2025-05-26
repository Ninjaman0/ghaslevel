package com.ninja.ghast.ghastLevels.utils;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MessageUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final LevelsPlugin plugin = LevelsPlugin.getInstance();

    public static String getMessage(String path) {
        FileConfiguration messages = plugin.getMessagesConfig();
        String prefix = messages.getString("prefix", "&8[&bLevels&8] ");
        String message = messages.getString(path);

        if (message == null) {
            return translateColors(prefix + "&cMissing message: " + path);
        }

        return translateColors(prefix + message);
    }

    public static String getMessageNoPrefix(String path) {
        FileConfiguration messages = plugin.getMessagesConfig();
        String message = messages.getString(path);

        if (message == null) {
            return translateColors("&cMissing message: " + path);
        }

        return translateColors(message);
    }

    public static void sendMessage(Player player, String path, Map<String, String> placeholders) {
        if (player == null) return;
        String message = getMessage(path);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        player.sendMessage(message);
    }

    public static void sendMessage(Player player, String path) {
        if (player == null) return;
        player.sendMessage(getMessage(path));
    }

    public static Map<String, String> placeholders() {
        return new HashMap<>();
    }

    public static String translateColors(String text) {
        if (text == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexColor = matcher.group(1);
            try {
                String replacement = net.md_5.bungee.api.ChatColor.of("#" + hexColor).toString();
                matcher.appendReplacement(buffer, replacement);
            } catch (Exception e) {
                // Fallback for servers without RGB support
                matcher.appendReplacement(buffer, ChatColor.WHITE.toString());
            }
        }

        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static List<String> translateColors(List<String> text) {
        if (text == null) return new ArrayList<>();
        return text.stream().map(MessageUtils::translateColors).collect(Collectors.toList());
    }

    public static String stripColors(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(translateColors(text));
    }

    public static List<String> stripColors(List<String> text) {
        if (text == null) return new ArrayList<>();
        return text.stream().map(MessageUtils::stripColors).collect(Collectors.toList());
    }

    public static String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m " + (seconds % 60) + "s";
        } else {
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }
    }

    public static void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        if (sender == null) return;
        String message = getMessage(path);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        sender.sendMessage(message);
    }

    public static void sendMessage(CommandSender sender, String path) {
        if (sender == null) return;
        sender.sendMessage(getMessage(path));
    }

    public static String createProgressBar(double progress, int length, String filledChar, String emptyChar) {
        if (filledChar == null) filledChar = "■";
        if (emptyChar == null) emptyChar = "□";

        int filled = (int) Math.round(progress * length);
        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < length; i++) {
            if (i < filled) {
                bar.append(filledChar);
            } else {
                bar.append(emptyChar);
            }
        }

        return bar.toString();
    }
}
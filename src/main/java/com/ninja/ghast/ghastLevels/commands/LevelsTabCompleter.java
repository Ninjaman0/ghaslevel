package com.ninja.ghast.ghastLevels.commands;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completer for the LevelsPlugin commands
 * Provides context-sensitive tab completion for all plugin commands
 */
public class LevelsTabCompleter implements TabCompleter {

    private final LevelsPlugin plugin;
    private final ArmorCommands armorCommands;

    public LevelsTabCompleter(LevelsPlugin plugin) {
        this.plugin = plugin;
        this.armorCommands = new ArmorCommands(plugin, plugin.getArmorManager());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First level commands
            List<String> commands = new ArrayList<>();

            // Add commands based on permissions
            if (sender.hasPermission("levels.help")) commands.add("help");
            if (sender.hasPermission("levels.get")) commands.add("get");
            if (sender.hasPermission("levels.give")) commands.add("give");
            if (sender.hasPermission("levels.take")) commands.add("take");
            if (sender.hasPermission("levels.set")) commands.add("set");
            if (sender.hasPermission("levels.reload")) commands.add("reload");
            if (sender.hasPermission("levels.event.start") ||
                    sender.hasPermission("levels.event.stop")) commands.add("event");
            if (sender.hasPermission("levels.armor.list") ||
                    sender.hasPermission("levels.armor.give")) commands.add("armor");
            if (sender.hasPermission("levels.pet")) commands.add("pet");
            if (sender.hasPermission("levels.booster")) commands.add("booster");
            if (sender.hasPermission("levels.multiplier")) commands.add("multiplier");
            if (sender.hasPermission("levels.bar")) commands.add("bar");
            commands.add("top");

            return StringUtil.copyPartialMatches(args[0], commands, completions);
        }

        // Handle different subcommands
        switch (args[0].toLowerCase()) {
            case "armor":
                return handleArmorTabComplete(sender, args);
            case "get":
            case "give":
            case "take":
            case "set":
                return handlePlayerTargetTabComplete(sender, args);
            case "event":
                return handleEventTabComplete(sender, args);
            case "pet":
                return handlePetTabComplete(sender, args);
            case "booster":
                return handleBoosterTabComplete(sender, args);
            case "top":
                if (args.length == 2) {
                    return Arrays.asList("1", "2", "3"); // Page suggestions
                }
                break;
        }

        return completions;
    }

    /**
     * Handles tab completion for armor-related commands
     */
    private List<String> handleArmorTabComplete(CommandSender sender, String[] args) {
        // Delegate to ArmorCommands for context-specific tab completion
        if (args.length >= 2) {
            return armorCommands.getTabCompletions(args);
        }
        return Collections.emptyList();
    }

    /**
     * Handles tab completion for commands that target players
     */
    private List<String> handlePlayerTargetTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Return online player names
            return getOnlinePlayerNames();
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Suggest "level" as an option for /levels give <player> level <amount>
            return StringUtil.copyPartialMatches(args[2], Collections.singletonList("level"), completions);
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give") && args[2].equalsIgnoreCase("level")) {
            // Suggest some reasonable level values
            return Arrays.asList("1", "5", "10", "25", "50", "100");
        } else if (args.length == 3) {
            // Suggest some reasonable point/level values
            return Arrays.asList("100", "500", "1000", "5000", "10000");
        }

        return completions;
    }

    /**
     * Handles tab completion for event-related commands
     */
    private List<String> handleEventTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            List<String> subCommands = new ArrayList<>();

            if (sender.hasPermission("levels.event.start")) {
                subCommands.add("start");
                subCommands.add("list");
                subCommands.add("status");
            }

            if (sender.hasPermission("levels.event.stop")) {
                subCommands.add("stop");
            }

            return StringUtil.copyPartialMatches(args[1], subCommands, completions);
        } else if (args.length == 3 && args[1].equalsIgnoreCase("start")) {
            // Return available event IDs
            return StringUtil.copyPartialMatches(args[2],
                    new ArrayList<>(plugin.getEventManager().getEventIds()), completions);
        }

        return completions;
    }

    /**
     * Handles tab completion for pet-related commands
     */
    private List<String> handlePetTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Suggest player names
            return getOnlinePlayerNames();
        } else if (args.length == 3) {
            List<String> actions = Arrays.asList("start", "stop");
            return StringUtil.copyPartialMatches(args[2], actions, completions);
        } else if (args.length == 4 && args[2].equalsIgnoreCase("start")) {
            // Suggest some reasonable multiplier values
            return Arrays.asList("1.2", "1.5", "2.0", "2.5", "3.0");
        }

        return completions;
    }

    /**
     * Handles tab completion for booster-related commands
     */
    private List<String> handleBoosterTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Suggest player names
            return getOnlinePlayerNames();
        } else if (args.length == 3) {
            // Suggest multiplier values
            return Arrays.asList("1.5", "2.0", "3.0", "5.0");
        } else if (args.length == 4) {
            // Suggest duration values in seconds
            return Arrays.asList("300", "1800", "3600", "7200", "86400");
        }

        return completions;
    }

    /**
     * Gets a list of online player names for tab completion
     */
    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }
}
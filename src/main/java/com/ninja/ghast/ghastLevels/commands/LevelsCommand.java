package com.ninja.ghast.ghastLevels.commands;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LevelsCommand implements CommandExecutor {

    private final LevelsPlugin plugin;
    private final ArmorCommands armorCommands;

    public LevelsCommand(LevelsPlugin plugin) {
        this.plugin = plugin;
        this.armorCommands = new ArmorCommands(plugin, plugin.getArmorManager());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return showHelp(sender);
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "armor":
                return armorCommands.handleCommand(sender, args);
            case "help":
                return showHelp(sender);
            case "get":
                return handleGet(sender, args);
            case "give":
                return handleGive(sender, args);
            case "take":
                return handleTake(sender, args);
            case "set":
                return handleSet(sender, args);
            case "reload":
                return handleReload(sender);
            case "event":
                return handleEvent(sender, args);
            case "booster":
                return handleBooster(sender, args);
            case "multiplier":
                return handleMultiplier(sender, args);
            case "pet":
                return handlePet(sender, args);
            case "bar":
                return handleBar(sender);
            case "top":
                return handleTop(sender, args);
            default:
                sendMessage(sender, "command.invalid-args");
                return false;
        }
    }

    private boolean handleTop(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "levels.top")) return false;

        // Force update top players
        plugin.getTopPlayersManager().updateTopPlayers();

        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) page = 1;
            } catch (NumberFormatException ignored) {}
        }

        sender.sendMessage(MessageUtils.translateColors("&b=== Top Players ==="));

        List<Map.Entry<Integer, UUID>> topPlayers = plugin.getTopPlayersManager().getTopPlayers();
        int position = (page - 1) * 10 + 1;

        for (Map.Entry<Integer, UUID> entry : topPlayers) {
            String playerName = Bukkit.getOfflinePlayer(entry.getValue()).getName();
            if (playerName == null) playerName = "Unknown";

            sender.sendMessage(MessageUtils.translateColors(
                    String.format("&e%d. &7%s &8- &bLevel %d",
                            position++,
                            playerName,
                            entry.getKey())
            ));
        }

        return true;
    }

    private boolean handleBar(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "command.player-only");
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("levels.bar")) {
            sendMessage(player, "levels.bar.no-permission");
            return false;
        }

        boolean enabled = plugin.getLevelManager().toggleActionBar(player.getUniqueId());

        if (enabled) {
            sendMessage(player, "levels.bar.enabled");
        } else {
            sendMessage(player, "levels.bar.disabled");
        }

        return true;
    }

    private boolean handlePet(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "levels.pet")) return false;

        if (args.length < 3) {
            sendMessage(sender, "command.invalid-args");
            return false;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sendMessage(sender, "command.player-not-found");
            return false;
        }

        String action = args[2].toLowerCase();

        if (action.equals("start")) {
            if (args.length < 4) {
                sendMessage(sender, "command.invalid-args");
                return false;
            }

            double multiplier;
            try {
                multiplier = Double.parseDouble(args[3]);
                if (multiplier <= 1.0) {
                    sendMessage(sender, "pet.invalid-multiplier");
                    return false;
                }
            } catch (NumberFormatException e) {
                sendMessage(sender, "pet.invalid-multiplier");
                return false;
            }

            plugin.getLevelManager().setPetMultiplier(target.getUniqueId(), multiplier);
            sender.sendMessage(MessageUtils.translateColors("&aSet pet multiplier for &e" + target.getName() + " &ato &e" + multiplier + "x"));
            return true;
        } else if (action.equals("stop")) {
            plugin.getLevelManager().clearPetMultiplier(target.getUniqueId());
            sender.sendMessage(MessageUtils.translateColors("&aCleared pet multiplier for &e" + target.getName()));
            return true;
        } else {
            sendMessage(sender, "command.invalid-args");
            return false;
        }
    }

    private boolean showHelp(CommandSender sender) {
        if (!hasPermission(sender, "levels.help")) return false;

        sender.sendMessage(MessageUtils.translateColors("&b=== LevelsPlugin Help ==="));
        sender.sendMessage(MessageUtils.translateColors("&e/levels get [player] &7- Check level and points"));
        sender.sendMessage(MessageUtils.translateColors("&e/levels multiplier &7- Check current multiplier"));
        sender.sendMessage(MessageUtils.translateColors("&e/levels bar &7- Toggle action bar display"));
        sender.sendMessage(MessageUtils.translateColors("&e/levels top [page] &7- View top players"));

        if (hasPermission(sender, "levels.give")) {
            sender.sendMessage(MessageUtils.translateColors("&e/levels give <player> <amount> &7- Give points"));
            sender.sendMessage(MessageUtils.translateColors("&e/levels give <player> level <amount> &7- Give levels"));
        }

        if (hasPermission(sender, "levels.take")) {
            sender.sendMessage(MessageUtils.translateColors("&e/levels take <player> <amount> &7- Take points"));
        }

        if (hasPermission(sender, "levels.set")) {
            sender.sendMessage(MessageUtils.translateColors("&e/levels set <player> <amount> &7- Set points"));
        }

        if (hasPermission(sender, "levels.reload")) {
            sender.sendMessage(MessageUtils.translateColors("&e/levels reload &7- Reload configuration"));
        }

        if (hasPermission(sender, "levels.event.start")) {
            sender.sendMessage(MessageUtils.translateColors("&e/levels event start <id> &7- Start an event"));
            sender.sendMessage(MessageUtils.translateColors("&e/levels event stop &7- Stop current event"));
            sender.sendMessage(MessageUtils.translateColors("&e/levels event list &7- List available events"));
            sender.sendMessage(MessageUtils.translateColors("&e/levels event status &7- Show current event status"));
        }

        if (hasPermission(sender, "levels.pet")) {
            sender.sendMessage(MessageUtils.translateColors("&e/levels pet <player> start <multiplier> &7- Set pet multiplier"));
            sender.sendMessage(MessageUtils.translateColors("&e/levels pet <player> stop &7- Clear pet multiplier"));
        }

        if (hasPermission(sender, "levels.armor.list")) {
            sender.sendMessage(MessageUtils.translateColors("&e/levels armor list &7- List available armor pieces"));
        }

        if (hasPermission(sender, "levels.armor.give")) {
            sender.sendMessage(MessageUtils.translateColors("&e/levels armor give <id> <player> &7- Give armor piece"));
        }

        return true;
    }

    private boolean handleGet(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "levels.get")) return false;

        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sendMessage(sender, "command.player-only");
                return false;
            }

            Player player = (Player) sender;
            int points = plugin.getLevelManager().getPoints(player.getUniqueId());
            int level = plugin.getLevelManager().getLevel(player.getUniqueId());

            Map<String, String> placeholders = MessageUtils.placeholders();
            placeholders.put("points", String.valueOf(points));
            placeholders.put("level", String.valueOf(level));

            MessageUtils.sendMessage(player, "levels.current", placeholders);
            return true;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sendMessage(sender, "command.player-not-found");
            return false;
        }

        int points = plugin.getLevelManager().getPoints(target.getUniqueId());
        int level = plugin.getLevelManager().getLevel(target.getUniqueId());

        Map<String, String> placeholders = MessageUtils.placeholders();
        placeholders.put("player", target.getName());
        placeholders.put("points", String.valueOf(points));
        placeholders.put("level", String.valueOf(level));

        sendMessage(sender, "levels.current-other", placeholders);
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "levels.give")) return false;

        if (args.length < 3) {
            sendMessage(sender, "command.invalid-args");
            return false;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sendMessage(sender, "command.player-not-found");
            return false;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sendMessage(sender, "command.invalid-amount");
            return false;
        }

        plugin.getLevelManager().givePoints(target, amount);

        Map<String, String> placeholders = MessageUtils.placeholders();
        placeholders.put("player", target.getName());
        placeholders.put("points", String.valueOf(amount));

        sendMessage(sender, "levels.give", placeholders);
        return true;
    }

    private boolean handleTake(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "levels.take")) return false;

        if (args.length < 3) {
            sendMessage(sender, "command.invalid-args");
            return false;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sendMessage(sender, "command.player-not-found");
            return false;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sendMessage(sender, "command.invalid-amount");
            return false;
        }

        if (!plugin.getLevelManager().takePoints(target, amount)) {
            return false;
        }

        Map<String, String> placeholders = MessageUtils.placeholders();
        placeholders.put("player", target.getName());
        placeholders.put("points", String.valueOf(amount));

        sendMessage(sender, "levels.take", placeholders);
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "levels.set")) return false;

        if (args.length < 3) {
            sendMessage(sender, "command.invalid-args");
            return false;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sendMessage(sender, "command.player-not-found");
            return false;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sendMessage(sender, "command.invalid-amount");
            return false;
        }

        plugin.getLevelManager().setPoints(target.getUniqueId(), amount);

        Map<String, String> placeholders = MessageUtils.placeholders();
        placeholders.put("player", target.getName());
        placeholders.put("points", String.valueOf(amount));

        sendMessage(sender, "levels.set", placeholders);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!hasPermission(sender, "levels.reload")) return false;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.reload();
            sendMessage(sender, "command.reload");
        });

        return true;
    }

    private boolean handleEvent(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMessage(sender, "command.invalid-args");
            return false;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "start":
                if (!hasPermission(sender, "levels.event.start")) return false;

                if (args.length < 3) {
                    sendMessage(sender, "command.invalid-args");
                    return false;
                }

                String eventId = args[2];

                if (plugin.getEventManager().isEventRunning()) {
                    sendMessage(sender, "event.already-running");
                    return false;
                }

                if (!plugin.getEventManager().startEvent(eventId)) {
                    sendMessage(sender, "event.invalid");
                    return false;
                }

                return true;

            case "stop":
                if (!hasPermission(sender, "levels.event.stop")) return false;

                if (!plugin.getEventManager().isEventRunning()) {
                    sendMessage(sender, "event.none-running");
                    return false;
                }

                plugin.getEventManager().stopEvent();
                return true;

            case "list":
                if (!hasPermission(sender, "levels.event.start")) return false;

                Map<String, String> events = plugin.getEventManager().getEventNames();

                if (events.isEmpty()) {
                    sender.sendMessage(MessageUtils.translateColors("&cNo events are configured."));
                    return false;
                }

                sender.sendMessage(MessageUtils.translateColors("&b=== Available Events ==="));
                for (Map.Entry<String, String> entry : events.entrySet()) {
                    sender.sendMessage(MessageUtils.translateColors("&e" + entry.getKey() + " &7- &b" + entry.getValue()));
                }

                return true;

            case "status":
                if (!hasPermission(sender, "levels.event.start")) return false;

                if (!plugin.getEventManager().isEventRunning()) {
                    sendMessage(sender, "event.none-running");
                    return false;
                }

                String name = plugin.getEventManager().getCurrentEventName();
                double multiplier = plugin.getEventManager().getCurrentMultiplier();
                long timeLeft = plugin.getEventManager().getCurrentEventTimeRemaining();

                Map<String, String> placeholders = MessageUtils.placeholders();
                placeholders.put("name", name);
                placeholders.put("multiplier", String.format("%.1f", multiplier));
                placeholders.put("time", MessageUtils.formatTime(timeLeft));

                sendMessage(sender, "event.status", placeholders);
                return true;

            default:
                sendMessage(sender, "command.invalid-args");
                return false;
        }
    }

    private boolean handleBooster(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendMessage(sender, "command.invalid-args");
            return false;
        }

        if (!hasPermission(sender, "levels.booster")) return false;

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);

        if (target == null) {
            sendMessage(sender, "command.player-not-found");
            return false;
        }

        double multiplier;
        long duration;

        try {
            multiplier = Double.parseDouble(args[2]);
            if (multiplier <= 1.0) {
                sender.sendMessage(MessageUtils.translateColors("&cMultiplier must be greater than 1.0!"));
                return false;
            }
        } catch (NumberFormatException e) {
            sendMessage(sender, "command.invalid-amount");
            return false;
        }

        try {
            duration = Long.parseLong(args[3]);
            if (duration <= 0) {
                sender.sendMessage(MessageUtils.translateColors("&cDuration must be positive!"));
                return false;
            }
        } catch (NumberFormatException e) {
            sendMessage(sender, "command.invalid-amount");
            return false;
        }

        plugin.getBoosterManager().addBooster(target, multiplier, duration);

        sender.sendMessage(MessageUtils.translateColors("&aAdded a &e" + multiplier + "x &abooster to &e" +
                target.getName() + " &afor &e" + MessageUtils.formatTime(duration)));

        return true;
    }

    private boolean handleMultiplier(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "levels.multiplier")) return false;

        if (!(sender instanceof Player)) {
            sendMessage(sender, "command.player-only");
            return false;
        }

        Player player = (Player) sender;
        double eventBonus = plugin.getEventManager().getCurrentMultiplier();
        double boosterBonus = plugin.getBoosterManager().getBoosterMultiplier(player.getUniqueId());
        double armorBonus = plugin.getConfig().getBoolean("armor-multipliers.enabled", true) ?
                plugin.getArmorListener().getArmorMultiplier(player) : 0.0;
        double petBonus = plugin.getRivalPetsBuff().getPlayerMultiplier(player);
        double totalMultiplier = 1.0 + eventBonus + boosterBonus + armorBonus + petBonus;

        sender.sendMessage(MessageUtils.translateColors("&b=== Multiplier Breakdown ==="));
        sender.sendMessage(MessageUtils.translateColors("&eBase: &71.0x"));
        sender.sendMessage(MessageUtils.translateColors("&eEvent Bonus: &7+" + String.format("%.2f", eventBonus) + "x"));
        sender.sendMessage(MessageUtils.translateColors("&eBooster Bonus: &7+" + String.format("%.2f", boosterBonus) + "x"));
        sender.sendMessage(MessageUtils.translateColors("&eArmor Bonus: &7+" + String.format("%.2f", armorBonus) + "x"));
        sender.sendMessage(MessageUtils.translateColors("&ePet Bonus: &7+" + String.format("%.2f", petBonus) + "x"));
        sender.sendMessage(MessageUtils.translateColors("&eTotal: &7" + String.format("%.2f", totalMultiplier) + "x"));
        return true;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;

        sendMessage(sender, "command.no-permission");
        return false;
    }

    private void sendMessage(CommandSender sender, String path) {
        if (sender instanceof Player) {
            MessageUtils.sendMessage((Player) sender, path);
        } else {
            sender.sendMessage(MessageUtils.getMessageNoPrefix(path));
        }
    }

    private void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        if (sender instanceof Player) {
            MessageUtils.sendMessage((Player) sender, path, placeholders);
        } else {
            String message = MessageUtils.getMessageNoPrefix(path);
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            sender.sendMessage(message);
        }
    }
}
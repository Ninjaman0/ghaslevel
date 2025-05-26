package com.ninja.ghast.ghastLevels.commands;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.managers.ArmorManager;
import com.ninja.ghast.ghastLevels.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArmorCommands {

    private final LevelsPlugin plugin;
    private final ArmorManager armorManager;

    public ArmorCommands(LevelsPlugin plugin, ArmorManager armorManager) {
        this.plugin = plugin;
        this.armorManager = armorManager;
    }

    public boolean handleCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "list":
                return handleListCommand(sender);
            case "give":
                return handleGiveCommand(sender, args);
            default:
                sendUsage(sender);
                return true;
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MessageUtils.translateColors("&6Armor Commands:"));
        sender.sendMessage(MessageUtils.translateColors("&e/essence armor list &7- List all armor pieces"));
        sender.sendMessage(MessageUtils.translateColors("&e/essence armor give <id> <player> &7- Give armor piece"));
    }

    private boolean handleListCommand(CommandSender sender) {
        if (!sender.hasPermission("essence.armor.list")) {
            MessageUtils.sendMessage(sender, "command.no-permission");
            return false;
        }

        Map<String, ArmorManager.ArmorPieceData> pieces = armorManager.getArmorPieces();
        MessageUtils.sendMessage(sender, "armor.list-header");

        if (pieces.isEmpty()) {
            MessageUtils.sendMessage(sender, "armor.list-empty");
            return true;
        }

        pieces.forEach((id, piece) -> {
            Map<String, String> placeholders = MessageUtils.placeholders();
            placeholders.put("id", id);
            placeholders.put("name", piece.getName());
            placeholders.put("multiplier", String.format("%.0f", piece.getMultiplier() * 100));

            // Add permission text as a placeholder instead of concatenating
            if (piece.getPermission() != null) {
                placeholders.put("permission_text", " &7(Req: &e" + piece.getPermission() + "&7)");
            } else {
                placeholders.put("permission_text", ""); // Empty if no permission
            }

            // Now pass the placeholders Map correctly
            MessageUtils.sendMessage(sender, "armor.list-entry", placeholders);
        });


        return true;
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("essence.armor.give")) {
            MessageUtils.sendMessage(sender, "command.no-permission");
            return false;
        }

        if (args.length < 4) {
            MessageUtils.sendMessage(sender, "command.invalid-args");
            return false;
        }

        String armorId = args[2];
        Player target = Bukkit.getPlayer(args[3]);

        if (target == null) {
            MessageUtils.sendMessage(sender, "command.player-not-found");
            return false;
        }

        if (!armorManager.getArmorPieces().containsKey(armorId)) {
            MessageUtils.sendMessage(sender, "armor.invalid-id");
            return false;
        }

        ItemStack item = armorManager.createArmorItem(armorId);
        if (item == null) {
            MessageUtils.sendMessage(sender, "armor.creation-failed");
            return false;
        }

        // Add to inventory or drop if full
        if (target.getInventory().firstEmpty() == -1) {
            target.getWorld().dropItemNaturally(target.getLocation(), item);
        } else {
            target.getInventory().addItem(item);
        }

        ArmorManager.ArmorPieceData piece = armorManager.getArmorPieces().get(armorId);
        Map<String, String> placeholders = MessageUtils.placeholders();
        placeholders.put("player", target.getName());
        placeholders.put("armor", piece.getName());
        MessageUtils.sendMessage(sender, "armor.give", placeholders);

        return true;
    }

    public List<String> getTabCompletions(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("armor")) {
                completions.add("list");
                completions.add("give");
            }
        } else if (args.length == 3 && args[1].equalsIgnoreCase("give")) {
            completions.addAll(armorManager.getArmorPieces().keySet());
        } else if (args.length == 4 && args[1].equalsIgnoreCase("give")) {
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
        }

        return completions;
    }

    public LevelsPlugin getPlugin() {
        return plugin;
    }
}
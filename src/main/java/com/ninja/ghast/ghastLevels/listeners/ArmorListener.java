package com.ninja.ghast.ghastLevels.listeners;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.managers.ArmorManager;
import com.ninja.ghast.ghastLevels.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listens for armor equip/unequip events and manages per-piece multipliers
 */
public class ArmorListener implements Listener {

    private final LevelsPlugin plugin;
    private final ArmorManager armorManager;
    private final Map<UUID, Map<String, String>> playerArmorPieces = new HashMap<>();
    private final Map<UUID, Double> playerMultipliers = new HashMap<>();

    public ArmorListener(LevelsPlugin plugin, ArmorManager armorManager) {
        this.plugin = plugin;
        this.armorManager = armorManager;
    }

    /**
     * Clears all player data
     */
    public void reload() {
        playerArmorPieces.clear();
        playerMultipliers.clear();

        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayerArmor(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        checkPlayerArmor(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        playerArmorPieces.remove(uuid);
        playerMultipliers.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!armorManager.isEnabled()) return;

        Player player = (Player) event.getWhoClicked();
        boolean isArmorRelated = false;

        // Direct armor slot clicks
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            isArmorRelated = true;
        }

        // Clicks in armor inventory slots (36–39)
        if (event.getClickedInventory() != null &&
                event.getClickedInventory().equals(player.getInventory()) &&
                (event.getSlot() >= 36 && event.getSlot() <= 39)) {
            isArmorRelated = true;
        }

        // Shift-clicks to equip armor
        if (event.isShiftClick() && event.getCurrentItem() != null &&
                isArmorMaterial(event.getCurrentItem().getType())) {
            isArmorRelated = true;
        }

        // Cursor-based equipping (clicking armor slot with armor on cursor)
        if (event.getCursor() != null && isArmorMaterial(event.getCursor().getType()) &&
                event.getSlotType() == InventoryType.SlotType.ARMOR) {
            isArmorRelated = true;
        }

        if (isArmorRelated) {

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                checkPlayerArmor(player);
                logCurrentArmor(player);
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!armorManager.isEnabled()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Debug every interact event


        if (item != null && isArmorMaterial(item.getType())) {
            // Check if this is a potential equip action
            if (event.getAction() == Action.RIGHT_CLICK_AIR ||
                    event.getAction() == Action.RIGHT_CLICK_BLOCK ||
                    event.useItemInHand() != Event.Result.DENY) {



                // Schedule a delayed check with slightly longer delay
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    // Verify the item was actually equipped
                    if (!item.equals(player.getInventory().getItemInMainHand())) {
                        checkPlayerArmor(player);
                        logCurrentArmor(player);
                    }
                }, 3L); // 3 tick delay for reliability
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        if (!armorManager.isEnabled()) return;

        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (newItem != null && isArmorMaterial(newItem.getType())) {

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                checkPlayerArmor(player);
                logCurrentArmor(player);
            }, 2L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (!armorManager.isEnabled()) return;

        Player player = event.getPlayer();
        if (isArmorMaterial(event.getOffHandItem().getType()) ||
                isArmorMaterial(event.getMainHandItem().getType())) {

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                checkPlayerArmor(player);
                logCurrentArmor(player);
            }, 1L);
        }
    }

    /**
     * Checks if a material is an armor piece
     *
     * @param material The material to check
     * @return true if the material is armor, false otherwise
     */
    private boolean isArmorMaterial(Material material) {
        if (material == null) return false;

        // Check equipment slot first (more reliable)
        try {
            EquipmentSlot slot = material.getEquipmentSlot();
            return slot == EquipmentSlot.HEAD ||
                    slot == EquipmentSlot.CHEST ||
                    slot == EquipmentSlot.LEGS ||
                    slot == EquipmentSlot.FEET;
        } catch (NoSuchMethodError e) {
            // Fallback for older versions
            String name = material.name();
            return name.endsWith("_HELMET") ||
                    name.endsWith("_CHESTPLATE") ||
                    name.endsWith("_LEGGINGS") ||
                    name.endsWith("_BOOTS");
        }
    }

    /**
     * Logs the current armor state of a player
     *
     * @param player The player to check
     */
    private void logCurrentArmor(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();

    }

    /**
     * Checks a player's armor and updates their multiplier
     *
     * @param player The player to check
     */
    public void checkPlayerArmor(Player player) {
        if (!armorManager.isEnabled()) {
            playerArmorPieces.remove(player.getUniqueId());
            playerMultipliers.remove(player.getUniqueId());
            return;
        }

        UUID uuid = player.getUniqueId();
        Map<String, String> previousArmorPieces = playerArmorPieces.getOrDefault(uuid, new HashMap<>());
        Map<String, String> currentArmorPieces = new HashMap<>();
        double previousMultiplier = playerMultipliers.getOrDefault(uuid, 1.0);
        double currentMultiplier = 1.0;

        // Get player's armor items
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();



        // Check each armor piece
        checkArmorPiece(player, "helmet", helmet, previousArmorPieces, currentArmorPieces);
        checkArmorPiece(player, "chestplate", chestplate, previousArmorPieces, currentArmorPieces);
        checkArmorPiece(player, "leggings", leggings, previousArmorPieces, currentArmorPieces);
        checkArmorPiece(player, "boots", boots, previousArmorPieces, currentArmorPieces);

        // Calculate total multiplier
        for (String armorId : currentArmorPieces.values()) {
            if (armorId != null) {
                ArmorManager.ArmorPieceData piece = armorManager.getArmorPieces().get(armorId);
                if (piece != null) {
                    currentMultiplier += piece.getMultiplier();

                } else {
                    plugin.getLogger().warning("Unknown armor ID: " + armorId);
                }
            }
        }

        // Cap multiplier
        double maxMultiplier = armorManager.getMaxMultiplier();
        if (currentMultiplier > maxMultiplier) {
            currentMultiplier = maxMultiplier;
            Map<String, String> placeholders = MessageUtils.placeholders();
            placeholders.put("multiplier", String.format("%.2f", maxMultiplier));
            MessageUtils.sendMessage(player, "armor.max-multiplier", placeholders);
        }

        // Store current state
        playerArmorPieces.put(uuid, currentArmorPieces);
        playerMultipliers.put(uuid, currentMultiplier);

        // Notify if multiplier changed
        if (Math.abs(currentMultiplier - previousMultiplier) > 0.001) {


            if (armorManager.areParticlesEnabled()) {
                try {
                    Location location = player.getLocation().add(0, 1.0, 0);
                    Particle particle = Particle.valueOf(armorManager.getParticleType());
                    player.getWorld().spawnParticle(
                            particle,
                            location,
                            armorManager.getParticleCount(),
                            armorManager.getParticleRadius(),
                            armorManager.getParticleRadius(),
                            armorManager.getParticleRadius(),
                            0.05
                    );
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid particle type: " + armorManager.getParticleType());
                }
            }

            Map<String, String> placeholders = MessageUtils.placeholders();
            placeholders.put("multiplier", String.format("%.2f", currentMultiplier));
            MessageUtils.sendMessage(player, "armor.total-multiplier", placeholders);
        } else {

        }
    }

    /**
     * Checks an individual armor piece and updates tracking maps
     *
     * @param player The player wearing the armor
     * @param slot The armor slot (helmet, chestplate, etc.)
     * @param item The item in that slot
     * @param previousArmor The previously equipped armor map to compare against
     * @param currentArmor The current armor map to update
     */
    private void checkArmorPiece(Player player, String slot, ItemStack item,
                                 Map<String, String> previousArmor, Map<String, String> currentArmor) {
        String previousArmorId = previousArmor.get(slot);
        String currentArmorId = null;

        if (item != null && item.getType() != Material.AIR) {
            currentArmorId = armorManager.getArmorId(item);


            if (currentArmorId != null) {
                ArmorManager.ArmorPieceData piece = armorManager.getArmorPieces().get(currentArmorId);
                if (piece != null) {
                    if (!armorManager.hasPermission(player, piece)) {
                        plugin.getLogger().info("Player " + player.getName() + " lacks permission for " + currentArmorId);
                        MessageUtils.sendMessage(player, "armor.permission-required");

                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(item);
                        } else {
                            player.getWorld().dropItemNaturally(player.getLocation(), item);
                        }

                        switch (slot) {
                            case "helmet":
                                player.getInventory().setHelmet(null);
                                break;
                            case "chestplate":
                                player.getInventory().setChestplate(null);
                                break;
                            case "leggings":
                                player.getInventory().setLeggings(null);
                                break;
                            case "boots":
                                player.getInventory().setBoots(null);
                                break;
                        }

                        currentArmorId = null;
                    }
                } else {
                    plugin.getLogger().warning("No ArmorPieceData for ID: " + currentArmorId);
                    currentArmorId = null;
                }
            }
        }

        currentArmor.put(slot, currentArmorId);

        if ((previousArmorId == null && currentArmorId != null) ||
                (previousArmorId != null && !previousArmorId.equals(currentArmorId))) {
            if (previousArmorId != null) {
                ArmorManager.ArmorPieceData oldPiece = armorManager.getArmorPieces().get(previousArmorId);
                if (oldPiece != null) {
                    Map<String, String> placeholders = MessageUtils.placeholders();
                    placeholders.put("name", MessageUtils.stripColors(oldPiece.getName()));
                    placeholders.put("multiplier", String.format("%.0f", oldPiece.getMultiplier() * 100));
                    MessageUtils.sendMessage(player, "armor.unequipped", placeholders);

                }
            }

            if (currentArmorId != null) {
                ArmorManager.ArmorPieceData newPiece = armorManager.getArmorPieces().get(currentArmorId);
                if (newPiece != null) {
                    Map<String, String> placeholders = MessageUtils.placeholders();
                    placeholders.put("name", MessageUtils.stripColors(newPiece.getName()));
                    placeholders.put("multiplier", String.format("%.0f", newPiece.getMultiplier() * 100));
                    MessageUtils.sendMessage(player, "armor.equipped", placeholders);

                }
            }
        }
    }

    /**
     * Gets the current multiplier for a player
     *
     * @param player The player to check
     * @return The player's armor multiplier
     */
    public double getArmorMultiplier(Player player) {
        if (!armorManager.isEnabled()) {
            return 1.0;
        }

        UUID uuid = player.getUniqueId();
        if (!playerMultipliers.containsKey(uuid)) {
            checkPlayerArmor(player);
        }

        return playerMultipliers.getOrDefault(uuid, 1.0);
    }
}
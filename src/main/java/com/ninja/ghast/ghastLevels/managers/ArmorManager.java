package com.ninja.ghast.ghastLevels.managers;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import com.ninja.ghast.ghastLevels.utils.MessageUtils;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ArmorManager {
    private final LevelsPlugin plugin;
    private final Map<String, ArmorPieceData> armorPieces = new HashMap<>();
    private boolean enabled;
    private double maxMultiplier;
    private boolean particlesEnabled;
    private String particleType;
    private int particleCount;
    private double particleRadius;

    public ArmorManager(LevelsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        armorPieces.clear();
        enabled = plugin.getConfig().getBoolean("armor-multipliers.enabled", true);
        maxMultiplier = plugin.getConfig().getDouble("armor-multipliers.max-multiplier", 2.0);

        ConfigurationSection particlesSection = plugin.getConfig().getConfigurationSection("armor-multipliers.particles");
        if (particlesSection != null) {
            particlesEnabled = particlesSection.getBoolean("enabled", true);
            particleType = particlesSection.getString("type", "SPELL_MOB");
            particleCount = particlesSection.getInt("count", 15);
            particleRadius = particlesSection.getDouble("radius", 0.5);
        } else {
            particlesEnabled = true;
            particleType = "SPELL_MOB";
            particleCount = 15;
            particleRadius = 0.5;
        }

        if (!enabled) return;

        ConfigurationSection armorSection = plugin.getConfig().getConfigurationSection("armor-multipliers");
        if (armorSection == null) return;

        for (String armorId : armorSection.getKeys(false)) {
            if (armorId.equals("enabled") || armorId.equals("max-multiplier") || armorId.equals("particles")) continue;

            ConfigurationSection pieceSection = armorSection.getConfigurationSection(armorId);
            if (pieceSection == null) continue;

            String materialName = pieceSection.getString("material", "DIAMOND_HELMET");
            Material material;
            try {
                material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material for armor piece " + armorId);
                continue;
            }

            String name = pieceSection.getString("name", materialName);
            List<String> lore = pieceSection.getStringList("lore");
            double multiplier = pieceSection.getDouble("multiplier", 0.1);
            String permission = pieceSection.getString("permission", null);
            Integer customModelData = pieceSection.contains("custom-model-data") ?
                    pieceSection.getInt("custom-model-data") : null;
            String headTexture = pieceSection.getString("head-texture", "");
            String headOwner = pieceSection.getString("head-owner", "");

            Color color = null;
            if (pieceSection.contains("color")) {
                try {
                    String colorStr = pieceSection.getString("color");
                    if (colorStr.startsWith("#")) {
                        color = Color.fromRGB(Integer.parseInt(colorStr.substring(1), 16));
                    } else {
                        String[] rgb = colorStr.split(",");
                        if (rgb.length == 3) {
                            color = Color.fromRGB(
                                    Integer.parseInt(rgb[0].trim()),
                                    Integer.parseInt(rgb[1].trim()),
                                    Integer.parseInt(rgb[2].trim())
                            );
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid color format for armor piece " + armorId);
                }
            }

            armorPieces.put(armorId, new ArmorPieceData(
                    material, name, lore, multiplier, permission, customModelData, color, headTexture, headOwner
            ));
        }
    }

    public ItemStack createArmorItem(String armorId) {
        if (!armorPieces.containsKey(armorId)) return null;

        ArmorPieceData piece = armorPieces.get(armorId);
        ItemStack item = new ItemStack(piece.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        if (piece.getName() != null) {
            meta.setDisplayName(MessageUtils.translateColors(piece.getName()));
        }
        if (!piece.getLore().isEmpty()) {
            meta.setLore(MessageUtils.translateColors(piece.getLore()));
        }
        if (piece.getCustomModelData() != null) {
            meta.setCustomModelData(piece.getCustomModelData());
        }

        // Handle player head textures
        if (piece.getMaterial() == Material.PLAYER_HEAD && meta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) meta;

            if (!piece.getHeadTexture().isEmpty()) {
                try {
                    PlayerProfile profile = plugin.getServer().createPlayerProfile(UUID.randomUUID(), "CustomHead");
                    PlayerTextures textures = profile.getTextures();
                    URL url = new URL(piece.getHeadTexture());
                    textures.setSkin(url);
                    profile.setTextures(textures);
                    skullMeta.setOwnerProfile(profile);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to set custom head texture for " + armorId + ": " + e.getMessage());
                }
            } else if (!piece.getHeadOwner().isEmpty()) {
                String owner = piece.getHeadOwner();
                if (owner.equals("{player}")) {
                    // Will be set when given to a specific player
                } else {
                    String trimmedName = owner.length() > 16 ? owner.substring(0, 16) : owner;
                    skullMeta.setOwner(trimmedName);
                }
            }
        }

        // Apply color if leather armor
        if (piece.getColor() != null && meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(piece.getColor());
        }

        item.setItemMeta(meta);
        return item;
    }

    public List<ItemStack> createAllArmorItems() {
        List<ItemStack> items = new ArrayList<>();
        for (String armorId : armorPieces.keySet()) {
            ItemStack item = createArmorItem(armorId);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    public boolean isArmorPiece(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        return getArmorId(item) != null;
    }

    public String getArmorId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return null;
        }

        for (Map.Entry<String, ArmorPieceData> entry : armorPieces.entrySet()) {
            if (isMatchingItem(item, entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }

    public ArmorPieceData getArmorPieceData(ItemStack item) {
        String armorId = getArmorId(item);
        return armorId != null ? armorPieces.get(armorId) : null;
    }

    public double getArmorMultiplier(ItemStack item) {
        ArmorPieceData piece = getArmorPieceData(item);
        return piece != null ? piece.multiplier : 0;
    }

    public Map<String, ArmorPieceData> getArmorPieces() {
        return armorPieces;
    }

    public double getMaxMultiplier() {
        return maxMultiplier;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private boolean isMatchingItem(ItemStack item, ArmorPieceData piece) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        if (item.getType() != piece.material) return false;

        if (piece.name != null && !piece.name.isEmpty()) {
            if (!meta.hasDisplayName()) return false;
            String itemName = MessageUtils.stripColors(meta.getDisplayName()).toLowerCase();
            String pieceName = MessageUtils.stripColors(piece.name).toLowerCase();
            if (!itemName.equals(pieceName)) return false;
        }

        if (!piece.lore.isEmpty()) {
            if (!meta.hasLore() || meta.getLore() == null) return false;

            List<String> itemLore = MessageUtils.stripColors(meta.getLore()).stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            List<String> pieceLore = MessageUtils.stripColors(piece.lore).stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            for (String requiredLine : pieceLore) {
                boolean found = itemLore.stream().anyMatch(itemLine -> itemLine.contains(requiredLine));
                if (!found) return false;
            }
        }

        if (piece.customModelData != null) {
            if (!meta.hasCustomModelData()) return false;
            if (meta.getCustomModelData() != piece.customModelData) return false;
        }

        return true;
    }

    public boolean hasPermission(Player player, ArmorPieceData piece) {
        if (piece.permission == null || piece.permission.isEmpty()) return true;
        return player.hasPermission(piece.permission);
    }

    public String getParticleType() {
        return particleType;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public double getParticleRadius() {
        return particleRadius;
    }

    public boolean areParticlesEnabled() {
        return particlesEnabled;
    }

    public static class ArmorPieceData {
        private final Material material;
        private final String name;
        private final List<String> lore;
        private final double multiplier;
        private final String permission;
        private final Integer customModelData;
        private final Color color;
        private final String headTexture;
        private final String headOwner;

        public ArmorPieceData(Material material, String name, List<String> lore,
                              double multiplier, String permission,
                              Integer customModelData, Color color,
                              String headTexture, String headOwner) {
            this.material = material;
            this.name = name;
            this.lore = lore != null ? lore : new ArrayList<>();
            this.multiplier = multiplier;
            this.permission = permission;
            this.customModelData = customModelData;
            this.color = color;
            this.headTexture = headTexture;
            this.headOwner = headOwner;
        }

        public Material getMaterial() { return material; }
        public String getName() { return name; }
        public List<String> getLore() { return lore; }
        public double getMultiplier() { return multiplier; }
        public String getPermission() { return permission; }
        public Integer getCustomModelData() { return customModelData; }
        public Color getColor() { return color; }
        public String getHeadTexture() { return headTexture; }
        public String getHeadOwner() { return headOwner; }
    }
}
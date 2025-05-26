package com.ninja.ghast.ghastLevels.managers;

import com.ninja.ghast.ghastLevels.LevelsPlugin;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class LevelAnimationManager {
    private final LevelsPlugin plugin;
    private boolean fireworksEnabled;
    private boolean soundEnabled;
    private Sound levelUpSound;
    private float soundVolume;
    private float soundPitch;
    private List<ParticleEffect> particleEffects;
    private boolean particleTrailsEnabled;
    private int particleTrailMinLevel;
    private Particle trailParticle;
    private int trailCount;

    public LevelAnimationManager(LevelsPlugin plugin) {
        this.plugin = plugin;
        this.particleEffects = new ArrayList<>();
        reload();
    }

    public void reload() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("animations");
        if (config == null) return;

        // Load firework settings
        fireworksEnabled = config.getBoolean("fireworks.enabled", true);

        // Load sound settings
        soundEnabled = config.getBoolean("sound.enabled", true);
        try {
            levelUpSound = Sound.valueOf(config.getString("sound.type", "ENTITY_PLAYER_LEVELUP"));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound type in config: " + config.getString("sound.type"));
            levelUpSound = Sound.ENTITY_PLAYER_LEVELUP;
        }
        soundVolume = (float) config.getDouble("sound.volume", 1.0);
        soundPitch = (float) config.getDouble("sound.pitch", 1.0);

        // Load particle effects
        loadParticleEffects(config.getConfigurationSection("particles"));

        // Load trail settings
        ConfigurationSection trailConfig = config.getConfigurationSection("trails");
        if (trailConfig != null) {
            particleTrailsEnabled = trailConfig.getBoolean("enabled", true);
            particleTrailMinLevel = trailConfig.getInt("min-level", 50);

            String particleType = trailConfig.getString("particle", "END_ROD");
            try {
                trailParticle = Particle.valueOf(particleType);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid particle type in config: " + particleType);
                trailParticle = Particle.END_ROD;
            }

            trailCount = trailConfig.getInt("count", 3);
        }
    }

    private void loadParticleEffects(ConfigurationSection config) {
        particleEffects.clear();
        if (config == null) return;

        for (String key : config.getKeys(false)) {
            ConfigurationSection effectConfig = config.getConfigurationSection(key);
            if (effectConfig == null) continue;

            String particleType = effectConfig.getString("type", "WITCH");
            try {
                Particle particle = Particle.valueOf(particleType);
                int count = effectConfig.getInt("count", 20);
                double radius = effectConfig.getDouble("radius", 1.0);
                double yOffset = effectConfig.getDouble("y-offset", 0.0);
                particleEffects.add(new ParticleEffect(particle, count, radius, yOffset));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid particle type in config: " + particleType);
            }
        }
    }

    public void playLevelUpAnimation(Player player) {
        if (player == null) return;

        Location loc = player.getLocation();

        // Play sound
        if (soundEnabled && levelUpSound != null) {
            try {
                player.playSound(loc, levelUpSound, soundVolume, soundPitch);
            } catch (Exception e) {
                plugin.getLogger().warning("Error playing level up sound: " + e.getMessage());
            }
        }

        // Spawn firework
        if (fireworksEnabled) {
            try {
                spawnFirework(loc);
            } catch (Exception e) {
                plugin.getLogger().warning("Error spawning firework: " + e.getMessage());
            }
        }

        // Play particle effects
        if (particleEffects != null) {
            for (ParticleEffect effect : particleEffects) {
                if (effect != null) {
                    try {
                        effect.play(loc, plugin);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error playing particle effect: " + e.getMessage());
                    }
                }
            }
        }
    }

    private void spawnFirework(Location loc) {
        if (loc == null || loc.getWorld() == null) return;

        try {
            Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
            FireworkMeta meta = fw.getFireworkMeta();

            FireworkEffect effect = FireworkEffect.builder()
                    .withColor(Color.fromRGB(226, 124, 42))
                    .withFade(Color.fromRGB(255, 255, 196))
                    .with(FireworkEffect.Type.STAR)
                    .trail(true)
                    .build();

            meta.addEffect(effect);
            meta.setPower(0);
            fw.setFireworkMeta(meta);
        } catch (Exception e) {
            plugin.getLogger().warning("Error spawning firework: " + e.getMessage());
        }
    }

    public void playTrailEffect(Player player) {
        if (!particleTrailsEnabled || player == null || trailParticle == null) return;

        int playerLevel = plugin.getLevelManager().getLevel(player.getUniqueId());
        if (playerLevel < particleTrailMinLevel) return;

        Location loc = player.getLocation();
        if (loc.getWorld() == null) return;

        try {
            loc.getWorld().spawnParticle(
                    trailParticle,
                    loc.add(0, 0.1, 0),
                    trailCount,
                    0.2, 0, 0.2,
                    0.01
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Error spawning trail particles: " + e.getMessage());
        }
    }

    private static class ParticleEffect {
        private final Particle particle;
        private final int count;
        private final double radius;
        private final double yOffset;

        public ParticleEffect(Particle particle, int count, double radius, double yOffset) {
            this.particle = particle;
            this.count = count;
            this.radius = radius;
            this.yOffset = yOffset;
        }

        public void play(Location center, LevelsPlugin plugin) {
            if (center == null || center.getWorld() == null || particle == null) return;

            new BukkitRunnable() {
                double angle = 0;
                int steps = 0;
                final int maxSteps = 20;

                @Override
                public void run() {
                    if (steps >= maxSteps) {
                        this.cancel();
                        return;
                    }

                    try {
                        for (int i = 0; i < count / maxSteps; i++) {
                            double x = Math.cos(angle) * radius;
                            double z = Math.sin(angle) * radius;
                            Location particleLoc = center.clone().add(x, yOffset, z);
                            center.getWorld().spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
                            angle += Math.PI / 8;
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error spawning particle effect: " + e.getMessage());
                        this.cancel();
                        return;
                    }

                    steps++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }
}
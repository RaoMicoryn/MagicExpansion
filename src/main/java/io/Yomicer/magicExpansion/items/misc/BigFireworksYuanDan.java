package io.Yomicer.magicExpansion.items.misc;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

import static org.bukkit.inventory.EquipmentSlot.HAND;

public class BigFireworksYuanDan extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    private final Random random = new Random();
    private static final int EXPLOSION_HEIGHT = 30; // Explosion height
    private static final int EXPLOSION_RADIUS = 50; // Explosion radius

    // Super bright colors
    private final Color[] SUPER_COLORS = {
            Color.fromRGB(255, 50, 50),     // Bright red
            Color.fromRGB(255, 255, 100),   // Bright yellow
            Color.fromRGB(100, 255, 100),   // Bright green
            Color.fromRGB(100, 200, 255),   // Bright blue
            Color.fromRGB(255, 100, 255),   // Bright purple
            Color.fromRGB(255, 150, 50),    // Bright orange
            Color.fromRGB(255, 255, 255),   // Pure white
            Color.fromRGB(100, 255, 255),   // Cyan
            Color.fromRGB(255, 100, 100),   // Pink
            Color.fromRGB(255, 255, 0)      // Pure yellow
    };

    // Particle types
    private final Particle[] EXPLOSION_PARTICLES = {
            Particle.FIREWORK,
            Particle.FLAME,
            Particle.DRAGON_BREATH,
            Particle.END_ROD,
            Particle.WITCH,
            Particle.INSTANT_EFFECT,
            Particle.CLOUD,
            Particle.ENTITY_EFFECT
    };

    public BigFireworksYuanDan(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return e -> {
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);

            Player player = e.getPlayer();
            Location playerLoc = player.getLocation();
            World world = player.getWorld();

            // Check if main hand is used
            if (e.getHand() != HAND) {
                player.sendMessage(ColorGradient.getGradientName("Please use the main hand~"));
                return;
            }

            // Check if there's an item in hand
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand == null || itemInHand.getAmount() == 0) {
                return;
            }

            // Reduce item amount
            if (itemInHand.getAmount() > 1) {
                itemInHand.setAmount(itemInHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }

            // Launch super firework
            launchUltimateFirework(player, playerLoc, world);
        };
    }

    private void launchUltimateFirework(Player player, Location startLoc, World world) {
        // Play launch sound
        world.playSound(startLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 3.0f, 0.5f);

        // Give the player a message
        player.sendMessage(ColorGradient.getGradientName("☄ Flying Pancake launched! ☄"));

        // Calculate the target location (directly above)
        Location targetLoc = startLoc.clone().add(0, EXPLOSION_HEIGHT, 0);

        // Create guide particle trail
        createTrail(startLoc, targetLoc, world);

        // Delay the explosion effect
        int delayTicks = (int) (EXPLOSION_HEIGHT * 0.5); // Flight time

        new BukkitRunnable() {
            @Override
            public void run() {
                // Play explosion sound
                world.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 5.0f, 0.3f);
                world.playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 4.0f, 0.5f);

                // Create super explosion effect
                createSuperExplosionEffect(targetLoc, world, player);

                // Send screen title
                for (Player p : world.getPlayers()) {
                    if (p.getLocation().distance(targetLoc) < 200) {
                        p.sendTitle(
                                ColorGradient.getGradientName("🎇 Flying Pancake 🎇"),
                                ColorGradient.getGradientName("Shocks everyone!"),
                                10, 60, 20
                        );
                    }
                }
            }
        }.runTaskLater(MagicExpansion.getInstance(), delayTicks);
    }

    private void createTrail(Location start, Location end, World world) {
        new BukkitRunnable() {
            double progress = 0;
            Location current = start.clone();

            @Override
            public void run() {
                if (progress > 1.0) {
                    this.cancel();
                    return;
                }

                // Calculate the current position
                double x = start.getX() + (end.getX() - start.getX()) * progress;
                double y = start.getY() + (end.getY() - start.getY()) * progress;
                double z = start.getZ() + (end.getZ() - start.getZ()) * progress;

                current.setX(x);
                current.setY(y);
                current.setZ(z);

                // Create particle trail
                world.spawnParticle(Particle.FIREWORK, current, 10, 0.2, 0.2, 0.2, 0);
                world.spawnParticle(Particle.FLAME, current, 5, 0.1, 0.1, 0.1, 0);

                // Play flight sound
                if (random.nextInt(3) == 0) {
                    world.playSound(current, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.5f, 2.0f);
                }

                progress += 0.05;
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, 1L);
    }

    private void createSuperExplosionEffect(Location center, World world, Player player) {
        // Step 1: core big explosion (white flash)
        createCoreExplosion(center, world);

        // Step 2: colored explosion rings
        createColorRings(center, world);

        // Step 3: meteor shower effect
        createMeteorShower(center, world);

        // Step 4: the final afterglow
        new BukkitRunnable() {
            @Override
            public void run() {
                createAfterglow(center, world);
            }
        }.runTaskLater(MagicExpansion.getInstance(), 40L);
    }

    private void createCoreExplosion(Location center, World world) {
        // Strong white flash
        for (int i = 0; i < 3; i++) {
            double radius = (i + 1) * 10;
            createExplosionSphere(center, world, radius, Particle.FLASH, 50, Color.WHITE);
        }

        // Shockwave effect
        for (int r = 1; r <= 10; r++) {
            final int radius = r * 5;
            new BukkitRunnable() {
                @Override
                public void run() {
                    createExplosionRing(center, world, radius, Particle.CLOUD, 100);
                }
            }.runTaskLater(MagicExpansion.getInstance(), r * 2L);
        }
    }

    private void createColorRings(Location center, World world) {
        // Create multi-layer colored rings
        for (int layer = 0; layer < 5; layer++) {
            final int currentLayer = layer;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double radius = 15 + currentLayer * 7;
                    int particleCount = 200 + currentLayer * 50;

                    // Use different colors
                    Color color = SUPER_COLORS[currentLayer % SUPER_COLORS.length];

                    // Create colored ring
                    for (int i = 0; i < particleCount; i++) {
                        double angle = (2 * Math.PI * i) / particleCount;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;

                        Location particleLoc = center.clone().add(x, 0, z);

                        // Shoot particles upward
                        for (int h = 0; h < 3; h++) {
                            Location loc = particleLoc.clone().add(0, h * 2, 0);
                            Particle.DustOptions dustOptions = new Particle.DustOptions(color, 3.0f);
                            world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dustOptions);
                        }
                    }

                    // Create the downward ring too
                    for (int i = 0; i < particleCount; i++) {
                        double angle = (2 * Math.PI * i) / particleCount;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;

                        Location particleLoc = center.clone().add(x, 0, z);

                        // Shoot particles downward
                        for (int h = 0; h < 3; h++) {
                            Location loc = particleLoc.clone().add(0, -h * 2, 0);
                            Particle.DustOptions dustOptions = new Particle.DustOptions(color, 3.0f);
                            world.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dustOptions);
                        }
                    }
                }
            }.runTaskLater(MagicExpansion.getInstance(), layer * 10L);
        }
    }

    private void createMeteorShower(Location center, World world) {
        // Create meteor shower effect
        for (int i = 0; i < 100; i++) {
            final int index = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Random direction
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double distance = 20 + random.nextDouble() * 30;

                    double x = Math.cos(angle) * distance;
                    double z = Math.sin(angle) * distance;

                    Location startLoc = center.clone().add(x, 0, z);

                    // Random color
                    Color color = SUPER_COLORS[random.nextInt(SUPER_COLORS.length)];
                    Particle.DustOptions dustOptions = new Particle.DustOptions(color, 2.0f);

                    // Create meteor trail
                    createMeteorTrail(startLoc, center, world, dustOptions);
                }
            }.runTaskLater(MagicExpansion.getInstance(), random.nextInt(40));
        }
    }

    private void createMeteorTrail(Location start, Location target, World world, Particle.DustOptions dustOptions) {
        new BukkitRunnable() {
            double progress = 0;
            Location current = start.clone();

            @Override
            public void run() {
                if (progress > 1.0) {
                    // Create a small explosion at the target location
                    world.spawnParticle(Particle.EXPLOSION, target, 1, 0, 0, 0, 0);
                    this.cancel();
                    return;
                }

                // Calculate the current position
                double x = start.getX() + (target.getX() - start.getX()) * progress;
                double y = start.getY() + (target.getY() - start.getY()) * progress;
                double z = start.getZ() + (target.getZ() - start.getZ()) * progress;

                current.setX(x);
                current.setY(y);
                current.setZ(z);

                // Create particles
                world.spawnParticle(Particle.DUST, current, 3, 0.1, 0.1, 0.1, 0, dustOptions);
                world.spawnParticle(Particle.FIREWORK, current, 2, 0, 0, 0, 0.1);

                progress += 0.1;
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, 1L);
    }

    private void createAfterglow(Location center, World world) {
        // Create afterglow effect
        for (int i = 0; i < 50; i++) {
            double radius = random.nextDouble() * 40;
            double angle = random.nextDouble() * 2 * Math.PI;
            double height = random.nextDouble() * 40 - 20;

            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location particleLoc = center.clone().add(x, height, z);

            // Random color (leaning warm) - using the fromRGB method
            Color color = Color.fromRGB(
                    255,
                    random.nextInt(100) + 155,
                    random.nextInt(100)
            );

            Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.5f);

            // Slowly falling afterglow particles
            world.spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dustOptions);
        }
    }

    // Helper method: create explosion sphere
    private void createExplosionSphere(Location center, World world, double radius, Particle particle, int count, Color color) {
        for (int i = 0; i < count; i++) {
            double phi = Math.acos(2 * random.nextDouble() - 1);
            double theta = 2 * Math.PI * random.nextDouble();

            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.sin(phi) * Math.sin(theta);
            double z = radius * Math.cos(phi);

            Location particleLoc = center.clone().add(x, y, z);

            if (particle == Particle.DUST && color != null) {
                Particle.DustOptions dustOptions = new Particle.DustOptions(color, 2.0f);
                world.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0, dustOptions);
            } else if (particle == Particle.FLASH && color != null) {
                world.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0, color);
            } else {
                world.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
            }
        }
    }

    // Helper method: create explosion ring
    private void createExplosionRing(Location center, World world, double radius, Particle particle, int count) {
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location particleLoc = center.clone().add(x, 0, z);

            // Random height
            particleLoc.add(0, random.nextDouble() * 20 - 10, 0);

            world.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
        }
    }
}
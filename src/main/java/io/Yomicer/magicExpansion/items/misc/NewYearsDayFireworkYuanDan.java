package io.Yomicer.magicExpansion.items.misc;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.bukkit.inventory.EquipmentSlot.HAND;

public class NewYearsDayFireworkYuanDan extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    private final Random random = new Random();
    private static final int EXPLOSION_HEIGHT = 130; // Explosion height
    private static final int EXPLOSION_RADIUS = 80; // Explosion radius

    private final Color[] NEW_YEAR_COLORS = {
            Color.RED, Color.WHITE, Color.YELLOW, Color.AQUA,
            Color.LIME, Color.FUCHSIA, Color.ORANGE,
            Color.fromRGB(255, 20, 147), // Deep pink
            Color.fromRGB(0, 255, 255),   // Cyan
            Color.fromRGB(255, 215, 0),   // Gold
            Color.fromRGB(50, 205, 50),   // Lime green
            Color.fromRGB(186, 85, 211)   // Medium purple
    };

    public NewYearsDayFireworkYuanDan(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
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

            if (!e.getClickedBlock().isEmpty()){
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                // Check if the player has an item in hand
                if (e.getHand()!= HAND) {
                    player.sendMessage(ColorGradient.getGradientName("Please use the main hand~"));
                    return;
                }
                // Reduce the item amount in hand
                if (itemInHand.getAmount() > 1) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null); // If the amount is 1, just remove it
                }

                Location blockLoc = e.getClickedBlock().get().getLocation().clone().add(0,1,0);



                // Play start sound
                world.playSound(playerLoc, Sound.ITEM_TOTEM_USE, 2.0f, 0.8f);
                world.playSound(playerLoc, Sound.BLOCK_BELL_USE, 1.5f, 0.9f);

                // Create ground marker
                createGroundMark(playerLoc);

                // Create a rising light beam in the center
                createCenterBeacon(playerLoc);

                for (int i = 0; i < 5; i++) {
                    final int currentIteration = i;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // Optional: shift the position a bit depending on the iteration
                            Location launchLoc = blockLoc.clone();
                            if (currentIteration > 0) {
                                double angle = currentIteration * (2 * Math.PI / 5); // Evenly spread around the circle
                                double x = Math.cos(angle) * 2;
                                double z = Math.sin(angle) * 2;
                                launchLoc.add(x, 2, z);
                            } else {
                                launchLoc.add(0, 2, 0);
                            }

                            launchMassFireworks(launchLoc);
                        }
                    }.runTaskLater(MagicExpansion.getInstance(), 40L + (i * 40L)); // Gap each time 40tick
                }
            }
        };
    }

    private void createGroundMark(Location location) {
        World world = location.getWorld();

        // Create five concentric circle markers
        for (int layer = 1; layer <= 5; layer++) {
            final int currentLayer = layer;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double radius = currentLayer * 2.0;

                    for (int i = 0; i < 72; i++) {
                        double angle = i * 5;
                        double radians = Math.toRadians(angle);

                        double x = radius * Math.cos(radians);
                        double z = radius * Math.sin(radians);

                        Location markLoc = location.clone().add(x, 0.2, z);

                        Color color = getRainbowColor(currentLayer / 5.0);
                        world.spawnParticle(Particle.DUST, markLoc, 1,
                                new Particle.DustOptions(color, 2.0f));
                    }

                    world.playSound(location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 0.8f + currentLayer * 0.1f);
                }
            }.runTaskLater(MagicExpansion.getInstance(), currentLayer * 3L);
        }
    }

    private void createCenterBeacon(Location location) {
        World world = location.getWorld();

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick++ >= 60) {
                    this.cancel();
                    return;
                }

                // Create vertical light beam
                for (double y = 0; y < EXPLOSION_HEIGHT; y += 0.8) {
                    double radius = Math.sin(y * 0.1 + tick * 0.1) * 2 + 0.5;

                    for (int i = 0; i < 12; i++) {
                        double angle = i * Math.PI / 6;
                        double x = radius * Math.cos(angle);
                        double z = radius * Math.sin(angle);

                        Location beamLoc = location.clone().add(x, y, z);

                        Color color = getRainbowColor((y / EXPLOSION_HEIGHT + tick * 0.02) % 1.0);
                        world.spawnParticle(Particle.DUST, beamLoc, 1,
                                new Particle.DustOptions(color, 1.2f));
                    }
                }

                // Top flash
                if (tick % 5 == 0) {
                    Location top = location.clone().add(0, EXPLOSION_HEIGHT, 0);
                    world.spawnParticle(Particle.FLASH, top, 2, 1, 1, 1, 0, Color.WHITE);
                    world.playSound(top, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.2f);
                }
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, 1L);
    }

    private void launchMassFireworks(Location center) {
        World world = center.getWorld();

        // Play epic sound
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.4f);
        world.playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2.5f, 0.7f);

        // Create the main firework in the center (change the amount: was 5, can go up to 8)
        for (int i = 0; i < 8; i++) { // More center fireworks
            final int index = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    launchCentralFireworkSingle(center, index);
                }
            }.runTaskLater(MagicExpansion.getInstance(), i * 1L); // Gap 1 tick
        }

        // Layer 1: small close-range fireworks (change the amount: 12 -> 24)
        launchRingFireworks(center.clone().add(0, EXPLOSION_HEIGHT, 0), 10, 30, 0); // 12 changed to 24

        // Layer 2: mid-range fireworks (change the amount: 16 -> 32)
        launchRingFireworks(center.clone().add(0, EXPLOSION_HEIGHT, 0), 20, 36, 5); // 16 changed to 32

        // Layer 3: big far-range fireworks (change the amount: 20 -> 40)
        launchRingFireworks(center.clone().add(0, EXPLOSION_HEIGHT, 0), 30, 42, 10); // 20 changed to 40

        // Layer 4: super big fireworks (change the amount: 8 -> 16)
        launchSuperFireworks(center.clone().add(0, EXPLOSION_HEIGHT, 0), 40, 24, 15);
    }

    private void launchCentralFireworkSingle(Location center, int index) {
        World world = center.getWorld();

        Firework firework = world.spawn(center.clone().add(0, 1, 0), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        // Set strong firework
        meta.setPower(4);

        // Multi-color combo
        List<Color> colors = new ArrayList<>();
        for (int j = 0; j < 3; j++) {
            colors.add(NEW_YEAR_COLORS[(index * 3 + j) % NEW_YEAR_COLORS.length]);
        }

        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(colors)
                .trail(true)
                .flicker(index % 2 == 0)
                .build());

        firework.setFireworkMeta(meta);
        firework.setVelocity(new Vector(0, 1.8, 0));

        // Create strong trail
        createPowerfulTrail(firework);

        // Set explosion - less delay
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!firework.isDead()) {
                    firework.detonate();
                    createCentralExplosion(firework.getLocation());
                }
            }
        }.runTaskLater(MagicExpansion.getInstance(), 20 + random.nextInt(10)); // From 45L down to 40L
    }


    private void launchRingFireworks(Location center, double radius, int count, int delayOffset) {
        World world = center.getWorld();

        // Launch ring fireworks
        for (int i = 0; i < count; i++) {
            final int index = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double angle = index * (2 * Math.PI / count);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location spawnLoc = center.clone().add(x, 0, z);

                    // Launch from the ground
                    Firework firework = world.spawn(spawnLoc.clone().subtract(0, EXPLOSION_HEIGHT, 0), Firework.class);

                    // Work out the launch direction (pointing at the explosion point)
                    Vector direction = spawnLoc.clone().subtract(firework.getLocation()).toVector().normalize();
                    direction.setY(1.0); // Keep the upward part
                    direction.multiply(1.5);

                    // Set firework
                    FireworkMeta meta = firework.getFireworkMeta();
                    meta.setPower(3);

                    List<Color> colors = new ArrayList<>();
                    colors.add(NEW_YEAR_COLORS[index % NEW_YEAR_COLORS.length]);
                    colors.add(NEW_YEAR_COLORS[(index + 4) % NEW_YEAR_COLORS.length]);

                    meta.addEffect(FireworkEffect.builder()
                            .with(getRandomType())
                            .withColor(colors)
                            .trail(true)
                            .flicker(random.nextBoolean())
                            .build());

                    firework.setFireworkMeta(meta);
                    firework.setVelocity(direction);

                    // Trail
                    createRocketTrail(firework);

                    // Explode at the same time
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!firework.isDead()) {
                                firework.detonate();
                                createRingExplosion(spawnLoc, index % 4);
                            }
                        }
                    }.runTaskLater(MagicExpansion.getInstance(), 20 + random.nextInt(10));
                }
            }.runTaskLater(MagicExpansion.getInstance(), delayOffset);
        }
    }

    private void launchSuperFireworks(Location center, double radius, int count, int delayOffset) {
        World world = center.getWorld();

        // Launch super big fireworks
        for (int i = 0; i < count; i++) {
            final int index = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double angle = index * (2 * Math.PI / count);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location spawnLoc = center.clone().add(x, 0, z);

                    // Launch from a lower position
                    Firework firework = world.spawn(spawnLoc.clone().subtract(0, EXPLOSION_HEIGHT + 20, 0), Firework.class);

                    // Strong launch
                    Vector direction = new Vector(0, 2.0, 0);
                    firework.setVelocity(direction);

                    // Set super firework
                    FireworkMeta meta = firework.getFireworkMeta();
                    meta.setPower(4);

                    List<Color> colors = new ArrayList<>();
                    for (int j = 0; j < 4; j++) {
                        colors.add(NEW_YEAR_COLORS[(index * 2 + j) % NEW_YEAR_COLORS.length]);
                    }

                    meta.addEffect(FireworkEffect.builder()
                            .with(FireworkEffect.Type.BURST)
                            .withColor(colors)
                            .withFade(Color.WHITE)
                            .trail(true)
                            .flicker(true)
                            .build());

                    firework.setFireworkMeta(meta);

                    // Super trail
                    createSuperTrail(firework);

                    // Explode at the same time
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!firework.isDead()) {
                                firework.detonate();
                                createSuperExplosion(spawnLoc);
                            }
                        }
                    }.runTaskLater(MagicExpansion.getInstance(), 20 + random.nextInt(10));
                }
            }.runTaskLater(MagicExpansion.getInstance(), delayOffset);
        }
    }

    private void createPowerfulTrail(Firework firework) {
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (firework.isDead()) {
                    this.cancel();
                    return;
                }

                Location loc = firework.getLocation();
                World world = loc.getWorld();

                // Multiple particles
                world.spawnParticle(Particle.FIREWORK, loc, 8, 0.3, 0.3, 0.3, 0.1);

                // Rainbow trail
                double hue = (tick / 30.0) % 1.0;
                Color color = getRainbowColor(hue);
                world.spawnParticle(Particle.DUST, loc, 5,
                        new Particle.DustOptions(color, 2.0f));

                // Lightning effect
                if (tick % 4 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double angle = random.nextDouble() * Math.PI * 2;
                        double distance = random.nextDouble() * 2;

                        double x = distance * Math.cos(angle);
                        double z = distance * Math.sin(angle);

                        Location sparkLoc = loc.clone().add(x, 0, z);
                        world.spawnParticle(Particle.FLASH, sparkLoc, 1, color);
                    }
                    world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.8f);
                }

                tick++;
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, 1L);
    }

    private void createRocketTrail(Firework firework) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (firework.isDead()) {
                    this.cancel();
                    return;
                }

                Location loc = firework.getLocation();
                World world = loc.getWorld();

                world.spawnParticle(Particle.FIREWORK, loc, 4, 0.2, 0.2, 0.2, 0.05);

                Color color = NEW_YEAR_COLORS[random.nextInt(NEW_YEAR_COLORS.length)];
                world.spawnParticle(Particle.DUST, loc, 2,
                        new Particle.DustOptions(color, 1.5f));
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, 1L);
    }

    private void createSuperTrail(Firework firework) {
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (firework.isDead()) {
                    this.cancel();
                    return;
                }

                Location loc = firework.getLocation();
                World world = loc.getWorld();

                // Lots of sparks
                world.spawnParticle(Particle.FIREWORK, loc, 12, 0.4, 0.4, 0.4, 0.15);

                // Double rainbow
                double hue1 = (tick / 25.0) % 1.0;
                double hue2 = (hue1 + 0.5) % 1.0;

                Color color1 = getRainbowColor(hue1);
                Color color2 = getRainbowColor(hue2);

                world.spawnParticle(Particle.DUST, loc, 4,
                        new Particle.DustOptions(color1, 2.0f));
                world.spawnParticle(Particle.DUST, loc, 4,
                        new Particle.DustOptions(color2, 2.0f));

                // Flash and sound
                if (tick % 3 == 0) {
                    world.spawnParticle(Particle.FLASH, loc, 2, 0.5, 0.5, 0.5, 0, color1);
                    world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.5f, 1.5f);
                }

                tick++;
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, 1L);
    }

    private void createCentralExplosion(Location center) {
        World world = center.getWorld();

        // Play strong explosion sound
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 4.0f, 0.5f);
        world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 3.0f, 0.7f);

        // Multi-layer explosion effect
        for (int layer = 1; layer <= 12; layer++) {
            final int currentLayer = layer;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double radius = currentLayer * 15;
                    int particles = 200 + currentLayer * 50;

                    for (int i = 0; i < particles; i++) {
                        double phi = random.nextDouble() * Math.PI;
                        double theta = random.nextDouble() * 2 * Math.PI;

                        double x = radius * Math.sin(phi) * Math.cos(theta);
                        double y = radius * Math.cos(phi);
                        double z = radius * Math.sin(phi) * Math.sin(theta);

                        Location explosionLoc = center.clone().add(x, y, z);

                        // Rainbow colors
                        double hue = (currentLayer + i / (double) particles) % 1.0;
                        Color color = getRainbowColor(hue);

                        world.spawnParticle(Particle.DUST, explosionLoc, 2,
                                new Particle.DustOptions(color, 3.5f));

                        // Flash
                        if (random.nextDouble() < 0.25) {
                            world.spawnParticle(Particle.FLASH, explosionLoc, 2, color);
                        }
                    }

                    // Sound
                    world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                            2.0f, 0.6f + currentLayer * 0.05f);
                }
            }.runTaskLater(MagicExpansion.getInstance(), layer * 2L);
        }

        // Final number effect
        new BukkitRunnable() {
            @Override
            public void run() {
                create2026Text(center.clone().add(0, 25, 0));
            }
        }.runTaskLater(MagicExpansion.getInstance(), 20L);
    }

    private void createRingExplosion(Location center, int type) {
        World world = center.getWorld();

        world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2.0f, 1.2f);

        switch (type) {
            case 0: // Sphere explosion
                createSphereExplosion(center, 15);
                break;
            case 1: // Ring explosion
                createRingPattern(center, 12);
                break;
            case 2: // Star explosion
                createStarExplosion(center, 15);
                break;
            case 3: // Heart explosion
                createHeartExplosion(center, 12);
                break;
        }

//        createEnhancedShockwave(center, 20);
    }

    private void createSuperExplosion(Location center) {
        World world = center.getWorld();

        // Play the stronger explosion sound
        world.playSound(center, Sound.ENTITY_WITHER_DEATH, 3.0f, 0.3f);
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 2.5f, 0.5f);

        // Bigger explosion effect
        for (int layer = 1; layer <= 15; layer++) { // From 8 up to 15
            final int currentLayer = layer;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double radius = currentLayer * 12; // From 7 up to 12
                    int particles = 200 + currentLayer * 40; // From 120+increase to 200+

                    for (int i = 0; i < particles; i++) {
                        double phi = random.nextDouble() * Math.PI;
                        double theta = random.nextDouble() * 2 * Math.PI;

                        double x = radius * Math.sin(phi) * Math.cos(theta);
                        double y = radius * Math.cos(phi);
                        double z = radius * Math.sin(phi) * Math.sin(theta);

                        Location explosionLoc = center.clone().add(x, y, z);

                        Color color;
                        if (currentLayer % 3 == 0) {
                            color = Color.fromRGB(255, 215, 0); // Gold
                        } else if (currentLayer % 3 == 1) {
                            color = Color.fromRGB(255, 50, 50); // Red
                        } else {
                            color = Color.fromRGB(100, 200, 255); // Blue
                        }

                        world.spawnParticle(Particle.DUST, explosionLoc, 3, // From 1 up to 3
                                new Particle.DustOptions(color, 3.2f)); // From 2.5f up to 3.2f

                        // Stronger flash effect
                        if (random.nextDouble() < 0.3) {
                            world.spawnParticle(Particle.FLASH, explosionLoc, 2, color);
                        }

                        // More sparks
                        if (random.nextDouble() < 0.4) {
                            world.spawnParticle(Particle.FIREWORK, explosionLoc,
                                    3, 0.3, 0.3, 0.3, 0.15);
                        }
                    }

                    // Stronger sound
                    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE,
                            1.8f, 0.5f + currentLayer * 0.04f);

                    // Add shockwave effect
                    if (currentLayer % 4 == 0) {
                        createEnhancedShockwave(center, radius);
                    }
                }
            }.runTaskLater(MagicExpansion.getInstance(), layer * 1L);
        }
    }

    private void createEnhancedShockwave(Location center, double radius) {
        World world = center.getWorld();

        // Multi-layer shockwave
        for (int wave = 0; wave < 3; wave++) {
            final int currentWave = wave;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double waveRadius = radius * (1 + currentWave * 0.3);

                    // More shockwave particle density
                    for (int i = 0; i < 144; i++) { // From 72 up to 144
                        double angle = i * 2.5;
                        double radians = Math.toRadians(angle);

                        double x = waveRadius * Math.cos(radians);
                        double z = waveRadius * Math.sin(radians);

                        Location waveLoc = center.clone().add(x, 0, z);

                        // Colored shockwave
                        Color color = getRainbowColor(i / 144.0);
                        world.spawnParticle(Particle.DUST, waveLoc, 2,
                                new Particle.DustOptions(color, 2.8f));

                        // Vertical shockwave layer
                        for (int h = -3; h <= 3; h++) {
                            Location verticalWave = waveLoc.clone().add(0, h * 2, 0);
                            world.spawnParticle(Particle.DUST, verticalWave, 1,
                                    new Particle.DustOptions(color, 2.0f));
                        }
                    }

                    // Shockwave sound
                    world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                            1.2f + currentWave * 0.3f, 0.8f);
                }
            }.runTaskLater(MagicExpansion.getInstance(), currentWave * 5L);
        }
    }

    private void createSphereExplosion(Location center, double radius) {
        World world = center.getWorld();

        // Add a lot more particles
        for (int i = 0; i < 400; i++) { // From 150 up to 400
            double phi = random.nextDouble() * Math.PI;
            double theta = random.nextDouble() * 2 * Math.PI;
            double r = radius * (0.8 + random.nextDouble() * 0.4); // More fill density

            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.cos(phi);
            double z = r * Math.sin(phi) * Math.sin(theta);

            Location sphereLoc = center.clone().add(x, y, z);

            Color color = NEW_YEAR_COLORS[random.nextInt(NEW_YEAR_COLORS.length)];
            world.spawnParticle(Particle.DUST, sphereLoc, 2, // More particles
                    new Particle.DustOptions(color, 2.5f)); // Bigger particle size

            // Add inner fill particles
            if (random.nextDouble() < 0.3) {
                double innerR = radius * random.nextDouble() * 0.7;
                double innerX = innerR * Math.sin(phi) * Math.cos(theta);
                double innerY = innerR * Math.cos(phi);
                double innerZ = innerR * Math.sin(phi) * Math.sin(theta);

                Location innerLoc = center.clone().add(innerX, innerY, innerZ);
                world.spawnParticle(Particle.DUST, innerLoc, 1,
                        new Particle.DustOptions(color, 1.8f));
            }
        }

        // Add explosion edge effect
        for (int i = 0; i < 100; i++) {
            double phi = random.nextDouble() * Math.PI;
            double theta = random.nextDouble() * 2 * Math.PI;

            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.cos(phi);
            double z = radius * Math.sin(phi) * Math.sin(theta);

            Location edgeLoc = center.clone().add(x, y, z);
            world.spawnParticle(Particle.FIREWORK, edgeLoc, 1);
        }
    }

    private void createRingPattern(Location center, double radius) {
        World world = center.getWorld();

        // More ring layers
        for (int ring = 1; ring <= 5; ring++) { // From 3 up to 5
            double ringRadius = radius * ring / 5.0;

            // More particle density per ring
            for (int i = 0; i < 96; i++) { // From 48 up to 96
                double angle = i * 3.75; // Smaller angle gap, more density
                double radians = Math.toRadians(angle);

                double x = ringRadius * Math.cos(radians);
                double z = ringRadius * Math.sin(radians);

                Location ringLoc = center.clone().add(x, 0, z);

                Color color = getRainbowColor(i / 96.0);
                world.spawnParticle(Particle.DUST, ringLoc, 2, // More particles
                        new Particle.DustOptions(color, 2.2f)); // Bigger particle size

                // Make the vertical ring thicker
                for (int v = -2; v <= 2; v++) {
                    Location verticalLoc = center.clone().add(0, x + v, z);
                    world.spawnParticle(Particle.DUST, verticalLoc, 1,
                            new Particle.DustOptions(color, 1.8f));
                }
            }
        }
    }

    private void createStarExplosion(Location center, double size) {
        World world = center.getWorld();

        // More star density
        for (double t = 0; t < 2 * Math.PI; t += 0.02) { // From 0.04 down to 0.02, more density
            double r = size * (0.4 + 0.6 * Math.cos(5 * t));
            double x = r * Math.cos(t);
            double y = r * Math.sin(t);

            // More rotating planes
            for (int rotation = 0; rotation < 4; rotation++) { // From 2 up to 4
                double rotAngle = rotation * Math.PI / 4;
                double xRot = x * Math.cos(rotAngle) - y * Math.sin(rotAngle);
                double yRot = x * Math.sin(rotAngle) + y * Math.cos(rotAngle);

                // Main star shape
                Location starLoc = center.clone().add(xRot, yRot, 0);
                Color color = Color.fromRGB(255, 255, 150 + rotation * 30);
                world.spawnParticle(Particle.DUST, starLoc, 2, // More particles
                        new Particle.DustOptions(color, 2.2f));

                // Another plane
                Location starLoc2 = center.clone().add(0, yRot, xRot);
                world.spawnParticle(Particle.DUST, starLoc2, 2,
                        new Particle.DustOptions(color, 2.2f));

                // Third plane
                Location starLoc3 = center.clone().add(yRot, 0, xRot);
                world.spawnParticle(Particle.DUST, starLoc3, 1,
                        new Particle.DustOptions(color, 1.8f));
            }
        }

        // Add star flash points
        for (int i = 0; i < 12; i++) {
            double angle = i * 30;
            double radians = Math.toRadians(angle);
            double tipRadius = size * 1.2; // Tip points that go past the main star

            double x = tipRadius * Math.cos(radians);
            double y = tipRadius * Math.sin(radians);

            Location tipLoc = center.clone().add(x, y, 0);
            world.spawnParticle(Particle.FLASH, tipLoc, 2, Color.WHITE);
            world.playSound(tipLoc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.3f, 1.5f);
        }
    }

    private void createHeartExplosion(Location center, double size) {
        World world = center.getWorld();

        // Heart formula
        for (double t = 0; t < 2 * Math.PI; t += 0.02) {
            double x = size * Math.pow(Math.sin(t), 3);
            double y = size * (13 * Math.cos(t) - 5 * Math.cos(2*t) - 2 * Math.cos(3*t) - Math.cos(4*t)) / 16;

            // Rotating heart
            for (int rotation = 0; rotation < 4; rotation++) {
                double angle = rotation * Math.PI / 2;
                double xRot = x * Math.cos(angle);
                double zRot = x * Math.sin(angle);

                Location heartLoc = center.clone().add(xRot, y, zRot);

                Color color = Color.fromRGB(255, 105, 180); // Pink
                world.spawnParticle(Particle.DUST, heartLoc, 1,
                        new Particle.DustOptions(color, 2.0f));
            }
        }
    }

    private void createShockwave(Location center, double radius) {
        World world = center.getWorld();

        // Create shockwave ring
        for (int i = 0; i < 72; i++) {
            double angle = i * 5;
            double radians = Math.toRadians(angle);

            double x = radius * Math.cos(radians);
            double z = radius * Math.sin(radians);

            Location waveLoc = center.clone().add(x, 0, z);

            Color color = Color.WHITE;
            world.spawnParticle(Particle.DUST, waveLoc, 3,
                    new Particle.DustOptions(color, 3.0f));

            // Vertical shockwave
            for (int h = -5; h <= 5; h += 2) {
                Location verticalWave = waveLoc.clone().add(0, h, 0);
                world.spawnParticle(Particle.DUST, verticalWave, 2,
                        new Particle.DustOptions(color, 2.0f));
            }
        }

        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.7f);
    }

    private void create2026Text(Location center) {
        World world = center.getWorld();

        // Play text sound
        world.playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 3.0f, 0.7f);
        world.playSound(center, Sound.BLOCK_BELL_USE, 2.0f, 1.0f);

        String number = "2026";

        for (int i = 0; i < number.length(); i++) {
            final int digitIndex = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    char digit = number.charAt(digitIndex);
                    int[][] pattern = getNumberPattern(digit);

                    for (int[] point : pattern) {
                        double x = center.getX() + (digitIndex * 15) + point[0] - 22.5;
                        double y = center.getY() + point[1];
                        double z = center.getZ() + point[2];

                        Location digitLoc = new Location(world, x, y, z);

                        // Show right away
                        Color color = Color.fromRGB(255, 215, 0); // Gold
                        world.spawnParticle(Particle.DUST, digitLoc, 2,
                                new Particle.DustOptions(color, 2.5f));

                        // Halo effect
                        for (int j = 0; j < 8; j++) {
                            double angle = j * 45;
                            double radians = Math.toRadians(angle);
                            double radius = 0.8;

                            double haloX = radius * Math.cos(radians);
                            double haloZ = radius * Math.sin(radians);

                            Location haloLoc = digitLoc.clone().add(haloX, 0, haloZ);
                            world.spawnParticle(Particle.DUST, haloLoc, 1,
                                    new Particle.DustOptions(Color.WHITE, 1.2f));
                        }

                        world.playSound(digitLoc, Sound.BLOCK_NOTE_BLOCK_BIT, 0.3f, 1.8f);
                    }
                }
            }.runTaskLater(MagicExpansion.getInstance(), i * 2L);
        }

        // Text flicker
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int blink = 0; blink < 5; blink++) {
                    final int currentBlink = blink;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            boolean show = currentBlink % 2 == 0;
                            if (show) {
                                for (int i = 0; i < number.length(); i++) {
                                    int[][] pattern = getNumberPattern(number.charAt(i));
                                    for (int[] point : pattern) {
                                        double x = center.getX() + (i * 15) + point[0] - 22.5;
                                        double y = center.getY() + point[1];
                                        double z = center.getZ() + point[2];

                                        Location digitLoc = new Location(world, x, y, z);
                                        Color color = getRainbowColor(currentBlink / 5.0);
                                        world.spawnParticle(Particle.DUST, digitLoc, 1,
                                                new Particle.DustOptions(color, 3.0f));
                                    }
                                }
                                world.playSound(center, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.9f);
                            }
                        }
                    }.runTaskLater(MagicExpansion.getInstance(), blink * 8L);
                }
            }
        }.runTaskLater(MagicExpansion.getInstance(), 30L);
    }

    private FireworkEffect.Type getRandomType() {
        FireworkEffect.Type[] types = {
                FireworkEffect.Type.BALL,
                FireworkEffect.Type.BALL_LARGE,
                FireworkEffect.Type.STAR,
                FireworkEffect.Type.CREEPER,
                FireworkEffect.Type.BURST
        };
        return types[random.nextInt(types.length)];
    }

    private int[][] getNumberPattern(char digit) {
        switch (digit) {
            case '2':
                return new int[][]{
                        {0,0,0},{1,0,0},{2,0,0},{3,0,0},{4,0,0},
                        {4,1,0},{4,2,0},{4,3,0},
                        {3,3,0},{2,3,0},{1,3,0},{0,3,0},
                        {0,4,0},{0,5,0},{0,6,0},
                        {1,6,0},{2,6,0},{3,6,0},{4,6,0}
                };
            case '0':
                return new int[][]{
                        {0,0,0},{1,0,0},{2,0,0},{3,0,0},{4,0,0},
                        {0,1,0},{4,1,0},{0,2,0},{4,2,0},
                        {0,3,0},{4,3,0},{0,4,0},{4,4,0},
                        {0,5,0},{4,5,0},{0,6,0},{4,6,0},
                        {0,6,0},{1,6,0},{2,6,0},{3,6,0},{4,6,0}
                };
            case '6':
                return new int[][]{
                        {2,0,0},{3,0,0},{4,0,0},
                        {1,1,0},{0,2,0},
                        {0,3,0},{1,3,0},{2,3,0},{3,3,0},{4,3,0},
                        {0,4,0},{4,4,0},{0,5,0},{4,5,0},
                        {0,6,0},{1,6,0},{2,6,0},{3,6,0},{4,6,0}
                };
            default:
                return new int[][]{{0,0,0}};
        }
    }

    private Color getRainbowColor(double position) {
        position = position % 1.0;

        if (position < 0.166) {
            return Color.fromRGB(255, (int)(position * 6 * 255), 0);
        } else if (position < 0.333) {
            return Color.fromRGB(255, 255, 0);
        } else if (position < 0.5) {
            position = (position - 0.333) * 6;
            return Color.fromRGB((int)((1-position) * 255), 255, 0);
        } else if (position < 0.666) {
            position = (position - 0.5) * 6;
            return Color.fromRGB(0, 255, (int)(position * 255));
        } else if (position < 0.833) {
            position = (position - 0.666) * 6;
            return Color.fromRGB(0, (int)((1-position) * 255), 255);
        } else {
            position = (position - 0.833) * 6;
            return Color.fromRGB((int)(position * 255), 0, 255);
        }
    }
}
package io.Yomicer.magicExpansion.items.misc.magicAlter;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.utils.log.Debug;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun; // E4: needed for the land protection check (copied from how getProtectionManager is already used in the project)
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction; // E4: interaction permission enum
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class MagicAltarManager {

    private final MagicExpansion plugin;
    private final Set<Location> activeAltars;
    private final HashMap<Location, BukkitTask> altarTasks;
    private final HashMap<Location, BukkitTask> particleTasks;
    private final Material[][][] altarPatterns;
    private final LinkedHashMap<String, MagicAltarRecipe> recipes;

    public MagicAltarManager(MagicExpansion plugin) {
        this.plugin = plugin;
        this.activeAltars = new HashSet<>();
        this.altarTasks = new HashMap<>();
        this.particleTasks = new HashMap<>();
        this.recipes = new LinkedHashMap<>();
        this.altarPatterns = loadAltarPatterns();
    }

    Material[][] pattern1 = {
            {Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN},
            {Material.OBSIDIAN, Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.OBSIDIAN},
            {Material.OBSIDIAN, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.GOLD_BLOCK, Material.OBSIDIAN},
            {Material.OBSIDIAN, Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.OBSIDIAN},
            {Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN}
    };

    Material[][] customBase = {
            {Material.NETHERRACK, Material.NETHERRACK, Material.NETHERRACK, Material.NETHERRACK, Material.NETHERRACK},
            {Material.NETHERRACK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.NETHERRACK},
            {Material.NETHERRACK, Material.MAGMA_BLOCK, Material.OBSIDIAN, Material.MAGMA_BLOCK, Material.NETHERRACK},
            {Material.NETHERRACK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.MAGMA_BLOCK, Material.NETHERRACK},
            {Material.NETHERRACK, Material.NETHERRACK, Material.NETHERRACK, Material.NETHERRACK, Material.NETHERRACK}
    };

    Material[][] enchantAlter = {
            {Material.LAPIS_BLOCK, Material.LAPIS_BLOCK, Material.LAPIS_BLOCK, Material.LAPIS_BLOCK, Material.LAPIS_BLOCK},
            {Material.LAPIS_BLOCK, Material.BLUE_CONCRETE, Material.BLUE_TERRACOTTA, Material.BLUE_CONCRETE, Material.LAPIS_BLOCK},
            {Material.LAPIS_BLOCK, Material.BLUE_TERRACOTTA, Material.ENCHANTING_TABLE, Material.BLUE_TERRACOTTA, Material.LAPIS_BLOCK},
            {Material.LAPIS_BLOCK, Material.BLUE_CONCRETE, Material.BLUE_TERRACOTTA, Material.BLUE_CONCRETE, Material.LAPIS_BLOCK},
            {Material.LAPIS_BLOCK, Material.LAPIS_BLOCK, Material.LAPIS_BLOCK, Material.LAPIS_BLOCK, Material.LAPIS_BLOCK},
    };

    // Define the altar's block layout patterns
    private Material[][][] loadAltarPatterns() {
        List<Material[][]> patternList = new ArrayList<>();
        new DefaultRecipes().registerAltarPatterns(patternList);
        return patternList.toArray(new Material[0][][]);
    }

    // Load recipes
    public void loadRecipes() {
        recipes.clear();
        new DefaultRecipes().registerRecipes(recipes);
        Debug.logInfo("Loaded " + recipes.size() + " Magic Altar recipes");
    }

    // Check if the altar structure is correct
    public boolean checkAltarStructure(Location centerDispenserLoc) {
        Location altarBase = centerDispenserLoc.clone().subtract(0, 1, 0);

        for (Material[][] pattern : altarPatterns) {
            boolean matches = true;

            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Block block = altarBase.clone().add(x, 0, z).getBlock();
                    Material expected = pattern[x + 2][z + 2];
                    Material actual = block.getType();

                    if (actual != expected) {
                        matches = false;
                        break;
                    }
                }
                if (!matches) break;
            }

            if (matches) {
                return true;
            }
        }

        return false;
    }

    // Check the dispenser layout
    public boolean checkDispenserLayout(Location centerDispenserLoc) {
        if (centerDispenserLoc.getBlock().getType() != Material.DISPENSER) {
            return false;
        }

        int dispenserCount = 1;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;

                Location loc = centerDispenserLoc.clone().add(x, 0, z);
                if (loc.getBlock().getType() == Material.DISPENSER) {
                    dispenserCount++;
                }
            }
        }

        return dispenserCount == 9;
    }

    // Check the item frame
    public boolean checkItemFrame(Location centerDispenserLoc) {
        Location frameLoc = centerDispenserLoc.clone().add(0, 1, 0);

        // E1 fix: the old getWorld().getEntitiesByClass(ItemFrame.class) went through every entity in the whole world,
        // so now we follow how completeCrafting already uses getNearbyEntitiesByType, with radius 2 (covers the real size of the altar) for a local search
        for (org.bukkit.entity.ItemFrame itemFrame : frameLoc.getWorld().getNearbyEntitiesByType(org.bukkit.entity.ItemFrame.class, frameLoc, 2)) {
            Location itemFrameLoc = itemFrame.getLocation();
            if (itemFrameLoc.getBlockX() == frameLoc.getBlockX() &&
                    itemFrameLoc.getBlockY() == frameLoc.getBlockY() &&
                    itemFrameLoc.getBlockZ() == frameLoc.getBlockZ()) {

                ItemStack frameItem = itemFrame.getItem();
                if (frameItem == null || frameItem.getType() == Material.AIR) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    // Get the items from all dispensers - in a fixed order
    public ItemStack[][] getDispenserItems(Location centerDispenserLoc) {
        ItemStack[][] items = new ItemStack[9][9];

        // Define the dispenser position order (fixed order, starting from top left, going clockwise)
        int[][] positions = {
                {-1, -1}, // 0: top left
                {0, -1},  // 1: top center
                {1, -1},  // 2: top right
                {-1, 0},  // 3: middle left
                {0, 0},   // 4: center
                {1, 0},   // 5: middle right
                {-1, 1},  // 6: bottom left
                {0, 1},   // 7: bottom center
                {1, 1}    // 8: bottom right
        };

        // Get all dispenser items in the fixed order
        for (int i = 0; i < 9; i++) {
            int[] pos = positions[i];
            Location loc = centerDispenserLoc.clone().add(pos[0], 0, pos[1]);

            if (loc.getBlock().getState() instanceof Dispenser) {
                Dispenser dispenser = (Dispenser) loc.getBlock().getState();

                for (int slot = 0; slot < 9; slot++) {
                    ItemStack item = dispenser.getInventory().getItem(slot);
                    items[i][slot] = (item != null) ? item.clone() : null;
                }
            }
        }

        return items;
    }

    // Start the crafting process - positions must match exactly
    public boolean startCrafting(Location centerDispenserLoc, Player player) {
        // E4 fix: check land protection before starting the crafting process (copied from how getProtectionManager is already used in the project),
        // players without interaction permission can't start altar crafting
        if (!Slimefun.getProtectionManager().hasPermission(player, centerDispenserLoc, Interaction.INTERACT_BLOCK)) {
            player.sendMessage("§c✗ You don't have permission to use the Magic Altar here!");
            return false;
        }

        if (activeAltars.contains(centerDispenserLoc)) {
            player.sendMessage("§c✗ This altar is already crafting something!");
            return false;
        }

        // Check the structure
        if (!checkAltarStructure(centerDispenserLoc)) {
            player.sendMessage("§c✗ The altar base structure is wrong!");
            return false;
        }

        if (!checkDispenserLayout(centerDispenserLoc)) {
            player.sendMessage("§c✗ The dispenser layout is wrong!");
            return false;
        }

        if (!checkItemFrame(centerDispenserLoc)) {
            player.sendMessage("§c✗ You need an empty item frame above the center dispenser!");
            return false;
        }

        ItemStack[][] items = getDispenserItems(centerDispenserLoc);
        MagicAltarRecipe matchedRecipe = null;
        String recipeName = null;

        // Send the dispenser item info to the player
        sendDispenserItemsToPlayer(player, items);

        for (java.util.Map.Entry<String, MagicAltarRecipe> entry : recipes.entrySet()) {
            String currentRecipeName = entry.getKey();
            MagicAltarRecipe recipe = entry.getValue();

            if (recipe.matches(items)) {
                matchedRecipe = recipe;
                recipeName = currentRecipeName;
//                Bukkit.getLogger().info("Found a matching recipe: " + recipeName);
                break;
            }
        }

        if (matchedRecipe == null) {
            player.sendMessage("§c✗ No matching recipe found!");
            player.sendMessage("§7Please check that the item amounts and positions are right");
            return false;
        }

        activeAltars.add(centerDispenserLoc);
        player.sendMessage("§a✓ Recipe found: §e" + recipeName);
        player.sendMessage("§a▶ Crafting started! Don't move any items...");

        startParticleEffect(centerDispenserLoc);

        // Create the crafting task - 15 seconds of crafting time
        MagicAltarRecipe finalMatchedRecipe = matchedRecipe;
        BukkitTask task = new BukkitRunnable() {
            int timeLeft = 15; // 15 seconds of crafting time

            @Override
            public void run() {
                // Check the structure every second
                if (!checkStructureIntegrity(centerDispenserLoc, player)) {
                    player.sendMessage("§c✗ Crafting interrupted! The altar structure was broken or items were moved!");
                    stopParticleEffect(centerDispenserLoc);
                    activeAltars.remove(centerDispenserLoc);
                    this.cancel();
                    altarTasks.remove(centerDispenserLoc);
                    return;
                }

                // Progress messages
                if (timeLeft == 11) {
                    player.sendMessage("§6⏳ Crafting progress: §e25% §6done");
                } else if (timeLeft == 8) {
                    player.sendMessage("§6⏳ Crafting progress: §e50% §6done");
                } else if (timeLeft == 4) {
                    player.sendMessage("§6⏳ Crafting progress: §e75% §6done");
                } else if (timeLeft <= 0) {
                    // Crafting finished
                    completeCrafting(centerDispenserLoc, finalMatchedRecipe, player);
                    stopParticleEffect(centerDispenserLoc);
                    strikeColoredLightning(centerDispenserLoc, 18);
                    this.cancel();
                    altarTasks.remove(centerDispenserLoc);
                    return;
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Runs once every second

        altarTasks.put(centerDispenserLoc, task);
        return true;
    }


    public void strikeColoredLightning(Location center, int count) {
        World world = center.getWorld();

        // Move the center position to the true center of the block
        Location centerLoc = center.clone().add(0.5, 0, 0.5);

        for (int i = 0; i < count; i++) {
            // Make a random offset (based on the block center)
            double offsetX = (Math.random() - 0.5) * 8; // Smaller offset range
            double offsetZ = (Math.random() - 0.5) * 8;
            double offsetY = Math.random() * 2; // Smaller height change range

            Location strikeLocation = centerLoc.clone().add(offsetX, offsetY, offsetZ);

            // Make the colored lightning effect
            createColoredLightningEffect(strikeLocation);

            // Play thunder sound
            world.playSound(strikeLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        }
    }

    private void createColoredLightningEffect(Location location) {
        World world = location.getWorld();
        Particle.DustOptions dustOptions = getRandomLightningColor();

        // E3 fix: the main lightning particles are now limited to 10 blocks above the strike point (it was 70 blocks), and fewer particles per block (about half)
        for (int y = 10; y >= 0; y--) {
            Location particleLoc = location.clone().add(0, y, 0);

            // Increase particle count (E3: 15 → 8, about half)
            world.spawnParticle(Particle.DUST, particleLoc, 8, 0.2, 0.2, 0.2, 0, dustOptions);

            // Add branch effect
            if (Math.random() < 0.4) {
                double branchX = (Math.random() - 0.5) * 1.5;
                double branchZ = (Math.random() - 0.5) * 1.5;

                for (int branch = 0; branch < 2; branch++) {
                    Location branchLoc = particleLoc.clone().add(branchX * branch, 0, branchZ * branch);
                    // E3: 6 → 3, about half
                    world.spawnParticle(Particle.DUST, branchLoc, 3, 0.15, 0.15, 0.15, 0, dustOptions);
                }
            }

            // Spark effect
            if (Math.random() < 0.5) {
                for (int spark = 0; spark < 2; spark++) {
                    Location sparkLoc = particleLoc.clone().add(
                            (Math.random() - 0.5) * 2,
                            (Math.random() - 0.5) * 1.5,
                            (Math.random() - 0.5) * 2
                    );
                    // E3: spark particles 4 → 2, crit particles 2 → 1, about half
                    world.spawnParticle(Particle.FIREWORK, sparkLoc, 2, 0.1, 0.1, 0.1, 0.05);
                    world.spawnParticle(Particle.CRIT, sparkLoc, 1, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }

        // Ground shockwave effect - centered on the lightning strike point
        for (int i = 0; i < 2; i++) {
            double radius = 1.0 + i * 0.5;
            for (int angle = 0; angle < 360; angle += 20) {
                double rad = Math.toRadians(angle);
                double x = Math.cos(rad) * radius;
                double z = Math.sin(rad) * radius;
                Location circleLoc = location.clone().add(x, 0.2, z);
                world.spawnParticle(Particle.DUST, circleLoc, 2, 0.1, 0.1, 0.1, 0, dustOptions);
            }
        }
    }

    private Particle.DustOptions getRandomLightningColor() {
        int colorChoice = (int) (Math.random() * 5);
        switch (colorChoice) {
            case 0: // Blue lightning
                return new Particle.DustOptions(Color.fromRGB(0, 150, 255), 2.0f);
            case 1: // Purple lightning
                return new Particle.DustOptions(Color.fromRGB(150, 0, 255), 2.0f);
            case 2: // Cyan lightning
                return new Particle.DustOptions(Color.fromRGB(0, 255, 255), 2.0f);
            case 3: // Red lightning
                return new Particle.DustOptions(Color.fromRGB(255, 50, 50), 2.0f);
            default: // White lightning
                return new Particle.DustOptions(Color.fromRGB(255, 255, 255), 2.0f);
        }
    }


    // Check structure integrity (keeps checking during crafting)
    private boolean checkStructureIntegrity(Location centerDispenserLoc, Player player) {
        // Check the altar base
        if (!checkAltarStructure(centerDispenserLoc)) {
            return false;
        }

        // Check the dispenser layout
        if (!checkDispenserLayout(centerDispenserLoc)) {
            return false;
        }

        // Check the item frame
        if (!checkItemFrame(centerDispenserLoc)) {
            return false;
        }

        // Check if the recipe items are still there
        ItemStack[][] currentItems = getDispenserItems(centerDispenserLoc);
        for (MagicAltarRecipe recipe : recipes.values()) {
            if (recipe.matches(currentItems)) {
                return true; // At least one recipe matches, so the items are still there
            }
        }

        return false; // No recipe matches, so the items were moved
    }

    // Send the dispenser item info to the player
    private void sendDispenserItemsToPlayer(Player player, ItemStack[][] items) {
        player.sendMessage("§6=== Dispenser Item Info ===");

        String[] dispenserNames = {
                "§bTop Left Dispenser", "§bTop Center Dispenser", "§bTop Right Dispenser",
                "§bMiddle Left Dispenser", "§bCenter Dispenser", "§bMiddle Right Dispenser",
                "§bBottom Left Dispenser", "§bBottom Center Dispenser", "§bBottom Right Dispenser"
        };

        for (int i = 0; i < 9; i++) {
            boolean hasItems = false;
            StringBuilder itemList = new StringBuilder();

            for (int slot = 0; slot < 9; slot++) {
                if (items[i][slot] != null) {
                    hasItems = true;
                    if (itemList.length() > 0) itemList.append(", ");
                    itemList.append(ItemStackHelper.getDisplayName(items[i][slot])).append("x").append(items[i][slot].getAmount());
                }
            }

            if (hasItems) {
                player.sendMessage(dispenserNames[i] + ": " + itemList.toString());
            } else {
                player.sendMessage(dispenserNames[i] + ": §7Empty");
            }
        }
        player.sendMessage("§6===================");
    }

    // Start particle effect
    private void startParticleEffect(Location centerDispenserLoc) {
        // The target position is the exact center of the center block
        Location targetLoc = centerDispenserLoc.clone().add(0.5, 1.5, 0.5);

        BukkitTask particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Radius set to 1, with the true center of the center block as the origin
                double radius = 2.0;
                int particleSources = 12;

                // Spawn particles around that fly toward the center
                for (int i = 0; i < particleSources; i++) {
                    double angle = 2 * Math.PI * i / particleSources;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    // Also spawn particles in the vertical direction
                    for (int h = 0; h < 3; h++) {
                        double y = (h - 1) * 0.5; // -0.5, 0, 0.5 three heights

                        // Source position offset from the target position
                        Location sourceLoc = targetLoc.clone().add(x, y, z);
                        Vector direction = targetLoc.clone().subtract(sourceLoc).toVector().normalize();

                        // Each source spawns particles
                        for (int j = 0; j < 2; j++) {
                            // Small random offset
                            double offsetX = (Math.random() - 0.5) * 0.6;
                            double offsetY = (Math.random() - 0.5) * 0.6;
                            double offsetZ = (Math.random() - 0.5) * 0.6;

                            Location particleLoc = sourceLoc.clone().add(offsetX, offsetY, offsetZ);

                            // Use several particle types
                            Particle particleType = Math.random() < 0.9 ? Particle.ENCHANT : Particle.ELECTRIC_SPARK;

                            sourceLoc.getWorld().spawnParticle(
                                    particleType,
                                    particleLoc,
                                    6,
                                    direction.getX() * 0.15,
                                    direction.getY() * 0.15,
                                    direction.getZ() * 0.15,
                                    0.1
                            );
                        }
                    }
                }

                // Gathering effect at the center - in the middle of the block
                targetLoc.getWorld().spawnParticle(
                        Particle.WITCH,
                        targetLoc,
                        10,
                        0.2, 0.2, 0.2,
                        0.1
                );

                targetLoc.getWorld().spawnParticle(
                        Particle.ENCHANT,
                        targetLoc,
                        23,
                        0.15, 0.15, 0.15,
                        0.08
                );
            }
        }.runTaskTimer(plugin, 0L, 4L);

        particleTasks.put(centerDispenserLoc, particleTask);
    }

    // Stop particle effect
    private void stopParticleEffect(Location centerDispenserLoc) {
        BukkitTask particleTask = particleTasks.remove(centerDispenserLoc);
        if (particleTask != null) {
            particleTask.cancel();
        }
    }

    // Finish crafting
    private void completeCrafting(Location centerDispenserLoc, MagicAltarRecipe recipe, Player player) {
        ItemStack[][] dispenserItems = getDispenserItems(centerDispenserLoc);

        // Define the dispenser position order (fixed order)
        int[][] positions = {
                {-1, -1}, // 0: top left
                {0, -1},  // 1: top center
                {1, -1},  // 2: top right
                {-1, 0},  // 3: middle left
                {0, 0},   // 4: center
                {1, 0},   // 5: middle right
                {-1, 1},  // 6: bottom left
                {0, 1},   // 7: bottom center
                {1, 1}    // 8: bottom right
        };

        // Clear all the recipe items inside all dispensers directly (leave the blocks themselves alone)
        for (int i = 0; i < 9; i++) {
            int[] pos = positions[i];
            Location loc = centerDispenserLoc.clone().add(pos[0], 0, pos[1]);

            // Make sure the position is right
            if (loc.getBlock().getType() == Material.DISPENSER) {
                BlockState state = loc.getBlock().getState();
                if (state instanceof Dispenser) {
                    Dispenser dispenser = (Dispenser) state;

                    // E2 fix: no more rebuilding the dispenser with setType(AIR)+setType(DISPENSER) -
                    // that destroyed the block entity and lost any data besides the facing direction (like NBT written by other plugins);
                    // here we just empty the inventory, which is enough to "remove the recipe items"
                    Inventory inventory = dispenser.getInventory();
                    for (int slot = 0; slot < inventory.getSize(); slot++) {
                        inventory.setItem(slot, null);
                    }
                    dispenser.update(true, false);
                }
            }
        }

        // Put the result item into the item frame
        Location frameLoc = centerDispenserLoc.clone().add(0, 1, 0);
        for (ItemFrame itemFrame : frameLoc.getWorld().getNearbyEntitiesByType(org.bukkit.entity.ItemFrame.class, frameLoc, 0.5)) {
            Location itemFrameLoc = itemFrame.getLocation();
            if (itemFrameLoc.getBlockX() == frameLoc.getBlockX() &&
                    itemFrameLoc.getBlockY() == frameLoc.getBlockY() &&
                    itemFrameLoc.getBlockZ() == frameLoc.getBlockZ()) {

                ItemStack result = recipe.getResult().clone();

                if (result.getAmount() > 1) {
                    // If the amount is more than 1, only put 1 in the item frame
                    ItemStack displayItem = result.clone();
                    displayItem.setAmount(1);
                    itemFrame.setItem(displayItem);

                    // Drop the rest of the items (dropped in batches)
                    ItemStack dropItem = result.clone();
                    dropItem.setAmount(result.getAmount() - 1);
                    dropItemInBatches(player, itemFrame.getLocation(), dropItem);
                } else {
                    // If there's only 1, just put it in the item frame
                    itemFrame.setItem(result);
                }
                break;
            }
        }

        Location targetLoc = centerDispenserLoc.clone().add(0.5, 1.5, 0.5);
        targetLoc.getWorld().spawnParticle(Particle.FIREWORK, targetLoc, 30, 0.5, 0.5, 0.5, 0.1);
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        player.sendMessage("§a✓ Crafting done! 100% §aYou got " + recipe.getResult().getType().name());
        activeAltars.remove(centerDispenserLoc);
    }

    // Method for dropping items in batches
    private static void dropItemInBatches(Player player, Location location, ItemStack item) {
        int totalAmount = item.getAmount();
        int maxDropSize = 64; // Max amount per drop

        while (totalAmount > 0) {
            // Make a new copy of the item and set the amount for this batch
            ItemStack dropItem = item.clone();
            int currentBatchSize = Math.min(totalAmount, maxDropSize);
            dropItem.setAmount(currentBatchSize);

            // Drop this batch of items
            player.getWorld().dropItemNaturally(location, dropItem);

            // Update the remaining amount
            totalAmount -= currentBatchSize;
        }
    }
    // Cancel all tasks
    public void cancelAllTasks() {
        for (BukkitTask task : altarTasks.values()) {
            task.cancel();
        }
        altarTasks.clear();

        for (BukkitTask task : particleTasks.values()) {
            task.cancel();
        }
        particleTasks.clear();

        activeAltars.clear();
    }

    public boolean isAltarActive(Location centerDispenserLoc) {
        return activeAltars.contains(centerDispenserLoc);
    }

    public HashMap<String, MagicAltarRecipe> getRecipes() {
        return recipes;
    }
}
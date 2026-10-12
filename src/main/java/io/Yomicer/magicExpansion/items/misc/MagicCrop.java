package io.Yomicer.magicExpansion.items.misc;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.items.electric.entitykillMachinee.EntityKillMachine;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.Placeable;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import net.guizhanss.guizhanlib.slimefun.utils.BlockStorageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientNameVer2;

public class MagicCrop extends SlimefunItem implements Listener, RecipeDisplayItem, Placeable {

    // D Fix: set of block materials this plugin's magic crops may take up (seed items WHEAT_SEEDS/POTATO/CARROT/BEETROOT_SEEDS
    // block after planting: wheat/potato/carrot/beetroot), used for the quick material filter in BlockPhysicsEvent
    private static final Set<Material> CROP_BLOCK_MATERIALS = new HashSet<>(Arrays.asList(
            Material.WHEAT, Material.POTATOES, Material.CARROTS, Material.BEETROOTS
    ));

    private final ItemStack seedDrop;
//    private final ItemStack[] fruitDrops;
    private final List<WeightedDrop> weightedDrops;
    private final int totalWeight;
    private final int min;
    private final int minDisplay;
    private final int maxDisplay;
    private final int bound;

    public static class WeightedDrop {
        public final ItemStack item;
        public final int weight;

        public WeightedDrop(ItemStack item, int weight) {
            this.item = item;
            this.weight = weight;
        }
    }

    public MagicCrop(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, List<WeightedDrop> drops, int min, int max) {
        super(itemGroup, item, recipeType, recipe);
        this.seedDrop = item;
        this.weightedDrops = new ArrayList<>(drops);
        this.min = min-1;
        this.minDisplay = min;
        this.maxDisplay = max;
        this.bound = max - min;
        int sum = 0;
        for (WeightedDrop drop : weightedDrops) {
            if (drop.weight > 0) {
                sum += drop.weight;
            }
        }
        this.totalWeight = sum;
        addItemHandler(onBlockBreak(), onBlockPlace());
        Bukkit.getPluginManager().registerEvents(this, MagicExpansion.getInstance());
    }
    /**
     * Handle player breaking (Normal harvest)
     */
    @Nonnull
    private BlockBreakHandler onBlockBreak() {
        return new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(BlockBreakEvent e, ItemStack item, List<ItemStack> drops) {
                Block block = e.getBlock();
                SlimefunItem sfItem = StorageCacheUtils.getSfItem(block.getLocation());
                if (sfItem != MagicCrop.this) {
                    return;
                }
                handleCropDrops(block, drops);
            }
        };
    }
    @Nonnull
    private BlockPlaceHandler onBlockPlace() {
        return new BlockPlaceHandler(true) {
            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                StorageCacheUtils.setData(e.getBlockPlaced().getLocation(), "type", "none");
            }
        };
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent e) {
        Block block = e.getBlock();
        // D Fix: BlockPhysicsEvent fires very often, so filter by crop block material first,
        // non-crop blocks return right away, so we don't query BlockStorage on every physics update (high performance cost)
        if (!CROP_BLOCK_MATERIALS.contains(block.getType())) {
            return;
        }
        Block below = block.getRelative(0, -1, 0);
        SlimefunItem sfItem = StorageCacheUtils.getSfItem(block.getLocation());
        if (sfItem != MagicCrop.this) {
            return;
        }
        if (block.getType() == Material.WATER || block.getType() == Material.AIR) {
            handleDestructionByWater(block, e);
            return;
        }

        if (below.getType() == Material.FARMLAND || below.getType() == Material.SOUL_SAND) {
            return;
        }
        handleDestruction(block, e);
    }

    private void handleDestructionByWater(Block block, BlockPhysicsEvent e) {
        e.setCancelled(true);
        Location loc = block.getLocation();
        Slimefun.getDatabaseManager().getBlockDataController().removeBlock(loc.clone());
        block.setType(Material.AIR, false);
    }

    /**
     * Shared destroy and drop logic
     */
    private void handleDestruction(Block block, BlockPhysicsEvent e) {
        Location loc = block.getLocation();

        // A. Clean up Slimefun data
        Slimefun.getDatabaseManager().getBlockDataController().removeBlock(loc.clone());

        // B. Get the growth stage to work out the drops
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            // If the data is broken or it's not a crop, just clear it
            block.setType(Material.AIR, false);
            return;
        }

        int age = ageable.getAge();
        int maxAge = ageable.getMaximumAge();

        // C. Cancel the vanilla physics behavior (Stops vanilla from washing the block away without dropping anything, or turning it into another block)
        e.setCancelled(true);

        // D. Spawn custom drops
        Location dropLoc = loc.clone().add(0.5, 0.5, 0.5);

        if (age < maxAge) {
            // Not ripe: only drops seeds
            block.getWorld().dropItemNaturally(dropLoc, new CustomItemStack(seedDrop, 1));
        } else {
            // Ripe: drops seed + fruits
            block.getWorld().dropItemNaturally(dropLoc, new CustomItemStack(seedDrop, 1));

            // Spawn the given number of fruits
            int fruitCount = min + ThreadLocalRandom.current().nextInt(bound);
            for (int i = 0; i < fruitCount; i++) {
                if (!weightedDrops.isEmpty()) {
                    ItemStack selected = getWeightedRandomItem();
                    if (selected != null) {
                        block.getWorld().dropItemNaturally(dropLoc, new CustomItemStack(selected, 1));
                    }
                }
            }
        }

        // E. Remove block (Use false to avoid triggering physics events again and again)
        block.setType(Material.AIR, false);
    }

    /**
     * Drop calculation when a player breaks it (reuse the logic)
     */
    private void handleCropDrops(Block block, List<ItemStack> drops) {
        if (!(block.getBlockData() instanceof Ageable ageable)) return;

        int age = ageable.getAge();
        int maxAge = ageable.getMaximumAge();
        drops.clear();
        if (age >= maxAge) {
            drops.add(new CustomItemStack(seedDrop, 1));

            int fruitCount = min + ThreadLocalRandom.current().nextInt(bound);
            for (int i = 0; i < fruitCount; i++) {
                if (!weightedDrops.isEmpty()) {
                    ItemStack selected = getWeightedRandomItem();
                    if (selected != null) {
                        drops.add(new CustomItemStack(selected, 1));
                    }
                }
            }
        }
    }

    /**
     * Core algorithm: pick a random item by weight
     * Algorithm: generate a [1, totalWeight] random number, add up the weights one by one, and return the item whose range it lands in
     */
    private ItemStack getWeightedRandomItem() {
        if (totalWeight <= 0 || weightedDrops.isEmpty()) return null;

        int randomValue = ThreadLocalRandom.current().nextInt(totalWeight) + 1;
        int currentSum = 0;

        for (WeightedDrop drop : weightedDrops) {
            currentSum += drop.weight;
            if (randomValue <= currentSum) {
                return drop.item;
            }
        }
        // In theory it never gets here, unless the weight calculation is wrong
        return weightedDrops.get(weightedDrops.size() - 1).item;
    }

    @Override
    public @NotNull List<ItemStack> getDisplayRecipes() {
        List<ItemStack> display = new ArrayList<>();

        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientNameVer2("Harvest Time · Drop List➵"),
                getGradientNameVer2("After each ripening, harvesting randomly drops " + minDisplay + "~" + maxDisplay + " fruits"),
                getGradientNameVer2("Plant trait: always drops one seed of the plant itself"),
                getGradientNameVer2("You can use bone meal to ripen it faster"),
                getGradientNameVer2("Different fruits have different drop weights")));

        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientNameVer2("Harvest Time · Drop List➵"),
                getGradientNameVer2("After each ripening, harvesting randomly drops " + minDisplay + "~" + maxDisplay + " fruits"),
                getGradientNameVer2("Plant trait: always drops one seed of the plant itself"),
                getGradientNameVer2("You can use bone meal to ripen it faster"),
                getGradientNameVer2("Different fruits have different drop weights")));

        if (weightedDrops.isEmpty()) {
            return display;
        }

        // Work out and add the detailed chance of each fruit
        for (WeightedDrop drop : weightedDrops) {
            if (drop.item != null) {
                ItemStack displayFruit = drop.item.clone();
                ItemMeta meta = displayFruit.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.getLore();
                    if (lore == null) {
                        lore = new ArrayList<>();
                    }

                    lore.add(getGradientNameVer2("------------------"));

                    // Work out the percentage: (weight / total weight) * 100
                    double probability = (double) drop.weight / totalWeight * 100.0;
                    String probString;

                    // Format: show a whole number if it's an integer, otherwise keep two decimal places
                    if (probability == (long) probability) {
                        probString = String.format("%d%%", (long) probability);
                    } else {
                        probString = String.format("%.2f%%", probability);
                    }

                    lore.add(getGradientNameVer2("Drop weight: " + drop.weight));
                    lore.add(getGradientNameVer2("Drop chance: " + probString));

                    meta.setLore(lore);
                    displayFruit.setItemMeta(meta);
                }
                // To show up properly in the Slimefun machine UI, you usually need a placeholder or a paired item
                // To keep it simple, just add the item; Slimefun will try to pair them, and if there's an odd number the last one may not show its pair
                // If you need strict pairing, you can add an air or add it again here
                display.add(new CustomItemStack(Material.GRAY_STAINED_GLASS_PANE, " "));
                display.add(displayFruit);
            }
        }
        return display;
    }
}
package io.Yomicer.magicExpansion.items.misc.fish;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class CommonFish extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    public CommonFish(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return e -> {

            // Stop the default behavior (placing a block or using the item)
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);
            ItemStack item = e.getItem();

            if (!FishItemReader.isFishItem(item)) {
                return; // Not a fish item, just return
            }

            Player player = e.getPlayer();
            Fish fish = FishItemReader.getFishType(item);

            if (fish == null) {
                player.sendMessage("§cThis fish's data is corrupted, so we can't tell what kind it is.");
                return;
            }

            double weight = FishItemReader.getFishWeight(item);
            if (weight <= 0) {
                player.sendMessage("§cThis fish's weight data looks wrong.");
                return;
            }
            Fish.WeightRarity wr = FishItemReader.getWeightRarity(item);
            // Show the info normally
            player.sendMessage("§6You're holding a §e" + fish.getDisplayName() + "§6.");
            player.sendMessage("§7Weight: §f" + String.format("%.3f", weight) + "kg");
            if (wr != null) {
                player.sendMessage("§7Weight rarity: " + wr.getColorCode() + wr.getDisplayName());
            }

        };
    }

    /**
     * Safety check: is this item a valid fish item?
     */
    public static boolean isFishItem(ItemStack item) {
        if (item == null) return false;
        if (item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!meta.hasDisplayName()) return false; // Optional: check that it has a name
        var container = meta.getPersistentDataContainer();
        return container.has(FishKeys.FISH_TYPE, PersistentDataType.STRING);
    }
    /**
     * Safely get the fish's type
     * @return the matching Fish enum, or null if it fails
     */
    public static Fish getFishType(ItemStack item) {
        if (!isFishItem(item)) return null;

        try {
            ItemMeta meta = item.getItemMeta();
            String typeName = meta.getPersistentDataContainer()
                    .get(FishKeys.FISH_TYPE, PersistentDataType.STRING);

            if (typeName == null || typeName.trim().isEmpty()) {
                return null;
            }

            return Fish.valueOf(typeName.trim());
        } catch (IllegalArgumentException e) {
            // The enum value doesn't exist (for example, an old item left over after a plugin update)
            return null;
        } catch (Exception e) {
            // Any other unexpected error (like a broken container)
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Safely get the fish's weight
     * @return weight in kg, or 0.0 if it fails
     */
    public static double getFishWeight(ItemStack item) {
        if (!isFishItem(item)) return 0.0;

        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return 0.0;

            Double weight = meta.getPersistentDataContainer()
                    .get(FishKeys.FISH_WEIGHT, PersistentDataType.DOUBLE);

            // Check that it's a valid number
            if (weight == null || Double.isNaN(weight) || weight < 0) {
                return 0.0;
            }

            // Optional: check that it's within a reasonable range for this fish
            Fish fish = getFishType(item);
            if (fish != null) {
                if (weight < fish.getMinWeight() || weight > fish.getMaxWeight() * 2) {
                    // Slightly over is allowed (like with bonus boosts), but not crazy values
                    return 0.0;
                }
            }

            return weight;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }
    /**
     * Safely get the weight rarity
     * @return a WeightRarity enum, or COMMON_FISH if it fails
     */
    public static Fish.WeightRarity getWeightRarity(ItemStack item) {
        if (!isFishItem(item)) {
            return Fish.WeightRarity.COMMON_FISH; // Default value
        }

        try {
            ItemMeta meta = item.getItemMeta();
            String wrName = meta.getPersistentDataContainer()
                    .get(FishKeys.FISH_WEIGHT_RARITY, PersistentDataType.STRING);

            if (wrName == null || wrName.trim().isEmpty()) {
                // If nothing is saved, work it out again from the current weight
                double weight = getFishWeight(item);
                Fish fish = getFishType(item);
                if (fish != null && weight > 0) {
                    return fish.getWeightRarity(weight);
                }
                return Fish.WeightRarity.COMMON_FISH;
            }

            return Fish.WeightRarity.valueOf(wrName.trim());
        } catch (IllegalArgumentException e) {
            // The enum value doesn't exist, work it out again
            double weight = getFishWeight(item);
            Fish fish = getFishType(item);
            if (fish != null && weight > 0) {
                return fish.getWeightRarity(weight);
            }
            return Fish.WeightRarity.COMMON_FISH;
        } catch (Exception e) {
            e.printStackTrace();
            return Fish.WeightRarity.COMMON_FISH;
        }
    }

}
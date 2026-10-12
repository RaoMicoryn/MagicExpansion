package io.Yomicer.magicExpansion.items.misc.fish;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class FishItemReader {

    public static boolean isFishItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(FishKeys.FISH_TYPE, PersistentDataType.STRING);
    }

    public static Fish getFishType(ItemStack item) {
        if (!isFishItem(item)) return null;
        String typeName = item.getItemMeta().getPersistentDataContainer()
                .get(FishKeys.FISH_TYPE, PersistentDataType.STRING);
        // Fix (T): valueOf throws IllegalArgumentException for invalid/outdated enum names, so wrap it in try-catch and return null on failure (the caller must check for null)
        try {
            return Fish.valueOf(typeName);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    public static double getFishWeight(ItemStack item) {
        if (!isFishItem(item)) return 0.0;
        // Fix (T): if the weight field is missing, get returns null and unboxing would throw an NPE, so safely return the default 0.0
        Double weight = item.getItemMeta().getPersistentDataContainer()
                .get(FishKeys.FISH_WEIGHT, PersistentDataType.DOUBLE);
        return weight == null ? 0.0 : weight;
    }

    public static Fish.WeightRarity getWeightRarity(ItemStack item) {
        if (!isFishItem(item)) return null;
        String wrName = item.getItemMeta().getPersistentDataContainer()
                .get(FishKeys.FISH_WEIGHT_RARITY, PersistentDataType.STRING);
        // Fix (T): valueOf safely returns null for invalid/missing enum names (the caller must check for null)
        try {
            return Fish.WeightRarity.valueOf(wrName);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

}
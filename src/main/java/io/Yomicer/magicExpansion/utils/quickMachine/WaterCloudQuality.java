package io.Yomicer.magicExpansion.utils;

import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import org.bukkit.inventory.ItemStack;

/**
 * Water Cloud catch quality check
 * Only separates special catches from normal catches; rarity is not checked
 */
public class WaterCloudQuality {

    private WaterCloudQuality() {
    }

    public static boolean isSpecial(ItemStack drop, ItemStack specialCatch) {
        return drop != null && specialCatch != null && SlimefunUtils.isItemSimilar(drop, specialCatch, true);
    }
}

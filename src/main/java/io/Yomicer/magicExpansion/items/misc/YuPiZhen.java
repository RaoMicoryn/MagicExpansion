package io.Yomicer.magicExpansion.items.misc;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import org.bukkit.inventory.ItemStack;

/**
 * Raincoat Needle: a special item caught with the Drizzle Breeze Fishing Rod. It can’t be crafted;
 * its way to get it is shown through the custom RecipeType (Drizzle Breeze Fishing Rod) and recipe (Drizzle Breeze Bait).
 */
public class YuPiZhen extends SlimefunItem {

    public YuPiZhen(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }
}

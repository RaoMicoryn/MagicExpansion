package io.Yomicer.magicExpansion.items.misc;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import org.bukkit.inventory.ItemStack;

/**
 * Snow Spirit Pearl: a special item caught with the Snowy River Fishing Rod. It can’t be crafted;
 * its way to get it is shown through the custom RecipeType (Snowy River Fishing Rod) and recipe (Snowy River Bait).
 */
public class XuePaoZhu extends SlimefunItem {

    public XuePaoZhu(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }
}

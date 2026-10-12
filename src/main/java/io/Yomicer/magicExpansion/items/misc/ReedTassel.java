package io.Yomicer.magicExpansion.items.misc;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import org.bukkit.inventory.ItemStack;

/**
 * Reed Tassel: Can't be crafted, Obtained through a custom RecipeType(Green Bamboo Rod) and recipe(Green Bamboo Rod Bait)Show
 */
public class ReedTassel extends SlimefunItem {

    public ReedTassel(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }
}
package io.Yomicer.magicExpansion.items.misc;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import org.bukkit.inventory.ItemStack;

/**
 * White Reed Feather: Special catch from the Reed Blossom Fishing Rod, Can't be crafted;
 * Obtained through a custom RecipeType(Reed Blossom Fishing Rod) and recipe(Reed Blossom Fishing Bait)displayed.
 */
public class BaiLuYu extends SlimefunItem {

    public BaiLuYu(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }
}
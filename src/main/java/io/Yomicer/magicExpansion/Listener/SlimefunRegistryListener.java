package io.Yomicer.magicExpansion.Listener;

import io.Yomicer.magicExpansion.utils.log.Debug;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemRegistryFinalizedEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlimefunRegistryListener implements Listener {


    // Preloaded Enhanced Crafting Table recipes (static list).
    public static final List<Map<String, Integer>> ENHANCED_CRAFTING_TABLE_RECIPES = new ArrayList<>();

    public static final List<Map<String, Integer>> ARMOR_FORGE_RECIPES = new ArrayList<>();

    public static final List<Map<String, Integer>> ANCIENT_ALTAR_RECIPES = new ArrayList<>();

    public static final List<Map<String, Integer>> GOLD_PAN_RECIPES = new ArrayList<>();

    public static final List<Map<String, Integer>> HEATED_PRESSURE_CHAMBER_RECIPES = new ArrayList<>();

    public static final List<Map<String, Integer>> MAGIC_WORKBENCH_RECIPES = new ArrayList<>();

    public static final List<Map<String, Integer>> ORE_CRUSHER_RECIPES = new ArrayList<>();

    @EventHandler
    public void onSlimefunRegistryFinalized(SlimefunItemRegistryFinalizedEvent event) {

        loadRecipes(ENHANCED_CRAFTING_TABLE_RECIPES, RecipeType.ENHANCED_CRAFTING_TABLE);
        loadRecipes(ARMOR_FORGE_RECIPES, RecipeType.ARMOR_FORGE);
        loadRecipes(ANCIENT_ALTAR_RECIPES, RecipeType.ANCIENT_ALTAR);
        loadRecipes(GOLD_PAN_RECIPES, RecipeType.GOLD_PAN);
        loadRecipes(HEATED_PRESSURE_CHAMBER_RECIPES, RecipeType.HEATED_PRESSURE_CHAMBER);
        loadRecipes(MAGIC_WORKBENCH_RECIPES, RecipeType.MAGIC_WORKBENCH);
        loadRecipes(ORE_CRUSHER_RECIPES, RecipeType.ORE_CRUSHER);

    }

    private void loadRecipes(List<Map<String, Integer>> Recipes, RecipeType type) {

        // Preload all recipes of this type when the class loads.
        List<SlimefunItem> smelteryItems = Slimefun.getRegistry().getAllSlimefunItems().stream()
                .filter(item -> item.getRecipeType() == type)
                .toList();

        for (SlimefunItem item : smelteryItems) {
            ItemStack[] recipe = item.getRecipe();
            Map<String, Integer> recipeMap = new HashMap<>();

            // Go through each item in the recipe.
            for (ItemStack requiredItem : recipe) {
                if (requiredItem == null || requiredItem.getType() == Material.AIR) {
                    continue; // Skip empty slots.
                }

                int requiredAmount = requiredItem.getAmount();

                // Check whether this is a SlimefunItem.
                String itemKey;
                SlimefunItem slimefunItem = SlimefunItem.getByItem(requiredItem);
                if (slimefunItem != null) {
                    // If it is a SlimefunItem, use its ID as the key.
                    itemKey = "sf:" + slimefunItem.getId();
                } else {
                    // If it isn't a SlimefunItem, treat it as a vanilla Minecraft item.
                    itemKey = "mc:" + requiredItem.getType().name();
                }

                // Add up the amounts of matching items.
                recipeMap.put(itemKey, recipeMap.getOrDefault(itemKey, 0) + requiredAmount);
            }

            // Add the combined recipe to the list.
            Recipes.add(recipeMap);
        }

    }

}

package io.Yomicer.magicExpansion.items.quickMachine;

import io.Yomicer.magicExpansion.utils.log.Debug;
import io.Yomicer.magicExpansion.utils.quickMachine.QuickMachineMBUtile;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static io.Yomicer.magicExpansion.Listener.SlimefunRegistryListener.HEATED_PRESSURE_CHAMBER_RECIPES;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;
import static io.Yomicer.magicExpansion.utils.quickMachine.QuickMachineUtils.addAvailableRecipesToMenu;

public class MagicExpansionQuickHeatedPressureChamber extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    public MagicExpansionQuickHeatedPressureChamber(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
    }

//
//    // Preloaded Enhanced Crafting Table recipe list (static constant)
//    public static final List<Map<String, Integer>> HEATED_PRESSURE_CHAMBER_RECIPES = new ArrayList<>();
//
//    static {
//        // Load all SMELTERY recipes when the class loads
//        List<SlimefunItem> smelteryItems = Slimefun.getRegistry().getAllSlimefunItems().stream()
//                .filter(item -> item.getRecipeType() == RecipeType.HEATED_PRESSURE_CHAMBER)
//                .toList();
//
//        for (SlimefunItem item : smelteryItems) {
//            ItemStack[] recipe = item.getRecipe();
//            Map<String, Integer> recipeMap = new HashMap<>();
//
//            // Go through each recipe item
//            for (ItemStack requiredItem : recipe) {
//                if (requiredItem == null || requiredItem.getType() == Material.AIR) {
//                    continue; // Skip empty slots
//                }
//
//                int requiredAmount = requiredItem.getAmount();
//
//                // Check if this is a SlimefunItem
//                String itemKey;
//                SlimefunItem slimefunItem = SlimefunItem.getByItem(requiredItem);
//                if (slimefunItem != null) {
//                    // If it's a SlimefunItem, use its ID as the key
//                    itemKey = "sf:" + slimefunItem.getId();
//                } else {
//                    // If it isn't SlimefunItem，treat it as a regular Minecraft itemitem
//                    itemKey = "mc:" + requiredItem.getType().name();
//                }
//
//                // Combine matchingitem item amounts
//                recipeMap.put(itemKey, recipeMap.getOrDefault(itemKey, 0) + requiredAmount);
//            }
//
//            // Add the combined recipe to the list
//            HEATED_PRESSURE_CHAMBER_RECIPES.add(recipeMap);
//        }
////        Debug.logWarn("Preloaded HEATED_PRESSURE_CHAMBER_RECIPES: " + HEATED_PRESSURE_CHAMBER_RECIPES);
////        System.out.println("Number of preloaded ANCIENT_ALTAR_RECIPES: " + ANCIENT_ALTAR_RECIPES.size());
//    }



    @Nonnull
    @Override
    public ItemUseHandler getItemHandler() {
        return e->{
            // Prevent the default action (placing a block or using the item)
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);

            // Get the player
            Player player = e.getPlayer();
            // Open the menuand load recipes
            ChestMenu menu = new ChestMenu(getGradientName("Magic Heated Pressure Chamber"));
            addAvailableRecipesToMenu(player, menu,0, HEATED_PRESSURE_CHAMBER_RECIPES, RecipeType.HEATED_PRESSURE_CHAMBER);

            // Set whether empty slots can be clicked
            menu.setEmptySlotsClickable(false);
            menu.setPlayerInventoryClickable(false);
            // Show the menu to the player
            menu.open(player);


        };
    }
}

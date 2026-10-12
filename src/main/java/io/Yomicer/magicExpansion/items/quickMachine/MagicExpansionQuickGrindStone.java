package io.Yomicer.magicExpansion.items.quickMachine;

import io.Yomicer.magicExpansion.utils.quickMachine.QuickMachineMBUtile;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

import static io.Yomicer.magicExpansion.Listener.SlimefunRegistryFinalized.GRIND_STONE_RECIPES;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;

public class MagicExpansionQuickGrindStone extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    public MagicExpansionQuickGrindStone(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
    }

//    // Preloaded Grindstone recipe list (static constant)
//    private static final List<Map<String, Integer>> GRIND_STONE_RECIPES = new ArrayList<>();
//
//    /**
//     * Static initializer: load all GRIND_STONE recipes when the class loads
//     */
//    static {
//        loadGrindStoneRecipes();
//    }
//
//    /**
//     * Load all GRIND_STONE recipes
//     */
//    private static void loadGrindStoneRecipes() {
//        // Get the Grindstone instance
//        String id = SlimefunItems.GRIND_STONE.getItemId();
//
//            MultiBlockMachine multiBlockMachine = (MultiBlockMachine) MultiBlockMachine.getById(id);
//
//            // Get recipes using getRecipes()
//            List<ItemStack[]> recipes = multiBlockMachine.getRecipes();
//            if (recipes == null || recipes.isEmpty()) {
//                System.out.println("Couldn’t find any Grindstone recipes.");
//                return;
//            }
//
//            // Convert recipes into a Map
//            for (ItemStack[] recipePair : recipes) {
//                if (recipePair == null || recipePair.length < 2) {
//                    continue; // Skip invalid recipe pairs
//                }
//
//                ItemStack input = recipePair[0];
//                ItemStack output = recipePair[1];
//
//                if (input == null || input.getType() == Material.AIR || output == null || output.getType() == Material.AIR) {
//                    continue; // Skip invalid recipes
//                }
//
//                Map<String, Integer> recipeMap = new HashMap<>();
//
//                // Input item
//                String inputKey = getUniqueItemKey(input);
//                if (inputKey != null) {
//                    recipeMap.put(inputKey, input.getAmount());
//                }
//
//                // Output item
//                String outputKey = getUniqueItemKey(output);
//                if (outputKey != null) {
//                    recipeMap.put(outputKey, -output.getAmount()); // Negative numbers mean output
//                }
//
//                if (!recipeMap.isEmpty()) {
//                    GRIND_STONE_RECIPES.add(recipeMap);
//                }
//            }
//    }
//
//    /**
//     * Get the item's unique key (Slimefun ID or Minecraft material name)
//     *
//     * @param item item
//     * @return Unique key ("sf:<ID>" or "mc:<Material>"); returns null if it can't be identified
//     */
//    private static String getUniqueItemKey(ItemStack item) {
//        if (item == null || item.getType() == Material.AIR) {
//            return null;
//        }
//
//        SlimefunItem slimefunItem = SlimefunItem.getByItem(item);
//        return slimefunItem != null ? "sf:" + slimefunItem.getId() : "mc:" + item.getType().name();
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
            ChestMenu menu = new ChestMenu(getGradientName("Magic Grindstone"));
            QuickMachineMBUtile.addAvailableRecipesToMenu(player, menu,0, GRIND_STONE_RECIPES);

            // Set whether empty slots can be clicked
            menu.setEmptySlotsClickable(false);
            menu.setPlayerInventoryClickable(false);
            // Show the menu to the player
            menu.open(player);


        };
    }

}

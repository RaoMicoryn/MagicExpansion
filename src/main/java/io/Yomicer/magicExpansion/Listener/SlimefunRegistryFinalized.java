package io.Yomicer.magicExpansion.Listener;

import io.Yomicer.magicExpansion.utils.log.Debug;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemRegistryFinalizedEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SlimefunRegistryFinalized implements Listener {



    // Preloaded Grind Stone recipes (static list).
    public static final List<Map<String, Integer>> SMELTERY_RECIPES = new ArrayList<>();

    public static final List<Map<String, Integer>> GRIND_STONE_RECIPES = new ArrayList<>();

    public static final List<Map<String, Integer>> ORE_CRUSHER_RECIPES = new ArrayList<>();

//    public static final List<Map<String, Integer>> ARMOR_FORGE_RECIPES = new ArrayList<>();

    public static final List<Map<String, Integer>> COMPRESSOR_RECIPES = new ArrayList<>();

    public static final List<Map<String, Integer>> PRESSURE_CHAMBER_RECIPES = new ArrayList<>();

//    public static final List<Map<String, Integer>> MAGIC_WORKBENCH_RECIPES = new ArrayList<>();

//    public static final List<Map<String, Integer>> ORE_WASHER_RECIPES = new ArrayList<>();

//    public static final List<Map<String, Integer>> AUTOMATED_PANNING_MACHINE_RECIPES = new ArrayList<>();




    //Smeltery
    String ID1 = "SMELTERY";
    //Grind Stone
    String ID2 = "GRIND_STONE";
    //Ore Crusher
    String ID3 = "ORE_CRUSHER";
    //Armor Forge
//    String ID4 = "ARMOR_FORGE";
    //Compressor
    String ID5 = "COMPRESSOR";
    //Pressure Chamber
    String ID6 = "PRESSURE_CHAMBER";
    //Magic Workbench
//    String ID7 = "MAGIC_WORKBENCH";




    @EventHandler
    public void onSlimefunRegistryFinalized(SlimefunItemRegistryFinalizedEvent event) {


        loadGrindStoneRecipes(ID1,SMELTERY_RECIPES);
        loadGrindStoneRecipes(ID2,GRIND_STONE_RECIPES);
        loadGrindStoneRecipes(ID3,ORE_CRUSHER_RECIPES);
//        loadGrindStoneRecipes(ID4,ARMOR_FORGE_RECIPES);
        loadGrindStoneRecipes(ID5,COMPRESSOR_RECIPES);
        loadGrindStoneRecipes(ID6,PRESSURE_CHAMBER_RECIPES);
//        loadGrindStoneRecipes(ID7,MAGIC_WORKBENCH_RECIPES);

        Debug.logInfo("Recipe loading finished.");
//        Debug.logError("Grind Stone recipe list: "+ GRIND_STONE_RECIPES);
    }


    /**
     * Get the Grind Stone instance (or another MultiBlockMachine instance).
     *
     * @return The MultiBlockMachine instance, or null if it can't be found.
     */
    private static MultiBlockMachine getGrindStoneMachine(String id) {
        try {
            return (MultiBlockMachine) SlimefunItem.getById(id);
        } catch (Exception e) {
            Debug.logInfo("Error while getting the GRIND_STONE instance: " + e.getMessage());
            return null;
        }
    }

    /**
     * Load Grind Stone recipes and store them in the static list.
     */
    private static void loadGrindStoneRecipes(String id,List<Map<String, Integer>> recipes) {
        // Get the GRIND_STONE instance.
        MultiBlockMachine machine = getGrindStoneMachine(id);
        if (machine == null) {
            Debug.logInfo("Couldn't find GRIND_STONE, or it isn't a MultiBlockMachine.");
            return;
        }

        // Use getRecipeInputList and getRecipeOutputList to get the input and output items.
        List<ItemStack[]> inputList = getRecipeInputList(machine);
        List<ItemStack> outputList = getRecipeOutputList(machine);

        if (inputList == null || inputList.isEmpty() || outputList == null || outputList.isEmpty()) {
//            Debug.logInfo("Couldn't load a valid list of input or output items.");
//            System.out.println("Recipe type: "+machine);
            return;
        }

        // Make sure the number of input recipes matches the number of output recipes.
        if (inputList.size() != outputList.size()) {
//            Debug.logInfo("The number of input and output recipes doesn't match!");
            return;
        }

        int validRecipesCount = 0;

        for (int i = 0; i < inputList.size(); i++) {
            ItemStack[] input = inputList.get(i);
            ItemStack output = outputList.get(i);

            // Make sure both input and output are valid.
            if (Arrays.stream(input).allMatch(item -> item == null || item.getType() == Material.AIR)) {
//                Debug.logInfo("Invalid input item: " + Arrays.toString(input));
                continue;
            }

            if (output == null || output.getType() == Material.AIR) {
//                Debug.logInfo("Invalid output item: " + output);
                continue;
            }

            // Build the recipe data.
            Map<String, Integer> recipeMap = new LinkedHashMap<>();

            // Handle the input items.
            for (ItemStack requiredItem : input) {
                if (requiredItem == null || requiredItem.getType() == Material.AIR) {
                    continue; // Skip empty slots.
                }

                String itemKey = getUniqueItemKey(requiredItem); // Get the unique key.
                if (itemKey != null) {
                    recipeMap.put(itemKey, recipeMap.getOrDefault(itemKey, 0) + requiredItem.getAmount());
                } else {
//                    Debug.logInfo("Couldn't identify input item: " + requiredItem);
                }
            }


            // Add the combined recipe to the list.
            recipes.add(recipeMap);
            validRecipesCount++;

            // Log the successfully loaded recipe.
//            System.out.println("Recipe loaded: " + recipeMap);
        }

//        System.out.println("Loaded a total of " + validRecipesCount + " valid recipes.");
    }

    /**
     * Get all input items for the machine.
     *
     * @param machine The MultiBlockMachine instance.
     * @return A list of all input items (each entry is an ItemStack array).
     */
    public static List<ItemStack[]> getRecipeInputList(MultiBlockMachine machine) {
        if (machine == null) {
            return new ArrayList<>();
        }

        // Get the machine's recipe list.
        List<ItemStack[]> recipes = machine.getRecipes();
        List<ItemStack[]> inputs = new ArrayList<>();

        // Go through the recipe list and get the input items (even indexes).
        for (int i = 0; i < recipes.size(); i += 2) { // Each pair has two entries; the first is the input.
            inputs.add(recipes.get(i));
        }

        return inputs;
    }

    /**
     * Get all output items for the machine.
     *
     * @param machine The MultiBlockMachine instance.
     * @return A list of all output items.
     */
    public static List<ItemStack> getRecipeOutputList(MultiBlockMachine machine) {
        if (machine == null) {
            return new ArrayList<>();
        }

        // Get the machine's recipe list.
        List<ItemStack[]> recipes = machine.getRecipes();
        List<ItemStack> outputs = new ArrayList<>();

        // Go through the recipe list and get the output items (odd indexes).
        for (int i = 1; i < recipes.size(); i += 2) { // Each pair has two entries; the second is the output.
            ItemStack[] outputArray = recipes.get(i);
            if (outputArray != null && outputArray.length > 0) {
                outputs.add(outputArray[0]); // Use the first item as the output.
            }
        }

        return outputs;
    }

    /**
     * Get the item's unique key (Slimefun ID or Minecraft material name).
     *
     * @param item The item.
     * @return The unique key ("sf:<ID>" or "mc:<Material>"), or null if it can't be identified.
     */
    public static String getUniqueItemKey(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        try {
            SlimefunItem slimefunItem = SlimefunItem.getByItem(item);
            return slimefunItem != null ? "sf:" + slimefunItem.getId() : "mc:" + item.getType().name();
        } catch (Exception e) {
//            Debug.logInfo("Error while getting the item's unique key: " + e.getMessage());
            return null;
        }
    }


}

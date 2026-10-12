package io.Yomicer.magicExpansion.utils.quickMachine;

import io.Yomicer.magicExpansion.utils.CreateItem;
import io.Yomicer.magicExpansion.utils.MagicExpansionSlimefunItemCache;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static io.Yomicer.magicExpansion.utils.GiveItem.giveOrDropItem;
import static io.Yomicer.magicExpansion.utils.Utils.doGlow;
import static io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils.isItemSimilar;

public class QuickMachineUtils {

    public static boolean consumeMaterials(Player player, Map<String, Integer> recipe, int amount) {
        PlayerInventory inventory = player.getInventory();

        // D2: Make material use safe: first copy the inventory contents as a snapshot,
        // if removing items fails, restore the snapshot so no materials are lost
        ItemStack[] storageSnapshot = inventory.getStorageContents();
        ItemStack[] snapshotCopy = new ItemStack[storageSnapshot.length];
        for (int i = 0; i < storageSnapshot.length; i++) {
            snapshotCopy[i] = storageSnapshot[i] == null ? null : storageSnapshot[i].clone();
        }

        // D2: Step 1: Check all materials first. If there aren't enough, stop without changing the inventory
        if (!hasEnoughMaterials(getPlayerInventoryItems(player), recipe, amount)) {
            player.sendMessage("§cNot enough materials. Please check your inventory!");
            return false;
        }

        for (Map.Entry<String, Integer> requiredEntry : recipe.entrySet()) {
            String requiredKey = requiredEntry.getKey();
            int requiredAmountPerCraft = requiredEntry.getValue();
            int totalRequiredAmount = requiredAmountPerCraft * amount;

            if (requiredKey.startsWith("mc:")) {
                // Minecraft item
                Material material = Material.matchMaterial(requiredKey.substring(3));
                if (material != null) {
                    ItemStack targetItem = new ItemStack(material);
                    if (!removeVanillaItemsFromInventory(inventory, targetItem, totalRequiredAmount)) {
                        player.sendMessage("§cCouldn't remove enough Minecraft items. Please check your inventory!");
                        // D2: If something fails midway, restore the snapshot to return removed materials
                        inventory.setStorageContents(snapshotCopy);
                        return false;
                    }
                }
            } else if (requiredKey.startsWith("sf:")) {
                // Slimefun item
                String slimefunId = requiredKey.substring(3);
                SlimefunItem slimefunItem = SlimefunItem.getById(slimefunId);
                if (slimefunItem != null) {
                    ItemStack targetItem = slimefunItem.getItem().clone();
                    if (!removeSlimefunItemsFromInventory(inventory, targetItem, totalRequiredAmount)) {
                        player.sendMessage("§cCouldn't remove enough Slimefun items. Please check your inventory!");
                        // D2: If something fails midway, restore the snapshot to return removed materials
                        inventory.setStorageContents(snapshotCopy);
                        return false;
                    }
                }
            }
        }

        // All materials were removed successfully
        return true;
    }

    /**
     * Remove a set amount of Minecraft items from the player's inventory
     *
     * @param inventory player inventory
     * @param targetItem item to remove
     * @param amount     amount to remove
     * @return whether the requested amount was removed
     */
    private static boolean removeVanillaItemsFromInventory(PlayerInventory inventory, ItemStack targetItem, int amount) {
        ItemStack[] contents = inventory.getStorageContents(); // Get storage slots (not including armor slots)
        int remainingAmount = amount;

        for (int i = 0; i < contents.length && remainingAmount > 0; i++) {
            ItemStack itemInSlot = contents[i];
            if (itemInSlot != null && itemInSlot.isSimilar(targetItem)) {
                int removed = Math.min(itemInSlot.getAmount(), remainingAmount);
                itemInSlot.setAmount(itemInSlot.getAmount() - removed);
                remainingAmount -= removed;

                if (itemInSlot.getAmount() <= 0) {
                    contents[i] = null; // Clear this slot if the item amount is 0
                }
            }
        }

        inventory.setStorageContents(contents); // Update the storage slots

        return remainingAmount == 0; // If the remaining amount is 0, all items were removed
    }

    /**
     * Remove a set amount of Slimefun items from the player's inventory
     *
     * @param inventory player inventory
     * @param targetItem item to remove
     * @param amount     amount to remove
     * @return whether the requested amount was removed
     */
    private static boolean removeSlimefunItemsFromInventory(PlayerInventory inventory, ItemStack targetItem, int amount) {
        ItemStack[] contents = inventory.getStorageContents(); // Get storage slots (not including armor slots)
        int remainingAmount = amount;

        for (int i = 0; i < contents.length && remainingAmount > 0; i++) {
            ItemStack itemInSlot = contents[i];
            if (itemInSlot != null && isItemSimilar(itemInSlot, targetItem, false)) {
                int removed = Math.min(itemInSlot.getAmount(), remainingAmount);
                itemInSlot.setAmount(itemInSlot.getAmount() - removed);
                remainingAmount -= removed;

                if (itemInSlot.getAmount() <= 0) {
                    contents[i] = null; // Clear this slot if the item amount is 0
                }
            }
        }

        inventory.setStorageContents(contents); // Update the storage slots

        return remainingAmount == 0; // If the remaining amount is 0, all items were removed
    }


   /**
     * Get the recipe result
     * Get the recipe result from the recipe type and item array
     *
     * @param recipe      recipe array
     * @param recipeType  recipe type
     * @return recipe result
     */
    public static Pair<ItemStack, Integer> getRecipeResult(Map<String, Integer> recipe, RecipeType recipeType) {
        for (SlimefunItem item : Slimefun.getRegistry().getAllSlimefunItems()) {
            if (item.getRecipeType() == recipeType) {
                Map<String, Integer> recipeMap = convertRecipeToMap(item.getRecipe());
                if (recipeMap.equals(recipe)) {
                    // Return the matching output and its amount
                    ItemStack output = item.getRecipeOutput().clone();
                    int outputAmount = output != null ? output.getAmount() : 1; // Default amount is 1
                    return new Pair<>(output, outputAmount);
                }
            }
        }
        return null; // Return null if no matching item is found
    }


    /**
     * Combine matching items in the recipe
     * Convert the recipe array to a Map
     *
     * @param recipe recipe array
     * @return recipe Map
     */
    public static Map<String, Integer> convertRecipeToMap(ItemStack[] recipe) {
        Map<String, Integer> recipeMap = new HashMap<>();
        for (ItemStack requiredItem : recipe) {
            if (requiredItem == null || requiredItem.getType() == Material.AIR) {
                continue; // Skip empty slots
            }

            int requiredAmount = requiredItem.getAmount();

            // Check whether this is a SlimefunItem
            String itemKey;
            SlimefunItem slimefunItem = SlimefunItem.getByItem(requiredItem);
            if (slimefunItem != null) {
                // If it's a SlimefunItem, use its ID as the key
                itemKey = "sf:" + slimefunItem.getId();
            } else {
                // Otherwise, treat it as a Minecraft item
                itemKey = "mc:" + requiredItem.getType().name();
            }

            // Add up the amounts of matching items
            recipeMap.put(itemKey, recipeMap.getOrDefault(itemKey, 0) + requiredAmount);
        }
        return recipeMap;
    }


    /**
     * Get the Slimefun item name
     * Get the display name based on the item type
     *
     * @param item item
     * @return display name
     */
    public static String getDisplayName(ItemStack item) {
        Material material = item.getType();
        SlimefunItem slimefunItem = SlimefunItem.getByItem(item);

        if (slimefunItem != null) {
            // If it's a SlimefunItem, return its ID
            return slimefunItem.getItemName();
        } else {
            // If it's a Minecraft item, return its English name
            return material.name();
        }
    }

    /**
     * Get display names for required Minecraft and Slimefun items
     * Get the display name for a recipe ingredient
     * Get the display name from the item key
     *
     * @param itemKey item key
     * @return display name
     */
    public static String getIngredientDisplayName(String itemKey) {
        if (itemKey.startsWith("mc:")) {
            // If it's a Minecraft item, get and return its Material name
            String materialName = itemKey.substring(3); // Remove the prefix "mc:"
            Material material = Material.matchMaterial(materialName);
            if (material != null) {
                // Create a temporary ItemStack
                ItemStack itemStack = new ItemStack(material);
                return ItemStackHelper.getName(itemStack);
            }
            return "Unknown Minecraft item (" + materialName + ")";
        } else if (itemKey.startsWith("sf:")) {
            // If it's a Slimefun item, get its ID and look up the matching SlimefunItem
            String slimefunId = itemKey.substring(3); // Remove the prefix "sf:"
            SlimefunItem slimefunItem = SlimefunItem.getById(slimefunId);
            if (slimefunItem != null) {
                return slimefunItem.getItemName(); // Return the Slimefun item's display name
            }
            return "Unknown Slimefun item (" + slimefunId + ")";
        } else {
            return "Unknown item type (" + itemKey + ")";
        }
    }



    /**
     * Find how many times the player can craft
     */
    public static int calculateMaxCraftableAmount(Map<String, Integer> playerItems, Map<String, Integer> recipe) {
        int maxCraftableAmount = Integer.MAX_VALUE;

        for (Map.Entry<String, Integer> requiredEntry : recipe.entrySet()) {
            String requiredKey = requiredEntry.getKey();
            int requiredAmount = requiredEntry.getValue();

            int availableAmount = playerItems.getOrDefault(requiredKey, 0);
            maxCraftableAmount = Math.min(maxCraftableAmount, availableAmount / requiredAmount);
        }

        return maxCraftableAmount;
    }


    /**
     * Get all items and their amounts in the player's inventory
     * and assign each item a key
     *
     * @param player player
     * @return a Map where the key is the item type or SlimefunItem ID and the value is its amount
     */
    public static Map<String, Integer> getPlayerInventoryItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        Map<String, Integer> itemCounts = new HashMap<>();

        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                // Check whether this is a SlimefunItem
                String itemKey;
                SlimefunItem slimefunItem = SlimefunItem.getByItem(item);
                if (slimefunItem != null) {
                    // If it's a SlimefunItem, use its ID as the key
                    itemKey = "sf:" + slimefunItem.getId();
                } else {
                    // Otherwise, treat it as a Minecraft item
                    itemKey = "mc:" + item.getType().name();
                }

                // Add up item amounts in the player's inventory
                itemCounts.put(itemKey, itemCounts.getOrDefault(itemKey, 0) + item.getAmount());
            }
        }

        return itemCounts;
    }



    /**
     * Check whether the player has enough items for the recipe
     *
     * @param playerItems all items and their amounts in the player's inventory
     * @param recipe      Recipe
     * @param amount      craft amount
     * @return Return true if the player has enough items, or false if not
     */
    public static boolean hasEnoughMaterials(Map<String, Integer> playerItems, Map<String, Integer> recipe, int amount) {
        // Check whether the player has enough items
        for (Map.Entry<String, Integer> requiredEntry : recipe.entrySet()) {
            String requiredKey = requiredEntry.getKey();
            int requiredAmount = requiredEntry.getValue() * amount; // Adjust the required materials based on the craft amount

            int availableAmount = playerItems.getOrDefault(requiredKey, 0);
            if (availableAmount < requiredAmount) {
                return false;
            }
        }
        return true;
    }

    // Overload with a default amount of 1
    public static boolean hasEnoughMaterials(Map<String, Integer> playerItems, Map<String, Integer> recipe) {
        return hasEnoughMaterials(playerItems, recipe, 1); // Call the method with the amount parameter, using 1 by default
    }

    /**
     * Get the recipe result and its amount
     *
     * @param recipe      Recipe
     * @param recipeType  recipe type
     * @return A Pair containing the recipe result: the first value is the ItemStack and the second is the result amount
     */
    private static Pair<ItemStack, Integer> getRecipeResultWithAmount(Map<String, Integer> recipe, RecipeType recipeType) {
        for (SlimefunItem item : MagicExpansionSlimefunItemCache.getAllSlimefunItems()) {
            if (item.getRecipeType() == recipeType) {
                Map<String, Integer> recipeMap = convertRecipeToMap(item.getRecipe());
                if (recipeMap.equals(recipe)) {
                    ItemStack output = item.getRecipeOutput().clone();
                    int outputAmount = output != null ? output.getAmount() : 1; // Default amount is 1
                    return new Pair<>(output, outputAmount);
                }
            }
        }
        return null; // Return null if no matching item is found
    }


    /**
     * Add matching recipes to the menu and support pages
     * Refresh logic
     *
     * @param player player
     * @param menu   menu
     * @param page   current page (starts at 0)
     */
    public static void addAvailableRecipesToMenu(Player player, ChestMenu menu, int page, List<Map<String, Integer>> ReceivedRecipes, RecipeType recipeType) {
        // Get all items and their amounts in the player's inventory
        Map<String, Integer> playerItems = getPlayerInventoryItems(player);

        // Clear the menu
        for (int i = 0; i < menu.getSize(); i++) {
            menu.addItem(i, null);
        }
        menu.addItem(49, new CustomItemStack(doGlow(Material.NETHER_STAR),"§x§F§D§B§7§D§4How to Use",
                        "§eright-click §bclick an item in the menu to see the materials needed for one craft",
                        "§eleft-click §bclick an item in the menu to craft once",
                        "§bHold §eShift right-click §bclick an item in the menu to craft 32 items at once",
                        "§bHold §eShift left-click §bclick an item in the menu to craft as many as you can"),
                (player1, slot, item, action) -> {
                    return false;
                });

        // Filter recipes the player can craft
        List<Map<String, Integer>> availableRecipes = ReceivedRecipes.stream()
                .filter(recipe -> hasEnoughMaterials(playerItems, recipe))
                .toList();

        // Page handling
        int itemsPerPage = 45; // maximum items shown per page
        int totalPages = (int) Math.ceil((double) availableRecipes.size() / itemsPerPage);
        page = Math.max(0, Math.min(page, totalPages - 1)); // Keep the page number in range

        // Recipes on the current page
        List<Map<String, Integer>> currentPageRecipes = availableRecipes.stream()
                .skip((long) page * itemsPerPage)
                .limit(itemsPerPage)
                .toList();

        // Add recipes to the menu
        int slot = 0;
        for (Map<String, Integer> recipe : currentPageRecipes) {
            // Get the recipe output and amount per craft
            Pair<ItemStack, Integer> recipeResult = getRecipeResultWithAmount(recipe, recipeType);
            if (recipeResult == null) continue;

            ItemStack resultItem = recipeResult.getFirstValue().clone(); // Copy the output so the original item isn't changed
            ItemStack resultItemDisplay = recipeResult.getFirstValue().clone(); // Copy the output so the original item isn't changed
            // Assume resultItemDisplay is a cloned ItemStack
            if (resultItemDisplay.hasItemMeta()) { // Make sure the ItemStack has ItemMeta
                ItemMeta itemMeta = resultItemDisplay.getItemMeta(); // Get ItemMeta
//                    String originalName = itemMeta.getDisplayName(); // Get the original display name
//                    String newName = "§x§F§D§B§7§D§4"+originalName + "§e [Output]"; // Add suffix [Output]
//                    itemMeta.setDisplayName(newName); // Set the new display name
                    // Prepare the lore list
                    List<String> lore = new ArrayList<>();
                    lore.add("");
                    // Add the 'Required Materials' heading
                    lore.add("§aRequired Materials:");
                    // Add each ingredient
                    for (Map.Entry<String, Integer> ingredient : recipe.entrySet()) {
                        String displayName = getIngredientDisplayName(ingredient.getKey());
                        int requiredAmount = ingredient.getValue();
                        lore.add(" §e- " + displayName + " x " + requiredAmount);
                    }
                    // Set the new lore in ItemMeta
                    itemMeta.setLore(lore);
                    resultItemDisplay.setItemMeta(itemMeta); // Apply the updated ItemMeta to the ItemStack
            }else{
//                String name = "§x§F§D§B§7§D§4"+ItemStackHelper.getName(resultItemDisplay);
//                String newName = name + "§e [Output]";
                ItemMeta meta = Bukkit.getItemFactory().getItemMeta(resultItemDisplay.getType());
//                meta.setDisplayName(newName);
                // Prepare the lore list
                List<String> lore = new ArrayList<>();
                lore.add("");
                // Add the 'Required Materials' heading
                lore.add("§aRequired Materials:");
                // Add each ingredient
                for (Map.Entry<String, Integer> ingredient : recipe.entrySet()) {
                    String displayName = getIngredientDisplayName(ingredient.getKey());
                    int requiredAmount = ingredient.getValue();
                    lore.add(" §e- " + displayName + " x " + requiredAmount);
                }
                // Set the new lore in ItemMeta
                meta.setLore(lore);
                resultItemDisplay.setItemMeta(meta);
            }
            int outputAmountPerCraft = recipeResult.getSecondValue(); // Get the amount produced per craft

            // Create a local variable, currentSlot, to store the current slot
            int currentSlot = slot++;
            menu.addItem(currentSlot, resultItemDisplay, (p, s, item, action) -> {
                if (action.isRightClicked() && !action.isShiftClicked()) {
                    // Right-click: show the materials needed for one craft
                    p.sendMessage("§aRecipe: §b" + getDisplayName(resultItem));
                    p.sendMessage("§aRequired Materials:");
                    for (Map.Entry<String, Integer> ingredient : recipe.entrySet()) {
                        String itemKey = ingredient.getKey(); // ingredient key (such as "mc:COBBLESTONE" or "sf:some_item_id")
                        int requiredAmount = ingredient.getValue(); // required amount

                        // Get the ingredient's display name
                        String displayName = getIngredientDisplayName(itemKey);

                        // Send a message
                        p.sendMessage(" - §e" + displayName + " x " + requiredAmount);
                    }
                } else if (!action.isRightClicked() && !action.isShiftClicked()) {
                    // Left-click: use the materials and give one result
                    Map<String, Integer> currentPlayerItems = getPlayerInventoryItems(p);
                    if (!hasEnoughMaterials(currentPlayerItems, recipe)) {
                        p.sendMessage("§cNot enough materials to craft this!");
                        return false;
                    }

                    // Use the materials
                    if (!consumeMaterials(p, recipe, 1)) {
                        // If there aren't enough materials, show a message and stop
                        p.sendMessage("§cItem data is wrong. Please use the right materials!");
                        return false; // or throw an exception or stop the operation if needed
                    }

                    // Give the output item
                    resultItem.setAmount(outputAmountPerCraft); // Set the output amount

                    giveOrDropItem(p, resultItem); // Give the item to the player

                    // Show a message
                    String productName = getDisplayName(resultItem);
                    p.sendMessage("§aYou crafted §b" + outputAmountPerCraft + " §a item(s)§b" + productName + "§a。");

                    // Refresh the menu
                    addAvailableRecipesToMenu(p, menu, 0, ReceivedRecipes, recipeType);
                } else if (!action.isRightClicked() && action.isShiftClicked()) {
                    // Shift + left-click: craft in bulk
                    Map<String, Integer> currentPlayerItems = getPlayerInventoryItems(p);
                    int maxCraftableAmount = calculateMaxCraftableAmount(currentPlayerItems, recipe);
                    if (maxCraftableAmount > 52000){
                        maxCraftableAmount = 52000;
                    }
                    if (maxCraftableAmount <= 0) {
                        p.sendMessage("§cNot enough materials to craft this!");
                        return false;
                    }

                    // Use the materials
                    if(!consumeMaterials(p, recipe, maxCraftableAmount)){
                        // If there aren't enough materials, show a message and stop
                        p.sendMessage("§cItem data is wrong. Please use the right materials!");
                        return false; // or throw an exception or stop the operation if needed
                    }

                    // Give the output item
                    int totalOutputAmount = maxCraftableAmount * outputAmountPerCraft; // Calculate the total output amount
                    resultItem.setAmount(totalOutputAmount); // Set the output amount

                    giveOrDropItem(p, resultItem); // Give the item to the player

                    // Show a message
                    String productName = getDisplayName(resultItem);
                    p.sendMessage("§aYou crafted §b" + totalOutputAmount + " §a item(s)§b" + productName + "§a。");

                    // Refresh the menu
                    addAvailableRecipesToMenu(p, menu, 0, ReceivedRecipes, recipeType);
                } else if (action.isRightClicked() && action.isShiftClicked()) {
                    // Shift + right-click: craft exactly 32 batches
                    Map<String, Integer> currentPlayerItems = getPlayerInventoryItems(p);
                    int requiredCrafts = 32;

                    // Check whether 32 batches can be crafted
                    if (!hasEnoughMaterials(currentPlayerItems, recipe, requiredCrafts)) {
                        p.sendMessage("§cNot enough materials to craft 32 batches!");
                        return false;
                    }

                    // Use the materials
                    if(!consumeMaterials(p, recipe, requiredCrafts)){
                        // If there aren't enough materials, show a message and stop
                        p.sendMessage("§cItem data is wrong. Please use the right materials!");
                        return false; // or throw an exception or stop the operation if needed
                    }

                    // Give the output item
                    int totalOutputAmount = requiredCrafts * outputAmountPerCraft; // Calculate the total output amount
                    resultItem.setAmount(totalOutputAmount); // Set the output amount

                    giveOrDropItem(p, resultItem); // Give the item to the player

                    // Show a message
                    String productName = getDisplayName(resultItem);
                    p.sendMessage("§aYou crafted §b" + totalOutputAmount + " §a item(s)§b" + productName + "§a。");

                    // Refresh the menu
                    addAvailableRecipesToMenu(p, menu, 0, ReceivedRecipes, recipeType);
                }
                return false; // Don't consume items
            });
        }

        // Add page buttons
        if (page > 0) {
            // Previous-page button
            final int previousPage = page - 1; // Store the previous page number in a local variable
            menu.addItem(45, CreateItem.createItem(Material.ARROW, "§aPrevious Page"), (p, s, item, action) -> {
                addAvailableRecipesToMenu(p, menu, previousPage,ReceivedRecipes, recipeType); // Go to the previous page
                return false;
            });
        }

        if (page < totalPages - 1) {
            // Next-page button
            final int nextPage = page + 1; // Store the next page number in a local variable
            menu.addItem(53, CreateItem.createItem(Material.ARROW, "§aNext Page"), (p, s, item, action) -> {
                addAvailableRecipesToMenu(p, menu, nextPage,ReceivedRecipes, recipeType); // Go to the next page
                return false;
            });
        }

        // Show a message if no matching recipes are found
        if (availableRecipes.isEmpty()) {
            menu.addItem(0, CreateItem.createItem(Material.BARRIER, "§cNo recipes available"), (p, s, item, action) -> {
                p.sendMessage("§cYou don't have enough materials in your inventory!");
                return false;
            });
        }
    }


}

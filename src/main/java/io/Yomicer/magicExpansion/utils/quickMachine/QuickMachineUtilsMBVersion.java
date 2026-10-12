package io.Yomicer.magicExpansion.utils.quickMachine;

import io.Yomicer.magicExpansion.utils.CreateItem;
import io.Yomicer.magicExpansion.utils.MagicExpansionSlimefunItemCache;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static io.Yomicer.magicExpansion.Listener.SlimefunRegistryFinalized.getUniqueItemKey;
import static io.Yomicer.magicExpansion.utils.GiveItem.giveOrDropItem;
import static io.Yomicer.magicExpansion.utils.Utils.doGlow;
import static io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils.isItemSimilar;

public class QuickMachineUtilsMBVersion {
    private static final int[] outputSlots = {35,34,33,32,31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1};


    /**
     * Static method: put an item into the given slots and return any leftover items.
     *
     * @param item  item to put
     * @param slots target slots
     * @param menu  current menu
     * @return items left over (or null if everything fits)
     */
    public static ItemStack pushItemRe(ItemStack item, int[] slots, DirtyChestMenu menu) {
        if (item == null || item.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot push null or AIR");
        }

        // Get the item's max stack size
        int maxStackSize = Math.min(item.getMaxStackSize(), menu.toInventory().getMaxStackSize());
        int totalAmount = item.getAmount(); // total amount to place

        ItemStackWrapper wrapper = null;

        for (int slot : slots) {
            if (totalAmount <= 0) {
                break; // Stop the loop when all items have been placed
            }

            ItemStack stack = menu.getItemInSlot(slot);

            if (stack == null) {
                // If the slot is empty, put the item there
                int amountToPlace = Math.min(totalAmount, maxStackSize);
                ItemStack newItem = item.clone();
                newItem.setAmount(amountToPlace);
                menu.replaceExistingItem(slot, newItem);
                totalAmount -= amountToPlace;
            } else {
                // Check whether the items can stack
                if (wrapper == null) {
                    wrapper = ItemStackWrapper.wrap(item);
                }

                if (SlimefunItem.getByItem(item) != null) {
                    if (!SlimefunUtils.isItemSimilar(stack, wrapper, true, false)) {
                        continue; // Item type doesn't match, so skip this slot
                    }
                } else if (!ItemUtils.canStack(wrapper, stack)) {
                    continue; // Items can't stack, so skip this slot
                }

                // Check how much space is left in the slot
                int spaceLeft = stack.getMaxStackSize() - stack.getAmount();
                if (spaceLeft > 0) {
                    int toAdd = Math.min(spaceLeft, totalAmount);
                    stack.setAmount(stack.getAmount() + toAdd);
                    totalAmount -= toAdd;

                    // Update the slot
                    menu.replaceExistingItem(slot, stack);
                }
            }
        }

        // Return any leftover items
        return totalAmount > 0 ? new CustomItemStack(item, totalAmount) : null;
    }

    /**
     * D1: Use consistent return values: true means success and false means failure (reversed from the old behavior)
     * D2: Make material use safe: snapshot input slots (0–35), check all materials, then remove them one by one,
     * if something fails midway, restore the snapshot so no materials are lost
     */
    private static boolean consumeMaterials(Player player, Map<String, Integer> recipe, int amount,BlockMenu menu) {
        // D2: Copy the items in input slots 0–35 for rollback if something fails
        ItemStack[] snapshotCopy = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            ItemStack it = menu.getItemInSlot(i);
            snapshotCopy[i] = it == null ? null : it.clone();
        }

        // D2: Step 1: Check all materials in the machine first. If there aren't enough, stop without changing the machine
        if (!hasEnoughMaterials(getInventoryItems(menu), recipe, amount)) {
            player.sendMessage("§cNot enough materials in the machine. Please check it!");
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
                    if (!removeVanillaItemsFromMenu(menu, targetItem, totalRequiredAmount)) {
                        player.sendMessage("§cCouldn't remove enough Minecraft items. Please check your machine!");
                        // D2: If something fails midway, restore the snapshot to return removed materials
                        restoreMenuSnapshot(menu, snapshotCopy);
                        // D1: Reverse the return value——failure=false
                        return false;
                    }
                }
            } else if (requiredKey.startsWith("sf:")) {
                // Slimefun item
                String slimefunId = requiredKey.substring(3);
                SlimefunItem slimefunItem = SlimefunItem.getById(slimefunId);
                if (slimefunItem != null) {
                    ItemStack targetItem = slimefunItem.getItem().clone();
                    if (!removeSlimefunItemsFromMenu(menu, targetItem, totalRequiredAmount)) {
                        player.sendMessage("§cCouldn't remove enough Slimefun items. Please check your machine!");
                        // D2: If something fails midway, restore the snapshot to return removed materials
                        restoreMenuSnapshot(menu, snapshotCopy);
                        // D1: Reverse the return value——failure=false
                        return false;
                    }
                }
            }
        }

        // All materials were removed successfully
        // D1: Reverse the return value——success=true
        return true;
    }

    /**
     * D2: Restore the items in machine input slots (0–35) from the snapshot
     */
    private static void restoreMenuSnapshot(BlockMenu menu, ItemStack[] snapshotCopy) {
        for (int i = 0; i < snapshotCopy.length; i++) {
            // Restore from a cloned snapshot so later changes don't affect the rollback copy
            menu.replaceExistingItem(i, snapshotCopy[i] == null ? null : snapshotCopy[i].clone());
        }
    }

    /**
     * Remove a set amount of Minecraft items from the machine inventory
     *
     * @param menu machine inventory
     * @param targetItem item to remove
     * @param amount     amount to remove
     * @return whether the requested amount was removed
     */
    private static boolean removeVanillaItemsFromMenu(BlockMenu menu, ItemStack targetItem, int amount) {
        int remainingAmount = amount; // amount still to remove

        // D5: Only check input slots 0–35. Do not remove items from decoration or page buttons in slots 36–53
        for (int slot = 0; slot <= 35 && remainingAmount > 0; slot++) {
            ItemStack itemInSlot = menu.getItemInSlot(slot); // Get the item in this slot

            // Check whether the slot has a matching item
            if (itemInSlot != null && itemInSlot.isSimilar(targetItem)) {
                int removed = Math.min(itemInSlot.getAmount(), remainingAmount); // Calculate how many items can be removed

                // Update the item amount in the slot
                if (removed >= itemInSlot.getAmount()) {
                    menu.replaceExistingItem(slot, null); // Clear the slot if all items are removed
                } else {
                    itemInSlot.setAmount(itemInSlot.getAmount() - removed); // Reduce the item amount
                    menu.replaceExistingItem(slot, itemInSlot); // Update the slot
                }

                remainingAmount -= removed; // Update the amount still to remove
            }
        }

        return remainingAmount == 0; // If the remaining amount is 0, all items were removed
    }

    /**
     * Remove a set amount of Slimefun items from the machine inventory
     *
     * @param menu machine inventory
     * @param targetItem item to remove
     * @param amount     amount to remove
     * @return whether the requested amount was removed
     */
    private static boolean removeSlimefunItemsFromMenu(BlockMenu menu, ItemStack targetItem, int amount) {
        int remainingAmount = amount; // amount still to remove

        // D5: Only check input slots 0–35. Do not remove items from decoration or page buttons in slots 36–53
        for (int slot = 0; slot <= 35 && remainingAmount > 0; slot++) {
            ItemStack itemInSlot = menu.getItemInSlot(slot); // Get the item in this slot

            // Check whether the slot has a matching Slimefun item
            if (itemInSlot != null && isItemSimilar(itemInSlot, targetItem, false)) {
                int removed = Math.min(itemInSlot.getAmount(), remainingAmount); // Calculate how many items can be removed

                // Update the item amount in the slot
                if (removed >= itemInSlot.getAmount()) {
                    menu.replaceExistingItem(slot, null); // Clear the slot if all items are removed
                } else {
                    itemInSlot.setAmount(itemInSlot.getAmount() - removed); // Reduce the item amount
                    menu.replaceExistingItem(slot, itemInSlot); // Update the slot
                }

                remainingAmount -= removed; // Update the amount still to remove
            }
        }

        return remainingAmount == 0; // If the remaining amount is 0, all items were removed
    }



    /**
     * Combine matching items in the recipe
     * Convert the recipe array to a Map
     *
     * @param recipe recipe array
     * @return recipe Map
     */
    private static Map<String, Integer> convertRecipeToMap(ItemStack[] recipe) {
        Map<String, Integer> recipeMap = new LinkedHashMap<>();

        // Process the input items
        for (ItemStack input : recipe) {
            if (input == null || input.getType() == Material.AIR) {
                continue; // Skip empty slots
            }

            String itemKey = getUniqueItemKey(input); // Get the unique key
            if (itemKey != null) {
                recipeMap.put(itemKey, recipeMap.getOrDefault(itemKey, 0) + input.getAmount()); // Keep the amount positive
            }
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
    private static String getDisplayName(ItemStack item) {
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
    private static String getIngredientDisplayName(String itemKey) {
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
    private static int calculateMaxCraftableAmount(Map<String, Integer> playerItems, Map<String, Integer> recipe) {
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
     * Get all items and their amounts in the machine inventory
     * and assign each item a key
     *
     * @param menu menu
     * @return a Map where the key is the item type or SlimefunItem ID and the value is its amount
     */
    private static Map<String, Integer> getInventoryItems(BlockMenu menu) {
        Map<String, Integer> itemCounts = new HashMap<>();

        // Loop through slots 1 to 35
        for (int slot = 0; slot <= 35; slot++) {
            ItemStack item = menu.getItemInSlot(slot); // Get the item in the slot

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

                // Add up item amounts in the menu
                itemCounts.put(itemKey, itemCounts.getOrDefault(itemKey, 0) + item.getAmount());
            }
        }

        return itemCounts;
    }



    /**
     * Check whether the player has enough items for the recipe
     *
     * @param playerItems all items and their amounts in the machine inventory
     * @param recipe      Recipe
     * @param amount      craft amount
     * @return Return true if the player has enough items, or false if not
     */
    private static boolean hasEnoughMaterials(Map<String, Integer> playerItems, Map<String, Integer> recipe, int amount) {
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
    private static boolean hasEnoughMaterials(Map<String, Integer> playerItems, Map<String, Integer> recipe) {
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


    // Store the current page for each BlockMenu
    // D3: Use WeakHashMap so closed or unloaded BlockMenus can be garbage-collected, preventing memory leaks
    private static final Map<BlockMenu, Integer> currentPageMap = new WeakHashMap<>();
    /**
     * Add matching recipes to the menu and support pages
     * Refresh logic
     *
     * @param menu   menu
     */
    public static void addAvailableRecipesToMenu(BlockMenu menu, List<Map<String, Integer>> receivedRecipes, RecipeType recipeType) {
        // Get the current page (default to the first page)
        int currentPage = currentPageMap.getOrDefault(menu, 0);

        // Get all items and their amounts in the machine inventory
        Map<String, Integer> chestItems = getInventoryItems(menu);

        // Clear the menu
        for (int i = 36; i < 45; i++) {
            menu.addItem(i, new CustomItemStack(doGlow(Material.BARRIER), "&a"), (player, slot, item, action) -> false);
        }
        menu.addItem(45, new CustomItemStack(doGlow(Material.BARRIER), "&a"), (player, slot, item, action) -> false);
        menu.addItem(53, new CustomItemStack(doGlow(Material.BARRIER), "&a"), (player, slot, item, action) -> false);

        // Add the help button
        menu.addItem(49, new CustomItemStack(doGlow(Material.NETHER_STAR), "§x§F§D§B§7§D§4How to Use",
                        "§x§F§D§B§7§D§4§lClick to refresh the output list",
                        "§eright-click §bto see the materials needed for one craft",
                        "§eleft-click §bto craft once",
                        "§bHold §eShift right-click §bto craft 32 items at once",
                        "§bHold §eShift left-click §bto craft as many as you can",
                        "§bNote: §eRows 1–4 §bare used for both input and output"),
                (player1, slot, item, action) -> {
                    addAvailableRecipesToMenu(menu, receivedRecipes, recipeType);
                    return false;
                });

        // Filter recipes the player can craft
        List<Map<String, Integer>> availableRecipes = receivedRecipes.stream()
                .filter(recipe -> hasEnoughMaterials(chestItems, recipe))
                .toList();

        // Page handling
        int itemsPerPage = 9; // maximum items shown per page
        int totalPages = (int) Math.ceil((double) availableRecipes.size() / itemsPerPage);

        // Check whether the current page is past the last page
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1); // If it is, reset to the last page
            currentPageMap.put(menu, currentPage); // Update the current page
        }

        // Keep the current page in range
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));

        // Recipes on the current page
        List<Map<String, Integer>> currentPageRecipes = availableRecipes.stream()
                .skip((long) currentPage * itemsPerPage)
                .limit(itemsPerPage)
                .toList();

        // Add recipes to the menu
        int slot = 36;
        for (Map<String, Integer> recipe : currentPageRecipes) {
            Pair<ItemStack, Integer> recipeResult = getRecipeResultWithAmount(recipe,recipeType);
            if (recipeResult == null || recipeResult.getFirstValue() == null) {
                continue;
            }

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

            // Add the output to the menu
            menu.addItem(slot++, resultItemDisplay, (p, s, item, action) -> {
                if (action.isRightClicked() && !action.isShiftClicked()) {
                    // Right-click: show the materials needed for one craft
                    p.sendMessage("§aRecipe: §b" + getDisplayName(resultItem));
                    p.sendMessage("§aRequired Materials:");
                    for (Map.Entry<String, Integer> ingredient : recipe.entrySet()) {
                        String displayName = getIngredientDisplayName(ingredient.getKey());
                        int requiredAmount = ingredient.getValue();
                        p.sendMessage(" - §e" + displayName + " x " + requiredAmount);
                    }
                } else if (!action.isRightClicked() && !action.isShiftClicked()) {
                    // Left-click: use the materials and give one result
                    // D1: consumeMaterials now returns true on success, so invert the check
                    if (!consumeMaterials(p, recipe, 1, menu)) {
                        p.sendMessage("§cNot enough materials or the item data is wrong!");
                        return false;
                    }

                    // Clone the output item
                    ItemStack resultItemClone = resultItem.clone();

                    // Call pushItem to try to put the item into the slots
                    ItemStack remainingItems = pushItemRe(resultItemClone, outputSlots,menu);

                    // Check whether any items are left over
                    if (remainingItems != null && remainingItems.getAmount() > 0) {
                        // If the slots are full, give the leftover items to the player or drop them
                        giveOrDropItem(p, new CustomItemStack(remainingItems, remainingItems.getAmount()));
                    }
                    p.sendMessage("§aCrafted §b" + outputAmountPerCraft + " §a item(s)§b" + getDisplayName(resultItem) + "§a。");
                    addAvailableRecipesToMenu(menu, receivedRecipes, recipeType); // Refresh the menu
                } else if (!action.isRightClicked() && action.isShiftClicked()) {
                    // Shift + left-click: craft in bulk
                    int maxCraftable = calculateMaxCraftableAmount(chestItems, recipe);
                    if (maxCraftable > 52000){
                        maxCraftable = 52000;
                    }
                    if (maxCraftable <= 0) {
                        p.sendMessage("§cNot enough materials to craft in bulk!");
                        return false;
                    }

                    // D1: consumeMaterials now returns true on success, so invert the check
                    if (!consumeMaterials(p, recipe, maxCraftable,menu)) {
                        p.sendMessage("§cNot enough materials or the item data is wrong!");
                        return false;
                    }

                    // D4: Use long to prevent int overflow when multiplying maxCraftable by outputAmountPerCraft,
                    // and cap the result at 0–2304, a reasonable upper limit
                    long totalOutputLong = Math.max(0L, Math.min(2304L, (long) maxCraftable * outputAmountPerCraft));
                    int totalOutput = (int) totalOutputLong;

                    // Clone the output item and set its amount to the total output
                    ItemStack resultItemClone = resultItem.clone();
                    resultItemClone.setAmount(totalOutput);

                    // Call pushItem to try to put the item into the slots
                    ItemStack remainingItems = pushItemRe(resultItemClone, outputSlots,menu);;

                    // Check whether any items are left over
                    if (remainingItems != null && remainingItems.getAmount() > 0) {
                        // If the slots are full, give the leftover items to the player or drop them
                        giveOrDropItem(p, remainingItems);
                    }
                    p.sendMessage("§aCrafted §b" + totalOutput + " §a item(s)§b" + getDisplayName(resultItem) + "§a。");
                    addAvailableRecipesToMenu(menu, receivedRecipes, recipeType); // Refresh the menu
                } else if (action.isRightClicked() && action.isShiftClicked()) {
                    // Shift + right-click: craft exactly 32 batches
                    int requiredCrafts = 32;
                    if (!hasEnoughMaterials(chestItems, recipe, requiredCrafts)) {
                        p.sendMessage("§cNot enough materials to craft 32 batches!");
                        return false;
                    }

                    // D1: consumeMaterials now returns true on success, so invert the check
                    if (!consumeMaterials(p, recipe, requiredCrafts,menu)) {
                        p.sendMessage("§cNot enough materials or the item data is wrong!");
                        return false;
                    }

                    // D4: Use long and cap the result at 0–2304 to avoid int overflow or oversized amounts
                    long totalOutputLong = Math.max(0L, Math.min(2304L, (long) requiredCrafts * outputAmountPerCraft));
                    int totalOutput = (int) totalOutputLong;

                    // Clone the output item and set its amount to the total output
                    ItemStack resultItemClone = resultItem.clone();
                    resultItemClone.setAmount(totalOutput);

                    // Call pushItem to try to put the item into the slots
                    ItemStack remainingItems = pushItemRe(resultItemClone, outputSlots,menu);;

                    // Check whether any items are left over
                    if (remainingItems != null && remainingItems.getAmount() > 0) {
                        // If the slots are full, give the leftover items to the player or drop them
                        giveOrDropItem(p, remainingItems);
                    }
                    p.sendMessage("§aCrafted §b" + totalOutput + " §a item(s)§b" + getDisplayName(resultItem) + "§a。");
                    addAvailableRecipesToMenu(menu, receivedRecipes, recipeType); // Refresh the menu
                }
                return false;
            });
        }

        // Add page buttons
        if (currentPage > 0) {
            // Previous-page button
            final int previousPage = currentPage - 1; // Store the previous page number in a local variable
            menu.addItem(45, CreateItem.createItem(Material.ARROW, "§aPrevious Page"), (p, s, item, action) -> {
                currentPageMap.put(menu, previousPage); // Update the current page
                addAvailableRecipesToMenu(menu, receivedRecipes, recipeType);
                return false;
            });
        }

        if (currentPage < totalPages - 1) {
            // Next-page button
            final int nextPage = currentPage + 1; // Store the next page number in a local variable
            menu.addItem(53, CreateItem.createItem(Material.ARROW, "§aNext Page"), (p, s, item, action) -> {
                currentPageMap.put(menu, nextPage); // Update the current page
                addAvailableRecipesToMenu(menu, receivedRecipes, recipeType);
                return false;
            });
        }

        // Show a message if no matching recipes are found
        if (availableRecipes.isEmpty()) {
            menu.addItem(36, CreateItem.createItem(Material.BARRIER, "§cNo recipes available"), (p, s, item, action) -> {
                p.sendMessage("§cThere aren't enough materials in the machine!");
                return false;
            });
        }

        // Save the current page in the Map
        currentPageMap.put(menu, currentPage);
    }




}

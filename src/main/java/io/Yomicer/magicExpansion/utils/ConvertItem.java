package io.Yomicer.magicExpansion.utils;

import io.Yomicer.magicExpansion.core.MagicExpansionItems;
import io.Yomicer.magicExpansion.utils.log.Debug;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ConvertItem {
    public static ItemStack BasicCreateItem(String selectedItem) {
        // Get the Slimefun item by ID
        SlimefunItem slimefunItem = SlimefunItem.getById(selectedItem);

        // Check whether the Slimefun item was found successfully
        if (slimefunItem == null) {
            Debug.logInfo("Magic 1.0 is not installed. Replaced with an equivalent material. This item’s Magic 1.0 ID is: "+ selectedItem);
            return MagicExpansionItems.MAGIC_EXPANSION_TO_MAGIC_ITEM_BASIC; // Return a basic item
        }

        // Get the item and create an ItemStack
        ItemStack itemStack = new ItemStack(slimefunItem.getItem());

        return itemStack;
    }

    public static SlimefunItemStack createItem(String selectedItem) {
        // Get the Slimefun item by ID
        SlimefunItem slimefunItem = SlimefunItem.getById(selectedItem);

        if (slimefunItem == null) {
            Debug.logInfo("Could not find an item with ID " + selectedItem + "!");
            return MagicExpansionItems.MAGIC_EXPANSION_TO_MAGIC_ITEM_BASIC; // Return the default item
        }

        // Return the correct SlimefunItemStack
        return new SlimefunItemStack(
                selectedItem, // Unique ID
                slimefunItem.getItem().clone()            // Clone the original item
        );
    }


    public static ItemStack AdvancedCreateItem(String selectedItem) {
        // Get the Slimefun item by ID
        SlimefunItem slimefunItem = SlimefunItem.getById(selectedItem);

        // Check whether the Slimefun item was found successfully
        if (slimefunItem == null) {
            Debug.logInfo("Magic 1.0 is not installed. Replaced with an equivalent material. This item’s Magic 1.0 ID is: "+ selectedItem);
            return MagicExpansionItems.MAGIC_EXPANSION_TO_MAGIC_ITEM_ADVANCED; // Return an advanced item
        }

        // Get the item and create an ItemStack
        ItemStack itemStack = new ItemStack(slimefunItem.getItem());

        return itemStack;
    }

    public static ItemStack stoneCreateItem(String selectedItem) {
        // Get the Slimefun item by ID
        SlimefunItem slimefunItem = SlimefunItem.getById(selectedItem);

        // Check whether the Slimefun item was found successfully
        if (slimefunItem == null) {
            Debug.logInfo("Could not find an item with ID " + selectedItem + "!");
            Debug.logInfo("Replaced with stone.");
            return new ItemStack(Material.STONE); // Return an advanced item
        }

        // Get the item and create an ItemStack
        ItemStack itemStack = new ItemStack(slimefunItem.getItem());

        return itemStack;
    }

    public static Boolean IfItemXist(String i) {
        // Get the Slimefun item by ID
        SlimefunItem slimefunItem = SlimefunItem.getById(i);
        // Check whether the Slimefun item was found successfully
        if (slimefunItem == null) {
            Debug.logInfo("Could not find an item with ID " + i + "!,skip registration");
            return false;
        }
        return true;
    }






}

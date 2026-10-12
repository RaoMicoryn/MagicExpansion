package io.Yomicer.magicExpansion.items.misc.magicAlter;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MagicAltarRecipe {
    private final ItemStack[][] dispenserItems;
    private final ItemStack result;
    private final Material[][] baseLayout;


    public MagicAltarRecipe(ItemStack[][] dispenserItems, ItemStack result) {
        this.dispenserItems = dispenserItems;
        this.result = result;
        this.baseLayout = createDefaultBaseLayout();
    }

    public MagicAltarRecipe(ItemStack[][] dispenserItems, ItemStack result, Material[][] baseLayout) {
        this.dispenserItems = dispenserItems;
        this.result = result;
        this.baseLayout = baseLayout;
    }

    private Material[][] createDefaultBaseLayout() {
        return new Material[][]{
                {Material.GLASS, Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN},
                {Material.OBSIDIAN, Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.OBSIDIAN},
                {Material.OBSIDIAN, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.GOLD_BLOCK, Material.OBSIDIAN},
                {Material.OBSIDIAN, Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.OBSIDIAN},
                {Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN, Material.OBSIDIAN}
        };
    }

    public ItemStack[][] getDispenserItems() {
        return dispenserItems;
    }

    public ItemStack getResult() {
        return result;
    }

    public Material[][] getBaseLayout() {
        return baseLayout;
    }

    // Check if the recipe matches - positions must match exactly
    public boolean matches(ItemStack[][] inputItems) {
        if (inputItems.length != 9) return false;

        for (int dispenserIndex = 0; dispenserIndex < 9; dispenserIndex++) {
            ItemStack[] recipeSlots = dispenserItems[dispenserIndex];
            ItemStack[] inputSlots = inputItems[dispenserIndex];

            for (int slotIndex = 0; slotIndex < 9; slotIndex++) {
                ItemStack recipeItem = recipeSlots[slotIndex];
                ItemStack inputItem = inputSlots[slotIndex];

                if (recipeItem != null) {
                    // Check if the input item matches exactly
                    if (inputItem == null ||
                            inputItem.getType() != recipeItem.getType() ||
                            inputItem.getAmount() != recipeItem.getAmount()) {
                        return false;
                    }

                    // Check the NBT data (if there is any)
                    if (recipeItem.hasItemMeta() && !recipeItem.getItemMeta().equals(inputItem.getItemMeta())) {
                        return false;
                    }
                } else {
                    // This spot in the recipe is empty, but there's an item in the input
                    if (inputItem != null) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    // Get the dispenser display item
    public ItemStack getDisplayItem(int dispenserIndex) {
        if (dispenserIndex < 0 || dispenserIndex >= 9) return null;

        boolean hasItems = false;
        for (int i = 0; i < 9; i++) {
            if (dispenserItems[dispenserIndex][i] != null) {
                hasItems = true;
                break;
            }
        }

        if (!hasItems) return null;

        ItemStack dispenser = new ItemStack(Material.DISPENSER);
        ItemMeta meta = dispenser.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§bDispenser #" + (dispenserIndex + 1));

            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7Click to see the details");

            int itemCount = 0;
            for (int i = 0; i < 9; i++) {
                if (dispenserItems[dispenserIndex][i] != null) {
                    itemCount++;
                }
            }
            lore.add("§7Item count: " + itemCount + "/9");

            meta.setLore(lore);
            dispenser.setItemMeta(meta);
        }

        return dispenser;
    }

    // Get the item in a specific dispenser slot
    public ItemStack getDispenserSlotItem(int dispenserIndex, int slotIndex) {
        if (dispenserIndex < 0 || dispenserIndex >= 9 || slotIndex < 0 || slotIndex >= 9) {
            return null;
        }
        return dispenserItems[dispenserIndex][slotIndex];
    }

    // Check if the dispenser is empty
    public boolean isDispenserEmpty(int dispenserIndex) {
        for (int i = 0; i < 9; i++) {
            if (dispenserItems[dispenserIndex][i] != null) {
                return false;
            }
        }
        return true;
    }

    // Get the number of items in the dispenser
    public int getDispenserItemCount(int dispenserIndex) {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            if (dispenserItems[dispenserIndex][i] != null) {
                count++;
            }
        }
        return count;
    }
}
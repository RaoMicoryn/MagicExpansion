package io.Yomicer.magicExpansion.items.misc.fish.fishInterface;

import org.bukkit.inventory.ItemStack;

public class FishManager {

    /**
     * Quickly get the fish ItemStack
     */
    public static ItemStack getFishItem(BaseFish fish) {
        return fish.copy().build();
    }

    /**
     * Get a fish with custom lore
     */
    public static ItemStack getFishItemWithLore(BaseFish fish, String... loreLines) {
        return fish.copy().addLore(loreLines).build();
    }

    /**
     * Get a fish for the guide (guide info is added automatically)
     */
    public static ItemStack getFishItemForGuide(BaseFish fish) {
        return fish.addLore("", "§7This is a guide entry for display only", "§8—— Display only, shows how to get it ——").build();
    }

}
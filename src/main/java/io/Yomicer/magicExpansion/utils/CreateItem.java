package io.Yomicer.magicExpansion.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class CreateItem {
    public static ItemStack createItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);

        //To set a display name or other metadata, you can use ItemMeta: ItemMeta：
        var meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);

        return item;
    }

    public static ItemStack createItem(Material material) {
        return createItem(material, null); // Call the original method without a name
    }

    public static ItemStack createItemWithLore(Material material, String displayName, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null) {
                meta.setDisplayName(displayName);
            }
            if (lore != null && lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

}

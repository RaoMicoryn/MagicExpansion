package io.Yomicer.magicExpansion.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class Utils {

    //Add a glow effect
    public static ItemStack doGlow(Object item) {
        // If the input is a Material, create a new ItemStack
        ItemStack result = (item instanceof Material)
                ? new ItemStack((Material) item)
                : (ItemStack) item;
        // Get the Luck of the Sea enchantment dynamically
        Enchantment protectionEnchant = Enchantment.getByKey(NamespacedKey.minecraft("luck_of_the_sea"));
        // Add an unsafe enchantment (for example, Protection at level 1)
        result.addUnsafeEnchantment(protectionEnchant, 1);

        // Get the item metadata and hide enchantment details
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            result.setItemMeta(meta);
        }

        return result;
    }

    public static ItemStack doGlowDisplayEnchant(Object item) {
        // If the input is a Material, create a new ItemStack
        ItemStack result = (item instanceof Material)
                ? new ItemStack((Material) item)
                : (ItemStack) item;
        // Get the Luck of the Sea enchantment dynamically
        Enchantment protectionEnchant = Enchantment.getByKey(NamespacedKey.minecraft("luck_of_the_sea"));
        // Add an unsafe enchantment (for example, Protection at level 1)
        result.addUnsafeEnchantment(protectionEnchant, 1);

        // Get the item metadata and hide enchantment details
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            result.setItemMeta(meta);
        }

        return result;
    }


}

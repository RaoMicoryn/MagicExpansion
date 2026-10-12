package io.Yomicer.magicExpansion.utils;

import com.google.gson.*;
import io.Yomicer.magicExpansion.utils.log.Debug;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class SameItemJudge {

    /**
     * ✅ Serialize the full ItemStack into a Base64 string
     * Supports all data, including PDC, enchantments, lore, custom models, Slimefun items, and NBT
     */
    @Nullable
    public static String itemToBase64(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            // Make sure the item amount is saved correctly first
            dataOutput.writeObject(item.clone()); // Use a clone to avoid changes from outside
            dataOutput.flush();

            byte[] serializedBytes = outputStream.toByteArray();
            return Base64Coder.encodeLines(serializedBytes);

        } catch (IOException e) {
            Debug.logWarn("Failed to serialize item: " + item.getType());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ✅ Deserialize a Base64 string into an ItemStack
     * Restore the original item fully, including PDC, enchantments, amount, and NBT
     */
    @Nullable
    public static ItemStack itemFromBase64(String data) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            ItemStack item = (ItemStack) dataInput.readObject();
            // Important: refresh the ItemMeta reference to avoid PDC cache issues
            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                item.setItemMeta(meta); // Set it again to keep it consistent
            }
            return item;

        } catch (IOException | ClassNotFoundException e) {
            Debug.logWarn("Failed to deserialize item");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ✅ Safely check whether two ItemStacks are the same (for stack checks)
     * Use Bukkit's built-in isSimilar method, with support for Slimefun items
     */
    public static boolean isSimilarSafe(ItemStack item1, ItemStack item2) {
        if (item1 == item2) return true;
        if (item1 == null || item2 == null) return false;

        // Check whether the item is air first
        if (item1.getType() == Material.AIR || item2.getType() == Material.AIR) {
            return item1.getType() == item2.getType();
        }

        // For non-Slimefun items, use Bukkit's isSimilar (ignores amount)
        return item1.isSimilar(item2);
    }

    /**
     * ✅ Check whether the items represented by two Base64 strings are the same
     */
    public static boolean isSameBase64Item(String data1, String data2) {
        if (data1 == null && data2 == null) return true;
        if (data1 == null || data2 == null) return false;

        ItemStack item1 = itemFromBase64(data1);
        ItemStack item2 = itemFromBase64(data2);

        return isSimilarSafe(item1, item2);
    }
}
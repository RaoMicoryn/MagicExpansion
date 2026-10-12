package io.Yomicer.magicExpansion.items.tools;

import io.Yomicer.magicExpansion.items.enchantMachine.EnchantingTable;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnchantmentEraser extends SimpleSlimefunItem<ItemUseHandler> {

    public EnchantmentEraser(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
    }

    @Nonnull
    @Override
    public ItemUseHandler getItemHandler() {
        return e -> {
            e.cancel();
            Player player = e.getPlayer();

            // Check which hand triggered the event
            if (e.getHand() != EquipmentSlot.HAND) {
                // Ignore offhand events (the eraser should be in the main hand)
                return;
            }

            // Get the main-hand item (should be the eraser)
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();

            // Get the offhand item (the item to clean)
            ItemStack offhandItem = player.getInventory().getItemInOffHand();

            // Check whether the offhand has an item and it's not air
            if (offhandItem == null || offhandItem.getType() == Material.AIR) {
                player.sendMessage(ColorGradient.getGradientNameVer2("Put the item you want to remove enchantment attributes from in your offhand!"));
                return;
            }

            // Check whether the offhand item has enchantment attributes
            if (!hasEnchantmentAttributesExact(offhandItem)) {
                player.sendMessage(ColorGradient.getGradientNameVer2("The offhand item has no magic enchantment attributes!"));
                return;
            }

            // Remove enchantment attributes from the offhand item
            ItemStack result = removeEnchantmentAttributes(offhandItem.clone());

            if (result != null) {
                // Play sound effects
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.5f);
                player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);

                // Put the cleaned item back in the offhand
                player.getInventory().setItemInOffHand(result);

                // Use up one eraser from the main hand
                if (mainHandItem.getAmount() > 1) {
                    mainHandItem.setAmount(mainHandItem.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }

                player.sendMessage(ColorGradient.getGradientNameVer2("Removed the magic enchantment attributes from the offhand item!"));
            } else {
                player.sendMessage(ColorGradient.getGradientNameVer2("Couldn't remove them. This item may not have any magic enchantment attributes!"));
            }
        };
    }

    /**
     * Remove all magic enchantment attributes from an item
     */
    public static ItemStack removeEnchantmentAttributes(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Set<String> attributesToRemove = new HashSet<>();
        boolean hasRemovedAttributes = false;

        // Check for and remove all magic enchantment attributes
        for (String attribute : EnchantingTable.ATTRIBUTE_POOL.keySet()) {
            NamespacedKey key = new NamespacedKey("magicexpansion", attribute.toLowerCase().replace(".", "_"));

            if (pdc.has(key, PersistentDataType.INTEGER) || pdc.has(key, PersistentDataType.BOOLEAN)) {
                pdc.remove(key);
                attributesToRemove.add(attribute);
                hasRemovedAttributes = true;
            }
        }

        // Return the item if no attributes were removed
        if (!hasRemovedAttributes) {
            return item;
        }

        // Update the lore and remove the whole Magic Enchantments section
        removeEnchantmentLore(meta, attributesToRemove);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Remove the whole Magic Enchantments section from the lore
     */
    private static void removeEnchantmentLore(ItemMeta meta, Set<String> attributesToRemove) {
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) {
            return;
        }

        List<String> newLore = new ArrayList<>();
        boolean inEnchantmentSection = false;
        int enchantmentSectionStart = -1;
        int sectionCount = 0;

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String strippedLine = stripAllColorCodes(line);

            // Check whether this is the "Magic Enchantments:" heading
            if (strippedLine.contains("Magic Enchantments") && (strippedLine.contains("：") || strippedLine.contains(":"))) {
                // If we're already in an enchantment section, end the previous section first
                if (inEnchantmentSection) {
                    // Skip all lines in the previous section
//                    MagicExpansion.getInstance().getLogger().info("Ended Magic Enchantments section " + sectionCount + ", from line " + enchantmentSectionStart + " to " + (i-1));
                }

                // Start a new enchantment section
                inEnchantmentSection = true;
                enchantmentSectionStart = i;
                sectionCount++;
//                MagicExpansion.getInstance().getLogger().info("Found Magic Enchantments section " + sectionCount + ", heading: " + strippedLine);
                continue; // Skip the heading line
            }

            // If we're inside an enchantment section
            if (inEnchantmentSection) {
                // Check whether this is an attribute line (starts with "- " after removing colors)
                if (strippedLine.startsWith("- ")) {
//                    MagicExpansion.getInstance().getLogger().info("Skipped attribute line: " + strippedLine);
                    continue; // Skip the attribute line
                }

                // If this isn't an attribute line, end the current enchantment section
//                MagicExpansion.getInstance().getLogger().info("Ended Magic Enchantments section " + sectionCount + ", from line " + enchantmentSectionStart + " to " + (i-1));
                inEnchantmentSection = false;

                // This isn't an attribute line, so add it to the new lore
                newLore.add(line);
            } else {
                // If we're not in an enchantment section, add it as-is
                newLore.add(line);
            }
        }

        // End the enchantment section if we're still in one at the end
        if (inEnchantmentSection) {
//            MagicExpansion.getInstance().getLogger().info("Ended Magic Enchantments section " + sectionCount + " at the end");
        }

//        MagicExpansion.getInstance().getLogger().info("Original lore lines: " + lore.size() + ", new lore lines: " + newLore.size());

        // Set the lore to null if removing the whole enchantment section leaves it empty
        if (newLore.isEmpty()) {
            meta.setLore(null);
        } else {
            meta.setLore(newLore);
        }
    }

    /**
     * Remove all Minecraft color codes from a string, including hex color codes
     * Handle gradient color codes like §x§F§D§B§7§D§4
     */
    private static String stripAllColorCodes(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Handle hex color codes like §x§F§D§B§7§D§4 first
        // This format has 12 characters after §x (6 pairs of § plus a hex digit)
        String result = input;

        // Loop through and remove all color codes
        while (result.contains("§")) {
            // Find the first §
            int index = result.indexOf("§");
            if (index < 0 || index >= result.length() - 1) {
                break;
            }

            char nextChar = result.charAt(index + 1);

            // If it's §x (the start of a hex color code)
            if (nextChar == 'x' || nextChar == 'X') {
                // There should be 12 characters after §x (6 pairs of § plus a hex digit)
                // 14 characters total: §x + 6*(§+digit)
                if (index + 13 < result.length()) {
                    // Check whether the following characters use the right format
                    boolean validHexColor = true;
                    for (int i = 0; i < 6; i++) {
                        if (result.charAt(index + 2 + i*2) != '§' ||
                                !isHexDigit(result.charAt(index + 3 + i*2))) {
                            validHexColor = false;
                            break;
                        }
                    }

                    if (validHexColor) {
                        // Remove these 14 characters
                        result = result.substring(0, index) + result.substring(index + 14);
                        continue;
                    }
                }
            }

            // Handle regular color codes §0-§9, §a-§f, §k-§o, §r
            if (isColorCodeChar(nextChar)) {
                // Remove these two characters
                result = result.substring(0, index) + result.substring(index + 2);
            } else {
                // If it's not a color code, remove only the § character
                result = result.substring(0, index) + result.substring(index + 1);
            }
        }

        return result;
    }

    /**
     * Check whether a character is a valid hex digit (0-9, a-f, A-F)
     */
    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /**
     * Check whether a character is a color code character
     */
    private static boolean isColorCodeChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'k' && c <= 'o') ||
                (c >= 'A' && c <= 'F') || (c >= 'K' && c <= 'O') || c == 'r' || c == 'R';
    }

    /**
     * Check whether the item has magic enchantment attributes
     */
    public static boolean hasEnchantmentAttributesExact(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        // First, check whether the PDC has magic enchantment attributes
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (String attribute : EnchantingTable.ATTRIBUTE_POOL.keySet()) {
            NamespacedKey key = new NamespacedKey("magicexpansion", attribute.toLowerCase().replace(".", "_"));

            if (pdc.has(key, PersistentDataType.INTEGER) || pdc.has(key, PersistentDataType.BOOLEAN)) {
                return true;
            }
        }

        // If the PDC doesn't have them, check whether the lore contains "Magic Enchantments:"
        List<String> lore = meta.getLore();
        if (lore != null) {
            for (String line : lore) {
                String strippedLine = stripAllColorCodes(line);
                if (strippedLine.contains("Magic Enchantments") && (strippedLine.contains("：") || strippedLine.contains(":"))) {
                    return true;
                }
            }
        }

        return false;
    }
}
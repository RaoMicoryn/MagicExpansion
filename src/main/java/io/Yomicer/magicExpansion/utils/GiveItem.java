package io.Yomicer.magicExpansion.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

public class GiveItem {
    public static void giveOrDropItem(Player player, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        Location location = player.getLocation();
        ItemStack item1 = item.clone();

        // Try to put the item into the player inventory
        Map<Integer, ItemStack> remainingItems = inventory.addItem(item1);

        // Drop any remaining items on the ground if the inventory is full
        if (!remainingItems.isEmpty()) {
            for (ItemStack remaining : remainingItems.values()) {
                dropItemInBatches(player, location, remaining);
            }
        }
    }

    /**
     * Drop items in batches (maximum 127 items per operation)
     *
     * @param player   玩家
     * @param location Drop location
     * @param item     Item to drop
     */
    private static void dropItemInBatches(Player player, Location location, ItemStack item) {
        int totalAmount = item.getAmount();
        int maxDropSize = 64; // Maximum drop amount per operation

        while (totalAmount > 0) {
            // Create an item copy and set the amount for this batch
            ItemStack dropItem = item.clone();
            int currentBatchSize = Math.min(totalAmount, maxDropSize);
            dropItem.setAmount(currentBatchSize);

            // Drop the current batch of items
            player.getWorld().dropItemNaturally(location, dropItem);

            // Update the remaining amount
            totalAmount -= currentBatchSize;
        }
    }

}

package io.Yomicer.magicExpansion.utils.itemUtils;

import io.Yomicer.magicExpansion.utils.log.Debug;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSpawnReason;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static io.Yomicer.magicExpansion.Listener.SlimefunRegistryGiftBox.itemMapMihoyoHonkai;

public class getRandomItemFromGroup {



    // Get a random item's ItemStack
    public static ItemStack getRandomItemStack(Map<String, SlimefunItem> itemMap) {
        if (itemMap.isEmpty()) {
            return null;
        }

        Collection<SlimefunItem> values = itemMap.values();
        List<SlimefunItem> list = new ArrayList<>(values);

        SlimefunItem randomItem = list.get(new Random().nextInt(list.size()));
        return randomItem.getItem().clone();
    }

    // Spawn a random item at the given location
    public void spawnRandomItem(Location location, Player player) {
        ItemStack gift = getRandomItemStack(itemMapMihoyoHonkai);

        if (gift != null) {
            SlimefunUtils.spawnItem(location, gift, ItemSpawnReason.CHRISTMAS_PRESENT_OPENED, true, player);
            player.sendMessage("§aCongrats! You got " + gift.getItemMeta().getDisplayName() + "!");
        } else {
            Debug.logWarn("❌ Could not spawn the item: item is null");
        }
    }
}

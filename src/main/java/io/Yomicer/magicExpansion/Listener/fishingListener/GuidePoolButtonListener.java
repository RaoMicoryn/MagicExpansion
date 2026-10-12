package io.Yomicer.magicExpansion.Listener.fishingListener;

import io.Yomicer.magicExpansion.items.tools.FishingRodWaterCloud;
import io.Yomicer.magicExpansion.utils.WaterCloudPoolMenu;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Guide recipe page"Open Bait Pool"button setup and click handling
 * The button is created by  FishingRodWaterCloud.getDisplayRecipes()  and shown on the Slimefun guide recipe page,
 * This listener places the button in slot 27 when the guide opens and opens the bait pool menu when clicked
 */
public class GuidePoolButtonListener implements Listener {

    /** Final button slot (one slot to the right below the bamboo fishing rod) */
    private static final int[] BUTTON_SLOTS = {26};

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpen(InventoryOpenEvent e) {
        // Check first: the bait pool button only appears on the Water Cloud fishing rod recipe page in the Slimefun guide (title contains "Water Cloud")。
        // Third-party machine menus do not contain "Water Cloud", so skip them instead of scanning or moving buttons in every menu.
        String title = e.getView().getTitle();
        if (title == null || !ChatColor.stripColor(title).contains("Water Cloud")) {
            return;
        }
        Inventory inv = e.getInventory();
        List<Integer> found = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (WaterCloudPoolMenu.isPoolButton(it)) {
                found.add(i);
            }
        }
        if (found.isEmpty()) {
            return;
        }

        int count = Math.min(found.size(), BUTTON_SLOTS.length);
        for (int i = 0; i < count; i++) {
            ItemStack button = inv.getItem(found.get(i));
            // Clear the old slot so no glass pane is left behind
            inv.setItem(found.get(i), new ItemStack(Material.AIR));
            inv.setItem(BUTTON_SLOTS[i], button);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) {
            return;
        }
        ItemStack clicked = e.getCurrentItem();
        if (!WaterCloudPoolMenu.isPoolButton(clicked)) {
            return;
        }

        e.setCancelled(true);
        String rodId = WaterCloudPoolMenu.getButtonRodId(clicked);
        if (rodId != null) {
            SlimefunItem sfItem = SlimefunItem.getById(rodId);
            if (sfItem instanceof FishingRodWaterCloud rod) {
                WaterCloudPoolMenu.openBaitList(p, rod);
            }
        }
    }
}

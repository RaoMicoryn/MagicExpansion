package io.Yomicer.magicExpansion.items.misc;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

/**
 * Page Chest listener
 * The player UI directly uses BlockMenu, Same as the cargo container
 * Click on the empty area outside the window: Left-click: previous page, Right-click: next page
 * Play the close animation when the UI closes(Data is saved automatically by BlockMenu)
 */
public class PageChestListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof BlockMenu menu)) {
            return;
        }
        Location location = menu.getLocation();
        if (!(BlockStorage.check(location.getBlock()) instanceof PageChest)) {
            return;
        }
        // Only when you click on the empty area outside the UI(No slot was clicked)then flip the page
        if (e.getClickedInventory() != null) {
            return;
        }
        e.setCancelled(true);
        ClickType click = e.getClick();
        if (click == ClickType.WINDOW_BORDER_LEFT || click == ClickType.LEFT) {
            PageChest.flip(player, location, -1);
        } else if (click == ClickType.WINDOW_BORDER_RIGHT || click == ClickType.RIGHT) {
            PageChest.flip(player, location, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory top = e.getInventory();
        if (!(top.getHolder() instanceof BlockMenu menu)) {
            return;
        }
        Location location = menu.getLocation();
        if (BlockStorage.check(location.getBlock()) instanceof PageChest) {
            PageChest.playAnimation(player, location, false);
        }
    }
}
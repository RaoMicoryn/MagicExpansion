package io.Yomicer.magicExpansion.Listener;

import io.Yomicer.magicExpansion.MagicExpansionItemSetup;
import io.Yomicer.magicExpansion.utils.GuideMenuGroups;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.Yomicer.magicExpansion.utils.GuideCategoryMenu;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Handle clicks on container groups in the default Slimefun guide:
 * Container group icons have the guide_virtual tag. Clicking one opens the custom three-level menu instead of the default item page.
 */
public class GuideVirtualGroupClickListener implements Listener {

    /**
     * Fallback: if the default click handler opens the container group's item page first, replace it with the custom three-level menu right away.
     * Narrowed fix: only take over screens whose title matches the container group name.
     * Ignore unrelated screens, such as JEG search results, so placeholder items like "xxx · Entry" don't get picked up.
     * If an item's name contains a group name and matches a search like "fish", changing pages should not wrongly open the virtual menu.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        Inventory inv = e.getInventory();
        String vid = findVirtualId(inv);
        if (vid == null) return;
        // Only take over container group pages. Let other screens, like search results, work as usual.
        if (!GuideMenuGroups.isContainerGroupPageTitle(e.getView().getTitle())) return;
        e.setCancelled(true);
        openVirtualMenu(player, vid);
    }

    /** Stop the rainbow color effect when the menu closes. */
    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player player) {
            GuideCategoryMenu.cancelRainbow(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        // Narrowed fix: only take over the Magic 2.0 custom menu (it reuses the default home screen title).
        // If a guide_virtual item remains in a third-party machine menu (titled with the machine name), let the click work normally instead of opening the home screen.
        if (!GuideCategoryMenu.isGuideMenuTitle(player, e.getView().getTitle())) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        String vid = GuideCategoryMenu.getVirtualGroupId(clicked);
        if (vid == null) return;

        e.setCancelled(true);
        openVirtualMenu(player, vid);
    }

    /** Find the virtual group ID linked to a placeholder item on the item page. */
    private String findVirtualId(Inventory inv) {
        for (ItemStack it : inv.getContents()) {
            if (it == null) continue;
            SlimefunItem sf = SlimefunItem.getByItem(it);
            if (sf != null && sf.getId().startsWith("VIRTUAL_ENTRY_")) {
                return sf.getId().substring("VIRTUAL_ENTRY_".length()).toLowerCase();
            }
        }
        return null;
    }

    /** Reset the guide history and open the custom three-level menu. */
    private void openVirtualMenu(Player player, String vid) {
        PlayerProfile.get(player, profile -> {
            GuideHistory history = profile.getGuideHistory();
            history.clear();
            history.add(MagicExpansionItemSetup.magicexpansion, 1);
            GuideCategoryMenu.openVirtualGroup(player, profile, SlimefunGuideMode.SURVIVAL_MODE, vid);
        });
    }
}
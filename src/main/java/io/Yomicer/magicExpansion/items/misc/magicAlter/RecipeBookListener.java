package io.Yomicer.magicExpansion.items.misc.magicAlter;

import io.Yomicer.magicExpansion.MagicExpansion;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.block.Action; // [E fix] used to check the right-click action type
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class RecipeBookListener implements Listener {

    private final MagicExpansion plugin;

    public RecipeBookListener(MagicExpansion plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Check if it's in one of the recipe book GUIs
        if (title.contains("Altar Recipe") ||
                title.contains("Recipe Details") ||
                title.contains("Dispenser Items") ||
                title.contains("Dispenser List") ||
                title.contains("Altar Base")) {

            // Fully cancel the event so no item can be moved
            event.setCancelled(true);

            // Only handle clicks on items
            if (event.getCurrentItem() != null) {
                plugin.getPluginInitializer().getRecipeBookManager().handleInventoryClick(player, event.getRawSlot(), event.getCurrentItem());
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String title = event.getView().getTitle();

        // Check if it's in one of the recipe book GUIs
        if (title.contains("Altar Recipe") ||
                title.contains("Recipe Details") ||
                title.contains("Dispenser Items") ||
                title.contains("Dispenser List") ||
                title.contains("Altar Base")) {

            // Cancel the drag event so items can't be dragged
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // [E fix] only handle right-clicks with the main hand (HAND); left-click mining/attacking, off hand, etc. just pass through without cancelling the event
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();

        // Check if the recipe book was right-clicked
        if (plugin.getPluginInitializer().getRecipeBookManager().isRecipeBook(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            plugin.getPluginInitializer().getRecipeBookManager().openRecipeBook(player);
        }
    }
}
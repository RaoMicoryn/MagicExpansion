package io.Yomicer.magicExpansion.items.misc.magicAlter;

import io.Yomicer.magicExpansion.MagicExpansion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MagicAltarListener implements Listener {

    private final MagicExpansion plugin;

    public MagicAltarListener(MagicExpansion plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        // Check if a dispenser was right-clicked
        if (block != null && block.getType() == Material.DISPENSER) {
            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            // Check if the player is holding the altar wand
            if (isAltarWand(itemInHand)) {
                event.setCancelled(true);

                // Play a click sound
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);

                // Check if it's the center dispenser
                if (isCenterDispenser(block.getLocation())) {
                    // Start the crafting check
                    boolean success = plugin.getPluginInitializer().getAltarManager().startCrafting(block.getLocation(), player);

                    if (success) {
                        player.sendMessage("§a✓ Magic Altar activated! Checking the recipe...");
                    } else {
                        player.sendMessage("§c✗ Can't activate the altar, please check the structure and recipe!");
                    }
                } else {
                    player.sendMessage("§e⚠ Right-click the center dispenser to activate the whole altar!");
                }
            }
        }
    }

    // Check if the item is the altar wand
    private boolean isAltarWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(
                plugin.getPluginInitializer().getAltarWandKey(),
                PersistentDataType.BYTE
        );
    }

    // Check if it's the center dispenser (all 8 blocks around it are dispensers)
    private boolean isCenterDispenser(Location location) {
        int dispenserCount = 0;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location checkLoc = location.clone().add(x, 0, z);
                if (checkLoc.getBlock().getType() == Material.DISPENSER) {
                    dispenserCount++;
                }
            }
        }

        return dispenserCount == 9; // A 3x3 grid of dispensers
    }
}
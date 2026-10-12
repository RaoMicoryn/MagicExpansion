package io.Yomicer.magicExpansion.items.tools;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FiveElementTouchLeftClick extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    public FiveElementTouchLeftClick(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    private static final NamespacedKey KEY_X = new NamespacedKey(MagicExpansion.getInstance(), "touch_x");
    private static final NamespacedKey KEY_Y = new NamespacedKey(MagicExpansion.getInstance(), "touch_y");
    private static final NamespacedKey KEY_Z = new NamespacedKey(MagicExpansion.getInstance(), "touch_z");
    private static final NamespacedKey KEY_WORLD = new NamespacedKey(MagicExpansion.getInstance(), "touch_world");

    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return e->{
            // Stop the default behavior
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);
            Player player = e.getPlayer();
            if (e.getHand() != EquipmentSlot.HAND) {
                return;
            }

            ItemStack item = e.getItem();
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            PersistentDataContainer container = meta.getPersistentDataContainer();

            // ========================
            // Case 1: Shift + right-click a block -> Save its coordinates to the item
            // ========================
            if (player.isSneaking() && e.getClickedBlock().isPresent()) {
                Block block = e.getClickedBlock().get();

                // Save the coordinates to the item's NBT
                container.set(KEY_X, PersistentDataType.INTEGER, block.getX());
                container.set(KEY_Y, PersistentDataType.INTEGER, block.getY());
                container.set(KEY_Z, PersistentDataType.INTEGER, block.getZ());
                container.set(KEY_WORLD, PersistentDataType.STRING, block.getWorld().getName());

                // Update the lore
                updateLore(meta, block.getLocation());
                item.setItemMeta(meta);

                player.sendMessage("🔗 Linked to block: " + formatLocation(block.getLocation()));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.5f);
            }

            // ========================
            // Case 2: Right-click anything -> Try interacting with the saved block from a distance
            // ========================
            else {
                Location targetLoc = null;

                if (container.has(KEY_X, PersistentDataType.INTEGER) &&
                        container.has(KEY_Y, PersistentDataType.INTEGER) &&
                        container.has(KEY_Z, PersistentDataType.INTEGER) &&
                        container.has(KEY_WORLD, PersistentDataType.STRING)) {

                    String worldName = container.get(KEY_WORLD, PersistentDataType.STRING);
                    World world = Bukkit.getWorld(worldName);

                    if (world != null) {
                        int x = container.get(KEY_X, PersistentDataType.INTEGER);
                        int y = container.get(KEY_Y, PersistentDataType.INTEGER);
                        int z = container.get(KEY_Z, PersistentDataType.INTEGER);
                        targetLoc = new Location(world, x, y, z);
                    }
                }

                if (targetLoc != null) {
                    Block targetBlock = targetLoc.getBlock();

                    // Simulate a right-click on this block
                    PlayerInteractEvent interactEvent = new PlayerInteractEvent(
                            player,
                            Action.LEFT_CLICK_BLOCK,
                            new ItemStack(Material.AIR),
                            targetBlock,
                            BlockFace.SELF
                    );

                    Bukkit.getPluginManager().callEvent(interactEvent);

                    player.sendMessage("🔁 Five Element Touch can reach anywhere: " + formatLocation(targetLoc));
                    player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
                }
            }
        };
    }

    // Update the item lore to show the linked location
    private void updateLore(ItemMeta meta, Location loc) {
        List<String> lore;
        if (meta.hasLore()) {
            List<String> existingLore = meta.getLore();
            int size = Math.min(existingLore.size(), 8);
            lore = new ArrayList<>(existingLore.subList(0, size));
        } else {
            lore = new ArrayList<>();
        }
        lore.add("§bLinked coordinates: §fX:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ());
        lore.add("§bWorld: §f" + loc.getWorld().getName());
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    }

    // Format the location for display
    private String formatLocation(Location loc) {
        return String.format("X:%d Y:%d Z:%d (World: %s)",
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                loc.getWorld().getName());
    }

}

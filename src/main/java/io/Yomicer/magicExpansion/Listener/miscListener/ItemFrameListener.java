package io.Yomicer.magicExpansion.Listener.miscListener;
import io.Yomicer.magicExpansion.utils.log.Debug;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

// E3: Removed import javax.swing.* (GUI libraries should not be used in a Bukkit plugin)

public class ItemFrameListener implements Listener {

    @EventHandler
    public void onItemFrameDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof ItemFrame itemFrame)) return;

        if (!(e.getDamager() instanceof Player p)) return;

        if (hasTapeTag(itemFrame) || hasInfiniteTag(itemFrame)) {

            ItemStack itemInside = itemFrame.getItem();
//            SlimefunItem sfItem = SlimefunItem.getByItem(itemInside);
            // E1: Check for null or air before calling hasItemMeta(), so itemInside does not cause a NullPointerException
            if (itemInside != null && !itemInside.getType().isAir() && itemInside.hasItemMeta()) {
                e.setCancelled(true);
                ItemStack doubleDrop = itemInside.clone();
                itemFrame.getWorld().dropItemNaturally(itemFrame.getLocation(), doubleDrop);
                itemFrame.setItem(null);
                p.sendMessage("Please do not put items with NBT data in the frame");
                return;
            }


            if (null != itemInside && !itemInside.getType().isAir()) {
                e.setCancelled(true);
                // E2: Do not use itemFrame.getItemDropChance() anymore. Frames placed in other ways default to 1.0, which always gives double drops.
                // Instead, read this plugin’s Schrodinger mark on the item frame (SchrodingerFrame stores metadata using the same keys:
                // "schrodinger_frame" / "schrodinger_frame_infinite"，the same keys used by hasTapeTag/hasInfiniteTag below),
                // use a 0.5 chance when marked; use the same fixed 0.5 chance when unmarked, so the double-drop chance stays consistent
                boolean hasSchrodingerMark = hasTapeTag(itemFrame) || hasInfiniteTag(itemFrame);
                boolean willDrop = Math.random() < (hasSchrodingerMark ? 0.5f : 0.5f);
                if (willDrop) {
                    ItemStack doubleDrop = itemInside.clone();
                    doubleDrop.setAmount(itemInside.getAmount() * 2);
                    itemFrame.getWorld().dropItemNaturally(itemFrame.getLocation(), doubleDrop);
                    itemFrame.setItem(null);
                    if (!hasInfiniteTag(itemFrame)) {
                        itemFrame.remove();
                    }
                    p.sendMessage("✅ Schrodinger picked you! You got double drops:" + ItemStackHelper.getDisplayName(doubleDrop) + " x" + doubleDrop.getAmount());
                } else {
                    itemFrame.setItem(null);
                    if (!hasInfiniteTag(itemFrame)) {
                        itemFrame.remove();
                    }
                    p.sendMessage("❌ Schrodinger did not pick you this time");
                }
            }
        }

    }






    private boolean hasTapeTag(ItemFrame frame) {
        return (frame.hasMetadata("schrodinger_frame") &&
                frame.getMetadata("schrodinger_frame").get(0).asBoolean());
    }
    private boolean hasInfiniteTag(ItemFrame frame) {
        return frame.hasMetadata("schrodinger_frame_infinite") &&
                frame.getMetadata("schrodinger_frame_infinite").get(0).asBoolean();
    }



}
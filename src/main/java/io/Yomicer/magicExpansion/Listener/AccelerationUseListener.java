package io.Yomicer.magicExpansion.Listener;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.Yomicer.magicExpansion.items.misc.fish.FishAttributeGenerator;
import io.Yomicer.magicExpansion.items.misc.fish.Gen2Fish;
import io.Yomicer.magicExpansion.utils.MachineBuffManager;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Layer 2 - Machine Speed Boost (registration stage): Hold an Acceleration Fish and right-click a machine to use the fish and give the machine a 5-minute speed boost.
 * <p>
 * The first version only stores buff data (see {@link MachineBuffManager}). Machine speed changes will be added later.
 * Buff data is stored in memory and is lost when the server restarts.
 */
public class AccelerationUseListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRightClickMachine(PlayerInteractEvent e) {
        // Only handle main-hand right-clicks on blocks (ignore duplicate off-hand events).
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player player = e.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) return;

        // Only handle Gen 2 fish. Only the Acceleration Fish can give a speed boost.
        if (!FishAttributeGenerator.isGen2Fish(held)) return;
        Gen2Fish type = FishAttributeGenerator.getType(held);
        if (type == null) return;
        Gen2Fish.Trait trait = FishAttributeGenerator.getTrait(held);
        Gen2Fish.Trait effectiveTrait = trait != null ? trait : type.getDefaultTrait();
        if (effectiveTrait != Gen2Fish.Trait.ACCEL) {
            return; // Fusion Fish and normal fish are ignored.
        }

        Block clicked = e.getClickedBlock();
        if (clicked == null) return;
        // Only use it on Slimefun blocks, such as machines, so the fish isn't wasted on dirt or air.
        SlimefunItem target = StorageCacheUtils.getSfItem(clicked.getLocation());
        if (target == null) return;

        double quality = FishAttributeGenerator.getQuality(held);
        double multiplier = MachineBuffManager.BASE_MULTIPLIER * quality;

        // Use up the whole Acceleration Fish (Gen 2 fish cannot be stacked).
        player.getInventory().setItemInMainHand(null);

        // Store a 5-minute buff (machines will use it for speed changes in a later update).
        MachineBuffManager.apply(clicked.getLocation(), MachineBuffManager.DEFAULT_DURATION_SECONDS, multiplier);

        e.setCancelled(true); // Stop the machine menu from opening or the block from being placed; treat this as a "boost" action.
        player.sendMessage("§b[Fish Power] §rAdded a speed boost to §f" + ItemStackHelper.getDisplayName(target.getItem())
                + " §rAdded fish power (multiplier ×" + String.format("%.2f", multiplier) + ", lasts 5 minutes)");
    }
}
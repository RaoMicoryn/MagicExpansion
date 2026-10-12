package io.Yomicer.magicExpansion.items.misc;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getRandomGradientName;

public class PowerCard extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    // Define NamespacedKey
    private static final NamespacedKey MaxPower = new NamespacedKey(MagicExpansion.getInstance(), "max_power");
    private static final NamespacedKey NowPower = new NamespacedKey(MagicExpansion.getInstance(), "now_power");

    // Fix: static waiting flag, to stop the same player from registering the temp listener again while waiting
    private static final Map<UUID, Boolean> WAITING_INPUT = new ConcurrentHashMap<>();

    public PowerCard(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return e -> {

            // Block the default behavior (placing a block or using an item)
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);
            ItemStack item = e.getItem();
            Player player = e.getPlayer();
            // You must hold Shift to set it
            if (!player.isSneaking()) {
                player.sendMessage("§eTip: Hold Shift + right-click to set the max energy storage.");
                return;
            }
            // Start waiting for input
            waitForMaxPowerInput(player, item);

        };
    }



    /**
     * Wait for the player to enter the max energy storage (positive whole number, long range)
     */
    private void waitForMaxPowerInput(Player player, ItemStack item) {
        // Fix: ignore repeated triggers while the same player is waiting, to stop the listener from being registered again (the flag is removed when waiting ends/times out)
        if (WAITING_INPUT.putIfAbsent(player.getUniqueId(), true) != null) {
            player.sendMessage(getRandomGradientName("[Magic 2.0] You already have a setting request waiting for input, please finish it first or type 'cancel' to cancel."));
            return;
        }

        player.sendMessage(getRandomGradientName("[Magic 2.0] Enter a positive whole number as the max energy storage ( 1 ~ "+ Long.MAX_VALUE +" ):"));
        player.sendMessage(getRandomGradientName("Tip: type 'cancel' to cancel the action."));

        // <<< Declare the task variable, used for cancelling later >>>
        BukkitTask[] timeoutTask = new BukkitTask[1];
        // Create a temporary listener
        Listener tempListener = new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onPlayerChat(AsyncPlayerChatEvent event) {
                Player p = event.getPlayer();
                if (!p.equals(player)) return;

                event.setCancelled(true); // Block the message

                // Fix: the async chat thread only reads and parses text (the async-safe part)
                String message = event.getMessage().trim();

                if ("cancel".equalsIgnoreCase(message)) {
                    player.sendMessage(getRandomGradientName("[Magic 2.0] Action cancelled."));
                }else {

                    try {
                        long maxPowerValue = Long.parseLong(message);
                        if (maxPowerValue <= 0) {
                            player.sendMessage(getRandomGradientName("Error: please enter a whole number greater than 0."));
                            return; // Don't unregister, keep waiting
                        }

                        // Fix: item(PDC/Lore)changes are switched back to the main thread, so the item isn't handled on an async thread
                        final long value = maxPowerValue;
                        org.bukkit.Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> {
                            handlePowerConfiguration(item, value);
                            player.sendMessage(getRandomGradientName("✔ Successfully set the max energy storage to: " + value + " J"));
                            player.updateInventory(); // Force refresh
                        });

                    } catch (NumberFormatException ex) {
                        player.sendMessage(getRandomGradientName("Error:'" + message + "' is not a valid whole number, please try again."));
                        return; // Keep waiting
                    }
                }

                // Fix: remove the repeat-guard flag when waiting ends
                WAITING_INPUT.remove(player.getUniqueId());

                // <<< Input done or cancelled: cancel the timeout task >>>
                if (timeoutTask[0] != null && !timeoutTask[0].isCancelled()) {
                    timeoutTask[0].cancel();
                }

                // Unregister the listener when done
                org.bukkit.event.HandlerList.unregisterAll(this);
            }
        };

        // Register the temp listener
        MagicExpansion.getInstance().getServer().getPluginManager().registerEvents(tempListener, MagicExpansion.getInstance());

        // Set a 15 sec timeout
        timeoutTask[0] = MagicExpansion.getInstance().getServer().getScheduler().runTaskLater(MagicExpansion.getInstance(), () -> {
            org.bukkit.event.HandlerList.unregisterAll(tempListener);
            // Fix: remove the repeat-guard flag after timeout
            WAITING_INPUT.remove(player.getUniqueId());
            if (player.isOnline()) {
                player.sendMessage(getRandomGradientName("[Magic 2.0] ⏰ Input timed out (15 sec), cancelled automatically."));
            }
        }, 300L); // 15 sec = 300 ticks
    }

    /**
     * Handle the item's energy storage config (PDC + Lore update)
     */
    private void handlePowerConfiguration(ItemStack item, long maxPowerValue) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Get the current power (inherit it or start at 0)
        long nowPower = pdc.has(NowPower, PersistentDataType.LONG)
                ? pdc.get(NowPower, PersistentDataType.LONG) : 0L;

        // Set the max power
        pdc.set(MaxPower, PersistentDataType.LONG, maxPowerValue);
        pdc.set(NowPower, PersistentDataType.LONG, nowPower); // Set explicitly (make sure it exists)

        // Update Lore: keep the first 11 lines, add the power info
        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) {
            List<String> existingLore = meta.getLore();
            int linesToKeep = Math.min(3, existingLore.size());
            for (int i = 0; i < linesToKeep; i++) {
                lore.add(existingLore.get(i));
            }
        }

        // Add the power display
        String powerLine = ("§x§F§F§3§2§C§ECurrent stored power: " + nowPower + "/" + maxPowerValue + " J");
        lore.add(powerLine);

        meta.setLore(lore);
        item.setItemMeta(meta);
    }


}
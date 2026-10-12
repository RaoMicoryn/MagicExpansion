package io.Yomicer.magicExpansion.items.misc.fish;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.Material;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getRandomGradientName;

public class PowerEel extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    // Define the NamespacedKeys
    private static final NamespacedKey MaxPower = new NamespacedKey(MagicExpansion.getInstance(), "max_power");
    private static final NamespacedKey NowPower = new NamespacedKey(MagicExpansion.getInstance(), "now_power");

    // Fix: static waiting flag, so the same player can't register extra temporary listeners while already waiting
    private static final Map<UUID, Boolean> WAITING_INPUT = new ConcurrentHashMap<>();

    public PowerEel(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return e -> {

            // Stop the default behavior (placing a block or using the item)
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);
            ItemStack item = e.getItem();
            Player player = e.getPlayer();
            // You must hold Shift to set it
            if (!player.isSneaking()) {
                player.sendMessage("§eTip: Hold Shift + Right Click to set the max energy storage.");
                return;
            }
            // Start waiting for input
            waitForMaxPowerInput(player, item);

        };
    }



    /**
     * Wait for the player to type the max energy storage value (a positive whole number, within the long range)
     */
    private void waitForMaxPowerInput(Player player, ItemStack item) {
        // Fix: if the same player triggers this again while waiting, ignore it so listeners aren't registered twice (the flag is removed when waiting ends or times out)
        if (WAITING_INPUT.putIfAbsent(player.getUniqueId(), true) != null) {
            player.sendMessage(getRandomGradientName("[Magic 2.0] You already have a pending setup request. Finish it first, or type 'cancel' to cancel it."));
            return;
        }

        player.sendMessage(getRandomGradientName("[Magic 2.0] Type a whole number for the max energy storage ( 1 ~ "+ Long.MAX_VALUE +" ):"));
        player.sendMessage(getRandomGradientName("Tip: Type 'cancel' to cancel."));

        // <<< Declare the task variable so we can cancel it later >>>
        BukkitTask[] timeoutTask = new BukkitTask[1];
        // Create the temporary listener
        Listener tempListener = new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onPlayerChat(AsyncPlayerChatEvent event) {
                Player p = event.getPlayer();
                if (!p.equals(player)) return;

                event.setCancelled(true); // Block the message

                // Fix: in the async chat thread, only read and parse the text (the async-safe part)
                String message = event.getMessage().trim();

                if ("cancel".equalsIgnoreCase(message)) {
                    player.sendMessage(getRandomGradientName("[Magic 2.0] Cancelled."));
                }else {

                    try {
                        long maxPowerValue = Long.parseLong(message);
                        if (maxPowerValue <= 0) {
                            player.sendMessage(getRandomGradientName("Error: Please type a whole number bigger than 0."));
                            return; // Don't unregister, keep waiting
                        }

                        // Fix: changing the item (PDC/Lore) is moved back to the main thread, so we don't touch items from the async thread
                        final long value = maxPowerValue;
                        org.bukkit.Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> {
                            handlePowerConfiguration(item, value);
                            player.sendMessage(getRandomGradientName("✔ Max energy storage set to: " + value + " J"));
                            player.updateInventory(); // Force a refresh
                        });

                    } catch (NumberFormatException ex) {
                        player.sendMessage(getRandomGradientName("Error: '" + message + "' isn't a valid whole number, please try again."));
                        return; // Keep waiting
                    }
                }

                // Fix: when waiting ends, remove the anti-duplicate flag
                WAITING_INPUT.remove(player.getUniqueId());

                // <<< Input finished or cancelled: cancel the timeout task >>>
                if (timeoutTask[0] != null && !timeoutTask[0].isCancelled()) {
                    timeoutTask[0].cancel();
                }

                // Unregister the listener when done
                org.bukkit.event.HandlerList.unregisterAll(this);
            }
        };

        // Register the temporary listener
        MagicExpansion.getInstance().getServer().getPluginManager().registerEvents(tempListener, MagicExpansion.getInstance());

        // Set a 15 second timeout
        timeoutTask[0] = MagicExpansion.getInstance().getServer().getScheduler().runTaskLater(MagicExpansion.getInstance(), () -> {
            org.bukkit.event.HandlerList.unregisterAll(tempListener);
            // Fix: after a timeout, remove the anti-duplicate flag
            WAITING_INPUT.remove(player.getUniqueId());
            if (player.isOnline()) {
                player.sendMessage(getRandomGradientName("[Magic 2.0] ⏰ Timed out (15 seconds), cancelled automatically."));
            }
        }, 300L); // 15 seconds = 300 ticks
    }

    /**
     * Handle the item's energy storage setup (update PDC + Lore)
     */
    private void handlePowerConfiguration(ItemStack item, long maxPowerValue) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Get the current energy (keep it, or start from 0)
        long nowPower = pdc.has(NowPower, PersistentDataType.LONG)
                ? pdc.get(NowPower, PersistentDataType.LONG) : 0L;

        // Set the max energy
        pdc.set(MaxPower, PersistentDataType.LONG, maxPowerValue);
        pdc.set(NowPower, PersistentDataType.LONG, nowPower); // Set it explicitly (makes sure it exists)

        // Update the lore: keep the first 11 lines, then add the energy info
        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) {
            List<String> existingLore = meta.getLore();
            int linesToKeep = Math.min(16, existingLore.size());
            for (int i = 0; i < linesToKeep; i++) {
                lore.add(existingLore.get(i));
            }
        }

        // Add the energy display
        String powerLine = ("§x§F§F§3§2§C§ECurrent energy: " + nowPower + "/" + maxPowerValue + " J");
        lore.add(powerLine);

        meta.setLore(lore);
        item.setItemMeta(meta);
    }


}
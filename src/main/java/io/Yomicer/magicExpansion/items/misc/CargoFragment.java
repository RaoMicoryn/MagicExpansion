package io.Yomicer.magicExpansion.items.misc;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.utils.SameItemJudge;
import io.Yomicer.magicExpansion.utils.networksUtils.DataTypeMethods;
import io.Yomicer.magicExpansion.utils.networksUtils.NetworksKeys;
import io.Yomicer.magicExpansion.utils.networksUtils.PersistentQuantumStorageType;
import io.Yomicer.magicExpansion.utils.networksUtils.QuantumCache;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.DistinctiveItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class CargoFragment extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable, DistinctiveItem {

    // List of players waiting for input
    private static final Map<UUID, FragmentTransfer> pendingTransfers = new HashMap<>();
    private static Listener chatListener;

    // CargoFragment's PDC key
    private static final NamespacedKey KEY_CARGO_ITEM = new NamespacedKey(MagicExpansion.getInstance(), "cargo_item_json");
    private static final NamespacedKey KEY_CARGO_AMOUNT = new NamespacedKey(MagicExpansion.getInstance(), "cargo_amount");

    public CargoFragment(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
    }

    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return e -> {
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);

            Player player = e.getPlayer();

            // Only respond to main hand
            if (e.getHand() != EquipmentSlot.HAND) {
                return;
            }

            ItemStack fragmentItem = e.getItem(); // Main hand'sCargoFragment
            ItemStack offhandItem = player.getInventory().getItemInOffHand(); // Off-hand item

            // Check the main hand and off hand item amount must be1
            if (fragmentItem.getAmount() != 1 || offhandItem.getAmount() != 1) {
                if (fragmentItem.getAmount() != 1) {
                    player.sendMessage(ChatColor.RED + "Main hand CargoFragment amount must be 1!");
                    player.sendMessage(ChatColor.GRAY + "Please split off a single fragment before transferring.");
                }
                if (offhandItem.getAmount() != 1) {
                    player.sendMessage(ChatColor.RED + "Off-hand quantum storage item amount must be 1!");
                    player.sendMessage(ChatColor.GRAY + "Please make sure you only hold one storage container.");
                }
                playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
                return;
            }

            // 1. Check off-hand item
            if (offhandItem == null || offhandItem.getType().isAir()) {
                sendError(player, "Put the quantum storage item in your off hand!");
                return;
            }

            // 2. Get from the off-hand itemQuantumCache
            ItemMeta offhandMeta = offhandItem.getItemMeta();
            if (offhandMeta == null) {
                sendError(player, "Off-hand item data is broken!");
                return;
            }

            // Get it the way you providedQuantumCache
            QuantumCache quantumCache = DataTypeMethods.getCustom(offhandMeta,
                    NetworksKeys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE);

            if (quantumCache == null || quantumCache.getItemStack() == null) {
                sendError(player, "Off-hand item has no valid quantum storage data!");
                return;
            }

            // 3. Get CargoFragment data
            ItemMeta fragmentMeta = fragmentItem.getItemMeta();
            if (fragmentMeta == null) {
                sendError(player, "Fragment data is corrupted!");
                return;
            }

            PersistentDataContainer fragmentPdc = fragmentMeta.getPersistentDataContainer();
            String itemJson = fragmentPdc.get(KEY_CARGO_ITEM, PersistentDataType.STRING);
            Integer fragmentAmount = fragmentPdc.get(KEY_CARGO_AMOUNT, PersistentDataType.INTEGER);

            if (itemJson == null || fragmentAmount == null || fragmentAmount <= 0) {
                sendError(player, "There's no stored item in this fragment!");
                return;
            }

            // 4. Parse the item in CargoFragment
            ItemStack storedItem = SameItemJudge.itemFromBase64(itemJson);
            if (storedItem == null) {
                sendError(player, "The item data in the fragment is corrupted!");
                return;
            }

            // 5. Check if the item type matches
            ItemStack cacheItem = quantumCache.getItemStack();
            if (!SlimefunUtils.isItemSimilar(storedItem, cacheItem, true)) {
                player.sendMessage(ChatColor.RED + "Item type doesn't match!");
                player.sendMessage(ChatColor.GRAY + "Fragment: " + ItemStackHelper.getDisplayName(storedItem));
                player.sendMessage(ChatColor.GRAY + "Storage: " + ItemStackHelper.getDisplayName(cacheItem));
                playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
                return;
            }

            // 6. Calculate transferable amount
            long currentAmount = quantumCache.getAmount();
            long maxCapacity = quantumCache.getLimit();
            long remainingSpace = maxCapacity - currentAmount;

            if (remainingSpace <= 0) {
                sendError(player, "§cQuantum storage is full!");
                return;
            }

            long maxTransfer = Math.min(fragmentAmount, remainingSpace);

            // 7. Create waiting state
            FragmentTransfer transfer = new FragmentTransfer(
                    player.getUniqueId(),
                    fragmentItem,
                    offhandItem,
                    storedItem,
                    fragmentAmount,
                    quantumCache,
                    maxTransfer
            );

            pendingTransfers.put(player.getUniqueId(), transfer);

            // 8. Show input hint
            showTransferPrompt(player, storedItem, fragmentAmount, currentAmount, maxCapacity, maxTransfer);

            // 9. Register chat listener
            registerChatListener();

            // 10. Set timeout
            scheduleTimeout(player.getUniqueId());
        };
    }

    /**
     * Show transfer hint message
     */
    private void showTransferPrompt(Player player, ItemStack item, long fragmentAmount,
                                    long currentStored, long maxCapacity, long maxTransfer) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "══════ " + ChatColor.BOLD + "Item transfer" + ChatColor.RESET + ChatColor.GOLD + " ══════");
        player.sendMessage(ChatColor.YELLOW + "Item type: " + ChatColor.WHITE + ItemStackHelper.getDisplayName(item));
        player.sendMessage(ChatColor.YELLOW + "Fragment stock: " + ChatColor.GREEN + fragmentAmount + " pcs");
        player.sendMessage(ChatColor.YELLOW + "Storage state: " + ChatColor.AQUA + currentStored + "/" + maxCapacity);
        player.sendMessage(ChatColor.YELLOW + "Transferable amount: " + ChatColor.GOLD + maxTransfer + " pcs");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Enter the transfer amount (" + ChatColor.GREEN + "1-" + maxTransfer + ChatColor.YELLOW + ")");
        player.sendMessage(ChatColor.YELLOW + "or input " + ChatColor.GREEN + "'all'" + ChatColor.YELLOW + " Transfer all transferable amount");
        player.sendMessage(ChatColor.GRAY + "Input " + ChatColor.RED + "'cancel'" + ChatColor.GRAY + " Cancel action (30sec timeout)");
        player.sendMessage("");

        playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.5f);
    }

    /**
     * Register chat event listener
     */
    private void registerChatListener() {
        if (chatListener != null) return;

        chatListener = new Listener() {
            @EventHandler
            public void onPlayerChat(AsyncPlayerChatEvent event) {
                Player player = event.getPlayer();
                FragmentTransfer transfer = pendingTransfers.get(player.getUniqueId());

                if (transfer == null) return;

                event.setCancelled(true);

                String input = event.getMessage().trim();

                Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> {
                    processTransferInput(player, transfer, input);
                });
            }

            @EventHandler
            public void onPlayerQuit(PlayerQuitEvent event) {
                pendingTransfers.remove(event.getPlayer().getUniqueId());
                cleanupListenerIfNeeded();
            }
        };

        Bukkit.getPluginManager().registerEvents(chatListener, MagicExpansion.getInstance());
    }

    /**
     * Handle player input
     */
    private void processTransferInput(Player player, FragmentTransfer transfer, String input) {
        try {
            // Handle cancel
            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.YELLOW + "Transfer action cancelled.");
                pendingTransfers.remove(player.getUniqueId());
                cleanupListenerIfNeeded();
                return;
            }

            // === New: check if the item has changed ===
            if (!validateItemsUnchanged(player, transfer)) {
                player.sendMessage(ChatColor.RED + "Action failed: the item has changed!");
                pendingTransfers.remove(player.getUniqueId());
                cleanupListenerIfNeeded();
                return;
            }

            int transferAmount;

            // Handle"all"
            if (input.equalsIgnoreCase("all")) {
                transferAmount = (int) transfer.maxTransfer;
            } else {
                // Parse number
                try {
                    transferAmount = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Please enter a valid number, 'all' or 'cancel'!");
                    return;
                }

                // Check range
                if (transferAmount <= 0) {
                    player.sendMessage(ChatColor.RED + "Please enter a number greater than 0!");
                    return;
                }

                if (transferAmount > transfer.maxTransfer) {
                    player.sendMessage(ChatColor.RED + "Exceeds the max transferable amount (" + transfer.maxTransfer + ")!");
                    return;
                }
            }

            // Run transfer
            executeQuantumTransfer(player, transfer, transferAmount);

        } finally {
            pendingTransfers.remove(player.getUniqueId());
            cleanupListenerIfNeeded();
        }
    }

    /**
     * Check if the item changed while waiting for input
     */
    private boolean validateItemsUnchanged(Player player, FragmentTransfer transfer) {
        // 1. Check if the player is online
        if (!player.isOnline()) {
            return false;
        }

        // 2. Check if the main hand item is still the fragment
        ItemStack currentMainHand = player.getInventory().getItemInMainHand();
        if (currentMainHand == null || currentMainHand.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "Main hand item is gone!");
            return false;
        }

        // Check if the amount is1
        if (currentMainHand.getAmount() != 1) {
            player.sendMessage(ChatColor.RED + "Main hand item amount changed!");
            return false;
        }

        // Check if it's the same fragment item
        if (!isSameFragmentItem(currentMainHand, transfer.fragmentItem)) {
            player.sendMessage(ChatColor.RED + "Main hand item was changed!");
            return false;
        }

        // 3. Check if the off-hand item is still a quantum storage item
        ItemStack currentOffhand = player.getInventory().getItemInOffHand();
        if (currentOffhand == null || currentOffhand.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "Off-hand item is gone!");
            return false;
        }

        // Check if the amount is1
        if (currentOffhand.getAmount() != 1) {
            player.sendMessage(ChatColor.RED + "Off-hand item amount changed!");
            return false;
        }

        // Check if it's the same quantum storage item
        if (!isSameQuantumStorageItem(currentOffhand, transfer.offhandItem)) {
            player.sendMessage(ChatColor.RED + "Off-hand item was changed!");
            return false;
        }

        // 4. Check if the quantum storage data changed
        ItemMeta currentOffhandMeta = currentOffhand.getItemMeta();
        if (currentOffhandMeta == null) {
            player.sendMessage(ChatColor.RED + "Off-hand item data is broken!");
            return false;
        }

        QuantumCache currentCache = DataTypeMethods.getCustom(currentOffhandMeta,
                NetworksKeys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE);

        if (currentCache == null) {
            player.sendMessage(ChatColor.RED + "Off-hand item's quantum storage data is lost!");
            return false;
        }

        // Check if the stored items are the same
        ItemStack currentCacheItem = currentCache.getItemStack();
        long currentStoredAmount = currentCache.getAmount();
        long currentMaxCapacity = currentCache.getLimit();

        if (currentCacheItem == null) {
            player.sendMessage(ChatColor.RED + "The item in the quantum storage is gone!");
            return false;
        }

        // Check if the fragment data changed
        ItemMeta currentFragmentMeta = currentMainHand.getItemMeta();
        if (currentFragmentMeta == null) {
            player.sendMessage(ChatColor.RED + "Fragment data is corrupted!");
            return false;
        }

        PersistentDataContainer fragmentPdc = currentFragmentMeta.getPersistentDataContainer();
        String currentItemJson = fragmentPdc.get(KEY_CARGO_ITEM, PersistentDataType.STRING);
        Integer currentFragmentAmount = fragmentPdc.get(KEY_CARGO_AMOUNT, PersistentDataType.INTEGER);

        if (currentItemJson == null || currentFragmentAmount == null) {
            player.sendMessage(ChatColor.RED + "Fragment data is incomplete!");
            return false;
        }

        // Parse the item in the fragment
        ItemStack currentStoredItem = SameItemJudge.itemFromBase64(currentItemJson);
        if (currentStoredItem == null) {
            player.sendMessage(ChatColor.RED + "The item data in the fragment is corrupted!");
            return false;
        }

        // Check if the item type matches
        if (!SlimefunUtils.isItemSimilar(currentStoredItem, currentCacheItem, true)) {
            player.sendMessage(ChatColor.RED + "Item type no longer matches!");
            return false;
        }

        // Check if the item in the fragment is the same as before
        if (!SlimefunUtils.isItemSimilar(currentStoredItem, transfer.storedItem, true)) {
            player.sendMessage(ChatColor.RED + "The item in the fragment has changed!");
            return false;
        }

        // Check if the fragment amount decreased
        if (currentFragmentAmount < transfer.fragmentAmount) {
            player.sendMessage(ChatColor.YELLOW + "Fragment amount decreased, update the transferable amount!");
            transfer.fragmentAmount = currentFragmentAmount;
        }

        // Check if the storage capacity changed
        if (currentStoredAmount != transfer.quantumCache.getAmount() ||
                currentMaxCapacity != transfer.quantumCache.getLimit()) {

            long remainingSpace = currentMaxCapacity - currentStoredAmount;
            long newMaxTransfer = Math.min(transfer.fragmentAmount, remainingSpace);

            if (remainingSpace <= 0) {
                player.sendMessage(ChatColor.RED + "Quantum storage is full!");
                return false;
            }

            if (newMaxTransfer < transfer.maxTransfer) {
                player.sendMessage(ChatColor.YELLOW + "Transferable amount updated to: " + newMaxTransfer);
                transfer.maxTransfer = newMaxTransfer;
            }

            transfer.quantumCache = currentCache;
        }

        return true;
    }

    /**
     * Check if it's the same fragment item
     */
    private boolean isSameFragmentItem(ItemStack item1, ItemStack item2) {
        // Simple comparison: type and custom name
        if (item1.getType() != item2.getType()) {
            return false;
        }

        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();

        if (meta1 == null || meta2 == null) {
            return meta1 == meta2;
        }

        // Compare display names (CargoFragment should have a special display name)
        String name1 = meta1.getDisplayName();
        String name2 = meta2.getDisplayName();

        if (name1 == null && name2 == null) {
            return true;
        }

        if (name1 == null || name2 == null) {
            return false;
        }

        return name1.equals(name2);
    }

    /**
     * Check if it's the same quantum storage item
     */
    private boolean isSameQuantumStorageItem(ItemStack item1, ItemStack item2) {
        // Compare the basic info first
        if (item1.getType() != item2.getType()) {
            return false;
        }

        // Get the PDC data of both items
        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();

        if (meta1 == null || meta2 == null) {
            return meta1 == meta2;
        }

        // Get QuantumCache from PDC for comparison
        QuantumCache cache1 = DataTypeMethods.getCustom(meta1,
                NetworksKeys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE);

        QuantumCache cache2 = DataTypeMethods.getCustom(meta2,
                NetworksKeys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE);

        // If one has no quantum cache, they're different
        if (cache1 == null || cache2 == null) {
            return false;
        }

        if (cache1.getAmount() != 0 && cache2.getAmount() != 0){
            return cache1.getAmount() == cache2.getAmount();
        }

        // Compare cache ID or other unique identifiers (if QuantumCache has a getId method)
        try {
            // If there's a getId method
            if (cache1.getItemStack() != null && cache2.getItemStack() != null) {
                return cache1.getItemStack().equals(cache2.getItemStack());
            }
        } catch (Exception e) {
            // If QuantumCache has no getId method, use hashCode as a backup
            return cache1.hashCode() == cache2.hashCode();
        }

        return false;
    }

    /**
     * Run quantum storage transfer
     */
    private void executeQuantumTransfer(Player player, FragmentTransfer transfer, int amount) {
        try {
            // 1. Increase quantum storage amount
            transfer.quantumCache.increaseAmount(amount);

            // 2. Update the off-hand item's QuantumCache data
            updateQuantumCacheInItem(transfer.offhandItem, transfer.quantumCache);

            // 3. UpdateCargoFragment
            long newFragmentAmount = transfer.fragmentAmount - amount;

            if (newFragmentAmount <= 0) {
                // Delete fragment
                if (transfer.fragmentItem.getAmount() > 1) {
                    transfer.fragmentItem.setAmount(transfer.fragmentItem.getAmount() - 1);
                    player.getInventory().setItemInMainHand(transfer.fragmentItem);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }

                player.sendMessage(ChatColor.GREEN + "✓ Transferred " + ChatColor.YELLOW + amount +
                        ChatColor.GREEN + " items, fragment used up.");
            } else {
                // Update fragment amount
                updateFragmentData(transfer.fragmentItem, newFragmentAmount);

                player.sendMessage(ChatColor.GREEN + "✓ Successfully transferred " + ChatColor.YELLOW + amount +
                        ChatColor.GREEN + " items.");
                player.sendMessage(ChatColor.GRAY + "Fragment remaining: " + newFragmentAmount + " pcs");
            }

            // 4. Update off-hand item display
            player.getInventory().setItemInOffHand(transfer.offhandItem);

            // 5. Play success sound
            playSound(player, Sound.ENTITY_ITEM_PICKUP, 1.2f);

        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Transfer failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update QuantumCache data to the item
     */
    private void updateQuantumCacheInItem(ItemStack item, QuantumCache cache) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Update QuantumCache to the item'sPDC
        DataTypeMethods.setCustom(meta, NetworksKeys.QUANTUM_STORAGE_INSTANCE,
                PersistentQuantumStorageType.TYPE, cache);
        cache.updateMetaLore(meta);
        item.setItemMeta(meta);
    }

    /**
     * Update CargoFragment data
     */
    private void updateFragmentData(ItemStack fragment, long newAmount) {
        ItemMeta meta = fragment.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_CARGO_AMOUNT, PersistentDataType.INTEGER, (int) newAmount);

        // Update the amount shown in Lore
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains("Amount: ")) {
                lore.set(i, "§fAmount: §a" + newAmount);
                break;
            }
        }

        meta.setLore(lore);
        fragment.setItemMeta(meta);
    }

    /**
     * Set timeout task
     */
    private void scheduleTimeout(UUID playerId) {
        Bukkit.getScheduler().runTaskLater(MagicExpansion.getInstance(), () -> {
            FragmentTransfer transfer = pendingTransfers.remove(playerId);
            if (transfer != null) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.RED + "Action timed out and was cancelled automatically.");
                }
                cleanupListenerIfNeeded();
            }
        }, 20 * 30); // 30sec
    }

    /**
     * Clean up listeners
     */
    private void cleanupListenerIfNeeded() {
        if (pendingTransfers.isEmpty() && chatListener != null) {
            HandlerList.unregisterAll(chatListener);
            chatListener = null;
        }
    }

    /**
     * Send error message
     */
    private void sendError(Player player, String message) {
        player.sendMessage(ChatColor.RED + message);
        playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
    }

    /**
     * Play sound
     */
    private void playSound(Player player, Sound sound, float pitch) {
        player.playSound(player.getLocation(), sound, 0.8f, pitch);
    }

    @Override
    public boolean canStack(@NotNull ItemMeta itemMeta, @NotNull ItemMeta itemMeta1) {
        return false;
    }

    /**
     * Transfer task info class
     */
    private static class FragmentTransfer {
        final UUID playerId;
        final ItemStack fragmentItem;
        final ItemStack offhandItem;
        final ItemStack storedItem;
        long fragmentAmount; // Changed to non-final, may be updated
        QuantumCache quantumCache; // Changed to non-final, may be updated
        long maxTransfer; // Changed to non-final, may be updated

        FragmentTransfer(UUID playerId, ItemStack fragmentItem, ItemStack offhandItem,
                         ItemStack storedItem, long fragmentAmount,
                         QuantumCache quantumCache, long maxTransfer) {
            this.playerId = playerId;
            this.fragmentItem = fragmentItem;
            this.offhandItem = offhandItem;
            this.storedItem = storedItem;
            this.fragmentAmount = fragmentAmount;
            this.quantumCache = quantumCache;
            this.maxTransfer = maxTransfer;
        }
    }
}
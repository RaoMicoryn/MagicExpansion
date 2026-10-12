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
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
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

import static io.Yomicer.magicExpansion.core.MagicExpansionItems.CARGO_FRAGMENT;

public class CargoFragmentExtract extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    // List of players waiting for input
    private static final Map<UUID, ExtractOperation> pendingExtracts = new HashMap<>();
    private static Listener chatListener;

    public CargoFragmentExtract(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
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

            ItemStack extractItem = e.getItem(); // Main hand'sCargoFragmentExtract
            ItemStack offhandItem = player.getInventory().getItemInOffHand(); // Off-hand item

            // 1. Check the main hand and off hand item amount must be1
            if (extractItem.getAmount() != 1 || offhandItem.getAmount() != 1) {
                if (extractItem.getAmount() != 1) {
                    player.sendMessage(ChatColor.RED + "Main hand extractor amount must be 1!");
                    player.sendMessage(ChatColor.GRAY + "Please make sure you only use one extractor.");
                }
                if (offhandItem.getAmount() != 1) {
                    player.sendMessage(ChatColor.RED + "Off-hand quantum storage item amount must be 1!");
                    player.sendMessage(ChatColor.GRAY + "Please make sure you only hold one storage container.");
                }
                playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f);
                return;
            }

            // 2. Check off-hand item
            if (offhandItem == null || offhandItem.getType().isAir()) {
                sendError(player, "Put the quantum storage item in your off hand!");
                return;
            }

            // 3. Get from the off-hand itemQuantumCache
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

            // 4. Get the item and amount in quantum storage
            ItemStack storedItem = quantumCache.getItemStack();
            long storedAmount = quantumCache.getAmount();

            if (storedItem == null || storedAmount <= 1) {
                // Amount in storage≤1can't be extracted (need to keep at least 1)
                sendError(player, "Not enough items in the quantum storage! You need at least 2 items to extract.");
                return;
            }

            // 5. Calculate the max extractable amount (current amount - 1)
            long maxExtract = storedAmount - 1;

            // 6. Create waiting-for-input state
            ExtractOperation operation = new ExtractOperation(
                    player.getUniqueId(),
                    extractItem,
                    offhandItem,
                    storedItem,
                    storedAmount,
                    quantumCache,
                    maxExtract
            );

            pendingExtracts.put(player.getUniqueId(), operation);

            // 7. Show extract hint
            showExtractPrompt(player, storedItem, storedAmount, maxExtract);

            // 8. Register chat listener
            registerChatListener();

            // 9. Set timeout
            scheduleTimeout(player.getUniqueId());
        };
    }

    /**
     * Show extract hint message
     */
    private void showExtractPrompt(Player player, ItemStack storedItem, long storedAmount, long maxExtract) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "══════ " + ChatColor.BOLD + "Item extraction" +
                ChatColor.RESET + ChatColor.GOLD + " ══════");
        player.sendMessage(ChatColor.YELLOW + "Item type: " + ChatColor.WHITE +
                ItemStackHelper.getDisplayName(storedItem));
        player.sendMessage(ChatColor.YELLOW + "Total stored: " + ChatColor.AQUA + storedAmount + " pcs");
        player.sendMessage(ChatColor.YELLOW + "Max takeable: " + ChatColor.GOLD + maxExtract + " pcs");
        player.sendMessage(ChatColor.YELLOW + "Minimum kept: " + ChatColor.GREEN + "1 pcs");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Enter the amount to extract (" + ChatColor.GREEN + "1-" +
                maxExtract + ChatColor.YELLOW + ")");
        player.sendMessage(ChatColor.GRAY + "Input " + ChatColor.RED + "'cancel'" +
                ChatColor.GRAY + " Cancel action (30sec timeout)");
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
                ExtractOperation operation = pendingExtracts.get(player.getUniqueId());

                if (operation == null) return;

                event.setCancelled(true);

                String input = event.getMessage().trim();

                Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> {
                    processExtractInput(player, operation, input);
                });
            }

            @EventHandler
            public void onPlayerQuit(PlayerQuitEvent event) {
                pendingExtracts.remove(event.getPlayer().getUniqueId());
                cleanupListenerIfNeeded();
            }
        };

        Bukkit.getPluginManager().registerEvents(chatListener, MagicExpansion.getInstance());
    }

    /**
     * Handle player extract input
     */
    private void processExtractInput(Player player, ExtractOperation operation, String input) {
        try {
            // Handle cancel
            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.YELLOW + "Extract action cancelled.");
                pendingExtracts.remove(player.getUniqueId());
                cleanupListenerIfNeeded();
                return;
            }

            long extractAmount;

            // Parse number
            try {
                extractAmount = Long.parseLong(input);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Please enter a valid number or 'cancel'!");
                return;
            }

            // Check range
            if (extractAmount <= 0) {
                player.sendMessage(ChatColor.RED + "Please enter a number greater than 0!");
                return;
            }

            if (extractAmount > operation.maxExtract) {
                player.sendMessage(ChatColor.RED + "Exceeds the max extractable amount (" + operation.maxExtract + ")!");
                return;
            }

            // Make sure at least 1 is kept after extraction
            if (operation.storedAmount - extractAmount < 1) {
                player.sendMessage(ChatColor.RED + "At least 1 item must stay in the quantum storage after extraction!");
                return;
            }

            // Make sure it doesn't go over the int max (because CargoFragment stores the amount as an int)
            if (extractAmount > Integer.MAX_VALUE) {
                player.sendMessage(ChatColor.RED + "Extract amount is over the max limit (" + Integer.MAX_VALUE + ")!");
                return;
            }

            // === New: check if the item has changed ===
            if (!validateItemsUnchanged(player, operation)) {
                player.sendMessage(ChatColor.RED + "Action failed: the item has changed!");
                pendingExtracts.remove(player.getUniqueId());
                cleanupListenerIfNeeded();
                return;
            }

            // Run extract
            executeExtract(player, operation, extractAmount);

        } finally {
            pendingExtracts.remove(player.getUniqueId());
            cleanupListenerIfNeeded();
        }
    }

    /**
     * Check if the item changed while waiting for input
     */
    private boolean validateItemsUnchanged(Player player, ExtractOperation operation) {
        // 1. Check if the player is online
        if (!player.isOnline()) {
            return false;
        }

        // 2. Check if the main hand item is still the extractor
        ItemStack currentMainHand = player.getInventory().getItemInMainHand();
        if (currentMainHand == null || currentMainHand.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "Main hand item is gone!");
            return false;
        }

        // Check if it's the same item (by type, name, etc.)
        if (!isSameExtractorItem(currentMainHand, operation.extractItem)) {
            player.sendMessage(ChatColor.RED + "Main hand item was changed!");
            return false;
        }

        // Check if the amount is1
        if (currentMainHand.getAmount() != 1) {
            player.sendMessage(ChatColor.RED + "Main hand item amount changed!");
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

        // Check if it's the same quantum storage item (via the unique ID in PDC)
        if (!isSameQuantumStorageItem(currentOffhand, operation.offhandItem)) {
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
        ItemStack currentStoredItem = currentCache.getItemStack();
        long currentStoredAmount = currentCache.getAmount();

        if (currentStoredItem == null) {
            player.sendMessage(ChatColor.RED + "The item in the quantum storage is gone!");
            return false;
        }

        // Use SameItemJudge to check if items are the same
        if (!SameItemJudge.isSimilarSafe(currentStoredItem, operation.storedItem)) {
            player.sendMessage(ChatColor.RED + "The item in the quantum storage has changed!");
            return false;
        }

        // Check if the amount is the same (can't be less than the amount recorded before)
        if (currentStoredAmount < operation.storedAmount) {
            player.sendMessage(ChatColor.RED + "The item amount in the quantum storage decreased!");
            return false;
        }

        // If the amount went up, use the new amount for the calculation
        if (currentStoredAmount > operation.storedAmount) {
            // Update the amount info in the action
            operation.storedAmount = currentStoredAmount;
            operation.maxExtract = currentStoredAmount - 1;

            // Tell the player the amount was updated
            player.sendMessage(ChatColor.GREEN + "Detected an amount increase, updated the extractable amount!");
            player.sendMessage(ChatColor.YELLOW + "New total stored: " + ChatColor.AQUA + currentStoredAmount + " pcs");
            player.sendMessage(ChatColor.YELLOW + "New max takeable: " + ChatColor.GOLD + operation.maxExtract + " pcs");

            // Update cache reference
            operation.quantumCache = currentCache;
        }

        return true;
    }

    /**
     * Check if it's the same extractor item
     */
    private boolean isSameExtractorItem(ItemStack item1, ItemStack item2) {
        // Simple comparison: type and custom name
        if (item1.getType() != item2.getType()) {
            return false;
        }

        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();

        if (meta1 == null || meta2 == null) {
            return meta1 == meta2;
        }

        // Compare display names
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
            if (cache1.getItemStack()!= null && cache2.getItemStack() != null) {
                return cache1.getItemStack().equals(cache2.getItemStack());
            }
        } catch (Exception e) {
            // If QuantumCache has no getId method, use hashCode as a backup
            return cache1.hashCode() == cache2.hashCode();
        }

        return false;
    }


    /**
     * Run extract action
     */
    private void executeExtract(Player player, ExtractOperation operation, long extractAmount) {
        try {
            // 1. Reduce the quantum storage amount
            long newStoredAmount = operation.storedAmount - extractAmount;
//            operation.quantumCache.setAmount((int) newStoredAmount);
            operation.quantumCache.setAmount(newStoredAmount);

            // 2. Update the off-hand item's QuantumCache data
            updateQuantumCacheInItem(operation.offhandItem, operation.quantumCache);

            // 3. Create Aether Casket
            ItemStack cargoFragment = createCargoFragment(operation.storedItem, (int) extractAmount);
            if (cargoFragment == null) {
                player.sendMessage(ChatColor.RED + "Failed to create Aether Casket!");
                return;
            }

            // 4. Give the player an Aether Casket
            giveCargoFragmentToPlayer(player, cargoFragment, operation.storedItem, extractAmount, newStoredAmount);

            // 5. Play success sound
            playSound(player, Sound.ENTITY_ITEM_PICKUP, 1.2f);

        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Extraction failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create a CargoFragment item that stands for a certain item's"Storage fragment"
     */
    private ItemStack createCargoFragment(ItemStack original, int amount) {
        ItemStack fragment = CARGO_FRAGMENT.clone();
        fragment.setType(original.getType());
        ItemMeta meta = fragment.getItemMeta();
        if (meta == null) return null;

        // === Display name: Storage Fragment: Original item name ===
        String itemName = ItemStackHelper.getDisplayName(original);
        if (original.hasItemMeta() && original.getItemMeta().hasDisplayName()) {
            itemName = original.getItemMeta().getDisplayName();
        }
        meta.setDisplayName("§b[Aether Casket]");

        // === Lore: original item Lore + Amount ===
        List<String> lore = new ArrayList<>();

        lore.add(itemName);

        if (original.hasItemMeta() && original.getItemMeta().hasLore()) {
            lore.addAll(original.getItemMeta().getLore());
            lore.add("");
        }

        lore.add("§fAmount: §a" + amount);
        meta.setLore(lore);

        // === Write to PDC: original item (JSON)+ Amount ===
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // Store the original item JSON (easy to restore)
        String json = itemToBase64(original.clone());
        if (json != null) {
            NamespacedKey keyItem = new NamespacedKey(MagicExpansion.getInstance(), "cargo_item_json");
            container.set(keyItem, PersistentDataType.STRING, json);
        }

        // Stored amount
        NamespacedKey keyAmount = new NamespacedKey(MagicExpansion.getInstance(), "cargo_amount");
        container.set(keyAmount, PersistentDataType.INTEGER, amount);

        fragment.setItemMeta(meta);
        return fragment;
    }

    /**
     * Serialize the item to a Base64 string
     */
    private String itemToBase64(ItemStack item) {
        try {
            return SameItemJudge.itemToBase64(item);
        } catch (Exception e) {
            return null;
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
     * Give the player an Aether Casket
     */
    private void giveCargoFragmentToPlayer(Player player, ItemStack cargoFragment, ItemStack storedItem,
                                           long extractedAmount, long remainingAmount) {
        // Try to add to the player's inventory
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(cargoFragment);

        // If there are leftover items (inventory is full)
        if (!leftover.isEmpty()) {
            // Dropped on the ground
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), item);
            }

            player.sendMessage(ChatColor.GREEN + "✓ Successfully took out " + ChatColor.YELLOW + extractedAmount +
                    ChatColor.GREEN + " x " + ItemStackHelper.getDisplayName(storedItem));
            player.sendMessage(ChatColor.GRAY + "Inventory is full, the Aether Casket was dropped on the ground!");
        } else {
            player.sendMessage(ChatColor.GREEN + "✓ Successfully took out " + ChatColor.YELLOW + extractedAmount +
                    ChatColor.GREEN + " x " + ItemStackHelper.getDisplayName(storedItem));
            player.sendMessage(ChatColor.GRAY + "Got an Aether Casket!");
        }

        // Show remaining quantum storage amount
        player.sendMessage(ChatColor.GRAY + "Quantum storage remaining: " + remainingAmount + " pcs");

        // Update off-hand item display
        player.getInventory().setItemInOffHand(player.getInventory().getItemInOffHand());
    }

    /**
     * Set timeout task
     */
    private void scheduleTimeout(UUID playerId) {
        Bukkit.getScheduler().runTaskLater(MagicExpansion.getInstance(), () -> {
            ExtractOperation operation = pendingExtracts.remove(playerId);
            if (operation != null) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.RED + "Extract action timed out and was cancelled automatically.");
                }
                cleanupListenerIfNeeded();
            }
        }, 20 * 30); // 30sec
    }

    /**
     * Clean up listeners
     */
    private void cleanupListenerIfNeeded() {
        if (pendingExtracts.isEmpty() && chatListener != null) {
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

    /**
     * Extract operation info class
     */
    private static class ExtractOperation {
        final UUID playerId;
        final ItemStack extractItem;
        final ItemStack offhandItem;
        ItemStack storedItem; // Changed to non-final, may be updated
        long storedAmount; // Changed to non-final, may be updated
        QuantumCache quantumCache; // Changed to non-final, may be updated
        long maxExtract; // Changed to non-final, may be updated

        ExtractOperation(UUID playerId, ItemStack extractItem, ItemStack offhandItem,
                         ItemStack storedItem, long storedAmount,
                         QuantumCache quantumCache, long maxExtract) {
            this.playerId = playerId;
            this.extractItem = extractItem;
            this.offhandItem = offhandItem;
            this.storedItem = storedItem;
            this.storedAmount = storedAmount;
            this.quantumCache = quantumCache;
            this.maxExtract = maxExtract;
        }
    }
}
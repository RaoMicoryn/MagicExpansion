package io.Yomicer.magicExpansion.items.misc;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortableCargoTransporter extends SlimefunItem implements Listener {

    // Transfer state management
    private static final Map<UUID, Boolean> playerTransmissionStatus = new HashMap<>();
    private static final Map<UUID, BukkitRunnable> activeTransmissions = new HashMap<>();

    // Last check time record, to avoid checking too often
    private static final Map<UUID, Long> lastCheckTime = new HashMap<>();

    public PortableCargoTransporter(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);

        // Register event listener
        MagicExpansion.getInstance().getServer().getPluginManager().registerEvents(this, MagicExpansion.getInstance());

        // Add the item use handler
        addItemHandler(onItemUse());

        // Start the global cleanup task
        startCleanupTask();
    }

    /**
     * Item use handler
     */
    @Nonnull
    private ItemUseHandler onItemUse() {
        return e -> {
            e.cancel();
            Player player = e.getPlayer();
            ItemStack item = e.getItem();

            if (player.isSneaking()) {
                // Shift+Right-click: show usage guide
                showUsage(player);
            } else {
                // Normal right-click: toggle transfer state
                toggleTransmission(player);
            }
        };
    }

    /**
     * Show the usage guide
     */
    private void showUsage(Player player) {
        player.sendMessage("§6§lPortable Aether Casket Transporter usage guide");
        player.sendMessage("§7§m--------------------------------");
        player.sendMessage("§fRight-click: §aStart transfer");
        player.sendMessage("§fRight-click again: §cStop transfer");
        player.sendMessage("§fShift+Right-click: §eShow this guide");
        player.sendMessage("");
        player.sendMessage("§7● Auto-detect the Aether Casket in the inventory");
        player.sendMessage("§7● Only handle Aether Caskets with a stack size of 1");
        player.sendMessage("§7● Each time, handle the first Aether Casket in the inventory");
        player.sendMessage("§7● Automatically delete the Aether Casket when the amount is 0");
        player.sendMessage("§7● Transfer interval: 0.05 sec");
        player.sendMessage("§7● Stop the transfer automatically when the player goes offline");
        player.sendMessage("§7§m--------------------------------");
    }

    /**
     * Toggle transfer state (triggered by right-click)
     */
    private void toggleTransmission(Player player) {
        UUID playerId = player.getUniqueId();

        // Check the current state
        boolean isActive = playerTransmissionStatus.getOrDefault(playerId, false);

        if (isActive) {
            // Stop transfer
            stopTransmission(player);
            player.sendMessage("§c✗ Transfer stopped!");
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.0f);
        } else {
            // Start transfer
            startTransmission(player);
        }
    }

    /**
     * Start transfer
     */
    private void startTransmission(Player player) {
        UUID playerId = player.getUniqueId();

        // Check if it's already transferring
        if (activeTransmissions.containsKey(playerId)) {
            return;
        }

        // Check if there's an Aether Casket in the inventory
        if (!hasValidCargoFragment(player)) {
            player.sendMessage("§cThere's no valid Aether Casket in your inventory!");
            player.sendMessage("§7Make sure the Aether Casket stack size is 1");
            return;
        }

        // Create a new transfer task
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if the player is online - if not, stop the transfer
                if (!player.isOnline()) {
                    stopTransmission(player);
//                    MagicExpansion.getInstance().getLogger().info("Player " + player.getName() + " is offline, stop the transfer automatically");
                    return;
                }

                // Check the transfer state
                if (!playerTransmissionStatus.getOrDefault(playerId, false)) {
                    return;
                }

                // Do the transfer
                performTransmission(player);
            }
        };

        // Runs once every second (20 tick = 1 sec)
        task.runTaskTimer(MagicExpansion.getInstance(), 0L, 1L);
        activeTransmissions.put(playerId, task);
        playerTransmissionStatus.put(playerId, true);

        player.sendMessage("§a✓ Transfer started!");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
    }

    /**
     * Stop transfer
     */
    private void stopTransmission(Player player) {
        UUID playerId = player.getUniqueId();

        if (activeTransmissions.containsKey(playerId)) {
            activeTransmissions.get(playerId).cancel();
            activeTransmissions.remove(playerId);
        }

        playerTransmissionStatus.put(playerId, false);
    }

    /**
     * Run the transfer logic
     */
    private void performTransmission(Player player) {

        // Find the first Aether Casket in the inventory
        ItemStack cargoFragment = findFirstValidCargoFragment(player);
        if (cargoFragment == null) {
            // No valid Aether Casket, don't stop the transfer (wait for the player to put in a new Aether Casket)
            return;
        }

        // Get the item and amount in the Aether Casket
        ItemStack storedItem = getStoredItemFromFragment(cargoFragment);
        int storedAmount = getStoredAmountFromFragment(cargoFragment);

        if (storedItem == null || storedAmount <= 0) {
            // Empty Aether Casket, remove it
            removeCargoFragment(player, cargoFragment);
            player.sendMessage("§cFound an empty Aether Casket, removed!");
            return;
        }

        // Work out the remaining inventory space
        int availableSpace = calculateAvailableSpace(player, storedItem);
        if (availableSpace <= 0) {
            // Inventory is full, skip this transfer
            return;
        }

        // Calculate the amount that can actually be distributed
        int amountToGive = Math.min(storedAmount, availableSpace);

        // Fill the player's inventory
        int actuallyGiven = fillPlayerInventory(player, storedItem, amountToGive);

        if (actuallyGiven > 0) {
            int newAmount = storedAmount - actuallyGiven;

            if (newAmount <= 0) {
                // Amount is 0, delete this Aether Casket
                removeCargoFragment(player, cargoFragment);
                player.sendMessage("§a✓ Transfer finished: " + getItemDisplayName(storedItem));
            } else {
                // Update the Aether Casket amount
                updateFragmentAmount(cargoFragment, newAmount);
                player.sendMessage("§a✓ Transferring: " + getItemDisplayName(storedItem) + " §7(Remaining: " + newAmount + ")");
            }
        }
    }

    /**
     * Check if there's a valid Aether Casket in the inventory
     */
    private boolean hasValidCargoFragment(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isCargoFragment(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find the first Aether Casket in the inventory
     */
    private ItemStack findFirstValidCargoFragment(Player player) {
        ItemStack[] inventory = player.getInventory().getContents();

        for (int i = 0; i < inventory.length; i++) {
            ItemStack item = inventory[i];
            if (isCargoFragment(item)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Remove the Aether Casket
     */
    private void removeCargoFragment(Player player, ItemStack cargoFragment) {
        ItemStack[] inventory = player.getInventory().getContents();

        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null && inventory[i].equals(cargoFragment)) {
                player.getInventory().setItem(i, null);
                break;
            }
        }
    }

    /**
     * Check if it's an Aether Casket
     */
    private boolean isCargoFragment(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;

        SlimefunItem sfItem = SlimefunItem.getByItem(item);
        if (!(sfItem instanceof CargoFragment)) return false;

        // Check if the stack amount is 1
        if (item.getAmount() != 1) return false;

        // Check if there are stored items
        ItemStack storedItem = getStoredItemFromFragment(item);
        int storedAmount = getStoredAmountFromFragment(item);

        return storedItem != null && storedAmount > 0;
    }

    /**
     * Get the stored item from the Aether Casket
     */
    private ItemStack getStoredItemFromFragment(ItemStack fragment) {
        if (fragment == null || !fragment.hasItemMeta()) return null;

        ItemMeta meta = fragment.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey keyItem = new NamespacedKey(MagicExpansion.getInstance(), "cargo_item_json");
        String json = container.get(keyItem, PersistentDataType.STRING);
        if (json == null) return null;

        return itemFromBase64(json);
    }

    /**
     * Get the stored amount from the Aether Casket
     */
    private int getStoredAmountFromFragment(ItemStack fragment) {
        if (fragment == null || !fragment.hasItemMeta()) return 0;

        ItemMeta meta = fragment.getItemMeta();
        if (meta == null) return 0;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey keyAmount = new NamespacedKey(MagicExpansion.getInstance(), "cargo_amount");
        Integer amount = container.get(keyAmount, PersistentDataType.INTEGER);
        return amount != null ? amount : 0;
    }

    /**
     * Update the Aether Casket's amount
     */
    private void updateFragmentAmount(ItemStack fragment, int newAmount) {
        if (fragment == null || !fragment.hasItemMeta()) return;

        ItemMeta meta = fragment.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey keyAmount = new NamespacedKey(MagicExpansion.getInstance(), "cargo_amount");
        container.set(keyAmount, PersistentDataType.INTEGER, newAmount);

        // Update the amount shown in Lore
        if (meta.hasLore()) {
            java.util.List<String> lore = meta.getLore();
            for (int i = 0; i < lore.size(); i++) {
                if (lore.get(i).startsWith("§fAmount: §a")) {
                    lore.set(i, "§fAmount: §a" + newAmount);
                    break;
                }
            }
            meta.setLore(lore);
        }

        fragment.setItemMeta(meta);
    }

    /**
     * Calculate the player's remaining inventory space
     */
    private int calculateAvailableSpace(Player player, ItemStack item) {
        int totalSpace = 0;

        for (ItemStack inventoryItem : player.getInventory().getStorageContents()) {
            if (inventoryItem == null || inventoryItem.getType() == Material.AIR) {
                totalSpace += item.getMaxStackSize();
            } else if (SlimefunUtils.isItemSimilar(inventoryItem, item, true, false)) {
                int space = inventoryItem.getMaxStackSize() - inventoryItem.getAmount();
                if (space > 0) {
                    totalSpace += space;
                }
            }
        }

        return totalSpace;
    }

    /**
     * Fill the player's inventory
     */
    private int fillPlayerInventory(Player player, ItemStack item, int maxAmount) {
        int totalGiven = 0;
        ItemStack singleItem = item.clone();
        singleItem.setAmount(1);

        // Fix (V): always use getStorageContents() the main storage area (36 slots) for the fill loop and space calculation,
        // Originally getSize()is 41 slots (includes armor slots/off-hand index), so items could get written into the armor slots
        ItemStack[] storage = player.getInventory().getStorageContents();

        // Fill the slots that already have the item first
        for (int i = 0; i < storage.length; i++) {
            if (totalGiven >= maxAmount) break;

            ItemStack slotItem = storage[i];
            if (slotItem != null && !slotItem.getType().isAir() &&
                    SlimefunUtils.isItemSimilar(slotItem, singleItem, true) &&
                    slotItem.getAmount() < slotItem.getMaxStackSize()) {

                int space = slotItem.getMaxStackSize() - slotItem.getAmount();
                int toAdd = Math.min(space, maxAmount - totalGiven);

                slotItem.setAmount(slotItem.getAmount() + toAdd);
                player.getInventory().setItem(i, slotItem); // Fix (V): explicitly write back to the main storage area slot
                totalGiven += toAdd;
            }
        }

        // Then fill empty slots
        for (int i = 0; i < storage.length; i++) {
            if (totalGiven >= maxAmount) break;

            ItemStack slotItem = storage[i];
            if (slotItem == null || slotItem.getType().isAir()) {
                int toAdd = Math.min(item.getMaxStackSize(), maxAmount - totalGiven);

                ItemStack newStack = item.clone();
                newStack.setAmount(toAdd);
                player.getInventory().setItem(i, newStack);

                totalGiven += toAdd;
            }
        }

        return totalGiven;
    }

    /**
     * Get item display name
     */
    private String getItemDisplayName(ItemStack item) {
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item != null ? item.getType().name().toLowerCase().replace('_', ' ') : "Unknown item";
    }

    /**
     * Start cleanup task - clean offline player data
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Clean up the data of offline players
                playerTransmissionStatus.keySet().removeIf(uuid -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null) {
                        // Player is offline, stop the transfer task
                        BukkitRunnable task = activeTransmissions.get(uuid);
                        if (task != null) {
                            task.cancel();
                        }
                        activeTransmissions.remove(uuid);
                        lastCheckTime.remove(uuid);
                        return true;
                    }
                    return false;
                });
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 6000L, 6000L); // Clean up every 5 minutes
    }

    /**
     * Event listener: stop transfer when the player quits
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        stopTransmission(player);
//        MagicExpansion.getInstance().getLogger().info("Player " + player.getName() + " Left the game, stop the transfer");
    }

    /**
     * Restore the item from Base64
     */
    private ItemStack itemFromBase64(String data) {
        try {
            return io.Yomicer.magicExpansion.utils.SameItemJudge.itemFromBase64(data);
        } catch (Exception e) {
            MagicExpansion.getInstance().getLogger().warning("Item deserialization failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Clean up all tasks when the server shuts down
     */
    public static void onPluginDisable() {
        // Stop all transfer tasks
        for (BukkitRunnable task : activeTransmissions.values()) {
            task.cancel();
        }
        activeTransmissions.clear();
        playerTransmissionStatus.clear();
        lastCheckTime.clear();
    }
}
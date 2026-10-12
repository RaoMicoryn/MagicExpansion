package io.Yomicer.magicExpansion.items.misc;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.items.misc.CargoFragment;
import io.Yomicer.magicExpansion.utils.log.Debug;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;

public class CargoFragmentDistributor extends SlimefunItem implements EnergyNetComponent {

    private final int inputSlot = 10; // CargoFragment Input slot
    private final int playerHeadSlot = 16; // Player head setting slot
    private final int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 12, 13, 14, 15, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};
    private final int[] infoSlots = {27, 28, 29, 30, 31, 32, 33, 34, 35}; // Info display slot

    // Store each machine's state
    public static final Map<Location, MachineState> machineStates = new HashMap<>();
    private static final int SLOW_CHECK_INTERVAL = 100; // Slow check:60 tick = 5 sec
    private static final int FAST_CHECK_INTERVAL = 1;   // Fast check:1 tick = 0.1sec
    public static BukkitRunnable globalTickTask;


    // Head skin cache
    private static final Map<String, ItemStack> playerHeadCache = new HashMap<>();
    private static final Map<String, Long> cacheTimestamps = new HashMap<>();
    private static final long CACHE_DURATION = 10 * 60 * 1000; // 10minute cache


    public CargoFragmentDistributor(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);

        constructMenu("Aether Casket Distributor");
        addItemHandler(onBlockPlace(), onBlockBreak());

        // Start the global tick task
        startGlobalTickTask();
    }

    /**
     * Machine state class
     */
    private static class MachineState {
        int tickCount = 0;
        String targetPlayerName = "";
        boolean hasValidFragment = false;
        boolean isActive = false;
        int currentCheckInterval = SLOW_CHECK_INTERVAL;

        void updateState(boolean hasFragment, boolean playerValid) {
            boolean shouldBeActive = hasFragment && playerValid;

            if (shouldBeActive != isActive) {
                isActive = shouldBeActive;
                currentCheckInterval = shouldBeActive ? FAST_CHECK_INTERVAL : SLOW_CHECK_INTERVAL;
                tickCount = 0; // Reset counter
            }
        }

        boolean shouldProcess() {
            tickCount++;
            if (tickCount >= currentCheckInterval) {
                tickCount = 0;
                return true;
            }
            return false;
        }
    }

    /**
     * Start the global tick task
     */
    private void startGlobalTickTask() {
        if (globalTickTask != null && !globalTickTask.isCancelled()) {
            return;
        }

        globalTickTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Go through all registered machines
                for (Map.Entry<Location, MachineState> entry : new HashMap<>(machineStates).entrySet()) {
                    Location location = entry.getKey();
                    MachineState state = entry.getValue();

                    // Check if the block still exists
                    if (!isBlockValid(location)) {
                        machineStates.remove(location);
                        continue;
                    }

                    // Check if it should be handled
                    if (state.shouldProcess()) {
                        processMachine(location, state);
                    }
                }
            }
        };
        globalTickTask.runTaskTimer(MagicExpansion.getInstance(), 1L, 1L);
    }

    /**
     * Check if the block is valid
     */
    private boolean isBlockValid(Location location) {
        if (!location.getChunk().isLoaded()) return false;

        Block block = location.getBlock();
        if (block.getType() == Material.AIR) return false;

        SlimefunItem sfItem = BlockStorage.check(location);
        return sfItem instanceof CargoFragmentDistributor;
    }

    /**
     * Handle machine logic
     */
    private void processMachine(Location location, MachineState state) {
        BlockMenu menu = StorageCacheUtils.getMenu(location);
        if (menu == null) return;

        // Check player settings
        String playerName = getPlayerNameFromStorage(location);
        boolean playerValid = isPlayerValid(playerName);

        // Check CargoFragment
        ItemStack fragment = menu.getItemInSlot(inputSlot);
        boolean hasFragment = isCargoFragmentValid(fragment);

        // Update machine state
        state.updateState(hasFragment, playerValid);

        // Update display info
        updateDisplayInfo(menu, state, playerName, hasFragment);

        // Use a safe method to update the player head display
        updatePlayerHeadSafely(menu, state, playerName);

        // If the condition is met, try to distribute
        if (state.isActive && hasFragment && playerValid) {
            distributeFragment(fragment, playerName, menu, location);
        }
    }

    /**
     * Distribute items in CargoFragment - fill the inventory as much as possible
     */
    private void distributeFragment(ItemStack fragment, String playerName, BlockMenu menu, Location machineLoc) {
        Player targetPlayer = Bukkit.getPlayerExact(playerName);
        if (targetPlayer == null) return;

        // Get the stored item and amount from CargoFragment
        ItemStack storedItem = getStoredItemFromFragment(fragment);
        int storedAmount = getStoredAmountFromFragment(fragment);

        if (storedItem == null || storedAmount <= 0) {
            // Invalid CargoFragment, delete it
            menu.consumeItem(inputSlot, 1);
            return;
        }

        // Calculate how many items the remaining inventory space can hold
        int availableSpace = calculateAvailableSpace(targetPlayer, storedItem);
        if (availableSpace <= 0) {
            // Inventory is full
            return;
        }

        // Calculate the amount that can actually be distributed
        int amountToGive = Math.min(storedAmount, availableSpace);

        // Try to fill the inventory
        int actuallyGiven = fillPlayerInventory(targetPlayer, storedItem, amountToGive);

        if (actuallyGiven > 0) {
            // Update the amount in CargoFragment
            int newAmount = storedAmount - actuallyGiven;
            if (newAmount <= 0) {
                // Amount is 0, delete CargoFragment
                menu.consumeItem(inputSlot, 1);
                targetPlayer.sendMessage("§a✓ Received all items: " + getItemDisplayName(storedItem));
            } else {
                // Update the CargoFragment amount
                updateFragmentAmount(fragment, newAmount);
                targetPlayer.sendMessage("§a✓ Received items: " + getItemDisplayName(storedItem) + " §7(Remaining: " + newAmount + ")");
            }
        }
    }

    /**
     * Calculate the player's remaining inventory space
     */
    private int calculateAvailableSpace(Player player, ItemStack item) {
        int totalSpace = 0;

        // Check all inventory slots
        for (ItemStack inventoryItem : player.getInventory().getStorageContents()) {
            if (inventoryItem == null || inventoryItem.getType() == Material.AIR) {
                // Empty slot, can hold a whole stack
                totalSpace += item.getMaxStackSize();
            } else if (SlimefunUtils.isItemSimilar(inventoryItem, item, true, false)) {
                // Same item, work out the remaining space
                int space = inventoryItem.getMaxStackSize() - inventoryItem.getAmount();
                if (space > 0) {
                    totalSpace += space;
                }
            }
            // Slots with different items can't stack, so they don't count as free space
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

        // Fill slots that already have the item first (same items can stack)
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (totalGiven >= maxAmount) break;

            ItemStack slotItem = player.getInventory().getItem(i);
            if (slotItem != null && !slotItem.getType().isAir() &&
                    SlimefunUtils.isItemSimilar(slotItem, singleItem, true) &&
                    slotItem.getAmount() < slotItem.getMaxStackSize()) {

                int space = slotItem.getMaxStackSize() - slotItem.getAmount();
                int toAdd = Math.min(space, maxAmount - totalGiven);

                slotItem.setAmount(slotItem.getAmount() + toAdd);
                totalGiven += toAdd;
            }
        }

        // Then fill empty slots
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (totalGiven >= maxAmount) break;

            ItemStack slotItem = player.getInventory().getItem(i);
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
     * Check if the player is valid
     */
    private boolean isPlayerValid(String playerName) {
        if (playerName == null || playerName.isEmpty()) return false;

        Player player = Bukkit.getPlayerExact(playerName);
        return player != null && player.isOnline();
    }

    /**
     * Check if the CargoFragment is valid
     */
    private boolean isCargoFragmentValid(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;

        SlimefunItem sfItem = SlimefunItem.getByItem(item);
        if (!(sfItem instanceof CargoFragment)) return false;
        // Check if the item stack amount is1
        if (item.getAmount() != 1) return false;
        // Check if there are stored items
        ItemStack storedItem = getStoredItemFromFragment(item);
        int storedAmount = getStoredAmountFromFragment(item);

        return storedItem != null && storedAmount > 0;
    }

    /**
     * Get the stored item from CargoFragment
     */
    private ItemStack getStoredItemFromFragment(ItemStack fragment) {
        ItemMeta meta = fragment.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey keyItem = new NamespacedKey(MagicExpansion.getInstance(), "cargo_item_json");
        String json = container.get(keyItem, PersistentDataType.STRING);
        if (json == null) return null;

        return itemFromBase64(json);
    }

    /**
     * Get the stored amount from CargoFragment
     */
    private int getStoredAmountFromFragment(ItemStack fragment) {
        ItemMeta meta = fragment.getItemMeta();
        if (meta == null) return 0;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey keyAmount = new NamespacedKey(MagicExpansion.getInstance(), "cargo_amount");
        Integer amount = container.get(keyAmount, PersistentDataType.INTEGER);
        return amount == null ? 0 : amount;
    }

    /**
     * Update the CargoFragment amount
     */
    private void updateFragmentAmount(ItemStack fragment, int newAmount) {
        ItemMeta meta = fragment.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey keyAmount = new NamespacedKey(MagicExpansion.getInstance(), "cargo_amount");
        container.set(keyAmount, PersistentDataType.INTEGER, newAmount);

        // Update Lore display
        List<String> lore = meta.getLore();
        if (lore != null) {
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
     * Get the player name from storage
     */
    private String getPlayerNameFromStorage(Location location) {
        SlimefunBlockData data = StorageCacheUtils.getBlock(location);
        if (data == null) return null;

        return data.getData("target_player");
    }
    /*
    Optimize player head fetching
     */
    private ItemStack createPlayerHeadWithCache(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return createPlayerHeadDisplay(null);
        }

        // Check if the cache is valid
        long currentTime = System.currentTimeMillis();
        if (playerHeadCache.containsKey(playerName)) {
            Long timestamp = cacheTimestamps.get(playerName);
            if (timestamp != null && (currentTime - timestamp) < CACHE_DURATION) {
                // Return a copy of the cache
                return playerHeadCache.get(playerName).clone();
            } else {
                // Cache expired, remove
                playerHeadCache.remove(playerName);
                cacheTimestamps.remove(playerName);
            }
        }

        // Create a new head
        ItemStack head = createPlayerHeadDisplay(playerName);

        // Cache the newly created head
        if (head != null) {
            playerHeadCache.put(playerName, head.clone());
            cacheTimestamps.put(playerName, currentTime);
        }

        return head;
    }
    private void updatePlayerHeadSafely(BlockMenu menu, MachineState state, String playerName) {
        ItemStack currentHead = menu.getItemInSlot(playerHeadSlot);

        // Decide which player name to show
        String expectedPlayerName = (playerName != null && !playerName.isEmpty()) ? playerName : null;

        // Check if it needs updating
        boolean needsUpdate = false;

        if (currentHead == null || currentHead.getType() != Material.PLAYER_HEAD) {
            // It's not a player head right now, needs updating
            needsUpdate = true;
        } else if (currentHead.hasItemMeta()) {
            ItemMeta meta = currentHead.getItemMeta();
            String currentDisplayName = meta.getDisplayName();

            // Work out the expected display name from the player name
            String expectedDisplayName = expectedPlayerName != null ?
                    "§aTarget player: " + expectedPlayerName : "§aClick to set target player";

            // If the display name doesn't match, it needs updating
            if (!currentDisplayName.equals(expectedDisplayName)) {
                needsUpdate = true;
            }

            // Check if Lore matches
            if (!needsUpdate && meta.hasLore()) {
                List<String> lore = meta.getLore();
                boolean hasPlayerSet = expectedPlayerName != null;
                boolean loreMatches = lore.size() >= 1 &&
                        lore.get(0).equals(hasPlayerSet ? "§7Target player set" : "§7No player set right now");

                if (!loreMatches) {
                    needsUpdate = true;
                }
            }
        } else {
            // No metadata, needs updating
            needsUpdate = true;
        }

        // If it needs updating, use the cached method to create a new head
        if (needsUpdate) {
            ItemStack newHead = createPlayerHeadWithCache(expectedPlayerName);
            menu.replaceExistingItem(playerHeadSlot, newHead);
        }
    }

    /**
     * Save the player name to storage
     */
    private void savePlayerNameToStorage(Location location, String playerName) {
        if (location == null) return;

        SlimefunBlockData data = StorageCacheUtils.getBlock(location);
        if (data == null) return;

        if (playerName == null) {
            data.removeData("target_player");
        } else {
            data.setData("target_player", playerName);
        }
    }

    /**
     * Update display info
     */
    private void updateDisplayInfo(BlockMenu menu, MachineState state, String playerName, boolean hasFragment) {
        String status;
        String details;
        Material displayMaterial;

        if (state.isActive) {
            status = "§a§lRunning - fast mode";
            details = "§7Check and distribute every tick";
            displayMaterial = Material.LIME_STAINED_GLASS_PANE;
        } else if (hasFragment && playerName != null && !playerName.isEmpty()) {
            status = "§cWaiting for the player to come online";
            details = "§7Player: " + playerName;
            displayMaterial = Material.YELLOW_STAINED_GLASS_PANE;
        } else if (hasFragment) {
            status = "§cNo player set";
            details = "§7Please click the player slot to set";
            displayMaterial = Material.ORANGE_STAINED_GLASS_PANE;
        } else if (playerName != null && !playerName.isEmpty()) {
            status = "§cWaiting for an Aether Casket";
            details = "§7Please put in an Aether Casket";
            displayMaterial = Material.ORANGE_STAINED_GLASS_PANE;
        } else {
            status = "§7Idle - slow mode";
            details = "§7Check once every 5 seconds";
            displayMaterial = Material.GRAY_STAINED_GLASS_PANE;
        }

        // Update info display slot
        ItemStack infoItem = new CustomItemStack(
                displayMaterial,
                "§6Aether Casket Distributor",
                "",
                "§fStatus: " + status,
                "§f" + details,
                "",
                "§eInput slot: §aPut into Aether Casket",
                "§ePlayer slot: §aClick to set player",
                "",
                "§7Fast mode: Distribute every tick",
                "§7Slow mode: Check every 5 seconds"
        );

        for (int slot : infoSlots) {
            menu.replaceExistingItem(slot, infoItem);
        }
    }

    /**
     * Get item display name
     */
    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name().toLowerCase().replace('_', ' ');
    }

    @Nonnull
    private BlockPlaceHandler onBlockPlace() {
        return new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                Location loc = e.getBlock().getLocation();
                machineStates.put(loc, new MachineState());
                // If the machine amount went up a lot, check the cache size
                if (machineStates.size() % 10 == 0) { // Check once every 10 machines
                    cleanExpiredCache();
                }
            }
        };
    }

    @Nonnull
    protected BlockBreakHandler onBlockBreak() {
        return new SimpleBlockBreakHandler() {
            @Override
            public void onBlockBreak(Block b) {
                BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());
                if (inv != null) {
                    inv.dropItems(b.getLocation(), new int[]{inputSlot});
                }
                // If the machine amount went up a lot, check the cache size
                if (machineStates.size() % 10 == 0) { // Check once every 10 machines
                    cleanExpiredCache();
                }
                // Remove from the state map
                machineStates.remove(b.getLocation());
            }
        };
    }

    private void constructMenu(String displayName) {
        new BlockMenuPreset(getId(), displayName) {
            @Override
            public void init() {
                constructMenu(this);
            }

            @Override
            public boolean canOpen(@Nonnull Block b, @Nonnull Player p) {
                return p.hasPermission("slimefun.inventory.bypass")
                        || Slimefun.getProtectionManager().hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow itemTransportFlow) {
                if (itemTransportFlow == ItemTransportFlow.INSERT) {
                    return new int[]{10};
                } else {
                    return new int[]{10};
                }
            }

        };
    }

    protected void constructMenu(BlockMenuPreset preset) {
        // Set border
        ItemStack borderItem = new CustomItemStack(Material.PURPLE_STAINED_GLASS_PANE, "§5Aether Casket Distributor");
        for (int slot : borderSlots) {
            preset.addItem(slot, borderItem, (p, slot1, item, action) -> false);
        }

//        // Set input slot - empty slot, items can be placed directly
//        preset.addItem(inputSlot, null, (p, slot, item, action) -> {
//            // Allow placing any item
//            return true;
//        });

        // Set player head slot - click to set player
        ItemStack playerHeadItem = createPlayerHeadDisplay(null);

        preset.addItem(playerHeadSlot, playerHeadItem, (p, slot, item, action) -> {
            // Use the block location where the player opened the menu
            Block b = p.getTargetBlockExact(5);
            if (b == null) return false;

            Location location = b.getLocation();

            // Get BlockMenu instance
            BlockMenu menu = BlockStorage.getInventory(b);
            if (menu == null) return false;

            if (action.isRightClicked()) {
                // Right-click to clear player setting
                savePlayerNameToStorage(location, null);
                p.sendMessage("§aTarget player setting cleared");

                // Update display - using BlockMenu update
                menu.replaceExistingItem(playerHeadSlot, createPlayerHeadDisplay(null));
            } else {
                // Left-click: set current player
                savePlayerNameToStorage(location, p.getName());
                p.sendMessage("§aTarget player set: " + p.getName());

                // Update display - using BlockMenu update
                menu.replaceExistingItem(playerHeadSlot, createPlayerHeadDisplay(p.getName()));
            }
            return false;
        });

        // Initialize info display
        ItemStack initialInfo = new CustomItemStack(
                Material.GRAY_STAINED_GLASS_PANE,
                "§6Aether Casket Distributor",
                "",
                "§fStatus: §7Idle - slow mode",
                "§fCheck once every 5 seconds",
                "",
                "§eInput slot: §aPut into Aether Casket",
                "§ePlayer slot: §aClick to set player",
                "",
                "§7Fast mode: Distribute every tick",
                "§7Slow mode: Check every 5 seconds"
        );

        for (int slot : infoSlots) {
            preset.addItem(slot, initialInfo, (p, slot1, item, action) -> false);
        }
    }

    /**
     * Create player head display item - fixed version, avoids 429 errors
     */
    private ItemStack createPlayerHeadDisplay(String playerName) {
        ItemStack headItem;

        if (playerName != null && !playerName.isEmpty()) {
            // Create player head
            headItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) headItem.getItemMeta();

            if (skullMeta != null) {
                // Safely set the player skin
                try {
                    Player targetPlayer = Bukkit.getPlayerExact(playerName);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        // Use the online player's skin
                        skullMeta.setOwningPlayer(targetPlayer);
                    } else {
                        // For offline players, use the default skin to avoid API calls
                        // You can use a known default player here
                        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer("Steve"));
                    }
                } catch (Exception e) {
                    // If an error happens, log it but don't stop the program
                    MagicExpansion.getInstance().getLogger().warning("Error creating player head: " + e.getMessage());
                }

                skullMeta.setDisplayName("§aTarget player: " + playerName);
                skullMeta.setLore(Arrays.asList(
                        "§7Target player set",
                        "",
                        "§eLeft click: Set yourself as the target",
                        "§eRight click: Clear target setting"
                ));

                headItem.setItemMeta(skullMeta);
            }
        } else {
            // No player set
            headItem = new CustomItemStack(Material.PLAYER_HEAD, "§aClick to set target player",
                    "§7No player set right now",
                    "",
                    "§eLeft click: Set yourself as the target",
                    "§eRight click: Clear target setting");
        }

        return headItem;
    }

    /**
     * Clean up expired cache
     */
    public static void cleanExpiredCache() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = cacheTimestamps.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (currentTime - entry.getValue() > CACHE_DURATION) {
                playerHeadCache.remove(entry.getKey());
                iterator.remove();
            }
        }
    }

    /**
     * Update player head display
     */
    private void updatePlayerHeadDisplay(BlockMenuPreset preset, Location location, String playerName) {
        ItemStack headItem;

        if (playerName != null && !playerName.isEmpty()) {
            // Create player head
            headItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) headItem.getItemMeta();

            // Set player skin
            Player targetPlayer = Bukkit.getPlayerExact(playerName);
            if (targetPlayer != null) {
                skullMeta.setOwningPlayer(targetPlayer);
            }

            skullMeta.setDisplayName("§aTarget player: " + playerName);
            skullMeta.setLore(List.of(
                    "§7Target player set",
                    "",
                    "§eLeft click: Set yourself as the target",
                    "§eRight click: Clear target setting"
            ));

            headItem.setItemMeta(skullMeta);
        } else {
            // No player set
            headItem = new CustomItemStack(Material.PLAYER_HEAD, "§aClick to set target player",
                    "§7No player set right now",
                    "",
                    "§eLeft click: Set yourself as the target",
                    "§eRight click: Clear target setting");
        }

        preset.addItem(playerHeadSlot, headItem, (p, slot, item, action) -> {
            // Use the block location where the player opened the menu
            Block b = p.getTargetBlockExact(5);
            if (b == null) return false;

            Location loc = b.getLocation();

            if (action.isRightClicked()) {
                // Right-click to clear player setting
                savePlayerNameToStorage(loc, null);
                p.sendMessage("§aTarget player setting cleared");

                // Update display
                updatePlayerHeadDisplay(preset, loc, null);
            } else {
                // Left-click: set current player
                savePlayerNameToStorage(loc, p.getName());
                p.sendMessage("§aTarget player set: " + p.getName());

                // Update display
                updatePlayerHeadDisplay(preset, loc, p.getName());
            }
            return false;
        });
    }

    @Override
    public @NotNull EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.NONE;
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    // Method to restore an item from Base64
    private ItemStack itemFromBase64(String data) {
        try {
            // Use the existing itemFromBase64 method in your project
            return io.Yomicer.magicExpansion.utils.SameItemJudge.itemFromBase64(data);
        } catch (Exception e) {
            return null;
        }
    }

}
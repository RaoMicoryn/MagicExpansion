package io.Yomicer.magicExpansion.items.misc;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.items.tools.VoidTouch;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.Yomicer.magicExpansion.utils.SameItemJudge;
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
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;

import static io.Yomicer.magicExpansion.core.MagicExpansionItems.CARGO_FRAGMENT;
import static io.Yomicer.magicExpansion.utils.SameItemJudge.itemFromBase64;
import static io.Yomicer.magicExpansion.utils.SameItemJudge.itemToBase64;
import static io.Yomicer.magicExpansion.utils.Utils.doGlow;

public class CargoCoreMore extends SlimefunItem implements EnergyNetComponent{


    //    private final int[] pinkBorder = {51,52};
    private final int[] blueBorder = {46,47};
    private final int[] inputSlots = {0,1,2,3,4 ,9,10,11,12,13 ,18,19,20,21,22};
    private final int[] outputSlots = {6,7,8,  15,16,17,  24,25,26};
    private final int[] inputOutputLine = {5,14,23};
    private final int[] arrowSlot = {45,48,50,51,52,53};
    private final int[] storageSlots = {28, 29, 30, 31, 32, 33, 34, 35}; // Show stored item
    private final int[] transportSlots = {36, 37, 38, 39}; // Quick craft slot (not implemented yet), used for output
    private final int[] transportSlots2 = { 41, 42, 43, 44}; // Quick craft slot (not implemented yet), used for input





    // Pagination settings
    private static final int ITEMS_PER_PAGE = 8;
    private static final int MAX_STORED_ITEMS = 1145; // Supports up to 18 different items
    private static final String OUTPUT_TARGET_KEY = "output_target_slot";

    public CargoCoreMore(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);

        constructMenu("Magic Storage · Reworked");
        addItemHandler(onBlockPlace(), onBlockBreak());
    }



    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem sf, SlimefunBlockData data) {
                CargoCoreMore.this.tick(b);
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }
        });
    }

    @Nonnull
    private BlockPlaceHandler onBlockPlace() {
        return new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {



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
                    inv.dropItems(b.getLocation(), getInputSlots());
                    inv.dropItems(b.getLocation(), getOutputSlots());
                }



                Location loc = b.getLocation();
                SlimefunBlockData data = StorageCacheUtils.getBlock(loc);
                if (data == null) return;

                // Get the player who broke the block
                Player player = getNearestPlayer(loc);

                // === Start handling each storage slot ===
                for (int i = 0; i < MAX_STORED_ITEMS; i++) {
                    String jsonData = data.getData("item_type_" + i);
                    String countStr = data.getData("item_count_" + i);

                    if (jsonData == null || countStr == null) continue;

                    ItemStack prototype = itemFromBase64(jsonData);
                    if (prototype == null) continue;

                    long amount;
                    try {
                        amount = Long.parseLong(countStr);
                    } catch (Exception e) {
                        continue;
                    }

                    if (amount <= 0) continue;


                    // === Calculate how many fragments to drop and the amount in each fragment ===
                    long remainingAmount = amount;
                    int fragmentCount = 0;
                    long lostAmount = 0;

                    // Drop at most 128 fragments
                    while (remainingAmount > 0 && fragmentCount < 2048) {
                        // Each fragment holds up to Integer.MAX_VALUE items
                        int amountInFragment = (int) Math.min(remainingAmount, Integer.MAX_VALUE);

                        ItemStack fragment = createCargoFragment(prototype, amountInFragment);
                        if (fragment != null) {
                            b.getWorld().dropItemNaturally(loc, fragment);
                            fragmentCount++;
                        }

                        remainingAmount -= amountInFragment;
                    }

                    // If there are leftover items that can't be dropped (because of the fragment amount limit)
                    if (remainingAmount > 0) {
                        lostAmount = remainingAmount;
                    }

                    // === If items got lost, tell the player ===
                    if (lostAmount > 0 && player != null) {
                        String itemName = ItemStackHelper.getDisplayName( prototype);
                        player.sendMessage(ChatColor.RED + "Warning: Lost, because of the fragment amount limit " +
                                lostAmount + " x " + itemName);
                        player.sendMessage("Each item type can drop at most 2048 Aether Caskets holding 2.14B items");
                        player.sendMessage("Max drop amount per item is4,398,046,509,056");
                    }


//                    // === Create CargoFragment ===
//                    ItemStack fragment = createCargoFragment(prototype, (int) Math.min(amount, Integer.MAX_VALUE));
//                    if (fragment != null) {
//                        b.getWorld().dropItemNaturally(loc, fragment);
//                    }
                }

            }
        };
    }

    // Helper method: get the nearest player (for sending messages)
    private Player getNearestPlayer(Location location) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : location.getWorld().getPlayers()) {
            double distance = player.getLocation().distanceSquared(location);
            if (distance < nearestDistance && distance < 100) { // 10players within the block range
                nearestDistance = distance;
                nearest = player;
            }
        }

        return nearest;
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

    public int[] getInputSlots() {
        return inputSlots;
    }

    public int[] getOutputSlots() {
        return outputSlots;
    }


    private void cleanupOutputState(@Nonnull SlimefunBlockData data, int dataSlot) {
        data.setData("output_target_slot", "-1");
    }

    /**
     * Handle auto output logic
     * Take items out of internal inventory and try to put them in the first 9 slots (0-8)
     */
    private void handleOutput(@Nonnull BlockMenu menu, @Nonnull SlimefunBlockData data) {
        // Get output target data slot
        String targetStr = data.getData("output_target_slot");
        if (targetStr == null || "-1".equals(targetStr)) {
            return;
        }

        int targetSlot;
        try {
            targetSlot = Integer.parseInt(targetStr);
        } catch (NumberFormatException e) {
            data.setData("output_target_slot", "-1");
            return;
        }

        // Get item prototype
        String json = data.getData("item_type_" + targetSlot);
        if (json == null || json.isEmpty()) {
            // If the item data doesn't exist, clear the output state
            cleanupOutputState(data, targetSlot);
            return;
        }

        ItemStack prototype = itemFromBase64(json);
        if (prototype == null || prototype.getType().isAir()) {
            // If the item prototype is invalid, clear the output state
            cleanupOutputState(data, targetSlot);
            return;
        }

        // Get stock amount
        long itemCount = 0;
        try {
            String countStr = data.getData("item_count_" + targetSlot);
            if (countStr != null && !countStr.isEmpty()) {
                itemCount = Long.parseLong(countStr);
            }
        } catch (NumberFormatException ignored) {}

        // Check if there's a max amount limit
        String maxStr = data.getData("item_max_" + targetSlot);
        long maxCount = -1;
        if (maxStr != null && !maxStr.isEmpty()) {
            try {
                maxCount = Long.parseLong(maxStr);
            } catch (Exception ignored) {}
        }

//        // If the amount is 0 and no max limit is set, stop output
//        if (itemCount <= 0 && maxCount == -1) {
//            cleanupOutputState(data, targetSlot);
//            return;
//        }

        // If the amount is 0 but a max limit is set, keep the output state but don't output
        if (itemCount <= 0) {
            cleanupOutputState(data, targetSlot);
            return;
        }

        // Run output (up to 9 slots, index 0~8)
        int[] outputSlots = getOutputSlots();
        int batchSize = 64*9; // e.g. 64, 512...

        int successfullyOutput = tryOutputItems(menu, prototype, itemCount, outputSlots, batchSize);

        if (successfullyOutput > 0) {
            // Update stock
            itemCount -= successfullyOutput;
            data.setData("item_count_" + targetSlot, String.valueOf(itemCount));

            // If the stock is 0 after output, stop output automatically
            if (itemCount <= 0) {
                cleanupOutputState(data, targetSlot);
            }

            // Update UI
            updateStorageDisplay(menu, data);

            // Sound
            Block block = menu.getLocation().getBlock();
//        block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2F, 1.2F);
        }
    }

    /**
     * Try to output the item to the given slots (safe segmented version)
     * Push up to 64 at a time to avoid errors when pushItem handles big amounts
     *
     * @param menu BlockMenu
     * @param prototype Item prototype (without amount)
     * @param availableCount Available amount in stock
     * @param outputSlots Array of available slots (e.g. 0~8)
     * @param maxPerTick Max output amount per tick (rate limit)
     * @return Amount actually output successfully
     */
    private int tryOutputItems(@Nonnull BlockMenu menu,
                               @Nonnull ItemStack prototype,
                               long availableCount,
                               int[] outputSlots,
                               int maxPerTick) {
        if (availableCount <= 0 || maxPerTick <= 0) {
            return 0;
        }

        // Limit the max amount handled at once (to avoid performance issues)
        int totalToOutput = (int) Math.min(availableCount, maxPerTick);
        if (totalToOutput <= 0) return 0;

        int successful = 0;
        int processed = 0;
        int maxBatch = 64; // Up to 64 per batch

        // Push in batches
        while (processed < totalToOutput) {
            int currentBatch = Math.min(maxBatch, totalToOutput - processed);

            // Create the current batch of items
            ItemStack batch = prototype.clone().asQuantity(currentBatch);

            // Push
            ItemStack leftover;
            try {
                leftover = menu.pushItem(batch, outputSlots);
            } catch (Exception e) {
                break; // An error happened, stop output
            }

            // Calculate the successful amount this time
            int batchSuccess = currentBatch;
            if (leftover != null) {
                batchSuccess -= leftover.getAmount();
            }

            successful += batchSuccess;

            // If it didn't all fit this time, it's full, stop
            if (batchSuccess < currentBatch) {
                break;
            }

            processed += currentBatch;
        }

        return Math.max(0, successful);
    }

    protected void tick(Block block) {

        Location loc = block.getLocation();
        BlockMenu menu = StorageCacheUtils.getMenu(loc);
        SlimefunBlockData data = StorageCacheUtils.getBlock(loc);

        if (menu == null || data == null) return;
        cleanupInvalidSlots(data);
        handleOutput(menu, data);
        handleAllTemplateTransfers(block);
        handleAllInputTransfers(block, data);    // Input transfer (new)

        // Handle input slot items - add amount limit check
        for (int slot : inputSlots) {
            ItemStack item = menu.getItemInSlot(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey keyItem = new NamespacedKey(MagicExpansion.getInstance(), "cargo_item_json");
            NamespacedKey keyAmount = new NamespacedKey(MagicExpansion.getInstance(), "cargo_amount");

            SlimefunItem sfItem = SlimefunItem.getByItem(item);
            if(sfItem instanceof CargoFragment) {
                // Check if it's a CargoFragment: see if it has PDC data
                if (container.has(keyItem, PersistentDataType.STRING)) {
                    String json = container.get(keyItem, PersistentDataType.STRING);
                    Integer amount = container.get(keyAmount, PersistentDataType.INTEGER);

                    if (json != null && amount != null && amount > 0) {
                        // Deserialize the original item
                        ItemStack originalItem = itemFromBase64(json);
                        if (originalItem != null) {
                            // Check how much can be stored
                            int amountToStore = canStoreMoreAmount(data, originalItem, amount);
                            if (amountToStore > 0) {
                                // Compatibility fix(1.20.5+): Template(amount=1)Stored separately from the amount——
                                // Large amount(>99)No longer written into the ItemStack object, Avoid the amount check errors from setAmount/serialization
                                ItemStack toStore = originalItem.clone();
                                toStore.setAmount(1);
                                storeItemCargoCoreMore(data, toStore, amountToStore);

                                // Consume this CargoFragment
                                menu.consumeItem(slot, 1);

                                // If only part of the amount was stored, create a new CargoFragment for the rest
                                if (amountToStore < amount) {
                                    int remaining = amount - amountToStore;
                                    ItemStack newFragment = createCargoFragment(originalItem, remaining);
                                    if (newFragment != null) {
                                        // Put the remaining CargoFragment back in the input slot
                                        menu.replaceExistingItem(slot, newFragment);
                                    }
                                }
                            } else {
                                // Amount reached the limit, don't return the item, leave it in the input slot
                            }
//                            continue; // Done with the fragment, skip the rest of the logic
                        }
                    }
                    // If parsing fails, treat it as a normal item too      //Cancel storing illegal items as normal items
//                    storeItem(data, item);
//                    menu.consumeItem(slot, item.getAmount());
//                    continue;
                }
                // If there's no PDC data, consume the whole item but don't store it      //Cancel deleting illegal items
//                menu.consumeItem(slot, item.getAmount());
            }else{
                // If it's not a CargoFragment, store it as a normal item
                int amountToStore = canStoreMoreAmount(data, item, item.getAmount());
                if (amountToStore > 0) {
                    // Compatibility fix(1.20.5+): Template(amount=1)Stored separately from the amount, Avoid the big-amount ItemStack check error
                    ItemStack toStore = item.clone();
                    toStore.setAmount(1);
                    storeItemCargoCoreMore(data, toStore, amountToStore);

                    // Consume the matching amount of items
                    if (amountToStore == item.getAmount()) {
                        // Fully stored, clear the slot
                        menu.consumeItem(slot, item.getAmount());
                    } else {
                        // Partly stored, only consume part of the amount
                        ItemStack remaining = item.clone();
                        remaining.setAmount(item.getAmount() - amountToStore);
                        menu.replaceExistingItem(slot, remaining);
                    }
                } else {
                    // Can't store any amount, leave it in the input slot
                    // You can add a sound or effect here
                }
            }
        }

        if(menu != null && menu.hasViewer()) {
            // Update storage display
//            updateArrowButtons(menu, data);

            updateStorageDisplay(menu, data);
            updatePageButtons(menu, data);
            updateInputBindDisplay(menu, block); // Update input binding display (new)
            updateTranslateOutPut(menu, block);
        }


    }

    /**
     * Check if more of this item can be stored (considering the amount limit)
     * Return the amount that can actually be stored
     */
    private int canStoreMoreAmount(SlimefunBlockData data, ItemStack item, int amountToAdd) {
        if (item == null || item.getType() == Material.AIR) return 0;

        ItemStack prototype = item.clone();
        prototype.setAmount(1);

        // Find the matching storage slot
        for (int i = 0; i < MAX_STORED_ITEMS; i++) {
            String jsonData = data.getData("item_type_" + i);
            if (jsonData == null || jsonData.isEmpty()) continue;

            try {
                ItemStack storedItem = itemFromBase64(jsonData);
                if (storedItem != null && SameItemJudge.isSimilarSafe(prototype, storedItem)) {
//                if (storedItem != null && SlimefunUtils.isItemSimilar(storedItem, prototype, true)) {
                    // Found a matching item, check the current amount and max limit
                    String countStr = data.getData("item_count_" + i);
                    String maxStr = data.getData("item_max_" + i); // Max amount limit

                    if (countStr == null || countStr.isEmpty()) continue;

                    long currentCount = Long.parseLong(countStr);
                    long maxCount = (maxStr != null && !maxStr.isEmpty()) ? Long.parseLong(maxStr) : -1;

                    // If no limit is set or the limit is -1, it means unlimited
                    if (maxCount == -1) return amountToAdd;

                    // Calculate remaining space
                    long remainingSpace = maxCount - currentCount;
                    if (remainingSpace <= 0) return 0;

                    // Return the amount that can be stored (the smaller of the remaining space and the amount to add)
                    return (int) Math.min(remainingSpace, amountToAdd);
                }
            } catch (Exception e) {
                continue;
            }
        }

        // New item: there must be an empty slot to store it.
        // Before, it returned the full amount whether full or not, so storeItem couldn't find an empty slot and went into the drop logic,
        // but tick runs on an async thread, and world.dropItem triggers an AsyncCatcher error and disables the machine.
        if (findEmptySlot(data) == -1) {
            return 0;
        }
        return amountToAdd;
    }


    /**
     * Check if more of this item can be stored (considering the amount limit)
     */
    private boolean canStoreMore(SlimefunBlockData data, ItemStack item, int amountToAdd) {
        if (item == null || item.getType() == Material.AIR) return false;

        ItemStack prototype = item.clone();
        prototype.setAmount(1);

        // Find the matching storage slot
        for (int i = 0; i < MAX_STORED_ITEMS; i++) {
            String jsonData = data.getData("item_type_" + i);
            if (jsonData == null || jsonData.isEmpty()) continue;

            try {
                ItemStack storedItem = itemFromBase64(jsonData);
                if (storedItem != null && SameItemJudge.isSimilarSafe(prototype, storedItem)) {
//                if (storedItem != null && SlimefunUtils.isItemSimilar(storedItem, prototype, true)) {
                    // Found a matching item, check the current amount and max limit
                    String countStr = data.getData("item_count_" + i);
                    String maxStr = data.getData("item_max_" + i); // Max amount limit

                    if (countStr == null || countStr.isEmpty()) continue;

                    long currentCount = Long.parseLong(countStr);
                    long maxCount = (maxStr != null && !maxStr.isEmpty()) ? Long.parseLong(maxStr) : -1;

                    // If no limit is set or the limit is -1, it means unlimited
                    if (maxCount == -1) return true;

                    // Check if it goes over the limit after adding
                    return currentCount + amountToAdd <= maxCount;
                }
            } catch (Exception e) {
                continue;
            }
        }

        // New item, check the default limit (you can set a global default limit here, or no limit)
        // Same as canStoreMoreAmount: when storage is full, new items can't be stored, so an item isn't lost when the store fails after extraction
        return findEmptySlot(data) != -1;
    }


    /**
     * Store item (fixed version)
     * Make sure we don't overwrite a slot that's being output
     */
    /**
     * Compatible with the old signature: By item.getAmount() as the amount to store.
     * Make a template inside first(amount=1)Then delegate to the new signature, Large amounts no longer go into the ItemStack object(1.20.5+ Compatible with).
     */
    public void storeItemCargoCoreMore(SlimefunBlockData data, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        int count = item.getAmount();
        ItemStack template = item.clone();
        template.setAmount(1);
        storeItemCargoCoreMore(data, template, count);
    }

    /**
     * Store into core(Template+Amount separated).
     * Compatibility fix(1.20.5+/1.21): The storage model was already item_type_N(Prototype) + item_count_N(Amount) two fields,
     * Before, when creating a new slot, an ItemStack with a big amount was serialized directly, in 1.20.5+/1.21 will trigger
     * ItemStack Amount check/serialization error(Tested >99 Amount will definitely blow up, ≤99 Normal), which makes the whole store fail and the machine get disabled.
     * Now the prototype is always amount=1 Serialize, The amount is only stored in the plain-number item_count_N field, Decoupled from the version.
     *
     * @param template Store prototype(must be amount=1 's item)
     * @param count    Amount stored this time
     */
    public void storeItemCargoCoreMore(SlimefunBlockData data, ItemStack template, int count) {
        cleanupInvalidSlots(data);

        // Check how much can be stored
        int amountToStore = canStoreMoreAmount(data, template, count);
        if (amountToStore <= 0) return;

        int slot = findMatchingSlot(data, template);
        if (slot != -1) {
            // Matched an existing slot
            long stored = 0;

            try {
                stored = Long.parseLong(data.getData("item_count_" + slot));
            } catch (Exception ignored) {}

            try {
                stored = Math.addExact(stored, amountToStore);
                data.setData("item_count_" + slot, String.valueOf(stored));
            } catch (ArithmeticException e) {
                // Overflow, drop (fallback, the normal flow is already blocked by the canStoreMoreAmount limit)
                Location loc = data.getLocation();
                dropItemSafely(loc, template, amountToStore);
            }

        } else {
            // Find a truly empty slot (normally canStoreMoreAmount already makes sure there's an empty slot, this is just a fallback)
            slot = findEmptySlot(data);
            if (slot == -1) {
                Location loc = data.getLocation();
                dropItemSafely(loc, template, amountToStore);
                return;
            }

            // The prototype is always amount=1 Serialize(The amount is stored separately in item_count_N, Decoupled from the version)
            String json = itemToBase64(template.clone());
            if (json == null) return;

            data.setData("item_type_" + slot, json);
            data.setData("item_count_" + slot, String.valueOf(amountToStore));
            // No max limit by default (-1 means unlimited)
            data.setData("item_max_" + slot, "-1");
        }
    }


    /**
     * Thread-safe item drop: tick runs on the async thread, so it can't directly call world.dropItem
     * (will trigger AsyncCatcher "main thread check: entity add" and the machine gets disabled).
     * Always schedule back to the main thread; if already on the main thread, drop directly.
     * Drops are grouped by max stack size, up to 64 groups, anything over is only logged(Only the fallback path gets here).
     */
    private void dropItemSafely(Location loc, ItemStack template, int count) {
        if (loc == null || loc.getWorld() == null || template == null || count <= 0) return;

        Runnable dropTask = () -> {
            int remaining = count;
            int maxStack = Math.max(1, template.getMaxStackSize());
            int droppedStacks = 0;
            while (remaining > 0 && droppedStacks < 64) {
                int n = Math.min(maxStack, remaining);
                ItemStack stack = template.clone();
                stack.setAmount(n);
                loc.getWorld().dropItem(loc, stack);
                remaining -= n;
                droppedStacks++;
            }
            if (remaining > 0) {
                MagicExpansion.getInstance().getLogger().warning(
                        "CargoCoreMore Fallback drop amount is over the limit(64group), lost " + remaining + " items @ "
                                + loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
            }
        };

        if (Bukkit.isPrimaryThread()) {
            dropTask.run();
        } else {
            try {
                Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), dropTask);
            } catch (Exception e) {
                // Scheduling failed (e.g. plugin is shutting down), can only give up on the drop
                MagicExpansion.getInstance().getLogger().warning("CargoCoreMore Fallback drop scheduling failed: " + e.getMessage());
            }
        }
    }

    /**
     * Check if the same item already exists (fixed version)
     * including slots with amount 0 but a max limit set
     */
    private int findMatchingSlot(SlimefunBlockData data, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return -1;
        }

        ItemStack prototype = item.clone();
        prototype.setAmount(1);

        // Find all matching slots, including ones with amount 0
        for (int i = 0; i < MAX_STORED_ITEMS; i++) {
            String base64Data = data.getData("item_type_" + i);
            if (base64Data == null || base64Data.isEmpty()) continue;

            ItemStack stored;
            try {
                stored = itemFromBase64(base64Data);
            } catch (Exception e) {
                continue;
            }

            if (stored == null) continue;
            stored.setAmount(1);

            if (SameItemJudge.isSimilarSafe(prototype, stored)) {
//            if (SlimefunUtils.isItemSimilar(prototype, stored, true)) {
                // Check if there's a max amount limit
                String maxStr = data.getData("item_max_" + i);
                long maxCount = -1;
                if (maxStr != null && !maxStr.isEmpty()) {
                    try {
                        maxCount = Long.parseLong(maxStr);
                    } catch (Exception ignored) {}
                }

                // If it's an output target slot, always return it
                String outputTargetStr = data.getData("output_target_slot");
                int outputTarget = -1;
                if (outputTargetStr != null && !outputTargetStr.isEmpty()) {
                    try {
                        outputTarget = Integer.parseInt(outputTargetStr);
                    } catch (Exception ignored) {}
                }

                if (i == outputTarget) {
                    return i;
                }

                // Otherwise, only return slots with amount greater than 0 or with a max limit set
                String countStr = data.getData("item_count_" + i);
                if (countStr != null && !countStr.isEmpty()) {
                    try {
                        long count = Long.parseLong(countStr);
                        if (count > 0 || maxCount != -1) {
                            return i;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        return -1;
    }


    /**
     * Clean up invalid slots:count <= 0 and slots with no max limit, unless it's an output target
     */
    public void cleanupInvalidSlots(SlimefunBlockData data) {
        // Get the current output target
        int currentOutputSlot = -1;
        try {
            String outputStr = data.getData("output_target_slot");
            if (outputStr != null && !outputStr.isEmpty()) {
                currentOutputSlot = Integer.parseInt(outputStr);
            }
        } catch (Exception ignored) {}

        for (int i = 0; i < MAX_STORED_ITEMS; i++) {
            String typeKey = "item_type_" + i;
            String countKey = "item_count_" + i;
            String maxKey = "item_max_" + i;

            String typeStr = data.getData(typeKey);
            String countStr = data.getData(countKey);
            String maxStr = data.getData(maxKey);

            boolean hasType = typeStr != null && !typeStr.trim().isEmpty();
            boolean hasCount = countStr != null && !countStr.trim().isEmpty();
            boolean hasMax = maxStr != null && !maxStr.trim().isEmpty();

            if (!hasType && !hasCount && !hasMax) continue;

            // One missing → Delete (unless it's an output target)
            if (hasType && !hasCount) {
                if (i != currentOutputSlot) {
                    data.removeData(typeKey);
                    if (hasMax) data.removeData(maxKey);
                }
                continue;
            }
            if (!hasType && hasCount) {
                if (i != currentOutputSlot) {
                    data.removeData(countKey);
                    if (hasMax) data.removeData(maxKey);
                }
                continue;
            }

            // Both have → Check count
            long count = 0;
            try {
                if (countStr == null) {
                    if (i != currentOutputSlot) {
                        data.removeData(typeKey);
                        data.removeData(countKey);
                        if (hasMax) data.removeData(maxKey);
                    }
                    continue;
                }

                count = Long.parseLong(countStr.trim());

                // Check if there's a max amount limit
                long maxCount = -1;
                if (maxStr != null && !maxStr.trim().isEmpty()) {
                    try {
                        maxCount = Long.parseLong(maxStr.trim());
                    } catch (Exception ignored) {}
                }

                // Only clean up the slot if no max amount limit is set and the amount is 0
                // But if it's an output target, don't clean up even if the amount is 0
                if (count <= 0 && maxCount == -1 && i != currentOutputSlot) {
                    data.removeData(typeKey);
                    data.removeData(countKey);
                    data.removeData(maxKey);
                }
                // If a max amount limit is set, keep the slot even if the amount is 0
            } catch (Exception e) {
                // Only clean up when count is wrong
                // But if it's an output target, don't delete
                if (i != currentOutputSlot) {
                    data.removeData(typeKey);
                    data.removeData(countKey);
                    if (hasMax) data.removeData(maxKey);
                }
            }
        }
    }

    /**
     * Find a truly empty slot (fixed version)
     * Only return completely unused slots, never slots with amount 0
     */
    private int findEmptySlot(SlimefunBlockData data) {
        for (int i = 0; i < MAX_STORED_ITEMS; i++) {
            String typeData = data.getData("item_type_" + i);
            String countData = data.getData("item_count_" + i);

            // Only treat it as a truly empty slot when both type and amount are empty
            if ((typeData == null || typeData.isEmpty()) &&
                    (countData == null || countData.isEmpty())) {
                return i;
            }

            // If there's type data but the amount is 0, check if it's an output target
            if (typeData != null && !typeData.isEmpty()) {
                String countStr = data.getData("item_count_" + i);
                if (countStr != null && !countStr.isEmpty()) {
                    try {
                        long count = Long.parseLong(countStr);
                        // If it's an output target, skip this slot
                        String outputTargetStr = data.getData("output_target_slot");
                        int outputTarget = -1;
                        if (outputTargetStr != null && !outputTargetStr.isEmpty()) {
                            try {
                                outputTarget = Integer.parseInt(outputTargetStr);
                            } catch (Exception ignored) {}
                        }

                        if (i == outputTarget) {
                            continue; // Skip the output target slot
                        }

                        // If the amount is 0 and there's no max limit, this slot should be cleaned up and not used
                        String maxStr = data.getData("item_max_" + i);
                        long maxCount = -1;
                        if (maxStr != null && !maxStr.isEmpty()) {
                            try {
                                maxCount = Long.parseLong(maxStr);
                            } catch (Exception ignored) {}
                        }

                        if (count <= 0 && maxCount == -1) {
                            continue; // Skip this slot
                        }
                    } catch (Exception e) {
                        // Parsing failed, skip this slot
                        continue;
                    }
                }
            }
        }
        return -1;
    }

    private List<Integer> getStoredItemSlots(SlimefunBlockData data) {
        List<Integer> slots = new ArrayList<>();

        for (int i = 0; i < MAX_STORED_ITEMS; i++) {
            String countStr = data.getData("item_count_" + i);
            String typeStr = data.getData("item_type_" + i);

            // Add null check
            if (countStr == null || typeStr == null) {
                continue;
            }

            try {
                long count = Long.parseLong(countStr.trim());
                // Check max amount limit
                String maxStr = data.getData("item_max_" + i);
                long maxCount = -1;
                if (maxStr != null && !maxStr.trim().isEmpty()) {
                    try {
                        maxCount = Long.parseLong(maxStr.trim());
                    } catch (Exception ignored) {}
                }

                // If the amount>0 Or a max amount limit is set, both include this slot
                if (count > 0 || maxCount != -1) {
                    slots.add(i);
                }
            } catch (Exception ignored) {}
        }

        return slots;
    }

    private void updateStorageDisplay(BlockMenu menu, SlimefunBlockData data) {
        List<Integer> slots = getStoredItemSlots(data);
        int page = getCurrentPage(data);
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, slots.size());

        // Get the current output target
        int currentOutputSlot = -1;
        try {
            String outputStr = data.getData("output_target_slot");
            if (outputStr != null && !outputStr.isEmpty()) {
                currentOutputSlot = Integer.parseInt(outputStr);
            }
        } catch (Exception ignored) {}

        // Clear display slot
        for (int slot : storageSlots) {
            menu.replaceExistingItem(slot, new CustomItemStack(Material.BARRIER, " "));
        }

        // Fill current page
        for (int i = 0; i < endIndex - startIndex; i++) {
            int dataSlot = slots.get(startIndex + i);
            String jsonData = data.getData("item_type_" + dataSlot);
            ItemStack prototype = itemFromBase64(jsonData); // ✅ Restore with JSON
            if (prototype == null) continue;

            long count = 0;
            long maxCount = -1;
            try {
                String countStr = data.getData("item_count_" + dataSlot);
                if (countStr != null && !countStr.isEmpty()) {
                    count = Long.parseLong(countStr);
                }
                String maxStr = data.getData("item_max_" + dataSlot);
                if (maxStr != null && !maxStr.isEmpty()) {
                    maxCount = Long.parseLong(maxStr);
                }
            } catch (Exception ignored) {}

            // Only skip showing when the amount is 0 and no max limit is set
            if (count <= 0 && maxCount == -1) {
                continue;
            }

            ItemStack display = prototype.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                if (lore == null) lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Stored amount: §a" + count);

                // Show max amount limit
                if (maxCount != -1) {
                    lore.add("§7Max amount: §e" + maxCount);
                    // If it's close to or at the limit, show a warning
                    if (count >= maxCount) {
                        lore.add("§c§lStorage limit reached!");
                    } else if (count >= maxCount * 0.9) {
                        lore.add("§6About to hit the storage limit!");
                    }
                } else {
                    lore.add("§7Max amount: §aUnlimited");
                }

                // ✅ Add"Outputting"Status hint
                if (dataSlot == currentOutputSlot) {
                    lore.add(""); // Blank line separator
                    lore.add("§6§l▶ §eOutputting now"); // Golden arrow + Yellow text
                }

                lore.add("");
                lore.add("§bLeft-click: Take out 64");
                lore.add("§bRight-click: Set continuous output");
                lore.add("§bShift+Left-click: Set max storage amount");
                lore.add("§bShift+Right-click: Clear amount limit");

                meta.setLore(lore);

                // Write to PDC for reverse lookup dataSlot
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(new NamespacedKey(MagicExpansion.getInstance(), "cargo_slot"), PersistentDataType.INTEGER, dataSlot);
                display.setItemMeta(meta);
            }
            display.setAmount(1);

            int finalCurrentOutputSlot = currentOutputSlot;
            final long finalMaxCount = maxCount;
            menu.addItem(storageSlots[i], display, (player, slotClicked, clickedItem, clickAction) -> {
                if (clickedItem == null) {
                    player.playSound(player.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
                    return false;
                }
                if (menu == null || data == null) return false;
                ItemMeta itemMeta = clickedItem.getItemMeta();
                if (itemMeta == null) return false;
                NamespacedKey slotKey = new NamespacedKey(MagicExpansion.getInstance(), "cargo_slot");
                if (!itemMeta.getPersistentDataContainer().has(slotKey, PersistentDataType.INTEGER)) return false;
                int targetDataSlot = itemMeta.getPersistentDataContainer().get(slotKey, PersistentDataType.INTEGER);
                String json = data.getData("item_type_" + targetDataSlot);
                long itemCount = 0;
                try {
                    String countStr = data.getData("item_count_" + targetDataSlot);
                    if (countStr != null && !countStr.isEmpty()) {
                        itemCount = Long.parseLong(countStr);
                    }
                } catch (Exception ignored) {}

                ItemStack itemPrototype = itemFromBase64(json);
                if (itemPrototype == null) return false;

                // Check if the item still has stock or has a max limit set
                String maxStr = data.getData("item_max_" + targetDataSlot);
                long currentMaxCount = -1;
                if (maxStr != null && !maxStr.isEmpty()) {
                    try {
                        currentMaxCount = Long.parseLong(maxStr);
                    } catch (Exception ignored) {}
                }

                // If the amount is 0 and there's no max limit, the item has been deleted
                if (itemCount <= 0 && currentMaxCount == -1) {
                    player.sendMessage("§cThis item was removed from storage");
                    updateStorageDisplay(menu, data);
                    return false;
                }

                ItemStack itemPrototypeClone = itemPrototype.clone();
                itemPrototypeClone.setAmount(1);

                // === Handle different click types ===
                if (clickAction.isShiftClicked() && clickAction.isRightClicked()) {
                    // Shift + Right-click: clear amount limit
                    String currentMaxStr = data.getData("item_max_" + targetDataSlot);
                    long currentMaxCount1 = -1;
                    if (currentMaxStr != null && !currentMaxStr.isEmpty()) {
                        try {
                            currentMaxCount1 = Long.parseLong(currentMaxStr);
                        } catch (Exception ignored) {}
                    }

                    if (currentMaxCount1 == -1) {
                        player.sendMessage("§cThis item has no amount limit set");
                        player.playSound(player.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
                    } else {
                        data.setData("item_max_" + targetDataSlot, "-1");
                        player.sendMessage("§aCleared " + ItemStackHelper.getDisplayName(itemPrototypeClone) + " 's amount limit");

                        // If the amount is 0, also clear the whole slot
                        if (itemCount <= 0) {
                            data.removeData("item_type_" + targetDataSlot);
                            data.removeData("item_count_" + targetDataSlot);
                            data.removeData("item_max_" + targetDataSlot);
                            player.sendMessage("§aRemoved from storage because the item amount is 0");
                        }

                        updateStorageDisplay(menu, data);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5F, 2.0F);
                    }
                }
                else if (clickAction.isShiftClicked() && !clickAction.isRightClicked()) {
                    // Shift + Left-click: set max storage amount
                    player.closeInventory();
                    player.sendMessage("§ePlease enter " + ItemStackHelper.getDisplayName(itemPrototypeClone) + " 's max storage amount:");
                    player.sendMessage("§bRange: -1~9,223,372,036,854,775,807 (No need to type commas, just the exact number)");
                    player.sendMessage("§7Enter -1 for unlimited, enter 0 to cancel");

                    // Set up a temporary store to record the player's input
                    NamespacedKey inputKey = new NamespacedKey(MagicExpansion.getInstance(), "setting_max_" + player.getUniqueId());
                    PersistentDataContainer playerData = player.getPersistentDataContainer();
                    playerData.set(inputKey, PersistentDataType.INTEGER, targetDataSlot);

                    // Set chat listener
                    MagicExpansion.getInstance().getServer().getScheduler().runTask(MagicExpansion.getInstance(), () -> {
                        setMaxAmountInputHandler(player, inputKey, data, menu);
                    });
                }
                else if (clickAction.isRightClicked() && !clickAction.isShiftClicked()) {
                    // Right-click: set continuous output state
                    if (finalCurrentOutputSlot == targetDataSlot) {
                        // Already outputting → Stop
                        data.setData("output_target_slot", "-1");
                        player.sendMessage("§aOutput stopped: " + ItemStackHelper.getDisplayName(itemPrototypeClone));
                    } else {
                        // Start outputting a new item
                        data.setData("output_target_slot", String.valueOf(targetDataSlot));
                        player.sendMessage("§eStart continuous output: " + ItemStackHelper.getDisplayName(itemPrototypeClone));
                    }

                    // Refresh the UI (update Lore status)
                    updateStorageDisplay(menu, data);
                }
                else if (!clickAction.isRightClicked() && !clickAction.isShiftClicked()) {
                    // Left-click: take out 64
                    // If the amount is 0, say it can't be taken out
                    if (itemCount <= 0) {
                        player.sendMessage("§cThis item's stock is 0, can't take it out");
                        player.playSound(player.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
                        return false;
                    }

                    int take = (int) Math.min(64, itemCount);
                    if (take <= 0) return false;
//                    ItemStack toTake = itemPrototype.clone().asQuantity(take);
//                    if (player.getInventory().addItem(toTake).isEmpty()) {
//                        itemCount -= take;
//                        data.setData("item_count_" + targetDataSlot, String.valueOf(itemCount));
//                        player.sendMessage("§aTook out §e" + take + " §ax " + ItemStackHelper.getDisplayName(itemPrototypeClone));
//                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5F, 1.0F);
//                        updateStorageDisplay(menu, data);
//                    } else {
//                        player.sendMessage("§cNot enough inventory space, can't take out the item.");
//                        player.playSound(player.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
//                    }

                    ItemStack itemToGive = itemPrototypeClone.clone();
                    itemToGive.setAmount(take); // Amount trying to take out
                    int maxCanHold = player.getInventory().getMaxStackSize();
                    int totalAvailableSpace = 0;
                    for (ItemStack stack : player.getInventory().getStorageContents()) { // Only look at the main inventory+Hotbar (no armor)
                        if (stack == null || stack.getType() == Material.AIR) {
                            totalAvailableSpace += maxCanHold;
                        } else if (stack.isSimilar(itemToGive)) {
                            totalAvailableSpace += maxCanHold - stack.getAmount();
                        }
                    }
                    int actualTake = Math.min(take, totalAvailableSpace);
                    if (actualTake <= 0) {
                        player.sendMessage("§cNot enough inventory space, can't take out the item.");
                        player.playSound(player.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
                    } else {
                        itemToGive.setAmount(actualTake);
                        Map<Integer, ItemStack> leftover = player.getInventory().addItem(itemToGive);
                        if (!leftover.isEmpty()) {
                            itemCount -= actualTake;
                            data.setData("item_count_" + targetDataSlot, String.valueOf(itemCount));
                            player.sendMessage("§aTook out §e" + actualTake + " §ax " + ItemStackHelper.getDisplayName(itemPrototypeClone));
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5F, 1.0F);
                            updateStorageDisplay(menu, data);
                            player.sendMessage("§cAn error happened: some items couldn't fit in your inventory and were dropped on the ground.");
                            for (ItemStack leftoverItem : leftover.values()) {
                                player.getWorld().dropItem(player.getLocation(), leftoverItem);
                            }
                        } else {
                            itemCount -= actualTake;
                            data.setData("item_count_" + targetDataSlot, String.valueOf(itemCount));
                            player.sendMessage("§aTook out §e" + actualTake + " §ax " + ItemStackHelper.getDisplayName(itemPrototypeClone));
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5F, 1.0F);
                            updateStorageDisplay(menu, data);
                        }
                    }

                } else {
                    player.playSound(player.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
                }

                return false;
            });
        }

        // Assuming blueBorder is int[] Array containing the slots to set
        for (int slot : blueBorder) {
            menu.addItem(slot, new CustomItemStack(Material.BLUE_STAINED_GLASS_PANE, "§bView stored items"),(player, sloti, itemStack, clickAction) -> {
                openStorageMenu(player, data,0); // Open the first page by default
                return false; // Don't consume the item or do the default behavior
            });
        }

        updatePageButtons(menu, data);
    }

    /**
     * Set max amount input handler
     */
    private void setMaxAmountInputHandler(Player player, NamespacedKey inputKey, SlimefunBlockData data, Object menu) {
        // Cancel the event listener to avoid registering twice
        org.bukkit.event.HandlerList.unregisterAll(new org.bukkit.event.Listener() {});

        // Create a temporary listener
        org.bukkit.event.Listener listener = new org.bukkit.event.Listener() {
            @EventHandler
            public void onPlayerChat(AsyncPlayerChatEvent event) {
                Player p = event.getPlayer();
                if (!p.equals(player)) return;

                event.setCancelled(true);
                String message = event.getMessage().trim();

                // Handle in the sync thread
                Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> {
                    try {
                        long maxAmount = Long.parseLong(message);
                        Integer targetSlot = p.getPersistentDataContainer().get(inputKey, PersistentDataType.INTEGER);

                        if (targetSlot != null) {
                            if (maxAmount == 0) {
                                p.sendMessage("§cMax amount setting cancelled");
                            } else if (maxAmount < -1) {
                                p.sendMessage("§cInvalid amount! Enter -1 (unlimited) or a number greater than or equal to 1");
                            } else {
                                // Set max amount
                                data.setData("item_max_" + targetSlot, String.valueOf(maxAmount));

                                // Get item info for feedback
                                String json = data.getData("item_type_" + targetSlot);
                                ItemStack itemPrototype = itemFromBase64(json);
                                String itemName = itemPrototype != null ?
                                        ItemStackHelper.getDisplayName(itemPrototype) : "Unknown item";

                                if (maxAmount == -1) {
                                    p.sendMessage("§aSet " + itemName + " means no amount limit");
                                } else {
                                    p.sendMessage("§aSet " + itemName + " 's max storage amount is: §e" + maxAmount);

                                    // If the current amount is over the new limit, adjust the amount
                                    String countStr = data.getData("item_count_" + targetSlot);
                                    if (countStr != null) {
                                        try {
                                            long currentCount = Long.parseLong(countStr);
                                            if (currentCount > maxAmount) {
                                                data.setData("item_count_" + targetSlot, String.valueOf(maxAmount));
                                                p.sendMessage("§6The current amount has gone from " + currentCount + " Adjusted to the new limit " + maxAmount);
                                            }
                                        } catch (NumberFormatException e) {
                                            // Ignore conversion errors
                                        }
                                    }
                                }

                                // Play success sound
                                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5F, 2.0F);

                                if (menu != null) {
                                    if (menu instanceof BlockMenu) {
                                        BlockMenu blockMenu = (BlockMenu) menu;
                                        if (blockMenu.getLocation() != null) {
                                            updateStorageDisplay(blockMenu, data);
                                        }
                                    } else if (menu instanceof ChestMenu) {
                                        ChestMenu chestMenu = (ChestMenu) menu;
                                        // For ChestMenu, we need to refresh the whole menu
                                        // We need to get the current page number info here
                                        int currentPage = 0; // You need to get the current page number from somewhere
                                        refreshStorageMenu(chestMenu, data, currentPage);
                                    }
                                }


                            }
                        } else {
                            p.sendMessage("§cSetup failed: data expired, please reopen the menu and try again");
                        }

                        // Clean up player data
                        p.getPersistentDataContainer().remove(inputKey);

                    } catch (NumberFormatException e) {
                        p.sendMessage("§cPlease enter a valid number!");
                        p.sendMessage("§bRange: -1~9,223,372,036,854,775,807 (No need to type commas, just the exact number)");
                        p.sendMessage("§7Input §e-1 §7means unlimited, enter §e0 §7Cancel");
                        p.sendMessage("§7Enter the new max amount:");
                        return; // Don't clean up the data, let the player keep typing
                    } catch (Exception e) {
                        p.sendMessage("§cAn error happened during setup, please try again");
                        e.printStackTrace();
                    } finally {
                        // Always unregister the listener
                        HandlerList.unregisterAll(this);
                    }
                });
            }

            @EventHandler
            public void onPlayerQuit(PlayerQuitEvent event) {
                Player p = event.getPlayer();
                if (p.equals(player)) {
                    // Player left, clean up data
                    p.getPersistentDataContainer().remove(inputKey);
                    HandlerList.unregisterAll(this);
                }
            }

            @EventHandler
            public void onInventoryClose(InventoryCloseEvent event) {
                if (event.getPlayer().equals(player)) {
                    // Player closed the inventory, clean up data (but keep chat input)
                    // Don't clean up here, let the player keep typing
                }
            }
        };

        // Register event listener
        Bukkit.getPluginManager().registerEvents(listener, MagicExpansion.getInstance());

        // Set timeout task (auto cleanup after 30 seconds)
        Bukkit.getScheduler().runTaskLater(MagicExpansion.getInstance(), () -> {
            if (player.getPersistentDataContainer().has(inputKey, PersistentDataType.INTEGER)) {
                player.getPersistentDataContainer().remove(inputKey);
                HandlerList.unregisterAll(listener);
                if (player.isOnline()) {
                    player.sendMessage("§cTimed out, cancelled automatically");
                }
            }
        }, 20 * 30); // 30sec timeout
    }

    private void openStorageMenu(Player player, SlimefunBlockData data, int currentPage) {
        ChestMenu menu = new ChestMenu("§6Storage overview");
        menu.setSize(9 * 6);
        menu.setEmptySlotsClickable(true);
        // Refresh right after initializing
        refreshStorageMenu(menu, data, currentPage);
        menu.open(player);
    }
    /**
     * Refresh paged storage menu (replaces the old updateStorageDisplay)
     */
    private void refreshStorageMenu(ChestMenu menu, SlimefunBlockData data, int currentPage) {
        if (data == null) return;

        // Collect valid slots again
        List<Integer> validSlots = new ArrayList<>();
        for (int i = 0; i < MAX_STORED_ITEMS; i++) {
            String json = data.getData("item_type_" + i);
            String countStr = data.getData("item_count_" + i);
            long count = 0;
            try {
                if (countStr != null && !countStr.isEmpty()) {
                    count = Long.parseLong(countStr);
                }
            } catch (Exception ignored) {}

            if (json != null && !json.isEmpty()) {
                // Check if there's a max amount limit
                String maxStr = data.getData("item_max_" + i);
                long maxCount = -1;
                if (maxStr != null && !maxStr.isEmpty()) {
                    try {
                        maxCount = Long.parseLong(maxStr);
                    } catch (Exception ignored) {}
                }

                // If the amount>0 Or a max amount limit is set, both include this slot
                if (count > 0 || maxCount != -1) {
                    validSlots.add(i);
                }
            }
        }

        int itemsPerPage = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) validSlots.size() / itemsPerPage));
        if (currentPage >= totalPages) currentPage = Math.max(0, totalPages - 1);

        // === Refill 0-44 ===
        int start = currentPage * itemsPerPage;
        for (int i = 0; i < 45; i++) {
            int index = start + i;
            if (index >= validSlots.size()) {
                menu.addItem(i, new CustomItemStack(Material.BARRIER, " "),(p, slot, item, action)-> false);
                continue;
            }

            int targetDataSlot = validSlots.get(index);
            final int finalTargetSlot = targetDataSlot; // ← Explicitly declared final
            String json = data.getData("item_type_" + finalTargetSlot);
            long itemCount = 0;
            long maxCount = -1;
            try {
                String countStr = data.getData("item_count_" + finalTargetSlot);
                if (countStr != null && !countStr.isEmpty()) {
                    itemCount = Long.parseLong(countStr);
                }
                String maxStr = data.getData("item_max_" + finalTargetSlot);
                if (maxStr != null && !maxStr.isEmpty()) {
                    maxCount = Long.parseLong(maxStr);
                }
            } catch (Exception ignored) {}

            ItemStack itemPrototype = itemFromBase64(json);
            if (itemPrototype == null) {
                menu.addItem(i, new CustomItemStack(Material.BARRIER, "§cData error"));
                continue;
            }

            // Only skip showing when the amount is 0 and no max limit is set
            if (itemCount <= 0 && maxCount == -1) {
                menu.addItem(i, new CustomItemStack(Material.BARRIER, " "));
                continue;
            }

            ItemStack itemPrototypeClone = itemPrototype.clone();
            itemPrototypeClone.setAmount(1);

            ItemStack display = itemPrototype.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                // Add PDC
                NamespacedKey slotKey = new NamespacedKey(MagicExpansion.getInstance(), "cargo_slot");
                meta.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, targetDataSlot);
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add("§7Stored amount: §e" + itemCount);

                // Show max amount limit
                if (maxCount != -1) {
                    lore.add("§7Max amount: §e" + maxCount);
                    // If it's close to or at the limit, show a warning
                    if (itemCount >= maxCount) {
                        lore.add("§c§lStorage limit reached!");
                    } else if (itemCount >= maxCount * 0.9) {
                        lore.add("§6About to hit the storage limit!");
                    }
                } else {
                    lore.add("§7Max amount: §aUnlimited");
                }

                // ✅ Add"Outputting"Status hint
                int currentOutput = -1;
                try {
                    String outputStr = data.getData(OUTPUT_TARGET_KEY);
                    if (outputStr != null && !outputStr.isEmpty()) {
                        currentOutput = Integer.parseInt(outputStr);
                    }
                } catch (Exception ignored) { }

                if (targetDataSlot == currentOutput) {
                    lore.add(""); // Blank line
                    lore.add("§6§l▶ §eOutputting now"); // Golden arrow + Yellow text
                }

                lore.add("");
                lore.add("§bLeft-click: Take out 64");
                lore.add("§bRight-click: Set continuous output");
                lore.add("§bShift+Left-click: Set max storage amount");
                lore.add("§bShift+Right-click: Clear amount limit");

                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            display.setAmount(1);

            int finalCurrentPage2 = currentPage;
            final long finalMaxCount = maxCount;
            menu.addItem(i, display,(p, slot, clicked, action) -> {
                if (clicked == null) {
                    p.playSound(p.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
                    return false;
                }
                if (menu == null || data == null) return false;
                ItemMeta itemMeta = clicked.getItemMeta();

                if (itemMeta == null) return false;
                NamespacedKey slotKey = new NamespacedKey(MagicExpansion.getInstance(), "cargo_slot");

                if (!itemMeta.getPersistentDataContainer().has(slotKey, PersistentDataType.INTEGER)) return false;

                int targetDataSlot1 = itemMeta.getPersistentDataContainer().get(slotKey, PersistentDataType.INTEGER);
                String json1 = data.getData("item_type_" + targetDataSlot1);
                long itemCount1 = 0;
                try {
                    String countStr = data.getData("item_count_" + targetDataSlot1);
                    if (countStr != null && !countStr.isEmpty()) {
                        itemCount1 = Long.parseLong(countStr);
                    }
                } catch (Exception e) {
                    p.sendMessage("§cFailed to read amount");
                    return false;
                }

                if (json1 == null) {
                    p.sendMessage("§cItem data error");
                    refreshStorageMenu(menu, data, finalCurrentPage2);
                    return false;
                }

                // Check if the item still has stock or has a max limit set
                String maxStr1 = data.getData("item_max_" + targetDataSlot1);
                long currentMaxCount1 = -1;
                if (maxStr1 != null && !maxStr1.isEmpty()) {
                    try {
                        currentMaxCount1 = Long.parseLong(maxStr1);
                    } catch (Exception ignored) {}
                }

                // If the amount is 0 and there's no max limit, the item has been deleted
                if (itemCount1 <= 0 && currentMaxCount1 == -1) {
                    p.sendMessage("§cThis item was removed from storage");
                    refreshStorageMenu(menu, data, finalCurrentPage2);
                    return false;
                }

                ItemStack itemPrototype1 = itemFromBase64(json1);
                if (itemPrototype1 == null) {
                    p.sendMessage("§cItem data error");
                    refreshStorageMenu(menu, data, finalCurrentPage2);
                    return false;
                }

                ItemStack itemPrototypeClone1 = itemPrototype1.clone();
                itemPrototypeClone1.setAmount(1);

                // === Tell click types apart ===
                // Get the target slot currently being output
                int currentOutputTarget = -1;
                try {
                    String outputStr = data.getData("output_target_slot");
                    if (outputStr != null && !outputStr.isEmpty()) {
                        currentOutputTarget = Integer.parseInt(outputStr);
                    }
                } catch (Exception ignored) { }

                if (action.isShiftClicked() && action.isRightClicked()) {
                    // Shift + Right-click: clear amount limit
                    data.setData("item_max_" + targetDataSlot1, "-1");
                    p.sendMessage("§aCleared " + ItemStackHelper.getDisplayName(itemPrototypeClone1) + " 's amount limit");
                    refreshStorageMenu(menu, data, finalCurrentPage2);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5F, 2.0F);
                }
                else if (action.isShiftClicked() && !action.isRightClicked()) {
                    // Shift + Left-click: set max storage amount
                    p.closeInventory();
                    p.sendMessage("§ePlease enter " + ItemStackHelper.getDisplayName(itemPrototypeClone1) + " 's max storage amount:");
                    p.sendMessage("§bRange: -1~9,223,372,036,854,775,807 (No need to type commas, just the exact number)");
                    p.sendMessage("§7Enter -1 for unlimited, enter 0 to cancel");

                    // Set up a temporary store to record the player's input
                    NamespacedKey inputKey = new NamespacedKey(MagicExpansion.getInstance(), "setting_max_" + p.getUniqueId());
                    PersistentDataContainer playerData = p.getPersistentDataContainer();
                    playerData.set(inputKey, PersistentDataType.INTEGER, targetDataSlot1);

                    // Set chat listener
                    MagicExpansion.getInstance().getServer().getScheduler().runTask(MagicExpansion.getInstance(), () -> {
                        setMaxAmountInputHandler(p, inputKey, data, menu);
                    });
                }
                else if (action.isRightClicked() && !action.isShiftClicked()) {
                    // Right-click: set continuous output state
                    if (targetDataSlot1 == currentOutputTarget) {
                        // The current item is being output → Stop
                        data.setData("output_target_slot", "-1");
                        p.sendMessage("§aOutput stopped: " + ItemStackHelper.getDisplayName(itemPrototypeClone1));
                    } else {
                        // The current item is not an output target → Start output
                        data.setData("output_target_slot", String.valueOf(targetDataSlot1));
                        p.sendMessage("§eStart continuous output: " + ItemStackHelper.getDisplayName(itemPrototypeClone1));
                    }

                    refreshStorageMenu(menu, data, finalCurrentPage2);
                    return false;
                }
                else if (!action.isRightClicked() && !action.isShiftClicked()) {
                    // Left-click: take out 64
                    // If the amount is 0, say it can't be taken out
                    if (itemCount1 <= 0) {
                        p.sendMessage("§cThis item's stock is 0, can't take it out");
                        p.playSound(p.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
                        return false;
                    }

                    int take = (int) Math.min(64, itemCount1);
                    if (take <= 0) return false;
//                    ItemStack toTake = itemPrototype1.clone().asQuantity(take);
//                    if (p.getInventory().addItem(toTake).isEmpty()) {
//                        itemCount1 -= take;
//                        data.setData("item_count_" + targetDataSlot1, String.valueOf(itemCount1));
//                        p.sendMessage("§aTook out §e" + take + " §ax " + ItemStackHelper.getDisplayName(itemPrototypeClone1));
//                        p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5F, 1.0F);
//                        refreshStorageMenu(menu, data, finalCurrentPage2);
//                    } else {
//                        p.sendMessage("§cNot enough inventory space, can't take out the item.");
//                        p.playSound(p.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
//                    }

                    ItemStack itemToGive = itemPrototypeClone.clone();
                    itemToGive.setAmount(take); // Amount trying to take out
                    int maxCanHold = p.getInventory().getMaxStackSize();
                    int totalAvailableSpace = 0;
                    for (ItemStack stack : p.getInventory().getStorageContents()) { // Only look at the main inventory+Hotbar (no armor)
                        if (stack == null || stack.getType() == Material.AIR) {
                            totalAvailableSpace += maxCanHold;
                        } else if (stack.isSimilar(itemToGive)) {
                            totalAvailableSpace += maxCanHold - stack.getAmount();
                        }
                    }
                    int actualTake = Math.min(take, totalAvailableSpace);
                    if (actualTake <= 0) {
                        p.sendMessage("§cNot enough inventory space, can't take out the item.");
                        p.playSound(p.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
                    } else {
                        itemToGive.setAmount(actualTake);
                        Map<Integer, ItemStack> leftover = p.getInventory().addItem(itemToGive);
                        if (!leftover.isEmpty()) {
                            itemCount1 -= actualTake;
                            data.setData("item_count_" + targetDataSlot, String.valueOf(itemCount1));
                            p.sendMessage("§aTook out §e" + actualTake + " §ax " + ItemStackHelper.getDisplayName(itemPrototypeClone));
                            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5F, 1.0F);
                            refreshStorageMenu(menu, data, finalCurrentPage2);
                            p.sendMessage("§cAn error happened: some items couldn't fit in your inventory and were dropped on the ground.");
                            for (ItemStack leftoverItem : leftover.values()) {
                                p.getWorld().dropItem(p.getLocation(), leftoverItem);
                            }
                        } else {
                            itemCount1 -= actualTake;
                            data.setData("item_count_" + targetDataSlot, String.valueOf(itemCount1));
                            p.sendMessage("§aTook out §e" + actualTake + " §ax " + ItemStackHelper.getDisplayName(itemPrototypeClone));
                            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5F, 1.0F);
                            refreshStorageMenu(menu, data, finalCurrentPage2);
                        }
                    }


                } else {
                    p.playSound(p.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
                }

                return false;
            });
        }

        // === Refresh page buttons ===
        if (currentPage > 0) {
            int finalCurrentPage = currentPage;
            menu.addItem(45, new CustomItemStack(Material.ARROW, "§aPrevious page, current page: "+(currentPage+1)+"/"+totalPages),(p, s, item, action) -> {
                refreshStorageMenu(menu, data, finalCurrentPage - 1);
                return false;
            });
        } else {
            menu.addItem(45, new CustomItemStack(Material.BARRIER, "§eAlready at the first page, current page: "+(currentPage+1)+"/"+totalPages),(p, s, item, action) -> false);
        }

        if (currentPage < totalPages - 1) {
            int finalCurrentPage1 = currentPage;
            menu.addItem(53, new CustomItemStack(Material.SPECTRAL_ARROW, "§aNext page, current page: "+(currentPage+1)+"/"+totalPages),(p, s, item, action) -> {
                refreshStorageMenu(menu, data, finalCurrentPage1 + 1);
                return false;
            });
        } else {
            menu.addItem(53, new CustomItemStack(Material.BARRIER, "§eAlready at the last page, current page: "+(currentPage+1)+"/"+totalPages),(p, s, item, action) -> false);
        }

        // Decorations and the center item stay the same, can refresh selectively
        ItemStack pinkGlow = doGlow(new ItemStack(Material.PINK_STAINED_GLASS_PANE));
        int[] pinkSlots = {46, 47, 48, 50, 51, 52};
        for (int slot : pinkSlots) {
            menu.addItem(slot, new CustomItemStack(pinkGlow, ColorGradient.getGradientName("Decorative border")),(p, s, item, action) -> false);
        }

        menu.addItem(49, new CustomItemStack(Material.NETHER_STAR, "§6Storage overview"),(p, s, item, action) -> false);
    }



    // Update page buttons (with dynamic click events)
    private void updatePageButtons(BlockMenu menu, SlimefunBlockData data) {
        List<Integer> slots = getStoredItemSlots(data);
        int page = getCurrentPage(data);
        int totalPages = (int) Math.ceil((double) slots.size() / ITEMS_PER_PAGE);

        boolean hasPrev = page > 0;
        boolean hasNext = (page + 1) < totalPages;

        // Previous page button
        menu.replaceExistingItem(45, hasPrev ?
                new CustomItemStack(Material.ARROW, "§bPrevious page (Store item)","§7Page: "+(page+1)+"/"+totalPages) :
                new CustomItemStack(Material.BARRIER, "§cAlready the first page (Store item)","§7Page: "+(page+1)+"/"+totalPages));

        // ✅ Rebind the click handler
        menu.addMenuClickHandler(45, (player, slot, item, action) -> {
            if (hasPrev) {
                data.setData("storage_page", String.valueOf(page - 1));
                updateStorageDisplay(menu, data); // This will call again updatePageButtons
                player.playSound(menu.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.5F, 1.0F);
            } else {
                player.playSound(menu.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
            }
            return false;
        });

        // Next page button
        menu.replaceExistingItem(48, hasNext ?
                new CustomItemStack(Material.SPECTRAL_ARROW, "§bNext page (Store item)","§7Page: "+(page+1)+"/"+totalPages) :
                new CustomItemStack(Material.BARRIER, "§cAlready the last page (Store item)","§7Page: "+(page+1)+"/"+totalPages));

        menu.addMenuClickHandler(48, (player, slot, item, action) -> {
            if (hasNext) {
                data.setData("storage_page", String.valueOf(page + 1));
                updateStorageDisplay(menu, data);
                player.playSound(menu.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.5F, 1.0F);
            } else {
                player.playSound(menu.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
            }
            return false;
        });
    }

    // Get current page number
    private int getCurrentPage(SlimefunBlockData data) {
        try {
            String pageStr = data.getData("storage_page");
            if (pageStr != null) {
                return Math.max(0, Integer.parseInt(pageStr));
            }
        } catch (Exception ignored) {}
        return 0;
    }


    // Format big number display
    private String formatNumber(long n) {
        if (n >= 1_000_000_000) return (n / 1_000_000_000) + "B";
        if (n >= 1_000_000) return (n / 1_000_000) + "M";
        if (n >= 1_000) return (n / 1_000) + "K";
        return String.valueOf(n);
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
                        || Slimefun.getProtectionManager().hasPermission(p, b.getLocation(),
                        Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.INSERT) {
                    return getInputSlots();
                } else {
                    return getOutputSlots();
                }
            }
        };
    }

    protected void constructMenu(BlockMenuPreset preset) {

//        for (int i : pinkBorder ) {
//            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack (Material.PINK_STAINED_GLASS_PANE)), ColorGradient.getGradientName("Quick craft page turn")),
//                    (p, slot, item, action) -> false);
//        }

        for (int i : blueBorder) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack (Material.LIGHT_BLUE_STAINED_GLASS_PANE)), ColorGradient.getGradientName("Item storage page turn")),
                    (p, slot, item, action) -> false);
        }

        for (int i : arrowSlot) {
            preset.addItem(i, new CustomItemStack(new ItemStack (Material.BARRIER), " "),
                    (p, slot, item, action) -> false);
        }
        for (int i : inputOutputLine) {
            preset.addItem(i, new CustomItemStack(new ItemStack (Material.PINK_STAINED_GLASS_PANE), ColorGradient.getGradientName("====== Divider ======="),ColorGradient.getGradientName("←←←Input slot"),ColorGradient.getGradientName("Output slot→→→")),
                    (p, slot, item, action) -> false);
        }
        for (int i : storageSlots) {
            preset.addItem(i, new CustomItemStack(new ItemStack (Material.BARRIER), ColorGradient.getGradientName(" ")),
                    (p, slot, item, action) -> false);
        }
        for (int i : transportSlots) {
            preset.addItem(i, new CustomItemStack(new ItemStack (Material.BARRIER), ColorGradient.getGradientName(" ")),
                    (p, slot, item, action) -> false);
        }
        for (int i : transportSlots2) {
            preset.addItem(i, new CustomItemStack(new ItemStack (Material.BARRIER), ColorGradient.getGradientName(" ")),
                    (p, slot, item, action) -> false);
        }
        preset.addItem(27, new CustomItemStack(new ItemStack (Material.LIGHT_BLUE_STAINED_GLASS_PANE), ColorGradient.getGradientName("Item storage")),
                (p, slot, item, action) -> false);
        preset.addItem(40, new CustomItemStack(new ItemStack (Material.PINK_STAINED_GLASS_PANE),
                        ColorGradient.getGradientName("← Transfer the items out"),
                        ColorGradient.getGradientName("← Max 256 combos"),
                        ColorGradient.getGradientName("Transfer the items out →"),
                        ColorGradient.getGradientName("Max 512 coordinates →"),
                        ColorGradient.getGradientName("Click to open the transport overview")
                ),
                (p, slot, item, action) -> false);

        preset.addItem(49, new CustomItemStack(new ItemStack (Material.SOUL_LANTERN), ColorGradient.getGradientName("Magic Storage Terminal"),"§eFor stored items","§bLeft-click: take out 1 stack","§bRight-click: set continuous output","§bshift+Left-click: set max storage amount","§bshift+Right-click: remove amount limit",
                        "§eFor remote transfer","§bSee the specific description"),
                (p, slot, item, action) -> false);

    }



    @Override
    public @NotNull EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.NONE;
    }

    @Override
    public int getCapacity() {
        return 0;
    }











    /*
    Remote transfer related code
     */

    private static final int[] TARGET_SLOTS = {36, 38};
    private static final int[] CUSTOM_SLOTS = {37, 39};
    private static final int BUTTON_PREV_PAGE = 50;
    private static final int BUTTON_NEXT_PAGE = 51;

    private static final int PAIRS_PER_PAGE = 2; //Define how many coordinate template groups show per page
    private static final int MAX_BIND_PAIRS = 256; //Define the max number of coordinate template groups

    private static final int[] TRANSFER_AMOUNTS = {
            1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 3456
    };

    private int translateOutputPage = 0;


    private long getStoredItemCount(@Nonnull Block block, int index) {
        try {
            String countStr = BlockStorage.getLocationInfo(block.getLocation())
                    .getString("stored_item_count_" + index);
            return countStr != null ? Long.parseLong(countStr) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }



    public void updateTranslateOutPut(BlockMenu menu, Block b) {
        int totalPages = Math.max(1, (MAX_BIND_PAIRS + PAIRS_PER_PAGE - 1) / PAIRS_PER_PAGE);

        for (int i = 0; i < PAIRS_PER_PAGE; i++) {
            int pairIndex = translateOutputPage * PAIRS_PER_PAGE + i;
            int targetSlot = TARGET_SLOTS[i];
            int customSlot = CUSTOM_SLOTS[i];

            // ====== Target coordinates (36/38) ======
            String binding = BlockStorage.getLocationInfo(b.getLocation(), "output_bind_pair_" + pairIndex);
            boolean hasBinding = binding != null && !binding.isEmpty();

            ItemStack targetItem;
            if (hasBinding) {
                Location loc = parseLocation(binding);
                SlimefunItem sfItem = BlockStorage.check(loc);
                if (sfItem != null) {
                    targetItem = sfItem.getItem().clone();
                } else {
                    Block block = loc.getBlock();
                    if (block.getType() != Material.AIR) {
                        // Safely create an item stack, avoiding invalid item types
                        try {
                            // Check if the material is a valid item type
                            Material blockType = block.getType();
                            if (isValidItemType(blockType)) {
                                targetItem = new ItemStack(blockType);
                                  // Removed the blockstate fetch on the main thread    not very useful
//                                // 🔹 Fix: get it on the main thread BlockState
//                                if (Bukkit.isPrimaryThread()) {
//                                    // If on the main thread, get it directly
//                                    BlockState state = block.getState();
//                                    ItemMeta meta = targetItem.getItemMeta();
//                                    if (meta instanceof BlockStateMeta bsm) {
//                                        bsm.setBlockState(state);
//                                        targetItem.setItemMeta(bsm);
//                                    }
//                                } else {
//                                    // If on an async thread, get it synchronously
//                                    ItemStack finalTargetItem = targetItem;
//                                    Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> {
//                                        try {
//                                            BlockState state = block.getState();
//                                            ItemMeta meta = finalTargetItem.getItemMeta();
//                                            if (meta instanceof BlockStateMeta bsm) {
//                                                bsm.setBlockState(state);
//                                                finalTargetItem.setItemMeta(bsm);
//                                            }
//                                        } catch (IllegalStateException e) {
//                                            // If it still fails, log the error and use the default way
//                                            MagicExpansion.getInstance().getLogger().warning(
//                                                    "Can't get the block state at location: " + block.getLocation() +
//                                                            ", Type: " + block.getType()
//                                            );
//                                        }
//                                    });
//                                }
                            } else {
                                // For non-item blocks like wall signs, use a replacement display item
                                targetItem = getAlternativeDisplayItem(blockType);
                            }
                        } catch (IllegalArgumentException e) {
                            // If creating the item stack fails, use the default replacement item
                            targetItem = new ItemStack(Material.COMPASS);
                        }
                    } else {
                        targetItem = new ItemStack(Material.COMPASS);
                    }
                }
            } else {
                targetItem = new ItemStack(Material.RECOVERY_COMPASS);
            }

            ItemMeta targetMeta = targetItem.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(ItemStackHelper.getDisplayName(targetItem));
            if (targetMeta != null && targetMeta.hasLore()) {
                lore.addAll(targetMeta.getLore());
            }
            lore.add("");
            if (hasBinding) {
                String[] parts = binding.split(",", 4);
                lore.add("§bBound coordinates: §e" + parts[0] + ", " + parts[1] + ", " + parts[2]);
                lore.add("§bWorld: §e" + parts[3]);
            } else {
                lore.add("§7No target bound");
            }
            lore.add("");
            lore.add("§fLeft-click: hold [Void Touch] and click a block to bind");
            lore.add("§fRight-click: clear");

            if (targetMeta == null) {
                targetMeta = Bukkit.getItemFactory().getItemMeta(Material.RECOVERY_COMPASS);
            }
            targetMeta.setDisplayName("§eTarget coordinates");
            targetMeta.setLore(lore);
            targetItem.setItemMeta(targetMeta);
            menu.addItem(targetSlot, targetItem, (player, slot, clickedItem, action) -> {
                // 🔹 Right-click: clear binding
                if (action.isRightClicked()) {
                    // Clear the binding of this slot
                    BlockStorage.addBlockInfo(b.getLocation(), "output_bind_pair_" + pairIndex, "");
                    updateTranslateOutPut(menu, b);
                    player.sendMessage("§aBinding cleared #" + (pairIndex + 1));
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.5F, 0.5F);
                    return false;
                }

                // 🔹 Left-click: bind (must have VoidTouch on the cursor)
                if (!action.isRightClicked()) {
                    ItemStack cursor = player.getItemOnCursor();
                    if (cursor == null || cursor.getType().isAir()) {
                        player.sendMessage("§cPlease put §dVoid Touch §con your cursor to bind!");
                        return false;
                    }

                    SlimefunItem sfItem = SlimefunItem.getByItem(cursor);
                    if (!(sfItem instanceof VoidTouch)) {
                        player.sendMessage("§cPlease put §dVoid Touch §con your cursor to bind!");
                        return false;
                    }

                    ItemMeta meta = cursor.getItemMeta();
                    if (meta == null) {
                        player.sendMessage("§cThis item is missing metadata, can't read the binding info.");
                        return false;
                    }

                    PersistentDataContainer container = meta.getPersistentDataContainer();

                    // Use a constant or a shared definition Keys
                    NamespacedKey keyX = new NamespacedKey(MagicExpansion.getInstance(), "touch_x");
                    NamespacedKey keyY = new NamespacedKey(MagicExpansion.getInstance(), "touch_y");
                    NamespacedKey keyZ = new NamespacedKey(MagicExpansion.getInstance(), "touch_z");
                    NamespacedKey keyWorld = new NamespacedKey(MagicExpansion.getInstance(), "touch_world");

                    if (!container.has(keyX, PersistentDataType.INTEGER) ||
                            !container.has(keyY, PersistentDataType.INTEGER) ||
                            !container.has(keyZ, PersistentDataType.INTEGER) ||
                            !container.has(keyWorld, PersistentDataType.STRING)) {
                        player.sendMessage("§cError:§dVoid Touch §cNo coordinates bound!");
                        return false;
                    }

                    int x = container.get(keyX, PersistentDataType.INTEGER);
                    int y = container.get(keyY, PersistentDataType.INTEGER);
                    int z = container.get(keyZ, PersistentDataType.INTEGER);
                    String worldName = container.get(keyWorld, PersistentDataType.STRING);

                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        player.sendMessage("§cBound world §e" + worldName + " §cdoesn't exist.");
                        return false;
                    }

                    // ✅ Save binding
                    String newValue = x + "," + y + "," + z + "," + worldName;
                    BlockStorage.addBlockInfo(b.getLocation(), "output_bind_pair_" + pairIndex, newValue);

                    // ✅ Update UI
                    updateTranslateOutPut(menu, b);

                    // ✅ Feedback
                    player.sendMessage("§aSuccessfully bound the target location §e#" + (pairIndex + 1));
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_LAUNCH, 0.5F, 1.0F);

                    return false;
                }

                return false; // Ignore other clicks
            });



            // ✅ Use addItem! Simple and direct!
            // ====== Item template (37/39) ======
            ItemStack templateItem = getItemTemplate(b, pairIndex);
            boolean hasTemplate = templateItem != null;
            int amount = getTransferAmount(b, pairIndex);

            ItemStack displayItem = hasTemplate ? templateItem.clone() : new ItemStack(Material.GRAY_DYE);
            ItemMeta customMeta = displayItem.getItemMeta();
            if (customMeta == null) {
                customMeta = Bukkit.getItemFactory().getItemMeta(displayItem.getType());
            }

            List<String> customLore = new ArrayList<>();
            customLore.add("§r"+ItemStackHelper.getDisplayName(displayItem));
            if (customMeta.hasLore()) {
                customLore.addAll(customMeta.getLore());
            }
            customLore.add("");

            if (hasTemplate) {
                // 3. Get current stock from the main storage system (fixed the key issue)
                long currentStock = getStoredItemCountFromMainStorage(Objects.requireNonNull(StorageCacheUtils.getBlock(b.getLocation())), displayItem);
                customLore.add("§bStock: §e" + currentStock);
                customLore.add("");
                customLore.add("§bTransfer amount: §e" + amount);
                customLore.add("");
                customLore.add("§fLeft-click: switch amount");
                customLore.add("§fRight-click: clear template");
            } else {
                customLore.add("§7No item template");
                customLore.add("");
                customLore.add("§fLeft-click + Cursor item: set template");
            }

            customMeta.setDisplayName("§eTransfer item template");
            customMeta.setLore(customLore);
            displayItem.setItemMeta(customMeta);

            menu.addItem(customSlot, displayItem, (player, slot, clickedItem, action) -> {
                ItemStack cursor = player.getItemOnCursor();

                // 🔹 Right-click: clear template (whether there is one or not)
                if (action.isRightClicked()) {
                    if (hasTemplate) {
                        setItemTemplate(b, pairIndex, null);
                        updateTranslateOutPut(menu, b);
                        player.sendMessage("§aTemplate cleared #" + (pairIndex + 1));
                        player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_OFF, 0.3F, 0.5F);
                    } else {
                        player.sendMessage("§cThis slot has no template to clear.");
                    }
                    return false;
                }

                // 🔹 Left-click: two cases
                if (!action.isRightClicked()) {
                    // Case 1: no template yet → Set template with the cursor item
                    if (!hasTemplate) {
                        if (cursor == null || cursor.getType().isAir()) {
                            player.sendMessage("§cPut the item you want as the template on your cursor!");
                            return false;
                        }

                        ItemStack newTemplate = cursor.clone();
                        newTemplate.setAmount(1); // Template is always 1 item
                        setItemTemplate(b, pairIndex, newTemplate);
                        setTransferAmount(b, pairIndex, 1); // The default amount is1

                        updateTranslateOutPut(menu, b);
                        String name = newTemplate.hasItemMeta() && newTemplate.getItemMeta().hasDisplayName() ?
                                newTemplate.getItemMeta().getDisplayName() : newTemplate.getType().name().toLowerCase().replace('_', ' ');

                        player.sendMessage("§aTemplate set: §e" + name);
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 0.8F);
                        return false;
                    }

                    // Case 2: already has a template → Switch transfer amount
                    int current = getTransferAmount(b, pairIndex);
                    int nextIdx = 0;
                    for (int j = 0; j < TRANSFER_AMOUNTS.length; j++) {
                        if (current == TRANSFER_AMOUNTS[j]) {
                            nextIdx = (j + 1) % TRANSFER_AMOUNTS.length;
                            break;
                        }
                    }
                    int newAmount = TRANSFER_AMOUNTS[nextIdx];
                    setTransferAmount(b, pairIndex, newAmount);
                    updateTranslateOutPut(menu, b);

                    player.sendMessage("§eTransfer amount: §6" + newAmount);
                    player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.3F, 1.0F);
                    return false;
                }

                return false;
            });



        }

        // === Page buttons ===
        ItemStack nextItem = new ItemStack(Material.SPECTRAL_ARROW);
        ItemMeta nextMeta = nextItem.getItemMeta();
        nextMeta.setDisplayName("§eNext page"+" (Output)");
        nextMeta.setLore(List.of("§7Page: " + (translateOutputPage + 1) + "/" + totalPages));
        nextItem.setItemMeta(nextMeta);

        if (translateOutputPage >= totalPages - 1) {
            nextItem = new ItemStack(Material.BARRIER);
            ItemMeta barrierMeta = nextItem.getItemMeta();
            barrierMeta.setDisplayName("§cNo more pages"+" (Output)");
            nextItem.setItemMeta(barrierMeta);
        }

        menu.addItem(BUTTON_NEXT_PAGE, nextItem, (player, slot, item, action) -> {
            if (translateOutputPage < totalPages - 1) {
                translateOutputPage++;
                updateTranslateOutPut(menu, b);
                player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.3F, 0.6F);
            }
            return false;
        });

        ItemStack prevItem = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prevItem.getItemMeta();
        prevMeta.setDisplayName("§ePrevious page"+" (Input)");
        prevMeta.setLore(List.of("§7Page: " + (translateOutputPage + 1) + "/" + totalPages));
        prevItem.setItemMeta(prevMeta);

        if (translateOutputPage <= 0) {
            prevItem = new ItemStack(Material.BARRIER);
            ItemMeta barrierMeta = prevItem.getItemMeta();
            barrierMeta.setDisplayName("§cAlready the first page"+" (Input)");
            prevItem.setItemMeta(barrierMeta);
        }

        menu.addItem(BUTTON_PREV_PAGE, prevItem, (player, slot, item, action) -> {
            if (translateOutputPage > 0) {
                translateOutputPage--;
                updateTranslateOutPut(menu, b);
                player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.3F, 0.6F);
            }
            return false;
        });
    }

    // ====== Helper method ======

    /**
     * Check if the material is a valid item type
     */
    private boolean isValidItemType(Material material) {
        try {
            // Try to create an item stack to verify
            new ItemStack(material);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get a replacement display item for invalid item types
     */
    private ItemStack getAlternativeDisplayItem(Material originalType) {
        // Try removing the WALL_ prefix first to get the normal version
        if (originalType.name().startsWith("WALL_")) {
            String baseName = originalType.name().replace("WALL_", "");
            try {
                Material baseMaterial = Material.valueOf(baseName);
                if (isValidItemType(baseMaterial)) {
                    return new ItemStack(baseMaterial);
                }
            } catch (IllegalArgumentException ignored) {
                // If conversion fails, keep handling the special case
            }

            // If it's invalid after removing the prefix, handle the special case
            if (originalType.name().contains("SIGN")) {
                return new ItemStack(Material.OAK_SIGN);
            }
            if (originalType.name().contains("BANNER")) {
                return new ItemStack(Material.BLACK_BANNER);
            }
        }

        // Default safe replacement
        return new ItemStack(Material.COMPASS);
    }


    /**
     * Provide a safe replacement display item for invalid item types
     */
    private ItemStack getSafeAlternativeItem(Material originalType) {
        // Try removing the WALL_ prefix first to get the normal version
        if (originalType.name().startsWith("WALL_")) {
            String baseName = originalType.name().replace("WALL_", "");
            try {
                Material baseMaterial = Material.valueOf(baseName);
                if (isValidItemType(baseMaterial)) {
                    return new ItemStack(baseMaterial);
                }
            } catch (IllegalArgumentException ignored) {
                // If conversion fails, keep handling the special case
            }

            // If it's invalid after removing the prefix, handle the special case
            if (originalType.name().contains("SIGN")) {
                return new ItemStack(Material.OAK_SIGN);
            }
            if (originalType.name().contains("BANNER")) {
                return new ItemStack(Material.BLACK_BANNER);
            }
        }

        // Default safe replacement
        return new ItemStack(Material.COMPASS);
    }


    private Location parseLocation(String str) {
        try {
            String[] parts = str.split(",", 4);
            World world = Bukkit.getWorld(parts[3]);
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            return new Location(world, x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack getItemTemplate(Block b, int index) {
        // Read config from BlockStorage
        Config config = BlockStorage.getLocationInfo(b.getLocation());
        String key = "template_item_" + index;

        if (!config.contains(key)) {
            return null;
        }

        // Deserialize from Base64 ItemStack
        return SameItemJudge.itemFromBase64(config.getString(key));
    }

    private void setItemTemplate(Block b, int index, ItemStack item) {
        String key = "template_item_" + index;

        if (item == null || item.getType().isAir()) {
            // Clear template
            BlockStorage.addBlockInfo(b.getLocation(), key, ""); // An empty string means clear
            return;
        }

        try {
            String base64 = SameItemJudge.itemToBase64(item);
            if (base64 != null && !base64.isEmpty()) {
                BlockStorage.addBlockInfo(b.getLocation(), key, base64);
            } else {
//                Debug.logWarn("Can't serialize the template item #" + index + " is Base64");
            }
        } catch (Exception e) {
//            Debug.logWarn("Failed to serialize template item: " + e.getMessage());
        }
    }

    private int getTransferAmount(Block b, int index) {
        String amountStr = BlockStorage.getLocationInfo(b.getLocation(), "output_amount_" + index);
        try {
            return amountStr != null ? Integer.parseInt(amountStr) : 64;
        } catch (NumberFormatException e) {
            return 64;
        }
    }

    private void setTransferAmount(Block b, int index, int amount) {
        BlockStorage.addBlockInfo(b.getLocation(), "output_amount_" + index, String.valueOf(amount));
    }



    /*
    Transfer out - fixed version
     */

    private static final int MAX_BATCH_SIZE = 64;

    private void handleAllTemplateTransfers(Block b) {
        SlimefunBlockData data = StorageCacheUtils.getBlock(b.getLocation());
        if (data == null) return;

        for (int pairIndex = 0; pairIndex < MAX_BIND_PAIRS; pairIndex++) {
            ItemStack templateItem = getItemTemplate(b, pairIndex);
            if (templateItem != null && !templateItem.getType().isAir()) {
                transferTemplateItem(b, pairIndex, data);
            }

            /*This part of the code only applies to vanilla containers*/
//            if (templateItem != null && !templateItem.getType().isAir()) {
////                transferToVanillaContainer(b, pairIndex, data);
//                if (Bukkit.isPrimaryThread()) {
//                    transferToVanillaContainer(b, pairIndex, data);
//                } else {
//                    int finalPairIndex = pairIndex;
//                    Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> {
//                        transferToVanillaContainer(b, finalPairIndex, data);
//                    });
//                }
//            }
            /*This part of the code only applies to vanilla containers*/


        }
    }

    /**
     * Transfer items to vanilla container
     */
    private void transferToVanillaContainer(Block sourceBlock, int pairIndex, SlimefunBlockData data) {
        // Get target location
        Location targetLocation = getTargetLocation(sourceBlock, pairIndex);
        if (targetLocation == null) {
            return;
        }

        // If it's a Slimefun block, don't handle it
        SlimefunItem sfItem = StorageCacheUtils.getSfItem(targetLocation);
        if (null != sfItem) return;

        // Get template item
        ItemStack template = getItemTemplate(sourceBlock, pairIndex);
        if (template == null || template.getType().isAir()) return;

        // Get transfer amount
        int configuredAmount = getTransferAmount(sourceBlock, pairIndex);
        if (configuredAmount <= 0) {
            return;
        }

        // Get current stock from the main storage system
        long currentStock = getStoredItemCountFromMainStorage(data, template);
        if (currentStock <= 0) {
            return;
        }

//        // Check if the target location is a vanilla container
//        if (!isVanillaContainer(targetLocation)) {
//            return;
//        }

        // Get target container
        Block targetBlock = targetLocation.getBlock();
        BlockState blockState = targetBlock.getState();

        if (!(blockState instanceof InventoryHolder)) {
            return;
        }

        InventoryHolder holder = (InventoryHolder) blockState;
        Inventory inventory = holder.getInventory();

        // Calculate the actual transfer amount
        int actualTransferAmount = (int) Math.min(currentStock, configuredAmount);

        // Predict how many items the container can take
        int maxFit = predictMaxFitVanilla(inventory, template, actualTransferAmount);
        if (maxFit <= 0) {
            return;
        }

        // Final transfer amount takes the smaller value
        int finalTransferAmount = Math.min(actualTransferAmount, maxFit);

        // Create the items to transfer
        ItemStack toTransfer = template.clone();
        toTransfer.setAmount(finalTransferAmount);

        // Try to put the item into the container
        HashMap<Integer, ItemStack> leftover = inventory.addItem(toTransfer);

        // Calculate the amount actually transferred
        int actuallyTransferred = finalTransferAmount;
        if (!leftover.isEmpty()) {
            for (ItemStack left : leftover.values()) {
                actuallyTransferred -= left.getAmount();
            }
        }

        if (actuallyTransferred > 0) {
            // Deduct the transferred items from storage
            deductStoredItemFromMainStorage(data, template, actuallyTransferred);

            // Show transfer effect
            showTransferParticles(sourceBlock.getLocation(), targetLocation, Particle.HAPPY_VILLAGER);
        }
    }

    /**
     * Predict how many items a vanilla container can take at most
     */
    private int predictMaxFitVanilla(Inventory inventory, ItemStack prototype, int maxAmount) {
        ItemStack singleItem = prototype.clone();
        singleItem.setAmount(1);

        int totalFit = 0;

        // Go through all slots
        for (int i = 0; i < inventory.getSize(); i++) {
            if (totalFit >= maxAmount) break;

            ItemStack existing = inventory.getItem(i);

            if (existing == null || existing.getType().isAir()) {
                // Empty slot, can hold a whole stack
                totalFit += Math.min(prototype.getMaxStackSize(), maxAmount - totalFit);
            } else if (existing.isSimilar(singleItem)) {
                // Same item, work out the remaining space
                int space = existing.getMaxStackSize() - existing.getAmount();
                totalFit += Math.min(space, maxAmount - totalFit);
            }
        }

        return totalFit;
    }

    /**
     * Do one item push (fixed version)
     */
    public void transferTemplateItem(@Nonnull Block sourceBlock, int pairIndex, @Nonnull SlimefunBlockData data) {
        // 1. Get template item
        ItemStack template = getItemTemplate(sourceBlock, pairIndex);
        if (template == null || template.getType().isAir()) return;

        // 2. Get the set push amount
        int configuredAmount = getTransferAmount(sourceBlock, pairIndex);
        if (configuredAmount <= 0) return;

        // 3. Get current stock from the main storage system
        long currentStock = getStoredItemCountFromMainStorage(data, template);
        if (currentStock <= 0) return;

        // 4. First find the matching data slot index
        int targetSlot = findMatchingSlot(data, template);
        if (targetSlot == -1) return;

        // 5. Get target location
        Location targetLocation = getTargetLocation(sourceBlock, pairIndex);
        if (targetLocation == null) return;

        // 6. Check if the target location is valid
        if (!isValidTarget(targetLocation)) {
            // Target is invalid, clear the binding
//            data.setData("output_bind_pair_" + pairIndex, "");
            return;
        }

        // 7. Predict how much the target can take at most
        int maxFit = predictMaxFit(targetLocation, template, configuredAmount);
        if (maxFit <= 0) return;

        // 8. Calculate the amount to actually transfer
        // Actual transferred amount = min(Set amount, Current stock, Amount the target can receive)
        int actualTransferAmount = (int) Math.min(Math.min(configuredAmount, currentStock), maxFit);
        if (actualTransferAmount <= 0) return;

        // 9. [Deduct stock from the main storage system] - fix key issue
        long deducted = deductStoredItemFromMainStorage(data, template, actualTransferAmount);
        if (deducted <= 0) return;

        // 10. Run push
        int actualPushed = pushItemsToLocation(sourceBlock, targetLocation, template, (int) deducted);

        // 11. Play sound and particle effects
        if (actualPushed > 0) {
            showTransferParticles(sourceBlock.getLocation(), targetLocation, Particle.END_ROD);
        }

        // 12. If the amount actually pushed is less than the amount deducted, return the difference to storage
        if (deducted > actualPushed) {
            long refundAmount = deducted - actualPushed;
            refundToMainStorage(data, template, refundAmount);
        }
    }

    /**
     * Get item amount from the main storage system (fixed the key issue)
     */
    private long getStoredItemCountFromMainStorage(@Nonnull SlimefunBlockData data, @Nonnull ItemStack template) {
        if (template.getType() == Material.AIR) return 0;

        ItemStack prototype = template.clone();
        prototype.setAmount(1);

        long totalCount = 0;

        // Go through all slots and add up the amount of matching items
        for (int i = 0; i < MAX_STORED_ITEMS; i++) {
            String jsonData = data.getData("item_type_" + i);
            if (jsonData == null || jsonData.isEmpty()) continue;

            try {
                ItemStack storedItem = itemFromBase64(jsonData);
                if (storedItem != null && SameItemJudge.isSimilarSafe(prototype, storedItem)) {
//                if (storedItem != null && SlimefunUtils.isItemSimilar(storedItem, prototype, true)) {
                    String countStr = data.getData("item_count_" + i);
                    if (countStr != null && !countStr.isEmpty()) {
                        try {
                            totalCount += Long.parseLong(countStr);
                        } catch (NumberFormatException e) {
                            // Ignore conversion errors
                        }
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }

        return totalCount;
    }

    /**
     * Take items out of the main storage system (fixed the key issue)
     */
    /**
     * Take items out of the main storage system - fixed version
     */
    private long deductStoredItemFromMainStorage(@Nonnull SlimefunBlockData data, @Nonnull ItemStack template, long amount) {
        if (template.getType() == Material.AIR || amount <= 0) return 0;

        ItemStack prototype = template.clone();
        prototype.setAmount(1);

        // Find the matching storage slot
        for (int i = 0; i < MAX_STORED_ITEMS; i++) {
            String jsonData = data.getData("item_type_" + i);
            if (jsonData == null || jsonData.isEmpty()) continue;

            try {
                ItemStack storedItem = itemFromBase64(jsonData);
                if (storedItem != null && SameItemJudge.isSimilarSafe(prototype, storedItem)) {
//                if (storedItem != null && SlimefunUtils.isItemSimilar(storedItem, prototype, true)) {
                    String countStr = data.getData("item_count_" + i);
                    if (countStr == null || countStr.isEmpty()) continue;

                    try {
                        long currentCount = Long.parseLong(countStr);
                        long toDeduct = Math.min(currentCount, amount);

                        if (toDeduct <= 0) return 0;

                        long newCount = currentCount - toDeduct;

                        // Check if there's a max amount limit
                        String maxStr = data.getData("item_max_" + i);
                        long maxCount = -1;
                        if (maxStr != null && !maxStr.isEmpty()) {
                            try {
                                maxCount = Long.parseLong(maxStr);
                            } catch (Exception ignored) {}
                        }

                        if (newCount <= 0) {
                            // Only clean up the slot if no max amount limit is set
                            if (maxCount == -1) {
                                data.removeData("item_type_" + i);
                                data.removeData("item_count_" + i);
                                data.removeData("item_max_" + i);
                            } else {
                                // A max amount limit is set, keep the slot, set the amount to0
                                data.setData("item_count_" + i, "0");
                            }
                        } else {
                            data.setData("item_count_" + i, String.valueOf(newCount));
                        }

                        return toDeduct;
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
        return 0;
    }

    /**
     * Return items to the main storage system
     */
    private void refundToMainStorage(@Nonnull SlimefunBlockData data, @Nonnull ItemStack template, long amount) {
        if (amount <= 0) return;

        // Call the existing storeItem method directly
        ItemStack refundStack = template.clone();
        refundStack.setAmount((int) Math.min(amount, Integer.MAX_VALUE));
        storeItemCargoCoreMore(data, refundStack);
    }

    /**
     * Get target location (fixed key name matching issue)
     */
    private Location getTargetLocation(Block block, int index) {
        // Use the correct key name: output_bind_pair_ instead of target_location_
        String locStr = BlockStorage.getLocationInfo(block.getLocation()).getString("output_bind_pair_" + index);
        if (locStr == null || locStr.isEmpty()) return null;

        String[] parts = locStr.split(",");
        if (parts.length != 4) return null;

        try {
            World world = Bukkit.getWorld(parts[3]);
            if (world == null) return null;

            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);

            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Check if the target location is valid
     */
    private boolean isValidTarget(Location location) {
        if (location == null) return false;

        World world = location.getWorld();
        if (world == null) return false;

        // Check if the chunk is loaded
        if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return false;
        }

        // Check if the target block exists and is a Slimefun block
        Block targetBlock = location.getBlock();
        return BlockStorage.check(targetBlock) != null;
    }

    /**
     * Predict how much the target can take at most (optimized version)
     */
    private int predictMaxFit(Location targetLocation, ItemStack prototype, int maxAmount) {
        Block targetBlock = targetLocation.getBlock();
        BlockMenu targetMenu = BlockStorage.getInventory(targetBlock);

        if (targetMenu == null) return 0;

        int[] inputSlots = targetMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.INSERT);
        if (inputSlots == null || inputSlots.length == 0) return 0;

        int totalFit = 0;
        ItemStack singleItem = prototype.clone();
        singleItem.setAmount(1);

        for (int slot : inputSlots) {
            if (totalFit >= maxAmount) break;

            ItemStack existing = targetMenu.getItemInSlot(slot);
            ItemStack existing2 = null;
            if (existing != null) {
                existing2 = targetMenu.getItemInSlot(slot).clone();
                existing2.setAmount(1);
            }
            if (existing == null || existing.getType().isAir()) {
                // Empty slot, can hold a whole stack
                totalFit += Math.min(prototype.getMaxStackSize(), maxAmount - totalFit);
            } else if (SameItemJudge.isSimilarSafe(singleItem, existing2)) {
//            } else if (SlimefunUtils.isItemSimilar(singleItem, existing2, true)) {
                // Same item, work out the remaining space
                int space = existing.getMaxStackSize() - existing.getAmount();
                totalFit += Math.min(space, maxAmount - totalFit);
            }
        }
        return totalFit;
    }

    /**
     * Push items to the target (optimized version)
     */
    private int pushItemsToLocation(Block sourceBlock, Location targetLocation, ItemStack prototype, int requestAmount) {
        Block targetBlock = targetLocation.getBlock();
        BlockMenu targetMenu = BlockStorage.getInventory(targetBlock);

        if (targetMenu == null) return 0;

        int[] inputSlots = targetMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.INSERT);
        if (inputSlots == null || inputSlots.length == 0) return 0;

        int totalPushed = 0;
        int remaining = requestAmount;

        while (remaining > 0) {
            int batchSize = Math.min(MAX_BATCH_SIZE, remaining);
            ItemStack toPush = prototype.clone();
            toPush.setAmount(batchSize);

            ItemStack leftover;
            try {
                leftover = targetMenu.pushItem(toPush, inputSlots);
            } catch (Exception e) {
//                Debug.logInfo("Error while pushing items to the target: " + e.getMessage());
                break;
            }

            int pushed = batchSize;
            if (leftover != null) {
                pushed = batchSize - leftover.getAmount();
            }

            totalPushed += pushed;
            remaining -= pushed;

            if (pushed == 0 || leftover != null && leftover.getAmount() > 0) {
                // Can't push any more items
                break;
            }
        }

        return totalPushed;
    }

    /**
     * Show transfer particle effect
     */
    private void showTransferParticles(Location from, Location to, Particle particle) {
        World world = from.getWorld();
        if (world == null) return;

        // Show particles at the start point
        world.spawnParticle(Particle.PORTAL, from.clone().add(0.5, 1, 0.5), 10, 0.3, 0.3, 0.3, 0.1);

        // Show particles at the end point
        world.spawnParticle(Particle.REVERSE_PORTAL, to.clone().add(0.5, 1, 0.5), 10, 0.3, 0.3, 0.3, 0.1);
        // Check if the two locations are in the same world
        if (!from.getWorld().equals(to.getWorld())) {
            // If they're not in the same world, you can skip the particle effect or handle it another way
            return; // In this example just skip generating the particle effect
        }
        // Show connection line (simple version)
        double distance = from.distance(to);
        if (distance < 20) { // Only show the line for short distances
            int points = (int) (distance * 2);
            for (int i = 0; i <= points; i++) {
                double ratio = (double) i / points;
                Location point = from.clone().add(
                        (to.getX() - from.getX()) * ratio,
                        (to.getY() - from.getY()) * ratio + Math.sin(ratio * Math.PI) * 2,
                        (to.getZ() - from.getZ()) * ratio
                );
                world.spawnParticle(particle, point, 1, 0, 0, 0, 0);
            }
        }
    }


















    /*
    Input transfer - extract items from other machines' output slots
     */

    private static final int[] INPUT_BIND_SLOTS = {41, 42, 43, 44}; // Input binding slot
    private static final int INPUT_PAIRS_PER_PAGE = 4; // Show 4 groups per page (matching 4 slots)
    private static final int MAX_INPUT_BIND_PAIRS = 512; // Max input binding count
    private int inputBindPage = 0; // Input binding current page number

    /**
     * Handle all input transfers (pull items from bound machines) - fixed version
     */
    private void handleAllInputTransfers(Block b, SlimefunBlockData data) {
        for (int pairIndex = 0; pairIndex < MAX_INPUT_BIND_PAIRS; pairIndex++) {
            Location sourceLocation = getInputSourceLocation(b, pairIndex);
            if (sourceLocation != null && isValidInputSource(sourceLocation)) {
                transferFromSource(b, data, sourceLocation, pairIndex);
            }
        }
    }

    /**
     * Check if the input source is valid
     */
    private boolean isValidInputSource(Location location) {
        if (location == null) return false;

        World world = location.getWorld();
        if (world == null) return false;

        // Check if the chunk is loaded
        if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return false;
        }

        // Check if the target block exists and is a Slimefun block
        Block targetBlock = location.getBlock();
        SlimefunItem sfItem = BlockStorage.check(targetBlock);
        if (sfItem == null) {
//            Debug.logInfo("Input source is not a Slimefun block: " + location);
            return false;
        }

        // Check if there are valid item slots
        BlockMenu sourceMenu = BlockStorage.getInventory(targetBlock);
        if (sourceMenu == null) {
//            Debug.logInfo("Input source has no valid menu: " + location);
            return false;
        }

        return true;
    }

    /**
     * Pull items from the source machine into this machine's storage - fixed version
     */
    private void transferFromSource(Block destBlock, SlimefunBlockData data, Location sourceLocation, int pairIndex) {
        Block sourceBlock = sourceLocation.getBlock();
        BlockMenu sourceMenu = BlockStorage.getInventory(sourceBlock);

        if (sourceMenu == null) {
//            Debug.logInfo("The source machine menu isnull: " + sourceLocation);
            return;
        }

        // Debug info: print source machine info
//        SlimefunItem sourceSfItem = BlockStorage.check(sourceBlock);
//        Debug.logInfo("Try to extract from the source machine: " + (sourceSfItem != null ? sourceSfItem.getId() : "Unknown") + " Location: " + sourceLocation);
//
        // Method 1: try to get the output slot first
        int[] outputSlots = sourceMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
//        Debug.logInfo("Method 1 - output slot amount: " + (outputSlots != null ? outputSlots.length : 0));

        if (outputSlots != null && outputSlots.length > 0) {
            // Extract from output slot
            extractFromOutputSlots(sourceMenu, outputSlots, data, pairIndex, destBlock);
            return;
        }

        // Method 2: if there's no clear output slot, try all slots (except certain slot types)
//        Debug.logInfo("Method 1 failed, try method 2 - scan all slots");
//        extractFromAllSlots(sourceMenu, data, pairIndex, destBlock, sourceSfItem);
    }

    /**
     * Extract items from output slot
     */
    private void extractFromOutputSlots(BlockMenu sourceMenu, int[] outputSlots, SlimefunBlockData destData, int pairIndex, Block destBlock) {
//        Debug.logInfo("Start extracting from the output slot, slot: " + Arrays.toString(outputSlots));
        boolean hasExtracted = false;

        for (int outputSlot : outputSlots) {
            ItemStack itemToExtract = sourceMenu.getItemInSlot(outputSlot);
            if (itemToExtract == null || itemToExtract.getType() == Material.AIR) {
//                Debug.logInfo("Slot " + outputSlot + " is empty");
                continue;
            }

//            Debug.logInfo("Found item: " + itemToExtract.getType() + " Amount: " + itemToExtract.getAmount() + " in slot: " + outputSlot);

            // Check filter template
            ItemStack filterTemplate = getInputFilterTemplate(destBlock, pairIndex);
            if (filterTemplate != null && !filterTemplate.getType().isAir()) {
                if (!isItemMatchFilter(itemToExtract, filterTemplate)) {
//                    Debug.logInfo("Item doesn't match the filter template, skip");
                    continue;
                }
            }

            // Check amount limit
            if (!canStoreMore(destData, itemToExtract, itemToExtract.getAmount())) {
                continue; // Can't store more, skip
            }

            // Try to extract items
            int extractedAmount = extractItemDirectly(sourceMenu, outputSlot, itemToExtract, destData);
//            Debug.logInfo("Extract result: " + extractedAmount + " items");

            if (extractedAmount > 0) {
                // Play extract sound
//                destBlock.getWorld().playSound(
//                        destBlock.getLocation(),
//                        Sound.ENTITY_ENDER_EYE_LAUNCH,
//                        0.2F,
//                        1.0F
//                );
                hasExtracted = true;
            }

        }
        if (hasExtracted) {
            // Show particle effect
            showTransferParticles(sourceMenu.getLocation(), destBlock.getLocation(), Particle.DRAGON_BREATH);
        }

    }


    /**
     * Extract items directly - fixed version (uses a simpler, more direct method)
     */
    private int extractItemDirectly(BlockMenu sourceMenu, int sourceSlot, ItemStack sourceItem, SlimefunBlockData destData) {
        if (sourceItem == null || sourceItem.getType() == Material.AIR) {
//            Debug.logInfo("Source item is empty");
            return 0;
        }

        // Calculate the max extract amount (up to 64 each time)
        int maxExtract = Math.min(sourceItem.getAmount(), 64);
//        Debug.logInfo("Getting ready to extract " + maxExtract + " items");

        // Create the items to extract
        ItemStack toExtract = sourceItem.clone();

        SlimefunItem sfToExtract = SlimefunItem.getByItem(toExtract);
        if (sfToExtract instanceof CargoFragment) {
            return 0;
        }

        toExtract.setAmount(maxExtract);

        // Store directly into the target storage system
        storeItemCargoCoreMore(destData, toExtract);
//        Debug.logInfo("Item stored in the target storage");

        // Update source slot: reduce the amount or empty it
        int newAmount = sourceItem.getAmount() - maxExtract;
//        Debug.logInfo("New source slot amount: " + newAmount);

        if (newAmount <= 0) {
            // Fully extracted, clear the slot
            sourceMenu.replaceExistingItem(sourceSlot, null);
//            Debug.logInfo("Clear source slot");
        } else {
            // Update amount
            ItemStack updatedItem = sourceItem.clone();
            updatedItem.setAmount(newAmount);
            sourceMenu.replaceExistingItem(sourceSlot, updatedItem);
//            Debug.logInfo("Update source slot amount");
        }

//        Debug.logInfo("Successfully extracted " + maxExtract + " x " + sourceItem.getType());
        return maxExtract;
    }

    /**
     * Check if the item matches the filter template
     */
    private boolean isItemMatchFilter(ItemStack item, ItemStack filter) {
        if (item == null || filter == null) return false;

        ItemStack itemCopy = item.clone();
        itemCopy.setAmount(1);
        ItemStack filterCopy = filter.clone();
        filterCopy.setAmount(1);

        boolean isSimilar = SameItemJudge.isSimilarSafe(itemCopy, filterCopy);
//        boolean isSimilar = SlimefunUtils.isItemSimilar(itemCopy, filterCopy, true);
//        Debug.logInfo("Item match check: " + item.getType() + " vs " + filter.getType() + " = " + isSimilar);

        return isSimilar;
    }

    /**
     * Get input source location
     */
    private Location getInputSourceLocation(Block block, int index) {
        String locStr = BlockStorage.getLocationInfo(block.getLocation()).getString("input_bind_pair_" + index);
        if (locStr == null || locStr.isEmpty()) {
//            Debug.logInfo("Input source binding " + index + " is empty");
            return null;
        }

        String[] parts = locStr.split(",");
        if (parts.length != 4) {
//            Debug.logInfo("Input source coordinate format is wrong: " + locStr);
            return null;
        }

        try {
            World world = Bukkit.getWorld(parts[3]);
            if (world == null) {
//                Debug.logInfo("World doesn't exist: " + parts[3]);
                return null;
            }

            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);

            Location location = new Location(world, x, y, z);
//            Debug.logInfo("Parse input source location: " + location);
            return location;
        } catch (NumberFormatException e) {
//            Debug.logInfo("Coordinate parsing error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get input filter template
     */
    private ItemStack getInputFilterTemplate(Block block, int index) {
        Config config = BlockStorage.getLocationInfo(block.getLocation());
        String key = "input_filter_template_" + index;

        if (!config.contains(key)) {
//            Debug.logInfo("Filter template " + index + " doesn't exist");
            return null;
        }

        String base64 = config.getString(key);
        if (base64 == null || base64.isEmpty()) {
//            Debug.logInfo("Filter template " + index + " Data is empty");
            return null;
        }

        ItemStack template = SameItemJudge.itemFromBase64(base64);
//        Debug.logInfo("Get filter template " + index + ": " + (template != null ? template.getType() : "null"));
        return template;
    }

    /**
     * Set input filter template
     */
    private void setInputFilterTemplate(Block block, int index, ItemStack item) {
        String key = "input_filter_template_" + index;

        if (item == null || item.getType().isAir()) {
            BlockStorage.addBlockInfo(block.getLocation(), key, "");
//            Debug.logInfo("Clear filter template " + index);
            return;
        }

        try {
            String base64 = SameItemJudge.itemToBase64(item);
            if (base64 != null && !base64.isEmpty()) {
                BlockStorage.addBlockInfo(block.getLocation(), key, base64);
//                Debug.logInfo("Set filter template " + index + ": " + item.getType());
            } else {
//                Debug.logInfo("Failed to serialize the filter template");
            }
        } catch (Exception e) {
//            Debug.logInfo("Failed to set filter template: " + e.getMessage());
//            Debug.logInfo("Failed to set input filter template: " + e.getMessage());
        }
    }
    /**
     * Update input binding UI
     */
    private void updateInputBindDisplay(BlockMenu menu, Block b) {
        menu.addItem(40, new CustomItemStack(new ItemStack (Material.PINK_STAINED_GLASS_PANE),
                        ColorGradient.getGradientName("← Transfer the items out"),
                        ColorGradient.getGradientName("← Max 256 combos"),
                        ColorGradient.getGradientName("Transfer the items out →"),
                        ColorGradient.getGradientName("Max 512 coordinates →"),
                        ColorGradient.getGradientName("Click to open the transport overview")),
                (player, slot, itemStack, clickAction) -> {

                    openTransportOverviewMenu(player, b);; // Open the first page by default
                    return false; // Don't consume the item or do the default behavior
                });
        int totalPages = Math.max(1, (MAX_INPUT_BIND_PAIRS + INPUT_PAIRS_PER_PAGE - 1) / INPUT_PAIRS_PER_PAGE);

        for (int i = 0; i < INPUT_PAIRS_PER_PAGE; i++) {
            int pairIndex = inputBindPage * INPUT_PAIRS_PER_PAGE + i;
            int bindSlot = INPUT_BIND_SLOTS[i];

            // ====== Input source binding display ======
            String binding = BlockStorage.getLocationInfo(b.getLocation(), "input_bind_pair_" + pairIndex);
            boolean hasBinding = binding != null && !binding.isEmpty();

            ItemStack bindItem;
            if (hasBinding) {
                Location loc = parseLocation(binding);
                SlimefunItem sfItem = BlockStorage.check(loc);
                if (sfItem != null) {
                    bindItem = sfItem.getItem().clone();
                } else {
                    Block block = loc.getBlock();
                    if (block.getType() != Material.AIR) {
                        // Safely create an item stack
                        try {
                            Material blockType = block.getType();
                            // Check if it's a valid item type
                            if (isValidItemType(blockType)) {
                                bindItem = new ItemStack(blockType);
                                // Removed the blockstate fetch on the main thread    not very useful
                                // 🔹 Get BlockState synchronously - must be sync
//                                if (Bukkit.isPrimaryThread()) {
//                                    BlockState state = block.getState();
//                                    ItemMeta meta = bindItem.getItemMeta();
//                                    if (meta instanceof BlockStateMeta bsm) {
//                                        bsm.setBlockState(state);
//                                        bindItem.setItemMeta(bsm);
//                                    }
//                                } else {
//                                    // If on an async thread, get it synchronously
//                                    ItemStack finalBindItem = bindItem;
//                                    Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> {
//                                        try {
//                                            BlockState state = block.getState();
//                                            ItemMeta meta = finalBindItem.getItemMeta();
//                                            if (meta instanceof BlockStateMeta bsm) {
//                                                bsm.setBlockState(state);
//                                                finalBindItem.setItemMeta(bsm);
//                                            }
//                                        } catch (IllegalStateException e) {
//                                            // If it still fails, log the error and use the default way
//                                            MagicExpansion.getInstance().getLogger().warning(
//                                                    "Can't get the block state at location: " + block.getLocation() +
//                                                            ", Type: " + block.getType()
//                                            );
//                                        }
//                                    });
//                                }
                            } else {
                                // For non-item blocks like wall signs, use a safe replacement
                                bindItem = getSafeAlternativeItem(blockType);
                            }
                        } catch (IllegalArgumentException e) {
                            // If creation fails, use the default safe item
                            bindItem = new ItemStack(Material.COMPASS);
                        }
                    } else {
                        bindItem = new ItemStack(Material.COMPASS);
                    }
                }
            } else {
                bindItem = new ItemStack(Material.RECOVERY_COMPASS);
            }


            ItemMeta bindMeta = bindItem.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(ItemStackHelper.getDisplayName(bindItem));
            if (bindMeta != null && bindMeta.hasLore()) {
                lore.addAll(bindMeta.getLore());
            }
            lore.add("");

            if (hasBinding) {
                String[] parts = binding.split(",", 4);
                lore.add("§aInput source coordinates: §e" + parts[0] + ", " + parts[1] + ", " + parts[2]);
                lore.add("§aWorld: §e" + parts[3]);
                Location loc = parseLocation(binding);
                BlockMenu sourceMenu = StorageCacheUtils.getMenu(loc);
                if (sourceMenu != null) {
                    int[] outputSlots = sourceMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
                    lore.add("§bOutput slot count: §e" + (outputSlots != null ? outputSlots.length : 0));
                }
                // Show filter template info
                ItemStack filterTemplate = getInputFilterTemplate(b, pairIndex);
                if (filterTemplate != null && !filterTemplate.getType().isAir()) {
                    lore.add("§6Filter template: §e" + ItemStackHelper.getDisplayName(filterTemplate));
                } else {
                    lore.add("§7No filter template (extract all items)");
                }
            } else {
                lore.add("§7No input source bound");
            }

            lore.add("");
            lore.add("§fLeft-click: hold [Void Touch] and click a block to bind");
            lore.add("§fRight-click: clear binding");
            lore.add("§fShift+Left-click: set filter template");
            lore.add("§fShift+Right-click: clear filter template");

            if (bindMeta == null) {
                bindMeta = Bukkit.getItemFactory().getItemMeta(bindItem.getType());
            }
            bindMeta.setDisplayName("§aInput source binding #" + (pairIndex + 1));
            bindMeta.setLore(lore);
            bindItem.setItemMeta(bindMeta);

            menu.addItem(bindSlot, bindItem, (player, slot, clickedItem, action) -> {
                // 🔹 Right-click: clear binding
                if (action.isRightClicked() && !action.isShiftClicked()) {
                    BlockStorage.addBlockInfo(b.getLocation(), "input_bind_pair_" + pairIndex, "");
                    setInputFilterTemplate(b, pairIndex, null); // Also clear filter template
                    updateInputBindDisplay(menu, b);
                    player.sendMessage("§aInput source binding cleared #" + (pairIndex + 1));
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.5F, 0.5F);
                    return false;
                }

                // 🔹 Shift+Right-click: clear filter template
                if (action.isRightClicked() && action.isShiftClicked()) {
                    setInputFilterTemplate(b, pairIndex, null);
                    updateInputBindDisplay(menu, b);
                    player.sendMessage("§aFilter template cleared #" + (pairIndex + 1));
                    player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_OFF, 0.3F, 0.5F);
                    return false;
                }

                // 🔹 Shift+Left-click: set filter template
                if (action.isShiftClicked() && !action.isRightClicked()) {
                    ItemStack cursor = player.getItemOnCursor();
                    if (cursor == null || cursor.getType().isAir()) {
                        player.sendMessage("§cPut the item you want as the filter template on your cursor!");
                        return false;
                    }

                    ItemStack newTemplate = cursor.clone();
                    newTemplate.setAmount(1);
                    setInputFilterTemplate(b, pairIndex, newTemplate);

                    updateInputBindDisplay(menu, b);
                    String name = newTemplate.hasItemMeta() && newTemplate.getItemMeta().hasDisplayName() ?
                            newTemplate.getItemMeta().getDisplayName() : newTemplate.getType().name().toLowerCase().replace('_', ' ');

                    player.sendMessage("§aFilter template set: §e" + name);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 0.8F);
                    return false;
                }

                // 🔹 Left-click: bind input source (must have VoidTouch on the cursor)
                if (!action.isShiftClicked() && !action.isRightClicked()) {
                    ItemStack cursor = player.getItemOnCursor();
                    if (cursor == null || cursor.getType().isAir()) {
                        player.sendMessage("§cPlease put §dVoid Touch §con your cursor to bind!");
                        return false;
                    }

                    SlimefunItem sfItem = SlimefunItem.getByItem(cursor);
                    if (!(sfItem instanceof VoidTouch)) {
                        player.sendMessage("§cPlease put §dVoid Touch §con your cursor to bind!");
                        return false;
                    }

                    ItemMeta meta = cursor.getItemMeta();
                    if (meta == null) {
                        player.sendMessage("§cThis item is missing metadata, can't read the binding info.");
                        return false;
                    }

                    PersistentDataContainer container = meta.getPersistentDataContainer();

                    NamespacedKey keyX = new NamespacedKey(MagicExpansion.getInstance(), "touch_x");
                    NamespacedKey keyY = new NamespacedKey(MagicExpansion.getInstance(), "touch_y");
                    NamespacedKey keyZ = new NamespacedKey(MagicExpansion.getInstance(), "touch_z");
                    NamespacedKey keyWorld = new NamespacedKey(MagicExpansion.getInstance(), "touch_world");

                    if (!container.has(keyX, PersistentDataType.INTEGER) ||
                            !container.has(keyY, PersistentDataType.INTEGER) ||
                            !container.has(keyZ, PersistentDataType.INTEGER) ||
                            !container.has(keyWorld, PersistentDataType.STRING)) {
                        player.sendMessage("§cError:§dVoid Touch §cNo coordinates bound!");
                        return false;
                    }

                    int x = container.get(keyX, PersistentDataType.INTEGER);
                    int y = container.get(keyY, PersistentDataType.INTEGER);
                    int z = container.get(keyZ, PersistentDataType.INTEGER);
                    String worldName = container.get(keyWorld, PersistentDataType.STRING);

                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        player.sendMessage("§cBound world §e" + worldName + " §cdoesn't exist.");
                        return false;
                    }

                    // Check if the target is a Slimefun machine
                    Location targetLoc = new Location(world, x, y, z);
                    if (BlockStorage.check(targetLoc.getBlock()) == null) {
                        player.sendMessage("§cThe target location isn't a Slimefun machine, so it can't be an input source!");
                        return false;
                    }

                    // ✅ Save binding
                    String newValue = x + "," + y + "," + z + "," + worldName;
                    BlockStorage.addBlockInfo(b.getLocation(), "input_bind_pair_" + pairIndex, newValue);

                    // ✅ Update UI
                    updateInputBindDisplay(menu, b);

                    // ✅ Feedback
                    player.sendMessage("§aSuccessfully bound the input source §e#" + (pairIndex + 1));
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_LAUNCH, 0.5F, 1.0F);

                    return false;
                }

                return false;
            });
        }

        // === Input binding page buttons ===
        updateInputBindPageButtons(menu, b, totalPages);
    }
    /**
     * Update input binding page buttons
     */
    private void updateInputBindPageButtons(BlockMenu menu, Block b, int totalPages) {
        // Previous page button (uses slot 52)
        ItemStack prevItem = new ItemStack(inputBindPage > 0 ? Material.ARROW : Material.BARRIER);
        ItemMeta prevMeta = prevItem.getItemMeta();
        prevMeta.setDisplayName((inputBindPage > 0 ? "§aPrevious page" : "§cAlready the first page")+" (Extract items from the machine at external coordinates)");
        prevMeta.setLore(List.of("§7Page: " + (inputBindPage + 1) + "/" + totalPages));
        prevItem.setItemMeta(prevMeta);

        menu.addItem(52, prevItem, (player, slot, item, action) -> {
            if (inputBindPage > 0) {
                inputBindPage--;
                updateInputBindDisplay(menu, b);
                player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.3F, 0.6F);
            }
            return false;
        });

        // Next page button (uses slot 53)
        ItemStack nextItem = new ItemStack(inputBindPage < totalPages - 1 ? Material.SPECTRAL_ARROW : Material.BARRIER);
        ItemMeta nextMeta = nextItem.getItemMeta();
        nextMeta.setDisplayName((inputBindPage < totalPages - 1 ? "§aNext page" : "§cAlready the last page")+" (Extract items from the machine at external coordinates)");
        nextMeta.setLore(List.of("§7Page: " + (inputBindPage + 1) + "/" + totalPages));
        nextItem.setItemMeta(nextMeta);

        menu.addItem(53, nextItem, (player, slot, item, action) -> {
            if (inputBindPage < totalPages - 1) {
                inputBindPage++;
                updateInputBindDisplay(menu, b);
                player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.3F, 0.6F);
            }
            return false;
        });
    }
















    /*
    Transfer submenu
     */
    private final int[] transportSlotsStep = {0,1,2,3, 9,10,11,12, 18,19,20,21, 27,28,29,30, 36,37,38,39}; // The jump menu shows what's input outward, same as what transportSlots stores, just bigger
    private final int[] transportSlots2Step = {5,6,7,8, 14,15,16,17, 23,24,25,26, 32,33,34,35, 41,42,43,44}; // The jump menu shows items extracted from outside, same as what transportSlots stores, just bigger
    private final int[] arrowSlot2Step = {45,48, 50,53};
    private final int[] pinkGlassPane2Step = {4,13,22,31,40,49, 46,47, 51,52};
    // Transfer menu page variable
    private int transportOutputPage = 0;
    private int transportInputPage = 0;

    /**
     * Open the transfer overview menu
     */
    private void openTransportOverviewMenu(Player player, Block b) {
        ChestMenu menu = new ChestMenu("§6§lTransfer overview menu");
        menu.setSize(9 * 6);
        menu.setEmptySlotsClickable(false);
        menu.setPlayerInventoryClickable(true);
        menu.addMenuCloseHandler(p -> {
            ItemStack cursorItem = p.getItemOnCursor();
            if (cursorItem != null && !cursorItem.getType().isAir()) {
                // Try to put the item back into the inventory
                HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(cursorItem);
                // If the inventory is full, drop the item
                for (ItemStack item : leftover.values()) {
                    p.getWorld().dropItem(p.getLocation(), item);
                }
                // Clear cursor
                p.setItemOnCursor(new ItemStack(Material.AIR));
            }
        });


        // Initialize menu
        refreshTransportOverviewMenu(menu, b, 0, 0);
        menu.open(player);
    }

    /**
     * Refresh transfer overview menu
     */
    private void refreshTransportOverviewMenu(ChestMenu menu, Block b, int outputPage, int inputPage) {
        // Clear menu
        for (int i = 0; i < 54; i++) {
            menu.replaceExistingItem(i, null);
        }

        // Set decorative glass panes
        ItemStack pinkPane = new CustomItemStack(Material.PINK_STAINED_GLASS_PANE, "§dTransfer overview");
        for (int slot : pinkGlassPane2Step) {
            menu.addItem(slot, pinkPane, (p, s, item, action) -> false);
        }

        // Set title
        menu.addItem(4, new CustomItemStack(Material.ORANGE_STAINED_GLASS_PANE,
                "§6§lOutput config", "§7Every 2 form a group: coordinate+Template"), (p, s, item, action) -> false);

        menu.addItem(49, new CustomItemStack(Material.NETHER_STAR,
                "§e§lTransfer overview",
                "§aLeft side: §eOutput config (coordinates+template)",
                "§aRight side: §eFrom external input config (coordinates+filter)",
                "§7Click to refresh menu"), (p, s, item, action) -> {
            refreshTransportOverviewMenu(menu, b, outputPage, inputPage);
            return false;
        });

        // Refresh output config area (every 2 slots form a group)
        refreshOutputOverview(menu, b, outputPage);

        // Refresh input config area
        refreshInputOverview(menu, b, inputPage);

        // Set page buttons
        setupTransportPagination(menu, b, outputPage, inputPage);
    }

    /**
     * Refresh output config area (every 2 slots form a group: coordinate slot+template slot)
     */
    private void refreshOutputOverview(ChestMenu menu, Block b, int page) {
        int pairsPerPage = transportSlotsStep.length / 2; // 10 groups per page
        int startIndex = page * pairsPerPage;

        for (int i = 0; i < pairsPerPage; i++) {
            int pairIndex = startIndex + i;

            if (pairIndex >= MAX_BIND_PAIRS) {
                // Out of range, show empty slot
                int coordSlot = transportSlotsStep[i * 2];
                int templateSlot = transportSlotsStep[i * 2 + 1];

                menu.addItem(coordSlot, new CustomItemStack(Material.GRAY_STAINED_GLASS_PANE, "§7Empty slot"),
                        (p, s, item, action) -> false);
                menu.addItem(templateSlot, new CustomItemStack(Material.GRAY_STAINED_GLASS_PANE, "§7Empty slot"),
                        (p, s, item, action) -> false);
                continue;
            }

            // Coordinate slot (the first slot of each group)
            int coordSlot = transportSlotsStep[i * 2];
            setupOutputCoordSlot(menu, b, pairIndex, coordSlot);

            // Template slot (the second slot of each group)
            int templateSlot = transportSlotsStep[i * 2 + 1];
            setupOutputTemplateSlot(menu, b, pairIndex, templateSlot);
        }
    }

    /**
     * Set the coordinate slots of the output config
     */
    private void setupOutputCoordSlot(ChestMenu menu, Block b, int pairIndex, int slot) {
        String binding = BlockStorage.getLocationInfo(b.getLocation(), "output_bind_pair_" + pairIndex);

        ItemStack coordItem;
        List<String> lore = new ArrayList<>();

        if (binding != null && !binding.isEmpty()) {
            Location loc = parseLocation(binding);
            if (loc != null) {
                SlimefunItem sfItem = BlockStorage.check(loc);
                if (sfItem != null) {
                    coordItem = sfItem.getItem().clone();
                } else {
                    Block block = loc.getBlock();
                    if (block != null && block.getType() != Material.AIR) {
                        try {
                            Material blockType = block.getType();
                            // Check if it's a valid item type
                            if (isValidItemType(blockType)) {
                                coordItem = new ItemStack(blockType);
                            } else {
                                // For non-item blocks like wall signs, use a safe replacement
                                coordItem = getSafeAlternativeItem(blockType);
                            }
                        } catch (IllegalArgumentException e) {
                            // If creation fails, use the default safe item
                            coordItem = new ItemStack(Material.COMPASS);
                        }
                        // Removed the blockstate fetch on the main thread    not very useful
                        // 🔹 Get block info synchronously
//                        if (Bukkit.isPrimaryThread()) {
//                            try {
//                                BlockState state = block.getState();
//                                if (state instanceof BlockData) {
//                                    ItemMeta meta = coordItem.getItemMeta();
//                                    if (meta instanceof BlockStateMeta bsm) {
//                                        bsm.setBlockState(state);
//                                        coordItem.setItemMeta(bsm);
//                                    }
//                                }
//                            } catch (Exception e) {
//                                // If it fails, at least show the basic type
//                            }
//                        } else {
//                            // If on an async thread, get it synchronously
//                            ItemStack finalCoordItem = coordItem;
//                            Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> {
//                                try {
//                                    BlockState state = block.getState();
//                                    if (state instanceof BlockData) {
//                                        ItemMeta meta = finalCoordItem.getItemMeta();
//                                        if (meta instanceof BlockStateMeta bsm) {
//                                            bsm.setBlockState(state);
//                                            finalCoordItem.setItemMeta(bsm);
//                                        }
//                                    }
//                                } catch (IllegalStateException e) {
//                                    // If it still fails, log the error
//                                    MagicExpansion.getInstance().getLogger().warning(
//                                            "Can't get the block state at location: " + block.getLocation() +
//                                                    ", Type: " + block.getType()
//                                    );
//                                }
//                            });
//                        }
                    } else {
                        coordItem = new ItemStack(Material.COMPASS);
                    }
                }

                lore.add("§6Template name: §e" + ItemStackHelper.getDisplayName(coordItem));
                ItemMeta coordItemMeta = coordItem.getItemMeta();
                if (coordItemMeta != null && coordItemMeta.hasLore()) {
                    lore.addAll(coordItemMeta.getLore());
                }

                lore.add("§aTarget coordinates: §e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
                lore.add("§aWorld: §e" + loc.getWorld().getName());
            } else {
                coordItem = new ItemStack(Material.BARRIER);
                lore.add("§cCoordinate parsing error");
            }
        } else {
            coordItem = new ItemStack(Material.RECOVERY_COMPASS);
            lore.add("§7No target bound");
        }

        lore.add("");
        lore.add("§fLeft-click: Set target coordinates");
        lore.add("§fRight-click: Clear target coordinates");

        ItemMeta meta = coordItem.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(coordItem.getType());
        meta.setDisplayName("§eTarget coordinates #" + (pairIndex + 1));
        meta.setLore(lore);
        coordItem.setItemMeta(meta);
        coordItem.setAmount(1);

        final int finalPairIndex = pairIndex;
        menu.addItem(slot, coordItem, (player, clickedSlot, clickedItem, action) -> {
            handleOutputCoordClick(player, b, finalPairIndex, action, menu);
            return false;
        });
    }

    /**
     * Set the template slots of the output config
     */
    private void setupOutputTemplateSlot(ChestMenu menu, Block b, int pairIndex, int slot) {
        ItemStack template = getItemTemplate(b, pairIndex);
        int amount = getTransferAmount(b, pairIndex);

        ItemStack templateItem;
        List<String> lore = new ArrayList<>();

        if (template != null && !template.getType().isAir()) {
            templateItem = template.clone();
            lore.add("§6Template name: §e" + ItemStackHelper.getDisplayName(template));
            ItemMeta tempMeta = template.getItemMeta();
            if (tempMeta != null && tempMeta.hasLore()) {
                lore.addAll(tempMeta.getLore());
            }
            lore.add("§6Amount: §e" + amount);

            // Show current stock
            SlimefunBlockData data = StorageCacheUtils.getBlock(b.getLocation());
            if (data != null) {
                long stock = getStoredItemCountFromMainStorage(data, template);
                lore.add("§bStock: §e" + stock);
            }
        } else {
            templateItem = new ItemStack(Material.GRAY_DYE);
            lore.add("§7No item template set");
        }

        lore.add("");
        lore.add("§fLeft-click: Set item template");
        lore.add("§fRight-click: Clear item template");
        lore.add("§fShift+Left-click: Switch transfer amount");

        ItemMeta meta = templateItem.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(templateItem.getType());
        meta.setDisplayName("§6Item template #" + (pairIndex + 1));
        meta.setLore(lore);
        templateItem.setItemMeta(meta);
        templateItem.setAmount(1);

        final int finalPairIndex = pairIndex;
        menu.addItem(slot, templateItem, (player, clickedSlot, clickedItem, action) -> {
            handleOutputTemplateClick(player, b, finalPairIndex, action, menu);
            return false;
        });
    }

    /**
     * Handle output config coordinate slot click events
     */
    private void handleOutputCoordClick(Player player, Block b, int pairIndex, ClickAction action, ChestMenu menu) {
        if (action.isRightClicked()) {
            // Right-click: Clear target binding
            BlockStorage.addBlockInfo(b.getLocation(), "output_bind_pair_" + pairIndex, "");
            player.sendMessage("§aOutput target cleared #" + (pairIndex + 1));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.5F, 0.5F);
            refreshTransportOverviewMenu(menu, b, transportOutputPage, transportInputPage);

        } else if (!action.isRightClicked() && !action.isShiftClicked()) {
            // Left-click: Set target binding (needs Void Touch)
            ItemStack cursor = player.getItemOnCursor();
            if (cursor == null || cursor.getType().isAir()) {
                player.sendMessage("§cPlease put §dVoid Touch §con your cursor to bind!");
                return;
            }

            SlimefunItem sfItem = SlimefunItem.getByItem(cursor);
            if (!(sfItem instanceof VoidTouch)) {
                player.sendMessage("§cPlease put §dVoid Touch §con your cursor to bind!");
                return;
            }

            ItemMeta meta = cursor.getItemMeta();
            if (meta == null) {
                player.sendMessage("§cThis item is missing metadata, can't read the binding info.");
                return;
            }

            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey keyX = new NamespacedKey(MagicExpansion.getInstance(), "touch_x");
            NamespacedKey keyY = new NamespacedKey(MagicExpansion.getInstance(), "touch_y");
            NamespacedKey keyZ = new NamespacedKey(MagicExpansion.getInstance(), "touch_z");
            NamespacedKey keyWorld = new NamespacedKey(MagicExpansion.getInstance(), "touch_world");

            if (!container.has(keyX, PersistentDataType.INTEGER) ||
                    !container.has(keyY, PersistentDataType.INTEGER) ||
                    !container.has(keyZ, PersistentDataType.INTEGER) ||
                    !container.has(keyWorld, PersistentDataType.STRING)) {
                player.sendMessage("§cError:§dVoid Touch §cNo coordinates bound!");
                return;
            }

            int x = container.get(keyX, PersistentDataType.INTEGER);
            int y = container.get(keyY, PersistentDataType.INTEGER);
            int z = container.get(keyZ, PersistentDataType.INTEGER);
            String worldName = container.get(keyWorld, PersistentDataType.STRING);

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                player.sendMessage("§cBound world §e" + worldName + " §cdoesn't exist.");
                return;
            }

            // Save binding
            String newValue = x + "," + y + "," + z + "," + worldName;
            BlockStorage.addBlockInfo(b.getLocation(), "output_bind_pair_" + pairIndex, newValue);

            player.sendMessage("§aSuccessfully bound the output target §e#" + (pairIndex + 1));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_LAUNCH, 0.5F, 1.0F);
            refreshTransportOverviewMenu(menu, b, transportOutputPage, transportInputPage);
        }
    }

    /**
     * Handle output config template slot click events
     */
    private void handleOutputTemplateClick(Player player, Block b, int pairIndex, ClickAction action, ChestMenu menu) {
        if (action.isShiftClicked() && !action.isRightClicked()) {
            // Shift+Left-click: Switch transfer amount
            ItemStack currentTemplate = getItemTemplate(b, pairIndex);
            if (currentTemplate == null || currentTemplate.getType().isAir()) {
                player.sendMessage("§cPlease set an item template first!");
                return;
            }

            int current = getTransferAmount(b, pairIndex);
            int nextIdx = 0;
            for (int j = 0; j < TRANSFER_AMOUNTS.length; j++) {
                if (current == TRANSFER_AMOUNTS[j]) {
                    nextIdx = (j + 1) % TRANSFER_AMOUNTS.length;
                    break;
                }
            }
            int newAmount = TRANSFER_AMOUNTS[nextIdx];
            setTransferAmount(b, pairIndex, newAmount);

            player.sendMessage("§eTransfer amount: §6" + newAmount);
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.3F, 1.0F);
            refreshTransportOverviewMenu(menu, b, transportOutputPage, transportInputPage);

        } else if (action.isRightClicked() && !action.isShiftClicked()) {
            // Right-click: Clear template
            setItemTemplate(b, pairIndex, null);
            player.sendMessage("§aOutput template cleared #" + (pairIndex + 1));
            player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_OFF, 0.3F, 0.5F);
            refreshTransportOverviewMenu(menu, b, transportOutputPage, transportInputPage);

        } else if (!action.isRightClicked() && !action.isShiftClicked()) {
            // Left-click: Set template
            ItemStack cursor = player.getItemOnCursor();
            if (cursor == null || cursor.getType().isAir()) {
                player.sendMessage("§cPut the item you want as the template on your cursor!");
                return;
            }

            ItemStack newTemplate = cursor.clone();
            newTemplate.setAmount(1);
            setItemTemplate(b, pairIndex, newTemplate);

            // If it's the first time setting a template, set the default amount to1
            if (getTransferAmount(b, pairIndex) <= 0) {
                setTransferAmount(b, pairIndex, 1);
            }

            player.sendMessage("§aOutput template set: §e" + ItemStackHelper.getDisplayName(newTemplate));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 0.8F);
            refreshTransportOverviewMenu(menu, b, transportOutputPage, transportInputPage);
        }
    }

    /**
     * Refresh input config area (each slot is independent)
     */
    private void refreshInputOverview(ChestMenu menu, Block b, int page) {
        int itemsPerPage = transportSlots2Step.length;
        int startIndex = page * itemsPerPage;

        for (int i = 0; i < itemsPerPage; i++) {
            int pairIndex = startIndex + i;
            int displaySlot = transportSlots2Step[i];

            if (pairIndex >= MAX_INPUT_BIND_PAIRS) {
                // Out of range, show empty slot
                menu.addItem(displaySlot, new CustomItemStack(Material.GRAY_STAINED_GLASS_PANE, "§7Empty slot"),
                        (p, s, item, action) -> false);
                continue;
            }

            // Get binding info
            String binding = BlockStorage.getLocationInfo(b.getLocation(), "input_bind_pair_" + pairIndex);
            ItemStack filterTemplate = getInputFilterTemplate(b, pairIndex);

            ItemStack displayItem;
            List<String> lore = new ArrayList<>();

            if (binding != null && !binding.isEmpty()) {
                // When there's a binding
                Location loc = parseLocation(binding);
                if (loc != null) {
                    SlimefunItem sfItem = BlockStorage.check(loc);
                    if (sfItem != null) {
                        displayItem = sfItem.getItem().clone();
                    } else {
                        Block block = loc.getBlock();
                        if (block != null && block.getType() != Material.AIR) {
                            try {
                                Material blockType = block.getType();
                                // Check if it's a valid item type
                                if (isValidItemType(blockType)) {
                                    displayItem = new ItemStack(blockType);
                                } else {
                                    // For non-item blocks like wall signs, use a safe replacement
                                    displayItem = getSafeAlternativeItem(blockType);
                                }
                            } catch (IllegalArgumentException e) {
                                // If creation fails, use the default safe item
                                displayItem = new ItemStack(Material.COMPASS);
                            }
                        }else {
                            displayItem = new ItemStack(Material.COMPASS);
                        }
//                        displayItem = new ItemStack(loc.getBlock().getType());
                        // Removed the blockstate fetch on the main thread    not very useful
                        // 🔹 Get block info synchronously
//                        if (Bukkit.isPrimaryThread()) {
//                            try {
//                                BlockState state = loc.getBlock().getState();
//                                ItemMeta meta = displayItem.getItemMeta();
//                                if (meta instanceof BlockStateMeta bsm) {
//                                    bsm.setBlockState(state);
//                                    displayItem.setItemMeta(bsm);
//                                }
//                            } catch (Exception e) {
//                                // If it fails, at least show the basic type
//                            }
//                        } else {
//                            // If on an async thread, get it synchronously
//                            ItemStack finalDisplayItem = displayItem;
//                            Block finalBlock = loc.getBlock();
//                            Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> {
//                                try {
//                                    BlockState state = finalBlock.getState();
//                                    ItemMeta meta = finalDisplayItem.getItemMeta();
//                                    if (meta instanceof BlockStateMeta bsm) {
//                                        bsm.setBlockState(state);
//                                        finalDisplayItem.setItemMeta(bsm);
//                                    }
//                                } catch (IllegalStateException e) {
//                                    // If it still fails, log the error
//                                    MagicExpansion.getInstance().getLogger().warning(
//                                            "Can't get the block state at location: " + finalBlock.getLocation() +
//                                                    ", Type: " + finalBlock.getType()
//                                    );
//                                }
//                            });
//                        }
                    }

                    lore.add(ItemStackHelper.getDisplayName(displayItem));
                    ItemMeta cdisplayItemMeta = displayItem.getItemMeta();
                    if (cdisplayItemMeta != null && cdisplayItemMeta.hasLore()) {
                        lore.addAll(cdisplayItemMeta.getLore());
                    }

                    lore.add("§aInput source coordinates: §e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
                    lore.add("§aWorld: §e" + loc.getWorld().getName());

                    // Show source machine status
//                    Block sourceBlock = loc.getBlock();
//                    BlockMenu sourceMenu = BlockStorage.getInventory(sourceBlock);
                    BlockMenu sourceMenu = StorageCacheUtils.getMenu(loc);
                    if (sourceMenu != null) {
                        int[] outputSlots = sourceMenu.getPreset().getSlotsAccessedByItemTransport(ItemTransportFlow.WITHDRAW);
                        lore.add("§bOutput slot count: §e" + (outputSlots != null ? outputSlots.length : 0));
                    }
                } else {
                    displayItem = new ItemStack(Material.BARRIER);
                    lore.add("§cCoordinate parsing error");
                }
            } else {
                displayItem = new ItemStack(Material.RECOVERY_COMPASS);
                lore.add("§7No input source bound");
            }

            if (filterTemplate != null && !filterTemplate.getType().isAir()) {
                lore.add("§6Filter template: §e" + ItemStackHelper.getDisplayName(filterTemplate));
            } else {
                lore.add("§7No filter template");
            }

            lore.add("");
            lore.add("§fLeft-click: Set input source");
            lore.add("§fRight-click: Clear input source");
            lore.add("§fShift+Left-click: Set filter template");
            lore.add("§fShift+Right-click: Clear filter template");

            ItemMeta meta = displayItem.getItemMeta();
            if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(displayItem.getType());
            meta.setDisplayName("§aInput config #" + (pairIndex + 1));
            meta.setLore(lore);
            displayItem.setItemMeta(meta);
            displayItem.setAmount(1);

            final int finalPairIndex = pairIndex;
            menu.addItem(displaySlot, displayItem, (player, slot, clickedItem, action) -> {
                handleInputConfigClick(player, b, finalPairIndex, action, menu);
                return false;
            });
        }
    }

    /**
     * Handle input config click events
     */
    private void handleInputConfigClick(Player player, Block b, int pairIndex, ClickAction action, ChestMenu menu) {
        if (action.isShiftClicked() && action.isRightClicked()) {
            // Shift+Right-click: Clear filter template
            setInputFilterTemplate(b, pairIndex, null);
            player.sendMessage("§aInput filter cleared #" + (pairIndex + 1));
            player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_OFF, 0.3F, 0.5F);
            refreshTransportOverviewMenu(menu, b, transportOutputPage, transportInputPage);

        } else if (action.isShiftClicked() && !action.isRightClicked()) {
            // Shift+Left-click: Set filter template
            ItemStack cursor = player.getItemOnCursor();
            if (cursor == null || cursor.getType().isAir()) {
                player.sendMessage("§cPut the item you want as the filter template on your cursor!");
                return;
            }

            ItemStack newFilter = cursor.clone();
            newFilter.setAmount(1);
            setInputFilterTemplate(b, pairIndex, newFilter);

            player.sendMessage("§aInput filter set: §e" + ItemStackHelper.getDisplayName(newFilter));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 0.8F);
            refreshTransportOverviewMenu(menu, b, transportOutputPage, transportInputPage);

        } else if (action.isRightClicked() && !action.isShiftClicked()) {
            // Right-click: Clear input source binding
            BlockStorage.addBlockInfo(b.getLocation(), "input_bind_pair_" + pairIndex, "");
            setInputFilterTemplate(b, pairIndex, null); // Also clear filter
            player.sendMessage("§aInput source cleared #" + (pairIndex + 1));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.5F, 0.5F);
            refreshTransportOverviewMenu(menu, b, transportOutputPage, transportInputPage);

        } else if (!action.isRightClicked() && !action.isShiftClicked()) {
            // Left-click: Set input source binding (needs Void Touch)
            ItemStack cursor = player.getItemOnCursor();
            if (cursor == null || cursor.getType().isAir()) {
                player.sendMessage("§cPlease put §dVoid Touch §con your cursor to bind!");
                return;
            }

            SlimefunItem sfItem = SlimefunItem.getByItem(cursor);
            if (!(sfItem instanceof VoidTouch)) {
                player.sendMessage("§cPlease put §dVoid Touch §con your cursor to bind!");
                return;
            }

            ItemMeta meta = cursor.getItemMeta();
            if (meta == null) {
                player.sendMessage("§cThis item is missing metadata, can't read the binding info.");
                return;
            }

            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey keyX = new NamespacedKey(MagicExpansion.getInstance(), "touch_x");
            NamespacedKey keyY = new NamespacedKey(MagicExpansion.getInstance(), "touch_y");
            NamespacedKey keyZ = new NamespacedKey(MagicExpansion.getInstance(), "touch_z");
            NamespacedKey keyWorld = new NamespacedKey(MagicExpansion.getInstance(), "touch_world");

            if (!container.has(keyX, PersistentDataType.INTEGER) ||
                    !container.has(keyY, PersistentDataType.INTEGER) ||
                    !container.has(keyZ, PersistentDataType.INTEGER) ||
                    !container.has(keyWorld, PersistentDataType.STRING)) {
                player.sendMessage("§cError:§dVoid Touch §cNo coordinates bound!");
                return;
            }

            int x = container.get(keyX, PersistentDataType.INTEGER);
            int y = container.get(keyY, PersistentDataType.INTEGER);
            int z = container.get(keyZ, PersistentDataType.INTEGER);
            String worldName = container.get(keyWorld, PersistentDataType.STRING);

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                player.sendMessage("§cBound world §e" + worldName + " §cdoesn't exist.");
                return;
            }

            // Check if the target is a Slimefun machine
            Location targetLoc = new Location(world, x, y, z);
            if (BlockStorage.check(targetLoc.getBlock()) == null) {
                player.sendMessage("§cThe target location isn't a Slimefun machine, so it can't be an input source!");
                return;
            }

            // Save binding
            String newValue = x + "," + y + "," + z + "," + worldName;
            BlockStorage.addBlockInfo(b.getLocation(), "input_bind_pair_" + pairIndex, newValue);

            player.sendMessage("§aSuccessfully bound the input source §e#" + (pairIndex + 1));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_LAUNCH, 0.5F, 1.0F);
            refreshTransportOverviewMenu(menu, b, transportOutputPage, transportInputPage);
        }
    }

    /**
     * Set the transfer menu page buttons
     */
    private void setupTransportPagination(ChestMenu menu, Block b, int outputPage, int inputPage) {
        // Save the current page number
        transportOutputPage = outputPage;
        transportInputPage = inputPage;

        // Output config page turn
        int outputTotalPages = (int) Math.ceil((double) MAX_BIND_PAIRS / (transportSlotsStep.length / 2));

        // Previous page button (45)
        if (outputPage > 0) {
            menu.addItem(45, new CustomItemStack(Material.ARROW, "§aOutput previous page", "§7Page: " + (outputPage + 1) + "/" + outputTotalPages),
                    (p, s, item, action) -> {
                        refreshTransportOverviewMenu(menu, b, outputPage - 1, inputPage);
                        return false;
                    });
        } else {
            menu.addItem(45, new CustomItemStack(Material.BARRIER, "§cOutput first page", "§7Page: " + (outputPage + 1) + "/" + outputTotalPages),
                    (p, s, item, action) -> false);
        }

        // Next page button (48)
        if (outputPage < outputTotalPages - 1) {
            menu.addItem(48, new CustomItemStack(Material.ARROW, "§aOutput next page", "§7Page: " + (outputPage + 1) + "/" + outputTotalPages),
                    (p, s, item, action) -> {
                        refreshTransportOverviewMenu(menu, b, outputPage + 1, inputPage);
                        return false;
                    });
        } else {
            menu.addItem(48, new CustomItemStack(Material.BARRIER, "§cOutput last page", "§7Page: " + (outputPage + 1) + "/" + outputTotalPages),
                    (p, s, item, action) -> false);
        }

        // Input config page turn
        int inputTotalPages = (int) Math.ceil((double) MAX_INPUT_BIND_PAIRS / transportSlots2Step.length);

        // Previous page button (50)
        if (inputPage > 0) {
            menu.addItem(50, new CustomItemStack(Material.ARROW, "§aEnter previous page", "§7Page: " + (inputPage + 1) + "/" + inputTotalPages),
                    (p, s, item, action) -> {
                        refreshTransportOverviewMenu(menu, b, outputPage, inputPage - 1);
                        return false;
                    });
        } else {
            menu.addItem(50, new CustomItemStack(Material.BARRIER, "§cEnter first page", "§7Page: " + (inputPage + 1) + "/" + inputTotalPages),
                    (p, s, item, action) -> false);
        }

        // Next page button (53)
        if (inputPage < inputTotalPages - 1) {
            menu.addItem(53, new CustomItemStack(Material.ARROW, "§aEnter next page", "§7Page: " + (inputPage + 1) + "/" + inputTotalPages),
                    (p, s, item, action) -> {
                        refreshTransportOverviewMenu(menu, b, outputPage, inputPage + 1);
                        return false;
                    });
        } else {
            menu.addItem(53, new CustomItemStack(Material.BARRIER, "§cEnter last page", "§7Page: " + (inputPage + 1) + "/" + inputTotalPages),
                    (p, s, item, action) -> false);
        }
    }


























}
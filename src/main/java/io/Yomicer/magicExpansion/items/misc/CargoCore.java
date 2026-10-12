package io.Yomicer.magicExpansion.items.misc;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.items.tools.VoidTouch;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.Yomicer.magicExpansion.utils.SameItemJudge;
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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static io.Yomicer.magicExpansion.core.MagicExpansionItems.CARGO_FRAGMENT;
import static io.Yomicer.magicExpansion.utils.SameItemJudge.*;
import static io.Yomicer.magicExpansion.utils.Utils.doGlow;

public class CargoCore extends SlimefunItem implements EnergyNetComponent{


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

    public CargoCore(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);

        constructMenu("Magic Storage");
        addItemHandler(onBlockPlace(), onBlockBreak());
    }



    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem sf, SlimefunBlockData data) {
                CargoCore.this.tick(b);
            }

            @Override
            public boolean isSynchronized() {
                // F1v2 Adjustment (as needed): the main part stays on the async tick to ease the main thread load;
                // tick Operations inside that need the main thread (BlockMenu changes, entity spawning, etc.) are scheduled back to the main thread via runOnMain
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
                    // F2 Fix: the fragment itself is"Merge stacks by item type"form (each fragment records item+amount),
                    // Used to drop at most 2048 fragment entities, now merged into at most 64 entities (each fragment can hold up to Integer.MAX_VALUE items),
                    // Exceeds 64 × Integer.MAX_VALUE Only an extreme amount would get lost (practically impossible to reach)
                    long remainingAmount = amount;
                    int fragmentCount = 0;
                    long lostAmount = 0;

                    while (remainingAmount > 0 && fragmentCount < 64) {
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
                        player.sendMessage("Each item type can drop at most 64 Aether Caskets holding 2.14B items");
                        player.sendMessage("Max drop amount per item is137,438,953,408");
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
     * Create a CargoFragment item that stands for a certain item's“Storage fragment”
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
        data.removeData("item_type_" + dataSlot);
        data.removeData("item_count_" + dataSlot);
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
            return;
        }

        ItemStack prototype = itemFromBase64(json);
        if (prototype == null || prototype.getType().isAir()) {
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

        if (itemCount <= 0) {
            return;
        }

        // Run output (up to 9 slots, index 0~8)
        int[] outputSlots = getOutputSlots();
        int batchSize = 64*9; // e.g. 64, 512...

        int successfullyOutput = tryOutputItems(menu, prototype, itemCount, outputSlots, batchSize);

        if (successfullyOutput > 0) {
            // Update stock
            itemCount -= successfullyOutput;
            if (itemCount <= 0) {
                cleanupOutputState(data, targetSlot);
            } else {
                data.setData("item_count_" + targetSlot, String.valueOf(itemCount));
            }

            // Update UI
            updateStorageDisplay(menu, data);

            // Sound
            Block block = menu.getLocation().getBlock();
//            block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2F, 1.2F);
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
        cleanupInvalidSlots(data); // Pure BlockStorage data read/write, safe to leave on the async thread

        // F1v2 Adjustment (as needed): the operations below involve BlockMenu/Inventory changes and entity spawning,
        // Must run on the main thread——The async tick only schedules; the actual actions switch back to the main thread and run in the order submitted
        runOnMain(() -> handleOutput(menu, data));
        runOnMain(() -> handleAllTemplateTransfers(block));
        runOnMain(() -> handleAllInputTransfers(block, data));    // Input transfer (new)

        // Handle input slot items: PDC reading and checks stay on the async thread,"Store+Consume"Schedule the action back to the main thread
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
                        // Deserialize the original item (read-only parsing on the async thread)
                        ItemStack originalItem = itemFromBase64(json);
                        if (originalItem != null) {
                            // Run on main thread: store CargoCore + Consume this CargoFragment (guarantees atomic pairs)
                            // Compatibility fix(1.20.5+): Template(amount=1)Stored separately from the amount,
                            // Large amount(>99)No longer written into the ItemStack object, Avoid the amount check errors from asQuantity/setAmount/serialization
                            ItemStack toStore = originalItem.clone();
                            toStore.setAmount(1);
                            runOnMain(() -> {
                                storeItem(data, toStore, amount);
                                menu.consumeItem(slot, 1);
                            });
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
                // F3 Fix (verified: non-CargoFragment items used to be silently absorbed and stored with no checks):
                // Add whitelist check——Only"Normal item"(functional items not registered by this plugin) are allowed to be auto-stored by the input slot;
                // This plugin's machines/tools and other Slimefun items (id starts with magic_expansion_) are no longer swallowed,
                // Left as-is in the input slot for the player to handle manually, so valuable items don't get silently consumed
                if (!isWhitelistedForDirectStore(item)) {
                    continue;
                }
                // Run on main thread: store + Consume (clone a snapshot to avoid async reads racing with main thread writes)
                ItemStack toStore = item.clone();
                int storeAmount = item.getAmount();
                runOnMain(() -> {
                    storeItem(data, toStore);
                    menu.consumeItem(slot, storeAmount);
                });
            }
        }

        if(menu.hasViewer()) {
            // F1v2 Adjustment: UI updates change Inventory slots, so they're also scheduled back to the main thread
            runOnMain(() -> {
                // Update storage display
                updateStorageDisplay(menu, data);
                updatePageButtons(menu, data);
                updateInputBindDisplay(menu, block); // Update input binding display (new)
                updateTranslateOutPut(menu, block);
            });
        }

    }

    /**
     * F1v2 New: operations in the async tick that need the main thread all get scheduled back to the main thread through here.
     * runTask Makes sure tasks run one after another on the main thread in the order they were submitted, so"Store+Consume"and other paired actions stay atomic.
     */
    private void runOnMain(Runnable runnable) {
        Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), runnable);
    }

    // F3: Input slot directly absorbs, whitelist check——Only returns true for normal items (not functional items registered by this plugin);
    // This plugin's item id prefix is always magic_expansion_ (the SlimefunItemStack id is the lowercase registered name)
    private boolean isWhitelistedForDirectStore(ItemStack item) {
        SlimefunItem sf = SlimefunItem.getByItem(item);
        return sf == null || sf.getId() == null || !sf.getId().startsWith("magic_expansion_");
    }

    // in tick() Used when calling storeItem in (compatible with the old signature: The amount takes item.getAmount()):
    private void storeItem(SlimefunBlockData data, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        int count = item.getAmount();
        // Compatibility fix(1.20.5+): Templated(amount=1)then delegate, Large amounts no longer go into the ItemStack object
        ItemStack template = item.clone();
        template.setAmount(1);
        storeItem(data, template, count);
    }

    /**
     * Store into core(Template+Amount separated).
     * Compatibility fix(1.20.5+/1.21): The storage model is item_type_N(Prototype) + item_count_N(Amount) two fields,
     * The prototype is always amount=1 Serialize, The amount is only written to the plain-number item_count_N field——
     * Large amount(>99)No longer put into the ItemStack object, Avoid 1.20.5+/1.21 's amount check/serialization error
     * (Tested: First store >99 An amount of Aether Caskets will definitely blow up and disable the machine).
     *
     * @param template Store prototype(must be amount=1 's item)
     * @param count    Amount stored this time
     */
    private void storeItem(SlimefunBlockData data, ItemStack template, int count) {
        cleanupInvalidSlots(data);
        if (count <= 0) return;
        int slot = findMatchingSlot(data, template);
        if (slot != -1) {
            // Matched an existing slot
            long stored = 0;

            try {
                stored = Long.parseLong(data.getData("item_count_" + slot));
            } catch (Exception ignored) {}

            try {
                stored = Math.addExact(stored, count);
                data.setData("item_count_" + slot, String.valueOf(stored));
            } catch (ArithmeticException e) {
                // Overflow, discard
                Location loc = data.getLocation();
                if (loc != null) {
                    // F1v2: spawning drop entities must run on the main thread, scheduled via runOnMain
                runOnMain(() -> loc.getWorld().dropItem(loc, template));
                }
            }

        } else {
            // Find an empty slot
            slot = findEmptySlot(data);
            if (slot == -1) {
                Location loc = data.getLocation();
                if (loc != null) {
                    // F1v2: spawning drop entities must run on the main thread, scheduled via runOnMain
                runOnMain(() -> loc.getWorld().dropItem(loc, template));
                }
                return;
            }

            // ✅ The prototype is always amount=1 Serialize, The amount is stored separately in item_count_N(Decoupled from the version)
            String json = itemToBase64(template.clone());
            if (json == null) return;

            data.setData("item_type_" + slot, json);
            data.setData("item_count_" + slot, String.valueOf(count));
        }
    }
    // Check if the same item already exists
    private int findMatchingSlot(SlimefunBlockData data, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return -1;
        }

        ItemStack prototype = item.clone();
        prototype.setAmount(1);

        for (int i = 0; i < MAX_STORED_ITEMS; i++) {
            String base64Data = data.getData("item_type_" + i);
            if (base64Data == null || base64Data.isEmpty()) continue;

            ItemStack stored;
            try {
                stored = itemFromBase64(base64Data);
            } catch (Exception e) {
                continue; // or log it
            }

            if (stored == null) continue;
            stored.setAmount(1); // Make sure the amount doesn't get in the way when comparing

            if (SameItemJudge.isSimilarSafe(prototype, stored)) {
//            if (SlimefunUtils.isItemSimilar(prototype, stored, true)) {
                return i;
            }
        }
        return -1;
    }
    /**
     * Clean up invalid slots:count <= 0 Or parsing fails, or the data doesn't match
     */
    public void cleanupInvalidSlots(SlimefunBlockData data) {
        for (int i = 0; i < MAX_STORED_ITEMS; i++) {
            String typeKey = "item_type_" + i;
            String countKey = "item_count_" + i;

            String typeStr = data.getData(typeKey);
            String countStr = data.getData(countKey);

            boolean hasType = typeStr != null && !typeStr.trim().isEmpty();
            boolean hasCount = countStr != null && !countStr.trim().isEmpty();

            if (!hasType && !hasCount) continue;

            // One missing → Delete
            if (hasType && !hasCount) {
                data.removeData(typeKey);
                continue;
            }
            if (!hasType && hasCount) {
                data.removeData(countKey);
                continue;
            }

            // Both have → Check count
            long count = 0;
            try {
                count = Long.parseLong(countStr.trim());
                if (count <= 0) {
                    data.removeData(typeKey);
                    data.removeData(countKey);
                }
            } catch (Exception e) {
                // Only clean up when count is wrong
                data.removeData(typeKey);
                data.removeData(countKey);
            }
        }
    }

    // Find an empty slot
    private int findEmptySlot(SlimefunBlockData data) {
        for (int i = 0; i < MAX_STORED_ITEMS; i++) {
            String typeData = data.getData("item_type_" + i);
            String countData = data.getData("item_count_" + i);

            if (typeData == null || typeData.isEmpty()) return i;
            if (countData == null || countData.isEmpty()) return i;

            try {
                long count = Long.parseLong(countData);
                if (count <= 0) return i; // Can be reused
            } catch (Exception e) {
                return i; // Parsing failed → Reusable
            }
        }
        return -1;
    }

    private List<Integer> getStoredItemSlots(SlimefunBlockData data) {
        List<Integer> slots = new ArrayList<>();

        for (int i = 0; i < MAX_STORED_ITEMS; i++) {
            String countStr = data.getData("item_count_" + i);
            if (countStr != null && !countStr.isEmpty()) {
                try {
                    long count = Long.parseLong(countStr);
                    if (count > 0) {
                        slots.add(i);
                    }
                } catch (Exception ignored) {}
            }
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
            try {
                count = Long.parseLong(data.getData("item_count_" + dataSlot));
            } catch (Exception ignored) {}

            ItemStack display = prototype.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                if (lore == null) lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Stored amount: §a" + count);
                // ✅ Add“Outputting”Status hint
                if (dataSlot == currentOutputSlot) {
                    lore.add(""); // Blank line separator
                    lore.add("§6§l▶ §eOutputting now"); // Golden arrow + Yellow text
                }
                meta.setLore(lore);


                // Write to PDC for reverse lookup dataSlot
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(new NamespacedKey(MagicExpansion.getInstance(), "cargo_slot"), PersistentDataType.INTEGER, dataSlot);
                display.setItemMeta(meta);
            }
            display.setAmount(1);

//            menu.replaceExistingItem(storageSlots[i], display);
            // ✅ One step done: set item + Bind click logic
            int finalCurrentOutputSlot = currentOutputSlot;
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
                    itemCount = Long.parseLong(data.getData("item_count_" + targetDataSlot));
                } catch (Exception ignored) {}

                ItemStack itemPrototype = itemFromBase64(json);
                if (itemPrototype == null || itemCount <= 0) return false;
                ItemStack itemPrototypeClone = itemPrototype.clone();
                itemPrototypeClone.setAmount(1);

                // === Handle different click types ===
                // === Tell click types apart ===
                if (clickAction.isShiftClicked() && clickAction.isRightClicked()) {
                    // ✅ Shift + Right-click: toggle continuous output

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
                else if (clickAction.isShiftClicked() && !clickAction.isRightClicked()) {
                    // Shift + Left-click: store all items of the same type in your inventory
                    int moved = 0;
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && !item.getType().isAir()) {
                            ItemStack itemClone = item.clone();
                            itemClone.setAmount(1);
                            if (SameItemJudge.isSimilarSafe(itemClone, itemPrototypeClone)) {
//                            if (SlimefunUtils.isItemSimilar(itemClone, itemPrototypeClone, true)) {
                                int amount = item.getAmount();
                                storeItem(data, item.clone());
                                player.getInventory().removeItem(item);
                                moved += amount;
                            }
                        }
                    }
                    if (moved > 0) {
                        player.sendMessage("§aStored §e" + moved + " §ax " + ItemStackHelper.getDisplayName(itemPrototypeClone) + " §r§ainto storage");
                        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 0.5F, 1.2F);
                        updateStorageDisplay(menu, data);
                    } else {
                        player.sendMessage("§7You don't have any of that item in your inventory to store.");
                        player.playSound(player.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
                    }

                } else if (clickAction.isRightClicked() && !clickAction.isShiftClicked()) {
                    // Right-click: fill the inventory as much as possible
                    int filled = 0;
                    ItemStack toGive = itemPrototype.clone();
                    for (int invSlot = 0; invSlot < 36; invSlot++) {
                        ItemStack invItem = player.getInventory().getItem(invSlot);
                        if (invItem == null || invItem.getType().isAir()) {
                            int giveAmount = (int) Math.min(64, itemCount);
                            if (giveAmount <= 0) break;
                            toGive.setAmount(giveAmount);
                            player.getInventory().setItem(invSlot, toGive.clone());
                            itemCount -= giveAmount;
                            filled += giveAmount;
                        } else if (SameItemJudge.isSimilarSafe(invItem, itemPrototype) && invItem.getAmount() < 64) {
//                        } else if (SlimefunUtils.isItemSimilar(invItem, itemPrototype, true) && invItem.getAmount() < 64) {
                            int space = 64 - invItem.getAmount();
                            int giveAmount = (int) Math.min(space, itemCount);
                            if (giveAmount <= 0) break;
                            invItem.setAmount(invItem.getAmount() + giveAmount);
                            itemCount -= giveAmount;
                            filled += giveAmount;
                        }
                        if (itemCount <= 0) break;
                    }
                    if (filled > 0) {
                        data.setData("item_count_" + targetDataSlot, String.valueOf(itemCount));
                        player.sendMessage("§aAdded to your inventory §e" + filled + " §ax " + ItemStackHelper.getDisplayName(itemPrototypeClone));
                        player.playSound(player.getLocation(), Sound.ITEM_BUNDLE_INSERT, 0.5F, 1.0F);
                        updateStorageDisplay(menu, data);
                    } else {
                        player.sendMessage("§7Inventory is full, can't take out more.");
                        player.playSound(player.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
                    }

                } else if (!clickAction.isRightClicked() && !clickAction.isShiftClicked()) {
                    // Left-click: take out 64
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


            // Assuming blueBorder is int[] Array containing the slots to set
            for (int slot : blueBorder) {
                menu.addItem(slot, new CustomItemStack(Material.BLUE_STAINED_GLASS_PANE, "§bView stored items"),(player, sloti, itemStack, clickAction) -> {
                    openStorageMenu(player, data,0); // Open the first page by default
                    return false; // Don't consume the item or do the default behavior
                });
            }



        }

        updatePageButtons(menu, data);
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
            try { count = Long.parseLong(countStr); } catch (Exception ignored) {}
            if (json != null && !json.isEmpty() && count > 0) {
                validSlots.add(i);
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
            try { itemCount = Long.parseLong(data.getData("item_count_" + finalTargetSlot)); } catch (Exception ignored) {}

            ItemStack itemPrototype = itemFromBase64(json);
            if (itemPrototype == null || itemCount <= 0) {
                menu.addItem(i, new CustomItemStack(Material.BARRIER, "§cData error"));
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

                // ✅ Add“Outputting”Status hint
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

                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            display.setAmount(1);

            int finalCurrentPage2 = currentPage;
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
                    itemCount1 = Long.parseLong(data.getData("item_count_" + targetDataSlot1));
                } catch (Exception e) {
                    p.sendMessage("§cFailed to read amount");
                    return false;
                }

                if (json1 == null || itemCount1 <= 0) {
                    p.sendMessage("§cItem is no longer valid");
                    openStorageMenu(p, data, finalCurrentPage2); // Refresh
                    return false;
                }


                // === Tell click types apart ===
                // Get the target slot currently being output
                int currentOutputTarget = -1;
                try {
                    String outputStr = data.getData("output_target_slot"); // Note: are you using OUTPUT_TARGET_KEY?
                    if (outputStr != null && !outputStr.isEmpty()) {
                        currentOutputTarget = Integer.parseInt(outputStr);
                    }
                } catch (Exception ignored) { }
                if (action.isShiftClicked() && action.isRightClicked()) {
                    if (targetDataSlot1 == currentOutputTarget) {
                        // The current item is being output → Stop
                        data.setData("output_target_slot", "-1");
                        p.sendMessage("§aOutput stopped: " + ItemStackHelper.getDisplayName(itemPrototypeClone));
                    } else {
                        // The current item is not an output target → Start output
                        data.setData("output_target_slot", String.valueOf(targetDataSlot1));
                        p.sendMessage("§eStart continuous output: " + ItemStackHelper.getDisplayName(itemPrototypeClone));
                    }

                    refreshStorageMenu(menu, data, finalCurrentPage2);
                    return false;
                }
                else if (action.isShiftClicked() && !action.isRightClicked()) {
                    // Shift + Left-click: store all items of the same type in your inventory
                    int moved = 0;
                    for (ItemStack item : p.getInventory().getContents()) {
                        if (item != null && !item.getType().isAir()){
                            ItemStack itemClone = item.clone();
                            if(SameItemJudge.isSimilarSafe(itemClone, itemPrototypeClone)){
//                            if(SlimefunUtils.isItemSimilar(itemClone, itemPrototypeClone, true)){
                            int amount = item.getAmount();
                            storeItem(data, item.clone());
                            p.getInventory().removeItem(item);
                            moved += amount;
                            }
                        }
                    }
                    if (moved > 0) {
                        p.sendMessage("§aStored §e" + moved + " §ax " + ItemStackHelper.getDisplayName(itemPrototype) + " §r§ainto storage");
                        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 0.5F, 1.2F);
                        refreshStorageMenu(menu, data, finalCurrentPage2);
                    } else {
                        p.sendMessage("§7You don't have any of that item in your inventory to store.");
                        p.playSound(p.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
                    }

                } else if (action.isRightClicked() && !action.isShiftClicked()) {
                    // Right-click: fill the inventory as much as possible
                    int filled = 0;
                    ItemStack toGive = itemPrototype.clone();
                    for (int invSlot = 0; invSlot < 36; invSlot++) {
                        ItemStack invItem = p.getInventory().getItem(invSlot);
                        if (invItem == null || invItem.getType().isAir()) {
                            int giveAmount = (int) Math.min(64, itemCount1);
                            if (giveAmount <= 0) break;
                            toGive.setAmount(giveAmount);
                            p.getInventory().setItem(invSlot, toGive.clone());
                            itemCount1 -= giveAmount;
                            filled += giveAmount;
                        } else if (SameItemJudge.isSimilarSafe(invItem, itemPrototypeClone) && invItem.getAmount() < 64) {
//                        } else if (SlimefunUtils.isItemSimilar(invItem, itemPrototypeClone, true) && invItem.getAmount() < 64) {
                            int space = 64 - invItem.getAmount();
                            int giveAmount = (int) Math.min(space, itemCount1);
                            if (giveAmount <= 0) break;
                            invItem.setAmount(invItem.getAmount() + giveAmount);
                            itemCount1 -= giveAmount;
                            filled += giveAmount;
                        }
                        if (itemCount1 <= 0) break;
                    }
                    if (filled > 0) {
                        data.setData("item_count_" + targetDataSlot, String.valueOf(itemCount1));
                        p.sendMessage("§aAdded to your inventory §e" + filled + " §ax " + ItemStackHelper.getDisplayName(itemPrototypeClone));
                        p.playSound(p.getLocation(), Sound.ITEM_BUNDLE_INSERT, 0.5F, 1.0F);
                        refreshStorageMenu(menu, data, finalCurrentPage2);
                    } else {
                        p.sendMessage("§7Inventory is full, can't take out more.");
                        p.playSound(p.getLocation(), Sound.BLOCK_METAL_HIT, 0.3F, 0.5F);
                    }

                } else if (!action.isRightClicked() && !action.isShiftClicked()) {
                    // Left-click: take out 64
                    int take = (int) Math.min(64, itemCount1);
                    if (take <= 0) return false;
//                    ItemStack toTake = itemPrototype.clone().asQuantity(take);
//                    if (p.getInventory().addItem(toTake).isEmpty()) {
//                        itemCount1 -= take;
//                        data.setData("item_count_" + targetDataSlot, String.valueOf(itemCount1));
//                        p.sendMessage("§aTook out §e" + take + " §ax " + ItemStackHelper.getDisplayName(itemPrototypeClone));
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

        preset.addItem(49, new CustomItemStack(new ItemStack (Material.SOUL_LANTERN), ColorGradient.getGradientName("Magic Storage Terminal"),"§eFor stored items","§bLeft-click: take out 1 stack","§bRight-click: fill inventory","§bshift+Left-click: store all of this item from your inventory into storage","§bshift+Right-click: set storage output item",
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
                                // 🔹 Fix: get it on the main thread BlockState
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
            if (templateItem != null && !templateItem.getType().isAir()) {
//                transferToVanillaContainer(b, pairIndex, data);
                if (Bukkit.isPrimaryThread()) {
                    transferToVanillaContainer(b, pairIndex, data);
                } else {
                    int finalPairIndex = pairIndex;
                    Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> {
                        transferToVanillaContainer(b, finalPairIndex, data);
                    });
                }
            }
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

        // 3. Get current stock from the main storage system (fixed the key issue)
        long currentStock = getStoredItemCountFromMainStorage(data, template);
        if (currentStock <= 0) return;

        // 4. Actual pushed amount = min(Set amount, Stock amount)
        int amountToTransfer = (int) Math.min(configuredAmount, currentStock);
        if (amountToTransfer <= 0) return;

        // 5. Get target location (fixed key name)
        Location targetLocation = getTargetLocation(sourceBlock, pairIndex);
        if (targetLocation == null) return;

        // 6. Check if the target location is valid
        if (!isValidTarget(targetLocation)) {
            // Target is invalid, clear the binding
//            data.setData("output_bind_pair_" + pairIndex, "");
            return;
        }

        // 7. Predict how much the target can take at most
        int maxFit = predictMaxFit(targetLocation, template, amountToTransfer);
        if (maxFit <= 0) return;

        // 8. [Deduct stock from the main storage system]
        long deducted = deductStoredItemFromMainStorage(data, template, maxFit);
        if (deducted <= 0) return;

        // 9. Run push
        int actualPushed = pushItemsToLocation(sourceBlock, targetLocation, template, (int) deducted);

        // 10. Play sound and particle effects
        if (actualPushed > 0) {
//            sourceBlock.getWorld().playSound(
//                    sourceBlock.getLocation(),
//                    Sound.ENTITY_ENDERMAN_TELEPORT,
//                    0.3F,
//                    1.0F
//            );

            // Add particle effect to show the transfer
            showTransferParticles(sourceBlock.getLocation(), targetLocation, Particle.END_ROD);
        }

        // 11. If the amount actually pushed is less than the amount deducted, return the difference to storage
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

        // Go through all storage slots to find the matching item
        for (int i = 0; i < MAX_STORED_ITEMS; i++) {
            String jsonData = data.getData("item_type_" + i);
            if (jsonData == null || jsonData.isEmpty()) continue;

            try {
                ItemStack storedItem = itemFromBase64(jsonData);
                if (storedItem != null && SameItemJudge.isSimilarSafe(storedItem, prototype)) {
//                if (storedItem != null && SlimefunUtils.isItemSimilar(storedItem, prototype, true)) {
                    String countStr = data.getData("item_count_" + i);
                    if (countStr != null && !countStr.isEmpty()) {
                        try {
                            return Long.parseLong(countStr);
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
        return 0;
    }

    /**
     * Take items out of the main storage system (fixed the key issue)
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
                if (storedItem != null && SameItemJudge.isSimilarSafe(storedItem, prototype)) {
//                if (storedItem != null && SlimefunUtils.isItemSimilar(storedItem, prototype, true)) {
                    String countStr = data.getData("item_count_" + i);
                    if (countStr == null || countStr.isEmpty()) continue;

                    try {
                        long currentCount = Long.parseLong(countStr);
                        long toDeduct = Math.min(currentCount, amount);

                        if (toDeduct <= 0) return 0;

                        long newCount = currentCount - toDeduct;

                        if (newCount <= 0) {
                            // Amount is 0, clear this slot
                            data.removeData("item_type_" + i);
                            data.removeData("item_count_" + i);
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
     * Compatibility fix(1.20.5+): Template(amount=1)Passed in separately from the amount, Large amounts are no longer written into the ItemStack object
     */
    private void refundToMainStorage(@Nonnull SlimefunBlockData data, @Nonnull ItemStack template, long amount) {
        if (amount <= 0) return;

        // Call the existing storeItem method directly
        ItemStack refundStack = template.clone();
        refundStack.setAmount(1);
        int refundCount = (int) Math.min(amount, Integer.MAX_VALUE);
        storeItem(data, refundStack, refundCount);
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
            SlimefunItem sfToExtract = SlimefunItem.getByItem(itemToExtract);
            if (sfToExtract instanceof CargoFragment) {
                continue;
            }
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
        toExtract.setAmount(maxExtract);

        // Store directly into the target storage system
        storeItem(destData, toExtract);
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
                        ColorGradient.getGradientName("Pull the items in →"),
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
//                        // 🔹 Get block info synchronously
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
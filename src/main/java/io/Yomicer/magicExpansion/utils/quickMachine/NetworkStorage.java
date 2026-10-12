package io.Yomicer.magicExpansion.utils;

import com.jeff_media.morepersistentdatatypes.DataType;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.Yomicer.magicExpansion.utils.SameItemJudge.itemFromBase64;

/**
 * Unified wrapper for network quantum storage.
 * <p>
 * Combines all quantum-storage code from the Networks / NetworksExpansion plugins:
 * NamespacedKey, PDC access, quantum storage data types, and QuantumCache,
 * Also provides storage methods with max-value and overflow protection, plus remaining output-slot capacity checks for aquariums.
 */
public final class NetworkStorage {

    /** NetworksExpansion quantum storage block class name (used via reflection to avoid a hard dependency) */
    private static final String QUANTUM_STORAGE_BLOCK_CLASS =
            "io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage";

    private NetworkStorage() {
    }

    // ==================== NamespacedKeys（Supports official, Chinese-localized, and newer key formats） ====================

    public static final String NETWORKS_ID = "networks"; // Official version / Chinese localized version
    public static final String NETWORKS_CHANGED_ID = "networks-changed"; // Xinzi version

    public static final NamespacedKey ITEM = newKey("item");
    public static final NamespacedKey ITEM2 = customNewKey(NETWORKS_ID, "item");
    public static final NamespacedKey ITEM3 = customNewKey(NETWORKS_CHANGED_ID, "item");

    public static final NamespacedKey AMOUNT = newKey("amount");
    public static final NamespacedKey AMOUNT2 = customNewKey(NETWORKS_ID, "amount");
    public static final NamespacedKey AMOUNT3 = customNewKey(NETWORKS_CHANGED_ID, "amount");

    public static final NamespacedKey MAX_AMOUNT = newKey("max_amount");
    public static final NamespacedKey MAX_AMOUNT2 = customNewKey(NETWORKS_ID, "max_amount");
    public static final NamespacedKey MAX_AMOUNT3 = customNewKey(NETWORKS_CHANGED_ID, "max_amount");

    public static final NamespacedKey VOID = newKey("void");
    public static final NamespacedKey VOID2 = customNewKey(NETWORKS_ID, "void");
    public static final NamespacedKey VOID3 = customNewKey(NETWORKS_CHANGED_ID, "void");

    public static final NamespacedKey SUPPORTS_CUSTOM_MAX_AMOUNT = newKey("supports_custom_max_amount");

    public static final NamespacedKey QUANTUM_STORAGE_INSTANCE = newKey("quantum_storage");
    public static final NamespacedKey QUANTUM_STORAGE_INSTANCE2 = customNewKey(NETWORKS_ID, "quantum_storage");
    public static final NamespacedKey QUANTUM_STORAGE_INSTANCE3 = customNewKey(NETWORKS_CHANGED_ID, "quantum_storage");

    public static NamespacedKey newKey(@NotNull String key) {
        return new NamespacedKey("networks", key);
    }

    public static NamespacedKey customNewKey(@NotNull String namespace, @NotNull String key) {
        return new NamespacedKey(namespace, key);
    }

    // ==================== PDC access tools (formerly DataTypeMethods) ====================

    @Nullable
    public static <T, Z> Z getCustom(@NotNull PersistentDataHolder holder, @NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type) {
        return holder.getPersistentDataContainer().get(key, type);
    }

    @NotNull
    public static <T, Z> Optional<Z> getOptionalCustom(@NotNull PersistentDataHolder holder, @NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type) {
        return Optional.ofNullable(getCustom(holder, key, type));
    }

    public static <T, Z> @NotNull Z getCustom(@NotNull PersistentDataHolder holder, @NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type, @NotNull Z defaultVal) {
        return holder.getPersistentDataContainer().getOrDefault(key, type, defaultVal);
    }

    public static <T, Z> boolean hasCustom(@NotNull PersistentDataHolder holder, @NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type) {
        return holder.getPersistentDataContainer().has(key, type);
    }

    public static <T, Z> void setCustom(@NotNull PersistentDataHolder holder, @NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type, @NotNull Z obj) {
        holder.getPersistentDataContainer().set(key, type, obj);
    }

    public static <T, Z> void removeCustom(@NotNull PersistentDataHolder holder, @NotNull NamespacedKey key) {
        holder.getPersistentDataContainer().remove(key);
    }

    // ==================== Quantum storage PDC data type (formerly PersistentQuantumStorageType) ====================

    public static final PersistentDataType<PersistentDataContainer, QuantumCache> QUANTUM_STORAGE_TYPE =
            new PersistentDataType<>() {
                @Override
                public @NotNull Class<PersistentDataContainer> getPrimitiveType() {
                    return PersistentDataContainer.class;
                }

                @Override
                public @NotNull Class<QuantumCache> getComplexType() {
                    return QuantumCache.class;
                }

                @Override
                public @NotNull PersistentDataContainer toPrimitive(@NotNull QuantumCache complex, @NotNull PersistentDataAdapterContext context) {
                    final PersistentDataContainer container = context.newPersistentDataContainer();

                    if (complex.getItemStack() != null) {
                        container.set(ITEM, DataType.ITEM_STACK, complex.getItemStack());
                    }
                    container.set(AMOUNT, DataType.LONG, complex.getAmountLong());
                    container.set(MAX_AMOUNT, DataType.LONG, complex.getLimitLong());
                    container.set(VOID, DataType.BOOLEAN, complex.isVoidExcess());
                    container.set(SUPPORTS_CUSTOM_MAX_AMOUNT, DataType.BOOLEAN, complex.supportsCustomMaxAmount());
                    return container;
                }

                @Override
                public @NotNull QuantumCache fromPrimitive(@NotNull PersistentDataContainer primitive, @NotNull PersistentDataAdapterContext context) {
                    ItemStack item = primitive.get(ITEM, DataType.ITEM_STACK);
                    if (item == null) {
                        item = primitive.get(ITEM2, DataType.ITEM_STACK);
                    }
                    if (item == null) {
                        item = primitive.get(ITEM3, DataType.ITEM_STACK);
                    }

                    long amount = readAmount(primitive);
                    long limit = readLimit(primitive);
                    boolean voidExcess = readVoidExcess(primitive);
                    boolean supportsCustomMaxAmount =
                            primitive.getOrDefault(SUPPORTS_CUSTOM_MAX_AMOUNT, DataType.BOOLEAN, false);

                    return new QuantumCache(item, amount, limit, voidExcess, supportsCustomMaxAmount);
                }
            };

    private static long readAmount(@NotNull PersistentDataContainer primitive) {
        Long amount;
        try {
            amount = primitive.get(AMOUNT, DataType.LONG);
            if (amount == null) {
                amount = primitive.get(AMOUNT2, DataType.LONG);
            }
            if (amount == null) {
                amount = primitive.getOrDefault(AMOUNT3, DataType.LONG, 0L);
            }
        } catch (Throwable ignored) {
            Integer amountI;
            amountI = primitive.get(AMOUNT, DataType.INTEGER);
            if (amountI == null) {
                amountI = primitive.get(AMOUNT2, DataType.INTEGER);
            }
            if (amountI == null) {
                amountI = primitive.getOrDefault(AMOUNT3, DataType.INTEGER, 0);
            }
            amount = amountI.longValue();
        }
        return amount;
    }

    private static long readLimit(@NotNull PersistentDataContainer primitive) {
        Long limit;
        try {
            limit = primitive.get(MAX_AMOUNT, DataType.LONG);
            if (limit == null) {
                limit = primitive.get(MAX_AMOUNT2, DataType.LONG);
            }
            if (limit == null) {
                limit = primitive.getOrDefault(MAX_AMOUNT3, DataType.LONG, 64L);
            }
        } catch (Throwable ignored) {
            Integer limitI;
            limitI = primitive.get(MAX_AMOUNT, DataType.INTEGER);
            if (limitI == null) {
                limitI = primitive.get(MAX_AMOUNT2, DataType.INTEGER);
            }
            if (limitI == null) {
                limitI = primitive.getOrDefault(MAX_AMOUNT3, DataType.INTEGER, 64);
            }
            limit = limitI.longValue();
        }
        return limit;
    }

    private static boolean readVoidExcess(@NotNull PersistentDataContainer primitive) {
        Boolean voidExcess = primitive.get(VOID, DataType.BOOLEAN);
        if (voidExcess == null) {
            voidExcess = primitive.get(VOID2, DataType.BOOLEAN);
        }
        if (voidExcess == null) {
            voidExcess = primitive.getOrDefault(VOID3, DataType.BOOLEAN, false);
        }
        return voidExcess;
    }

    // ==================== ItemStack cache (formerly ItemStackCache) ====================

    public static class ItemStackCache {

        private @Nullable ItemStack itemStack;
        private @Nullable ItemMeta itemMeta = null;
        private boolean metaCached = false;

        public ItemStackCache(@Nullable ItemStack itemStack) {
            this.itemStack = itemStack;
        }

        @Nullable
        public ItemStack getItemStack() {
            return this.itemStack;
        }

        public void setItemStack(ItemStack itemStack) {
            this.itemStack = itemStack;
            this.metaCached = false;
            this.itemMeta = null;
        }

        @Nullable
        public ItemMeta getItemMeta() {
            if (this.itemMeta == null && !this.metaCached) {
                this.itemMeta = itemStack == null ? null : itemStack.hasItemMeta() ? itemStack.getItemMeta() : null;
                this.metaCached = true;
            }
            return this.itemMeta;
        }

        @Nullable
        public Material getItemType() {
            return this.itemStack == null ? null : this.itemStack.getType();
        }
    }

    // ==================== Quantum cache (formerly QuantumCache) ====================

    public static class QuantumCache extends ItemStackCache {

        @Nullable
        private final ItemMeta storedItemMeta;

        private final boolean supportsCustomMaxAmount;

        private long limit;

        private long amount;

        private boolean voidExcess;

        public QuantumCache(
                @Nullable ItemStack storedItem,
                long amount,
                long limit,
                boolean voidExcess,
                boolean supportsCustomMaxAmount) {
            super(storedItem);
            this.storedItemMeta = storedItem == null ? null : storedItem.getItemMeta();
            this.amount = amount;
            this.limit = limit;
            this.voidExcess = voidExcess;
            this.supportsCustomMaxAmount = supportsCustomMaxAmount;
        }

        public int getLimit() {
            return limit > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) limit;
        }

        public long getLimitLong() {
            return limit;
        }

        public void setLimit(long limit) {
            this.limit = limit;
        }

        public long getAmount() {
            return amount;
        }

        public long getAmountLong() {
            return amount;
        }

        public int getAmountInt() {
            return amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
        }

        public boolean isVoidExcess() {
            return voidExcess;
        }

        public void setVoidExcess(boolean voidExcess) {
            this.voidExcess = voidExcess;
        }

        @Nullable
        public ItemMeta getStoredItemMeta() {
            return storedItemMeta;
        }

        public void setAmount(int amount) {
            if (amount < -2_000_000_000) {
                this.amount = -amount; // just for data fix in some case, normally nothing will reach -2B
            } else {
                this.amount = amount;
            }
        }

        public void setAmount(long amount) {
            if (amount < -2_000_000_000) {
                this.amount = -amount; // just for data fix in some case, normally nothing will reach -2B
            } else {
                this.amount = amount;
            }
        }

        public boolean supportsCustomMaxAmount() {
            return supportsCustomMaxAmount;
        }

        public int increaseAmount(int amount) {
            long total = this.amount + (long) amount;
            if (total > this.limit) {
                this.amount = this.limit;
                if (!this.voidExcess) {
                    return (int) (total - this.limit);
                }
            } else {
                this.amount = this.amount + amount;
            }
            return 0;
        }

        // Long version: supports batch storage above the int limit (for large outputs such as Ether aquarium arrays)
        // Returns the amount that could not be stored because capacity was exceeded (returns 0 when voidExcess=true, meaning all excess is consumed)
        public long increaseAmount(long amount) {
            long total = this.amount + amount;
            if (total > this.limit) {
                this.amount = this.limit;
                if (!this.voidExcess) {
                    return total - this.limit;
                }
            } else {
                this.amount = this.amount + amount;
            }
            return 0;
        }

        public void reduceAmount(int amount) {
            this.amount = this.amount - amount;
        }

        @Nullable
        public ItemStack withdrawItem(int amount) {
            if (this.getItemStack() == null) {
                return null;
            }
            final ItemStack clone = this.getItemStack().clone();
            clone.setAmount((int) Math.min(this.amount, amount));
            reduceAmount(clone.getAmount());
            return clone;
        }

        @Nullable
        public ItemStack withdrawItem() {
            if (this.getItemStack() == null) {
                return null;
            }
            return withdrawItem(this.getItemStack().getMaxStackSize());
        }

        public void addMetaLore(@NotNull ItemMeta itemMeta) {
            List<String> old = itemMeta.getLore();
            final List<String> lore = old != null ? new ArrayList<>(old) : new ArrayList<>();
            String itemName = "Empty";
            if (getItemStack() != null) {
                itemName = ItemStackHelper.getDisplayName(this.getItemStack());
            }
            lore.add("");
            lore.add(String.format("§eItem: %s", itemName));
            lore.add(String.format("§eAmount: §f%s", this.getAmount()));
            if (this.supportsCustomMaxAmount) {
                lore.add(String.format("§eCurrent capacity limit: §c%s", this.getLimit()));
            }

            itemMeta.setLore(lore);
        }

        public void updateMetaLore(@NotNull ItemMeta itemMeta) {
            List<String> lore = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            String itemName = "Empty";
            if (getItemStack() != null) {
                itemName = ItemStackHelper.getDisplayName(this.getItemStack());
            }
            final int loreIndexModifier = this.supportsCustomMaxAmount ? 1 : 0;
            lore.set(lore.size() - 2 - loreIndexModifier, String.format("§eItem: %s", itemName));
            lore.set(lore.size() - 1 - loreIndexModifier, String.format("§eAmount: §f%s", this.getAmount()));
            if (this.supportsCustomMaxAmount) {
                lore.set(lore.size() - loreIndexModifier, String.format("§eCurrent capacity limit: §c%s", this.getLimit()));
            }

            itemMeta.setLore(lore);
        }
    }

    // ==================== High-level API (called by machines such as aquariums) ====================

    private static @Nullable NamespacedKey findStorageKey(@Nullable ItemMeta meta) {
        if (meta == null) {
            return null;
        }
        if (hasCustom(meta, QUANTUM_STORAGE_INSTANCE, QUANTUM_STORAGE_TYPE)) {
            return QUANTUM_STORAGE_INSTANCE;
        }
        if (hasCustom(meta, QUANTUM_STORAGE_INSTANCE2, QUANTUM_STORAGE_TYPE)) {
            return QUANTUM_STORAGE_INSTANCE2;
        }
        if (hasCustom(meta, QUANTUM_STORAGE_INSTANCE3, QUANTUM_STORAGE_TYPE)) {
            return QUANTUM_STORAGE_INSTANCE3;
        }
        return null;
    }

    /**
     * Read the Networks quantum storage cache from an item's ItemMeta (supports three item-key variants).
     *
     * @param meta item metadata
     * @return quantum cache; null if none exists
     */
    @Nullable
    public static QuantumCache getQuantumCache(@NotNull ItemMeta meta) {
        NamespacedKey key = findStorageKey(meta);
        return key == null ? null : getCustom(meta, key, QUANTUM_STORAGE_TYPE);
    }

    /**
     * Check whether an item is a Networks quantum storage item bound to a specific item type.
     *
     * @param item item in the slot
     * @return true if quantum storage is connected and can accept the specified item
     */
    public static boolean isQuantumStorageItem(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        QuantumCache cache = getQuantumCache(item.getItemMeta());
        return cache != null && cache.getItemStack() != null && !cache.getItemStack().getType().isAir();
    }

    /**
     * Check whether a block is a NetworksExpansion quantum storage block (NetworkQuantumStorage).
     *
     * @param sfItem SlimefunItem for the target block
     * @return true if it is a quantum storage block
     */
    public static boolean isQuantumStorageBlock(@Nullable SlimefunItem sfItem) {
        return sfItem != null && QUANTUM_STORAGE_BLOCK_CLASS.equals(sfItem.getClass().getName());
    }

    /**
     * When Void Touch is bound to a quantum storage block, store the output directly in that block's quantum cache.
     * <p>
     * Call NetworksExpansion's getCaches / tryInputItem / syncBlock via reflection,
     * and let the plugin's QuantumCache handle max-value and overflow protection (capacity limits and configurable voidExcess).
     *
     * @param location location of the quantum storage block
     * @param output   item produced this time
     * @return amount left over; 0 means everything was stored
     */
    public static long storeToQuantumStorageBlock(@NotNull Location location, @NotNull ItemStack output) {
        try {
            Class<?> cls = Class.forName(QUANTUM_STORAGE_BLOCK_CLASS);

            Object cache = ((Map<?, ?>) cls.getMethod("getCaches").invoke(null)).get(location);
            if (cache == null) {
                return output.getAmount();
            }

            ItemStack stored = (ItemStack) cache.getClass().getMethod("getItemStack").invoke(cache);
            if (stored == null || stored.getType().isAir()) {
                return output.getAmount();
            }
            if (!SlimefunUtils.isItemSimilar(output, stored, true)) {
                return output.getAmount();
            }

            // tryInputItem writes the amount that could not be stored back to input[0],
            // so pass a clone to avoid changing the caller's (machine's) outItems
            ItemStack[] input = new ItemStack[]{ output.clone() };
            cls.getMethod("tryInputItem", Location.class, ItemStack[].class, cache.getClass())
                    .invoke(null, location, input, cache);
            cls.getMethod("syncBlock", Location.class, cache.getClass()).invoke(null, location, cache);

            ItemStack leftover = input[0];
            return leftover == null ? 0 : leftover.getAmount();
        } catch (Throwable t) {
            // Reflection failed (for example, NetworksExpansion is not installed); treat it as not stored
            return output.getAmount();
        }
    }

    /**
     * When Void Touch is bound to a quantum storage block, store items using an item template and a long amount,
     * bypassing ItemStack's per-call int limit (large-amount validation in 1.20.5+).
     * <p>
     * Call tryInputItem in batches via reflection without exceeding capacity; each batch is limited to the int maximum,
     * until everything is stored or storage stops accepting items (the remainder is returned to the caller).
     *
     * @param location  location of the quantum storage block
     * @param prototype item template (any amount; only used to compare item types)
     * @param outputAmount total amount to store this time (long; may exceed the int limit)
     * @return amount left over; 0 means everything was stored
     */
    public static long storeToQuantumStorageBlock(@NotNull Location location, @NotNull ItemStack prototype, long outputAmount) {
        if (outputAmount <= 0) {
            return 0;
        }
        try {
            Class<?> cls = Class.forName(QUANTUM_STORAGE_BLOCK_CLASS);

            Object cache = ((Map<?, ?>) cls.getMethod("getCaches").invoke(null)).get(location);
            if (cache == null) {
                return outputAmount;
            }

            ItemStack stored = (ItemStack) cache.getClass().getMethod("getItemStack").invoke(cache);
            if (stored == null || stored.getType().isAir()) {
                return outputAmount;
            }
            if (!SlimefunUtils.isItemSimilar(prototype, stored, true)) {
                return outputAmount;
            }

            // Call tryInputItem in batches to bypass the per-call int limit; each batch is min(remaining, int limit)
            long remaining = outputAmount;
            while (remaining > 0) {
                int batch = (int) Math.min(remaining, Integer.MAX_VALUE);
                ItemStack[] input = new ItemStack[]{ prototype.clone() };
                input[0].setAmount(batch);
                cls.getMethod("tryInputItem", Location.class, ItemStack[].class, cache.getClass())
                        .invoke(null, location, input, cache);
                ItemStack leftover = input[0];
                int notStored = leftover == null ? 0 : leftover.getAmount();
                long storedThisBatch = batch - notStored;
                if (storedThisBatch <= 0) {
                    break; // Storage is full or no longer accepting items; leave the remainder for the next tick
                }
                remaining -= storedThisBatch;
            }
            cls.getMethod("syncBlock", Location.class, cache.getClass()).invoke(null, location, cache);
            return remaining;
        } catch (Throwable t) {
            // Reflection failed (for example, NetworksExpansion is not installed); treat it as not stored
            return outputAmount;
        }
    }

    /**
     * When Void Touch is bound to a Magic Storage terminal (CargoCoreMore), put the output into a slot that already contains that item.
     * <p>
     * Like aquariums, only store items if that item type is already in the terminal (do not create a new slot).
     *
     * @param location Magic Storage terminal location
     * @param item     item produced this time
     * @return true if the item was stored in the terminal
     */
    public static boolean storeToCargoCore(@NotNull Location location, @NotNull ItemStack item) {
        BlockMenu inv = StorageCacheUtils.getMenu(location);
        if (inv == null) {
            return false;
        }
        SlimefunBlockData data = StorageCacheUtils.getBlock(location);
        if (data == null) {
            return false;
        }
        if (hasStoredItem(data, item)) {
            storeItemToExistingSlot(data, item);
            return true;
        }
        return false;
    }

    /** Check whether CargoCoreMore already has this item (amount greater than 0) */
    private static boolean hasStoredItem(SlimefunBlockData data, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemStack prototype = item.clone();
        prototype.setAmount(1);

        for (int i = 0; i < CARGO_CORE_MAX_STORED_ITEMS; i++) {
            String jsonData = data.getData("item_type_" + i);
            if (jsonData == null || jsonData.isEmpty()) {
                continue;
            }

            try {
                ItemStack storedItem = itemFromBase64(jsonData);
                if (storedItem != null && storedItem.getType() != Material.AIR) {
                    storedItem.setAmount(1);
                    if (SlimefunUtils.isItemSimilar(prototype, storedItem, true)) {
                        String countStr = data.getData("item_count_" + i);
                        if (countStr != null && !countStr.isEmpty()) {
                            try {
                                if (Long.parseLong(countStr) > 0) {
                                    return true;
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    /** Store only in slots that already contain the item (do not create new slots), with max-value protection */
    private static void storeItemToExistingSlot(SlimefunBlockData data, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        ItemStack prototype = item.clone();
        prototype.setAmount(1);
        long amountToStore = item.getAmount();

        for (int i = 0; i < CARGO_CORE_MAX_STORED_ITEMS; i++) {
            String jsonData = data.getData("item_type_" + i);
            if (jsonData == null || jsonData.isEmpty()) {
                continue;
            }

            try {
                ItemStack storedItem = itemFromBase64(jsonData);
                if (storedItem != null && storedItem.getType() != Material.AIR) {
                    storedItem.setAmount(1);
                    if (SlimefunUtils.isItemSimilar(prototype, storedItem, true)) {
                        long currentCount = 0;
                        String countStr = data.getData("item_count_" + i);
                        if (countStr != null && !countStr.isEmpty()) {
                            try {
                                currentCount = Long.parseLong(countStr);
                            } catch (NumberFormatException ignored) {
                            }
                        }

                        // Max-value protection: the new amount will not overflow
                        long newCount = amountToStore > Long.MAX_VALUE - currentCount
                                ? Long.MAX_VALUE : currentCount + amountToStore;
                        data.setData("item_count_" + i, String.valueOf(newCount));

                        // Amount limit protection
                        String maxStr = data.getData("item_max_" + i);
                        if (maxStr != null && !maxStr.isEmpty()) {
                            try {
                                long maxCount = Long.parseLong(maxStr);
                                if (maxCount != -1 && newCount > maxCount) {
                                    data.setData("item_count_" + i, String.valueOf(maxCount));
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        return;
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static final int CARGO_CORE_MAX_STORED_ITEMS = 1145;

    /**
     * Store output in a quantum storage item (with max-value and overflow protection).
     * <p>
     * Only store up to the remaining capacity and the int limit, without exceeding capacity or causing numeric overflow;
     * After a successful write, the item's lore is updated and saved back to ItemMeta.
     *
     * @param storageItem quantum storage item (the caller must put the updated item back in its slot)
     * @param output      item produced this time
     * @return amount left over; 0 means everything was stored
     */
    public static long store(@NotNull ItemStack storageItem, @NotNull ItemStack output) {
        if (!storageItem.hasItemMeta()) {
            return output.getAmount();
        }
        ItemMeta meta = storageItem.getItemMeta();
        NamespacedKey key = findStorageKey(meta);
        if (key == null) {
            return output.getAmount();
        }
        QuantumCache cache = getCustom(meta, key, QUANTUM_STORAGE_TYPE);
        if (cache == null || cache.getItemStack() == null) {
            return output.getAmount();
        }
        // Cannot store items of different types
        if (!SlimefunUtils.isItemSimilar(output, cache.getItemStack(), true)) {
            return output.getAmount();
        }

        long stored = cache.getAmountLong();
        long limit = cache.getLimitLong();

        // Max-value protection: remaining space cannot be negative
        long remaining = limit > stored ? limit - stored : 0L;
        // Overflow protection: store at most the output amount or remaining capacity per call, and never exceed the int limit (increaseAmount only accepts int)
        long toAdd = Math.min(output.getAmount(), remaining);
        if (toAdd > Integer.MAX_VALUE) {
            toAdd = Integer.MAX_VALUE;
        }
        if (toAdd <= 0) {
            return output.getAmount();
        }

        cache.increaseAmount((int) toAdd);
        setCustom(meta, key, QUANTUM_STORAGE_TYPE, cache);
        refreshLore(meta, cache);
        storageItem.setItemMeta(meta);

        return output.getAmount() - toAdd;
    }

    /**
     * Store output in quantum storage in batches using an item template and a long amount,
     * bypassing ItemStack's per-call int limit (large-amount validation in 1.20.5+).
     * <p>
     * Unlike {@link #store(ItemStack, ItemStack)}, this method does not rely on ItemStack#getAmount() (int limit),
     * and accepts the total amount as a long, supporting batches over 2.1 billion items (for Ether aquarium arrays);
     * it still caps capacity and protects against overflow, then refreshes the item's lore after storing.
     *
     * @param storageItem quantum storage item (the caller must put the updated item back in its slot)
     * @param prototype   item template (any amount; only used to compare item types)
     * @param outputAmount total amount to store this time (long; may exceed the int limit)
     * @return amount left over; 0 means everything was stored
     */
    public static long store(@NotNull ItemStack storageItem, @NotNull ItemStack prototype, long outputAmount) {
        if (outputAmount <= 0) {
            return 0;
        }
        if (!storageItem.hasItemMeta()) {
            return outputAmount;
        }
        ItemMeta meta = storageItem.getItemMeta();
        NamespacedKey key = findStorageKey(meta);
        if (key == null) {
            return outputAmount;
        }
        QuantumCache cache = getCustom(meta, key, QUANTUM_STORAGE_TYPE);
        if (cache == null || cache.getItemStack() == null) {
            return outputAmount;
        }
        // Cannot store items of different types
        if (!SlimefunUtils.isItemSimilar(prototype, cache.getItemStack(), true)) {
            return outputAmount;
        }

        long stored = cache.getAmountLong();
        long limit = cache.getLimitLong();

        // Max-value protection: remaining space cannot be negative
        long remaining = limit > stored ? limit - stored : 0L;
        // Only store as much as the remaining capacity allows
        long toAdd = Math.min(outputAmount, remaining);
        if (toAdd <= 0) {
            return outputAmount;
        }

        cache.increaseAmount(toAdd);
        setCustom(meta, key, QUANTUM_STORAGE_TYPE, cache);
        refreshLore(meta, cache);
        storageItem.setItemMeta(meta);

        return outputAmount - toAdd;
    }

    /**
     * Calculate how many items can still fit in output slots (Plan A: count empty slots only; each empty slot holds 64 items).
     * <p>
     * This limit applies only when no external storage is connected; it does not apply when quantum storage or a Magic Storage terminal is connected.
     *
     * @param menu        machine menu
     * @param outputSlots output slots
     * @param output      item produced this time (not used in Plan A; kept for caller compatibility)
     * @return remaining capacity (with integer overflow protection)
     */
    public static int calculateFitAmount(@NotNull BlockMenu menu, int[] outputSlots, @NotNull ItemStack output) {
        int emptySlots = 0;
        for (int slot : outputSlots) {
            ItemStack item = menu.getItemInSlot(slot);
            if (item == null || item.getType().isAir()) {
                emptySlots++;
            }
        }
        // Integer overflow protection (48 slots can hold at most 3072, so this should never trigger)
        return emptySlots > Integer.MAX_VALUE / 64 ? Integer.MAX_VALUE : emptySlots * 64;
    }

    public static void refreshLore(@NotNull ItemMeta meta, @NotNull QuantumCache cache) {
        try {
            cache.updateMetaLore(meta);
        } catch (Throwable t) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            int suffix = cache.supportsCustomMaxAmount() ? 3 : 2;
            while (suffix-- > 0 && !lore.isEmpty()) {
                lore.remove(lore.size() - 1);
            }
            meta.setLore(lore);
            cache.addMetaLore(meta);
        }
    }
}

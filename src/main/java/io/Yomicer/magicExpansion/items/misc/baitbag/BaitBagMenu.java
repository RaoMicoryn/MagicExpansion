package io.Yomicer.magicExpansion.items.misc.baitbag;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.core.MagicExpansionItems;
import io.Yomicer.magicExpansion.items.misc.CargoFragment;
import io.Yomicer.magicExpansion.items.tools.VoidTouch;
import io.Yomicer.magicExpansion.utils.NetworkStorage;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static io.Yomicer.magicExpansion.core.MagicExpansionItems.FISHING_ROD_BETWEEN_WATER_CLOUD_REED;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientNameVer2;
import static io.Yomicer.magicExpansion.utils.SameItemJudge.itemFromBase64;
import static io.Yomicer.magicExpansion.utils.SameItemJudge.itemToBase64;

/**
 * Bait bag menu: fixed slots store Dreamweaver / Water Cloud Realm / Memory Fragment baits, supports priority and input from external storage
 */
public final class BaitBagMenu {

    /** Bait registry (id / fishing logic key / series / display item) */
    public record BaitEntry(String id, String key, String series, ItemStack item) {
    }

    private static final List<BaitEntry> BAITS = List.of(
            new BaitEntry("fishlurebasic", "fishLureBasic", "Dreamweaver", MagicExpansionItems.FISH_LURE_BASIC),
            new BaitEntry("fishluredust", "fishLureDust", "Dreamweaver", MagicExpansionItems.FISH_LURE_DUST),
            new BaitEntry("fishlureore", "fishLureOre", "Dreamweaver", MagicExpansionItems.FISH_LURE_ORE),
            new BaitEntry("fishlurealloy", "fishLureAlloyIngot", "Dreamweaver", MagicExpansionItems.FISH_LURE_ALLOY_INGOT),
            new BaitEntry("cuixia", "CuiXia", "Water Cloud Realm", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_CUIXIA),
            new BaitEntry("weichen", "WeiChen", "Water Cloud Realm", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_WEICHEN),
            new BaitEntry("ronghuo", "RongHuo", "Water Cloud Realm", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_RONGHUO),
            new BaitEntry("yuejin", "YueJin", "Water Cloud Realm", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_YUEJIN),
            new BaitEntry("xinghe", "XingHe", "Water Cloud Realm", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XINGHE),
            new BaitEntry("memory", "fishLureFinal", "Memory Fragment", memoryFragment()),
            new BaitEntry("jianjia", "JianJia", "Reed Blossom", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_REED_JIANJIA),
            new BaitEntry("luxue", "LuXue", "Reed Blossom", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_REED_LUXUE),
            new BaitEntry("weilu", "WeiLu", "Reed Blossom", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_REED_WEILU),
            new BaitEntry("bailu", "BaiLu", "Reed Blossom", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_REED_BAILU),
            new BaitEntry("luya", "LuYa", "Reed Blossom", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_REED_LUYA),
            new BaitEntry("ningshuang", "NingShuang", "Cold River Snow", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_HANJIANG_NINGSHUANG),
            new BaitEntry("luoxu", "LuoXu", "Cold River Snow", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_HANJIANG_LUOXU),
            new BaitEntry("bingpo", "BingPo", "Cold River Snow", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_HANJIANG_BINGPO),
            new BaitEntry("chuji", "ChuJi", "Cold River Snow", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_HANJIANG_CHUJI),
            new BaitEntry("chuilun", "ChuiLun", "Cold River Snow", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_HANJIANG_CHUILUN),
            new BaitEntry("fengsi", "FengSi", "Drizzle · Slant Wind", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XIYU_FENGSI),
            new BaitEntry("yanyu", "YanYu", "Drizzle · Slant Wind", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XIYU_YANYU),
            new BaitEntry("lianbai", "LianBai", "Drizzle · Slant Wind", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XIYU_LIANBAI),
            new BaitEntry("xiaofeng", "XiaoFeng", "Drizzle · Slant Wind", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XIYU_XIAOFENG),
            new BaitEntry("xieying", "XieYing", "Drizzle · Slant Wind", MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XIYU_XIEYING),
            new BaitEntry("magicsugar", "magic_sugar", "Dreamweaver", SlimefunItems.MAGIC_SUGAR)
    );

    // ==================== 4×7 layout data model (series → rod → bait, data-driven, just register a new rod here and it lays out automatically) ====================

    /** Rod layout: display name / rod icon / list of bait keys this rod uses */
    private record RodLayout(String rodName, ItemStack rodIcon, List<String> lureKeys) {
    }

    /** Series layout: series name / series icon / list of rods in this series */
    private record SeriesLayout(String seriesName, Material seriesIcon, List<RodLayout> rods) {
    }

    /** Starting slot of each row in the middle 4×7 storage area */
    private static final int[] GRID_ROWS = {10, 19, 28, 37};

    // ==================== Paging helpers (build rows first → page by rows → repeat the series header on each page) ====================

    /** Rows per page = 4 */
    private static final int PAGE_ROWS = GRID_ROWS.length;

    /** Current page bait → slot map (rebuilt when turning pages / refreshing) */
    private static final Map<BaitEntry, Integer> PAGE_SLOTS = new java.util.LinkedHashMap<>();

    /** Render cell type */
    private enum CellType { SERIES, ROD, BAIT }

    /** A render unit that takes one slot (series header / rod display / bait) */
    private record GridCell(CellType type, String seriesName, Material seriesIcon,
                            RodLayout rod, BaitEntry bait) {
        static GridCell series(String name, Material icon) {
            return new GridCell(CellType.SERIES, name, icon, null, null);
        }
        static GridCell rod(RodLayout rod) {
            return new GridCell(CellType.ROD, null, null, rod, null);
        }
        static GridCell bait(BaitEntry bait) {
            return new GridCell(CellType.BAIT, null, null, null, bait);
        }
    }

    /** One row (7 columns): the start of the row can have a series header anchor, the rest is "this rod + its baits" */
    private record Row(GridCell seriesAnchor, List<GridCell> cells) {
    }

    /** Turn LAYOUT into rows: each series starts with a "series header row", and every rod in that series gets its own row (row start = series header (first rod only) or empty) */
    private static List<Row> buildRows() {
        List<Row> rows = new ArrayList<>();
        for (SeriesLayout series : LAYOUT) {
            GridCell anchor = GridCell.series(series.seriesName(), series.seriesIcon());
            boolean firstRod = true;
            for (RodLayout rod : series.rods()) {
                // One rod per row: the first rod's row starts with the series header, later rods leave the start empty
                Row row = new Row(firstRod ? anchor : null, new ArrayList<>());
                row.cells().add(GridCell.rod(rod));
                for (String key : rod.lureKeys()) {
                    BaitEntry entry = findEntryByKey(key);
                    if (entry != null) {
                        row.cells().add(GridCell.bait(entry));
                    }
                }
                rows.add(row);
                firstRod = false;
            }
        }
        return rows;
    }

    /** Cut the finished rows into pages of PAGE_ROWS rows (no replaying across rows, one rod per row keeps it tidy) */
    private static List<List<Row>> buildPages() {
        List<Row> rows = ROWS;
        List<List<Row>> pages = new ArrayList<>();
        int idx = 0;
        while (idx < rows.size()) {
            List<Row> page = new ArrayList<>();
            int count = 0;
            while (idx < rows.size() && count < PAGE_ROWS) {
                page.add(rows.get(idx));
                count++;
                idx++;
            }
            pages.add(page);
        }
        if (pages.isEmpty()) {
            pages.add(List.of());
        }
        return pages;
    }

    /** Series → rod → bait layout (register new rods here, they lay out automatically) */
    private static final List<SeriesLayout> LAYOUT = List.of(
            new SeriesLayout("Dreamweaver", Material.GHAST_TEAR, List.of(
                    new RodLayout("Beginner Rod / Windspeaker Rod", MagicExpansionItems.FISHING_ROD_WIND_SPEAKER,
                            List.of("fishLureBasic", "fishLureDust", "fishLureOre", "fishLureAlloyIngot", "magic_sugar")),
                    new RodLayout("Entangled Knot: Thread of the End · Paradox Hook", MagicExpansionItems.FISHING_ROD_FINAL_STICK,
                            List.of("fishLureFinal"))
            )),
            new SeriesLayout("Water Cloud Realm", Material.CYAN_DYE, List.of(
                    new RodLayout("Green Bamboo Rod", MagicExpansionItems.FISHING_ROD_BETWEEN_WATER_CLOUD_CYAN_BAMBOO,
                            List.of("CuiXia", "WeiChen", "RongHuo", "YueJin", "XingHe")),
                    new RodLayout("Reed Blossom Rod", FISHING_ROD_BETWEEN_WATER_CLOUD_REED,
                            List.of("JianJia", "LuXue", "WeiLu", "BaiLu", "LuYa")),
                    new RodLayout("Cold River Snow", MagicExpansionItems.FISHING_ROD_BETWEEN_WATER_CLOUD_HANJIANG,
                            List.of("NingShuang", "LuoXu", "BingPo", "ChuJi", "ChuiLun")),
                    new RodLayout("Drizzle · Slant Wind", MagicExpansionItems.FISHING_ROD_BETWEEN_WATER_CLOUD_XIYU,
                            List.of("FengSi", "YanYu", "LianBai", "XiaoFeng", "XieYing"))
            ))
    );

    /** Finished rows (initialized after LAYOUT so it's visible) */
    private static final List<Row> ROWS = buildRows();

    /** List of pages made by paging by rows */
    private static final List<List<Row>> PAGES = buildPages();

    /** Total pages */
    private static int pageCount() {
        return PAGES.size();
    }

    /** Bait → storage slot map (built statically, shared with rendering) */
    private static final Map<BaitEntry, Integer> ENTRY_SLOTS = new java.util.LinkedHashMap<>();

    static {
        int row = 0;
        int col = 0;
        for (SeriesLayout series : LAYOUT) {
            if (col != 0) {
                row++;
                col = 0;
            }
            col = 1; // slot 0 = series icon slot
            for (RodLayout rod : series.rods()) {
                if (col > 6) {
                    // This row is full: go to a new row, the row start is a decoration slot (series icon isn't repeated)
                    row++;
                    col = 1;
                }
                // Fix (L2): boundary protection — if the row number overflows, put it in the last row instead of throwing an array out of bounds exception
                if (row >= GRID_ROWS.length) row = GRID_ROWS.length - 1;
                col++; // rod display slot takes 1 slot
                for (String key : rod.lureKeys()) {
                    if (col > 6) {
                        row++;
                        col = 1;
                        // Fix (L2): boundary protection — if the row number overflows, put it in the last row
                        if (row >= GRID_ROWS.length) row = GRID_ROWS.length - 1;
                    }
                    BaitEntry entry = findEntryByKey(key);
                    if (entry != null) {
                        ENTRY_SLOTS.put(entry, GRID_ROWS[row] + col);
                    }
                    col++;
                }
            }
            row++;
            col = 0;
        }
    }

    private static BaitEntry findEntryByKey(String key) {
        for (BaitEntry entry : BAITS) {
            if (entry.key().equals(key)) return entry;
        }
        return null;
    }

    /** The old fixed slot layout has been replaced by the data-driven LAYOUT (constants kept so old code still works) */
    private static final int INPUT_SLOT = 49;

    // Ether Chest internal data key (same as CargoFragment)
    private static final NamespacedKey KEY_FRAGMENT_ITEM = new NamespacedKey(MagicExpansion.getInstance(), "cargo_item_json");
    private static final NamespacedKey KEY_FRAGMENT_AMOUNT = new NamespacedKey(MagicExpansion.getInstance(), "cargo_amount");
    // Void Touch bound location key (same as the vivarium)
    private static final NamespacedKey KEY_X = new NamespacedKey(MagicExpansion.getInstance(), "touch_x");
    private static final NamespacedKey KEY_Y = new NamespacedKey(MagicExpansion.getInstance(), "touch_y");
    private static final NamespacedKey KEY_Z = new NamespacedKey(MagicExpansion.getInstance(), "touch_z");
    private static final NamespacedKey KEY_WORLD = new NamespacedKey(MagicExpansion.getInstance(), "touch_world");
    private static final NamespacedKey KEY_INPUT_ITEM = new NamespacedKey(MagicExpansion.getInstance(), "baitbag_input_item");
    private static final NamespacedKey KEY_BAG_ID = new NamespacedKey(MagicExpansion.getInstance(), "baitbag_id");
    private static final String QUANTUM_STORAGE_BLOCK_CLASS =
            "io.github.sefiraat.networks.slimefun.network.NetworkQuantumStorage";

    private BaitBagMenu() {
    }

    // ==================== Memory Fragment (reuses the way the fishing pool makes it) ====================

    private static ItemStack memoryFragment() {
        return new CustomItemStack(new ItemStack(Material.PRISMARINE_SHARD),
                getGradientNameVer2("Bait · Memory Fragment"),
                "§fThis bait can catch any item",
                "§fIt exists in the past, or maybe the future",
                "§fWhat you see now isn't its true form");
    }

    // ==================== PDC storage ====================

    private static NamespacedKey amountKey(String id) {
        return new NamespacedKey(MagicExpansion.getInstance(), "baitbag_" + id + "_amount");
    }

    private static NamespacedKey priorityKey(String id) {
        return new NamespacedKey(MagicExpansion.getInstance(), "baitbag_" + id + "_priority");
    }

    /** Read the amount (stored as long, works with old int data and migrates it automatically) */
    public static long getAmount(ItemStack bag, BaitEntry entry) {
        if (bag == null || !bag.hasItemMeta()) return 0;
        PersistentDataContainer c = bag.getItemMeta().getPersistentDataContainer();
        Long v = c.get(amountKey(entry.id()), PersistentDataType.LONG);
        if (v != null) return v;
        Integer old = c.get(amountKey(entry.id()), PersistentDataType.INTEGER);
        return old == null ? 0 : old;
    }

    private static void setAmount(ItemStack bag, BaitEntry entry, long amount) {
        if (amount < 0) amount = 0; // Overflow protection: no negative numbers
        ItemMeta meta = bag.getItemMeta();
        meta.getPersistentDataContainer().set(amountKey(entry.id()), PersistentDataType.LONG, amount);
        bag.setItemMeta(meta);
    }

    private static int getPriority(ItemStack bag, BaitEntry entry) {
        if (bag == null || !bag.hasItemMeta()) return defaultPriority(entry);
        Integer v = bag.getItemMeta().getPersistentDataContainer().get(priorityKey(entry.id()), PersistentDataType.INTEGER);
        return v == null ? defaultPriority(entry) : v;
    }

    /** Default priority: within the same series, 1~X from left to right */
    private static int defaultPriority(BaitEntry entry) {
        int order = 0;
        for (BaitEntry other : BAITS) {
            if (!other.series().equals(entry.series())) continue;
            order++;
            if (other == entry) return order;
        }
        return 1;
    }

    private static void setPriority(ItemStack bag, BaitEntry entry, int priority) {
        ItemMeta meta = bag.getItemMeta();
        meta.getPersistentDataContainer().set(priorityKey(entry.id()), PersistentDataType.INTEGER, priority);
        bag.setItemMeta(meta);
    }

    /** How many entries this series has in the bait registry (BAITS), used as the priority denominator (counted dynamically, new series work automatically) */
    private static int seriesSize(String series) {
        int size = 0;
        for (BaitEntry entry : BAITS) {
            if (entry.series().equals(series)) {
                size++;
            }
        }
        return Math.max(1, size);
    }

    // ==================== Finding the bag ====================

    public static boolean isBaitBag(ItemStack item) {
        return item != null && SlimefunItem.getByItem(item) instanceof BaitBag;
    }

    private static int findBagSlot(Player player, ItemStack target) {
        PlayerInventory inv = player.getInventory();
        if (target != null) {
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack it = inv.getItem(i);
                if (it != null && SlimefunUtils.isItemSimilar(it, target, true)) return i;
            }
        }
        for (int i = 0; i < inv.getSize(); i++) {
            if (isBaitBag(inv.getItem(i))) return i;
        }
        return -1;
    }

    private static int findBagSlotById(Player player, String id) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && id.equals(getBagId(it))) return i;
        }
        return -1;
    }

    private static int findFreshBagSlot(Player player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (isBaitBag(it) && getBagId(it) == null) return i;
        }
        return -1;
    }

    private static String getBagId(ItemStack bag) {
        if (bag == null || !bag.hasItemMeta()) return null;
        return bag.getItemMeta().getPersistentDataContainer().get(KEY_BAG_ID, PersistentDataType.STRING);
    }

    private static void setBagId(ItemStack bag, String id) {
        ItemMeta meta = bag.getItemMeta();
        meta.getPersistentDataContainer().set(KEY_BAG_ID, PersistentDataType.STRING, id);
        bag.setItemMeta(meta);
    }

    // ==================== Fishing consumption (called by the listener) ====================

    /**
     * Use 1 available bait from the bait bag in the player's inventory by priority, and return the key of the used bait (smaller number is used first)
     *
     * @return the key of the used bait (like CuiXia / fishLureBasic / fishLureFinal), or null if there's no bag or no bait
     */
    public static String tryConsumeFromBag(Player player, Set<String> supportedKeys) {
        // Main hand → off hand → hotbar (left to right) → inventory (top to bottom, left to right), only uses the first bag that has bait
        for (int slot : bagSlotsInOrder(player)) {
            ItemStack bag = player.getInventory().getItem(slot);
            if (!isBaitBag(bag)) continue;

            List<BaitEntry> candidates = new ArrayList<>();
            for (BaitEntry entry : BAITS) {
                if (!supportedKeys.contains(entry.key())) continue;
                if (getAmount(bag, entry) > 0) candidates.add(entry);
            }
            if (candidates.isEmpty()) continue;

            candidates.sort((a, b) -> {
                int pa = getPriority(bag, a);
                int pb = getPriority(bag, b);
                if (pa != pb) return Integer.compare(pa, pb); // smaller priority number is used first
                return Integer.compare(BAITS.indexOf(a), BAITS.indexOf(b)); // same priority goes in the order Dreamweaver → Water Cloud Realm → Memory Fragment
            });

            BaitEntry chosen = candidates.get(0);
            setAmount(bag, chosen, getAmount(bag, chosen) - 1);
            player.getInventory().setItem(slot, bag);
            return chosen.key();
        }
        return null;
    }

    /**
     * Read-only lock: check by priority for the key of the first available bait in the bag (doesn't use it, used to lock the catch pool before the catch is settled)
     *
     * @return the key of an available bait (like CuiXia / fishLureBasic), or null if there's no bag or no bait
     */
    public static String peekFromBag(Player player, Set<String> supportedKeys) {
        for (int slot : bagSlotsInOrder(player)) {
            ItemStack bag = player.getInventory().getItem(slot);
            if (!isBaitBag(bag)) continue;

            List<BaitEntry> candidates = new ArrayList<>();
            for (BaitEntry entry : BAITS) {
                if (!supportedKeys.contains(entry.key())) continue;
                if (getAmount(bag, entry) > 0) candidates.add(entry);
            }
            if (candidates.isEmpty()) continue;

            candidates.sort((a, b) -> {
                int pa = getPriority(bag, a);
                int pb = getPriority(bag, b);
                if (pa != pb) return Integer.compare(pa, pb); // smaller priority number is used first
                return Integer.compare(BAITS.indexOf(a), BAITS.indexOf(b)); // same priority goes in the order Dreamweaver → Water Cloud Realm → Memory Fragment
            });
            return candidates.get(0).key();
        }
        return null;
    }

    /**
     * Use 1 bait from the bag by the given key (nothing is used if the bag is lost or has no such bait)
     *
     * @return whether it was used successfully
     */
    public static boolean consumeFromBagByKey(Player player, String key) {
        for (int slot : bagSlotsInOrder(player)) {
            ItemStack bag = player.getInventory().getItem(slot);
            if (!isBaitBag(bag)) continue;
            for (BaitEntry entry : BAITS) {
                if (!entry.key().equals(key)) continue;
                long amount = getAmount(bag, entry);
                if (amount <= 0) continue;
                setAmount(bag, entry, amount - 1);
                player.getInventory().setItem(slot, bag);
                return true;
            }
        }
        return false;
    }

    /** Slot order: main hand → off hand → hotbar → inventory */
    private static List<Integer> bagSlotsInOrder(Player player) {
        PlayerInventory inv = player.getInventory();
        List<Integer> slots = new ArrayList<>();
        int held = inv.getHeldItemSlot();
        slots.add(held);
        slots.add(40);
        for (int i = 0; i < 9; i++) {
            if (i != held) slots.add(i);
        }
        for (int i = 9; i < 36; i++) {
            slots.add(i);
        }
        return slots;
    }

    // ==================== Menu ====================

    public static void open(Player player, ItemStack bag) {
        openPage(player, bag, 0);
    }

    private static void openPage(Player player, ItemStack bag, int page) {
        String bagId = getBagId(bag);
        int slot = bagId != null ? findBagSlotById(player, bagId) : -1;
        if (slot < 0) {
            // New bag: find the first Cloud Dream Bag that has no unique ID yet (the one that was clicked)
            slot = findFreshBagSlot(player);
        }
        if (slot < 0) {
            slot = findBagSlot(player, bag); // fallback
        }
        if (slot < 0) {
            player.sendMessage(getGradientNameVer2("The bait bag isn't in your inventory"));
            return;
        }
        ItemStack current = player.getInventory().getItem(slot);
        if (current == null) return;
        if (bagId == null) {
            // First open: write a unique ID, then find it exactly by ID later, so it doesn't get mixed up with other Cloud Dream Bags
            setBagId(current, UUID.randomUUID().toString());
            player.getInventory().setItem(slot, current);
        }
        final int bagSlot = slot;

        ChestMenu menu = new ChestMenu(getGradientNameVer2("✦ Bait Bag ✦"));
        menu.setEmptySlotsClickable(false);
        menu.setPlayerInventoryClickable(true);
        menu.addMenuOpeningHandler(p -> p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f));

        // White filler (outer decoration, not including the middle 4×7 storage area)
        for (int s : new int[]{0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 53}) {
            menu.addItem(s, plainPane(Material.WHITE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        // Pink input slots (named)
        for (int s : new int[]{47, 48, 50, 51}) {
            menu.addItem(s, new CustomItemStack(Material.PINK_STAINED_GLASS_PANE, getGradientNameVer2("Input Slot")), ChestMenuUtils.getEmptyClickHandler());
        }
        // Middle 4×7 storage area: lay light blue decoration first, then renderGrid covers it (series icon / rod display / bait storage)
        for (int s : new int[]{10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43}) {
            menu.addItem(s, plainPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        renderGrid(menu, current, bagSlot, page);

        // slot 4: how to use
        menu.addItem(4, new CustomItemStack(Material.BOOK,
                getGradientNameVer2("Cloud Dream Bag"),
                getGradientNameVer2("Left-click to put in the matching bait"),
                getGradientNameVer2("Left-click with an empty hand to take out 1"),
                getGradientNameVer2("Shift+Left-click to take out all (up to 3456)"),
                getGradientNameVer2("Right-click to cycle the priority")), ChestMenuUtils.getEmptyClickHandler());

        // Series labels and bait slots are all rendered by renderGrid (data-driven)

        // Page slots (46 = previous page / 52 = next page): can turn = yellow-green glass pane, can't turn = white glass pane (same as the guide menu template)
        int totalPages = Math.max(1, pageCount());
        boolean hasPrev = page > 0;
        boolean hasNext = page < totalPages - 1;
        if (hasPrev) {
            menu.addItem(46, plainPane(Material.LIME_STAINED_GLASS_PANE));
            menu.addMenuClickHandler(46, (p, s, it, a) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                openPage(p, bag, page - 1);
                return false;
            });
        } else {
            menu.addItem(46, plainPane(Material.WHITE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        if (hasNext) {
            menu.addItem(52, plainPane(Material.LIME_STAINED_GLASS_PANE));
            menu.addMenuClickHandler(52, (p, s, it, a) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                openPage(p, bag, page + 1);
                return false;
            });
        } else {
            menu.addItem(52, plainPane(Material.WHITE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }

        // Input slot: empty by default, only stores when the conditions are met
        ItemStack inputItem = getInputItem(current);
        if (inputItem != null) {
            menu.addItem(INPUT_SLOT, inputItem, ChestMenuUtils.getEmptyClickHandler());
        }
        menu.addMenuClickHandler(INPUT_SLOT, (p, s, it, a) -> {
            handleInput(p, menu, current, bagSlot, a);
            return false;
        });
        // Shift+left-click an inventory item: quickly put it in the input slot (when the input slot is empty)
        menu.addPlayerInventoryClickHandler((p, s, it, a) -> {
            if (!a.isShiftClicked() || it == null || it.getType().isAir()) return true;
            if (getInputItem(current) != null) return true;
            fastInputFromInventory(p, menu, current, bagSlot, it, s);
            return false;
        });

        // Bait slots: registered by renderGrid following the layout (data-driven)

        menu.open(player);
    }

    /**
     * Render one page of the middle 4×7 storage area (rendered by rows, the series header is kept at the top of the page).
     * Each page has at most PAGE_ROWS rows; turn pages with slots 46/52. When refreshing, the current page's bait → slot map is rebuilt.
     */
    private static void renderGrid(ChestMenu menu, ItemStack bag, int bagSlot, int page) {
        if (page < 0 || page >= PAGES.size()) {
            return;
        }

        // Reset the current page's bait → slot map (used to find slots when refreshing)
        PAGE_SLOTS.clear();

        List<Row> pageRows = PAGES.get(page);
        int rowIdx = 0;
        for (Row rw : pageRows) {
            int row = rowIdx;
            if (row >= GRID_ROWS.length) {
                // Safety: if there are more than 4 rows, drop the rest (shouldn't happen)
                break;
            }
            int col = 0;
            if (rw.seriesAnchor() != null) {
                GridCell anchor = rw.seriesAnchor();
                menu.addItem(GRID_ROWS[row], new CustomItemStack(anchor.seriesIcon(),
                        getGradientNameVer2(anchor.seriesName()),
                        getGradientNameVer2("Priority 1~" + seriesSize(anchor.seriesName()))), ChestMenuUtils.getEmptyClickHandler());
            } else {
                // No series header at the row start: put an empty placeholder slot (lined up with the series header, filled with the same light blue glass pane as the storage area)
                menu.addItem(GRID_ROWS[row], plainPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
            }
            col = 1;
            for (GridCell cell : rw.cells()) {
                if (cell.type() == CellType.ROD) {
                    menu.addItem(GRID_ROWS[row] + col, rodDisplay(cell.rod()), ChestMenuUtils.getEmptyClickHandler());
                } else if (cell.type() == CellType.BAIT) {
                    BaitEntry e = cell.bait();
                    int slot = GRID_ROWS[row] + col;
                    PAGE_SLOTS.put(e, slot);
                    menu.addItem(slot, baitIcon(bag, e), ChestMenuUtils.getEmptyClickHandler());
                    menu.addMenuClickHandler(slot, (p, s, it, a) -> {
                        handleBaitClick(p, menu, bag, bagSlot, e, a);
                        return false;
                    });
                }
                col++;
            }
            rowIdx++;
        }
    }

    /** Rod display slot: rod material + glow + display name + description (tells the player which baits this rod uses) */
    private static ItemStack rodDisplay(RodLayout rod) {
        ItemStack icon = rod.rodIcon().clone();
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            // Glow: resolved by key at runtime (works with 1.20.4 DURABILITY and 1.21 UNBREAKING)
            Enchantment glow = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
            if (glow != null) {
                meta.addEnchant(glow, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.setDisplayName(getGradientNameVer2(rod.rodName()));
            meta.setLore(List.of(getGradientNameVer2("This rod uses these baits")));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private static ItemStack plainPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static ItemStack baitIcon(ItemStack bag, BaitEntry entry) {
        long amount = getAmount(bag, entry);
        int priority = getPriority(bag, entry);
        ItemStack icon = entry.item().clone();
        icon.setAmount(1);
        ItemMeta meta = icon.getItemMeta();
        List<String> lore = new ArrayList<>();
        if (meta != null && meta.hasLore()) lore.addAll(meta.getLore());
        lore.add("");
        lore.add(getGradientNameVer2("Stock: " + amount));
        lore.add(getGradientNameVer2("Priority: " + priority + " / " + seriesSize(entry.series())));
        lore.add(getGradientNameVer2("Empty-hand left-click takes out 1 · Left-click puts in · Shift takes out (≤3456) · Right-click sets priority"));
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private static void handleBaitClick(Player player, ChestMenu menu, ItemStack bag, int bagSlot, BaitEntry entry, me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction action) {
        if (action.isShiftClicked()) {
            withdraw(player, menu, bag, bagSlot, entry);
        } else if (action.isRightClicked()) {
            cyclePriority(player, menu, bag, bagSlot, entry);
        } else {
            depositOrTake(player, menu, bag, bagSlot, entry);
        }
    }

    /** Right-click cycles the priority, and makes sure it's not duplicated within the same series (a taken value is swapped with the clicked one) */
    private static void cyclePriority(Player player, ChestMenu menu, ItemStack bag, int bagSlot, BaitEntry entry) {
        int size = seriesSize(entry.series());
        int current = getPriority(bag, entry);
        int next = current % size + 1;
        for (BaitEntry other : BAITS) {
            if (other == entry || !other.series().equals(entry.series())) continue;
            if (getPriority(bag, other) == next) {
                setPriority(bag, other, current);
                // Fix: the swapped one's priority changed, so refresh its slot icon too
                refreshBaitSlot(menu, bag, other);
                break;
            }
        }
        setPriority(bag, entry, next);
        player.getInventory().setItem(bagSlot, bag);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        refreshBaitSlot(menu, bag, entry);
    }

    private static void depositOrTake(Player player, ChestMenu menu, ItemStack bag, int bagSlot, BaitEntry entry) {
        ItemStack cursor = player.getItemOnCursor();
        // Left-click with an empty hand: take 1 to the cursor
        if (cursor == null || cursor.getType().isAir()) {
            long have = getAmount(bag, entry);
            if (have <= 0) return;
            setAmount(bag, entry, have - 1);
            ItemStack one = entry.item().clone();
            one.setAmount(1);
            player.setItemOnCursor(one);
            player.getInventory().setItem(bagSlot, bag);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
            refreshBaitSlot(menu, bag, entry);
            return;
        }
        BaitEntry match = matchBait(cursor);
        if (match == null) {
            player.sendMessage(getGradientNameVer2("Hold the matching bait and click the slot"));
            return;
        }
        if (!match.id().equals(entry.id())) {
            player.sendMessage(getGradientNameVer2("This isn't the bait for this slot"));
            return;
        }
        long cur = getAmount(bag, entry);
        long add = cursor.getAmount();
        long nv = cur + add;
        if (nv < cur) nv = Long.MAX_VALUE; // overflow protection
        setAmount(bag, entry, nv);
        player.setItemOnCursor(null);
        player.getInventory().setItem(bagSlot, bag);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
        refreshBaitSlot(menu, bag, entry);
    }

    private static void withdraw(Player player, ChestMenu menu, ItemStack bag, int bagSlot, BaitEntry entry) {
        long remaining = getAmount(bag, entry);
        if (remaining <= 0) return;
        long toTake = Math.min(remaining, 3456L); // take out at most 3456
        long left = toTake;
        while (left > 0) {
            ItemStack stack = entry.item().clone();
            int give = (int) Math.min(left, 64);
            stack.setAmount(give);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
            // Fix (L1): addItem returns overflow when only part fits, so left must be reduced by the "amount really put in",
            // otherwise (tried amount - the left that wasn't reduced) the bag loses too little and the player can take out repeatedly to dupe items
            int overflowTotal = 0;
            for (ItemStack rest : leftovers.values()) {
                overflowTotal += rest.getAmount();
            }
            int actuallyAdded = give - overflowTotal;
            left -= actuallyAdded;
            if (!leftovers.isEmpty()) break; // Inventory is full (there's overflow), end the loop
        }
        setAmount(bag, entry, remaining - (toTake - left));
        player.getInventory().setItem(bagSlot, bag);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.0f);
        refreshBaitSlot(menu, bag, entry);
    }

    // ==================== Input slot ====================

    /** Real input slot: click to pick up / put down, Shift+click to move into the inventory, everything refreshes in place without flickering */
    private static void handleInput(Player player, ChestMenu menu, ItemStack bag, int bagSlot, me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction action) {
        ItemStack cursor = player.getItemOnCursor();
        ItemStack stored = getInputItem(bag);

        // Shift+click: move the item in the slot to the inventory
        if (action.isShiftClicked()) {
            if (stored != null) {
                Map<Integer, ItemStack> left = player.getInventory().addItem(stored);
                // Fix: the Map key returned by addItem isn't guaranteed to be 0, so use values to get the first leftover,
                // to avoid silently wiping out "the part that doesn't fit in the inventory" when left.get(0) is null
                ItemStack leftover = left.isEmpty() ? null : left.values().iterator().next();
                setInputItem(bag, leftover);
                player.getInventory().setItem(bagSlot, bag);
                refreshInputSlot(menu, bag);
            }
            return;
        }
        // Click with an empty hand: pick up the item in the slot to the cursor, and put it down somewhere yourself
        if (cursor == null || cursor.getType().isAir()) {
            if (stored != null) {
                player.setItemOnCursor(stored);
                setInputItem(bag, null);
                player.getInventory().setItem(bagSlot, bag);
                refreshInputSlot(menu, bag);
            }
            return;
        }
        // If the slot already has an item, do nothing and keep it as is
        if (stored != null) {
            return;
        }

        placeIntoInput(player, menu, bag, bagSlot, cursor, true);
    }

    /** Put into the input slot: bait is eaten and stored right away, other items stay saved in the slot */
    private static void placeIntoInput(Player player, ChestMenu menu, ItemStack bag, int bagSlot, ItemStack item, boolean fromCursor) {
        BaitEntry direct = matchBait(item);
        if (direct != null) {
            // Bait: not saved, it's eaten and stored in the matching slot right away
            long cur = getAmount(bag, direct);
            long add = item.getAmount();
            long nv = cur + add;
            if (nv < cur) nv = Long.MAX_VALUE;
            setAmount(bag, direct, nv);
            player.getInventory().setItem(bagSlot, bag);
            if (fromCursor) {
                player.setItemOnCursor(null);
            }
            refreshBaitSlot(menu, bag, direct);
            return;
        }

        // Other items: saved in the input slot, they won't be lost even if the menu is closed
        setInputItem(bag, item.clone());
        if (fromCursor) {
            player.setItemOnCursor(null);
        }
        player.getInventory().setItem(bagSlot, bag);
        processInputSource(player, menu, bag, bagSlot, item);
        refreshInputSlot(menu, bag);
    }

    /** Shift+left-click an inventory item to quickly put it in the input slot */
    private static void fastInputFromInventory(Player player, ChestMenu menu, ItemStack bag, int bagSlot, ItemStack item, int invSlot) {
        player.getInventory().setItem(invSlot, null);
        placeIntoInput(player, menu, bag, bagSlot, item, false);
    }

    /** Handle the source item saved in the input slot (Ether Chest / quantum storage / Void Touch) */
    private static void processInputSource(Player player, ChestMenu menu, ItemStack bag, int bagSlot, ItemStack source) {
        SlimefunItem sf = SlimefunItem.getByItem(source);

        // ① Ether Chest (CargoFragment)
        if (sf instanceof CargoFragment) {
            ItemMeta meta = source.getItemMeta();
            String json = meta == null ? null : meta.getPersistentDataContainer().get(KEY_FRAGMENT_ITEM, PersistentDataType.STRING);
            Integer fragAmount = meta == null ? null : meta.getPersistentDataContainer().get(KEY_FRAGMENT_AMOUNT, PersistentDataType.INTEGER);
            if (json != null && fragAmount != null && fragAmount > 0) {
                BaitEntry fragBait = matchBait(itemFromBase64(json));
                if (fragBait != null) {
                    long cur = getAmount(bag, fragBait);
                    long free = Long.MAX_VALUE - cur;
                    int take = (int) Math.min(fragAmount, free);
                    if (take <= 0) return;
                    setAmount(bag, fragBait, cur + take);
                    int newAmount = fragAmount - take;
                    if (newAmount <= 0) {
                        // Everything in the chest has been taken out, clear the input slot
                        setInputItem(bag, null);
                    } else {
                        // Update the chest's remaining amount and save it
                        meta.getPersistentDataContainer().set(KEY_FRAGMENT_AMOUNT, PersistentDataType.INTEGER, newAmount);
                        source.setItemMeta(meta);
                        setInputItem(bag, source);
                    }
                    player.getInventory().setItem(bagSlot, bag);
                    player.sendMessage(getGradientNameVer2("Took bait out of the Ether Chest"));
                    refreshBaitSlot(menu, bag, fragBait);
                    return;
                }
            }
            return;
        }

        // ② Quantum storage item
        if (NetworkStorage.isQuantumStorageItem(source)) {
            ItemMeta meta = source.getItemMeta();
            NetworkStorage.QuantumCache qc = NetworkStorage.getQuantumCache(meta);
            if (qc == null || qc.getItemStack() == null) return;
            BaitEntry qBait = matchBait(qc.getItemStack());
            if (qBait == null) return;
            long bagAmount = getAmount(bag, qBait);
            long take = Math.min(qc.getAmountLong(), Long.MAX_VALUE - bagAmount);
            if (take <= 0) return;
            setAmount(bag, qBait, bagAmount + take);
            qc.setAmount(qc.getAmountLong() - take);
            NamespacedKey storageKey = findQuantumKey(meta);
            if (storageKey != null) {
                NetworkStorage.setCustom(meta, storageKey, NetworkStorage.QUANTUM_STORAGE_TYPE, qc);
                NetworkStorage.refreshLore(meta, qc);
            }
            source.setItemMeta(meta);
            // Update the quantum storage item saved in the input slot (already reduced)
            setInputItem(bag, source);
            player.getInventory().setItem(bagSlot, bag);
            player.sendMessage(getGradientNameVer2("Took bait out of the quantum storage"));
            refreshBaitSlot(menu, bag, qBait);
            return;
        }

        // ③ Void Touch → bound quantum storage block
        if (sf instanceof VoidTouch) {
            handleVoidTouchInput(player, menu, bag, bagSlot, source);
        }
    }

    /** Bait storage slot map (built dynamically for the current page, after paging it only finds the baits shown on this page) */
    private static int baitSlotFor(BaitEntry entry) {
        Integer slot = PAGE_SLOTS.get(entry);
        return slot != null ? slot : -1;
    }

    private static void refreshBaitSlot(ChestMenu menu, ItemStack bag, BaitEntry entry) {
        int slot = baitSlotFor(entry);
        if (slot >= 0) {
            menu.replaceExistingItem(slot, baitIcon(bag, entry));
        }
    }

    private static void refreshInputSlot(ChestMenu menu, ItemStack bag) {
        ItemStack input = getInputItem(bag);
        menu.replaceExistingItem(INPUT_SLOT, input == null ? new ItemStack(Material.AIR) : input);
    }

    private static ItemStack getInputItem(ItemStack bag) {
        if (bag == null || !bag.hasItemMeta()) return null;
        String json = bag.getItemMeta().getPersistentDataContainer().get(KEY_INPUT_ITEM, PersistentDataType.STRING);
        if (json == null || json.isEmpty()) return null;
        return itemFromBase64(json);
    }

    private static void setInputItem(ItemStack bag, ItemStack item) {
        ItemMeta meta = bag.getItemMeta();
        if (item == null || item.getType().isAir()) {
            meta.getPersistentDataContainer().remove(KEY_INPUT_ITEM);
        } else {
            meta.getPersistentDataContainer().set(KEY_INPUT_ITEM, PersistentDataType.STRING, itemToBase64(item));
        }
        bag.setItemMeta(meta);
    }

    private static void handleVoidTouchInput(Player player, ChestMenu menu, ItemStack bag, int bagSlot, ItemStack voidTouch) {
        ItemMeta meta = voidTouch.getItemMeta();
        if (meta == null) return;
        var container = meta.getPersistentDataContainer();
        if (!container.has(KEY_X, PersistentDataType.INTEGER) || !container.has(KEY_Y, PersistentDataType.INTEGER)
                || !container.has(KEY_Z, PersistentDataType.INTEGER) || !container.has(KEY_WORLD, PersistentDataType.STRING)) {
            return;
        }
        org.bukkit.World world = Bukkit.getWorld(container.get(KEY_WORLD, PersistentDataType.STRING));
        if (world == null) {
            return;
        }
        Location target = new Location(world,
                container.get(KEY_X, PersistentDataType.INTEGER),
                container.get(KEY_Y, PersistentDataType.INTEGER),
                container.get(KEY_Z, PersistentDataType.INTEGER));
        SlimefunItem targetSf = StorageCacheUtils.getSfItem(target);
        if (targetSf == null || !NetworkStorage.isQuantumStorageBlock(targetSf)) {
            return;
        }

        try {
            Class<?> cls = Class.forName(QUANTUM_STORAGE_BLOCK_CLASS);
            Object cache = ((Map<?, ?>) cls.getMethod("getCaches").invoke(null)).get(target);
            if (cache == null) {
                return;
            }
            ItemStack stored = (ItemStack) cache.getClass().getMethod("getItemStack").invoke(cache);
            BaitEntry vBait = matchBait(stored);
            if (vBait == null) {
                return;
            }
            long amount = ((Number) cache.getClass().getMethod("getAmount").invoke(cache)).longValue();
            long bagAmount = getAmount(bag, vBait);
            long take = Math.min(amount, Long.MAX_VALUE - bagAmount);
            if (take <= 0) return;
            try {
                cache.getClass().getMethod("setAmount", long.class).invoke(cache, amount - take);
            } catch (NoSuchMethodException e) {
                cache.getClass().getMethod("setAmount", int.class).invoke(cache, (int) (amount - take));
            }
            cls.getMethod("syncBlock", Location.class, cache.getClass()).invoke(null, target, cache);

            setAmount(bag, vBait, getAmount(bag, vBait) + take);
            player.setItemOnCursor(null);
            player.getInventory().setItem(bagSlot, bag);
            player.sendMessage(getGradientNameVer2("Took bait out of the quantum storage"));
            refreshBaitSlot(menu, bag, vBait);
        } catch (Throwable t) {
            // Fix (L3): instead of silently swallowing the exception, print a warning log, to make quantum storage compatibility problems easier to track down
            MagicExpansion.getInstance().getLogger().warning("Bait bag failed to read quantum storage data: " + t.getMessage());
        }
    }

    private static BaitEntry matchBait(ItemStack item) {
        if (item == null) return null;
        SlimefunItem sf = SlimefunItem.getByItem(item);
        if (sf != null) {
            for (BaitEntry entry : BAITS) {
                SlimefunItem entrySf = SlimefunItem.getByItem(entry.item());
                if (entrySf != null && entrySf.getId().equals(sf.getId())) return entry;
            }
        }
        for (BaitEntry entry : BAITS) {
            if (SlimefunUtils.isItemSimilar(item, entry.item(), true)) return entry;
        }
        return null;
    }

    /** Works with three quantum storage keys: official / Chinese / new version */
    private static NamespacedKey findQuantumKey(ItemMeta meta) {
        if (meta == null) return null;
        var container = meta.getPersistentDataContainer();
        if (container.has(NetworkStorage.QUANTUM_STORAGE_INSTANCE, NetworkStorage.QUANTUM_STORAGE_TYPE)) {
            return NetworkStorage.QUANTUM_STORAGE_INSTANCE;
        }
        if (container.has(NetworkStorage.QUANTUM_STORAGE_INSTANCE2, NetworkStorage.QUANTUM_STORAGE_TYPE)) {
            return NetworkStorage.QUANTUM_STORAGE_INSTANCE2;
        }
        if (container.has(NetworkStorage.QUANTUM_STORAGE_INSTANCE3, NetworkStorage.QUANTUM_STORAGE_TYPE)) {
            return NetworkStorage.QUANTUM_STORAGE_INSTANCE3;
        }
        return null;
    }
}
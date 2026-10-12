package io.Yomicer.magicExpansion.utils;

import io.Yomicer.magicExpansion.core.MagicExpansionItems;
import io.Yomicer.magicExpansion.items.misc.WeightedItem;
import io.Yomicer.magicExpansion.items.tools.FishingRodWaterCloud;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.Yomicer.magicExpansion.MagicExpansion.getInstance;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;

/**
 * Water Cloud bait pool menu
 * The main page lists baits supported by the rod. Click a bait to view its drops and chances.
 */
public class WaterCloudPoolMenu {

    public static final NamespacedKey POOL_BUTTON_KEY = new NamespacedKey(getInstance(), "watercloud_pool_button");
    public static final NamespacedKey POOL_ROD_KEY = new NamespacedKey(getInstance(), "watercloud_rod");

    /** Items shown per drop page (pagination, 4 rows × 7 columns) */
    private static final int ITEMS_PER_PAGE = 28;

    private static final String[] BAIT_KEYS = {
            "CuiXia", "WeiChen", "RongHuo", "YueJin", "XingHe",                    // Green Bamboo Rod: 5 baits
            "JianJia", "LuXue", "WeiLu", "BaiLu", "LuYa",                          // Reed Blossom Rod: 5 baits
            "NingShuang", "LuoXu", "BingPo", "ChuJi", "ChuiLun",                   // Cold River Snow: 5 baits
            "FengSi", "YanYu", "LianBai", "XiaoFeng", "XieYing"                    // Drizzle · Side Wind: 5 baits
    };
    private static final String[] BAIT_NAMES = {
            "Dawn Glow", "Fine Dust", "Molten Fire", "Golden Leap", "Star Core",
            "Reeds", "Reed Snow", "Reed Dew", "White Dew", "Reed Sprout",
            "Frost", "Falling Fluff", "Ice Soul", "Clear Skies", "Fishing Line",
            "Wind Thread", "Misty Rain", "White Ripples", "Morning Breeze", "Slanted Shadow"
    };
    private static final String[] BAIT_COLORS = {
            "§c", "§7", "§4", "§6", "§d",
            "§b", "§f", "§3", "§e", "§a",
            "§f", "§b", "§3", "§e", "§8",
            "§7", "§d", "§f", "§b", "§9"
    };
    private static final ItemStack[] BAIT_ICONS = {
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_CUIXIA,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_WEICHEN,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_RONGHUO,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_YUEJIN,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XINGHE,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_REED_JIANJIA,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_REED_LUXUE,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_REED_WEILU,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_REED_BAILU,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_REED_LUYA,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_HANJIANG_NINGSHUANG,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_HANJIANG_LUOXU,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_HANJIANG_BINGPO,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_HANJIANG_CHUJI,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_HANJIANG_CHUILUN,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XIYU_FENGSI,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XIYU_YANYU,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XIYU_LIANBAI,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XIYU_XIAOFENG,
            MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XIYU_XIEYING
    };
    private static final ItemStack[] SPECIAL_ICONS = {
            MagicExpansionItems.REED_TASSEL,  // Dawn Glow special catch = Reed Tassel
            MagicExpansionItems.REED_TASSEL,  // Fine Dust special catch = Reed Tassel
            MagicExpansionItems.REED_TASSEL,  // Molten Fire special catch = Reed Tassel
            MagicExpansionItems.REED_TASSEL,  // Golden Leap special catch = Reed Tassel
            MagicExpansionItems.REED_TASSEL,  // Star Core special catch = Reed Tassel
            MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_BAILUYU, // Special catches for Reeds/Reed Snow/Reed Dew/White Dew/Reed Sprout (all Reed Blossom baits use White Reed Feather)
            MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_BAILUYU,
            MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_BAILUYU,
            MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_BAILUYU,
            MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_BAILUYU,
            MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_HANJIANG_XUEPOZHU, // Special catch for all Cold River Snow baits = Snow Soul Pearl
            MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_HANJIANG_XUEPOZHU,
            MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_HANJIANG_XUEPOZHU,
            MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_HANJIANG_XUEPOZHU,
            MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_HANJIANG_XUEPOZHU,
            MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_XIYU_YUPIZHEN, // Special catch for all Drizzle · Side Wind baits = Rain Needle
            MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_XIYU_YUPIZHEN,
            MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_XIYU_YUPIZHEN,
            MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_XIYU_YUPIZHEN,
            MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_XIYU_YUPIZHEN
    };

    private WaterCloudPoolMenu() {
    }

    /**
     * Create the 'View Bait Pool' button on the guide recipe page (with an NBT tag so the listener can identify it)
     */
    public static ItemStack createPoolButton(FishingRodWaterCloud rod) {
        CustomItemStack btn = new CustomItemStack(Material.BOOK,
                getGradientName("✦ View Bait Pool ✦"),
                getGradientName("Click to view all baits available for this rod"),
                getGradientName("and the drop chances for each bait"));
        ItemMeta meta = btn.getItemMeta();
        meta.getPersistentDataContainer().set(POOL_BUTTON_KEY, PersistentDataType.BOOLEAN, true);
        meta.getPersistentDataContainer().set(POOL_ROD_KEY, PersistentDataType.STRING, rod.getId());
        btn.setItemMeta(meta);
        return btn;
    }

    public static boolean isPoolButton(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(POOL_BUTTON_KEY, PersistentDataType.BOOLEAN);
    }

    public static String getButtonRodId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(POOL_ROD_KEY, PersistentDataType.STRING);
    }

    /**
     * Main page: choose a bait
     */
    public static void openBaitList(Player player, FishingRodWaterCloud rod) {
        ChestMenu menu = new ChestMenu(getGradientName("✦ Water Cloud · Bait Pool ✦"));
        setupBorders(menu, "§9✦ Water Cloud");
        menu.setEmptySlotsClickable(false);
        menu.setPlayerInventoryClickable(false);

        Map<String, List<WeightedItem>> table = rod.getLootTable();
        // Bait slots (4 rows × 7 columns, up to 28; currently at most 20 baits, so one page is enough)
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int slotIdx = 0;

        for (int i = 0; i < BAIT_KEYS.length && slotIdx < slots.length; i++) {
            // Only show the bait pool available to this rod
            if (!table.containsKey(BAIT_KEYS[i])) continue;
            final int fi = i;
            int baitSlot = slots[slotIdx];
            slotIdx++;
            List<WeightedItem> pool = table.getOrDefault(BAIT_KEYS[i], List.of());
            int total = pool.stream().mapToInt(WeightedItem::getWeight).sum();
            WeightedItem special = pool.stream().filter(w -> isSpecialItem(w.getItem(), fi)).findFirst().orElse(null);
            int specialWeight = special == null ? 0 : special.getWeight();
            String specialName = special == null ? "§8None yet" : "§6" + ItemStackHelper.getDisplayName(special.getItem());

            ItemStack icon = BAIT_ICONS[i].clone();
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(BAIT_COLORS[i] + "✦ " + BAIT_NAMES[i]);
            List<String> lore = new ArrayList<>();
            lore.add("§7Catchable items: §f" + pool.size() + " items");
            lore.add("§6 · Special item chance: " + formatPercent(total, specialWeight));
            lore.add("");
            lore.add("§7" + specialName + " §7is a special catch");
            lore.add("");
            lore.add("§eClick to view drop chances");
            meta.setLore(lore);
            icon.setItemMeta(meta);

            menu.addItem(baitSlot, icon, (p, slot, item, action) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                openPool(p, rod, BAIT_KEYS[fi], 0);
                return false;
            });
        }

        // Close button
        menu.addItem(49, new CustomItemStack(Material.BARRIER, "§cClose Menu", "§7Return to game"));
        menu.addMenuClickHandler(49, (p, slot, item, action) -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            p.closeInventory();
            return false;
        });

        menu.open(player);
    }

    /**
     * Drop details page (paged)
     */
    public static void openPool(Player player, FishingRodWaterCloud rod, String key, int page) {
        int index = indexOfKey(key);
        if (index < 0) return;

        List<WeightedItem> pool = rod.getLootTable().getOrDefault(key, List.of());
        int total = pool.stream().mapToInt(WeightedItem::getWeight).sum();
        int pages = Math.max(1, (pool.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        final int currentPage = page;

        ChestMenu menu = new ChestMenu(getGradientName(BAIT_NAMES[index] + " · Drops"));
        setupBorders(menu, BAIT_COLORS[index] + "✦ " + BAIT_NAMES[index]);
        menu.setEmptySlotsClickable(false);
        menu.setPlayerInventoryClickable(false);

        // Top-center info: use the clicked bait's material and name
        ItemStack info = BAIT_ICONS[index].clone();
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(BAIT_COLORS[index] + "✦ " + BAIT_NAMES[index]);
        infoMeta.setLore(List.of("§6Page " + (currentPage + 1) + "/" + pages + ""));
        info.setItemMeta(infoMeta);
        menu.addItem(4, info);
        menu.addMenuClickHandler(4, (p, slot, item, action) -> false);

        // Drop entries (4 rows × 7 columns; do not fill the remaining slots with glass panes)
        int[] grid = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int start = currentPage * ITEMS_PER_PAGE;
        for (int i = 0; i < grid.length; i++) {
            int poolIndex = start + i;
            if (poolIndex < pool.size()) {
                WeightedItem w = pool.get(poolIndex);
                ItemStack display = buildPoolItem(w, total, index);
                menu.addItem(grid[i], display, (p, slot, item, action) -> false);
            }
        }

        // Bottom row: previous / next / back (slot 45 stays decorative)
        if (currentPage > 0) {
            menu.addItem(48, new CustomItemStack(Material.ARROW, "§ePrevious Page", "§7Page " + currentPage + "/" + pages + ""));
            menu.addMenuClickHandler(48, (p, slot, item, action) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                openPool(p, rod, key, currentPage - 1);
                return false;
            });
        }
        if (currentPage + 1 < pages) {
            menu.addItem(50, new CustomItemStack(Material.ARROW, "§eNext Page", "§7Page " + (currentPage + 2) + "/" + pages + ""));
            menu.addMenuClickHandler(50, (p, slot, item, action) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                openPool(p, rod, key, currentPage + 1);
                return false;
            });
        }

        menu.addItem(49, new CustomItemStack(Material.OAK_DOOR, "§bBack to Bait List"));
        menu.addMenuClickHandler(49, (p, slot, item, action) -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            openBaitList(p, rod);
            return false;
        });

        menu.open(player);
    }

    private static ItemStack buildPoolItem(WeightedItem w, int total, int baitIndex) {
        String name = ItemStackHelper.getDisplayName(w.getItem());
        String displayName;
        if (isSpecialItem(w.getItem(), baitIndex)) {
            displayName = "§6🌟 " + name;
        } else {
            displayName = "§7" + name;
        }
        ItemStack display = w.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(List.of("§eDrop Chance: §f" + formatPercent(total, w.getWeight())));
        display.setItemMeta(meta);
        return display;
    }

    private static boolean isSpecialItem(ItemStack item, int baitIndex) {
        return item != null && SlimefunUtils.isItemSimilar(item, SPECIAL_ICONS[baitIndex], true);
    }

    private static String formatPercent(int total, int part) {
        if (total <= 0) return "0.00%";
        return String.format("%.2f%%", part * 100.0 / total);
    }

    private static int indexOfKey(String key) {
        for (int i = 0; i < BAIT_KEYS.length; i++) {
            if (BAIT_KEYS[i].equals(key)) return i;
        }
        return -1;
    }

    private static void setupBorders(ChestMenu menu, String sideLabel) {
        menu.addItem(0, new CustomItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§9✦"));
        menu.addMenuClickHandler(0, (p, slot, item, action) -> false);
        menu.addItem(8, new CustomItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§9✦"));
        menu.addMenuClickHandler(8, (p, slot, item, action) -> false);
        menu.addItem(45, new CustomItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§9✦"));
        menu.addMenuClickHandler(45, (p, slot, item, action) -> false);
        menu.addItem(53, new CustomItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§9✦"));
        menu.addMenuClickHandler(53, (p, slot, item, action) -> false);

        for (int i = 1; i < 8; i++) {
            menu.addItem(i, new CustomItemStack(Material.CYAN_STAINED_GLASS_PANE, sideLabel));
            menu.addMenuClickHandler(i, (p, slot, item, action) -> false);
        }
        for (int i = 46; i < 53; i++) {
            menu.addItem(i, new CustomItemStack(Material.CYAN_STAINED_GLASS_PANE, sideLabel));
            menu.addMenuClickHandler(i, (p, slot, item, action) -> false);
        }
        int[] left = {9, 18, 27, 36};
        for (int slot : left) {
            menu.addItem(slot, new CustomItemStack(Material.CYAN_STAINED_GLASS_PANE, "§9🎣"));
            menu.addMenuClickHandler(slot, (p, s, item, action) -> false);
        }
        int[] right = {17, 26, 35, 44};
        for (int slot : right) {
            menu.addItem(slot, new CustomItemStack(Material.CYAN_STAINED_GLASS_PANE, "§9🐟"));
            menu.addMenuClickHandler(slot, (p, s, item, action) -> false);
        }
    }
}

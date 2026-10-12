package io.Yomicer.magicExpansion.utils.shop;

import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.Yomicer.magicExpansion.MagicExpansion;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShopGUI implements Listener {

    // Fix: Use ConcurrentHashMap for static session maps because async chat also accesses them; allow cleanup to clear them
    private static final Map<UUID, String> pendingShopNameCreation = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> playerMainPage = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> adminMainPage = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Integer>> shopTradesPage = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Integer>> adminTradesPage = new ConcurrentHashMap<>();

    private static final Map<UUID, Integer> probPage = new ConcurrentHashMap<>();

    private static final Map<UUID, ShopEditData> currentEditingData = new ConcurrentHashMap<>();
    // Fix: Use a thread-safe Set
    private static final Set<UUID> safeClose = ConcurrentHashMap.newKeySet();

    private static final int[] BLACK_MARKET_SLOTS = {
            20, 21, 22, 23, 24,
            29, 30, 31, 32, 33
    };

    private static class ShopEditData {
        String shopName;
        ItemStack result;
        List<ItemStack> costs = new ArrayList<>();
        int globalLimit = 0;
        int personalLimit = 0;
        String editing = "none";
        boolean isNew = true;
    }
    private static final Map<UUID, ShopEditData> pendingEditData = new ConcurrentHashMap<>();

    /**
     * Fix: Clear this player's data from all session maps when they leave to prevent memory leaks
     */
    public static void cleanup(UUID uuid) {
        pendingShopNameCreation.remove(uuid);
        playerMainPage.remove(uuid);
        adminMainPage.remove(uuid);
        shopTradesPage.remove(uuid);
        adminTradesPage.remove(uuid);
        probPage.remove(uuid);
        currentEditingData.remove(uuid);
        safeClose.remove(uuid);
        pendingEditData.remove(uuid);
    }

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static ItemStack createBorder() {
        return createInfoItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
    }

    private static ItemStack createInfoItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static void fillBorder(Inventory inv) {
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8) {
                if (inv.getItem(i) == null) inv.setItem(i, createBorder());
            }
        }
    }

    // ================= Player Menu =================
    public static void openPlayerMainMenu(Player player) {
        openPlayerMainMenu(player, playerMainPage.getOrDefault(player.getUniqueId(), 0));
    }

    public static void openPlayerMainMenu(Player player, int page) {
        playerMainPage.put(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(new ShopHolder(), 54, ChatColor.DARK_GREEN + "Magic Market");
        fillBorder(inv);

        inv.setItem(4, createInfoItem(Material.BOOK, ChatColor.AQUA + "Magic Market", Arrays.asList(ChatColor.GRAY + "Choose a shop to open")));

        List<ShopManager.Shop> shops = ShopManager.getShops();
        int maxPage = Math.max(0, (shops.size() - 1) / CONTENT_SLOTS.length);
        page = Math.min(page, maxPage);

        int startIndex = page * CONTENT_SLOTS.length;
        for (int i = 0; i < CONTENT_SLOTS.length && (startIndex + i) < shops.size(); i++) {
            ShopManager.Shop shop = shops.get(startIndex + i);
            ItemStack icon = new ItemStack(Material.CHEST);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + shop.name);
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Click to view trades", ChatColor.GRAY + "Total trades: " + shop.trades.size() + " trades"));
            icon.setItemMeta(meta);
            inv.setItem(CONTENT_SLOTS[i], icon);
        }

        if (page > 0) inv.setItem(45, createInfoItem(Material.ARROW, ChatColor.YELLOW + "Previous Page", null));
        if (page < maxPage) inv.setItem(53, createInfoItem(Material.ARROW, ChatColor.YELLOW + "Next Page", null));

        // Added: Daily black market button (slot 49)
        inv.setItem(49, createInfoItem(Material.WITHER_SKELETON_SKULL, ChatColor.DARK_PURPLE + "Daily Mystery Market", Arrays.asList(
                ChatColor.GRAY + "10 mystery items refresh every 4 hours",
                ChatColor.GRAY + "You might find free items",
                ChatColor.GRAY + "Each player can buy each item once",
                ChatColor.GRAY + "Item chances change with each refresh"
        )));

        player.openInventory(inv);
    }

    public static void openShopTrades(Player player, String shopName) {
        Map<String, Integer> pages = shopTradesPage.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        openShopTrades(player, shopName, pages.getOrDefault(shopName, 0));
    }

    public static void openShopTrades(Player player, String shopName, int page) {
        shopTradesPage.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(shopName, page);
        ShopManager.Shop shop = ShopManager.getShop(shopName);
        if (shop == null) return;

        Inventory inv = Bukkit.createInventory(new ShopHolder(), 54, ChatColor.DARK_GREEN + shopName);
        fillBorder(inv);

        inv.setItem(4, createInfoItem(Material.ENDER_CHEST, ChatColor.AQUA + shopName, Arrays.asList(ChatColor.GRAY + "Click an item to trade")));

        int maxPage = Math.max(0, (shop.trades.size() - 1) / CONTENT_SLOTS.length);
        page = Math.min(page, maxPage);

        int startIndex = page * CONTENT_SLOTS.length;
        UUID playerId = player.getUniqueId();

        for (int i = 0; i < CONTENT_SLOTS.length && (startIndex + i) < shop.trades.size(); i++) {
            ShopManager.Trade trade = shop.trades.get(startIndex + i);
            if (trade.result == null) continue;
            ItemStack display = trade.result.clone();
            ItemMeta meta = display.getItemMeta();

            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.AQUA + "Items needed:");
            if (trade.costItems != null && !trade.costItems.isEmpty()) {
                for (ItemStack cost : trade.costItems) {
                    if (cost != null && cost.getType() != Material.AIR) {
                        String name = ItemStackHelper.getDisplayName(cost);
                        lore.add(ChatColor.RED + " - " + name + " x" + cost.getAmount());
                    }
                }
            } else {
                lore.add(ChatColor.RED + " (Free)");
            }

            boolean canBuy = ShopManager.canPurchase(playerId, trade);
            if (trade.globalLimit > 0 || trade.personalLimit > 0) {
                lore.add("");
                if (trade.globalLimit > 0) {
                    lore.add(ChatColor.YELLOW + "Server stock left: " + (canBuy ? ChatColor.AQUA : ChatColor.RED) + (trade.globalLimit - trade.globalUsed));
                }
                if (trade.personalLimit > 0) {
                    int used = trade.personalUsed.getOrDefault(playerId, 0);
                    lore.add(ChatColor.YELLOW + "Your limit left: " + (canBuy ? ChatColor.AQUA : ChatColor.RED) + (trade.personalLimit - used));
                }
                if (!canBuy) {
                    lore.add(ChatColor.DARK_RED + "You've reached the purchase limit!");
                }
            }

            meta.setLore(lore);
            display.setItemMeta(meta);
            inv.setItem(CONTENT_SLOTS[i], display);
        }

        if (page > 0) inv.setItem(45, createInfoItem(Material.ARROW, ChatColor.YELLOW + "Previous Page", null));
        inv.setItem(49, createInfoItem(Material.ARROW, ChatColor.GRAY + "Back", null));
        if (page < maxPage) inv.setItem(53, createInfoItem(Material.ARROW, ChatColor.YELLOW + "Next Page", null));

        player.openInventory(inv);
    }

    // ================= Admin Menu =================
    public static void openAdminMainMenu(Player player) {
        openAdminMainMenu(player, adminMainPage.getOrDefault(player.getUniqueId(), 0));
    }

    public static void openAdminMainMenu(Player player, int page) {
        adminMainPage.put(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(new ShopHolder(), 54, ChatColor.DARK_RED + "Magic Market Admin");
        fillBorder(inv);

        inv.setItem(4, createInfoItem(Material.WRITABLE_BOOK, ChatColor.AQUA + "Admin Menu", Arrays.asList(ChatColor.GRAY + "Manage the Magic Market for everyone")));

        List<ShopManager.Shop> shops = ShopManager.getShops();
        int maxPage = Math.max(0, (shops.size() - 1) / CONTENT_SLOTS.length);
        page = Math.min(page, maxPage);

        int startIndex = page * CONTENT_SLOTS.length;
        for (int i = 0; i < CONTENT_SLOTS.length && (startIndex + i) < shops.size(); i++) {
            ShopManager.Shop shop = shops.get(startIndex + i);
            ItemStack icon = new ItemStack(Material.CHEST);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + shop.name);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Left-click: Manage trades",
                    ChatColor.RED + "Right-click: Delete shop"
            ));
            icon.setItemMeta(meta);
            inv.setItem(CONTENT_SLOTS[i], icon);
        }

        if (page > 0) inv.setItem(45, createInfoItem(Material.ARROW, ChatColor.YELLOW + "Previous Page", null));
        inv.setItem(49, createInfoItem(Material.WRITABLE_BOOK, ChatColor.GREEN + "Create Shop", Arrays.asList(ChatColor.GRAY + "Click, then type a name in chat")));
        if (page < maxPage) inv.setItem(53, createInfoItem(Material.ARROW, ChatColor.YELLOW + "Next Page", null));

        inv.setItem(51, createInfoItem(Material.SUNFLOWER, ChatColor.AQUA + "Reload Shop Data", Arrays.asList(
                ChatColor.GRAY + "Click to reload data from the folder",
                ChatColor.YELLOW + "Note: Unsaved changes will be lost"
        )));

        // Added: Force refresh black market button
        inv.setItem(52, createInfoItem(Material.WITHER_SKELETON_SKULL, ChatColor.DARK_PURPLE + "Force Refresh Market", Arrays.asList(
                ChatColor.GRAY + "Refresh today's market items now",
                ChatColor.YELLOW + "Time left: " + BlackMarketManager.getTimeRemaining()
        )));

        player.openInventory(inv);
    }

    public static void openAdminTradesMenu(Player player, String shopName) {
        Map<String, Integer> pages = adminTradesPage.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        openAdminTradesMenu(player, shopName, pages.getOrDefault(shopName, 0));
    }

    public static void openAdminTradesMenu(Player player, String shopName, int page) {
        adminTradesPage.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(shopName, page);
        ShopManager.Shop shop = ShopManager.getShop(shopName);
        if (shop == null) return;

        Inventory inv = Bukkit.createInventory(new ShopHolder(), 54, ChatColor.DARK_RED + "Manage: " + shopName);
        fillBorder(inv);

        inv.setItem(4, createInfoItem(Material.ENDER_CHEST, ChatColor.AQUA + "Manage: " + shopName, Arrays.asList(ChatColor.GRAY + "Middle-click to reset purchase counts")));

        int maxPage = Math.max(0, (shop.trades.size() - 1) / CONTENT_SLOTS.length);
        page = Math.min(page, maxPage);

        int startIndex = page * CONTENT_SLOTS.length;
        for (int i = 0; i < CONTENT_SLOTS.length && (startIndex + i) < shop.trades.size(); i++) {
            ShopManager.Trade trade = shop.trades.get(startIndex + i);
            if (trade.result == null) continue;
            ItemStack display = trade.result.clone();
            ItemMeta meta = display.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Left-click: Edit trade");
            lore.add(ChatColor.RED + "Right-click: Delete trade");
            lore.add(ChatColor.BLUE + "Middle-click: Reset server and personal purchase counts");
            meta.setLore(lore);
            display.setItemMeta(meta);
            inv.setItem(CONTENT_SLOTS[i], display);
        }

        if (page > 0) inv.setItem(45, createInfoItem(Material.ARROW, ChatColor.YELLOW + "Previous Page", null));
        inv.setItem(49, createInfoItem(Material.LIME_DYE, ChatColor.GREEN + "Add Trade", null));
        if (page < maxPage) inv.setItem(53, createInfoItem(Material.ARROW, ChatColor.YELLOW + "Next Page", null));

        player.openInventory(inv);
    }

    public static void openTradeEditor(Player player, String shopName, ShopEditData editData) {
        currentEditingData.put(player.getUniqueId(), editData);

        Inventory inv = Bukkit.createInventory(new ShopHolder(), 54, ChatColor.DARK_RED + "Edit: " + shopName);

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, createBorder());
        }

        inv.setItem(4, createInfoItem(Material.ANVIL, ChatColor.AQUA + "Trade Editor", Arrays.asList(ChatColor.GRAY + "Set up items and purchase limits")));

        inv.setItem(11, createInfoItem(Material.PINK_STAINED_GLASS_PANE, ChatColor.LIGHT_PURPLE + "Place the item you get on the right →→", null));
        inv.setItem(12, createInfoItem(Material.PINK_STAINED_GLASS_PANE, ChatColor.LIGHT_PURPLE + "Place the item you get on the right →→", null));
        inv.setItem(14, createInfoItem(Material.PINK_STAINED_GLASS_PANE, ChatColor.LIGHT_PURPLE + "←← Place the item you give on the left", null));
        inv.setItem(15, createInfoItem(Material.PINK_STAINED_GLASS_PANE, ChatColor.LIGHT_PURPLE + "←← Place the item you give on the left", null));

        if (editData.result != null) {
            inv.setItem(13, editData.result.clone());
        } else {
            inv.setItem(13, null);
        }

        inv.setItem(27, createInfoItem(Material.BLUE_STAINED_GLASS_PANE, ChatColor.BLUE + "Place cost items", null));
        inv.setItem(35, createInfoItem(Material.BLUE_STAINED_GLASS_PANE, ChatColor.BLUE + "Place cost items", null));

        for (int i = 28; i <= 34; i++) {
            int costIndex = i - 28;
            if (costIndex < editData.costs.size() && editData.costs.get(costIndex) != null) {
                inv.setItem(i, editData.costs.get(costIndex).clone());
            } else {
                inv.setItem(i, null);
            }
        }

        inv.setItem(40, createInfoItem(Material.COMPARATOR, ChatColor.GOLD + "Server limit: " + (editData.globalLimit == 0 ? "No limit" : editData.globalLimit), Arrays.asList(ChatColor.GRAY + "Left-click to set")));
        inv.setItem(41, createInfoItem(Material.PAPER, ChatColor.GOLD + "Personal limit: " + (editData.personalLimit == 0 ? "No limit" : editData.personalLimit), Arrays.asList(ChatColor.GRAY + "Left-click to set")));

        inv.setItem(45, createInfoItem(Material.RED_CONCRETE, ChatColor.RED + "Delete Trade", Collections.singletonList(ChatColor.GRAY + "Only works when editing")));
        inv.setItem(49, createInfoItem(Material.ARROW, ChatColor.GRAY + "Back (Don't Save)", null));
        inv.setItem(53, createInfoItem(Material.LIME_CONCRETE, ChatColor.GREEN + "Save Trade", null));

        player.openInventory(inv);
    }

    // ================= Black Market Menu =================
    public static void openBlackMarket(Player player) {
        BlackMarketManager.checkAndRefresh();
        Inventory inv = Bukkit.createInventory(new ShopHolder(), 54, ChatColor.DARK_PURPLE + "Daily Mystery Market");

        // Fill all slots with black glass panes
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, createBorder());
        }

        // Top info
        inv.setItem(4, createInfoItem(Material.WITHER_SKELETON_SKULL, ChatColor.LIGHT_PURPLE + "Today's Market", Arrays.asList(
                ChatColor.GRAY + "Refreshes every 4 hours",
                ChatColor.GRAY + "Each player can buy each item once",
                ChatColor.YELLOW + "Next refresh in: " + BlackMarketManager.getTimeRemaining()
        )));

        // Clear the 10 center slots for items
        for (int slot : BLACK_MARKET_SLOTS) {
            inv.setItem(slot, null);
        }

        List<BlackMarketManager.BlackMarketTrade> trades = BlackMarketManager.getTodayTrades(player);
        UUID playerId = player.getUniqueId();

        for (int i = 0; i < trades.size(); i++) {
            int slot = BLACK_MARKET_SLOTS[i];
            BlackMarketManager.BlackMarketTrade trade = trades.get(i);
            if (trade.result == null || trade.result.getType() == Material.AIR) continue;

            if (BlackMarketManager.hasPurchased(playerId, i)) {
                inv.setItem(slot, createInfoItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.DARK_RED + "Purchased", null));
                continue;
            }

            if (!BlackMarketManager.isRevealed(playerId, i)) {
                inv.setItem(slot, createInfoItem(Material.PURPLE_SHULKER_BOX, ChatColor.DARK_PURPLE + "Mystery Market Box", Arrays.asList(
                        ChatColor.GRAY + "Click to reveal your surprise!"
                )));
                continue;
            }

            ItemStack display = trade.result.clone();
            ItemMeta meta = display.getItemMeta();
            // Fix: If meta is null, try getting it from the item factory
            if (meta == null) {
                meta = Bukkit.getItemFactory().getItemMeta(display.getType());
            }

            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");

                if (trade.isHard) lore.add(ChatColor.GOLD + "Rare item!");
                else lore.add(ChatColor.GRAY + "Common item");

                if (trade.isFree) {
                    lore.add(ChatColor.GOLD + "★ FREE ITEM ★");
                } else {
                    lore.add(ChatColor.DARK_RED + "---- Items needed:");
                    if (trade.costs != null) {
                        for (ItemStack cost : trade.costs) {
                            if (cost != null && cost.getType() != Material.AIR) {
                                String name = ItemStackHelper.getDisplayName(cost);
                                lore.add(ChatColor.RED + " - " + name + " x" + cost.getAmount());
                            }
                        }
                    }
                }
                lore.add("");
                lore.add(ChatColor.GREEN + "Click again to trade");

                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(slot, display);
        }

        inv.setItem(45, createInfoItem(Material.KNOWLEDGE_BOOK, ChatColor.AQUA + "Item Chances", Arrays.asList(ChatColor.GRAY + "Click to see the chance for each item")));
        // Back button goes in slot 49
        inv.setItem(49, createInfoItem(Material.ARROW, ChatColor.GRAY + "Back", null));
        player.openInventory(inv);
    }


    public static void openBlackMarketProbabilities(Player player) {
        openBlackMarketProbabilities(player, probPage.getOrDefault(player.getUniqueId(), 0));
    }

    public static void openBlackMarketProbabilities(Player player, int page) {
        List<ItemStack> simplePool = BlackMarketManager.getSimplePool();
        Map<ItemStack, Integer> hardPool = BlackMarketManager.getHardPool();

        double hardChance = BlackMarketManager.HARD_CHANCE;
        double simpleChance = BlackMarketManager.SIMPLE_CHANCE;

        Map<String, Double> nameToProb = new HashMap<>();
        Map<String, ItemStack> nameToItem = new HashMap<>();

        if (simplePool != null && !simplePool.isEmpty()) {
            double singleProb = simpleChance / simplePool.size();
            for (ItemStack item : simplePool) {
                if (item == null || item.getType() == Material.AIR) continue;
                String name = ItemStackHelper.getDisplayName(item);
                nameToProb.merge(name, singleProb, Double::sum);
                nameToItem.putIfAbsent(name, item.clone());
            }
        }

        double hardTotalWeight = 0;
        if (hardPool != null) {
            for (Integer weight : hardPool.values()) {
                hardTotalWeight += (weight == null ? 1 : weight);
            }
        }

        if (hardPool != null && hardTotalWeight > 0) {
            for (Map.Entry<ItemStack, Integer> entry : hardPool.entrySet()) {
                ItemStack item = entry.getKey();
                if (item == null || item.getType() == Material.AIR) continue;

                double weight = entry.getValue() == null ? 1 : entry.getValue();
                double singleProb = hardChance * (weight / hardTotalWeight);

                String name = ItemStackHelper.getDisplayName(item);
                nameToProb.merge(name, singleProb, Double::sum);
                nameToItem.putIfAbsent(name, item.clone());
            }
        }

        // Convert the Map to a List for paging
        List<Map.Entry<String, Double>> probList = new ArrayList<>(nameToProb.entrySet());

        Inventory inv = Bukkit.createInventory(new ShopHolder(), 54, ChatColor.DARK_AQUA + "Black Market Item Chances");
        fillBorder(inv);

        int maxPage = Math.max(0, (probList.size() - 1) / CONTENT_SLOTS.length);
        if (page < 0) page = 0;
        page = Math.min(page, maxPage);
        probPage.put(player.getUniqueId(), page);

        int startIndex = page * CONTENT_SLOTS.length;
        for (int i = 0; i < CONTENT_SLOTS.length && (startIndex + i) < probList.size(); i++) {
            Map.Entry<String, Double> entry = probList.get(startIndex + i);
            ItemStack displayItem = nameToItem.get(entry.getKey());
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                double prob = entry.getValue() * 100;
                lore.add(ChatColor.GOLD + "Spawn chance: " + String.format("%.5f", prob) + "%");
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            inv.setItem(CONTENT_SLOTS[i], displayItem);
        }

        // Page buttons
        if (page > 0) inv.setItem(45, createInfoItem(Material.ARROW, ChatColor.YELLOW + "Previous Page", null));
        if (page < maxPage) inv.setItem(53, createInfoItem(Material.ARROW, ChatColor.YELLOW + "Next Page", null));

        inv.setItem(49, createInfoItem(Material.ARROW, ChatColor.GRAY + "Back", null));
        player.openInventory(inv);
    }





    private void handleBlackMarketClick(Player player, int slot) {
        // Get the slot index used by the black market
        int index = getBlackMarketIndexBySlot(slot);
        if (index == -1 || index >= BlackMarketManager.getTodayTrades(player).size()) return;

        UUID playerId = player.getUniqueId();

        if (BlackMarketManager.hasPurchased(playerId, index)) {
            player.sendMessage(ChatColor.RED + "You've already bought this item today!");
            return;
        }

        if (!BlackMarketManager.isRevealed(playerId, index)) {
            BlackMarketManager.reveal(playerId, index);
            BlackMarketManager.BlackMarketTrade trade = BlackMarketManager.getTodayTrades(player).get(index);

            org.bukkit.Sound sound;
            if (trade.isFree) {
                sound = org.bukkit.Sound.BLOCK_BELL_USE;
            } else if (trade.isHard) {
                sound = org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL;
            } else {
                sound = org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING;
            }
            player.playSound(player.getLocation(), sound, 1f, 1f);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "The box is now open!");

            openBlackMarket(player);
            return;
        }

        handleBlackMarketPurchase(player, index);
    }

    private void handleBlackMarketPurchase(Player player, int index) {
        UUID playerId = player.getUniqueId();
        BlackMarketManager.BlackMarketTrade trade = BlackMarketManager.getTodayTrades(player).get(index);

        if (!trade.isFree && trade.costs != null) {
            for (ItemStack cost : trade.costs) {
                if (cost == null || cost.getType() == Material.AIR) continue;
                if (!player.getInventory().containsAtLeast(cost, cost.getAmount())) {
                    String costName = ItemStackHelper.getDisplayName(cost);
                    player.sendMessage(ChatColor.RED + "Missing item: " + costName + " x" + cost.getAmount());
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
            }

            for (ItemStack cost : trade.costs) {
                if (cost == null || cost.getType() == Material.AIR) continue;
                ItemStack costToRemove = cost.clone();
                HashMap<Integer, ItemStack> notRemoved = player.getInventory().removeItem(costToRemove);
                if (!notRemoved.isEmpty()) {
                    for (ItemStack leftOver : notRemoved.values()) {
                        removeItems(player.getInventory(), leftOver);
                    }
                }
            }
        }

        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(trade.result.clone());
        if (!overflow.isEmpty()) {
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.sendMessage(ChatColor.YELLOW + "Your inventory is full. Some items were dropped on the ground!");
        }

        BlackMarketManager.recordPurchase(playerId, index);

        player.sendMessage(ChatColor.GREEN + "Trade completed!");
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        openBlackMarket(player);
    }

    // ================= Event Handling =================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof ShopHolder)) return;

        if (e.getClickedInventory() == null) {
            e.setCancelled(true);
            return;
        }

        String title = e.getView().getTitle();
        Player player = (Player) e.getWhoClicked();

        if (title.startsWith(ChatColor.DARK_RED + "Edit: ")) {
            if (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT) {
                e.setCancelled(true);
                return;
            }

            if (e.getClickedInventory().getHolder() instanceof ShopHolder) {
                int slot = e.getRawSlot();
                if (slot == 13 || (slot >= 28 && slot <= 34)) {
                    e.setCancelled(false);
                    return;
                } else {
                    e.setCancelled(true);
                }
            } else {
                e.setCancelled(false);
                return;
            }
        } else {
            e.setCancelled(true);
        }

        int slot = e.getRawSlot();
        ItemStack item = e.getCurrentItem();

        if (item == null || item.getType() == Material.AIR) return;

        // Player shop list
        if (title.equals(ChatColor.DARK_GREEN + "Magic Market")) {
            if (slot == 49) {
                openBlackMarket(player);
                return;
            }
            if (slot == 45) openPlayerMainMenu(player, playerMainPage.getOrDefault(player.getUniqueId(), 0) - 1);
            else if (slot == 53) openPlayerMainMenu(player, playerMainPage.getOrDefault(player.getUniqueId(), 0) + 1);
            else if (item.getType() == Material.CHEST) {
                String shopName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                openShopTrades(player, shopName);
            }
        }
        // Player trade list
        else if (title.startsWith(ChatColor.DARK_GREEN + "")) {
            if (slot == 49) {
                openPlayerMainMenu(player);
                return;
            }
            if (slot == 45 || slot == 53) {
                String shopName = ChatColor.stripColor(title.replace(ChatColor.DARK_GREEN + "", ""));
                int current = shopTradesPage.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault(shopName, 0);
                openShopTrades(player, shopName, slot == 45 ? current - 1 : current + 1);
                return;
            }
            String shopName = ChatColor.stripColor(title.replace(ChatColor.DARK_GREEN + "", ""));
            handlePurchase(player, shopName, slot, item);
        }
        // Black Market Menu
        else if (title.equals(ChatColor.DARK_PURPLE + "Daily Mystery Market")) {
            if (slot == 49) {
                openPlayerMainMenu(player);
            } else if (slot == 45) {
                openBlackMarketProbabilities(player);
            }else {
                handleBlackMarketClick(player, slot);
            }
        }
        // Black market chances screen
        else if (title.equals(ChatColor.DARK_AQUA + "Black Market Item Chances")) {
            if (slot == 49) {
                // Return to the black market menu
                openBlackMarket(player);
            } else if (slot == 45) {
                // Previous Page
                openBlackMarketProbabilities(player, probPage.getOrDefault(player.getUniqueId(), 0) - 1);
            } else if (slot == 53) {
                // Next Page
                openBlackMarketProbabilities(player, probPage.getOrDefault(player.getUniqueId(), 0) + 1);
            }
        }
        // Admin shop list
        else if (title.equals(ChatColor.DARK_RED + "Magic Market Admin")) {
            if (slot == 51) {
                player.closeInventory();
                ShopManager.reload();
                player.sendMessage(ChatColor.GREEN + "Magic Market data reloaded!");
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
                Bukkit.getScheduler().runTaskLater(MagicExpansion.getInstance(), () -> openAdminMainMenu(player, adminMainPage.getOrDefault(player.getUniqueId(), 0)), 1L);
                return;
            }
            if (slot == 52) {
                BlackMarketManager.forceRefresh();
                player.sendMessage(ChatColor.DARK_PURPLE + "Black market refreshed!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                openAdminMainMenu(player, adminMainPage.getOrDefault(player.getUniqueId(), 0));
                return;
            }
            if (slot == 45) openAdminMainMenu(player, adminMainPage.getOrDefault(player.getUniqueId(), 0) - 1);
            else if (slot == 53) openAdminMainMenu(player, adminMainPage.getOrDefault(player.getUniqueId(), 0) + 1);
            else if (slot == 49) {
                player.closeInventory();
                player.sendMessage(ChatColor.GREEN + "Type the new shop name in chat, or type 'cancel' to cancel.");
                pendingShopNameCreation.put(player.getUniqueId(), "");
            } else if (item.getType() == Material.CHEST) {
                String shopName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                if (e.isRightClick()) {
                    ShopManager.deleteShop(shopName);
                    openAdminMainMenu(player, adminMainPage.getOrDefault(player.getUniqueId(), 0));
                } else if (e.isLeftClick()) {
                    openAdminTradesMenu(player, shopName);
                }
            }
        }
        // Admin trade list
        else if (title.startsWith(ChatColor.DARK_RED + "Manage: ")) {
            String shopName = title.replace(ChatColor.DARK_RED + "Manage: ", "");
            if (slot == 49) {
                ShopEditData data = new ShopEditData();
                data.shopName = shopName;
                data.isNew = true;
                openTradeEditor(player, shopName, data);
                return;
            }
            if (slot == 45 || slot == 53) {
                int current = adminTradesPage.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault(shopName, 0);
                openAdminTradesMenu(player, shopName, slot == 45 ? current - 1 : current + 1);
                return;
            }
            int index = getTradeIndexBySlot(slot);
            ShopManager.Shop shop = ShopManager.getShop(shopName);
            if (shop == null || index == -1 || index >= shop.trades.size()) return;

            if (e.isRightClick()) {
                shop.trades.remove(index);
                ShopManager.saveShop(shop);
                openAdminTradesMenu(player, shopName, adminTradesPage.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault(shopName, 0));
            } else if (e.isLeftClick()) {
                ShopManager.Trade t = shop.trades.get(index);
                ShopEditData data = new ShopEditData();
                data.shopName = shopName;
                data.result = t.result;
                data.costs = t.costItems;
                data.globalLimit = t.globalLimit;
                data.personalLimit = t.personalLimit;
                data.isNew = false;
                openTradeEditor(player, shopName, data);
            } else if (e.getClick() == ClickType.MIDDLE) {
                ShopManager.resetUsage(shop, shop.trades.get(index));
                player.sendMessage(ChatColor.GREEN + "Purchase counts for this trade have been reset!");
                openAdminTradesMenu(player, shopName, adminTradesPage.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault(shopName, 0));
            }
        }
        // Admin editor
        else if (title.startsWith(ChatColor.DARK_RED + "Edit: ")) {
            String shopName = title.replace(ChatColor.DARK_RED + "Edit: ", "");
            ShopEditData currentData = readEditDataFromInventory(e.getInventory(), shopName);
            // Fix: currentEditingData may be missing (reload/restart/session loss), so check for null to avoid an NPE
            ShopEditData editingData = currentEditingData.get(player.getUniqueId());
            if (editingData == null) {
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "This edit session has expired. Please reopen the trade editor!");
                return;
            }
            currentData.isNew = editingData.isNew;

            if (slot == 45) {
                ShopManager.Shop shop = ShopManager.getShop(shopName);
                safeClose.add(player.getUniqueId());
                if (currentData.result != null) {
                    shop.trades.removeIf(t -> t.result != null && SlimefunUtils.isItemSimilar(t.result, currentData.result, true));
                    ShopManager.saveShop(shop);
                    player.sendMessage(ChatColor.RED + "Trade deleted!");
                }
                openAdminTradesMenu(player, shopName);
                return;
            }
            if (slot == 49) {
                openAdminTradesMenu(player, shopName);
                return;
            }
            if (slot == 40) {
                safeClose.add(player.getUniqueId());
                player.closeInventory();
                currentData.editing = "global";
                pendingEditData.put(player.getUniqueId(), currentData);
                player.sendMessage(ChatColor.GREEN + "Type the server purchase limit in chat (0 means no limit), or type 'cancel' to cancel.");
                return;
            }
            if (slot == 41) {
                safeClose.add(player.getUniqueId());
                player.closeInventory();
                currentData.editing = "personal";
                pendingEditData.put(player.getUniqueId(), currentData);
                player.sendMessage(ChatColor.GREEN + "Type your personal purchase limit in chat (0 means no limit), or type 'cancel' to cancel.");
                return;
            }
            if (slot == 53) {
                ShopManager.Shop shop = ShopManager.getShop(shopName);
                safeClose.add(player.getUniqueId());

                ShopManager.Trade newTrade = new ShopManager.Trade();
                newTrade.result = currentData.result;
                newTrade.costItems = currentData.costs;
                newTrade.globalLimit = currentData.globalLimit;
                newTrade.personalLimit = currentData.personalLimit;

                if (newTrade.result == null || newTrade.result.getType() == Material.AIR) {
                    player.sendMessage(ChatColor.RED + "Place the item you want to give in slot 13 first!");
                    safeClose.remove(player.getUniqueId());
                    openTradeEditor(player, shopName, currentData);
                    return;
                }

                int existIndex = -1;
                for (int i = 0; i < shop.trades.size(); i++) {
                    ShopManager.Trade t = shop.trades.get(i);
                    if (t.result != null && SlimefunUtils.isItemSimilar(t.result, newTrade.result, true)) {
                        existIndex = i;
                        break;
                    }
                }

                if (existIndex != -1) {
                    shop.trades.set(existIndex, newTrade);
                } else {
                    shop.trades.add(newTrade);
                }
                ShopManager.saveShop(shop);
                player.sendMessage(ChatColor.GREEN + "Trade saved!");
                openAdminTradesMenu(player, shopName);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getInventory().getHolder() instanceof ShopHolder)) return;
        String title = e.getView().getTitle();
        if (title.startsWith(ChatColor.DARK_RED + "Edit: ")) {
            for (int slot : e.getRawSlots()) {
                if (slot != 13 && (slot < 28 || slot > 34)) {
                    e.setCancelled(true);
                    return;
                }
            }
            e.setCancelled(false);
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof ShopHolder)) return;
        String title = e.getView().getTitle();
        Player player = (Player) e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (title.startsWith(ChatColor.DARK_RED + "Edit: ")) {
            ShopEditData data = currentEditingData.get(uuid);
            currentEditingData.remove(uuid);

            if (safeClose.contains(uuid)) {
                safeClose.remove(uuid);
                return;
            }

            if (data != null && data.isNew) {
                boolean hasItem = false;
                List<Integer> slotsToCheck = new ArrayList<>();
                slotsToCheck.add(13);
                for(int i = 28; i <= 34; i++) slotsToCheck.add(i);

                for (int slot : slotsToCheck) {
                    ItemStack item = e.getInventory().getItem(slot);
                    if (item != null && item.getType() != Material.AIR && !item.getType().name().endsWith("STAINED_GLASS_PANE")) {
                        hasItem = true;
                        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                        for (ItemStack drop : overflow.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                        }
                        e.getInventory().setItem(slot, null);
                    }
                }
                if (hasItem) {
                    player.sendMessage(ChatColor.YELLOW + "Unsaved items were found and returned to your inventory!");
                }
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        if (pendingShopNameCreation.containsKey(uuid)) {
            e.setCancelled(true);
            // Fix: Only read chat input on the async thread, then switch to the main thread before changing maps or shop data
            String msg = e.getMessage().trim();
            Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> {
                // The pending marker may be cleared before switching threads (leave/cancel), so check it again
                if (!pendingShopNameCreation.containsKey(uuid)) return;

                if (msg.equalsIgnoreCase("cancel")) {
                    player.sendMessage(ChatColor.YELLOW + "Shop creation canceled.");
                    pendingShopNameCreation.remove(uuid);
                } else {
                    boolean exists = false;
                    for (ShopManager.Shop shop : ShopManager.getShops()) {
                        if (shop.name.equals(msg)) {
                            exists = true;
                            break;
                        }
                    }

                    if (exists) {
                        player.sendMessage(ChatColor.RED + "A shop with this name already exists: " + msg + " . Enter another name or type 'cancel' to cancel.");
                    } else {
                        ShopManager.createShop(msg);
                        player.sendMessage(ChatColor.GREEN + "Shop " + msg + " created!");
                        pendingShopNameCreation.remove(uuid);
                        openAdminMainMenu(player);
                    }
                }
            });
            return;
        }

        if (pendingEditData.containsKey(uuid)) {
            e.setCancelled(true);
            // Fix: Only read chat input on the async thread, then switch to the main thread before changing maps or data
            String msg = e.getMessage().trim();
            Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> {
                // The pending marker may be cleared before switching threads, so check it again
                ShopEditData data = pendingEditData.get(uuid);
                if (data == null) return;

                if (msg.equalsIgnoreCase("cancel")) {
                    player.sendMessage(ChatColor.YELLOW + "Limit setup canceled.");
                } else {
                    try {
                        int amount = Math.max(0, Integer.parseInt(msg));
                        if (data.editing.equals("global")) {
                            data.globalLimit = amount;
                            player.sendMessage(ChatColor.GREEN + "Server purchase limit set to: " + (amount == 0 ? "No limit" : amount));
                        } else if (data.editing.equals("personal")) {
                            data.personalLimit = amount;
                            player.sendMessage(ChatColor.GREEN + "Personal purchase limit set to: " + (amount == 0 ? "No limit" : amount));
                        }
                    } catch (NumberFormatException ex) {
                        player.sendMessage(ChatColor.RED + "Invalid input. Please enter a number!");
                    }
                }

                openTradeEditor(player, data.shopName, data);
                pendingEditData.remove(uuid);
            });
            return;
        }
    }

    // ================= Helper Logic =================
    private void handlePurchase(Player player, String shopName, int slot, ItemStack clickedItem) {
        int index = getTradeIndexBySlot(slot);
        ShopManager.Shop shop = ShopManager.getShop(shopName);
        if (shop == null || index == -1 || index >= shop.trades.size()) return;

        ShopManager.Trade trade = shop.trades.get(index);
        if (trade.result == null) return;

        UUID playerId = player.getUniqueId();
        if (!ShopManager.canPurchase(playerId, trade)) {
            player.sendMessage(ChatColor.RED + "You've reached the purchase limit!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        for (ItemStack cost : trade.costItems) {
            if (cost == null || cost.getType() == Material.AIR) continue;
            if (!player.getInventory().containsAtLeast(cost, cost.getAmount())) {
                String costName = ItemStackHelper.getDisplayName(cost);
                player.sendMessage(ChatColor.RED + "Missing item: " + costName + " x" + cost.getAmount());
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
        }

        for (ItemStack cost : trade.costItems) {
            if (cost == null || cost.getType() == Material.AIR) continue;
            ItemStack costToRemove = cost.clone();
            HashMap<Integer, ItemStack> notRemoved = player.getInventory().removeItem(costToRemove);
            if (!notRemoved.isEmpty()) {
                for (ItemStack leftOver : notRemoved.values()) {
                    removeItems(player.getInventory(), leftOver);
                }
            }
        }

        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(trade.result.clone());
        if (!overflow.isEmpty()) {
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.sendMessage(ChatColor.YELLOW + "Your inventory is full. Some items were dropped on the ground!");
        }

        ShopManager.recordPurchase(playerId, shop, trade);

        player.sendMessage(ChatColor.GREEN + "Trade completed!");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);

        openShopTrades(player, shopName, shopTradesPage.getOrDefault(player.getUniqueId(), new HashMap<>()).getOrDefault(shopName, 0));
    }

    private void removeItems(org.bukkit.inventory.Inventory inv, ItemStack item) {
        int amountToRemove = item.getAmount();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is != null && is.isSimilar(item)) {
                if (is.getAmount() > amountToRemove) {
                    is.setAmount(is.getAmount() - amountToRemove);
                    inv.setItem(i, is);
                    return;
                } else {
                    amountToRemove -= is.getAmount();
                    inv.setItem(i, null);
                    if (amountToRemove <= 0) return;
                }
            }
        }
    }

    private int getTradeIndexBySlot(int slot) {
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    private int getBlackMarketIndexBySlot(int slot) {
        for (int i = 0; i < BLACK_MARKET_SLOTS.length; i++) {
            if (BLACK_MARKET_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    private ShopEditData readEditDataFromInventory(Inventory inv, String shopName) {
        ShopEditData data = new ShopEditData();
        data.shopName = shopName;

        ItemStack result = inv.getItem(13);
        if (result != null && result.getType() != Material.AIR && !result.getType().name().endsWith("STAINED_GLASS_PANE")) {
            data.result = result.clone();
        }

        for (int i = 28; i <= 34; i++) {
            ItemStack cost = inv.getItem(i);
            if (cost != null && cost.getType() != Material.AIR && !cost.getType().name().endsWith("STAINED_GLASS_PANE")) {
                data.costs.add(cost.clone());
            }
        }

        ItemStack gItem = inv.getItem(40);
        if (gItem != null && gItem.hasItemMeta()) {
            String name = gItem.getItemMeta().getDisplayName();
            String numStr = name.replace(ChatColor.GOLD + "Server limit: ", "").replaceAll("[^0-9]", "");
            try {
                if (!name.contains("No limit")) data.globalLimit = Integer.parseInt(numStr);
            } catch (Exception ignored) {}
        }

        ItemStack pItem = inv.getItem(41);
        if (pItem != null && pItem.hasItemMeta()) {
            String name = pItem.getItemMeta().getDisplayName();
            String numStr = name.replace(ChatColor.GOLD + "Personal limit: ", "").replaceAll("[^0-9]", "");
            try {
                if (!name.contains("No limit")) data.personalLimit = Integer.parseInt(numStr);
            } catch (Exception ignored) {}
        }
        return data;
    }

    public static class ShopHolder implements InventoryHolder {
        @NotNull
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}

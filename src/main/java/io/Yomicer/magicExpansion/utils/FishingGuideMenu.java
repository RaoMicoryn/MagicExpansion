package io.Yomicer.magicExpansion.utils;

import io.Yomicer.magicExpansion.core.MagicExpansionItems;
import io.Yomicer.magicExpansion.items.misc.WeightedItem;
import io.Yomicer.magicExpansion.items.misc.fish.fishInterface.BaseFish;
import io.Yomicer.magicExpansion.items.misc.fish.fishInterface.FishManager;
import io.Yomicer.magicExpansion.utils.CustomHeadUtils.CustomHead;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientNameVer2;
import static io.Yomicer.magicExpansion.utils.CreateItem.createItem;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;

public class FishingGuideMenu {

    // Main menu title (gradient)
    private static final String MAIN_TITLE = getGradientName("✨Fishing Encyclopedia✨");

    // Category data
    private static final String[] CATEGORIES = {
            "common_fish", "uncommon_fish", "rare_fish",
            "epic_fish", "legendary_fish", "mythical_fish", "junk",
            "magic_sugar", "bread"
    };
    private static final String[] DISPLAY_NAMES = {
            "§f✦ Common Fish", "§a✦ Uncommon Fish", "§9✦ Rare Fish",
            "§5✦ Epic Fish", "§6✦ Legendary Fish", "§e✦ Mythical Fish", "§7✦ Junk",
            "§d✦ Bait: Magic Candy", "§b✦ Bait: Bread"
    };
    private static final Material[] ICONS = {
            Material.COD, Material.SALMON, Material.PUFFERFISH,
            Material.TROPICAL_FISH, Material.NAUTILUS_SHELL, Material.NETHER_STAR, Material.FISHING_ROD,
            Material.SUGAR, Material.BREAD
    };
    private static final String[] LORES = {
            "§eThe most common fish, easy to catch",
            "§eLess common than ordinary fish, but not rare",
            "§eFound in the deep sea; patience is required",
            "§eA legendary sea creature, extremely rare",
            "§eA miracle appears! Only found in special weather",
            "§eA fish that exists only in myths!",
            "§eA pile of junk… but maybe there’s a surprise?",
            "§e—— Shows possible catches only; catches from highly targeted bait are not listed yet ——",
            "§e—— Shows possible catches only; catches from highly targeted bait are not listed yet ——"
    };

    // Decorative glass
    private static final Material BORDER_GLASS = Material.BLUE_STAINED_GLASS_PANE;
    private static final Material CORNER_GLASS = Material.LIGHT_BLUE_STAINED_GLASS_PANE;

    // Back button
    private static final ItemStack BACK_BUTTON = createItemWithLore(Material.ARROW, "§a← Back");

    /**
     * 创建带有lore的Items
     */
    private static ItemStack createItemWithLore(Material material, String displayName, String... lore) {
        ItemStack item = createItem(material, displayName);
        if (lore != null && lore.length > 0) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setLore(Arrays.asList(lore));
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    /**
     * 创建分类Items
     */
    private static ItemStack createCategoryItem(Material material, String name, String lore) {
        List<String> loreList = Arrays.asList("", lore, "", "§a▶ Click to open");
        return createItemWithLore(material, name, loreList.toArray(new String[0]));
    }

    /**
     * 创建Decorative glass
     */
    private static ItemStack createDecorItem(Material material, String name) {
        return createItemWithLore(material, name, "§7Fishing Encyclopedia");
    }

    /**
     * Open the main menu
     */
    public static void openMainMenu(Player player) {
        ChestMenu menu = new ChestMenu(MAIN_TITLE);

        // 设置边框装饰
        setupMenuBorders(menu);

        // Add category buttons (starting at the first empty slot)
        int[] categorySlots = getAvailableSlots();
        for (int i = 0; i < CATEGORIES.length && i < categorySlots.length; i++) {
            ItemStack item = createCategoryItem(ICONS[i], DISPLAY_NAMES[i], LORES[i]);
            menu.addItem(categorySlots[i], item);

            final String category = CATEGORIES[i];
            menu.addMenuClickHandler(categorySlots[i], (p, slot, itemStack, click) -> {
                openCategoryPage(p, category);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                return false;
            });
        }

        // Add a close button (49位置）
        menu.addItem(49, createItemWithLore(Material.BARRIER, "§cClose the menu"));
        menu.addMenuClickHandler(49, (p, slot, item, click) -> {
            p.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return false;
        });

        int randomSlot = 21 + new Random().nextInt(24);
        // 50%概率添加Items
        if (new Random().nextBoolean()) {
            int choice = new Random().nextInt(3);

            switch (choice) {
                case 0:
                    menu.addItem(randomSlot,
                            new CustomItemStack(Material.BREAD,
                                    getGradientName("Bait: Bread"),
                                    getGradientName("Click to pick up one bread")),
                            (player1, slot, item, action) -> {
                                ItemStack drop = new ItemStack(Material.BREAD);
                                player1.getWorld().dropItemNaturally(player1.getLocation(), drop);
                                menu.addItem(slot,
                                        new ItemStack(Material.AIR), (player2, slot2, item2, action2) -> false
                                );
                                return false;
                            });
                    break;
                case 1:
                    menu.addItem(randomSlot,
                            new CustomItemStack(Material.SUGAR,
                                    getGradientName("Bait: Magic Candy"),
                                    getGradientName("Click to pick up one Magic Candy")),
                            (player1, slot, item, action) -> {
                                SlimefunItem sfItem = SlimefunItem.getByItem(SlimefunItems.MAGIC_SUGAR);
                                ItemStack drop = sfItem.getItem().clone();
                                player1.getWorld().dropItemNaturally(player1.getLocation(), drop);
                                menu.addItem(slot,
                                        new ItemStack(Material.AIR), (player2, slot2, item2, action2) -> false
                                );
                                return false;
                            });
                    break;
                case 2:
                    menu.addItem(randomSlot,
                            new CustomItemStack(Material.BONE_MEAL,
                                    getGradientName("Bait: Basic Magic Bait"),
                                    getGradientName("Click to pick up one Basic Magic Bait")),
                            (player1, slot, item, action) -> {
                                SlimefunItem sfItem = SlimefunItem.getByItem(MagicExpansionItems.FISH_LURE_BASIC);
                                ItemStack drop = sfItem.getItem().clone();
                                player1.getWorld().dropItemNaturally(player1.getLocation(), drop);
                                menu.addItem(slot,
                                        new ItemStack(Material.AIR), (player2, slot2, item2, action2) -> false
                                );
                                return false;
                            });
                    break;
            }
        }

        // 设置不可Click空槽和玩家背包
        menu.setEmptySlotsClickable(false);
        menu.setPlayerInventoryClickable(false);

        // Open the menu
        menu.open(player);
    }

    /**
     * Set menu border decorations
     */
    private static void setupMenuBorders(ChestMenu menu) {
        menu.addItem(0, createDecorItem(CORNER_GLASS, "§b✨"));
        menu.addMenuClickHandler(0, (p, slot, item, click) -> false);
        menu.addItem(8, createDecorItem(CORNER_GLASS, "§b✨"));
        menu.addMenuClickHandler(8, (p, slot, item, click) -> false);
        menu.addItem(45, createDecorItem(CORNER_GLASS, "§b✨"));
        menu.addMenuClickHandler(45, (p, slot, item, click) -> false);
        menu.addItem(53, createDecorItem(CORNER_GLASS, "§b✨"));
        menu.addMenuClickHandler(53, (p, slot, item, click) -> false);

        for (int i = 1; i < 8; i++) {
            menu.addItem(i, createDecorItem(BORDER_GLASS, "§9🌊 Ocean Encyclopedia"));
            menu.addMenuClickHandler(i, (p, slot, item, click) -> false);
        }

        for (int i = 46; i < 53; i++) {
            menu.addItem(i, createDecorItem(BORDER_GLASS, "§bDiving and exploring..."));
            menu.addMenuClickHandler(i, (p, slot, item, click) -> false);
        }

        // Left border (9,18,27,36)
        int[] leftBorder = {9, 18, 27, 36};
        for (int slot : leftBorder) {
            menu.addItem(slot, createDecorItem(BORDER_GLASS, "§9🎣"));
            menu.addMenuClickHandler(slot, (p, slot1, item, click) -> false);
        }

        // Right border (17,26,35,44)
        int[] rightBorder = {17, 26, 35, 44};
        for (int slot : rightBorder) {
            menu.addItem(slot, createDecorItem(BORDER_GLASS, "§9🐟"));
            menu.addMenuClickHandler(slot, (p, slot1, item, click) -> false);
        }
    }

    /**
     * Get available slots (empty slots excluding borders)
     */
    private static int[] getAvailableSlots() {
        // All occupied border slots
        Set<Integer> occupiedSlots = new HashSet<>();

        // Four corners
        occupiedSlots.add(0);
        occupiedSlots.add(8);
        occupiedSlots.add(45);
        occupiedSlots.add(53);

        // Top and bottom borders
        for (int i = 1; i < 8; i++) occupiedSlots.add(i);
        for (int i = 46; i < 53; i++) occupiedSlots.add(i);

        // Left and right borders
        int[] leftRightBorder = {9, 17, 18, 26, 27, 35, 36, 44};
        for (int slot : leftRightBorder) occupiedSlots.add(slot);

        // Collect all available slots
        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < 54; i++) {
            if (!occupiedSlots.contains(i)) {
                availableSlots.add(i);
            }
        }

        return availableSlots.stream().mapToInt(i -> i).toArray();
    }

    /**
     * Open the category details page (with pagination)
     */
    public static void openCategoryPage(Player player, String category) {
        openCategoryPage(player, category, 0); // Open by default at page 0page
    }

    private static void openCategoryPage(Player player, String category, int page) {
        String title = getCategoryTitle(category);
        ChestMenu menu = new ChestMenu(getGradientName("✦ " + title));

        // 设置边框装饰
        setupCategoryBorders(menu, title);

        // Get all items in this category
        List<ItemStack> allItems = getItemsByCategory(category);

        // 分page设置 - Use the actual number of available slots
        int[] availableSlots = getAvailableSlots();
        int itemsPerPage = availableSlots.length; // Items per page equals the number of available slots
        int totalPages = (int) Math.ceil((double) allItems.size() / itemsPerPage);

        // Keep the page number within the valid range
        page = Math.max(0, Math.min(page, totalPages - 1));

        // Get items on the current page
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allItems.size());
        List<ItemStack> currentPageItems = allItems.subList(startIndex, endIndex);

        // Fill available slots with items from the current page
        for (int i = 0; i < currentPageItems.size() && i < availableSlots.length; i++) {
            ItemStack originalItem = currentPageItems.get(i);

            // 克隆Items并设置渐变名称
            ItemStack display = originalItem.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                String displayName = ItemStackHelper.getDisplayName(display);
                meta.setDisplayName("§r"+(displayName));

                List<String> newLore = getStrings(page, meta, totalPages);
                meta.setLore(newLore);

                display.setItemMeta(meta);
            }

            menu.addItem(availableSlots[i], display);
            menu.addMenuClickHandler(availableSlots[i], (p, s, i1, c) -> false);
        }

        // Add pagination buttons
        setupCategoryPagination(menu, category, page, totalPages);

        // Disable clicks and inventory interaction
        menu.setEmptySlotsClickable(false);
        menu.setPlayerInventoryClickable(false);

        // Open the menu
        menu.open(player);
    }

    private static @NotNull List<String> getStrings(int page, ItemMeta meta, int totalPages) {
        List<String> originalLore = meta.getLore();
        List<String> newLore = new ArrayList<>();
        // 如果原有Lore不为空，先添加原有Lore
        if (originalLore != null && !originalLore.isEmpty()) {
            newLore.addAll(originalLore);
        }
        newLore.add("");
        newLore.add("§7This compendium entry is for display only");
        newLore.add("§8—— Shows only use and acquisition requirements ——");
        if (totalPages > 1) {
            newLore.add("§6Page number: " + (page + 1) + "/" + totalPages); // Show page numbers only when there are multiple pages
        }
        return newLore;
    }

    /**
     * Set category-page pagination controls
     */
    private static void setupCategoryPagination(ChestMenu menu, String category, int currentPage, int totalPages) {
        // Place the back button at49号槽位
        menu.addItem(49, BACK_BUTTON);
        menu.addMenuClickHandler(49, (p, s, i, c) -> {
            openMainMenu(p);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return false;
        });

        // Hide page buttons when there is only one page and use barriers as placeholders
        if (totalPages <= 1) {
            // Place a barrier in the previous-page slot
            menu.addItem(48, createItemWithLore(Material.BARRIER, "§8"));
            menu.addMenuClickHandler(48, (p, s, i, c) -> false);

            // Place a barrier in the next-page slot
            menu.addItem(50, createItemWithLore(Material.BARRIER, "§8"));
            menu.addMenuClickHandler(50, (p, s, i, c) -> false);
            return;
        }

        // Previous-page button (放在48号槽位)
        if (currentPage > 0) {
            ItemStack prevButton = createItemWithLore(
                    Material.ARROW,
                    "§6← Previous Page",
                    "§7Click to view the previous page",
                    "§8Current: " + (currentPage + 1) + " / " + totalPages
            );
            menu.addItem(48, prevButton);
            menu.addMenuClickHandler(48, (p, s, i, c) -> {
                openCategoryPage(p, category, currentPage - 1);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                return false;
            });
        } else {
            // Show a barrier on the first page
            menu.addItem(48, createItemWithLore(Material.BARRIER, "§8"));
            menu.addMenuClickHandler(48, (p, s, i, c) -> false);
        }

        // Next-page button (放在50号槽位)
        if (currentPage < totalPages - 1) {
            ItemStack nextButton = createItemWithLore(
                    Material.ARROW,
                    "§6Next Page →",
                    "§7Click to view the next page",
                    "§8Current: " + (currentPage + 1) + " / " + totalPages
            );
            menu.addItem(50, nextButton);
            menu.addMenuClickHandler(50, (p, s, i, c) -> {
                openCategoryPage(p, category, currentPage + 1);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                return false;
            });
        } else {
            // Show a barrier on the last page
            menu.addItem(50, createItemWithLore(Material.BARRIER, "§8"));
            menu.addMenuClickHandler(50, (p, s, i, c) -> false);
        }
    }

    /**
     * Set category-page border decorations
     */
    private static void setupCategoryBorders(ChestMenu menu, String title) {
        // Four corners
        menu.addItem(0, createDecorItem(CORNER_GLASS, "§b✨"));
        menu.addMenuClickHandler(0, (p, slot, item, click) -> false);
        menu.addItem(8, createDecorItem(CORNER_GLASS, "§b✨"));
        menu.addMenuClickHandler(8, (p, slot, item, click) -> false);
        menu.addItem(45, createDecorItem(CORNER_GLASS, "§b✨"));
        menu.addMenuClickHandler(45, (p, slot, item, click) -> false);
        menu.addItem(53, createDecorItem(CORNER_GLASS, "§b✨"));
        menu.addMenuClickHandler(53, (p, slot, item, click) -> false);


        // Top border
        for (int i = 1; i < 8; i++) {
            menu.addItem(i, createDecorItem(BORDER_GLASS, "§9" + title));
            menu.addMenuClickHandler(i, (p, slot, item, click) -> false);
        }

        // Bottom border - 跳过48,49,50槽位（用于分page按钮）
        for (int i = 46; i < 53; i++) {
            if (i == 48 || i == 49 || i == 50) continue; // 跳过分page按钮位置
            menu.addItem(i, createDecorItem(BORDER_GLASS, "§bCategory: " + title));
            menu.addMenuClickHandler(i, (p, slot, item, click) -> false);
        }

        // Left border
        int[] leftBorder = {9, 18, 27, 36};
        for (int slot : leftBorder) {
            menu.addItem(slot, createDecorItem(BORDER_GLASS, "§9📖"));
            menu.addMenuClickHandler(slot, (p, slot1, item, click) -> false);
        }

        // Right border
        int[] rightBorder = {17, 26, 35, 44};
        for (int slot : rightBorder) {
            menu.addItem(slot, createDecorItem(BORDER_GLASS, "§9📚"));
            menu.addMenuClickHandler(slot, (p, slot1, item, click) -> false);
        }
    }

    /**
     * Get the category title
     */
    private static String getCategoryTitle(String category) {
        return switch (category) {
            case "common_fish" -> "Common Fish";
            case "uncommon_fish" -> "Uncommon Fish";
            case "rare_fish" -> "Rare Fish";
            case "epic_fish" -> "Epic Fish";
            case "legendary_fish" -> "Legendary Fish";
            case "mythical_fish" -> "Mythical Fish";
            case "junk" -> "Junk";
            case "magic_sugar" -> "Magic Candy Catch";
            case "bread" -> "Bread Catch";
            default -> "Unknown category";
        };
    }

    /**
     * Get category items
     */
    private static List<ItemStack> getItemsByCategory(String category) {
        List<ItemStack> items = new ArrayList<>();

        switch (category) {
            case "common_fish":
                // 添加Common Fish类
                items.add(new CustomItemStack(Material.COD,getGradientName("About fish preferences"),getGradientName("Preference only means this fish favors this bait"),getGradientName("Other bait can also catch this fish, with some chance"),getGradientName("Of course, you might not catch it at all")));
                items.add(FishManager.getFishItemWithLore(BaseFish.SanWenFish,"",getGradientName("Can be caught with any bait")));
                items.add(FishManager.getFishItemWithLore(BaseFish.XueFish,"",getGradientName("Can be caught with any bait")));
                break;
            case "uncommon_fish":
                items.add(new CustomItemStack(Material.COD,getGradientName("About fish preferences"),getGradientName("Preference only means this fish favors this bait"),getGradientName("Other bait can also catch this fish, with some chance"),getGradientName("Of course, you might not catch it at all")));
                items.add(FishManager.getFishItemWithLore(BaseFish.HeTun,"",getGradientName("Can be caught with any bait")));
                break;
            case "rare_fish":
                items.add(new CustomItemStack(Material.COD,getGradientName("About fish preferences"),getGradientName("Preference only means this fish favors this bait"),getGradientName("Other bait can also catch this fish, with some chance"),getGradientName("Of course, you might not catch it at all")));
                items.add(FishManager.getFishItemWithLore(BaseFish.ReDaiFish,"",getGradientName("Can be caught with any bait")));
                items.add(FishManager.getFishItemWithLore(BaseFish.CopperDustFish,"",getGradientName("Bite preference: Magic Bait - Mixed Ore Dust")));
                items.add(FishManager.getFishItemWithLore(BaseFish.GoldDustFish,"",getGradientName("Bite preference: Magic Bait - Mixed Ore Dust")));
                items.add(FishManager.getFishItemWithLore(BaseFish.IronDustFish,"",getGradientName("Bite preference: Magic Bait - Mixed Ore Dust")));
                items.add(FishManager.getFishItemWithLore(BaseFish.TinDustFish,"",getGradientName("Bite preference: Magic Bait - Mixed Ore Dust")));
                items.add(FishManager.getFishItemWithLore(BaseFish.SilverDustFish,"",getGradientName("Bite preference: Magic Bait - Mixed Ore Dust")));
                items.add(FishManager.getFishItemWithLore(BaseFish.AluminumDustFish,"",getGradientName("Bite preference: Magic Bait - Mixed Ore Dust")));
                items.add(FishManager.getFishItemWithLore(BaseFish.LeadDustFish,"",getGradientName("Bite preference: Magic Bait - Mixed Ore Dust")));
                items.add(FishManager.getFishItemWithLore(BaseFish.ZincDustFish,"",getGradientName("Bite preference: Magic Bait - Mixed Ore Dust")));
                items.add(FishManager.getFishItemWithLore(BaseFish.MagnesiumDustFish,"",getGradientName("Bite preference: Magic Bait - Mixed Ore Dust")));
                items.add(FishManager.getFishItemWithLore(BaseFish.CoalFish,"",getGradientName("Bite preference: Magic Bait - Mixed Minerals")));
                items.add(FishManager.getFishItemWithLore(BaseFish.EmeraldFish,"",getGradientName("Bite preference: Magic Bait - Mixed Minerals")));
                items.add(FishManager.getFishItemWithLore(BaseFish.LapisFish,"",getGradientName("Bite preference: Magic Bait - Mixed Minerals")));
                items.add(FishManager.getFishItemWithLore(BaseFish.DiamondFish,"",getGradientName("Bite preference: Magic Bait - Mixed Minerals")));
                items.add(FishManager.getFishItemWithLore(BaseFish.QuartzFish,"",getGradientName("Bite preference: Magic Bait - Mixed Minerals")));
                items.add(FishManager.getFishItemWithLore(BaseFish.AmethystFish,"",getGradientName("Bite preference: Magic Bait - Mixed Minerals")));
                items.add(FishManager.getFishItemWithLore(BaseFish.IronFish,"",getGradientName("Bite preference: Magic Bait - Mixed Minerals")));
                items.add(FishManager.getFishItemWithLore(BaseFish.GoldFish,"",getGradientName("Bite preference: Magic Bait - Mixed Minerals")));
                items.add(FishManager.getFishItemWithLore(BaseFish.CopperFish,"",getGradientName("Bite preference: Magic Bait - Mixed Minerals")));
                items.add(FishManager.getFishItemWithLore(BaseFish.RedstoneFish,"",getGradientName("Bite preference: Magic Bait - Mixed Minerals")));
                items.add(FishManager.getFishItemWithLore(BaseFish.NetheriteFish,"",getGradientName("Bite preference: Magic Bait - Mixed Minerals")));
                items.add(FishManager.getFishItemWithLore(BaseFish.GlowStoneDustFish,"",getGradientName("Bite preference: Magic Bait - Mixed Minerals")));
                items.add(FishManager.getFishItemWithLore(BaseFish.ShuLingYu,"",getGradientName("Bite preference: basic magic bait"),getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.SulfateFish,"",getGradientName("Bite preference: basic magic bait"),getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.SiliconFish,"",getGradientName("Bite preference: basic magic bait"),getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.UraniumFish,"",getGradientName("Bite preference: basic magic bait"),getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.OilRockFish,"",getGradientName("Bite preference: basic magic bait"),getGradientName("Favors: more powerful Magic Fishing Rods")));
                break;
            case "epic_fish":
                items.add(new CustomItemStack(Material.COD,getGradientName("About fish preferences"),getGradientName("Preference only means this fish favors this bait"),getGradientName("Other bait can also catch this fish, with some chance"),getGradientName("Of course, you might not catch it at all")));
                items.add(FishManager.getFishItemWithLore(BaseFish.FoamCrystalFish,"",getGradientName("Bite preference: basic magic bait"),getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.BlackDiamondFish,"",getGradientName("Bite preference: basic magic bait"),getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.EnchantedBottleFish,"",getGradientName("Bite preference: basic magic bait"),getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.MYSTIC_EEL,"",getGradientName("Bite preference: any bait"),getGradientName("Favors: more powerful Magic Fishing Rods")));
                //合金锭Fish
                items.add(FishManager.getFishItemWithLore(BaseFish.ReinforcedAlloyFish,"",getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"),getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.HardenedMetalFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.DamascusSoulFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.SteelSoulFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.BronzeAncientFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.HardlightAluFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.SilverCopperFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.BrassResonanceFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.AluminumBrassFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.AluminumBronzeFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.CorinthianBronzeFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.SolderFlowFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.NickelSpiritFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.CobaltFlameFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.SiliconIronFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.CarbonSoulFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.GildedIronFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.RedstoneAlloyFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.NeptuniumShadowFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                items.add(FishManager.getFishItemWithLore(BaseFish.PlutoniumCoreFish, "", getGradientName("Exclusive Bait：Magic Bait - Mixed Alloy Slurry"), getGradientName("Favors: more powerful Magic Fishing Rods")));
                break;
            case "legendary_fish":
                items.add(new CustomItemStack(Material.COD,getGradientName("About fish preferences"),getGradientName("Preference only means this fish favors this bait"),getGradientName("Other bait can also catch this fish, with some chance"),getGradientName("Of course, you might not catch it at all")));
                items.add(FishManager.getFishItemWithLore(BaseFish.LegendaryLuFish,"",getGradientName("Preference: Entangled Knot: End Thread - Paradox Hook")));
                items.add(FishManager.getFishItemWithLore(BaseFish.LegendaryEelFish,"",getGradientName("Exclusive bait: Magic Candy")));
                break;
            case "mythical_fish":
                items.add(new CustomItemStack(Material.COD,getGradientName("About fish preferences"),getGradientName("Preference only means this fish favors this bait"),getGradientName("Other bait can also catch this fish, with some chance"),getGradientName("Of course, you might not catch it at all")));
                break;
            case "junk":
                items.add(new CustomItemStack(Material.COD,getGradientName("About fish preferences"),getGradientName("Preference only means this fish favors this bait"),getGradientName("Other bait can also catch this fish, with some chance"),getGradientName("Of course, you might not catch it at all")));
                items.add(new CustomItemStack(new ItemStack(Material.PRISMARINE_SHARD,1),getGradientNameVer2("Bait: Memory Fragment"),
                        ("§fThis bait can catch any item"),("§fIt exists in the past, or perhaps the future"),("§fWhat you see now is not its true self"),
                        "",getGradientName("Exclusive Fishing Rod: Wooden Fishing Rod"),getGradientName("Exclusive Fishing Rod: Wind Whisperer's Rod"),getGradientName("Exclusive Fishing Rod: Entangled Knot: End Thread - Paradox Hook")));
                items.add(new CustomItemStack(new ItemStack(Material.COD),"§bLost Raw Cod",getGradientName("Whose cod is this?"),
                        "",getGradientName("Exclusive Bait: Bread")));
                items.add(new CustomItemStack(new ItemStack(Material.SALMON),"§bDizzy Raw Salmon",getGradientName("Ever heard of the Salmon Return Formation?"),
                        "",getGradientName("Exclusive Bait: Bread")));
                items.add(new CustomItemStack(new ItemStack(Material.TROPICAL_FISH),"§bTropical Fish with 1.4",getGradientName("How did a tropical fish end up in a river?"),
                        "",getGradientName("Exclusive Bait: Bread")));
                items.add(new CustomItemStack(new ItemStack(Material.PUFFERFISH),"§bGreen Pufferfish",getGradientName("You shouldn’t eat this carelessly~"),
                        "",getGradientName("Exclusive Bait: Bread")));
                items.add(new CustomItemStack(new ItemStack(Material.POTION),"§bMysterious Potion",getGradientName("Maybe it’s drinkable?"),
                        "",getGradientName("Exclusive Bait: Bread")));
                items.add(new CustomItemStack(new ItemStack(Material.HONEY_BOTTLE,15),"§eHoney Bottle",getGradientName("This is 15 vanilla Honey Bottles"),
                        "",getGradientName("Exclusive Bait: Bread")));

                items.add(new CustomItemStack(Material.GLOW_BERRIES, "§aFirefly Grass Tassel", getGradientName("Glows faintly at night; said to lure curious fish"),
                        "",getGradientName("Exclusive Bait: Basic Magic Bait")));
                items.add(new CustomItemStack(Material.MOSS_CARPET, "§aWater Moss Fluff", getGradientName("Soft and damp, a favorite hiding spot for small fish"),
                        "",getGradientName("Exclusive Bait: Basic Magic Bait")));
                items.add(new CustomItemStack(Material.SLIME_BALL, "§aFrogcall Shell", getGradientName("A gentle squeeze makes a ‘Ribbit’ sound; fish think it’s one of their own"),
                        "",getGradientName("Exclusive Bait: Basic Magic Bait")));
                items.add(new CustomItemStack(Material.POPPY, "§aDew Lotus Petal", getGradientName("Carries the fresh scent of morning dew and purifies murky water"),
                        "",getGradientName("Exclusive Bait: Basic Magic Bait")));
                items.add(new CustomItemStack(Material.PRISMARINE_SHARD, "§aFish Scale Dust", ("§fGlimmers in the sunlight, a signal of the school’s identity"),
                        "",getGradientName("Exclusive Bait: Basic Magic Bait")));

                items.add(new CustomItemStack(Material.RED_SAND, "§6Ground Copper Sand", getGradientName("Has a faint metallic sheen, a scent beacon for copper-vein fish"),
                        "",getGradientName("Exclusive Bait: Magic Bait - Mixed Ore Dust")));
                items.add(new CustomItemStack(Material.RED_DYE, "§6Rust Powder", getGradientName("Rust scraped from abandoned mining gear; fish sense a ‘kindred’ presence"),
                        "",getGradientName("Exclusive Bait: Magic Bait - Mixed Ore Dust")));
                items.add(new CustomItemStack(Material.GLOW_INK_SAC, "§6Gold Dust Residue", getGradientName("Tailings from gold panning, still carrying the magic of a ‘rich ore zone’"),
                        "",getGradientName("Exclusive Bait: Magic Bait - Mixed Ore Dust")));
                items.add(new CustomItemStack(Material.QUARTZ, "§6Quartz Shards", getGradientName("Crystal fragments from Nether veins that stabilize the ore powder’s energy field"),
                        "",getGradientName("Exclusive Bait: Magic Bait - Mixed Ore Dust")));
                items.add(new CustomItemStack(Material.COAL, "§6Carbon Crystal Granules", getGradientName("Ancient plant remains buried deep in the crust, giving the ore powder an energy base"),
                        "",getGradientName("Exclusive Bait: Magic Bait - Mixed Ore Dust")));
                items.add(new CustomItemStack(Material.NETHER_STAR, "§dStardust Iron Dust", ("§fExtremely rare, said to come from a meteor core; greatly boosts attraction"),
                        "",getGradientName("Exclusive Bait: Magic Bait - Mixed Ore Dust")));

                items.add(new CustomItemStack(Material.COPPER_INGOT, "§bNative Copper Vein Fragment", getGradientName("Not smelted, but a natural conductive ore vein stripped straight from the rock"),
                        "",getGradientName("Exclusive Bait: Magic Bait - Mixed Minerals")));
                items.add(new CustomItemStack(Material.IRON_INGOT, "§bHematite Core", ("§fA high-purity iron core with intact crystal structure, emitting a ‘metallic heartbeat’ signal"),
                        "",getGradientName("Exclusive Bait: Magic Bait - Mixed Minerals")));
                items.add(new CustomItemStack(Material.GOLD_INGOT, "§bGold Vein Crystal", getGradientName("A net-like gold crystal formed naturally under pressure, a living beacon of the ‘rich ore zone’"),
                        "",getGradientName("Exclusive Bait: Magic Bait - Mixed Minerals")));
                items.add(new CustomItemStack(Material.AMETHYST_SHARD, "§bDeepstone Crystal Core", getGradientName("A resonant crystal from deep in the crust that amplifies the range of ore signals"),
                        "",getGradientName("Exclusive Bait: Magic Bait - Mixed Minerals")));
                items.add(new CustomItemStack(Material.COAL_BLOCK, "§bLava Carbon Core", getGradientName("A wood heart carbonized beside magma for a thousand years, holding geothermal energy and stabilizing ore core activity"),
                        "",getGradientName("Exclusive Bait: Magic Bait - Mixed Minerals")));
                items.add(new CustomItemStack(Material.NETHER_STAR, "§5Star Core Fragment", getGradientName("Said to be a core shard of a fallen star, able to mimic ‘earth-core-grade’ vein signals"),
                        "",getGradientName("Exclusive Bait: Magic Bait - Mixed Minerals")));

                items.add(new CustomItemStack(new ItemStack(Material.SUGAR_CANE,2),"§bRotten Sugar Cane",getGradientName("Why is there sugar cane in the river?"),
                        "",getGradientName("Exclusive Bait: Jiang Taigong Fishing")));
                items.add(new CustomItemStack(new ItemStack(Material.STICK,2),"§b锟斤拷",getGradientName("What is this thing?"),
                        "",getGradientName("Exclusive Bait: Jiang Taigong Fishing")));
                items.add(new CustomItemStack(new ItemStack(Material.INK_SAC,2),"§bFresh Ink Sac",getGradientName("Who throws an ink sac into a river?"),
                        "",getGradientName("Exclusive Bait: Jiang Taigong Fishing")));
                items.add(new CustomItemStack(new ItemStack(Material.CAKE,2),"§bStale Cake",getGradientName("Couldn’t finish the cake?"),
                        "",getGradientName("Exclusive Bait: Jiang Taigong Fishing")));
                items.add(new CustomItemStack(new ItemStack(Material.REDSTONE,8),"§b8-bit Redstone",getGradientName("A handful of exactly 8?"),
                        "",getGradientName("Exclusive Bait: Jiang Taigong Fishing")));
                items.add(new CustomItemStack(new ItemStack(Material.DISPENSER,2),"§bThe Dispenser Slimefun Needs",getGradientName("Is putting it on the ground enough?"),
                        "",getGradientName("Exclusive Bait: Jiang Taigong Fishing")));


                items.add(new CustomItemStack(new ItemStack(Material.BOWL),"§6Toilet Lid",getGradientName("The sacred ring of an ancient purification rite: open it to reach the netherworld, close it to seal away filth."), getGradientName("Mortals do not know it was once the barrier door of a god relieving themselves.")
                        ,"",getGradientName("Exclusive Bait: Magic Bait - Mixed Alloy Slurry")));
                items.add(new CustomItemStack(new ItemStack(Material.YELLOW_DYE),"§eBanana Peel",getGradientName("Has tripped three kings, two unicorns, and an adventurer who claimed never to fall."),getGradientName("Said to come from a golden tree smiling under the moonlight, born for fate’s stumbles.")
                        ,"",getGradientName("Exclusive Bait: Magic Bait - Mixed Alloy Slurry")));
                items.add(new CustomItemStack(new ItemStack(Material.IRON_SHOVEL),"§aPlunger",getGradientName("An imitation of the abyssal soul-sucker; every press seals a whisper from the sewers."),getGradientName("A true master uses it not just to clear pipes, but to commune with the spirit realm."),getGradientName("ber~ber~ber~")
                        ,"",getGradientName("Exclusive Bait: Magic Bait - Mixed Alloy Slurry")));
                items.add(new CustomItemStack(CustomHead.getHead("1421f1514da756c8c6c7c0b83a79265c26c9ece66b3bad8fbd94bd96d7040d7e"),"§bMoray Eel",getGradientName("A living electric whip swimming in deep-sea rifts, with the remnant soul of an ancient thunder god in its spine."),getGradientName("Fishermen call it “Wrath of the Black Tide”; those who touch it convulse and spit out sparks.")
                        ,"",getGradientName("Exclusive Bait: Magic Bait - Mixed Alloy Slurry")));
                items.add(new CustomItemStack(CustomHead.getHead("a1f71182915f5f862189a81f690acde4f671075db267eb6128fd1b4a84da8d7c"),"§cLengshang’s Wheelchair",getGradientName("Legendary gear made for “AFK players”; equip it and you can beat the final boss in your sleep."),getGradientName("—It’s not that you’re too strong; the wheelchair took on all the difficulty for you.")
                        ,"",getGradientName("Exclusive Bait: Magic Bait - Mixed Alloy Slurry")));
                items.add(new CustomItemStack(new ItemStack(Material.COCOA_BEANS),"§cPoop"
                        ,"",getGradientName("Exclusive Bait: Magic Bait - Mixed Alloy Slurry")));
                items.add(new CustomItemStack(CustomHead.MAGICSOLO.getItem(),getGradientName("magicsolo"),getGradientName("A dream must end in waking; all of life is but an empty dream~"),getGradientName("If a dream is like life, I dream of neither past nor present~")
                        ,"",getGradientName("Exclusive Bait: Magic Bait - Mixed Alloy Slurry")));
                items.add(new CustomItemStack(new ItemStack(Material.GOLDEN_SHOVEL),"§eGolden Shovel",getGradientName("Are you trying to get more people?"),getGradientName("Or do you want an emblem?")
                        ,"",getGradientName("Exclusive Bait: Magic Bait - Mixed Alloy Slurry")));




                break;
            case "magic_sugar":
                items.add(new CustomItemStack(Material.SUGAR,("§6Magic Candy")," ",("§a§oFeel the power of Hermes!")));
                items.add(MagicExpansionItems.MAGIC_EXPANSION_MAGIC_SUGAR_1);
                items.add(SlimefunItems.MAGIC_LUMP_1);
                items.add(SlimefunItems.MAGIC_LUMP_2);
                items.add(SlimefunItems.MAGIC_LUMP_3);
                items.add(SlimefunItems.ENDER_LUMP_1);
                items.add(SlimefunItems.ENDER_LUMP_2);
                items.add(SlimefunItems.ENDER_LUMP_3);
                items.add(SlimefunItems.MAGICAL_GLASS);
                items.add(SlimefunItems.MAGICAL_BOOK_COVER);
                items.add(SlimefunItems.LAVA_CRYSTAL);
                items.add(SlimefunItems.COMMON_TALISMAN);
                items.add(SlimefunItems.NECROTIC_SKULL);
                items.add(SlimefunItems.ESSENCE_OF_AFTERLIFE);
                items.add(SlimefunItems.SYNTHETIC_SHULKER_SHELL);
                items.add(SlimefunItems.BLANK_RUNE);
                items.add(SlimefunItems.AIR_RUNE);
                items.add(SlimefunItems.EARTH_RUNE);
                items.add(SlimefunItems.FIRE_RUNE);
                items.add(SlimefunItems.WATER_RUNE);
                items.add(SlimefunItems.ENDER_RUNE);
                items.add(SlimefunItems.LIGHTNING_RUNE);
                items.add(SlimefunItems.RAINBOW_RUNE);
                items.add(SlimefunItems.SOULBOUND_RUNE);
                items.add(SlimefunItems.ENCHANTMENT_RUNE);
                items.add(SlimefunItems.VILLAGER_RUNE);
                items.add(SlimefunItems.STRANGE_NETHER_GOO);
                items.add(SlimefunItems.RAINBOW_LEATHER);
                items.add(MagicExpansionItems.RANDOM_FISH_COMMON);
                items.add(MagicExpansionItems.RANDOM_FISH_UNCOMMON);
                items.add(MagicExpansionItems.RANDOM_FISH_RARE_POOL_DUST);
                items.add(MagicExpansionItems.RANDOM_FISH_RARE_POOL_ORE);
                items.add(MagicExpansionItems.RANDOM_FISH_RARE_POOL_INDUSTRY);
                items.add(MagicExpansionItems.RANDOM_FISH_EPIC_POOL_INDUSTRY);
                items.add(MagicExpansionItems.RANDOM_FISH_EPIC);
                items.add(FishManager.getFishItemWithLore(BaseFish.LegendaryEelFish,"",getGradientName("Exclusive bait: Magic Candy")));
                items.add(new CustomItemStack(
                        new ItemStack(Material.PRISMARINE_SHARD, 1),
                        getGradientNameVer2("Bait: Memory Fragment"),
                        ("§fThis bait can catch any item"),
                        ("§fIt exists in the past, or perhaps the future"),
                        ("§fWhat you see now is not its true self")
                ));
                break;
            case "bread":
                items.add(new CustomItemStack(Material.BREAD,("§bBread")," ",getGradientName("It’s just an ordinary piece of bread")));
                items.add(new CustomItemStack(new ItemStack(Material.COD, 3), "§bLost Raw Cod", getGradientName("Whose cod is this?")));
                items.add(new CustomItemStack(new ItemStack(Material.SALMON, 3), "§bDizzy Raw Salmon", getGradientName("Ever heard of the Salmon Return Formation?")));
                items.add(new CustomItemStack(new ItemStack(Material.TROPICAL_FISH, 3), "§bTropical Fish with 1.4", getGradientName("How did a tropical fish end up in a river?")));
                items.add(new CustomItemStack(new ItemStack(Material.PUFFERFISH, 3), "§bGreen Pufferfish", getGradientName("You shouldn’t eat this carelessly~")));
                items.add(new CustomItemStack(new ItemStack(Material.POTION, 3), "§bMysterious Potion", getGradientName("Maybe it’s drinkable?")));
                items.add(new ItemStack(Material.HONEY_BOTTLE, 256));
                items.add(MagicExpansionItems.RANDOM_FISH_COMMON);
                items.add(MagicExpansionItems.RANDOM_FISH_UNCOMMON);
                items.add(MagicExpansionItems.RANDOM_FISH_EPIC);
                items.add(MagicExpansionItems.RANDOM_FISH_EPIC_POOL_INDUSTRY);
                items.add(MagicExpansionItems.RANDOM_FISH_RARE_POOL_DUST);
                items.add(MagicExpansionItems.RANDOM_FISH_RARE_POOL_ORE);
                items.add(MagicExpansionItems.RANDOM_FISH_RARE_POOL_INDUSTRY);
                items.add(new CustomItemStack(
                        new ItemStack(Material.PRISMARINE_SHARD, 1),
                        getGradientNameVer2("Bait: Memory Fragment"),
                        ("§fThis bait can catch any item"),
                        ("§fIt exists in the past, or perhaps the future"),
                        ("§fWhat you see now is not its true self")
                ));
                break;
            default:
                items.add(new ItemStack(Material.BARRIER));
                break;
        }

        return items;
    }
}
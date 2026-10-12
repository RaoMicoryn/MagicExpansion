package io.Yomicer.magicExpansion.utils;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.MagicExpansionItemSetup;
import io.Yomicer.magicExpansion.core.MagicExpansionItems;
import io.Yomicer.magicExpansion.items.groups.VirtualGuideGroup;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.Yomicer.magicExpansion.utils.CustomHeadUtils.CustomHead;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.groups.FlexItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.chat.ChatInput;
import io.github.thebusybiscuit.slimefun4.implementation.guide.SurvivalSlimefunGuide;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.List;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientNameVer2;
import static io.Yomicer.magicExpansion.utils.Utils.doGlow;

/**
 * Magic2.0 Custom category page + Level-three menu
 * UI Closely matches the native Slimefun guide page:顶部 createHeader,槽1 Back,槽9~44 Group icon(Per page36个),槽46/52 翻page
 * Back链:每"进入新层级"才写入 GuideHistory(Paging within the same level does not push history, 避免逐page回退),Back button左键逐级Back(goBack)、Shift Return to main menu
 */
public final class GuideCategoryMenu {

    private static final int ITEMS_PER_PAGE = 36;
    private static final int FIRST_SLOT = 9;
    private static final int LAST_SLOT = 44;

    private GuideCategoryMenu() {
    }

    // ==================== Level-one menu:54格大箱子 ====================

    /** White filler slots */
    private static final int[] WHITE_SLOTS = {0, 2, 3, 5, 6, 8, 9, 17, 18, 26, 36, 44, 45, 53};
    /** Rainbow color-changing slots(原品红色玻璃板) */
    private static final int[] RAINBOW_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 25, 28, 34, 37, 43, 46, 47, 48, 50, 51, 52};
    /** Rainbow color sequence(Cycles every second) */
    private static final Material[] RAINBOW_PANES = {
            Material.RED_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE,
            Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE,
            Material.PINK_STAINED_GLASS_PANE
    };
    /** Snake-like gradient loop order(顺时针, 跳过第49槽的神器小按钮) */
    private static final int[] RAINBOW_RING = {
            10, 11, 12, 13, 14, 15, 16,
            25, 34, 43,
            52, 51, 50, 48, 47, 46,
            37, 28, 19
    };
    /** Snake-like gradient rotation speed(Moves one slot every 2 ticks) */
    private static final long RAINBOW_STEP_TICKS = 2L;
    /** 与 RAINBOW_PANES 对应的色相(角度), 用于把渐变色映射到最接近的玻璃板 */
    private static final float[] RAINBOW_HUES = {
            0f, 30f, 60f, 90f, 120f, 180f, 195f, 240f, 270f, 300f, 330f
    };
    /** Main group display slots(20-24,29-33,38-42) */
    private static final int[] BIG_GROUP_SLOTS = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42};
    /** Level-two menu white filler slots */
    private static final int[] SECOND_WHITE_SLOTS = {0, 2, 3, 5, 6, 8, 9, 17, 36, 44, 45, 47, 48, 50, 51, 53};
    /** Level-two menu pink filler slots */
    private static final int[] SECOND_PINK_SLOTS = {10, 11, 12, 13, 14, 15, 16, 37, 38, 39, 40, 41, 42, 43};
    /** Level-two menu light-blue filler slots */
    private static final int[] SECOND_LIGHT_BLUE_SLOTS = {18, 26, 27, 35};
    /** Level-two menu magenta filler slots(固定,不做彩虹) */
    private static final int[] SECOND_MAGENTA_SLOTS = {19, 25, 28, 34};
    /** Level-two menu group slots(20-24,29-33) */
    private static final int[] SECOND_GROUP_SLOTS = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
    /** Level-four menu(Item List)Items per page: 4x7 */
    private static final int ITEM_PAGE_SIZE = 28;
    /** Level-four item slots(4行x7列, 居中) */
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    /** Level-four menu白色填充(顶行+底角) */
    private static final int[] ITEM_WHITE_SLOTS = {0, 2, 3, 5, 6, 8, 45, 53};
    /** Level-four menuItems区边框(淡蓝) */
    private static final int[] ITEM_FRAME_SLOTS = {9, 17, 18, 26, 27, 35, 36, 44};
    /** Level-four menu底部点缀(粉红) */
    private static final int[] ITEM_BOTTOM_SLOTS = {47, 48, 50, 51};
    /** Level-four menu历史锚点缓存(Each group+Page number一个稳定锚点, 保证Back不重复堆历史) */
    private static final Map<String, VirtualGuideGroup> ITEM_PAGE_ANCHORS = new HashMap<>();


    private static final Map<UUID, Integer> RAINBOW_TASKS = new HashMap<>();

    /** Developer magicsolo 头颅材质(与Contributors组的 MAGIC_EXPANSION_AUTHOR 一致) */
    private static final ItemStack DEVELOPER_HEAD = CustomHead.getHead("8adb25ab9976d89d0bd8118d72c1c06bb907060c1e02a729b652d1e86b1ebbbc");

    /** 1-3 阶Magic Candy(神奇小按钮随机奖励范围, 由 1-5 阶下调) */
    private static final ItemStack[] MAGIC_SUGARS = {
            MagicExpansionItems.MAGIC_EXPANSION_MAGIC_SUGAR_1,
            MagicExpansionItems.MAGIC_EXPANSION_MAGIC_SUGAR_2,
            MagicExpansionItems.MAGIC_EXPANSION_MAGIC_SUGAR_3
    };

    /** 无名字的染色玻璃板(隐藏原版名称, 用单个空格代替) */
    private static ItemStack plainPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }
    /** A magical little button:10 个特殊事件随机触发,概率相同 */
    private static void triggerMysteryButton(Player player) {
        final Random random = new Random();
        switch (random.nextInt(10)) {
            case 0 -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 100, 0));
                player.sendMessage(getGradientNameVer2("Minecrafters can fly!"));
            }
            case 1 -> {
                player.setHealth(0.0);
                player.sendMessage(getGradientNameVer2(player.getName() + " felt the magic"));
            }
            case 2 -> {
                ItemStack sugar = MAGIC_SUGARS[random.nextInt(MAGIC_SUGARS.length)].clone();
                Map<Integer, ItemStack> left = player.getInventory().addItem(sugar);
                if (!left.isEmpty()) {
                    player.getWorld().dropItem(player.getLocation(), left.get(0));
                }
                player.sendMessage(getGradientNameVer2("Come have some candy"));
            }
            case 3 -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                player.sendMessage(getGradientNameVer2("Fall into the darkness!"));
            }
            case 4 -> {
                // 饱餐一顿
                player.setFoodLevel(20);
                player.setSaturation(20.0f);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 60, 0));
                player.sendMessage(getGradientNameVer2("Fireworks in the heart, and the world turns gentle."));
            }
            case 5 -> {
                // 空间错位: 周围 5~15 格随机传送(落点取安全高度)
                Location loc = player.getLocation();
                double angle = random.nextDouble() * Math.PI * 2;
                double dist = 5 + random.nextDouble() * 10;
                double x = loc.getX() + Math.cos(angle) * dist;
                double z = loc.getZ() + Math.sin(angle) * dist;
                int y = loc.getWorld().getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1;
                player.teleport(new Location(loc.getWorld(), x + 0.5, y, z + 0.5, loc.getYaw(), loc.getPitch()));
                player.sendMessage(getGradientNameVer2("Wind stirs in the sleeves; mountains and rivers step back."));
            }
            case 6 -> {
                // 放一场烟花
                Firework firework = player.getWorld().spawn(player.getLocation().add(0, 1, 0), Firework.class);
                FireworkMeta meta = firework.getFireworkMeta();
                Color color = Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256));
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BURST)
                        .withColor(color)
                        .withFade(Color.WHITE)
                        .build());
                meta.setPower(1);
                firework.setFireworkMeta(meta);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        firework.detonate();
                    }
                }.runTaskLater(MagicExpansion.getInstance(), 2L);
                player.sendMessage(getGradientNameVer2("All is silent; only this moment blooms."));
            }
            case 7 -> {
                // 音效合唱: 从原版全部音效里随机播一种
                Sound[] sounds = Sound.values();
                Sound sound = sounds[random.nextInt(sounds.length)];
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f + random.nextFloat());
                player.sendMessage(getGradientNameVer2("What's that racket?"));
            }
            case 8 -> {
                // 按钮低语: 只低语一句, 不再额外提示
                String[] whispers = {
                        "You stepped on the hem of time.",
                        "Tonight's moon is sweet.",
                        "The wind carried a name to your ear.",
                        "Some doors open only for those who turn back.",
                        "It remembers you pressed it in your past life too.",
                        "Far away, the sea turned over for you."
                };
                player.sendMessage(getGradientNameVer2(whispers[random.nextInt(whispers.length)]));
            }
            case 9 -> {
                // Anglers的馈赠: 随机 1~3 种Bait(Dream Weaver+Aquatic Realm), 每种 1~5 个
                ItemStack[] baits = {
                        MagicExpansionItems.FISH_LURE_BASIC,
                        MagicExpansionItems.FISH_LURE_DUST,
                        MagicExpansionItems.FISH_LURE_ORE,
                        MagicExpansionItems.FISH_LURE_ALLOY_INGOT,
                        MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_CUIXIA,
                        MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_WEICHEN,
                        MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_RONGHUO,
                        MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_YUEJIN,
                        MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XINGHE
                };
                List<ItemStack> pool = new ArrayList<>(List.of(baits));
                Collections.shuffle(pool, random);
                int kinds = 1 + random.nextInt(3);
                for (int i = 0; i < kinds && i < pool.size(); i++) {
                    int amount = 1 + random.nextInt(5);
                    ItemStack bait = pool.get(i).clone();
                    bait.setAmount(amount);
                    Map<Integer, ItemStack> left = player.getInventory().addItem(bait);
                    if (!left.isEmpty()) {
                        player.getWorld().dropItem(player.getLocation(), left.get(0));
                    }
                }
                player.sendMessage(getGradientNameVer2("A gift from the anglers"));
            }
            default -> { }
        }
    }
    /** Level-one menu:ClickMagic2.0进入(入口调用, 记录历史) */
    public static void openCategoryPage(Player player, PlayerProfile profile, SlimefunGuideMode mode, int page) {
        openCategoryPage(player, profile, mode, page, true);
    }

    /**
     * Level-one menu打开逻辑。
     * 翻page修复: 仅"进入该层级"时写入 GuideHistory, 同层翻page(trackHistory=false)只重绘不压栈,
     * Allow the back button to return to the parent menu in one click, 而不是逐page回退历史翻page操作。
     * 防御性自愈: Level-one menu是Magic2.0 的顶层入口, 进入时清空历史栈后仅保留自身条目——
     * Clear external components(如 JEG 搜索翻page的"弹1压1"失衡)留下的残留条目,
     * 避免残留的Magic2.0 旧条目被重开导致界面意外跳转。
     */
    public static void openCategoryPage(Player player, PlayerProfile profile, SlimefunGuideMode mode, int page, boolean trackHistory) {
        if (trackHistory) {
            profile.getGuideHistory().clear(); // 自愈: 顶层入口重置历史, 隔离外部残留
            addHistory(profile, mode, MagicExpansionItemSetup.magicexpansion, page);
        }

        ChestMenu menu = createMainMenu(player, mode);

        // 白色填充
        for (int slot : WHITE_SLOTS) {
            menu.addItem(slot, plainPane(Material.WHITE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        // 彩虹填充(初始品红,打开后每秒变色)
        for (int slot : RAINBOW_SLOTS) {
            menu.addItem(slot, plainPane(Material.MAGENTA_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }

        // 1槽:Back(Return to main menu)
        menu.addItem(1, ChestMenuUtils.getBackButton(player));
        menu.addMenuClickHandler(1, (p, s, it, a) -> {
            new SurvivalSlimefunGuide().openMainMenu(profile, profile.getGuideHistory().getMainMenuPage());
            return false;
        });

        // 7槽:原版搜索
        menu.addItem(7, ChestMenuUtils.getSearchButton(player));
        menu.addMenuClickHandler(7, (p, s, it, a) -> {
            p.closeInventory();
            Slimefun.getLocalization().sendMessage(p, "guide.search.message");
            ChatInput.waitForPlayer(MagicExpansion.getInstance(), p, message -> {
                SlimefunGuideImplementation guide = Slimefun.getRegistry().getSlimefunGuide(mode);
                guide.openSearch(profile, message, false);
            });
            return false;
        });

        // 4槽:Additional Info(Developer头颅材质,Click由监听器打开Level-two menu)
        menu.addItem(4, createVirtualIcon(DEVELOPER_HEAD, ColorGradient.getRandomGradientName("Additional Info"), "Click to view contributors and changelog", "attachmentinfo"));
        menu.addMenuClickHandler(4, (p, s, it, a) -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return false;
        });

        // 大Group展示(按当前顺序:Container group + Leaf group,剩余用屏障占位)
        List<ItemGroup> bigGroups = GuideMenuGroups.getTopLevel();
        for (int i = 0; i < BIG_GROUP_SLOTS.length; i++) {
            int slot = BIG_GROUP_SLOTS[i];
            if (i < bigGroups.size()) {
                ItemGroup group = bigGroups.get(i);
                ItemStack icon = group.getItem(player);
                menu.addItem(slot, icon);
                menu.addMenuClickHandler(slot, (p, s, it, a) -> {
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    // Container group由 GuideVirtualGroupClickListener Intercept and handle;Leaf group直接打开原生Itemspage
                    if (getVirtualGroupId(it) == null) {
                        openItemGroupPage(p, profile, mode, group, 1);
                    }
                    return false;
                });
            } else {
                menu.addItem(slot, new CustomItemStack(Material.BARRIER, getGradientNameVer2("Coming soon")), ChestMenuUtils.getEmptyClickHandler());
            }
        }

        // 翻page:27Previous Page / 35Next Page(目前一page,Feature implemented)
        // 翻page:27Previous Page / 35Next Page(Show white glass panes when there are no more pages)
        int pages = Math.max(1, (bigGroups.size() + BIG_GROUP_SLOTS.length - 1) / BIG_GROUP_SLOTS.length);
        if (page > 1) {
            menu.addItem(27, ChestMenuUtils.getPreviousButton(player, page, pages));
            menu.addMenuClickHandler(27, (p, s, it, a) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                // 翻page修复: trackHistory=false, 同层翻page不写入历史栈
                openCategoryPage(p, profile, mode, page - 1, false);
                return false;
            });
        } else {
            menu.addItem(27, plainPane(Material.WHITE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        if (page < pages) {
            menu.addItem(35, ChestMenuUtils.getNextButton(player, page, pages));
            menu.addMenuClickHandler(35, (p, s, it, a) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                // 翻page修复: trackHistory=false, 同层翻page不写入历史栈
                openCategoryPage(p, profile, mode, page + 1, false);
                return false;
            });
        } else {
            menu.addItem(35, plainPane(Material.WHITE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        // 49槽:神器小按钮(Feature pending,后续单独文件存放)
        menu.addItem(49, new CustomItemStack(Material.NETHER_STAR, getGradientNameVer2("A magical little button"), getGradientNameVer2("What happens if you click it?")));
        menu.addMenuClickHandler(49, (p, s, it, a) -> {
            triggerMysteryButton(p);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return false;
        });

        menu.open(player);
        startRainbow(player, menu);
    }

    private static ChestMenu createMainMenu(Player player, SlimefunGuideMode mode) {
        String title = Slimefun.getLocalization().getMessage(player, "guide.title.main");
        ChestMenu menu = new ChestMenu(title);
        menu.setEmptySlotsClickable(false);
        menu.addMenuOpeningHandler(p -> p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f));
        return menu;
    }

    /** 判断界面标题是否为Magic2.0自绘菜单标题(即复用了原版主page标题 guide.title.main)。
     *  用于收窄全局Click监听器的劫持范围：第三方Machines菜单的标题不是这个, Allow through directly. */
    public static boolean isGuideMenuTitle(Player player, String title) {
        if (title == null) return false;
        String expected = Slimefun.getLocalization().getMessage(player, "guide.title.main");
        return ChatColor.stripColor(title).trim().equals(ChatColor.stripColor(expected).trim());
    }

    /** 彩虹玻璃板:蛇形渐变环绕, 每2 tick 顺时针挪一格 */
    private static void startRainbow(Player player, ChestMenu menu) {
        cancelRainbow(player);
        final int[] step = {0};
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    RAINBOW_TASKS.remove(player.getUniqueId());
                    return;
                }
                int current = step[0]++;
                for (int p = 0; p < RAINBOW_RING.length; p++) {
                    int slot = RAINBOW_RING[p];
                    int colorIndex = Math.floorMod(p - current, RAINBOW_RING.length);
                    menu.replaceExistingItem(slot, plainPane(rainbowGradientMaterial(colorIndex, RAINBOW_RING.length)));
                }
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, RAINBOW_STEP_TICKS);
        RAINBOW_TASKS.put(player.getUniqueId(), task.getTaskId());
    }

    /** 把环上的等距位置映射为最接近的玻璃板渐变色(19格顺滑衔接头尾) */
    private static Material rainbowGradientMaterial(int index, int size) {
        float hue = index * 360f / size;
        int best = 0;
        float bestDiff = Float.MAX_VALUE;
        for (int i = 0; i < RAINBOW_HUES.length; i++) {
            float diff = Math.abs(RAINBOW_HUES[i] - hue);
            diff = Math.min(diff, 360f - diff);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = i;
            }
        }
        return RAINBOW_PANES[best];
    }

    /** Stop the player’s rainbow task(菜单关闭时调用) */
    public static void cancelRainbow(Player player) {
        Integer id = RAINBOW_TASKS.remove(player.getUniqueId());
        if (id != null) {
            Bukkit.getScheduler().cancelTask(id);
        }
    }
    public static final NamespacedKey VIRTUAL_KEY = new NamespacedKey(MagicExpansion.getInstance(), "guide_virtual");

    /** 创建带虚拟Group标记的图标(用于原生Grouppage显示Container group,Click由监听器拦截) */
    public static ItemStack createVirtualIcon(ItemStack icon, String name, String lore, String id) {
        CustomItemStack base = new CustomItemStack(icon, name, lore);
        ItemMeta meta = base.getItemMeta();
        meta.getPersistentDataContainer().set(VIRTUAL_KEY, PersistentDataType.STRING, id);
        base.setItemMeta(meta);
        return base;
    }

    /** 读取虚拟Group标记,非虚拟GroupBack null */
    public static String getVirtualGroupId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(VIRTUAL_KEY, PersistentDataType.STRING);
    }

    /** 按标记打开对应的自绘菜单(Container group查注册表, Leaf groups have no virtual marker) */
    public static void openVirtualGroup(Player player, PlayerProfile profile, SlimefunGuideMode mode, String id) {
        if (id == null) return;
        if (GuideMenuGroups.isContainer(id)) {
            openContainer(player, profile, mode, id);
        }
    }

    /* ==================== Container group的子Grouppage(Use the registry consistently) ==================== */

    /** 打开任意Container group的子级page面(层级由 GuideMenuGroups 配置决定) */
    public static void openContainer(Player player, PlayerProfile profile, SlimefunGuideMode mode, String id) {
        VirtualGuideGroup anchor = GuideMenuGroups.getAnchor(id);
        if (anchor == null) return;
        // 入口调用: 记录历史(压栈), Back button可回到上级
        openSecondLevel(player, profile, mode, 1, anchor, GuideMenuGroups.getChildren(id), true);
    }

    /** Level-two menu:Page opened from a container group(54格大箱子)。入口(openContainer)压栈, Paging within the same level does not push history */
    private static void openSecondLevel(Player player, PlayerProfile profile, SlimefunGuideMode mode, int page,
                                        ItemGroup historyAnchor, List<ItemGroup> groups, boolean trackHistory) {
        if (trackHistory) {
            addHistory(profile, mode, historyAnchor, page);
        }
        ChestMenu menu = createMainMenu(player, mode);

        // 固定装饰
        for (int slot : SECOND_WHITE_SLOTS) {
            menu.addItem(slot, plainPane(Material.WHITE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        for (int slot : SECOND_PINK_SLOTS) {
            menu.addItem(slot, plainPane(Material.PINK_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        for (int slot : SECOND_LIGHT_BLUE_SLOTS) {
            menu.addItem(slot, plainPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        for (int slot : SECOND_MAGENTA_SLOTS) {
            menu.addItem(slot, plainPane(Material.MAGENTA_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }

        // 1槽:Back上一级(确定性:直接重开Magic2.0Level-one menu, 不依赖历史栈)
        addDeterministicBack(menu, player, profile, mode,
                () -> openCategoryPage(player, profile, mode, 1, true));

        // 4槽:Author head(纯展示,无Click)
        menu.addItem(4, new CustomItemStack(DEVELOPER_HEAD, getGradientNameVer2("magicsolo"), getGradientNameVer2("This is the Magic author")), ChestMenuUtils.getEmptyClickHandler());

        // 7槽:原版搜索
        menu.addItem(7, ChestMenuUtils.getSearchButton(player));
        menu.addMenuClickHandler(7, (p, s, it, a) -> {
            p.closeInventory();
            Slimefun.getLocalization().sendMessage(p, "guide.search.message");
            ChatInput.waitForPlayer(MagicExpansion.getInstance(), p, message -> {
                SlimefunGuideImplementation guide = Slimefun.getRegistry().getSlimefunGuide(mode);
                guide.openSearch(profile, message, false);
            });
            return false;
        });

        // 二级Group槽位(20-24,29-33)
        for (int i = 0; i < SECOND_GROUP_SLOTS.length; i++) {
            int slot = SECOND_GROUP_SLOTS[i];
            if (i < groups.size()) {
                ItemGroup group = groups.get(i);
                menu.addItem(slot, group.getItem(player));
                menu.addMenuClickHandler(slot, (p, s, it, a) -> {
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    if (group instanceof FlexItemGroup) {
                        // 容器/Virtual group: 交给原生调度(FlexItemGroup.open -> Custom level-three page)
                        SlimefunGuide.openItemGroup(profile, group, mode, 1);
                    } else {
                        openItemGroupPage(p, profile, mode, group, 1);
                    }
                    return false;
                });
            } else {
                menu.addItem(slot, new CustomItemStack(Material.BARRIER, getGradientNameVer2("Coming soon")), ChestMenuUtils.getEmptyClickHandler());
            }
        }

        // 翻page:46Previous Page / 52Next Page(No more pages=白色,More pages available=黄绿色)
        int pages = Math.max(1, (groups.size() + SECOND_GROUP_SLOTS.length - 1) / SECOND_GROUP_SLOTS.length);
        if (page > 1) {
            menu.addItem(46, plainPane(Material.LIME_STAINED_GLASS_PANE));
            menu.addMenuClickHandler(46, (p, s, it, a) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                // 翻page修复: 同层翻page不写入历史栈
                openSecondLevel(p, profile, mode, page - 1, historyAnchor, groups, false);
                return false;
            });
        } else {
            menu.addItem(46, plainPane(Material.WHITE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        if (page < pages) {
            menu.addItem(52, plainPane(Material.LIME_STAINED_GLASS_PANE));
            menu.addMenuClickHandler(52, (p, s, it, a) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                // 翻page修复: 同层翻page不写入历史栈
                openSecondLevel(p, profile, mode, page + 1, historyAnchor, groups, false);
                return false;
            });
        } else {
            menu.addItem(52, plainPane(Material.WHITE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }

        // 49槽:神秘按钮(与一级一致,Feature pending)
        menu.addItem(49, new CustomItemStack(Material.NETHER_STAR, getGradientNameVer2("A magical little button"), getGradientNameVer2("What happens if you click it?")));
        menu.addMenuClickHandler(49, (p, s, it, a) -> {
            triggerMysteryButton(p);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return false;
        });

        menu.open(player);
    }

    /** Level-three menu:嵌套Container group(Dream Weaver/Aquatic Realm等)page面, 风格与Level-two menu一致。入口压栈, Paging within the same level does not push history */
    private static void openThirdLevel(Player player, PlayerProfile profile, SlimefunGuideMode mode, int page,
                                       ItemGroup historyAnchor, List<ItemGroup> groups, boolean trackHistory) {
        if (trackHistory) {
            addHistory(profile, mode, historyAnchor, page);
        }
        ChestMenu menu = createMainMenu(player, mode);

        // 固定装饰(与Level-two menu一致: 白/粉/淡蓝/品红)
        for (int slot : SECOND_WHITE_SLOTS) {
            menu.addItem(slot, plainPane(Material.WHITE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        for (int slot : SECOND_PINK_SLOTS) {
            menu.addItem(slot, plainPane(Material.PINK_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        for (int slot : SECOND_LIGHT_BLUE_SLOTS) {
            menu.addItem(slot, plainPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        for (int slot : SECOND_MAGENTA_SLOTS) {
            menu.addItem(slot, plainPane(Material.MAGENTA_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }

        // 1槽:Back上一级(确定性:直接重开Magic2.0Level-one menu, 不依赖历史栈)
        addDeterministicBack(menu, player, profile, mode,
                () -> openCategoryPage(player, profile, mode, 1, true));

        // 4槽:Author head(纯展示)
        menu.addItem(4, new CustomItemStack(DEVELOPER_HEAD, getGradientNameVer2("magicsolo"), getGradientNameVer2("This is the Magic author")), ChestMenuUtils.getEmptyClickHandler());

        // 7槽:原版搜索
        menu.addItem(7, ChestMenuUtils.getSearchButton(player));
        menu.addMenuClickHandler(7, (p, s, it, a) -> {
            p.closeInventory();
            Slimefun.getLocalization().sendMessage(p, "guide.search.message");
            ChatInput.waitForPlayer(MagicExpansion.getInstance(), p, message -> {
                SlimefunGuideImplementation guide = Slimefun.getRegistry().getSlimefunGuide(mode);
                guide.openSearch(profile, message, false);
            });
            return false;
        });

        // Level-three group slots(20-24,29-33): Leaf group直接打开Itemspage, Container group后续可继续下钻
        for (int i = 0; i < SECOND_GROUP_SLOTS.length; i++) {
            int slot = SECOND_GROUP_SLOTS[i];
            if (i < groups.size()) {
                ItemGroup group = groups.get(i);
                menu.addItem(slot, group.getItem(player));
                menu.addMenuClickHandler(slot, (p, s, it, a) -> {
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                    if (group instanceof FlexItemGroup) {
                        // 容器/Virtual group: 交给原生调度(FlexItemGroup.open -> Custom level-three page)
                        SlimefunGuide.openItemGroup(profile, group, mode, 1);
                    } else {
                        openItemGroupPage(p, profile, mode, group, 1);
                    }
                    return false;
                });
            } else {
                menu.addItem(slot, new CustomItemStack(Material.BARRIER, getGradientNameVer2("Coming soon")), ChestMenuUtils.getEmptyClickHandler());
            }
        }

        // 翻page:46Previous Page / 52Next Page(无page=白色, 有page=黄绿)
        int pages = Math.max(1, (groups.size() + SECOND_GROUP_SLOTS.length - 1) / SECOND_GROUP_SLOTS.length);
        if (page > 1) {
            menu.addItem(46, plainPane(Material.LIME_STAINED_GLASS_PANE));
            menu.addMenuClickHandler(46, (p, s, it, a) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                // 翻page修复: 同层翻page不写入历史栈
                openThirdLevel(p, profile, mode, page - 1, historyAnchor, groups, false);
                return false;
            });
        } else {
            menu.addItem(46, plainPane(Material.WHITE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        if (page < pages) {
            menu.addItem(52, plainPane(Material.LIME_STAINED_GLASS_PANE));
            menu.addMenuClickHandler(52, (p, s, it, a) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                // 翻page修复: 同层翻page不写入历史栈
                openThirdLevel(p, profile, mode, page + 1, historyAnchor, groups, false);
                return false;
            });
        } else {
            menu.addItem(52, plainPane(Material.WHITE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }

        // 49槽:A magical little button(与一二级一致)
        menu.addItem(49, new CustomItemStack(Material.NETHER_STAR, getGradientNameVer2("A magical little button"), getGradientNameVer2("What happens if you click it?")));
        menu.addMenuClickHandler(49, (p, s, it, a) -> {
            triggerMysteryButton(p);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return false;
        });

        menu.open(player);
    }

    /* ==================== Level-four menu: Leaf groupItem Listpage(4x7) ==================== */

    /** Level-four menu:Item list page opened from a leaf group(入口调用, 记录历史) */
    public static void openItemGroupPage(Player player, PlayerProfile profile, SlimefunGuideMode mode, ItemGroup group, int page) {
        openItemGroupPage(player, profile, mode, group, page, true);
    }

    /**
     * Level-four menu打开逻辑。风格延续二、三级, 淡蓝边框包裹 4x7 Items格。
     * 翻page修复: 仅"进入该层级"时写入 GuideHistory, 同层翻page(trackHistory=false)只重绘不压栈,
     * Allow the back button to return to the parent menu in one click。
     */
    private static void openItemGroupPage(Player player, PlayerProfile profile, SlimefunGuideMode mode, ItemGroup group, int page, boolean trackHistory) {
        if (group == null) return;
        if (group instanceof FlexItemGroup) {
            // 兜底: 容器/Virtual groups have no item list, 交还原生调度(FlexItemGroup.open -> Custom child page)
            SlimefunGuide.openItemGroup(profile, group, mode, 1);
            return;
        }

        if (trackHistory) {
            addHistory(profile, mode, itemPageAnchor(group, page, player), page);
        }

        String title = group.getDisplayName(player);
        ChestMenu menu = new ChestMenu(title == null ? "Item List" : title);
        menu.setEmptySlotsClickable(false);
        menu.addMenuOpeningHandler(p -> p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f));

        // 白色填充
        for (int slot : ITEM_WHITE_SLOTS) {
            menu.addItem(slot, plainPane(Material.WHITE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        // 淡蓝边框
        for (int slot : ITEM_FRAME_SLOTS) {
            menu.addItem(slot, plainPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        // 底部粉红点缀
        for (int slot : ITEM_BOTTOM_SLOTS) {
            menu.addItem(slot, plainPane(Material.PINK_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }

        // 1槽:Back上一级(确定性:反查Leaf group的父容器, Return to the parent container’s subcategory page; 一级Leaf group则回Magic2.0Level-one menu)
        addDeterministicBack(menu, player, profile, mode, () -> {
            String parentId = GuideMenuGroups.getParentContainerId(group.getKey().getKey());
            if (parentId != null) {
                openContainer(player, profile, mode, parentId);
            } else {
                openCategoryPage(player, profile, mode, 1, true);
            }
        });

        // 4槽:Author head(纯展示)
        menu.addItem(4, new CustomItemStack(DEVELOPER_HEAD, getGradientNameVer2("magicsolo"), getGradientNameVer2("This is the Magic author")), ChestMenuUtils.getEmptyClickHandler());

        // 7槽:原版搜索
        menu.addItem(7, ChestMenuUtils.getSearchButton(player));
        menu.addMenuClickHandler(7, (p, s, it, a) -> {
            p.closeInventory();
            Slimefun.getLocalization().sendMessage(p, "guide.search.message");
            ChatInput.waitForPlayer(MagicExpansion.getInstance(), p, message -> {
                SlimefunGuideImplementation guide = Slimefun.getRegistry().getSlimefunGuide(mode);
                guide.openSearch(profile, message, false);
            });
            return false;
        });
        // Visible-item filter(与原版一致: Disable/隐藏/Hide inaccessible groups)
        List<SlimefunItem> visible = new ArrayList<>();
        for (SlimefunItem item : group.getItems()) {
            if (item == null || item.isDisabledIn(player.getWorld()) || item.isHidden()) continue;
            if (!Slimefun.getConfigManager().isShowHiddenItemGroupsInSearch() && !item.getItemGroup().isAccessible(player)) continue;
            visible.add(item);
        }

        int pages = Math.max(1, (visible.size() + ITEM_PAGE_SIZE - 1) / ITEM_PAGE_SIZE);
        page = Math.max(1, Math.min(page, pages));
        final int currentPage = page;
        int start = (currentPage - 1) * ITEM_PAGE_SIZE;

        // 4x7 Items格
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            int slot = ITEM_SLOTS[i];
            int index = start + i;
            if (index < visible.size()) {
                SlimefunItem item = visible.get(index);
                if (isUiSeparatorPane(item)) {
                    // Contributors组的 UI 玻璃分隔板: 纯展示, 不可Click
                    menu.addItem(slot, item.getItem(), ChestMenuUtils.getEmptyClickHandler());
                } else if (isItemClickable(player, profile, mode, item)) {
                    menu.addItem(slot, item.getItem());
                    menu.addMenuClickHandler(slot, (p, s, it, a) -> {
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                        SlimefunGuide.displayItem(profile, item, true);
                        return false;
                    });
                } else {
                    menu.addItem(slot, renderLockedIcon(player, profile, mode, item), ChestMenuUtils.getEmptyClickHandler());
                }
            }
        }

        // 翻page:46Previous Page / 52Next Page(无page=白色, 有page=黄绿)
        if (currentPage > 1) {
            menu.addItem(46, plainPane(Material.LIME_STAINED_GLASS_PANE));
            menu.addMenuClickHandler(46, (p, s, it, a) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                // 翻page修复: 同层翻page不写入历史栈
                openItemGroupPage(p, profile, mode, group, currentPage - 1, false);
                return false;
            });
        } else {
            menu.addItem(46, plainPane(Material.WHITE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }
        if (currentPage < pages) {
            menu.addItem(52, plainPane(Material.LIME_STAINED_GLASS_PANE));
            menu.addMenuClickHandler(52, (p, s, it, a) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                // 翻page修复: 同层翻page不写入历史栈
                openItemGroupPage(p, profile, mode, group, currentPage + 1, false);
                return false;
            });
        } else {
            menu.addItem(52, plainPane(Material.WHITE_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        }

        // 49槽:A magical little button(与一二级一致)
        menu.addItem(49, new CustomItemStack(Material.NETHER_STAR, getGradientNameVer2("A magical little button"), getGradientNameVer2("What happens if you click it?")));
        menu.addMenuClickHandler(49, (p, s, it, a) -> {
            triggerMysteryButton(p);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return false;
        });

        menu.open(player);
    }

    /** Contributors组里的 UI 玻璃分隔板(UI_5~UI_9): 仅展示, 不可Click */
    private static boolean isUiSeparatorPane(SlimefunItem item) {
        return item != null && item.getId().startsWith("UI_")
                && item.getItem().getType().name().endsWith("STAINED_GLASS_PANE");
    }

    /** 为每个(Group, Page number)Cache a stable history anchor, 保证Prevent duplicate entries in the back-navigation history */
    private static VirtualGuideGroup itemPageAnchor(ItemGroup group, int page, Player player) {
        String rawKey = group.getKey().getKey() + ":" + page;
        String safeKey = rawKey.replaceAll("[^a-z0-9/._-]", "_");
        return ITEM_PAGE_ANCHORS.computeIfAbsent(rawKey, k -> new VirtualGuideGroup(
                new NamespacedKey(MagicExpansion.getInstance(), "itempage_" + safeKey),
                group.getItem(player),
                (p, pr, m) -> openItemGroupPage(p, pr, m, group, page)
        ));
    }

    /** Items是否可直接Click查看配方(No permission/未研究解锁则不可Click) */
    private static boolean isItemClickable(Player player, PlayerProfile profile, SlimefunGuideMode mode, SlimefunItem item) {
        if (mode != SlimefunGuideMode.SURVIVAL_MODE) return true;
        if (!Slimefun.getPermissionsService().hasPermission(player, item)) return false;
        return item.getResearch() == null || profile.hasUnlocked(item.getResearch());
    }

    /** No permission/未解锁Items的锁定样式(沿用原版 NoPermission 图标) */
    private static ItemStack renderLockedIcon(Player player, PlayerProfile profile, SlimefunGuideMode mode, SlimefunItem item) {
        List<String> lore = new ArrayList<>();
        if (!Slimefun.getPermissionsService().hasPermission(player, item)) {
            lore.addAll(Slimefun.getPermissionsService().getLore(item));
        }
        if (item.getResearch() != null && !profile.hasUnlocked(item.getResearch())) {
            lore.add("§7Requires research to unlock: §e" + item.getResearch().getName(player));
        }
        return new CustomItemStack(ChestMenuUtils.getNoPermissionItem(), item.getItemName(), lore.toArray(new String[0]));
    }

    /* ==================== 公共渲染 ==================== */

    private static void addHistory(PlayerProfile profile, SlimefunGuideMode mode, ItemGroup group, int page) {
        if (mode == SlimefunGuideMode.SURVIVAL_MODE) {
            profile.getGuideHistory().add(group, page);
        }
    }

    /**
     * 确定性Back button: 不依赖 GuideHistory 栈的 goBack(外部 JEG 搜索翻page可能污染栈导致误回主界面),
     * Back动作由调用方显式指定(直接调用上级界面的打开方法), Shift + Clicking still returns to the Slimefun guide main page.
     */
    private static void addDeterministicBack(ChestMenu menu, Player player, PlayerProfile profile,
                                             SlimefunGuideMode mode, Runnable backAction) {
        menu.addItem(1, new CustomItemStack(ChestMenuUtils.getBackButton(player),
                "",
                "§fClick: §7Back to previous level",
                "§fShift + Click: §7Back to main menu"));
        menu.addMenuClickHandler(1, (p, slot, item, clickAction) -> {
            if (clickAction.isShiftClicked()) {
                // Shift + Click: Return to the Slimefun guide main page(使用原版实现, 不经过 JEG 等替换引导)
                new SurvivalSlimefunGuide().openMainMenu(profile, profile.getGuideHistory().getMainMenuPage());
            } else {
                backAction.run(); // 确定性Back上级
            }
            return false;
        });
    }

    private static void addBack(ChestMenu menu, PlayerProfile profile, SlimefunGuideMode mode) {
        Player player = profile.getPlayer();
        GuideHistory history = profile.getGuideHistory();
        // 使用原版 SurvivalSlimefunGuide, 不经过 JEG 等替换引导, Keep back-button behavior consistent with the original
        SlimefunGuideImplementation guide = new SurvivalSlimefunGuide();

        // 完整复刻原版粘液书Back逻辑: 历史>1 时BackPrevious Page(Shift Return to main menu), Otherwise, return directly to the main menu
        if (mode == SlimefunGuideMode.SURVIVAL_MODE && history.size() > 1) {
            menu.addItem(1, new CustomItemStack(ChestMenuUtils.getBackButton(player),
                    "",
                    "§fClick: §7BackPrevious Page",
                    "§fShift + Click: §7Back to main menu"));
            menu.addMenuClickHandler(1, (p, slot, item, clickAction) -> {
                if (clickAction.isShiftClicked()) {
                    guide.openMainMenu(profile, history.getMainMenuPage());
                } else {
                    history.goBack(guide);
                }
                return false;
            });
        } else {
            menu.addItem(1, new CustomItemStack(ChestMenuUtils.getBackButton(player),
                    "",
                    "§7Back to main menu"));
            menu.addMenuClickHandler(1, (p, slot, item, clickAction) -> {
                guide.openMainMenu(profile, history.getMainMenuPage());
                return false;
            });
        }
    }

    private static ChestMenu createBaseMenu(Player player, PlayerProfile profile, SlimefunGuideMode mode) {
        String title = Slimefun.getLocalization().getMessage(player, "guide.title.main");
        ChestMenu menu = new ChestMenu(title);
        menu.setEmptySlotsClickable(false);
        menu.addMenuOpeningHandler(p -> p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f));
        SlimefunGuideImplementation guide = Slimefun.getRegistry().getSlimefunGuide(mode);
        if (guide instanceof SurvivalSlimefunGuide survival) {
            survival.createHeader(player, profile, menu);
        }
        return menu;
    }

    private static void renderGroupButtons(ChestMenu menu, Player player, PlayerProfile profile, SlimefunGuideMode mode,
                                           List<ItemGroup> groups, int page,
                                           GroupClickHandler clickHandler,
                                           PageReopener pageReopener) {
        int pages = Math.max(1, (groups.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        page = Math.max(1, Math.min(page, pages));
        int start = (page - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, groups.size());
        final int currentPage = page;

        int slot = FIRST_SLOT;
        for (int i = start; i < end; i++) {
            ItemGroup sub = groups.get(i);
            int currentSlot = slot;
            menu.addItem(currentSlot, sub.getItem(player));
            menu.addMenuClickHandler(currentSlot, (p, s, item, action) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                clickHandler.onClick(p, profile, mode, sub);
                return false;
            });
            slot++;
        }

        if (page > 1) {
            menu.addItem(46, ChestMenuUtils.getPreviousButton(player, page, pages));
            menu.addMenuClickHandler(46, (p, s, item, action) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                pageReopener.reopen(p, profile, mode, currentPage - 1);
                return false;
            });
        }
        if (page < pages) {
            menu.addItem(52, ChestMenuUtils.getNextButton(player, page, pages));
            menu.addMenuClickHandler(52, (p, s, item, action) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                pageReopener.reopen(p, profile, mode, currentPage + 1);
                return false;
            });
        }
    }

    /** 复刻原生 getVisibleItemGroups:FlexItemGroup 用三参 isVisible,普通Group只看组本身是否隐藏 */
    private static List<ItemGroup> visibleGroups(List<ItemGroup> groups, Player player, PlayerProfile profile, SlimefunGuideMode mode) {
        List<ItemGroup> result = new ArrayList<>();
        for (ItemGroup group : groups) {
            if (group == null) continue;
            if (group instanceof FlexItemGroup flex) {
                if (flex.isVisible(player, profile, mode)) {
                    result.add(group);
                }
            } else if (group instanceof SubItemGroup sub) {
                // 原生Grouppage同款判断:isVisibleInNested 直调父类 ItemGroup.isVisible,不受 SubItemGroup 覆写影响
                if (sub.isVisibleInNested(player)) {
                    result.add(group);
                }
            } else {
                if (!group.isHidden(player)) {
                    result.add(group);
                }
            }
        }
        return result;
    }

    private static void openMenu(ChestMenu menu, Player player) {
        menu.open(player);
    }

    @FunctionalInterface
    private interface GroupClickHandler {
        void onClick(Player player, PlayerProfile profile, SlimefunGuideMode mode, ItemGroup group);
    }

    @FunctionalInterface
    private interface PageReopener {
        void reopen(Player player, PlayerProfile profile, SlimefunGuideMode mode, int page);
    }
}
package io.Yomicer.magicExpansion.items.misc;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.Yomicer.magicExpansion.items.abstracts.AbstractContainer;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.Random;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getRandomGradientName;
import static io.Yomicer.magicExpansion.utils.SameItemJudge.itemFromBase64;
import static io.Yomicer.magicExpansion.utils.SameItemJudge.itemToBase64;

/**
 * Page Chest
 * The player UI directly uses Slimefun BlockMenu("items"), is the same one that Slimefun cargo works on inventory,
 * Cargo writes/takes are visible in real time, No copy, so no item duplication/desync problems
 * The current page data is in the BlockMenu, Other pages are stored in custom BlockStorage keys, they read/write each other when flipping pages
 * Shift+Right-click won't open the UI, can be used to place Slimefun cargo nodes
 */
public class PageChest extends AbstractContainer {

    public static final int TOTAL_PAGES = 5;
    public static final int CHEST_SIZE = 54;

    public static final String DATA_CURRENT_PAGE = "magicexpansion_pagechest_currentpage";
    private static final String DATA_PREFIX = "magicexpansion_pagechest_";

    private static final int[] STORAGE_SLOTS;

    // Page flip sound: random note block sound
    private static final Sound[] FLIP_SOUNDS = {
            Sound.BLOCK_NOTE_BLOCK_BANJO,
            Sound.BLOCK_NOTE_BLOCK_BELL,
            Sound.BLOCK_NOTE_BLOCK_BIT,
            Sound.BLOCK_NOTE_BLOCK_CHIME,
            Sound.BLOCK_NOTE_BLOCK_COW_BELL,
            Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO,
            Sound.BLOCK_NOTE_BLOCK_FLUTE,
            Sound.BLOCK_NOTE_BLOCK_GUITAR,
            Sound.BLOCK_NOTE_BLOCK_HARP,
            Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE,
            Sound.BLOCK_NOTE_BLOCK_PLING,
            Sound.BLOCK_NOTE_BLOCK_SNARE,
            Sound.BLOCK_NOTE_BLOCK_XYLOPHONE
    };

    static {
        STORAGE_SLOTS = new int[CHEST_SIZE];
        for (int i = 0; i < CHEST_SIZE; i++) {
            STORAGE_SLOTS[i] = i;
        }
    }

    public PageChest(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        // Intercept right-click interactions on placed Page Chests
        addItemHandler((BlockUseHandler) e -> {
            if (e.getHand() != EquipmentSlot.HAND) {
                return;
            }
            Block block = e.getClickedBlock().orElse(null);
            if (block == null) {
                return;
            }
            if (!(BlockStorage.check(block) instanceof PageChest)) {
                // What was clicked isn't a placed Page Chest, Let it through
                return;
            }
            // Block the vanilla chest UI
            e.setUseBlock(Event.Result.DENY);
            e.setUseItem(Event.Result.DENY);
            if (e.getPlayer().isSneaking()) {
                // Shift+Right-click: Doesn't open the UI, so items like Slimefun cargo nodes can run their binding logic normally
                return;
            }
            open(e.getPlayer(), block.getLocation());
        });
    }

    @Override
    protected void setupMenu(@Nonnull BlockMenuPreset preset) {
        // BlockMenu is the cargo container, The UI is opened directly by PageChest.open
        preset.setSize(CHEST_SIZE);
    }

    @Override
    protected @Nonnull int[] getInputSlots() {
        return STORAGE_SLOTS;
    }

    @Override
    protected @Nonnull int[] getOutputSlots() {
        return STORAGE_SLOTS;
    }

    @Override
    protected void onPlace(@Nonnull BlockPlaceEvent e, @Nonnull Block b) {
        BlockStorage.addBlockInfo(b.getLocation(), DATA_CURRENT_PAGE, "1");
    }

    @Override
    protected void onBreak(@Nonnull BlockBreakEvent e, @Nonnull BlockMenu menu, @Nonnull Location location) {
        dropAllPages(location, menu);
    }

    // ===================== Open the UI =====================

    // Open directly BlockMenu("items"), Shares the same one with cargo inventory, naturally in sync
    public static void open(@Nonnull Player player, @Nonnull Location location) {
        BlockMenu menu = StorageCacheUtils.getMenu(location);
        if (menu == null) {
            return;
        }
        menu.open(player);
        player.getOpenInventory().setTitle(title(currentPage(location)));
        playAnimation(player, location, true);
    }

    private static String title(int page) {
        return getRandomGradientName("Warp Chest · Page " + page + "/" + TOTAL_PAGES + "  (Left-click: previous page | Right-click: next page)");
    }

    // ===================== Page number =====================

    public static int currentPage(@Nonnull Location location) {
        String data = BlockStorage.getLocationInfo(location, DATA_CURRENT_PAGE);
        if (data != null && !data.isEmpty()) {
            try {
                return Math.max(1, Math.min(TOTAL_PAGES, Integer.parseInt(data)));
            } catch (NumberFormatException ignored) {
            }
        }
        return 1;
    }

    // ===================== Flip page =====================

    public static void flip(@Nonnull Player player, @Nonnull Location location, int direction) {
        BlockMenu menu = StorageCacheUtils.getMenu(location);
        if (menu == null) {
            return;
        }
        int page = currentPage(location);
        int next = page + direction;
        if (next < 1) {
            next = TOTAL_PAGES;
        } else if (next > TOTAL_PAGES) {
            next = 1;
        }

        // Old page(player changes already took effect directly on BlockMenu) write back to the save
        savePage(location, page, menu.toInventory().getContents());
        BlockStorage.addBlockInfo(location, DATA_CURRENT_PAGE, String.valueOf(next));

        // Load the new page into BlockMenu, The UI and cargo switch to the new page at the same time
        menu.toInventory().setContents(loadPage(location, next));
        menu.markDirty();
        player.getOpenInventory().setTitle(title(next));

        // Page flip sound: random note block sound
        Sound flipSound = FLIP_SOUNDS[new Random().nextInt(FLIP_SOUNDS.length)];
        location.getWorld().playSound(location, flipSound, 0.5f, 1.0f);
    }

    // ===================== Data read/write =====================

    private static String slotKey(int page, int slot) {
        return DATA_PREFIX + page + "_" + slot;
    }

    public static ItemStack[] loadPage(@Nonnull Location location, int page) {
        ItemStack[] contents = new ItemStack[CHEST_SIZE];
        for (int i = 0; i < CHEST_SIZE; i++) {
            String data = BlockStorage.getLocationInfo(location, slotKey(page, i));
            contents[i] = (data == null || data.isEmpty()) ? null : itemFromBase64(data);
        }
        return contents;
    }

    public static void savePage(@Nonnull Location location, int page, @Nonnull ItemStack[] contents) {
        for (int i = 0; i < CHEST_SIZE; i++) {
            ItemStack item = (i < contents.length) ? contents[i] : null;
            if (item == null || item.getType() == Material.AIR) {
                BlockStorage.addBlockInfo(location, slotKey(page, i), "");
            } else {
                BlockStorage.addBlockInfo(location, slotKey(page, i), itemToBase64(item));
            }
        }
    }

    // ===================== Drops when broken =====================

    public static void dropAllPages(@Nonnull Location location, @Nonnull BlockMenu menu) {
        // Current page: BlockMenu("items") data in
        ItemStack[] current = (menu != null) ? menu.toInventory().getContents() : new ItemStack[CHEST_SIZE];
        for (ItemStack item : current) {
            if (item != null && item.getType() != Material.AIR) {
                drop(location, item);
            }
        }

        // Other pages: custom key save
        int currentPage = currentPage(location);
        for (int p = 1; p <= TOTAL_PAGES; p++) {
            if (p == currentPage) {
                continue;
            }
            for (ItemStack item : loadPage(location, p)) {
                if (item != null && item.getType() != Material.AIR) {
                    drop(location, item);
                }
            }
        }
    }

    private static void drop(@Nonnull Location location, @Nonnull ItemStack item) {
        location.getWorld().dropItemNaturally(location, item);
    }

    // ===================== Animation =====================

    public static void playAnimation(@Nonnull Player player, @Nonnull Location location, boolean open) {
        BlockState state = location.getBlock().getState();
        if (state instanceof Chest chest) {
            if (open) {
                chest.open();
            } else {
                chest.close();
            }
        }
        player.getWorld().playSound(location, open ? Sound.BLOCK_CHEST_OPEN : Sound.BLOCK_CHEST_CLOSE, 0.5f, 1.0f);
    }
}
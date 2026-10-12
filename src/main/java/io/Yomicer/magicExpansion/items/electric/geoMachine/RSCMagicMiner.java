package io.Yomicer.magicExpansion.items.electric.geoMachine;

import com.xzavier0722.mc.plugin.slimefun4.storage.callback.IAsyncReadCallback;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunChunkData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.Yomicer.magicExpansion.MagicExpansion;
import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.HologramOwner;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.operations.CraftingOperation;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static io.Yomicer.magicExpansion.core.MagicExpansionItems.*;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;

public class RSCMagicMiner extends AContainer implements RecipeDisplayItem , HologramOwner{

    private static final String HOLOGRAM_ENABLED_KEY = "hologram_enabled";
    private static final ItemStack TOGGLE_ON = new CustomItemStack(Material.GREEN_DYE, "&aHologram text: &fOn", "&7Click to turn off");
    private static final ItemStack TOGGLE_OFF = new CustomItemStack(Material.GRAY_DYE, "&cHologram text: &fOff", "&7Click to turn on");

    private static final int[] INPUT_SLOTS = new int[] { 0,1,2,3, 9,10,11,12, 18,19,20,21, 27,28,29,30, 36,37,38,39, 45,46,47,48  };
    private static final int[] OUTPUT_SLOTS = new int[] { 5,6,7,8, 14,15,16,17, 23,24,25,26, 32,33,34,35, 41,42,43,44, 50,51,52,53 };

    private static final int[] INPUT_BORDER_SLOTS = new int[] { 13 };
    private static final int[] OUTPUT_BORDER_SLOTS = new int[] { 40 };
    private static final int[] BACKGROUND_SLOTS = new int[] { 49 };

    private static final ItemStack PROGRESS_ITEM = new ItemStack(Material.SOUL_LANTERN);
    private static final ItemStack PROGRESS_STACK = new CustomItemStack(Material.SOUL_CAMPFIRE, getGradientName("Info"), getGradientName("Type: Five Elements Resource Collector"), getGradientName("Addon: Magic"));

    private final MachineProcessor<CraftingOperation> processor = new MachineProcessor<>(this);


    private final GEOResource redstone;
    private final GEOResource dust;
    private final GEOResource soul;

    private final ItemStack redstone_input = new ItemStack(Material.REDSTONE);
    private final ItemStack dust_input = new ItemStack(Material.GUNPOWDER);
    private final ItemStack soul_input = new ItemStack(Material.SOUL_SAND);


    @ParametersAreNonnullByDefault
    public RSCMagicMiner(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        processor.setProgressBar(getProgressBar());

//        redstone = Slimefun.getRegistry()
//                .getGEOResources()
//                .get(new NamespacedKey("rykenslimefuncustomizer", "magic_redstone"))
//                .orElse(null);
//        dust = Slimefun.getRegistry()
//                .getGEOResources()
//                .get(new NamespacedKey("rykenslimefuncustomizer", "magic_cosmic_dust"))
//                .orElse(null);
//        soul = Slimefun.getRegistry()
//                .getGEOResources()
//                .get(new NamespacedKey("rykenslimefuncustomizer", "magic_soul"))
//                .orElse(null);

        redstone = Slimefun.getRegistry()
                .getGEOResources()
                .get(new NamespacedKey(MagicExpansion.getInstance(), "magic_expansion_rsc_magic_redstone"))
                .orElse(null);
        dust = Slimefun.getRegistry()
                .getGEOResources()
                .get(new NamespacedKey(MagicExpansion.getInstance(), "magic_expansion_rsc_magic_cosmic_dust"))
                .orElse(null);
        soul = Slimefun.getRegistry()
                .getGEOResources()
                .get(new NamespacedKey(MagicExpansion.getInstance(), "magic_expansion_rsc_magic_soul"))
                .orElse(null);

        addItemHandler(onBlockPlace(), onBlockBreak());


        new BlockMenuPreset(getId(), getInventoryTitle()) {

            @Override
            public void init() {
                setupMenu(this);
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                if (!(p.hasPermission("slimefun.inventory.bypass")
                        || Slimefun.getProtectionManager()
                        .hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK))) {
                    return false;
                }

                return true;
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

    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem sf, SlimefunBlockData data) {
                RSCMagicMiner.this.tick(b);
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
                SlimefunBlockData data = StorageCacheUtils.getBlock(e.getBlock().getLocation());
                if (data != null) {
                    data.setData(HOLOGRAM_ENABLED_KEY, "true"); // Initialize explicitly
                }
                updateHologram(e.getBlock(), "&7Idle...");
            }
        };
    }

    @Nonnull
    protected BlockBreakHandler onBlockBreak() {
        return new SimpleBlockBreakHandler() {
            public void onBlockBreak(Block b) {
                removeHologram(b);
                BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());
                if (inv != null) {
                    inv.dropItems(b.getLocation(), getInputSlots());
                    inv.dropItems(b.getLocation(), getOutputSlots());
                }

                processor.endOperation(b);
            }
        };
    }


    // ==================== B1 Performance fix: chunk resource data cache ====================
    // The old version ran an async getChunkDataAsync query every tick. Now results are cached by "world UUID:chunk key"
    // The "does this chunk have data" result is only re-checked about every 100 ticks (5 seconds), which cuts IO and scheduling cost a lot.
    private static final long CHUNK_CACHE_TTL_MS = 5000; // Cache refresh interval ≈ 100 ticks
    // Uses ConcurrentHashMap: the async tick thread reads it and the main-thread callback writes it
    private final Map<String, ChunkCache> chunkDataCache = new java.util.concurrent.ConcurrentHashMap<>();

    // Chunk data cache entry: has data or not + expiry time
    private static final class ChunkCache {
        final boolean hasData;
        final long expireAt;

        ChunkCache(boolean hasData, long expireAt) {
            this.hasData = hasData;
            this.expireAt = expireAt;
        }
    }

    protected void tick(Block b) {
        // B1: Check the cache first. If it is a hit and not expired, skip the async query
        String cacheKey = b.getWorld().getUID() + ":" + b.getChunk().getChunkKey();
        long now = System.currentTimeMillis();
        ChunkCache cached = chunkDataCache.get(cacheKey);
        if (cached != null && now < cached.expireAt) {
            if (cached.hasData) {
                // B2 fix: The cache-hit path also only checks and marks; startTick is scheduled back onto the main thread
                Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> startTick(b));
            } else {
                updateHologram(b, "&4Scan the terrain first!");
            }
            return;
        }

        Slimefun.getDatabaseManager()
                .getBlockDataController()
                .getChunkDataAsync(b.getChunk(), new IAsyncReadCallback<>() {
                    @Override
                    public void onResult(SlimefunChunkData result) {
                        // B2 fix: The async callback only checks and marks the cache; it no longer touches BlockMenu directly
                        boolean hasData = !result.getAllData().isEmpty();
                        chunkDataCache.put(cacheKey, new ChunkCache(hasData, System.currentTimeMillis() + CHUNK_CACHE_TTL_MS));
                        if (!hasData) {
                            updateHologram(b, "&4Scan the terrain first!");
                        } else {
                            // B2 fix: startTick, which uses BlockMenu, is scheduled back onto the main thread with runTask
                            Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> startTick(b));
                        }
                    }
                });
    }

    private void startTick(Block b){
        BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());
        // B3 fix: The menu may not be available while the chunk is still loading, so null-check it before use
        if (inv == null) {
            return;
        }
        CraftingOperation currentOperation = processor.getOperation(b);

        if (currentOperation != null) {
            if (takeCharge(b.getLocation())) {

                if (!currentOperation.isFinished()) {
                    processor.updateProgressBar(inv, 31, currentOperation);
                    currentOperation.addProgress(1);
                } else {
                    inv.replaceExistingItem(31, PROGRESS_ITEM);

                    for (ItemStack output : currentOperation.getResults()) {
                        inv.pushItem(output.clone(), getOutputSlots());
                    }

                    processor.endOperation(b);
                }
            }
        }else {
            MachineRecipe next = findNextRecipe(inv);

            if (next != null) {
                currentOperation = new CraftingOperation(next);
                processor.startOperation(b, currentOperation);

                // Fixes #3534 - Update indicator immediately
                processor.updateProgressBar(inv, 31, currentOperation);
            }
        }
    }

    @Override
    protected MachineRecipe findNextRecipe(BlockMenu inv) {

        MachineRecipe recipe;

        recipe = tryProcessEarthLikeRecipe(inv, redstone_input, RSC_MAGIC_REDSTONE, redstone, 5);
        if (recipe != null) {
            return recipe;
        }
        recipe = tryProcessEarthLikeRecipe(inv, soul_input, RSC_MAGIC_SOUL, soul, 5);
        if (recipe != null) {
            return recipe;
        }
        recipe = tryProcessEarthLikeRecipe(inv, dust_input, RSC_MAGIC_COSMIC_DUST, dust, 5);
        return recipe;

    }


    private MachineRecipe tryProcessEarthLikeRecipe(BlockMenu inv, ItemStack inputItem, SlimefunItemStack outputItem, GEOResource resourceType, int seconds) {
        Block b = inv.getBlock();

        // Check that the output slots can hold the result
        if (!inv.fits(outputItem, getOutputSlots())) {
            return null;
        }

        for (int slot : getInputSlots()) {
            ItemStack itemInSlot = inv.getItemInSlot(slot);

            // Check that the input slots have a matching item
            if (SlimefunUtils.isItemSimilar(itemInSlot, inputItem, true, false)) {
                OptionalInt supplies = Slimefun.getGPSNetwork()
                        .getResourceManager()
                        .getSupplies(resourceType, b.getWorld(), b.getX() >> 4, b.getZ() >> 4);


                if (supplies.isPresent() && supplies.getAsInt() > 0) {
                    // Create the recipe and consume the input items
                    MachineRecipe recipe = new MachineRecipe(seconds, new ItemStack[] {inputItem}, new ItemStack[] {outputItem});
                    inv.consumeItem(slot);

                    // Subtract the resources
                    Slimefun.getGPSNetwork()
                            .getResourceManager()
                            .setSupplies(resourceType, b.getWorld(), b.getX() >> 4, b.getZ() >> 4, supplies.getAsInt() - 1);
                    this.updateHologram(b, "&7Mining: &r" + resourceType.getName());

                    return recipe;
                } else {
                    // Not enough resources. Move things like empty buckets to the output slot so it does not retry over and over (prevents lag)
                    // B4 fix: Before pushItem, use fits to check that the output slot has room. If not, put the materials back in the original input slot and never discard them
                    ItemStack item = itemInSlot.clone();
                    if (inv.fits(item, getOutputSlots())) {
                        inv.replaceExistingItem(slot, null);
                        inv.pushItem(item, getOutputSlots());
                    }
                    this.updateHologram(b, resourceType.getName()+ "&7 Mining done");
                    return null;
                }
            }

        }

        return null; // No matching item, or not enough resources
    }

    @Override
    public void updateHologram(@NotNull Block block, @NotNull String text) {
        SlimefunBlockData data = StorageCacheUtils.getBlock(block.getLocation());
        if (data != null && !isHologramEnabled(data)) {
            return;
        }
        HologramOwner.super.updateHologram(block, text);
    }


    protected void setupMenu(BlockMenuPreset preset) {

        preset.drawBackground(new CustomItemStack(Material.PINK_STAINED_GLASS_PANE, " "), BACKGROUND_SLOTS);
        preset.drawBackground(new CustomItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " "), INPUT_BORDER_SLOTS);
        preset.drawBackground(new CustomItemStack(Material.LIME_STAINED_GLASS_PANE, " "), OUTPUT_BORDER_SLOTS);

        preset.addItem(getProgressSlot(), new CustomItemStack(Material.PINK_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());

        preset.addItem(22, PROGRESS_STACK, ChestMenuUtils.getEmptyClickHandler());

        // === New: toggle button in slot 4 ===
        preset.addItem(4, TOGGLE_ON,
                new ChestMenu.AdvancedMenuClickHandler() {
                    @Override
                    public boolean onClick(InventoryClickEvent e, Player p, int slot, ItemStack cursor, ClickAction action) {
                        if (!(e.getInventory().getHolder() instanceof BlockMenu menu)) return false;
                        Block b = menu.getBlock();
                        SlimefunBlockData data = StorageCacheUtils.getBlock(b.getLocation());
                        if (data == null) return false;

                        boolean enabled = "true".equals(data.getData(HOLOGRAM_ENABLED_KEY));
                        boolean now = !enabled;
                        data.setData(HOLOGRAM_ENABLED_KEY, String.valueOf(now));

                        // Update the icon
                        menu.replaceExistingItem(4,
                                now ? TOGGLE_ON
                                        : TOGGLE_OFF
                        );

                        // Control the hologram display
                        if (now) {
                        } else {
                            removeHologram(b);
                        }

                        return false;
                    }

                    @Override
                    public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
                        return false;
                    }
                }
        );

        for (int slot : getOutputSlots()) {
            preset.addMenuClickHandler(slot,new ChestMenu.AdvancedMenuClickHandler() {
                @Override
                public boolean onClick(InventoryClickEvent e, Player p, int slot, ItemStack cursor, ClickAction action) {
                    return cursor.getType().isAir();
                }

                @Override
                public boolean onClick(Player p, int slot, ItemStack item, ClickAction action) {
                    return false;
                }
            });
        }
    }

    private boolean isHologramEnabled(SlimefunBlockData data) {
        String value = data.getData(HOLOGRAM_ENABLED_KEY);
        return value == null || Boolean.parseBoolean(value); // On by default
    }


    @Override
    public List<ItemStack> getDisplayRecipes() {
        List<ItemStack> display = new ArrayList<>();
        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientName("Materials⇨")));
        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientName("Output (mining time: 5 sec)⇨")));
        display.add(redstone_input);
        display.add(RSC_MAGIC_REDSTONE);
        display.add(soul_input);
        display.add(RSC_MAGIC_SOUL);
        display.add(dust_input);
        display.add(RSC_MAGIC_COSMIC_DUST);





        return display;
    }



    protected int getProgressSlot() {
        return 31;
    }

    @Override
    public int[] getInputSlots() {
        return INPUT_SLOTS;
    }

    @Override
    public int[] getOutputSlots() {
        return OUTPUT_SLOTS;
    }


    @Override
    public ItemStack getProgressBar() {
        return PROGRESS_ITEM;
    }


    @Override
    public @NotNull String getMachineIdentifier() {
        // B5 fix: This used to return the same identifier as FiveElementsMiner, so the two overwrote each other's progress in MachineProcessor;
        // Changed to an identifier specific to this machine
        return "MAGIC_EXPANSION_RSC_MAGIC_MINER";
    }
}
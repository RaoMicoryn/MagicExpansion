package io.Yomicer.magicExpansion.items.generators;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.core.MagicExpansionItems;
import io.Yomicer.magicExpansion.items.abstracts.MenuBlock;
import io.Yomicer.magicExpansion.items.misc.CargoCoreMore;
import io.Yomicer.magicExpansion.items.misc.CargoFragment;
import io.Yomicer.magicExpansion.items.misc.fish.Fish;
import io.Yomicer.magicExpansion.items.misc.fish.FishKeys;
import io.Yomicer.magicExpansion.items.tools.VoidTouch;
import io.Yomicer.magicExpansion.utils.CustomHeadUtils.CustomHead;
import io.Yomicer.magicExpansion.utils.NetworkStorage;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.Yomicer.magicExpansion.items.generators.FishOutputMachine.FISH_OUTPUT_MAP;
import static io.Yomicer.magicExpansion.items.misc.fish.Fish.WeightRarity;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getRandomGradientName;
import static io.Yomicer.magicExpansion.utils.SameItemJudge.itemFromBase64;
import static io.Yomicer.magicExpansion.utils.Utils.doGlow;

public class FishOutputMachineStack extends MenuBlock implements EnergyNetComponent, RecipeDisplayItem {

    private final int Capacity;
    public static final int ENERGY_CONSUMPTION = 26000;
    private static final int FishSlot = 49;
    private static final int VoidTouchSlot = 50;
    private static final NamespacedKey KEY_X = new NamespacedKey(MagicExpansion.getInstance(), "touch_x");
    private static final NamespacedKey KEY_Y = new NamespacedKey(MagicExpansion.getInstance(), "touch_y");
    private static final NamespacedKey KEY_Z = new NamespacedKey(MagicExpansion.getInstance(), "touch_z");
    private static final NamespacedKey KEY_WORLD = new NamespacedKey(MagicExpansion.getInstance(), "touch_world");
    private static final NamespacedKey KEY_ITEM = new NamespacedKey(MagicExpansion.getInstance(), "cargo_item_json");
    private static final NamespacedKey KEY_AMOUNT = new NamespacedKey(MagicExpansion.getInstance(), "cargo_amount");
    private static final int [] MachineSlots = new int [] {
            0,1,2,3,4,5,6,7,8,
            9,10,11,12,13,14,15,16,17,
            18,19,20,21,22,23,24,25,26,
            27,28,29,30,31,32,33,34,35,
            36,37,38, 42,43,44,
            45,46,47, 51,52,53
    };

//    // 1. Map every fish type to its output item (one place, easy to add more)
//    private final Map<String, ItemStack> FISH_OUTPUT_MAP = new LinkedHashMap<>() {{
//        put("CopperDustFish",     SlimefunItems.COPPER_DUST);
//        put("GoldDustFish",       SlimefunItems.GOLD_DUST);
//        put("IronDustFish",       SlimefunItems.IRON_DUST);
//        put("TinDustFish",        SlimefunItems.TIN_DUST);
//        put("SilverDustFish",     SlimefunItems.SILVER_DUST);
//        put("AluminumDustFish",   SlimefunItems.ALUMINUM_DUST);
//        put("LeadDustFish",       SlimefunItems.LEAD_DUST);
//        put("ZincDustFish",       SlimefunItems.ZINC_DUST);
//        put("MagnesiumDustFish",  SlimefunItems.MAGNESIUM_DUST);
//        // 🔶 Coal Crystal Fish → Coal
//        put("CoalFish", new ItemStack(Material.COAL));
//        // 💚 Emerald Fish → Emerald
//        put("EmeraldFish", new ItemStack(Material.EMERALD));
//        // 🔷 Lapis Spirit Fish → Lapis Lazuli
//        put("LapisFish", new ItemStack(Material.LAPIS_LAZULI));
//        // 💎 Diamond Shine Fish → Diamond
//        put("DiamondFish", new ItemStack(Material.DIAMOND));
//        // 🔴 Flame Crystal Fish → Nether Quartz
//        put("QuartzFish", new ItemStack(Material.QUARTZ));
//        // 🟣 Tremor Fish → Amethyst Shard
//        put("AmethystFish", new ItemStack(Material.AMETHYST_SHARD));
//        // ⚫ Iron Core Fish → Iron Ingot
//        put("IronFish", new ItemStack(Material.IRON_INGOT));
//        // 🟡 Gold Core Fish → Gold Ingot
//        put("GoldFish", new ItemStack(Material.GOLD_INGOT));
//        // 🟠 Copper Vein Fish → Copper Ingot
//        put("CopperFish", new ItemStack(Material.COPPER_INGOT));
//        // 🟠 Red Vein Fish → Redstone
//        put("RedstoneFish", new ItemStack(Material.REDSTONE));
//        // ⚔️ Netherforged Fish → Netherite Ingot
//        put("NetheriteFish", new ItemStack(Material.NETHERITE_INGOT));
//        // ⚔️ Lantern Fish → Glowstone Dust
//        put("GlowStoneDustFish", new ItemStack(Material.GLOWSTONE_DUST));
//        // ⚔️ Plastic Spirit Fish → Plastic Sheet
//        put("ShuLingYu", SlimefunItems.PLASTIC_SHEET);
//        // ⚔️ Uranium Core Fish → U
//        put("UraniumFish", SlimefunItems.URANIUM);
//        // ⚔️ Oil Rock Fish → Crude Oil Bucket
//        put("OilRockFish", SlimefunItems.OIL_BUCKET);
//        // ⚔️ Bubble Crystal Fish → Foam Ingot
//        put("FoamCrystalFish", SlimefunItems.BLISTERING_INGOT_3);
//        // ⚔️ Obsidian Fish → Black Diamond
//        put("BlackDiamondFish", SlimefunItems.CARBONADO);
//        // ⚔️ Curse Spirit Fish → Bottle o' Enchanting
//        put("EnchantedBottleFish", new ItemStack(Material.EXPERIENCE_BOTTLE));
//        // ⚔️ Crystal Scale Fish → Sulfate
//        put("SulfateFish", SlimefunItems.SULFATE);
//        // ⚔️ Acid Crystal Fish → Silicon
//        put("SiliconFish", SlimefunItems.SILICON);
//
//        // [Alloy Spirit Fish]makes: Reinforced Alloy Ingot
//        put("ReinforcedAlloyFish", SlimefunItems.REINFORCED_ALLOY_INGOT);
//
//        // [Hardened Spirit Fish]makes: Hardened Metal
//        put("HardenedMetalFish", SlimefunItems.HARDENED_METAL_INGOT);
//
//        // [Damascus Spirit Fish]makes: Damascus Steel Ingot
//        put("DamascusSoulFish", SlimefunItems.DAMASCUS_STEEL_INGOT);
//
//        // [Steel Soul Fish]makes: Steel Ingot
//        put("SteelSoulFish", SlimefunItems.STEEL_INGOT);
//
//        // [Bronze Shadow Fish]makes: Bronze Ingot
//        put("BronzeAncientFish", SlimefunItems.BRONZE_INGOT);
//
//        // [Duralumin Sky Fish]makes: Duralumin Ingot
//        put("HardlightAluFish", SlimefunItems.DURALUMIN_INGOT);
//
//        // [Silver Copper Spirit Fish]makes: Silver Copper Alloy Ingot
//        put("SilverCopperFish", SlimefunItems.BILLON_INGOT);
//
//        // [Brass Chime Fish]makes: Brass Ingot
//        put("BrassResonanceFish", SlimefunItems.BRASS_INGOT);
//
//        // [Aluminum Brass Spirit Fish]makes: Aluminum Brass Ingot
//        put("AluminumBrassFish", SlimefunItems.ALUMINUM_BRASS_INGOT);
//
//        // [Aluminum Bronze Spirit Fish]makes: Aluminum Bronze Ingot
//        put("AluminumBronzeFish", SlimefunItems.ALUMINUM_BRONZE_INGOT);
//
//        // [Corinthian Bronze Spirit Fish]makes: Corinthian Bronze Ingot
//        put("CorinthianBronzeFish", SlimefunItems.CORINTHIAN_BRONZE_INGOT);
//
//        // [Solder Spirit Fish]makes: Solder Ingot
//        put("SolderFlowFish", SlimefunItems.SOLDER_INGOT);
//
//        // [Nickel Soul Fish]makes: Nickel Ingot
//        put("NickelSpiritFish", SlimefunItems.NICKEL_INGOT);
//
//        // [Cobalt Flame Fish]makes: Cobalt Ingot
//        put("CobaltFlameFish", SlimefunItems.COBALT_INGOT);
//
//        // [Ferrosilicon Spirit Fish]makes: Ferrosilicon
//        put("SiliconIronFish", SlimefunItems.FERROSILICON);
//
//        // [Carbon Soul Fish]makes: Carbon Block
//        put("CarbonSoulFish", SlimefunItems.CARBON_CHUNK);
//
//        // [Gilded Spirit Fish]makes: Gilded Iron Ingot
//        put("GildedIronFish", SlimefunItems.GILDED_IRON);
//
//        // [Redstone Alloy Spirit Fish]makes: Redstone Alloy Ingot
//        put("RedstoneAlloyFish", SlimefunItems.REDSTONE_ALLOY);
//
//        // [Neptunium Shadow Fish]makes: Neptunium
//        put("NeptuniumShadowFish", SlimefunItems.NEPTUNIUM);
//
//        // [Plutonium Heart Fish]makes: Plutonium
//        put("PlutoniumCoreFish", SlimefunItems.PLUTONIUM);
//
//
//
//
//
//    }};


    public FishOutputMachineStack(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, int Capacity) {
        super(category, item, recipeType, recipe);
        this.Capacity = Capacity;
    }

    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem sf, SlimefunBlockData data) {
                FishOutputMachineStack.this.tick(b);
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }
        });
    }
    protected void tick(Block block) {
        BlockMenu inv = StorageCacheUtils.getMenu(block.getLocation());
        // A3 fix: the menu may not be available right when the chunk loads (inv == null), so check for null at the start and return, to avoid an NPE in the slot loop below
        if (inv == null) {
            return;
        }

        if(inv != null && inv.hasViewer()) {
            if (getCharge(block.getLocation()) < getEnergyConsumption()) {
                inv.addItem(48, new CustomItemStack(doGlow(Material.LANTERN), getGradientName("⚡Machine stopped⚡"),
                                getGradientName("Check that there's enough power")),
                        (player1, slot, item, action) -> false);
                return;
            }
        }
        long totalNormalAmount = 0;
        long totalEasyAmount = 0;

        for (int slot : MachineSlots) {
            ItemStack item = inv.getItemInSlot(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }

            SlimefunItem sfItem = SlimefunItem.getByItem(item);
            if (sfItem == null) {
                continue;
            }

            int amount = item.getAmount(); // Get the stack amount of the item in this slot

            if (sfItem instanceof FishOutputMachine) {
                totalNormalAmount += amount;
            } else if (sfItem instanceof FishOutputMachineEasy) {
                totalEasyAmount += amount;
            }
            // === Step 2: Check if it's a CargoFragment and read the item inside ===
            if (sfItem instanceof CargoFragment){
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();

                if (pdc.has(KEY_ITEM, PersistentDataType.STRING) && pdc.has(KEY_AMOUNT, PersistentDataType.INTEGER)) {
                    String json = pdc.get(KEY_ITEM, PersistentDataType.STRING);
                    Integer fragmentAmount = pdc.get(KEY_AMOUNT, PersistentDataType.INTEGER);

                    if (json != null && fragmentAmount != null && fragmentAmount > 0) {
                        ItemStack originalItem = itemFromBase64(json);
                        if (originalItem != null) {
                            SlimefunItem originalSfItem = SlimefunItem.getByItem(originalItem);
                            if (originalSfItem != null) {
                                if (originalSfItem instanceof FishOutputMachine) {
                                    totalNormalAmount += fragmentAmount;
                                } else if (originalSfItem instanceof FishOutputMachineEasy) {
                                    totalEasyAmount += fragmentAmount;
                                }
                            }
                        }
                    }
                }
            }
            }
            // === Step 3: Check if it's a quantum storage item (counted when it holds a Vivarium / Easy Vivarium) ===
            if (NetworkStorage.isQuantumStorageItem(item)) {
                NetworkStorage.QuantumCache qc = NetworkStorage.getQuantumCache(item.getItemMeta());
                if (qc != null && qc.getItemStack() != null) {
                    SlimefunItem storedSf = SlimefunItem.getByItem(qc.getItemStack());
                    long storedAmount = qc.getAmountLong();
                    if (storedSf instanceof FishOutputMachine) {
                        totalNormalAmount += storedAmount;
                    } else if (storedSf instanceof FishOutputMachineEasy) {
                        totalEasyAmount += storedAmount;
                    }
                }
            }
        }


        ItemStack fish = null;
        ItemMeta meta = null;
        if (inv != null) {
            fish = inv.getItemInSlot(FishSlot);
            if (fish != null && !fish.getType().isAir()) {
                fish = fish.clone();
            }
        }
        if(fish != null) {
            meta = fish.getItemMeta();
        }
        ItemStack outItems = null;
        long outAmount = 0; // Total amount made this time (long, can go over the int limit, for bulk storing into external storage)
        if(meta != null) {

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            // Read PDC data
            String fishType = pdc.get(FishKeys.FISH_TYPE, PersistentDataType.STRING);
            Double weight = pdc.get(FishKeys.FISH_WEIGHT, PersistentDataType.DOUBLE);
            String weightRarityName = pdc.get(FishKeys.FISH_WEIGHT_RARITY, PersistentDataType.STRING);

            // Basic checks
            if (fishType == null || weight == null || weight == 0.0 || weightRarityName == null) {
                return ;
            }

            ItemStack baseOutputOrigin = FISH_OUTPUT_MAP.get(fishType);
            if(baseOutputOrigin != null) {
                ItemStack baseOutput = FISH_OUTPUT_MAP.get(fishType).clone();
                if (baseOutput != null) {
                    int multiplier = WeightRarity.getMultiplierByName(weightRarityName);
                    int multiplierEasy = 0;
                    if (multiplier == 1) multiplierEasy = 1;
                    if (multiplier == 7) multiplierEasy = 3;
                    if (multiplier == 15) multiplierEasy = 8;
                    if (multiplier == 9999) multiplierEasy = 888;

                    long amount = (long) (weight * multiplier);
                    long amountEasy = (long) (weight * multiplierEasy);
                    if (amount <= 0) amount = 1;
                    if (amountEasy <= 0) amountEasy = 1;
                    long finalAmount = safeMul(amount, totalNormalAmount);
                    long finalAmountEasy = safeMul(amountEasy, totalEasyAmount);
                    long finalAmountTotal = safeAdd(finalAmount, finalAmountEasy);
                    if (finalAmountTotal <= 0) {
                        finalAmountTotal = 1;
                    }

                    // Keep a template (amount 1) for item type matching and showing the name in the menu; the real amount is held separately by outAmount as a long,
                    // which gets past the ItemStack single int limit (1.20.5+ big-amount check), so "however many you put in, the machine stores all of it at that rate"
                    baseOutput.setAmount(1);
                    outItems = baseOutput;
                    outAmount = finalAmountTotal;

                }
            }

        }

        if (inv != null && inv.hasViewer() && outItems != null) {
            inv.addItem(48, new CustomItemStack(doGlow(Material.SOUL_LANTERN), getGradientName("⚡Machine running⚡"),
                            getGradientName("This machine has no normal output port"),
                            getGradientName("Current output: ")+ ItemStackHelper.getDisplayName(outItems),
                            ItemStackHelper.getDisplayName(MagicExpansionItems.FISH_VIVARIUM_EASY)+getRandomGradientName(": " + totalEasyAmount + " items"),
                            ItemStackHelper.getDisplayName(MagicExpansionItems.FISH_VIVARIUM)+getRandomGradientName(": " + totalNormalAmount + " items"),
                            getGradientName("Current speed")+ "§r" +getRandomGradientName(": " + outAmount + " items/tick")), // show the full amount as long, no longer limited by int
                    (player1, slot, item, action) -> false);
        } else {
            if (inv != null && inv.hasViewer()) {
                inv.addItem(48, new CustomItemStack(doGlow(Material.LANTERN), getGradientName("⚡Machine stopped⚡"),
                                getGradientName("Check that the fish type fits")),
                        (player1, slot, item, action) -> false);
            }
        }
        ItemStack VoidTouchSlotItem = inv.getItemInSlot(VoidTouchSlot);
        if (VoidTouchSlotItem != null && !VoidTouchSlotItem.getType().isAir() && outItems != null && outAmount > 0){
            SlimefunItem VoidTouchItem = SlimefunItem.getByItem(VoidTouchSlotItem);
            // ① Void Touch: bound to a Magic Storage Terminal / network quantum storage block
            if (VoidTouchItem != null && VoidTouchItem instanceof VoidTouch) {
                ItemMeta VoidTouchMeta = VoidTouchSlotItem.getItemMeta();
                if (VoidTouchMeta != null) {
                    PersistentDataContainer container = VoidTouchMeta.getPersistentDataContainer();
                    if (container.has(KEY_X, PersistentDataType.INTEGER) &&
                            container.has(KEY_Y, PersistentDataType.INTEGER) &&
                            container.has(KEY_Z, PersistentDataType.INTEGER) &&
                            container.has(KEY_WORLD, PersistentDataType.STRING)) {

                        String worldName = container.get(KEY_WORLD, PersistentDataType.STRING);
                        World world = Bukkit.getWorld(worldName);

                        if (world != null) {
                            int x = container.get(KEY_X, PersistentDataType.INTEGER);
                            int y = container.get(KEY_Y, PersistentDataType.INTEGER);
                            int z = container.get(KEY_Z, PersistentDataType.INTEGER);

                            Location targetLocation = new Location(world, x, y, z);
                            SlimefunItem sfItem = StorageCacheUtils.getSfItem(targetLocation);

                            if (sfItem instanceof CargoCoreMore) {
                                if (pushItemToCargoCoreLong(targetLocation, outItems, outAmount)){
                                    removeCharge(block.getLocation(), getEnergyConsumption());
                                }
                            } else if (NetworkStorage.isQuantumStorageBlock(sfItem)) {
                                // New: Void Touch bound to a network quantum storage block (bulk store with long, can go past the int limit)
                                long leftover = NetworkStorage.storeToQuantumStorageBlock(targetLocation, outItems, outAmount);
                                if (leftover < outAmount) {
                                    removeCharge(block.getLocation(), getEnergyConsumption());
                                }
                            } else {
                                // The bound target was destroyed or isn't a storage block: tell the player, don't take power
                                showDeadTargetWarning(inv);
                            }
                        }
                    }
                }
            }
            // ② Quantum storage item: store directly (bulk store with long, max value / overflow protection is handled inside NetworkStorage)
            else if (VoidTouchSlotItem.getAmount() == 1 && NetworkStorage.isQuantumStorageItem(VoidTouchSlotItem)) {
                long leftover = NetworkStorage.store(VoidTouchSlotItem, outItems, outAmount);
                if (leftover < outAmount) {
                    inv.replaceExistingItem(VoidTouchSlot, VoidTouchSlotItem);
                    removeCharge(block.getLocation(), getEnergyConsumption());
                }
            }
        }

    }

    // Message when the storage target bound to the Void Touch is no longer valid (only shown when a player has the menu open)
    private void showDeadTargetWarning(BlockMenu inv) {
        if (inv != null && inv.hasViewer()) {
            inv.replaceExistingItem(48, new CustomItemStack(doGlow(Material.BARRIER),
                    getGradientName("⚡Void Touch target invalid⚡"),
                    getGradientName("The bound storage block was destroyed"),
                    getGradientName("Bind again or remove the Void Touch")));
        }
    }

    // Multiplication overflow protection: returns Long.MAX_VALUE if the result goes over the long limit
    private static long safeMul(long a, long b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        if (a > Long.MAX_VALUE / b) {
            return Long.MAX_VALUE;
        }
        return a * b;
    }

    // Addition overflow protection: returns Long.MAX_VALUE if the result goes over the long limit
    private static long safeAdd(long a, long b) {
        if (b > Long.MAX_VALUE - a) {
            return Long.MAX_VALUE;
        }
        return a + b;
    }

    private boolean pushItemToCargoCore (Location loc, ItemStack item) {
        BlockMenu inv = StorageCacheUtils.getMenu(loc);
        if (inv != null) {
            SlimefunBlockData data = StorageCacheUtils.getBlock(loc);
            if (data == null) return false;
            if (hasStoredItem(data, item)) {
                storeItemToExistingSlot(data, item);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
    /**
     * long version: bulk write "item template + long amount" into the CargoCore's existing slots,
     * doesn't depend on ItemStack#getAmount() (int limit), can go past 2.1 billion (Ether Ecosystem Array case).
     */
    private boolean pushItemToCargoCoreLong (Location loc, ItemStack item, long amountToStore) {
        BlockMenu inv = StorageCacheUtils.getMenu(loc);
        if (inv != null) {
            SlimefunBlockData data = StorageCacheUtils.getBlock(loc);
            if (data == null) return false;
            if (hasStoredItem(data, item)) {
                // Write using the long version of the shared cache (reuses CargoCoreCache, overflow protected with long addition)
                FishOutputMachine.storeItemToExistingSlotCachedLong(data, item, amountToStore);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
    /**
     * Check if the CargoCore already has this item
     */
    private boolean hasStoredItem(SlimefunBlockData data, ItemStack item) {
        // A2 fix: now calls the shared cache version in FishOutputMachine, to avoid 1145 Base64 deserializations every tick
        return FishOutputMachine.hasStoredItemCached(data, item);
    }
    /**
     * Only store into slots that already have the item (don't make new slots)
     */
    private void storeItemToExistingSlot(SlimefunBlockData data, ItemStack item) {
        // A2 fix: now calls the shared cache version in FishOutputMachine (marks dirty after writing, cache is rebuilt next tick)
        FishOutputMachine.storeItemToExistingSlotCached(data, item);
    }

    // A5: per-tick output cap (same as FishOutputMachine, replaces the old Integer.MAX_VALUE)
    private static final int MAX_OUTPUT_PER_TICK = FishOutputMachine.MAX_OUTPUT_PER_TICK;

    protected void pushAllItems(BlockMenu menu, ItemStack item, int[] outputSlots) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return;
        }

        int totalAmount = item.getAmount();  // how many in total
        int perPush = 64;                    // push 64 each time

        // A4 fix: add up what pushItem really accepted, and stop pushing this tick if the output slots are full,
        // the rest is made again next tick, so rejected items are no longer silently destroyed
        while (totalAmount > 0) {
            int batch = Math.min(totalAmount, perPush);
            ItemStack toPush = item.clone();
            toPush.setAmount(batch);

            ItemStack leftover = menu.pushItem(toPush, outputSlots); // returns the part that didn't fit
            int accepted = batch - (leftover == null ? 0 : leftover.getAmount());
            totalAmount -= accepted;

            if (accepted <= 0) {
                break; // Output slots are full, the rest waits for next tick
            }
        }
    }

    public int getEnergyConsumption() {
        return ENERGY_CONSUMPTION;
    }



    @Override
    protected void setup(BlockMenuPreset var1) {
        var1.drawBackground(new CustomItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE,getGradientName("↓Magic fish slot↓")),new int[] {

                40
        });
        var1.drawBackground(new CustomItemStack(Material.CHAIN,getGradientName("Void Touch↓Network Quantum Storage")),new int[] {

                41
        });
        var1.drawBackground(new CustomItemStack(Material.END_CRYSTAL,getGradientName("Machine status")),new int[] {

                39, 48
        });

    }
    @Nonnull
    @Override
    protected int[] getInputSlots(DirtyChestMenu dirtyChestMenu, ItemStack itemStack) {
        return new int[]{49, 50,
                0,1,2,3,4,5,6,7,8,
                9,10,11,12,13,14,15,16,17,
                18,19,20,21,22,23,24,25,26,
                27,28,29,30,31,32,33,34,35,
                36,37,38, 42,43,44,
                45,46,47, 51,52,53};
    }

    @Override
    protected int[] getInputSlots() {
        return new int[]{
                49, 50,
                0,1,2,3,4,5,6,7,8,
                9,10,11,12,13,14,15,16,17,
                18,19,20,21,22,23,24,25,26,
                27,28,29,30,31,32,33,34,35,
                36,37,38, 42,43,44,
                45,46,47, 51,52,53
        };
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[]{

        };
    }

    @Override
    public @NotNull EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    public int getCapacity() {
        return Capacity;
    }

    @Override
    public @NotNull List<ItemStack> getDisplayRecipes() {

        List<ItemStack> display = new ArrayList<>();
        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientName("How to use⇩"),getGradientName("Please read carefully")));
        display.add(new CustomItemStack(Material.BOOK, getGradientName("How to use:"),getGradientName("Put a magic fish in the machine slot to start producing")
                ,getGradientName("The fish type decides what it produces")
                ,getGradientName("The fish weight decides how much it produces")));
        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientName("How to use⇩"),getGradientName("Please read carefully")));
        display.add(new CustomItemStack(Material.BOOK, getGradientName("Output formula:"),getGradientName("Each machine takes only one magic fish")
                ,getGradientName("Output = weight (rounded down) * magic fish rarity")
                ,getGradientName("Common/Rare/Super Rare/Fish King : 1/7/15/9999")));
        display.add(new CustomItemStack(CustomHead.getHead("26314d31b095e4d421760497be6a156f459d8c9957b7e6b1c12deb4e47860d71"),getGradientName("Supported fish ⇨")));
        display.add(new CustomItemStack(CustomHead.getHead("26314d31b095e4d421760497be6a156f459d8c9957b7e6b1c12deb4e47860d71"),getGradientName("Products ⇨")));

//        display.add(new CustomItemStack(Material.PUFFERFISH_BUCKET,CopperDustFish.getDisplayName(),getGradientName("Items per second: "+ " weight * magic fish rarity ")));
//        display.add(outputCopperDust);


        for (Map.Entry<String, ItemStack> entry : FISH_OUTPUT_MAP.entrySet()) {
            String fishTypeName = entry.getKey();
            Fish fish = Fish.fromString(fishTypeName);
            if (fish == null) {
                continue; // Skip invalid types
            }
            ItemStack output = entry.getValue();
            // Pick a different fish bucket by rarity
            Material displayMaterial = switch (fish.getRarity()) {
                case COMMON -> Material.COD_BUCKET;           // Common - cod bucket
                case UNCOMMON -> Material.SALMON_BUCKET;     // Uncommon - salmon bucket
                case RARE -> Material.PUFFERFISH_BUCKET;  // Rare - pufferfish
                case EPIC -> Material.TROPICAL_FISH_BUCKET;     // Epic - tropical fish
                case LEGENDARY -> Material.AXOLOTL_BUCKET;   // Legendary - axolotl
                case MYTHICAL -> Material.NAUTILUS_SHELL;       // Mythical - nether star (rarest)
                default -> Material.COD_BUCKET;
            };

            display.add(new CustomItemStack(
                    displayMaterial,
                    fish.getDisplayName(),
                    getGradientName("Items per second: weight × magic fish weight rarity")
            ));
            display.add(output);
        }
        return display;
    }



    private static void addDisplay(List<ItemStack> l,Material m, String s, ItemStack i){
        l.add(new CustomItemStack(m, s, getGradientName("Items per second: " + " weight * magic fish rarity ")));
        l.add(i);
    }

}
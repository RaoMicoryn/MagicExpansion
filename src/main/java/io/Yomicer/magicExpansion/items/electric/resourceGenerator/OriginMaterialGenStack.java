package io.Yomicer.magicExpansion.items.electric.resourceGenerator;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.items.abstracts.AbstractElectricResourceMachine;
import io.Yomicer.magicExpansion.items.misc.CargoCoreMore;
import io.Yomicer.magicExpansion.items.misc.CargoFragment;
import io.Yomicer.magicExpansion.items.tools.VoidTouch;
import io.Yomicer.magicExpansion.utils.NetworkStorage;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientNameVer2;
import static io.Yomicer.magicExpansion.utils.SameItemJudge.itemFromBase64;

public class OriginMaterialGenStack extends AbstractElectricResourceMachine {

    private static final int[] INPUT_SLOTS = new int[] {
            0, 1, 2, 3,
            9, 10, 11, 12,
            18, 19, 20, 21,
            27, 28, 29, 30,
            36, 37, 38, 39,
            45, 46, 47, 48
    };

    private static final int[] OUTPUT_SLOTS = new int[] {
            5, 6, 7, 8,
            14, 15, 16, 17,
            23, 24, 25, 26,
            32, 33, 34, 35,
            41, 42, 43, 44,
            50, 51, 52, 53
    };
    private ItemStack material;
    private static final int[] INFO_BORDER_SLOTS = new int[] { 4, 22, 13, 31, 40, 49 };

    private static final String MACHINE_MULTIPLIER_KEY = "machine_random_multiplier";

    private static final ItemStack PROGRESS_ITEM = new ItemStack(Material.SOUL_LANTERN);
    private static final NamespacedKey ORIGIN_MATERIAL_KEY = new NamespacedKey(MagicExpansion.getInstance(), "origin_material");

    // Void Touch bound coordinates PDC keys (same as the Eco Tank)
    private static final NamespacedKey KEY_X = new NamespacedKey(MagicExpansion.getInstance(), "touch_x");
    private static final NamespacedKey KEY_Y = new NamespacedKey(MagicExpansion.getInstance(), "touch_y");
    private static final NamespacedKey KEY_Z = new NamespacedKey(MagicExpansion.getInstance(), "touch_z");
    private static final NamespacedKey KEY_WORLD = new NamespacedKey(MagicExpansion.getInstance(), "touch_world");

    // Ether Casket internal data keys (same as CargoFragment)
    private static final NamespacedKey KEY_FRAGMENT_ITEM = new NamespacedKey(MagicExpansion.getInstance(), "cargo_item_json");
    private static final NamespacedKey KEY_FRAGMENT_AMOUNT = new NamespacedKey(MagicExpansion.getInstance(), "cargo_amount");

    public OriginMaterialGenStack(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }
    @Override
    protected void onPlace(BlockPlaceEvent e, Block b) {
        super.onPlace(e, b);
        double randomMultiplier = 1.0 + Math.random();

        Location loc = b.getLocation();
        SlimefunBlockData data = StorageCacheUtils.getBlock(loc);
        if (data != null) {
            data.setData(MACHINE_MULTIPLIER_KEY, String.valueOf(randomMultiplier));
        }
    }

    @Override
    protected int[] getInputSlots() {
        return INPUT_SLOTS;
    }

    @Override
    protected int[] getOutputSlots() {
        return OUTPUT_SLOTS;
    }
    @Override
    public boolean isSynchronized() {
        return false;
    }

    @Override
    public MachineRecipe findNextRecipe(BlockMenu menu) {
        if (!hasOutputSpace(menu)) return null;

        for (int slot : INPUT_SLOTS) {
            if (hasGeneratorInSlot(menu, slot)) {
                return new MachineRecipe(1, new ItemStack[]{null}, new ItemStack[]{null});
            }
        }
        return null;
    }

    @Override
    protected void addOutputs(BlockMenu menu, Block b, ItemStack[] outputs) {
        Location loc = b.getLocation();
        SlimefunBlockData data = StorageCacheUtils.getBlock(loc);
        double randomFactor = 1.0;
        if (data != null) {
            String multiplierStr = data.getData(MACHINE_MULTIPLIER_KEY);
            if (multiplierStr != null) {
                try {
                    randomFactor = Double.parseDouble(multiplierStr);
                } catch (NumberFormatException e) {
                    randomFactor = 1.0;
                }
            } else {
                randomFactor = 1.0 + Math.random();
                data.setData(MACHINE_MULTIPLIER_KEY, String.valueOf(randomFactor));
            }
        }

        Map<Material, Long> productionMap = new HashMap<>();
        long[] totalRef = new long[]{0};
        for (int slot : INPUT_SLOTS) {
            addGeneratorContributions(menu, slot, productionMap, totalRef);
        }
        long totalGenerators = totalRef[0];
        int distinctTypes = productionMap.size();

        if (totalGenerators > 0) {
            for (Map.Entry<Material, Long> entry : productionMap.entrySet()) {
                Material mat = entry.getKey();
                long baseAmount = entry.getValue();
                long finalAmount = (long) (baseAmount * randomFactor);
                if (finalAmount > Integer.MAX_VALUE) {
                    finalAmount = Integer.MAX_VALUE; // Overflow protection
                }
                if (finalAmount <= 0) {
                    continue;
                }
                ItemStack singleOutput = new ItemStack(mat, 1);
                long remaining = finalAmount;
                for (int slot : OUTPUT_SLOTS) {
                    if (remaining <= 0) {
                        break;
                    }
                    remaining = pushOutputToSlot(menu, b, slot, singleOutput, remaining);
                }
            }
        }

        updateStatusUI(menu, (int) Math.min(totalGenerators, Integer.MAX_VALUE), distinctTypes, productionMap, randomFactor);
    }

    private boolean isValidGenerator(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(ORIGIN_MATERIAL_KEY, PersistentDataType.STRING);
    }

    // ==================== Input side: direct Evolution Table / Quantum Storage / Ether Casket ====================

    // Check whether an input slot has an Evolution Table (placed directly / inside Quantum Storage / inside an Ether Casket)
    private boolean hasGeneratorInSlot(BlockMenu menu, int slot) {
        ItemStack item = menu.getItemInSlot(slot);
        if (item == null) {
            return false;
        }
        if (isValidGenerator(item)) {
            return true;
        }
        if (NetworkStorage.isQuantumStorageItem(item)) {
            NetworkStorage.QuantumCache qc = NetworkStorage.getQuantumCache(item.getItemMeta());
            return qc != null && qc.getItemStack() != null && isValidGenerator(qc.getItemStack());
        }
        if (SlimefunItem.getByItem(item) instanceof CargoFragment) {
            ItemStack stored = readFragmentItem(item);
            return stored != null && isValidGenerator(stored);
        }
        return false;
    }

    // Add up the Evolution Table contribution of a single input slot (the count uses long to avoid overflow)
    private void addGeneratorContributions(BlockMenu menu, int slot, Map<Material, Long> productionMap, long[] totalRef) {
        ItemStack item = menu.getItemInSlot(slot);
        if (item == null) {
            return;
        }

        if (isValidGenerator(item)) {
            addContribution(productionMap, item, item.getAmount(), totalRef);
            return;
        }
        if (NetworkStorage.isQuantumStorageItem(item)) {
            NetworkStorage.QuantumCache qc = NetworkStorage.getQuantumCache(item.getItemMeta());
            if (qc == null || qc.getItemStack() == null || !isValidGenerator(qc.getItemStack())) {
                return;
            }
            addContribution(productionMap, qc.getItemStack(), qc.getAmountLong(), totalRef);
            return;
        }
        if (SlimefunItem.getByItem(item) instanceof CargoFragment) {
            ItemStack stored = readFragmentItem(item);
            if (stored == null || !isValidGenerator(stored)) {
                return;
            }
            addContribution(productionMap, stored, readFragmentAmount(item), totalRef);
        }
    }

    private void addContribution(Map<Material, Long> productionMap, ItemStack generator, long count, long[] totalRef) {
        String materialName = generator.getItemMeta().getPersistentDataContainer()
                .get(ORIGIN_MATERIAL_KEY, PersistentDataType.STRING);
        if (materialName == null) {
            return;
        }
        try {
            Material mat = Material.valueOf(materialName);
            productionMap.merge(mat, count, Long::sum);
            totalRef[0] += count;
        } catch (IllegalArgumentException ignored) {
        }
    }

    // Read the item and amount inside an Ether Casket
    private ItemStack readFragmentItem(ItemStack fragment) {
        if (fragment == null || !fragment.hasItemMeta()) {
            return null;
        }
        String json = fragment.getItemMeta().getPersistentDataContainer()
                .get(KEY_FRAGMENT_ITEM, PersistentDataType.STRING);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return itemFromBase64(json);
    }

    private int readFragmentAmount(ItemStack fragment) {
        if (fragment == null || !fragment.hasItemMeta()) {
            return 0;
        }
        Integer amount = fragment.getItemMeta().getPersistentDataContainer()
                .get(KEY_FRAGMENT_AMOUNT, PersistentDataType.INTEGER);
        return amount == null ? 0 : amount;
    }

    // ==================== Output side: Quantum Storage / Void Touch / physical slots ====================

    // Try to put amount products into the given output slot; returns how many still didn't fit (0 = everything fit)
    private long pushOutputToSlot(BlockMenu menu, Block block, int slot, ItemStack output, long amount) {
        ItemStack slotItem = menu.getItemInSlot(slot);

        // ① The slot holds a Quantum Storage item → only store if the type matches
        if (NetworkStorage.isQuantumStorageItem(slotItem)) {
            NetworkStorage.QuantumCache qc = NetworkStorage.getQuantumCache(slotItem.getItemMeta());
            if (qc == null || qc.getItemStack() == null) {
                return amount;
            }
            if (!SlimefunUtils.isItemSimilar(output, qc.getItemStack(), true)) {
                return amount; // Type doesn't match, skip this slot
            }
            ItemStack toStore = output.clone();
            toStore.setAmount((int) Math.min(amount, Integer.MAX_VALUE));
            long leftover = NetworkStorage.store(slotItem, toStore);
            if (leftover < amount) {
                menu.replaceExistingItem(slot, slotItem); // Write the updated storage item back
            }
            return leftover;
        }

        // ② The slot holds a Void Touch → store into the bound Quantum Storage block / Magic Storage Terminal
        SlimefunItem sf = slotItem == null ? null : SlimefunItem.getByItem(slotItem);
        if (sf instanceof VoidTouch) {
            Location target = readVoidTouchTarget(slotItem);
            if (target != null) {
                SlimefunItem targetSf = StorageCacheUtils.getSfItem(target);
                if (NetworkStorage.isQuantumStorageBlock(targetSf)) {
                    ItemStack toStore = output.clone();
                    toStore.setAmount((int) Math.min(amount, Integer.MAX_VALUE));
                    return NetworkStorage.storeToQuantumStorageBlock(target, toStore);
                }
                if (targetSf instanceof CargoCoreMore) {
                    ItemStack toStore = output.clone();
                    toStore.setAmount((int) Math.min(amount, Integer.MAX_VALUE));
                    return NetworkStorage.storeToCargoCore(target, toStore) ? 0 : amount;
                }
            }
        }

        // ③ Physical slot: one stack at a time, pushItem stacks automatically
        ItemStack toPush = output.clone();
        int toPushAmount = (int) Math.min(amount, 64);
        toPush.setAmount(toPushAmount);
        ItemStack leftoverStack = menu.pushItem(toPush, new int[]{slot});
        // Note: pushItem changes the amount of the item passed in, so save the amount before calling it
        long placed = leftoverStack == null ? toPushAmount : toPushAmount - leftoverStack.getAmount();
        return amount - placed;
    }

    // Read the target coordinates bound to the Void Touch
    private Location readVoidTouchTarget(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(KEY_X, PersistentDataType.INTEGER) ||
                !container.has(KEY_Y, PersistentDataType.INTEGER) ||
                !container.has(KEY_Z, PersistentDataType.INTEGER) ||
                !container.has(KEY_WORLD, PersistentDataType.STRING)) {
            return null;
        }
        World world = Bukkit.getWorld(container.get(KEY_WORLD, PersistentDataType.STRING));
        if (world == null) {
            return null;
        }
        return new Location(world,
                container.get(KEY_X, PersistentDataType.INTEGER),
                container.get(KEY_Y, PersistentDataType.INTEGER),
                container.get(KEY_Z, PersistentDataType.INTEGER));
    }

    private boolean hasOutputSpace(BlockMenu menu) {
        for (int slot : OUTPUT_SLOTS) {
            ItemStack item = menu.getItemInSlot(slot);
            if (item == null || item.getType().isAir() || item.getAmount() < item.getMaxStackSize()) {
                return true;
            }
            // Quantum Storage still has free space
            if (NetworkStorage.isQuantumStorageItem(item)) {
                NetworkStorage.QuantumCache qc = NetworkStorage.getQuantumCache(item.getItemMeta());
                if (qc != null && qc.getItemStack() != null && qc.getAmountLong() < qc.getLimitLong()) {
                    return true;
                }
            }
            // The Void Touch is bound to valid storage
            SlimefunItem sf = SlimefunItem.getByItem(item);
            if (sf instanceof VoidTouch) {
                Location target = readVoidTouchTarget(item);
                if (target != null) {
                    SlimefunItem targetSf = StorageCacheUtils.getSfItem(target);
                    if (NetworkStorage.isQuantumStorageBlock(targetSf) || targetSf instanceof CargoCoreMore) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void updateStatusUI(BlockMenu menu, int total, int types, Map<Material, Long> productionMap, double randomFactor) {
        int usedOutputSlots = 0;
        for (int slot : OUTPUT_SLOTS) {
            if (menu.getItemInSlot(slot) != null) usedOutputSlots++;
        }
        double fillPercentage = (double) usedOutputSlots / OUTPUT_SLOTS.length * 100;

        // Build the lore for slot 4
        List<String> slot4Lore = new ArrayList<>();
        if (total > 0) {
            slot4Lore.add(getGradientNameVer2("Status: Running"));
        } else {
            slot4Lore.add(getGradientNameVer2("Status: Idle / no input"));
        }
        slot4Lore.add(getGradientNameVer2("Output used: " + String.format("%.1f", fillPercentage) + "%"));
        slot4Lore.add(getGradientNameVer2("Used: " + usedOutputSlots + " / " + OUTPUT_SLOTS.length));

        menu.replaceExistingItem(4, new CustomItemStack(
                total > 0 ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE,
                getGradientNameVer2("Machine Status"),
                slot4Lore
        ));


        List<String> combinedLore = new ArrayList<>();
        combinedLore.add(getGradientNameVer2("Evolution Tables: " + total));
        combinedLore.add(getGradientNameVer2("Origin Multiplier: " + String.format("%.2f", randomFactor)));
        menu.replaceExistingItem(13, new CustomItemStack(Material.REPEATER, getGradientNameVer2("Core Data"), combinedLore));
        updateInfoSlots(menu, productionMap, randomFactor);
    }
    private void updateInfoSlots(BlockMenu menu, Map<Material, Long> productionMap, double randomFactor) {
        Inventory inv = menu.toInventory();
        if (inv == null || inv.getViewers().isEmpty()) {
            return;
        }
        String[] easterEggs = {
                getGradientNameVer2("Feeling lucky?"),
                getGradientNameVer2("The machine is slacking off..."),
                getGradientNameVer2("That's not scientific!"),
                getGradientNameVer2("Try rolling more multipliers"),
                getGradientNameVer2("Producing efficiently..."),
                getGradientNameVer2("This is way too slow..."),
                getGradientNameVer2("A little faster!"),
                getGradientNameVer2("Magic in progress...")
        };

        int[] infoSlots = {31, 40, 49};
        List<Map.Entry<Material, Long>> entryList = new ArrayList<>(productionMap.entrySet());
        if (entryList.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                String eggText = easterEggs[(int) (Math.random() * easterEggs.length)];
                menu.replaceExistingItem(infoSlots[i], new CustomItemStack(Material.PAPER, getGradientNameVer2("System Log"), getGradientNameVer2(eggText)));
            }
        } else {
            List<String> linesToShow = new ArrayList<>();
            linesToShow.add(getGradientNameVer2("--- Current Output List ---"));
            for (Map.Entry<Material, Long> entry : entryList) {
                Material mat = entry.getKey();
                long count = entry.getValue();
                long outCount = (long) (count * randomFactor);
                if (outCount > Integer.MAX_VALUE) {
                    outCount = Integer.MAX_VALUE; // Overflow protection
                }
                String name = ItemStackHelper.getDisplayName(new ItemStack(mat));
                linesToShow.add(getGradientNameVer2(name + ": " + outCount));
            }
            if (entryList.size() > 6) {
                linesToShow.add(getGradientNameVer2("... (click to see more)"));
            } else {
                linesToShow.add(getGradientNameVer2("(click a slot for details)"));
            }
            for (int i = 0; i < 3; i++) {
                List<String> slotLore = new ArrayList<>();
                int start = i * 4;
                if (start < linesToShow.size()) {
                    int end = Math.min(start + 4, linesToShow.size());
                    slotLore = linesToShow.subList(start, end);
                    menu.replaceExistingItem(infoSlots[i], new CustomItemStack(Material.WRITABLE_BOOK, getGradientNameVer2("Production Log"), slotLore));
                } else {
                    String eggText = easterEggs[(int) (Math.random() * easterEggs.length)];
                    menu.replaceExistingItem(infoSlots[i], new CustomItemStack(Material.PAPER, getGradientNameVer2("Easter Egg"), (eggText)));
                }
            }
        }
    }

    private void openProductionMenu(Player p, Map<Material, Long> productionMap) {
        ChestMenu menu = new ChestMenu(getGradientNameVer2("Detailed Production Report"));

        menu.setPlayerInventoryClickable(false);
        menu.setEmptySlotsClickable(false);
        ItemStack background = new CustomItemStack(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            menu.addItem(i, background, ChestMenuUtils.getEmptyClickHandler());
        }
        int index = 0;
        int slotIndex = 0;
        for (Map.Entry<Material, Long> entry : productionMap.entrySet()) {
            if (slotIndex >= 54) break; // Beyond the menu size
            Material mat = entry.getKey();
            long count = entry.getValue();
            ItemStack displayItem = new ItemStack(mat);
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(getGradientNameVer2("Output: " + ItemStackHelper.getDisplayName(displayItem)));
                List<String> lore = new ArrayList<>();
                lore.add(getGradientNameVer2("Evolution Tables: " + count));
                lore.add(getGradientNameVer2("Final output is affected by the multiplier"));
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            menu.addItem(slotIndex, displayItem, ChestMenuUtils.getEmptyClickHandler());
            slotIndex++;
        }
        if (productionMap.isEmpty()) {
            menu.addItem(22, new CustomItemStack(Material.BARRIER, getGradientNameVer2("No production data yet")), ChestMenuUtils.getEmptyClickHandler());
        }
        menu.open(p);
    }

    private Map<Material, Long> calculateProductionMap(BlockMenu menu) {
        Map<Material, Long> map = new HashMap<>();
        long[] totalRef = new long[]{0};
        for (int slot : INPUT_SLOTS) {
            addGeneratorContributions(menu, slot, map, totalRef);
        }
        return map;
    }

    @Override
    protected void setupMenu(BlockMenuPreset preset) {
        for (int slot : INFO_BORDER_SLOTS) {
            preset.addItem(slot, new CustomItemStack(Material.CYAN_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());
        }
        preset.addItem(13, new CustomItemStack(Material.CYAN_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(22, new CustomItemStack(Material.PINK_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());
        ChestMenu.MenuClickHandler infoClickHandler = (player, slot, item, action) -> {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof BlockMenu) {
                BlockMenu menu = (BlockMenu) player.getOpenInventory().getTopInventory().getHolder();
                Map<Material, Long> currentMap = calculateProductionMap(menu);
                openProductionMenu(player, currentMap);
            }
            return false;
        };
        preset.addItem(31, new CustomItemStack(Material.BOOK, getGradientNameVer2("Click for details")), infoClickHandler);
        preset.addItem(40, new CustomItemStack(Material.BOOK, getGradientNameVer2("Click for details")), infoClickHandler);
        preset.addItem(49, new CustomItemStack(Material.BOOK, getGradientNameVer2("Click for details")), infoClickHandler);
    }


    @Override
    protected int getProgressSlot() { return 22; }
    @Override
    public ItemStack getProgressBar() { return PROGRESS_ITEM; }
    @Override
    public void postRegister() {
        registerRecipe(getCraftSecond(), new ItemStack[] { this.material }, getItemStackOutputs());
    }
    @Override
    public List<ItemStack> getDisplayRecipes() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientNameVer2("Put in Evolution Table: Origin"), getGradientNameVer2("A fixed multiplier is set when the machine is placed")));
        list.addAll(Arrays.asList(getItemStackOutputs()));
        return list;
    }
}
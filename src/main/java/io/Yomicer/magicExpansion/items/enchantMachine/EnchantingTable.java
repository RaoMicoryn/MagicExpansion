package io.Yomicer.magicExpansion.items.enchantMachine;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.Yomicer.magicExpansion.utils.itemUtils.newItem;
import io.Yomicer.magicExpansion.utils.log.Debug;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import java.util.*;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;
import static io.Yomicer.magicExpansion.utils.Utils.doGlow;
import static io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils.isItemSimilar;

public class EnchantingTable extends SimpleSlimefunItem<ItemUseHandler> implements EnergyNetComponent {


    private final int[] pinkBorder = {0,1, 7,8,9,10, 16,17,18,19,20,21,22,23,24,25,26, 28,29, 30,31, 32,33, 34, 36,37,38, 42,43,44,45,46,47, 51,52,53};
    private final int[] lightBlueBorder = {2, 4, 6, 11, 13, 15, 27, 35};
    private final int[] yellowBorder = {39,40,41, 48,50};
    private final int[] iBorder = {12};
    private final int[] iiBorder = {14};
    public static final int ENERGY_CONSUMPTION = 10000;
    public static final int CAPACITY = 8888888;
    private final int[] inputSlots = {3,5};
    private final int weaponSlot = 3;
    private final int resourceSlot = 5;
    private final int[] outputSlot = {49};
    private final String machineName;

    // Define the attribute pool (with the MagicExpansion prefix)
    public static final Map<String, Object> ATTRIBUTE_POOL = new LinkedHashMap<>();
    public static final Map<String, Integer> ATTRIBUTE_POOL_S = new LinkedHashMap<>();
    public static final Map<String, Integer> ATTRIBUTE_POOL_A = new LinkedHashMap<>();
    public static final Map<String, Integer> ATTRIBUTE_POOL_B = new LinkedHashMap<>();
    public static final Map<String, Integer> ATTRIBUTE_POOL_T = new LinkedHashMap<>();

    static {
        ATTRIBUTE_POOL.put("MagicExpansion.AttackTpToTp", true);     // Attack Swap     S-A-T
        ATTRIBUTE_POOL.put("MagicExpansion.Knockback", 1);            // Knockback Level   S-A-T    S sword A axe B bow/crossbow T trident (trident only has melee effects for now)
        ATTRIBUTE_POOL.put("MagicExpansion.Attraction", true);        // Pull Effect   S-A-T
        ATTRIBUTE_POOL.put("MagicExpansion.FireDamage", 1);           // Fire Damage   S-A-T
        ATTRIBUTE_POOL.put("MagicExpansion.Slowness", 1);           // Slowness Level     S-A-T
        ATTRIBUTE_POOL.put("MagicExpansion.Lightning", true);        // Lightning Effect    S-A-T
        ATTRIBUTE_POOL.put("MagicExpansion.EntitySpawn", 1);        // Spawn egg effect    S-A-T
        ATTRIBUTE_POOL.put("MagicExpansion.ArrowTp", true);        // Arrow tp effect    B

    }
    static {
        ATTRIBUTE_POOL_S.put("MagicExpansion.Knockback", 70);            // Knockback Level   S-A-T    S sword A axe B bow/crossbow T trident (trident only has melee effects for now)
        ATTRIBUTE_POOL_S.put("MagicExpansion.Attraction", 80);        // Pull Effect   S-A-T
        ATTRIBUTE_POOL_S.put("MagicExpansion.FireDamage", 100);           // Fire Damage   S-A-T
        ATTRIBUTE_POOL_S.put("MagicExpansion.Slowness", 50);           // Slowness Level     S-A-T
        ATTRIBUTE_POOL_S.put("MagicExpansion.Lightning", 10);        // Lightning Effect    S-A-T
        ATTRIBUTE_POOL_S.put("MagicExpansion.EntitySpawn", 1);        // Spawn egg effect    S-A-T
        ATTRIBUTE_POOL_S.put("MagicExpansion.AttackTpToTp", 30);     // Attack Swap     S-A-T
    }
    static {
        ATTRIBUTE_POOL_A.put("MagicExpansion.Knockback", 70);            // Knockback Level   S-A-T    S sword A axe B bow/crossbow T trident (trident only has melee effects for now)
        ATTRIBUTE_POOL_A.put("MagicExpansion.Attraction", 80);        // Pull Effect   S-A-T
        ATTRIBUTE_POOL_A.put("MagicExpansion.FireDamage", 100);           // Fire Damage   S-A-T
        ATTRIBUTE_POOL_A.put("MagicExpansion.Slowness", 50);           // Slowness Level     S-A-T
        ATTRIBUTE_POOL_A.put("MagicExpansion.Lightning", 10);        // Lightning Effect    S-A-T
        ATTRIBUTE_POOL_A.put("MagicExpansion.EntitySpawn", 1);        // Spawn egg effect    S-A-T
        ATTRIBUTE_POOL_A.put("MagicExpansion.AttackTpToTp", 30);     // Attack Swap     S-A-T
    }
    static {
        ATTRIBUTE_POOL_B.put("MagicExpansion.ArrowTp", 1);        // Arrow tp effect    B      S sword A axe B bow/crossbow T trident (trident only has melee effects for now)
    }
    static {
        ATTRIBUTE_POOL_T.put("MagicExpansion.Knockback", 70);            // Knockback Level   S-A-T    S sword A axe B bow/crossbow T trident (trident only has melee effects for now)
        ATTRIBUTE_POOL_T.put("MagicExpansion.Attraction", 80);        // Pull Effect   S-A-T
        ATTRIBUTE_POOL_T.put("MagicExpansion.FireDamage", 100);           // Fire Damage   S-A-T
        ATTRIBUTE_POOL_T.put("MagicExpansion.Slowness", 50);           // Slowness Level     S-A-T
        ATTRIBUTE_POOL_T.put("MagicExpansion.Lightning", 10);        // Lightning Effect    S-A-T
        ATTRIBUTE_POOL_T.put("MagicExpansion.EntitySpawn", 1);        // Spawn egg effect    S-A-T
        ATTRIBUTE_POOL_T.put("MagicExpansion.AttackTpToTp", 30);     // Attack Swap     S-A-T
    }

    // Define the name map (key -> display name)
    public static final Map<String, String> ATTRIBUTE_NAME_MAP = new HashMap<>();

    static {
        ATTRIBUTE_NAME_MAP.put("MagicExpansion.Knockback", "Knockback Level");
        ATTRIBUTE_NAME_MAP.put("MagicExpansion.Attraction", "Pull Effect");
        ATTRIBUTE_NAME_MAP.put("MagicExpansion.FireDamage", "Fire Damage");
        ATTRIBUTE_NAME_MAP.put("MagicExpansion.Slowness", "Slowness Level");
        ATTRIBUTE_NAME_MAP.put("MagicExpansion.Lightning", "Lightning Effect");
        ATTRIBUTE_NAME_MAP.put("MagicExpansion.EntitySpawn", "Power of Creation");
        ATTRIBUTE_NAME_MAP.put("MagicExpansion.ArrowTp", "Ender Pearl Arrow");
        ATTRIBUTE_NAME_MAP.put("MagicExpansion.AttackTpToTp", "Attack Swap");
    }

    public EnchantingTable(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, String displayName, String machineName) {
        super(category, item, recipeType, recipe);


        this.machineName = machineName;

        constructMenu(displayName);
        addItemHandler(onBreak());
    }

    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem sf, SlimefunBlockData data) {
                EnchantingTable.this.tick(b);
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }
        });
    }


    protected void tick(Block block) {
        BlockMenu menu = StorageCacheUtils.getMenu(block.getLocation());

        if(menu != null && menu.hasViewer()) {
            if (getCharge(block.getLocation()) < getEnergyConsumption()) {
                menu.addItem(18, new CustomItemStack(doGlow(Material.NETHER_STAR), "§x§F§D§B§7§D§4⚡Not enough power⚡",
                                getGradientName("Each enchant uses 10,000 power")),
                        (player1, slot, item, action) -> false);
                return;
            }
            menu.addItem(18, new CustomItemStack(doGlow(Material.PINK_STAINED_GLASS_PANE), ""),
                    (player1, slot, item, action) -> false);

            craftIfValid(block);
        }
    }

    /**
     * Randomly pick PDC attributes and set them on the item
     *
     * @param item  the input item
     * @param resourceLevel input value (1, 2, 3)
     * @param num 1-3 amount
     * @return the changed item
     */
    public static ItemStack randomizeAttributes(ItemStack item, int resourceLevel, Map<String, Integer> POOL, int num) {
        // Work out the pick range
        int minAttributes = 1;
        int maxAttributes = 1;
        if(num == 1){
        }
        else if(num == 2){
            minAttributes = num;
            maxAttributes = num + 1;
        }else if(num == 3){
            minAttributes = num;
            maxAttributes = num + 2;
        }
        Random random = new Random();
        int attributeCount = random.nextInt(maxAttributes - minAttributes + 1) + minAttributes;

        // Pick attributes randomly
        List<String> selectedAttributes = selectRandomAttributesByWeight(attributeCount,POOL);

        ItemStack item1 = item.clone();
        // Set to PDC
        ItemMeta meta = item1.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            for (String attribute : selectedAttributes) {
                Object value = ATTRIBUTE_POOL.get(attribute);
                NamespacedKey key = new NamespacedKey("magicexpansion", attribute.toLowerCase().replace(".", "_"));
                if (value instanceof Integer) {
                    Integer value1 = generateWeightedRandomLevel();
                    pdc.set(key, PersistentDataType.INTEGER, value1);
                } else if (value instanceof Boolean) {
                    Random random1 = new Random();
                    boolean randomBoolean = random1.nextBoolean();
                    pdc.set(key, PersistentDataType.BOOLEAN, randomBoolean);
                }
            }

            // Update lore
            updateLore(meta, selectedAttributes);

            item1.setItemMeta(meta);
        }

        return item1;
    }

    private static final Random RANDOM1 = new Random();

    /**
     * Make a random integer from 1 to 101 using the given chances
     *
     * @return the random integer
     */
    public static int generateWeightedRandomLevel() {
        double randomValue = RANDOM1.nextDouble() * 100; // Make a random value in [0, 100)

        if (randomValue < 60.0) { // 60% chance
            return RANDOM1.nextInt(20) + 1; // 1 to 20
        } else if (randomValue < 85.0) { // 25% chance
            return RANDOM1.nextInt(30) + 21; // 21 to 50
        } else if (randomValue < 95.0) { // 10% chance
            return RANDOM1.nextInt(20) + 51; // 51 to 70
        } else if (randomValue < 98.0) { // 3% chance
            return RANDOM1.nextInt(10) + 71; // 71 to 80
        } else if (randomValue < 99.0) { // 1% chance
            return RANDOM1.nextInt(10) + 81; // 81 to 90
        } else { // 1% chance
            return RANDOM1.nextInt(11) + 91; // 91 to 101
        }
    }

    /**
     * Pick several attributes randomly by weight
     *
     * @param count      how many attributes to pick
     * @param weightMap  map of attributes and their weights
     * @return list of the attributes picked
     */
    public static List<String> selectRandomAttributesByWeight(int count, Map<String, Integer> weightMap) {
        List<String> selectedAttributes = new ArrayList<>();
        Set<String> usedAttributes = new HashSet<>(); // to avoid duplicates

        // Loop to pick the given number of attributes
        while (selectedAttributes.size() < count) {
            String attribute = getRandomAttributeByWeight(weightMap);
            if (attribute != null && !usedAttributes.contains(attribute)) {
                selectedAttributes.add(attribute);
                usedAttributes.add(attribute);
            }

            // If every attribute is already picked, stop the loop
            if (usedAttributes.size() == weightMap.size()) {
                break;
            }
        }

        return selectedAttributes;
    }

    /**
     * Pick one attribute randomly by weight
     *
     * @param weightMap map of attributes and their weights
     * @return name of the attribute picked
     */
    public static String getRandomAttributeByWeight(Map<String, Integer> weightMap) {
        // Work out the total weight
        int totalWeight = weightMap.values().stream().mapToInt(Integer::intValue).sum();

        // Make a random number in [0, totalWeight)
        int randomValue = RANDOM.nextInt(totalWeight);

        // Add up the weights to find the matching attribute
        int cumulativeWeight = 0;
        for (Map.Entry<String, Integer> entry : weightMap.entrySet()) {
            cumulativeWeight += entry.getValue(); // add this attribute's weight
            if (randomValue < cumulativeWeight) {
                return entry.getKey(); // return this attribute
            }
        }

        // If nothing matched (shouldn't happen), return null
        return null;
    }

    /**
     * Randomly pick the given number of attributes from the pool
     *
     * @param count attribute count
     * @return list of attributes picked
     */
    private static List<String> selectRandomAttributes(int count,Map<String, Object> POOL) {
        List<String> attributes = new ArrayList<>(POOL.keySet());
        Collections.shuffle(attributes, new Random());

        return attributes.subList(0, Math.min(count, attributes.size()));
    }


    private static void updateLore(ItemMeta meta, List<String> selectedAttributes) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        // Get the current lore or start with an empty list
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        lore.add(getGradientName("Magic Enchant:"));

        for (String attribute : selectedAttributes) {
            NamespacedKey key = new NamespacedKey("magicexpansion", attribute.toLowerCase().replace(".", "_"));
            String chineseName = ATTRIBUTE_NAME_MAP.getOrDefault(attribute, "Unknown Attribute");

            if (pdc.has(key, PersistentDataType.INTEGER)) {
                // Number attribute
                int value = pdc.get(key, PersistentDataType.INTEGER);
                lore.add("§f- " + getGradientName(chineseName) + ": §e" + value);
            } else if (pdc.has(key, PersistentDataType.BOOLEAN)) {
                // Yes/no attribute
                boolean value = pdc.get(key, PersistentDataType.BOOLEAN);
                lore.add("§f- " + getGradientName(chineseName) + ": " + (value ? "§eActive" : "§cSealed"));
            }
        }
        meta.setLore(lore);
    }

    /**
     * Check if the PDC has any attribute from the pool
     *
     * @param item the input item
     * @return whether it has any attribute
     */
    public static boolean hasAnyAttribute(ItemStack item) {
        if(item == null){
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false; // If there's no ItemMeta, just return false
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        for (String attribute : ATTRIBUTE_POOL.keySet()) {
            NamespacedKey key = new NamespacedKey("magicexpansion", attribute.toLowerCase().replace(".", "_"));

            // Check if the key exists
            if (pdc.has(key, PersistentDataType.INTEGER) || pdc.has(key, PersistentDataType.BOOLEAN)) {
                return true; // return true as soon as one attribute is found
            }
        }

        return false; // No attribute found
    }






    private void craftIfValid(Block block) {
        BlockMenu menu = StorageCacheUtils.getMenu(block.getLocation());

        // Make sure at least 1 slot is free
        for (int outSlot : getOutputSlots()) {
            ItemStack outItem = menu.getItemInSlot(outSlot);
            if (outItem == null || outItem.getAmount() < outItem.getMaxStackSize()) {
                break;
            } else if (outSlot == getOutputSlots()[getOutputSlots().length - 1]) {
                return;
            }
        }
        boolean process = true;
        // Check if slot 29 contains a weapon
        ItemStack weaponItem = menu.getItemInSlot(weaponSlot);
        if (!isWeapon(weaponItem)) {
            menu.addItem(26, new CustomItemStack(doGlow(Material.NETHER_STAR),"§x§F§D§B§7§D§4Missing Weapon",
                            getGradientName("Put in a magic enchant material"),
                            getGradientName("Supported for now:"),
                            getGradientName("Sword, axe, bow, crossbow, trident")),
                    (player1, slot, item, action) -> false);
            process = false; // Not a weapon, stop processing
        }else{
            menu.addItem(26, new CustomItemStack(doGlow(Material.PINK_STAINED_GLASS_PANE),""),
                    (player1, slot, item, action) -> false);
        }
        // Check if slot 30 contains Lapis Lazuli
        ItemStack resourceItem = menu.getItemInSlot(resourceSlot);
        int resourceLevel = 0;
        SlimefunItem level1 = SlimefunItem.getById("MAGIC_EXPANSION_BASIC_ENCHANT_STONE");
        ItemStack targetItem1 = level1.getItem().clone();

        if(isItemSimilar(resourceItem, targetItem1, false)){
            resourceLevel = 1;
        }

        int finalresourceLevel = resourceLevel;
//        if (!isLapisLazuli(resourceItem)) {
//            return; // Not Lapis Lazuli, stop processing
//        }
        if (finalresourceLevel == 0){
            menu.addItem(44, new CustomItemStack(doGlow(Material.NETHER_STAR),"§x§F§D§B§7§D§4Missing Required Material",
                            getGradientName("Put in a magic enchant material"),
                            getGradientName("Supported for now:"),
                            getGradientName("Star Iron"),
                            getGradientName("The better the material quality, the better the enchant")),
                    (player1, slot, item, action) -> {
                        return false;
                    });
            process = false;
        }else{
            menu.addItem(44, new CustomItemStack(doGlow(Material.PINK_STAINED_GLASS_PANE),""),
                    (player1, slot, item, action) -> false);
        }

        if (hasAnyAttribute(weaponItem)){
            menu.addItem(36, new CustomItemStack(doGlow(Material.NETHER_STAR),"§x§F§D§B§7§D§4Your weapon is already enchanted",
                            getGradientName("Each piece of gear can only be enchanted once")),
                    (player1, slot, item, action) -> false);
            process = false;
        }else{
            menu.addItem(36, new CustomItemStack(doGlow(Material.PINK_STAINED_GLASS_PANE),""),
                    (player1, slot, item, action) -> false);
        }
        if (!process) {
            menu.addItem(29, new CustomItemStack(doGlow(Material.BARRIER),ColorGradient.getGradientName("=======⚡Tier 1 Enchant⚡=======")),
                    (player1, slot, item, action) -> false);
            menu.addItem(31, new CustomItemStack(doGlow(Material.BARRIER),ColorGradient.getGradientName("=======⚡Tier 2 Enchant⚡=======")),
                    (player1, slot, item, action) -> false);
            menu.addItem(33, new CustomItemStack(doGlow(Material.BARRIER),ColorGradient.getGradientName("=======⚡Tier 3 Enchant⚡=======")),
                    (player1, slot, item, action) -> false);
            return;
        }

        Map<String, Integer> ATTRIBUTE_POOL_USE = new LinkedHashMap<>();
        String itemName = weaponItem.getType().name();
        if (itemName.endsWith("_SWORD")) {
            ATTRIBUTE_POOL_USE = ATTRIBUTE_POOL_S; // Sword
        } else if (itemName.endsWith("_AXE")) {
            ATTRIBUTE_POOL_USE = ATTRIBUTE_POOL_A; // Axe
        } else if (itemName.endsWith("BOW")) {
            ATTRIBUTE_POOL_USE = ATTRIBUTE_POOL_B; // Bow
        } else if (itemName.endsWith("TRIDENT")) {
            ATTRIBUTE_POOL_USE = ATTRIBUTE_POOL_T; // Trident
        }
        Map<String, Integer> finalATTRIBUTE_POOL_USE = ATTRIBUTE_POOL_USE;

        if(resourceItem.getAmount()>=1) {
            menu.addItem(29, newItem.themed(doGlow(Material.NETHER_STAR), "=======⚡Tier 1 Enchant⚡=======",
                            generateRandomLore(), 1),
                    (player1, slot, item, action) -> {

                        if(!isItemSimilar(menu.getItemInSlot(weaponSlot), weaponItem, true)){
                            player1.sendMessage(ColorGradient.getGradientName("Trying to exploit a bug again, huh?"));
                            return false;
                        }
                        if(resourceItem.getAmount()<1||!isItemSimilar(menu.getItemInSlot(resourceSlot), resourceItem, true)){
                            player1.sendMessage(ColorGradient.getGradientName("Didn't expect that, did you? I fixed this one too."));
                            return false;
                        }
                        ItemStack output = randomizeAttributes(weaponItem, finalresourceLevel, finalATTRIBUTE_POOL_USE,1);

                        if (!menu.fits(output, getOutputSlots())) {
                            return false;
                        }
                        if(resourceItem.getAmount()<1){
                            return false;
                        }
                        removeCharge(block.getLocation(), getEnergyConsumption());
                        craftI1(output, menu);
                        return false;
                    });
        }else {
            menu.addItem(29, new CustomItemStack(Material.BARRIER,ColorGradient.getGradientName("=======⚡Tier 1 Enchant⚡=======")),
                    (player1, slot, item, action) -> false);
        }
        if(resourceItem.getAmount()>=2) {
        menu.addItem(31, newItem.themed(doGlow(Material.NETHER_STAR), "=======⚡Tier 2 Enchant⚡=======",
                        generateRandomLore(),1),
                (player1, slot, item, action) -> {

                    if(!isItemSimilar(menu.getItemInSlot(weaponSlot), weaponItem, true)){
                        player1.sendMessage(ColorGradient.getGradientName("Trying to exploit a bug again, huh?"));
                        return false;
                    }
                    if(resourceItem.getAmount()<2||!isItemSimilar(menu.getItemInSlot(resourceSlot), resourceItem, true)){
                        player1.sendMessage(ColorGradient.getGradientName("Didn't expect that, did you? I fixed this one too."));
                        return false;
                    }
                    ItemStack output = randomizeAttributes(weaponItem, finalresourceLevel,finalATTRIBUTE_POOL_USE,2);

                    if (!menu.fits(output, getOutputSlots())) {
                        return false;
                    }
                    if(resourceItem.getAmount()<2){
                        return false;
                    }
                    removeCharge(block.getLocation(), getEnergyConsumption());
                    craftI2(output, menu);
                    return false;
                });
        }else {
            menu.addItem(31, new CustomItemStack(Material.BARRIER,ColorGradient.getGradientName("=======⚡Tier 2 Enchant⚡=======")),
                    (player1, slot, item, action) -> false);
        }
        if(resourceItem.getAmount()>=3) {
        menu.addItem(33, newItem.themed(doGlow(Material.NETHER_STAR), "=======⚡Tier 3 Enchant⚡=======",
                        generateRandomLore(),1),
                (player1, slot, item, action) -> {

                    if(!isItemSimilar(menu.getItemInSlot(weaponSlot), weaponItem, true)){
                        player1.sendMessage(ColorGradient.getGradientName("Trying to exploit a bug again, huh?"));
                        return false;
                    }
                    if(resourceItem.getAmount()<3||!isItemSimilar(menu.getItemInSlot(resourceSlot), resourceItem, true)){
                        player1.sendMessage(ColorGradient.getGradientName("Didn't expect that, did you? I fixed this one too."));
                        return false;
                    }
                    ItemStack output = randomizeAttributes(weaponItem, finalresourceLevel,finalATTRIBUTE_POOL_USE,3);

                    if (!menu.fits(output, getOutputSlots())) {
                        return false;
                    }
                    if(resourceItem.getAmount()<3){
                        return false;
                    }
                    removeCharge(block.getLocation(), getEnergyConsumption());
                    craftI3(output, menu);
                    return false;
                });
        }else {
            menu.addItem(33, new CustomItemStack(Material.BARRIER,ColorGradient.getGradientName( "=======⚡Tier 3 Enchant⚡=======")),
                    (player1, slot, item, action) -> false);
        }

//        ItemStack output = randomizeAttributes(weaponItem, resourceLevel);
//
//            if (!menu.fits(output, getOutputSlots())) {
//                return;
//            }
//            removeCharge(block.getLocation(), getEnergyConsumption());
//            craft(output, menu);


    }

    // Helper method to check if an item is a weapon
    private boolean isWeapon(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        // Add more logic to determine if the item is a weapon
        // For example, check if it's a sword, axe, or any other weapon type
        return item.getType().name().endsWith("_SWORD") ||
                item.getType().name().endsWith("_AXE")||
                item.getType().name().endsWith("BOW")||
                item.getType().name().endsWith("TRIDENT");
    }

    // Helper method to check if an item is Lapis Lazuli
    private boolean isLapisLazuli(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        return item.getType() == Material.LAPIS_LAZULI;
    }


    private void craft(ItemStack output, BlockMenu inv) {
        for (int j = 0; j < 9; j++) {
            ItemStack item = inv.getItemInSlot(getInputSlots()[j]);

            if (item != null && item.getType() != Material.AIR) {
                inv.consumeItem(getInputSlots()[j]);
            }
        }

        inv.pushItem(output, outputSlot);
    }

    private void craftI1(ItemStack output, BlockMenu inv) {
        ItemStack weapon = inv.getItemInSlot(weaponSlot);

        if (weapon != null && weapon.getType() != Material.AIR) {
            inv.consumeItem(weaponSlot);
        }
        ItemStack resource = inv.getItemInSlot(resourceSlot);

        if (resource != null && resource.getType() != Material.AIR) {
            inv.consumeItem(resourceSlot);
        }

        if (output != null && output.getType() != Material.AIR) {
            inv.pushItem(output, outputSlot);
        }
    }
    private void craftI2(ItemStack output, BlockMenu inv) {
        ItemStack weapon = inv.getItemInSlot(weaponSlot);

        if (weapon != null && weapon.getType() != Material.AIR) {
            inv.consumeItem(weaponSlot);
        }
        ItemStack resource = inv.getItemInSlot(resourceSlot);

        if (resource != null && resource.getType() != Material.AIR) {
            inv.consumeItem(resourceSlot,2);
        }

        if (output != null && output.getType() != Material.AIR) {
            inv.pushItem(output, outputSlot);
        }
    }
    private void craftI3(ItemStack output, BlockMenu inv) {
        ItemStack weapon = inv.getItemInSlot(weaponSlot);

        if (weapon != null && weapon.getType() != Material.AIR) {
            inv.consumeItem(weaponSlot);
        }
        ItemStack resource = inv.getItemInSlot(resourceSlot);

        if (resource != null && resource.getType() != Material.AIR) {
            inv.consumeItem(resourceSlot,3);
        }

        if (output != null && output.getType() != Material.AIR) {
            inv.pushItem(output, outputSlot);
        }
    }




    @Nonnull
    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER; // Define this block's type, e.g. consumer (CONSUMER)
    }

    public int getEnergyConsumption() {
        return ENERGY_CONSUMPTION;
    }

    public int getCapacity() {
        return CAPACITY;
    }

    public int[] getInputSlots() {
        return inputSlots;
    }

    public int[] getOutputSlots() {
        return outputSlot;
    }


    @Nonnull
    @Override
    public ItemUseHandler getItemHandler() {
        return null;
    }


    static void borders1(BlockMenuPreset preset, int[] border, int[] inputBorder, int[] outputBorder) {
        for (int i : border) {
            preset.addItem(i, new CustomItemStack(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), " "),
                    (p, slot, item, action) -> false);
        }

        for (int i : inputBorder) {
            preset.addItem(i, new CustomItemStack(new ItemStack(Material.CYAN_STAINED_GLASS_PANE), " "),
                    (p, slot, item, action) -> false);
        }

        for (int i : outputBorder) {
            preset.addItem(i, new CustomItemStack(new ItemStack(Material.ORANGE_STAINED_GLASS_PANE), " "),
                    (p, slot, item, action) -> false);
        }

    }

    static void borders(BlockMenuPreset preset, int[] pinkBorder, int[] lightBlueBorder, int[] yellowBorder, int[] j, int[] jj) {
        for (int i : pinkBorder ) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack (Material.PINK_STAINED_GLASS_PANE)), " "),
                    (p, slot, item, action) -> false);
        }

        for (int i : lightBlueBorder) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack (Material.LIGHT_BLUE_STAINED_GLASS_PANE)), " "),
                    (p, slot, item, action) -> false);
        }

        for (int i : yellowBorder) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack (Material.YELLOW_STAINED_GLASS_PANE)), " "),
                    (p, slot, item, action) -> false);
        }

        for (int i : j) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack (Material.DIAMOND_SWORD)), ColorGradient.getGradientName("Put the weapon above")),
                    (p, slot, item, action) -> false);
        }

        for (int i : jj) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack (Material.LAPIS_LAZULI)), ColorGradient.getGradientName("Put the enchant material above")),
                    (p, slot, item, action) -> false);
        }

    }


    private BlockBreakHandler onBreak() {
        return new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(@Nonnull BlockBreakEvent e, @Nonnull ItemStack i, @Nonnull List<ItemStack> list) {
                Block b = e.getBlock();
                BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());

                if (inv != null) {
                    inv.dropItems(b.getLocation(), getInputSlots());
                    inv.dropItems(b.getLocation(), getOutputSlots());
                }

            }
        };
    }

    private void constructMenu(String displayName) {
        new BlockMenuPreset(getId(), displayName) {

            @Override
            public void init() {
                constructMenu(this);
            }

            @Override
            public boolean canOpen(@Nonnull Block b, @Nonnull Player p) {
                return p.hasPermission("slimefun.inventory.bypass")
                        || Slimefun.getProtectionManager().hasPermission(p, b.getLocation(),
                        Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow itemTransportFlow) {
                return new int[0];
            }
            @Override
            public int[] getSlotsAccessedByItemTransport(DirtyChestMenu menu, ItemTransportFlow flow, ItemStack item) {
                return getCustomItemTransport(menu, flow, item);
            }
        };
    }
    protected int[] getCustomItemTransport(DirtyChestMenu menu, ItemTransportFlow flow, ItemStack item) {
        if (flow == ItemTransportFlow.WITHDRAW) {
            return getOutputSlots();
        }else{
            return getInputSlots();
        }
    }

    protected Comparator<Integer> compareSlots(DirtyChestMenu menu) {
        return Comparator.comparingInt(slot -> menu.getItemInSlot(slot).getAmount());
    }

    protected void constructMenu(BlockMenuPreset preset) {
        borders(preset, pinkBorder, lightBlueBorder, yellowBorder, iBorder, iiBorder);

        for (int i : getOutputSlots()) {
            preset.addMenuClickHandler(i, new ChestMenu.AdvancedMenuClickHandler() {

                @Override
                public boolean onClick(Player p, int slot, ItemStack cursor, ClickAction action) {
                    return false;
                }

                @Override
                public boolean onClick(InventoryClickEvent e, Player p, int slot, ItemStack cursor,
                                       ClickAction action) {
                    if (cursor == null) return true;
                    return cursor.getType() == Material.AIR;
                }
            });
        }

    }







    private static final Random RANDOM = new Random();

    /**
     * Make random garbled lines
     *
     * @return list with random garbled text
     */
    public static List<String> generateRandomLore() {
        // Randomly pick the number of lines (2 to 5)
        int randomLines = 2 + RANDOM.nextInt(4); // [2, 5]

        List<String> lore = new ArrayList<>();

        for (int i = 0; i < randomLines; i++) {
            // Make one garbled line
            String randomLine = generateRandomLine();
            lore.add(randomLine);
        }

        return lore;
    }

    /**
     * Make one random garbled line
     *
     * @return single garbled line string
     */
    private static String generateRandomLine() {
        StringBuilder line = new StringBuilder();

        // Each line starts with §k
        line.append("§k");

        // Each line is 10 to 23 characters long (not counting the leading §k)
        int length = 10 + RANDOM.nextInt(14); // [10, 23]

        for (int i = 0; i < length; i++) {
            line.append("§k");
            // Randomly decide whether to add a color code (20% chance)
            if (RANDOM.nextDouble() < 0.5) {
                line.append(getRandomColorCode());
            }

            // Randomly make a character ('A'-'Z', 'a'-'z', '0'-'9', or other symbols)
            char randomChar = getRandomCharacter();
            line.append(randomChar);

        }

        // Make sure each line ends with the garbled effect
        line.append("§k");

        return line.toString();
    }

    /**
     * Get a random color code
     *
     * @return random color code (like §c, §a, §b, etc.)
     */
    private static String getRandomColorCode() {
        String[] colorCodes = {
                "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9",
                "§a", "§b", "§c", "§d", "§e", "§f",
                "§l", "§m", "§n", "§o"
        };
        return colorCodes[RANDOM.nextInt(colorCodes.length)];
    }

    /**
     * Get a random character
     *
     * @return random character
     */
    private static char getRandomCharacter() {
        // Character range: uppercase, lowercase, digits, some symbols
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-=_+[]{}|;:,.<>?";
        return chars.charAt(RANDOM.nextInt(chars.length()));
    }


}
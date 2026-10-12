package io.Yomicer.magicExpansion.items.tools;

import io.Yomicer.magicExpansion.utils.WaterCloudPoolMenu;
import io.Yomicer.magicExpansion.items.misc.Lure;
import io.Yomicer.magicExpansion.items.misc.WeightedItem;
import io.Yomicer.magicExpansion.items.misc.fish.Gen2Fish;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;

public class FishingRodWaterCloud extends SlimefunItem implements RecipeDisplayItem {

    private final Map<Enchantment, Integer> enchantments;
    private final boolean glow;
    private final Map<String, List<WeightedItem>> lootTable;

    // ✅ All usable bait is listed here (material, display name, lore)
    private final List<ItemStack> USABLE_LURES;

    /**
     * Fishing rod rarity -> Gen 2 fish stat range multiplier (weightBoost).
     * The higher this is, the better the fish's five stats and quality. Bamboo is the lowest, and Taigong is the highest.
     */
    private final double weightBoost;


    /**
     * Constructor
     *
     * @param itemGroup   Category
     * @param item        Item
     * @param recipeType  Recipe type
     * @param recipe      Recipe
     * @param enchantments Enchantments to add (enchantment -> level)
     * @param glow         Whether to show the enchantment glow (true = show, false = hide)
     * @param weightBoost  Gen 2 fish stat range multiplier (0-4 levels; Bamboo is lowest, Taigong is highest)
     */
    public FishingRodWaterCloud(ItemGroup itemGroup,
                                SlimefunItemStack item,
                                RecipeType recipeType,
                                ItemStack[] recipe,
                                Map<Enchantment, Integer> enchantments,
                                boolean glow,
                                double weightBoost,
                                Map<String, List<WeightedItem>> lootTable,
                                List<ItemStack> USABLE_LURES) {
        super(itemGroup, item, recipeType, recipe);
        this.enchantments = enchantments;
        this.glow = glow;
        this.weightBoost = weightBoost;
        this.lootTable = lootTable;
        this.USABLE_LURES = USABLE_LURES;
    }

    /** Get the Gen 2 fish stat range multiplier (level) */
    public double getWeightBoost() {
        return weightBoost;
    }


    @Override
    public void preRegister() {
        ItemStack itemStack = getItem();
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            // 1. Make the item unbreakable (existing behavior)
            meta.setUnbreakable(true);

            // 2. Add the enchantments passed in
            if (enchantments != null && !enchantments.isEmpty()) {
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    Enchantment enchant = entry.getKey();
                    int level = entry.getValue();
                    if (enchant != null && level > 0) {
                        meta.addEnchant(enchant, level, true);
                    }
                }
            }
            if (!glow) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            }
            itemStack.setItemMeta(meta);
        }

    }
    public List<WeightedItem> getLootPoolForLure(Lure lure) {
        String key = lure != null ? lure.getKey() : "default";
        return lootTable.getOrDefault(key, getDefaultLootPool());
    }

    private List<WeightedItem> getDefaultLootPool() {
        return List.of(
                new WeightedItem(new ItemStack(Material.COD), 50),
                new WeightedItem(new ItemStack(Material.INK_SAC), 30)
        );
    }


    public Map<String, List<WeightedItem>> getLootTable() {
        return lootTable;
    }


    @Override
    public @NotNull List<ItemStack> getDisplayRecipes() {
        List<ItemStack> display = new ArrayList<>();
        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientName("Available Bait ⇩"),getGradientName("Bait listed first has higher priority")));
        display.add(new CustomItemStack(Material.AIR));
        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientName("Available Bait ⇩"),getGradientName("Offhand bait always comes first. No bait has the lowest priority")));
        display.add(new CustomItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE, getGradientName("No Bait")));
        // Add all bait
        for (ItemStack lure : USABLE_LURES) {
            display.add(new CustomItemStack(Material.AIR));
            display.add(lure);
        }

        // Loot pool button (the listener places it in slot 26 on the right side of the recipe page)
        display.add(WaterCloudPoolMenu.createPoolButton(this));

        return display;
    }

    // ============================================================
    // Build the Gen 2 fish loot pool (shared pool + bait-specific pool + junk layer)
    // ============================================================

    /**
     * Seed items for the shared "common fish pool" used by all three areas (real fish stats are added when caught).
     * Weights stay the same for all rods and bait, so basic catches stay consistent.
     */
    private static final List<WeightedItem> GEN2_COMMON_POOL = List.of(
            // Common fish seeds: weights are set for common catches, with rare fish having lower weights
            gen2Seed(Gen2Fish.YUNYIN, 450),
            gen2Seed(Gen2Fish.LANGXI, 450),
            gen2Seed(Gen2Fish.HENBO, 400),
            gen2Seed(Gen2Fish.WUYU, 400)
    );

    /**
     * Common junk pool (L3-A: fallback only). buildLootTable adds this and the elemental materials layer (B) separately.
     */
    private static final List<WeightedItem> GEN2_JUNK_POOL = List.of(
            new WeightedItem(new ItemStack(Material.COBBLESTONE, 15), 66),
            new WeightedItem(new ItemStack(Material.GRAVEL, 15), 66),
            new WeightedItem(new ItemStack(Material.SAND, 15), 66),
            new WeightedItem(new ItemStack(Material.FLINT, 15), 66),
            new WeightedItem(new ItemStack(Material.STRING, 15), 66),
            new WeightedItem(new ItemStack(Material.BONE, 15), 66)
    );

    /**
     * Create a Gen 2 fish loot seed (an ItemStack without stats; the stat generator adds the five stats and quality when caught).
     */
    private static WeightedItem gen2Seed(Gen2Fish type, int weight) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§dRarity · " + type.getRarity().getDisplayName());
        lore.add(type.getLore());
        // Build it the same way as FishAttributeGenerator.generate:
        // For fish bucket items (COD_BUCKET/PUFFERFISH_BUCKET, etc.), use CustomItemStack to keep the name and lore:
        // This avoids losing item metadata with buckets when using a plain new ItemStack + setItemMeta (lore wasn't being saved in testing).
        ItemStack item = new CustomItemStack(type.getMaterial(), type.getGradientName(), lore.toArray(new String[0]));
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            // Mark this as a Gen 2 fish "seed" so the catch listener can recognize it and add stats
            meta.getPersistentDataContainer().set(
                    io.Yomicer.magicExpansion.items.misc.fish.FishAttributeGenerator.GEN2_TYPE,
                    org.bukkit.persistence.PersistentDataType.STRING,
                    type.getId());
            item.setItemMeta(meta);
        }
        return new WeightedItem(item, weight);
    }

    /**
     * Build the Gen 2 fish loot pool for one bait:
     * = Shared common pool + bait-specific fish (main/epic) + fallback junk + elemental materials + special catch.
     *
     * @param lureKey          Bait key
     * @param mainFish         Main fish for this bait (weight around 800)
     * @param epicFish         Epic fish for this bait (weight around 400)
     * @param elementMaterials Element-themed materials for this bait (L3-B), can be empty
     * @param specialCatch     Special catch for this bait (weight 40)
     * @return The finished loot pool
     */
    public static List<WeightedItem> buildLootTable(String lureKey,
                                                    Gen2Fish mainFish, Gen2Fish epicFish,
                                                    List<WeightedItem> elementMaterials,
                                                    ItemStack specialCatch) {
        List<WeightedItem> pool = new ArrayList<>();
        // L1: shared common pool
        pool.addAll(GEN2_COMMON_POOL);
        // L2: bait-specific fish (main + epic)
        pool.add(gen2Seed(mainFish, 800));
        pool.add(gen2Seed(epicFish, 400));
        // L3-B: optional elemental materials + common junk fallback
        if (elementMaterials != null && !elementMaterials.isEmpty()) {
            pool.addAll(elementMaterials);
        }
        pool.addAll(GEN2_JUNK_POOL);
        // L4 Special catch
        if (specialCatch != null) {
            pool.add(new WeightedItem(specialCatch, 40));
        }
        return pool;
    }

    /**
     * Quickly create an elemental material entry.
     */
    public static WeightedItem elementMaterial(ItemStack item, int weight) {
        return new WeightedItem(item, weight);
    }
}

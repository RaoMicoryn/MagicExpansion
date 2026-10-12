package io.Yomicer.magicExpansion.items.tools;

import io.Yomicer.magicExpansion.items.misc.Lure;
import io.Yomicer.magicExpansion.items.misc.WeightedItem;
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

public class FishingRod extends SlimefunItem implements RecipeDisplayItem {

    private final Map<Enchantment, Integer> enchantments;
    private final boolean glow;
    private final Map<String, List<WeightedItem>> lootTable;

    // ✅ All usable bait is listed here (material, display name, lore)
    private final List<ItemStack> USABLE_LURES;


    /**
     * Constructor
     *
     * @param itemGroup   Category
     * @param item        Item
     * @param recipeType  Recipe type
     * @param recipe      Recipe
     * @param enchantments Enchantments to add (enchantment -> level)
     * @param glow         Whether to show the enchantment glow (true = show, false = hide)
     */
    public FishingRod(ItemGroup itemGroup,
                      SlimefunItemStack item,
                      RecipeType recipeType,
                      ItemStack[] recipe,
                      Map<Enchantment, Integer> enchantments,
                      boolean glow,
                      Map<String, List<WeightedItem>> lootTable,
                      List<ItemStack> USABLE_LURES) {
        super(itemGroup, item, recipeType, recipe);
        this.enchantments = enchantments;
        this.glow = glow;
        this.lootTable = lootTable;
        this.USABLE_LURES = USABLE_LURES;
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


        return display;
    }
}

package io.Yomicer.magicExpansion.items.quickMachine;
import io.Yomicer.magicExpansion.utils.GiveItem;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import java.util.*;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;
import static io.Yomicer.magicExpansion.utils.CreateItem.createItem;

public class MagicExpansionMineralCave extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    public MagicExpansionMineralCave(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
    }



    @Nonnull
    @Override
    public ItemUseHandler getItemHandler() {

        return e -> {
            // Prevent the default action (placing a block or using the item)
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);

            // Get the player
            Player player = e.getPlayer();

            // Open the custom menu
            openCustomMenu(player);
        };
    }


    private void openCustomMenu(Player player) {
        // Create a new ChestMenu
        ChestMenu menu = new ChestMenu(getGradientName("Magic Mine"));
        // Keep track of clicked slots
        Set<Integer> clickedSlots = new HashSet<>();
        // Set up the menu slots
        for (int i = 0; i < 54; i++) {
            Random random = new Random();
            // Check if the 80% chance succeeds
            boolean isGenerated = random.nextDouble() < 0.8;
            if (isGenerated||i==53){
            menu.addItem(i, createItem(Material.STONE, getGradientName("Click to mine this stone")), (player1, slot, item, action) -> {

                // Check if this slot has already been clicked
                if (clickedSlots.contains(slot)) {
                    player1.sendMessage("§cYou’ve already mined this spot!");
                    return false; // Return false so the item isn't used up
                }
                // Replace this slot with regular cobblestone after the player clicks it
                ItemStack randomItem = getRandomItem(random);
                ItemStack treasureItem = randomItem.clone();
                ItemMeta meta = treasureItem.getItemMeta();
                meta.setDisplayName("§eTreasure: §b" + ItemStackHelper.getDisplayName(treasureItem));
                treasureItem.setItemMeta(meta);
                menu.replaceExistingItem(slot, treasureItem);
                GiveItem.giveOrDropItem(player1, randomItem);
                // Mark this slot as clicked
                clickedSlots.add(slot);
                player1.sendMessage("You mined the stone at slot " + slot+" stone！");
                return false; // Return false so the item isn't used up
            });
        }
        }

        // Set whether empty slots can be clicked
        menu.setEmptySlotsClickable(false);
        menu.setPlayerInventoryClickable(false);

        // Open the menu
        menu.open(player);
    }



    // Preset item list (static constant)
    private static final List<ItemStack> ITEMS;
    static {
        // Set up the item list
        ITEMS = new ArrayList<>();
        ITEMS.add(new ItemStack(Material.COBBLESTONE));
        ITEMS.add(new ItemStack(Material.IRON_INGOT));
        ITEMS.add(new ItemStack(Material.GOLD_INGOT));
        ITEMS.add(new ItemStack(Material.COPPER_INGOT));
        ITEMS.add(new ItemStack(Material.COAL));
        ITEMS.add(new ItemStack(Material.RAW_IRON));
        ITEMS.add(new ItemStack(Material.RAW_COPPER));
        ITEMS.add(new ItemStack(Material.RAW_GOLD));
        ITEMS.add(new ItemStack(Material.EMERALD));
        ITEMS.add(new ItemStack(Material.DIAMOND));
        ITEMS.add(new ItemStack(Material.LAPIS_LAZULI));
        ITEMS.add(new ItemStack(Material.QUARTZ));
        ITEMS.add(new ItemStack(Material.AMETHYST_SHARD));
        ITEMS.add(new ItemStack(Material.FLINT));
        ITEMS.add(new ItemStack(Material.BONE));
        ITEMS.add(new ItemStack(Material.CLAY));
        ITEMS.add(new ItemStack(Material.BONE_BLOCK));
        ITEMS.add(new ItemStack(Material.NETHERITE_SCRAP));
        ITEMS.add(new ItemStack(Material.SOUL_SAND));
        ITEMS.add(new ItemStack(Material.GOLD_NUGGET));
        ITEMS.add(new ItemStack(Material.NETHERRACK));
        ITEMS.add(SlimefunItems.TIN_INGOT);
        ITEMS.add(SlimefunItems.SILVER_INGOT);
        ITEMS.add(SlimefunItems.LEAD_INGOT);
        ITEMS.add(SlimefunItems.ALUMINUM_INGOT);
        ITEMS.add(SlimefunItems.ZINC_INGOT);
        ITEMS.add(SlimefunItems.MAGNESIUM_INGOT);
        ITEMS.add(SlimefunItems.MAGNESIUM_DUST);
        ITEMS.add(SlimefunItems.ZINC_DUST);
        ITEMS.add(SlimefunItems.ALUMINUM_DUST);
        ITEMS.add(SlimefunItems.LEAD_DUST);
        ITEMS.add(SlimefunItems.SILVER_DUST);
        ITEMS.add(SlimefunItems.TIN_DUST);
        ITEMS.add(SlimefunItems.COPPER_DUST);
        ITEMS.add(SlimefunItems.IRON_DUST);
        ITEMS.add(SlimefunItems.GOLD_DUST);

    }

    // Pick a random item
    private static ItemStack getRandomItem(Random random) {
        // Pick a random item
        return ITEMS.get(random.nextInt(ITEMS.size()));
    }



}

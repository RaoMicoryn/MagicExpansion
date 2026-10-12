package io.Yomicer.magicExpansion.items.electric.recipeMachine;

import io.Yomicer.magicExpansion.core.MagicExpansionItems;
import io.Yomicer.magicExpansion.items.abstracts.AbstractElectricRecipeMachine;
import io.Yomicer.magicExpansion.utils.itemUtils.getRandomItemFromGroup;
import io.Yomicer.magicExpansion.utils.log.Debug;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import static io.Yomicer.magicExpansion.Listener.SlimefunRegistryGiftBox.itemMapMihoyoHonkai;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;

public class RandomBoxMachine extends AbstractElectricRecipeMachine {
    private static final int[] INPUT_SLOTS = new int[] { 0,1,2,3, 9,10,11,12, 18,19,20,21, 27,28,29,30, 36,37,38,39, 45,46,47,48  };
    private static final int[] OUTPUT_SLOTS = new int[] { 5,6,7,8, 14,15,16,17, 23,24,25,26, 32,33,34,35, 41,42,43,44, 50,51,52,53 };

    private static final int[] INPUT_BORDER_SLOTS = new int[] { 13 };
    private static final int[] OUTPUT_BORDER_SLOTS = new int[] { 40 };
    private static final int[] BACKGROUND_SLOTS = new int[] { 4, 49 };

    private static final ItemStack PROGRESS_ITEM = new ItemStack(Material.SOUL_LANTERN);
    private static final ItemStack PROGRESS_STACK = new CustomItemStack(Material.SOUL_CAMPFIRE, getGradientName("Info"), getGradientName("Type: Processing Machine"), getGradientName("Addon: Magic"));

    public RandomBoxMachine(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

    }
    
    @Override
    public void postRegister() {
        registerDefaultRecipes();
    }

    protected void registerDefaultRecipes() {

    }

    // E2: Box-opening throttle counter. The opening logic runs at most once every 10 ticks, so boxes aren't opened every single tick
    private int openBoxTickCounter = 0;

    @Override
    public void tick(BlockMenu menu, Block b) {

        // E2: Throttle: open at most 1 box every 10 ticks
        openBoxTickCounter++;
        if (openBoxTickCounter < 10) {
            return;
        }
        openBoxTickCounter = 0;

        // E1: Power check. If there isn't enough energy (or no energy use is set), the machine doesn't work this tick, so it can't produce for free
        if (!takeCharge(b.getLocation())) {
            return;
        }

        int slot = containsTargetItem(menu, MagicExpansionItems.HONKAI_STAR_RAIL_BOX);
        // E3: Removed a dead branch left over from copy and paste. The second containsTargetItem lookup was still checking the same kind of box
        // (HONKAI_STAR_RAIL_BOX), and neither this class nor MagicExpansionItems has a second box constant,
        // so the else if branch could never be reached and was removed entirely
        if(slot!=-1){
            ItemStack gift = getRandomItemFromGroup.getRandomItemStack(itemMapMihoyoHonkai);
            if (gift != null && menu.fits(gift, getOutputSlots())) {
                craft(gift, menu, slot);
            }
        }


    }

    private void craft(ItemStack output, BlockMenu inv,int slot) {

        ItemStack box = inv.getItemInSlot(slot);

        if (box != null && box.getType() != Material.AIR) {
            inv.consumeItem(slot);
//            Debug.logInfo("Blind box used");
        }

        if (output != null && output.getType() != Material.AIR) {
            inv.pushItem(output, getOutputSlots());
//            Debug.logInfo("Gift generated");
        }
    }

    /**
     * Checks whether the given slots contain an ItemStack similar to the target item
     */
    public static int containsTargetItem(BlockMenu menu, ItemStack target) {
        for (int slot : INPUT_SLOTS) {
            ItemStack item = menu.getItemInSlot(slot);

            if (SlimefunUtils.isItemSimilar(item, target, true)) {
                return slot; // Found a matching item
            }
        }

        return -1; // Not found
    }



    @Override
	public List<ItemStack> getDisplayRecipes() {
		List<ItemStack> display = new ArrayList<>();
        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientName("Materials⇨"),getGradientName("The middle divider separates the recipes")));
        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientName("Output⇨"),getGradientName("The middle divider separates the recipes")));
        // Go through all recipes and build the display list
        for (MachineRecipe recipe : recipes) {
            ItemStack[] inputs = recipe.getInput();
            ItemStack[] outputs = recipe.getOutput();
            int productionTime = recipe.getTicks()/2;

            int maxItems = Math.max(inputs.length, outputs.length); // Get the largest item count
            for (int i = 0; i < maxItems; i++) {
                // Add the input item (with production time)
                if (i < inputs.length) {
                    display.add(addLore(inputs[i], "§7Production time: §e" + productionTime + " sec"));
                } else {
                    display.add(new ItemStack(Material.AIR)); // If there are no more input items, add AIR
                }

                // Add the output item (with production time)
                if (i < outputs.length) {
                    display.add(addLore(outputs[i], "§7Production time: §e" + productionTime + " sec (random drop)"));
                } else {
                    display.add(new ItemStack(Material.AIR)); // If there are no more output items, add AIR
                }
            }
            display.add(new CustomItemStack(Material.PINK_STAINED_GLASS_PANE, getGradientName("<== Recipe Divider =>>"),getGradientName("<== Input Materials =>>")));
            display.add(new CustomItemStack(Material.PINK_STAINED_GLASS_PANE, getGradientName("<== Recipe Divider =>>"),getGradientName("<== Output Materials =>>")));
            // Go through all recipes and build the display list
        }
        return display;
	}

    /**
     * Adds a description (lore) to an item
     */
    private ItemStack addLore(ItemStack item, String loreText) {
        ItemStack newItem = item.clone(); // Avoid changing the original item directly
        ItemMeta meta = newItem.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(loreText);
        meta.setLore(lore);
        newItem.setItemMeta(meta);
        return newItem;
    }
    protected int getProgressSlot() {
        return 31;
    }

	@Override
	protected void setupMenu(BlockMenuPreset preset) {

        preset.drawBackground(new CustomItemStack(Material.PINK_STAINED_GLASS_PANE," "), BACKGROUND_SLOTS);
        preset.drawBackground(new CustomItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE,getGradientName("←Input Slot")), INPUT_BORDER_SLOTS);
        preset.drawBackground(new CustomItemStack(Material.LIME_STAINED_GLASS_PANE,getGradientName("Output Slot→")), OUTPUT_BORDER_SLOTS);
    
        preset.addItem(getProgressSlot(), new CustomItemStack(Material.PINK_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());

        preset.addItem(22, PROGRESS_STACK, ChestMenuUtils.getEmptyClickHandler());

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

	@Override
	protected int[] getInputSlots() {
		return INPUT_SLOTS;
	}

	@Override
	protected int[] getOutputSlots() {
		return OUTPUT_SLOTS;
	}

  
    @Override
    public ItemStack getProgressBar() {
        return PROGRESS_ITEM;
    }
}
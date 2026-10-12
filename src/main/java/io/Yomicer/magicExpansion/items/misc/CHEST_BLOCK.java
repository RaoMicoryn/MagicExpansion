package io.Yomicer.magicExpansion.items.misc;

import io.Yomicer.magicExpansion.items.abstracts.AbstractElectricRecipeMachine;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;

public class CHEST_BLOCK extends AbstractElectricRecipeMachine {
    private static final int[] INPUT_SLOTS = new int[] { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52 };
    private static final int[] OUTPUT_SLOTS = new int[] { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52 };

    private static final int[] OUTPUT_BORDER_SLOTS = new int[] { 53 };

    private static final ItemStack PROGRESS_ITEM = new ItemStack(Material.SOUL_LANTERN);
    private static final ItemStack PROGRESS_STACK = new CustomItemStack(Material.SOUL_CAMPFIRE, getGradientName("Info"), getGradientName("Type: Container"), getGradientName("Addon: Magic"));

    public CHEST_BLOCK(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

    }
    
    @Override
    public void postRegister() {
        registerDefaultRecipes();
    }

    protected void registerDefaultRecipes() {

    }

    @Override
	public List<ItemStack> getDisplayRecipes() {
		List<ItemStack> display = new ArrayList<>();
        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientName("Input side⇨"),getGradientName("Every slot is an input slot")));
        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientName("Output side⇨"),getGradientName("Every slot is an output slot")));
        // Go through all recipes and generate the display content dynamically
        for (MachineRecipe recipe : recipes) {
            ItemStack[] inputs = recipe.getInput();
            ItemStack[] outputs = recipe.getOutput();
            int productionTime = recipe.getTicks()/2;

            int maxItems = Math.max(inputs.length, outputs.length); // Get max item amount
            for (int i = 0; i < maxItems; i++) {
                // Add input item (with production time)
                if (i < inputs.length) {
                    display.add(addLore(inputs[i], "§7Production time: §e" + productionTime + " sec"));
                } else {
                    display.add(new ItemStack(Material.AIR)); // If there are no more input items, add AIR
                }

                // Add output item (with production time)
                if (i < outputs.length) {
                    display.add(addLore(outputs[i], "§7Production time: §e" + productionTime + " sec"));
                } else {
                    display.add(new ItemStack(Material.AIR)); // If there are no more output items, add AIR
                }
            }
            display.add(new CustomItemStack(Material.PINK_STAINED_GLASS_PANE, getGradientName("<== Recipe divider =>>"),getGradientName("<== Input materials =>>")));
            display.add(new CustomItemStack(Material.PINK_STAINED_GLASS_PANE, getGradientName("<== Recipe divider =>>"),getGradientName("<== Output materials =>>")));
            // Go through all recipes and generate the display content dynamically
        }
        return display;
	}

    /**
     * Add a description (lore) to the item
     */
    private ItemStack addLore(ItemStack item, String loreText) {
        ItemStack newItem = item.clone(); // Prevent changing the original item directly
        ItemMeta meta = newItem.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add(loreText);
        meta.setLore(lore);
        newItem.setItemMeta(meta);
        return newItem;
    }
    protected int getProgressSlot() {
        return 53;
    }

	@Override
	protected void setupMenu(BlockMenuPreset preset) {

        preset.drawBackground(PROGRESS_STACK, OUTPUT_BORDER_SLOTS);
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
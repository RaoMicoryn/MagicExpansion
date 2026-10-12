package io.Yomicer.magicExpansion.items.electric.resourceGenerator;

import io.Yomicer.magicExpansion.items.abstracts.AbstractElectricResourceMachine;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;

public class ResourceRandomOneMachine extends AbstractElectricResourceMachine {

    // L4 fix: Share one static final Random so a new Random isn't created for every roll
    private static final Random RANDOM = new Random();

    private static final int[] BACKGROUND_SLOTS = new int[] { 0, 4, 8, 9, 13, 17 };
    private static final int[] OUTPUT_BORDER_SLOTS = new int[] { 10, 11, 12, 14, 15, 16};
    private static final int[] INPUT_BORDER_SLOTS = new int[] {1, 2, 3, 5, 6, 7};

    private static final int[] OUTPUT_SLOTS = new int[] { 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};

    private static final ItemStack PROGRESS_ITEM = new ItemStack(Material.SOUL_LANTERN);
    private ItemStack material;

    private static final ItemStack PROGRESS_STACK = new CustomItemStack(Material.SOUL_CAMPFIRE, getGradientName("Info"), getGradientName("Type: Resource Generator"), getGradientName("Addon: Magic"));



    public ResourceRandomOneMachine(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

    }




    @Override
    public void postRegister() {
        registerRecipe(getCraftSecond(), new ItemStack[] { this.material }, getItemStackOutputs());
    }
    
    @Override
    public void tick(BlockMenu menu, Block b) {
//            updateInfoStack(menu);
        super.tick(menu, b);
    }

    protected void addOutputs(BlockMenu menu, Block b, ItemStack[] outputs) {
        List<ItemStack> validOutputs = new ArrayList<>();

        // Collect all non-null outputs
        for (ItemStack output : outputs) {
            if (output != null) {
                validOutputs.add(output);
            }
        }

        // If there are any valid outputs
        if (!validOutputs.isEmpty()) {
            // If there is only one, output it directly
            if (validOutputs.size() == 1) {
                menu.pushItem(validOutputs.get(0).clone(), getOutputSlots());
            } else {
                // Otherwise pick one at random (L4 fix: use the shared static final Random instead of creating a new one every time)
                ItemStack randomOutput = validOutputs.get(RANDOM.nextInt(validOutputs.size()));
                menu.pushItem(randomOutput.clone(), getOutputSlots());
            }
        }
    }

    @Override
	public MachineRecipe findNextRecipe(BlockMenu menu) {
        
        int maxedSlots = 0; 
        for (int slots : getOutputSlots()) {
            ItemStack item = menu.getItemInSlot(slots);
            if (item != null && item.getMaxStackSize() == item.getAmount()) {
                maxedSlots += 1;
            }
        }

        if (maxedSlots == getOutputSlots().length) { return null; }

        // L1 fix: If the recipe list is empty, return null right away so recipes.get(0) doesn't throw IndexOutOfBoundsException
        if (recipes == null || recipes.isEmpty()) { return null; }

        MachineRecipe recipe = recipes.get(0);

        return recipe;

    }


	@Override
	public List<ItemStack> getDisplayRecipes() {
	    List<ItemStack> display = new ArrayList<>();

        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientName("Output⇨"),getGradientName("Speed⇨ ⚙ Once every " + getCraftSecond() + " s"),getGradientName("Energy use⇨ ⚡ "+ getEnergyConsumption()*2 +" J/s"),getGradientName("Produces one random item")));
        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientName("Output⇨"),getGradientName("Speed⇨ ⚙ Once every " + getCraftSecond() + " s"),getGradientName("Energy use⇨ ⚡ "+ getEnergyConsumption()*2 +" J/s"),getGradientName("Produces one random item")));
        // Add every item in the output array to the display list
        display.addAll(Arrays.asList(getItemStackOutputs()));

        return display;
	}

	@Override
	protected ItemStack getProgressBar() {
		return PROGRESS_ITEM;
	}

    protected int getProgressSlot() {
        return 13;
    }

	@Override
	protected void setupMenu(BlockMenuPreset preset) {
	    preset.drawBackground(new CustomItemStack(Material.PINK_STAINED_GLASS_PANE," "), BACKGROUND_SLOTS);
        preset.drawBackground(new CustomItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE," "), INPUT_BORDER_SLOTS);
        preset.drawBackground(new CustomItemStack(Material.LIME_STAINED_GLASS_PANE," "), OUTPUT_BORDER_SLOTS);
        preset.addItem(getProgressSlot(), new CustomItemStack(Material.PINK_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());

        preset.addItem(4, PROGRESS_STACK, ChestMenuUtils.getEmptyClickHandler());
 
    }

	@Override
	protected int[] getInputSlots() {
		return new int[] {};
	}

	@Override
	protected int[] getOutputSlots() {
		return OUTPUT_SLOTS;
	}
    
    @Override
    public boolean isSynchronized() {
        return false;
    }

    private static void updateInfoStack(BlockMenu menu) {
        Inventory inv = menu.toInventory();

        if (inv == null || inv.getViewers().isEmpty()) {
            return;
        }
//
//        ItemStack item = PROGRESS_STACK.clone();
//        ItemMeta meta = item.getItemMeta();
//
//        meta.setDisplayName(getGradientName("Info"));
//        ArrayList lore = new ArrayList<>();
//        lore.add(getGradientName("Production speed: " + getCraftSecondDisplay() + " sec"));
//        item.setLore(lore);
//        item.setItemMeta(meta);
//
//        menu.replaceExistingItem(4, item);
    }
}

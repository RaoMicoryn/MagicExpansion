package io.Yomicer.magicExpansion.items.electric.templateMachine;

import io.Yomicer.magicExpansion.items.abstracts.AbstractElectricRecipeMachine;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.inventory.InvUtils;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
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

import java.util.*;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;

public class ItemOriginBackTrackMachine extends AbstractElectricRecipeMachine {

    private static final int[] INPUT_SLOTS = new int[] { 0,1,2,3, 9,10,11,12, 18,19,20,21, 27,28,29,30, 36,37,38,39, 45,46,47,48  };
    private static final int[] OUTPUT_SLOTS = new int[] { 5,6,7,8, 14,15,16,17, 23,24,25,26, 32,33,34,35, 41,42,43,44, 50,51,52,53 };

    private static final int[] INPUT_BORDER_SLOTS = new int[] { 13 };
    private static final int[] OUTPUT_BORDER_SLOTS = new int[] { 40 };
    private static final int[] BACKGROUND_SLOTS = new int[] { 4, 49 };

    private static final ItemStack PROGRESS_ITEM = new ItemStack(Material.SOUL_LANTERN);
    private static final ItemStack PROGRESS_STACK = new CustomItemStack(Material.SOUL_CAMPFIRE, getGradientName("Info"), getGradientName("Type: Template Machine"), getGradientName("Addon: Magic"));

    public ItemOriginBackTrackMachine(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
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
        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientName("Inputs ⇨"),getGradientName("The divider line separates the recipes")));
        display.add(new CustomItemStack(Material.KNOWLEDGE_BOOK, getGradientName("Outputs ⇨"),getGradientName("The divider line separates the recipes")));
        // Go through all recipes and build the display items
        for (MachineRecipe recipe : recipes) {
            ItemStack[] inputs = recipe.getInput();
            ItemStack[] outputs = recipe.getOutput();
            int productionTime = recipe.getTicks()/2;

            int maxItems = Math.max(inputs.length, outputs.length); // Get the biggest item count
            for (int i = 0; i < maxItems; i++) {
                // Add input item
                if (i < inputs.length) {
                    if (i == 0) {
                        display.add(addLore(inputs[i], "§e§l☞The Origin Backtrack Machine doesn't use up any items when it runs☜"));
                    } else {
                        display.add(addLore(inputs[i], "§7Time: §e" + productionTime + " sec"));
                    }
                } else {
                    display.add(new ItemStack(Material.AIR)); // No input item, so fill with AIR
                }

                // Add output item (with production time)
                if (i < outputs.length) {
                    display.add(addLore(outputs[i], "§7Time: §e" + productionTime + " sec"));
                } else {
                    display.add(new ItemStack(Material.AIR)); // No more output items, so add AIR
                }
            }
            display.add(new CustomItemStack(Material.PINK_STAINED_GLASS_PANE, getGradientName("<== Recipe Divider =>>"),getGradientName("<== Inputs =>>")));
            display.add(new CustomItemStack(Material.PINK_STAINED_GLASS_PANE, getGradientName("<== Recipe Divider =>>"),getGradientName("<== Outputs =>>")));
            // Go through all recipes and build the display items
        }
        return display;
    }

    /**
     * Add lore to an item
     */
    private ItemStack addLore(ItemStack item, String loreText) {
        ItemStack newItem = item.clone(); // Don't change the original item directly
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
        preset.drawBackground(new CustomItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE,getGradientName("←Input")), INPUT_BORDER_SLOTS);
        preset.drawBackground(new CustomItemStack(Material.LIME_STAINED_GLASS_PANE,getGradientName("Output→")), OUTPUT_BORDER_SLOTS);

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

    protected void addOutputs(BlockMenu menu, Block b, ItemStack[] outputs) {
        ItemStack OriginItem = findFirstSfItem(menu);
        if (OriginItem != null) {
            SlimefunItem OriginSfItem = getByItem(OriginItem);
            ItemStack [] recipe = OriginSfItem.getRecipe().clone();
            if (recipe.length == 0) {
                return;
            }
            List<ItemStack> validItems = new ArrayList<>();
            for (ItemStack item : recipe) {
                if (item != null && !item.getType().isAir()) {
                    validItems.add(item);
                }
            }
            if (validItems.isEmpty()) {
                return;
            }
            Random random = new Random();
            ItemStack randomlySelected = validItems.get(random.nextInt(validItems.size()));
            menu.pushItem(randomlySelected.clone(), getOutputSlots());
        }
    }


    private ItemStack findFirstSfItem(BlockMenu menu) {
        for (int slot : getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);
            if (item != null) {
                if (SlimefunItem.getByItem(item) != null) {
                    return item.clone();
                }
            }
        }
        return null; // Not found
    }


    @Override
    public MachineRecipe findNextRecipe(BlockMenu menu) {
        Map<Integer, ItemStack> inv = new HashMap<>();

        for (int slot : getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);

            if (item != null) {
                inv.put(slot, ItemStackWrapper.wrap(item));
            }
        }

        int maxedSlots = 0;
        for (int slot : getOutputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);
            if (item != null && item.getAmount() == item.getMaxStackSize()) {
                maxedSlots += 1;
            }
        }

        if (maxedSlots == getOutputSlots().length) { return null; }

        ItemStack OriginItem = findFirstSfItem(menu);
        if (OriginItem != null) {
            SlimefunItem OriginSfItem = getByItem(OriginItem);
            ItemStack [] recipe = null;
            if (OriginSfItem != null) {
                recipe = OriginSfItem.getRecipe();
            }
            if (recipe != null && recipe.length == 0) {
                return null;
            }
            return new MachineRecipe(5, new ItemStack[]{new ItemStack(Material.BARRIER)}, new ItemStack[]{new ItemStack(Material.BARRIER)});
        }
        return null;
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
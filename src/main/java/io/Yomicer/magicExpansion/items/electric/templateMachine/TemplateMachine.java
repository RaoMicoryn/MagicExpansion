package io.Yomicer.magicExpansion.items.electric.templateMachine;

import io.Yomicer.magicExpansion.items.abstracts.AbstractElectricRecipeMachine;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
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
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;

public class TemplateMachine extends AbstractElectricRecipeMachine {

    private static final int[] INPUT_SLOTS = new int[] { 0,1,2,3, 9,10,11,12, 18,19,20,21, 27,28,29,30, 36,37,38,39, 45,46,47,48  };
    private static final int[] OUTPUT_SLOTS = new int[] { 5,6,7,8, 14,15,16,17, 23,24,25,26, 32,33,34,35, 41,42,43,44, 50,51,52,53 };

    private static final int[] INPUT_BORDER_SLOTS = new int[] { 13 };
    private static final int[] OUTPUT_BORDER_SLOTS = new int[] { 40 };
    private static final int[] BACKGROUND_SLOTS = new int[] { 4, 49 };

    private static final ItemStack PROGRESS_ITEM = new ItemStack(Material.SOUL_LANTERN);
    private static final ItemStack PROGRESS_STACK = new CustomItemStack(Material.SOUL_CAMPFIRE, getGradientName("Info"), getGradientName("Type: Doesn't use up everything"), getGradientName("Addon: Magic"));

    public TemplateMachine(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
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
                        display.add(addLore(inputs[i], "§e§l☞This item is not used up in this recipe☜"));
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

        Map<Integer, Integer> found = new HashMap<>();

        for (MachineRecipe recipe : recipes) {
            for (ItemStack input : recipe.getInput()) {
                for (int slot : getInputSlots()) {
                    if (SlimefunUtils.isItemSimilar(inv.get(slot), input, true)) {
                        found.put(slot, input.getAmount());
                        break;
                    }
                }
            }

            if (found.size() == recipe.getInput().length) {
                if(!InvUtils.fitAll(menu.toInventory(), recipe.getOutput(), getOutputSlots())) {
                    return null;
                }

                // Find the slot that holds the first input item
                ItemStack firstInput = recipe.getInput()[0];
                Integer firstSlotToSkip = null;
                for (Map.Entry<Integer, Integer> entry : found.entrySet()) {
                    ItemStack current = inv.get(entry.getKey());
                    if (SlimefunUtils.isItemSimilar(current, firstInput, true)) {
                        firstSlotToSkip = entry.getKey();
                        break;
                    }
                }

                for (Map.Entry<Integer, Integer> entry : found.entrySet()) {
                    if (entry.getKey().equals(firstSlotToSkip)) {
                        continue; // Don't use up the first input item
                    }
                    menu.consumeItem(entry.getKey(), entry.getValue());
                }

                return recipe;
            } else {
                found.clear();
            }
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
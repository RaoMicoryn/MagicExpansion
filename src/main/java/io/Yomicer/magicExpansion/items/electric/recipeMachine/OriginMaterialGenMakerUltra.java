package io.Yomicer.magicExpansion.items.electric.recipeMachine;

import io.Yomicer.magicExpansion.MagicExpansion;
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
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static io.Yomicer.magicExpansion.core.MagicExpansionItems.ORIGIN_MATERIAL_GEN;
import static io.Yomicer.magicExpansion.core.MagicExpansionItems.ORIGIN_MATERIAL_GEN_ULTRA;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientNameVer2;

public class OriginMaterialGenMakerUltra extends AbstractElectricRecipeMachine {
    private static final int[] INPUT_SLOTS = new int[] { 0,1,2, 9,10,11,12, 18,19,20,21, 27,28,29,30, 36,37,38,39, 45,46,47,48  };
    private static final int[] OUTPUT_SLOTS = new int[] { 6,7,8, 14,15,16,17, 23,24,25,26, 32,33,34,35, 41,42,43,44, 50,51,52,53 };

    private static final int[] INPUT_BORDER_SLOTS = new int[] { 13 };
    private static final int[] OUTPUT_BORDER_SLOTS = new int[] { 40 };

    private static final int[] ORIGIN_MATERIAL_BORDER_SLOTS = new int[] { 3, 5 };
    private static final int ORIGIN_MATERIAL_SLOTS = 4;

    private static final int[] BACKGROUND_SLOTS = new int[] { 49 };

    private static final ItemStack PROGRESS_ITEM = new ItemStack(Material.SOUL_LANTERN);
    private static final ItemStack PROGRESS_STACK = new CustomItemStack(Material.SOUL_CAMPFIRE, getGradientName("Info"), getGradientName("Type: Processing Machine"), getGradientName("Addon: Magic"));

    public OriginMaterialGenMakerUltra(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
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
                    display.add(addLore(outputs[i], "§7Production time: §e" + productionTime + " sec"));
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

    @Nullable
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

            ItemStack item = menu.getItemInSlot(ORIGIN_MATERIAL_SLOTS);
            if (item == null) {
                return null;
            }

            ItemStack[] outputs = new ItemStack[]{OriginMaterialGen(item)};

            if (found.size() == recipe.getInput().length) {
                if(!InvUtils.fitAll(menu.toInventory(), outputs, getOutputSlots())) {  //recipe.getOutput() needs to be changed
                    return null;
                }

                if(item == null){
                    return null;
                }

                for (Map.Entry<Integer, Integer> entry : found.entrySet()) {
                    menu.consumeItem(entry.getKey(), entry.getValue());
                }

                menu.consumeItem(ORIGIN_MATERIAL_SLOTS, 1);
                MachineRecipe newRecipe = new MachineRecipe(recipe.getTicks()/2, recipe.getInput(), outputs);

                return newRecipe;
            } else {
                found.clear();
            }
        }

        return null;
    }

    private ItemStack OriginMaterialGen(ItemStack item) {
        String n = ItemStackHelper.getDisplayName(item);
        ItemStack itemOutput = ORIGIN_MATERIAL_GEN_ULTRA.clone();
        ItemMeta meta = itemOutput.getItemMeta();
        if (meta != null) {
            String originalName = meta.getDisplayName();
            meta.setDisplayName(originalName + " - " + n);
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add(getGradientNameVer2( "Current item: ") + n);
            meta.setLore(lore);

            // [Changed]: Serialize the whole item into a Base64 string, keeping all NBT, enchantments, Slimefun properties, etc.
            String serializedItem = serializeItemStack(item);

            NamespacedKey key = new NamespacedKey(MagicExpansion.getInstance(), "origin_material");
            // Store the full serialized item data instead of just the Material name
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, serializedItem);
            itemOutput.setItemMeta(meta);
        }
        return itemOutput;
    }

    /**
     * Serializes an ItemStack into a Base64 string
     */
    private String serializeItemStack(ItemStack item) {
        if (item == null) return "";
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(item);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Deserializes a Base64 string back into an ItemStack
     * (Call this method elsewhere when you need the original item data)
     */
    public static ItemStack deserializeItemStack(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            return (ItemStack) dataInput.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }



//    protected void addOutputs(BlockMenu menu, Block b, ItemStack[] outputs) {
//        ItemStack item = menu.getItemInSlot(ORIGIN_MATERIAL_SLOTS);
//        if (item == null || !item.hasItemMeta()) {
//            return;
//        }
//        menu.pushItem(OriginMaterialGen(item), getOutputSlots());
//    }


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

        preset.drawBackground(new CustomItemStack(Material.YELLOW_STAINED_GLASS_PANE,getGradientName("Source Item Slot")), ORIGIN_MATERIAL_BORDER_SLOTS);

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
package io.Yomicer.magicExpansion.items.electric;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.items.abstracts.MenuBlock;
import io.Yomicer.magicExpansion.items.misc.fish.FishAttributeGenerator;
import io.Yomicer.magicExpansion.items.misc.fish.Gen2Fish;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientNameVer2;
import static io.Yomicer.magicExpansion.utils.Utils.doGlow;

/**
 * Breeding Pool: uses two gen 2 fish and makes one baby fish.
 * <p>
 * Crossbreeding rules (layer 2):
 * <ul>
 *   <li>Baby elements = parent 1 ×0.4 + parent 2 ×0.4, with a 20% chance per element of a ±0.05 mutation (then normalized so the sum = 1);</li>
 *   <li>Baby quality = (parent 1 + parent 2) / 2 ± 0.05, clamped between 0.7 and 2.5;</li>
 *   <li>Baby gene type: 30% chance to copy one parent, 70% chance random (accel / synth 50/50);</li>
 *   <li>Baby fish type (look) is picked randomly from the father or the mother.</li>
 * </ul>
 */
public class FishBreedPool extends MenuBlock {

    // ===== 27 slots (3×9), laid out like the user's picture =====
    // Row 1, slot 5: info slot (shows the breeding rules and the quality formula)
    private static final int SLOT_INFO = 4;
    // Row 2, slot 3 / slot 7: input slots (put the father/mother fish here), both are empty slots for fish
    private static final int SLOT_PARENT_1 = 11;
    private static final int SLOT_PARENT_2 = 15;
    // Row 2, slot 5: breed button (click to start breeding), material = vanilla bell
    private static final int SLOT_BUTTON = 13;
    // Row 3, slot 5: output slot (take out the baby fish)
    private static final int SLOT_OUTPUT = 22;

    // Row 2 blue glass pane label slots (slot 2/4/6/8; material and name use the gen 2 gradient color, lore is filled in by the user)
    private static final int SLOT_DECOR_A = 10;
    private static final int SLOT_DECOR_B = 12;
    private static final int SLOT_DECOR_C = 14;
    private static final int SLOT_DECOR_D = 16;

    public FishBreedPool(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
    }

    @Override
    protected void setup(BlockMenuPreset preset) {
        // Menu size is 27 slots (3×9)
        preset.setSize(27);

        // Background: light gray glass panes fill every slot except the function slots / info slot / button / colored glass panes
        // Function slots (11/15/22) stay empty, so you can put fish in and take fish out
        preset.drawBackground(new CustomItemStack(Material.WHITE_STAINED_GLASS_PANE, " "), new int[]{
                0, 1, 2, 3, 5, 6, 7, 8,
                9, 17,
                18, 19, 20, 24, 25, 26
        });

        // ===== Row 1, slot 5 (index 4): info slot — breeding rules + quality formula =====
        preset.addItem(SLOT_INFO, new CustomItemStack(doGlow(Material.BOOK),
                        getGradientNameVer2("Breeding Pool · Crossbreeding Rules"),
                        getGradientNameVer2("Baby elements = Parent 1 ×0.4 + Parent 2 ×0.4"),
                        getGradientNameVer2("20% chance of a ±5% mutation on each element"),
                        getGradientNameVer2("Baby quality = parents' average ±0.05"),
                        getGradientNameVer2("Gene type: 30% from one parent, 70% random"),
                        getGradientNameVer2("Baby fish type is picked from one parent at random")),
                (p, s, i, a) -> false);

        // Each lore line is wrapped with getGradientNameVer2 to get the "Magic gen 2 gradient color"
        preset.addItem(SLOT_DECOR_A, new CustomItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                        getGradientNameVer2("Parent Fish 1"),
                        getGradientNameVer2("")),
                (p, s, i, a) -> false);
        preset.addItem(SLOT_DECOR_B, new CustomItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                        getGradientNameVer2(""),
                        getGradientNameVer2("")),
                (p, s, i, a) -> false);
        preset.addItem(SLOT_DECOR_C, new CustomItemStack(Material.BLUE_STAINED_GLASS_PANE,
                        getGradientNameVer2("Parent Fish 2"),
                        getGradientNameVer2("")),
                (p, s, i, a) -> false);
        preset.addItem(SLOT_DECOR_D, new CustomItemStack(Material.BLUE_STAINED_GLASS_PANE,
                        getGradientNameVer2(""),
                        getGradientNameVer2("")),
                (p, s, i, a) -> false);

        // ===== Orange glass panes on both sides of the output slot in row 3 (index 21/23), each slot is separate =====
        preset.addItem(21, new CustomItemStack(Material.ORANGE_STAINED_GLASS_PANE,
                        getGradientNameVer2(""),
                        getGradientNameVer2("")),
                (p, s, i, a) -> false);   // 21 orange
        preset.addItem(23, new CustomItemStack(Material.ORANGE_STAINED_GLASS_PANE,
                        getGradientNameVer2(""),
                        getGradientNameVer2("")),
                (p, s, i, a) -> false);   // 23 orange

        // ===== Row 2, slot 5 (index 13): breed button = vanilla bell =====
        preset.addItem(SLOT_BUTTON, new CustomItemStack(doGlow(Material.BELL),
                        getGradientNameVer2("Start Breeding"),
                        getGradientNameVer2("Uses 2 parent fish, makes 1 baby fish"),
                        getGradientNameVer2("Takes 2 seconds")),
                (p, s, item, a) -> {
                    onBreedClick(p);
                    return false;
                });
    }

    @Override
    protected int[] getInputSlots() {
        return new int[]{SLOT_PARENT_1, SLOT_PARENT_2};
    }

    @Override
    protected int[] getOutputSlots() {
        return new int[]{SLOT_OUTPUT};
    }

    @Override
    protected int[] getInputSlots(DirtyChestMenu menu, ItemStack item) {
        return getInputSlots();
    }

    /** Click the breed button: check first, and if everything is OK, start the 2 second crafting, then make the baby fish when the time is up */
    private void onBreedClick(Player player) {
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof BlockMenu menu)) {
            return;
        }

        ItemStack f1 = menu.getItemInSlot(SLOT_PARENT_1);
        ItemStack f2 = menu.getItemInSlot(SLOT_PARENT_2);
        if (!FishAttributeGenerator.isGen2Fish(f1) || !FishAttributeGenerator.isGen2Fish(f2)) {
            player.sendMessage(getGradientNameVer2("Breeding Pool: Put in two gen 2 fish"));
            return;
        }

        // Output slot must be empty to start crafting
        ItemStack occupied = menu.getItemInSlot(SLOT_OUTPUT);
        if (occupied != null && !occupied.getType().isAir()) {
            player.sendMessage(getGradientNameVer2("Breeding Pool: Output slot is full, take out the baby fish first"));
            return;
        }

        // Check if the user has permission to use this machine (optional, the default MenuBlock behavior is fine)

        // Lock the button as "crafting" so it can't be clicked again during the 2 seconds
        menu.replaceExistingItem(SLOT_BUTTON, craftingItem());
        player.sendMessage(getGradientNameVer2("Breeding... wait 2 seconds"));

        // After 2 seconds (40 ticks), do the real crossbreeding
        Bukkit.getScheduler().runTaskLater(MagicExpansion.getInstance(), () -> {
            // Read the parent fish from the menu again, in case the player took or swapped them during crafting
            ItemStack cur1 = menu.getItemInSlot(SLOT_PARENT_1);
            ItemStack cur2 = menu.getItemInSlot(SLOT_PARENT_2);
            BlockMenu fresh = menu;
            if (!FishAttributeGenerator.isGen2Fish(cur1) || !FishAttributeGenerator.isGen2Fish(cur2)) {
                player.sendMessage(getGradientNameVer2("Breeding Pool: Parent fish were removed, cancelled"));
                resetButton(fresh);
                return;
            }
            completeBreed(player, fresh, cur1, cur2);
        }, 40L);
    }

    /** Button icon while crafting */
    private CustomItemStack craftingItem() {
        return new CustomItemStack(doGlow(Material.CLOCK),
                getGradientNameVer2("Breeding..."),
                getGradientNameVer2("Please wait 2 seconds"));
    }

    /** Put the button back to how it was at the start */
    private void resetButton(BlockMenu menu) {
        menu.replaceExistingItem(SLOT_BUTTON, new CustomItemStack(doGlow(Material.BELL),
                getGradientNameVer2("Start Breeding"),
                getGradientNameVer2("Uses 2 parent fish, makes 1 baby fish"),
                getGradientNameVer2("Output slot must be empty to breed"),
                getGradientNameVer2("Wait 2 seconds after clicking")));
    }

    /** Do the real crossbreeding: read both parent fish → work out the baby → put it in the output slot → clear the input slots → reset the button */
    private void completeBreed(Player player, BlockMenu menu, ItemStack f1, ItemStack f2) {
        Gen2Fish type1 = FishAttributeGenerator.getType(f1);
        Gen2Fish type2 = FishAttributeGenerator.getType(f2);
        double[] e1 = FishAttributeGenerator.getElements(f1);
        double[] e2 = FishAttributeGenerator.getElements(f2);
        if (type1 == null || type2 == null || e1 == null || e2 == null) {
            player.sendMessage(getGradientNameVer2("Breeding Pool: Parent fish data is broken, can't breed"));
            resetButton(menu);
            return;
        }

        // Baby fish type (look): picked randomly from the father or the mother
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Gen2Fish childType = rnd.nextBoolean() ? type1 : type2;

        // Baby elements = parent 1 ×0.4 + parent 2 ×0.4; 20% chance per element of a ±0.05 mutation (breed clamps and normalizes it inside)
        double[] childElements = new double[5];
        for (int i = 0; i < 5; i++) {
            childElements[i] = e1[i] * 0.4 + e2[i] * 0.4;
            if (rnd.nextDouble() < 0.20) {
                childElements[i] += (rnd.nextDouble() - 0.5) * 0.1;
            }
        }

        // Baby quality = parents' average ± 0.05 (clamped to 0.7~2.5 inside)
        double q1 = FishAttributeGenerator.getQuality(f1);
        double q2 = FishAttributeGenerator.getQuality(f2);
        double childQuality = (q1 + q2) / 2.0 + (rnd.nextDouble() - 0.5) * 0.1;

        // Gene type: 30% copy one parent (old fish with no gene fall back to the default of their fish type), 70% random
        Gen2Fish.Trait t1 = FishAttributeGenerator.getTrait(f1);
        Gen2Fish.Trait t2 = FishAttributeGenerator.getTrait(f2);
        Gen2Fish.Trait effT1 = t1 != null ? t1 : type1.getDefaultTrait();
        Gen2Fish.Trait effT2 = t2 != null ? t2 : type2.getDefaultTrait();
        Gen2Fish.Trait childTrait;
        if (rnd.nextDouble() < 0.30) {
            childTrait = rnd.nextBoolean() ? effT1 : effT2;
        } else {
            childTrait = rnd.nextBoolean() ? Gen2Fish.Trait.ACCEL : Gen2Fish.Trait.SYNTH;
        }

        // Check again that the output slot is still empty (it might have been used during crafting)
        ItemStack occupied = menu.getItemInSlot(SLOT_OUTPUT);
        if (occupied != null && !occupied.getType().isAir()) {
            player.sendMessage(getGradientNameVer2("Breeding Pool: Output slot is full, take out the baby fish first"));
            resetButton(menu);
            return;
        }

        // Pass in the parents' fish type IDs so the baby's lore can look up the parents' names
        ItemStack child = FishAttributeGenerator.breed(childType, childElements, childQuality,
                childTrait, type1.getId(), type2.getId());
        menu.replaceExistingItem(SLOT_OUTPUT, child);
        menu.replaceExistingItem(SLOT_PARENT_1, null);
        menu.replaceExistingItem(SLOT_PARENT_2, null);
        resetButton(menu);
        player.sendMessage(getGradientNameVer2("Breeding done!"));
    }
}
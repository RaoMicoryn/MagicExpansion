package io.Yomicer.magicExpansion;

import io.Yomicer.magicExpansion.core.MagicExpansionItems;
import io.Yomicer.magicExpansion.items.misc.food.CloudBread;
import io.Yomicer.magicExpansion.items.misc.food.FarmHavestBread;
import io.Yomicer.magicExpansion.items.misc.food.HolyPie;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.Yomicer.magicExpansion.utils.itemUtils.sfItemUtils;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.UnplaceableBlock;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

import static io.Yomicer.magicExpansion.MagicExpansionItemSetup.magicexpansionfood;
import static io.Yomicer.magicExpansion.MagicExpansionItemSetup.magicexpansionfoodresource;
import static io.Yomicer.magicExpansion.core.MagicExpansionItems.HARVEST_WHEAT;
import static io.Yomicer.magicExpansion.utils.itemUtils.sfItemUtils.sfItemAmount;

public class MagicExpansionFoodSetup {


    public static void setup(@Nonnull MagicExpansion plugin) {


        // Food items below
        //Harvest Bread
        new FarmHavestBread(magicexpansionfood, MagicExpansionItems.FARM_HARVEST_BREAD, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                HARVEST_WHEAT, HARVEST_WHEAT, HARVEST_WHEAT,
                null, null ,null,
                null, null,null
        }).register(plugin);

        //Holy Pie
        new HolyPie(magicexpansionfood, MagicExpansionItems.HOLY_PIE, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                new ItemStack(Material.MILK_BUCKET), new ItemStack(Material.MILK_BUCKET), new ItemStack(Material.MILK_BUCKET),
                MagicExpansionItems.WHEAT_FLOUR, MagicExpansionItems.WHEAT_FLOUR ,MagicExpansionItems.WHEAT_FLOUR,
                new ItemStack(Material.PUMPKIN), new ItemStack(Material.PUMPKIN),new ItemStack(Material.PUMPKIN)
        }).register(plugin);

        //Cloud Bread
        new CloudBread(magicexpansionfood, MagicExpansionItems.CLOUD_BREAD, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                new ItemStack(Material.MILK_BUCKET), new ItemStack(Material.MILK_BUCKET), new ItemStack(Material.MILK_BUCKET),
                MagicExpansionItems.WHEAT_FLOUR, MagicExpansionItems.WHEAT_FLOUR ,MagicExpansionItems.WHEAT_FLOUR,
                new ItemStack(Material.EGG), new ItemStack(Material.EGG),new ItemStack(Material.EGG)
        },sfItemAmount(MagicExpansionItems.CLOUD_BREAD,3)).register(plugin);


        //Harvest Wheat
        new UnplaceableBlock(magicexpansionfoodresource, MagicExpansionItems.HARVEST_WHEAT, RecipeType.NULL, new ItemStack[] {
                null, null ,null,
                null, new CustomItemStack(new ItemStack(Material.IRON_HOE), ColorGradient.getGradientNameVer2("Get this by harvesting fully grown crops"),
                ColorGradient.getGradientNameVer2("Fully grown sweet wheat may drop this")), null,
                null, null,null
        }).register(plugin);
        //Wheat Flour
        new UnplaceableBlock(magicexpansionfoodresource, MagicExpansionItems.WHEAT_FLOUR, RecipeType.GRIND_STONE, new ItemStack[] {
                MagicExpansionItems.HARVEST_WHEAT, null ,null,
                null, null, null,
                null, null,null
        },sfItemAmount(MagicExpansionItems.WHEAT_FLOUR,4)).register(plugin);
        //
        new UnplaceableBlock(magicexpansionfoodresource, MagicExpansionItems.HARVEST_RICE, RecipeType.NULL, new ItemStack[] {
                null, null ,null,
                null, new CustomItemStack(new ItemStack(Material.IRON_HOE), ColorGradient.getGradientNameVer2("Get this by harvesting fully grown crops"),
                ColorGradient.getGradientNameVer2("Fully grown hybrid rice may drop this")), null,
                null, null,null
        }).register(plugin);
        //
        new UnplaceableBlock(magicexpansionfoodresource, MagicExpansionItems.DREAM_KERNEL, RecipeType.NULL, new ItemStack[] {
                null, null ,null,
                null, new CustomItemStack(new ItemStack(Material.IRON_HOE), ColorGradient.getGradientNameVer2("Get this by harvesting fully grown crops"),
                ColorGradient.getGradientNameVer2("Fully grown hybrid rice may drop this")), null,
                null, null,null
        }).register(plugin);
        //
        new UnplaceableBlock(magicexpansionfoodresource, MagicExpansionItems.ORE_DUST_CRYSTAL, RecipeType.NULL, new ItemStack[] {
                null, null ,null,
                null, new CustomItemStack(new ItemStack(Material.IRON_HOE), ColorGradient.getGradientNameVer2("Get this by harvesting fully grown crops"),
                ColorGradient.getGradientNameVer2("Fully grown ore dust crops may drop this")), null,
                null, null,null
        }).register(plugin);
        //
        new UnplaceableBlock(magicexpansionfoodresource, MagicExpansionItems.ORE_INGOT_CRYSTAL, RecipeType.NULL, new ItemStack[] {
                null, null ,null,
                null, new CustomItemStack(new ItemStack(Material.IRON_HOE), ColorGradient.getGradientNameVer2("Get this by harvesting fully grown crops"),
                ColorGradient.getGradientNameVer2("Fully grown ore ingot crops may drop this")), null,
                null, null,null
        }).register(plugin);
        //
        new UnplaceableBlock(magicexpansionfoodresource, MagicExpansionItems.ORE_MIX_CRYSTAL, RecipeType.NULL, new ItemStack[] {
                null, null ,null,
                null, new CustomItemStack(new ItemStack(Material.IRON_HOE), ColorGradient.getGradientNameVer2("Get this by harvesting fully grown crops"),
                ColorGradient.getGradientNameVer2("Fully grown alloy ingot crops may drop this")), null,
                null, null,null
        }).register(plugin);



    }


}

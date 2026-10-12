package io.Yomicer.magicExpansion.items.quickMachine;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.Yomicer.magicExpansion.Listener.SlimefunRegistryListener.ENHANCED_CRAFTING_TABLE_RECIPES;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;
import static io.Yomicer.magicExpansion.utils.quickMachine.QuickMachineUtils.addAvailableRecipesToMenu;

public class MagicExpansionQuickEnhancedCraftingTable extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {


    public MagicExpansionQuickEnhancedCraftingTable(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
    }



    @Nonnull
    @Override
    public ItemUseHandler getItemHandler() {
        return e->{
            // Prevent the default action (placing a block or using the item)
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);

            // Get the player
            Player player = e.getPlayer();
            // Open the menuand load recipes
            ChestMenu menu = new ChestMenu(getGradientName("Magic Enhanced Crafting Table"));
            addAvailableRecipesToMenu(player, menu,0, ENHANCED_CRAFTING_TABLE_RECIPES, RecipeType.ENHANCED_CRAFTING_TABLE);

            // Set whether empty slots can be clicked
            menu.setEmptySlotsClickable(false);
            menu.setPlayerInventoryClickable(false);
            // Show the menu to the player
            menu.open(player);


        };
    }
}

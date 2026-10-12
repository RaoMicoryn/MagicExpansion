package io.Yomicer.magicExpansion.items.quickMachine;

import io.Yomicer.magicExpansion.utils.quickMachine.QuickMachineMBUtile;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

import static io.Yomicer.magicExpansion.Listener.SlimefunRegistryFinalized.ORE_CRUSHER_RECIPES;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;

public class MagicExpansionQuickOreCrusher extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    public MagicExpansionQuickOreCrusher(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
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
            ChestMenu menu = new ChestMenu(getGradientName("Magic Ore Crusher"));
            QuickMachineMBUtile.addAvailableRecipesToMenu(player, menu,0, ORE_CRUSHER_RECIPES);

            // Set whether empty slots can be clicked
            menu.setEmptySlotsClickable(false);
            menu.setPlayerInventoryClickable(false);
            // Show the menu to the player
            menu.open(player);


        };
    }


}

package io.Yomicer.magicExpansion.items.misc.magicAlter;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MagicWand extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    public MagicWand(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public void preRegister() {
        ItemStack itemStack = getItem();
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorGradient.getRandomGradientName("Magic Altar Wand"));
            meta.getPersistentDataContainer().set(
                    MagicExpansion.getInstance().getPluginInitializer().getAltarWandKey(),
                    PersistentDataType.BYTE,
                    (byte) 1
            );
            List<String> lore = new ArrayList<>();
            lore.add(ColorGradient.getRandomGradientName("Right-click the altar dispenser to start crafting"));
            lore.add(ColorGradient.getRandomGradientName("You need the right block layout and item recipe"));
            meta.setLore(lore);

            itemStack.setItemMeta(meta);
        }
    }

    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return e -> {
            // Stop the default behavior (placing a block or using the item)
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);
        };
    }
}
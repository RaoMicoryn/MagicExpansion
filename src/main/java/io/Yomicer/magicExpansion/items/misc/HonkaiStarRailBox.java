package io.Yomicer.magicExpansion.items.misc;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSpawnReason;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.utils.FireworkUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static io.Yomicer.magicExpansion.Listener.SlimefunRegistryGiftBox.itemMapMihoyoHonkai;

public class HonkaiStarRailBox extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    // Fix (M): Random changed to a shared static instance, to avoid creating a new Random
    private static final Random RANDOM = new Random();

    public HonkaiStarRailBox(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }



    // Get a random item's ItemStack
    public ItemStack getRandomItemStack(Map<String, SlimefunItem> itemMap) {
        if (itemMap.isEmpty()) {
            return null;
        }

        Collection<SlimefunItem> values = itemMap.values();
        List<SlimefunItem> list = new ArrayList<>(values);

        // Fix (M): Use a shared static Random
        SlimefunItem randomItem = list.get(RANDOM.nextInt(list.size()));
        return randomItem.getItem().clone();
    }

    // Spawn a random item at the given location
    public void spawnRandomItem(Location location, Player player) {
        ItemStack gift = getRandomItemStack(itemMapMihoyoHonkai);

        if (gift != null) {
            SlimefunUtils.spawnItem(location, gift, ItemSpawnReason.CHRISTMAS_PRESENT_OPENED, true, player);
            player.sendMessage("§aCongrats! You opened a Honkai: Star Rail blind box and got " + gift.getItemMeta().getDisplayName() + "!");
        }
    }



    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return (e) -> {
            e.cancel();
            e.getClickedBlock().ifPresent((block) -> {
                // Fix (M): Check the prize pool is not empty before using up the blind box, so an item isn't swallowed when the pool is empty
                ItemStack gift = getRandomItemStack(itemMapMihoyoHonkai);
                if (gift == null) {
                    e.getPlayer().sendMessage("§cThe prize pool is empty, the blind box wasn't used up, can't open any item!");
                    return;
                }

                if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
                    ItemUtils.consumeItem(e.getItem(), false);
                }

                FireworkUtils.launchRandom(e.getPlayer(), 3);
                Block b = block.getRelative(e.getClickedFace());
                SlimefunUtils.spawnItem(b.getLocation(), gift, ItemSpawnReason.CHRISTMAS_PRESENT_OPENED, true, e.getPlayer());
                e.getPlayer().sendMessage("§aCongrats! You opened a Honkai: Star Rail blind box and got " + gift.getItemMeta().getDisplayName() + "!");

            });
        };
    }



}
package io.Yomicer.magicExpansion.utils;

import io.Yomicer.magicExpansion.utils.log.Debug;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MagicExpansionSlimefunItemCache {
    // Static cache for all Slimefun items
    private static final List<SlimefunItem> SFITEMCACHE = new ArrayList<>();
    private static final Random RANDOM = new Random(); // Avoid creating Random more than once

    static {
        // Preload all Slimefun items when the class loads
        loadAllSlimefunItems();
    }

    /**
     * Load all Slimefun items into the cache
     */
    private static void loadAllSlimefunItems() {
        SFITEMCACHE.addAll(Slimefun.getRegistry().getAllSlimefunItems());
    }

    /**
     * Get all Slimefun items in the cache
     *
     * @return List of all Slimefun items
     */
    public static List<SlimefunItem> getAllSlimefunItems() {
        return SFITEMCACHE;
    }

    /**
     * Clear the cache and reload all Slimefun items
     */
    public static void reloadCache() {
        SFITEMCACHE.clear();
        loadAllSlimefunItems();
    }

    public static SlimefunItem getRandomSlimefunItem() {
        if (SFITEMCACHE.isEmpty()) {
            return null;
        }
        return SFITEMCACHE.get(RANDOM.nextInt(SFITEMCACHE.size()));
    }

    public static ItemStack getRandomItemStack() {
        SlimefunItem item = getRandomSlimefunItem();
        return item != null ? item.getItem().clone() : null;
    }
}

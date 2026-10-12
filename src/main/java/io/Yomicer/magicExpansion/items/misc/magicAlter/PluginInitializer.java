package io.Yomicer.magicExpansion.items.misc.magicAlter;

import io.Yomicer.magicExpansion.MagicExpansion;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;

public class PluginInitializer {

    private final MagicExpansion plugin;
    private MagicAltarManager altarManager;
    private RecipeBookManager recipeBookManager;
    private NamespacedKey altarWandKey;
    private NamespacedKey recipeBookKey;

    public PluginInitializer(MagicExpansion plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        // Set up the keys
        altarWandKey = new NamespacedKey(plugin, "altar_wand");
        recipeBookKey = new NamespacedKey(plugin, "recipe_book");

        // Set up the managers
        altarManager = new MagicAltarManager(plugin);
        recipeBookManager = new RecipeBookManager(plugin);

        // Register the event listeners
        Bukkit.getPluginManager().registerEvents(new MagicAltarListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(new RecipeBookListener(plugin), plugin);

        // Register the commands
        plugin.getCommand("mxwand").setExecutor(new MagicAltarCommand(plugin));
        plugin.getCommand("mxalter").setExecutor(new MxEnchantCommand(plugin));

        // Load recipes
        altarManager.loadRecipes();
    }

    public MagicAltarManager getAltarManager() {
        return altarManager;
    }

    public RecipeBookManager getRecipeBookManager() {
        return recipeBookManager;
    }

    public NamespacedKey getAltarWandKey() {
        return altarWandKey;
    }

    public NamespacedKey getRecipeBookKey() {
        return recipeBookKey;
    }
}
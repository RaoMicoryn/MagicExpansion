package io.Yomicer.magicExpansion;

import io.Yomicer.magicExpansion.Listener.RecipePreLoader;
import io.Yomicer.magicExpansion.Listener.SlimefunRegistryFinalized;
import io.Yomicer.magicExpansion.Listener.SlimefunRegistryGiftBox;
import io.Yomicer.magicExpansion.Listener.SlimefunRegistryListener;
import io.Yomicer.magicExpansion.Listener.bossListener.BasicBossAttackListener;
import io.Yomicer.magicExpansion.Listener.bossListener.BasicBossDropListener;
import io.Yomicer.magicExpansion.Listener.fishingListener.GuidePoolButtonListener;
import io.Yomicer.magicExpansion.Listener.AccelerationUseListener;
import io.Yomicer.magicExpansion.Listener.GuideVirtualGroupClickListener;
import io.Yomicer.magicExpansion.Listener.fishingListener.PlayerFishingListener;
import io.Yomicer.magicExpansion.Listener.fishingListener.PlayerFishingWaterCloudListener;
import io.Yomicer.magicExpansion.Listener.magicItemEffectManager.ArrowHitLocationListener;
import io.Yomicer.magicExpansion.Listener.magicItemEffectManager.ItemEffectAttackListener;
import io.Yomicer.magicExpansion.Listener.miscListener.ItemFrameListener;
import io.Yomicer.magicExpansion.Listener.PlayerCleanupListener;
import io.Yomicer.magicExpansion.Listener.worldListener.Events;
import io.Yomicer.magicExpansion.items.misc.CargoFragmentDistributor;
import io.Yomicer.magicExpansion.items.misc.PageChestListener;
import io.Yomicer.magicExpansion.items.misc.DrawMachine;
import io.Yomicer.magicExpansion.items.misc.magicAlter.PluginInitializer;
import io.Yomicer.magicExpansion.specialActions.Command.*;
import io.Yomicer.magicExpansion.Listener.magicItemEffectManager.ItemEffectKillListener;
import io.Yomicer.magicExpansion.utils.FishingGuideMenu;
import io.Yomicer.magicExpansion.utils.Language;
import io.Yomicer.magicExpansion.utils.WaterCloudHookManager;
import io.Yomicer.magicExpansion.utils.VirtualPlayerManager;
import io.Yomicer.magicExpansion.utils.aiManager.AIManager;
import io.Yomicer.magicExpansion.utils.shop.BlackMarketManager;
import io.Yomicer.magicExpansion.utils.shop.ShopCommand;
import io.Yomicer.magicExpansion.utils.shop.ShopGUI;
import io.Yomicer.magicExpansion.utils.shop.ShopManager;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import net.guizhanss.guizhanlibplugin.updater.GuizhanUpdater;
import org.bukkit.plugin.java.JavaPlugin;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.logging.Level;

import static io.Yomicer.magicExpansion.items.misc.PortableCargoTransporter.onPluginDisable;

public class MagicExpansion extends JavaPlugin implements SlimefunAddon {
    public static boolean testmod=false;
    public static boolean clearConfig=false;
    public static boolean testmode(){
        return testmod;
    }
    private static MagicExpansion instance;
    private PluginInitializer pluginInitializer;




    @Override
    public void onEnable() {
        // Lombok @SneakyThrows was expanded manually: catch errors around startup,
        // item notes，item notes"partially enabled"state（Fixfix 1 ）
        try {
            onEnableInternal();
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "A serious error occurred while enabling Magic Expansion. The plugin has been disabled!", t);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    // Actual enable logic (moved from the original onEnable without changing behavior)
    private void onEnableInternal() throws Exception {
        // Plugin startup logic
        instance = this;
        saveDefaultConfig();
        // Read something from your config.yml
        Config cfg = new Config(this);

        getLogger().info("§bLoading Magic Expansion!");

        if (!getServer().getPluginManager().isPluginEnabled("GuizhanLibPlugin")) {
            getLogger().log(Level.SEVERE, "This plugin needs GuizhanLibPlugin to run!");
            getLogger().log(Level.SEVERE, "Download it here: https://50l.cc/gzlib");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (cfg.getBoolean("options.auto-update") && getDescription().getVersion().startsWith("Build ")) {
            getLogger().info("§bChecking for updates!");
            GuizhanUpdater.start(this, getFile(), "Yomicer", "MagicExpansion", "master");
            getLogger().info("§bUpdate complete!");
        }else{
            getLogger().info("§bAuto-update is disabled!");
        }
        ConfigLoader.load(this);
        Language.loadConfig(ConfigLoader.LANGUAGE);
        getLogger().info("§bLanguage files loaded!");

        // Magic Altar
        pluginInitializer = new PluginInitializer(this);
        pluginInitializer.initialize();
        getLogger().info("Magic 2.0 - Magic Altar is enabled!");

        // Registering Items
        MagicExpansionItemSetup.setup(this);
        MagicExpansionRecipeMachineSetup.setup(this);
        MagicExpansionPowerMachineSetup.setup(this);
        MagicExpansionQuickMachineSetup.setup(this);
        MagicExpansionFoodSetup.setup(this);
        getLogger().info("§bItems registered!");


        getLogger().info("§bLoading AI chat...");
        // Create and enable the AI module
        AIManager aiManager = new AIManager(this);
        aiManager.onEnable();  // ✅ Start the AI module
        getLogger().info("✅ Magic 2.0 AI chat is ready! Use /mxai chaton to turn it on.");


        // Registering Command
        this.getCommand("magicexpansion").setExecutor(new MagicExpansionCommand());
        this.getCommand("mxw").setExecutor(new WorldCommand(this));
        this.getCommand("mxf").setExecutor(new FishingGuideCommand());
        this.getCommand("mxf").setTabCompleter(new FishingGuideCommand());
        this.getCommand("mxai").setExecutor(new AIChat(aiManager));
        this.getCommand("magicfish").setExecutor(new MagicFishCommand());
        ShopCommand shopCommand = new ShopCommand();
        this.getCommand("magicshop").setExecutor(shopCommand);
        this.getCommand("mxfishmode").setExecutor(new FishModeCommand());
//        this.getCommand("mxai").setTabCompleter(new AIChat());

        // Create the folder for saved maps
        File mapsDir = new File(getDataFolder(), "maps");
        if (!mapsDir.exists()) {
            mapsDir.mkdirs();
        }
        getLogger().info("§bCommands registered");
        // Register event listeners
        getServer().getPluginManager().registerEvents(new SlimefunRegistryFinalized(), this);
        getServer().getPluginManager().registerEvents(new SlimefunRegistryListener(), this);
        getServer().getPluginManager().registerEvents(new RecipePreLoader(), this);
        getServer().getPluginManager().registerEvents(new SlimefunRegistryGiftBox(), this);
        getServer().getPluginManager().registerEvents(new ItemEffectAttackListener(), this);
        getServer().getPluginManager().registerEvents(new ItemEffectKillListener(), this);
        getServer().getPluginManager().registerEvents(new ArrowHitLocationListener(), this);
        getServer().getPluginManager().registerEvents(new BasicBossAttackListener(), this);
        getServer().getPluginManager().registerEvents(new BasicBossDropListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerFishingListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerFishingWaterCloudListener(), this);
        getServer().getPluginManager().registerEvents(new GuidePoolButtonListener(), this);
        getServer().getPluginManager().registerEvents(new GuideVirtualGroupClickListener(), this);
        getServer().getPluginManager().registerEvents(new AccelerationUseListener(), this);
        getServer().getPluginManager().registerEvents(new Events(), this);
        getServer().getPluginManager().registerEvents(new ItemFrameListener(), this);
        getServer().getPluginManager().registerEvents(new PageChestListener(), this);
        getServer().getPluginManager().registerEvents(aiManager, this);
        ShopManager.load();
        getServer().getPluginManager().registerEvents(new ShopGUI(), this);
        // Fix：Register a cleanup listener to clear session data when a player logs out
        getServer().getPluginManager().registerEvents(new PlayerCleanupListener(), this);

        // New Water Cloud fishing scheduler (second-based state machine + hook movement)
        WaterCloudHookManager.startTicking(this);
        BlackMarketManager.init();
        getLogger().info("Portable shop system loaded!");
        getLogger().info("§bEvent listeners registered!");












        getLogger().info("§bMagic Expansion enabled!");










    }

    @Override
    public void onDisable() {
        if (pluginInitializer != null) {
            pluginInitializer.getAltarManager().cancelAllTasks();
        }
        getLogger().info("Magic 2.0 - Magic Altar is disabled!");
        DrawMachine.cleanupAllHolograms();
        getLogger().info("All lottery machine holograms have been cleared!");
        VirtualPlayerManager.shutdown(); // G1v3：Clean up virtual players used by interactive bots (including removing Citizens NPCs)

        if (CargoFragmentDistributor.globalTickTask != null) {
            CargoFragmentDistributor.globalTickTask.cancel();
            CargoFragmentDistributor.globalTickTask = null;
        }
        CargoFragmentDistributor.machineStates.clear();
        getLogger().info("All Aether Chest Transporter tasks have stopped!");

        //Portable Aether Chest Transporter
        onPluginDisable();
        ShopManager.saveAll();
        WaterCloudHookManager.stopTicking();


        // Plugin shutdown logic
        getLogger().info("§bMagic Expansion has been disabled!");
    }
    public PluginInitializer getPluginInitializer() {
        return pluginInitializer;
    }
    @Nonnull
    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Nullable
    @Override
    public String getBugTrackerURL() {
        return "";
    }

    public static MagicExpansion getInstance() {
        return instance;
    }
}

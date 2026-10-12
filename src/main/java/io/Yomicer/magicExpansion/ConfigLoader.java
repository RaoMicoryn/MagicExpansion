package io.Yomicer.magicExpansion;

import com.google.common.base.Charsets;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;

public class ConfigLoader {


    public static boolean TESTMODE = MagicExpansion.testmode();

    public static void load(Plugin plugin) {
        ConfigLoader.plugin=plugin;
        init();
        //final File scAddonFile = new File(plugin.getDataFolder(), "language.yml");
        //copyFile(scAddonFile, "language");
        CONFIG=loadExternalConfig("config");
        INNERCONFIG=loadInternalConfig("config");
        if(INNERCONFIG.getBoolean("options.test")) {
            MagicExpansion.testmod=true;
            TESTMODE=true;
            // Fix(P2)：Logger.getLogger the parameter is"name"not a message，the old code would not print anything；use the main class logger
            MagicExpansion.getInstance().getLogger().info("Addon is running on TEST MODE");
        }
        if(INNERCONFIG.getBoolean("options.clear-old-config")) {
            // Fix(P1)：The clear-old-config branch set testmod by mistake; it should set clearConfig to mark old config for clearing
            MagicExpansion.clearConfig=true;
        }
        LANGUAGE=loadInternalConfig("language");   //new Config(plugin,"language.yml");

    }
    public static Plugin plugin;
    public static Config CONFIG;
    public static Config INNERCONFIG;
    public static Config LANGUAGE;
    public static Config SERVER_CONFIG;
    public static void init() {
        SERVER_CONFIG=new Config(plugin);
    }
    public static void copyFile(File file, String name) {
        if(MagicExpansion.clearConfig){

            try{
                Files.delete(file.toPath());
            }catch(Throwable e){
                // Fix(P2)：Logger.getLogger was used incorrectly; use the main class logger to show warnings
                MagicExpansion.getInstance().getLogger().warning("[TEST MODE] FAILED TO DELETE FILE: "+file.getAbsolutePath());
            }
        }
        if (!file.exists()) {
            try {
                if(!file.toPath().getParent().toFile().exists()) {
                    Files.createDirectories(file.toPath().getParent());
                }
                Files.copy(plugin.getClass().getResourceAsStream("/"+ name + ".yml"),file.toPath());
            } catch (Throwable e) {

                // Fix(P2)：Logger.getLogger was used incorrectly; use the main class logger to show warnings
                MagicExpansion.getInstance().getLogger().warning("Could not find the default config file. Creating an empty file instead.");
                try{
                    Files.createDirectories(file.toPath().getParent());
                    Files.createFile(file.toPath());
                }catch (IOException e1){
                    // Fix(P2)：Logger.getLogger was used incorrectly; use the main class logger to show warnings
                    MagicExpansion.getInstance().getLogger().warning("Failed to create an empty config file!");
                }
            }

        }
    }
    public static Config loadInternalConfig(String name){
        FileConfiguration config = new YamlConfiguration();
        // Fix(P3)：getResourceAsStream may return null, so throw a clear error instead of causing a later NPE
        InputStream in = plugin.getClass().getResourceAsStream("/"+ name + ".yml");
        if (in == null) {
            throw new RuntimeException("Internal config resource not found: /" + name + ".yml (please check that the plugin was packaged correctly)");
        }
        // Fix(P3)：Use try-with-resources to close the stream and avoid leaks
        try (Reader reader = new InputStreamReader(in, Charsets.UTF_8)) {
            config.load(reader);
            config.getString("options.test");

        }catch (Throwable e){
            // Fix(P3)：Throw a RuntimeException with the cause on load failure, so the main class can disable the plugin safely;
            // Returning null could cause an NPE and leave the plugin "partially enabled"state
            throw new RuntimeException("Failed to load internal config " + name + ".yml: " + e.getMessage(), e);
        }
        return new Config(null,config);
    }
    public static Config loadExternalConfig(String name){
        FileConfiguration config = new YamlConfiguration();
        final File cfgFile = new File(plugin.getDataFolder(), "%s.yml".formatted(name));
        copyFile(cfgFile, name);
        return new Config(plugin, "%s.yml".formatted(name));
    }




}

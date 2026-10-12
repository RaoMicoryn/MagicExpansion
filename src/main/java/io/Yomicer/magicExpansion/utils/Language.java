package io.Yomicer.magicExpansion.utils;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.utils.itemUtils.newItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Language {



    public static final String LANGUAGE_NAMESPACE = "zh_CN";
    public static final HashMap<String, Object> LANGUAGE = new HashMap<>() {{
    }};

    public static HashMap<String, Object> getLanguage() {
        return LANGUAGE;
    }

    public static String get(String key) {
        String s = (String) getLanguage().get(key);
        return s != null ? s : key;
    }

    public static List<String> getList(String key) {
        List<String> ls = (List<String>) getLanguage().get(key);
        return ls != null ? ls : new ArrayList<>();
    }

    public static boolean loadConfig(Config cfg) {
        // Load language data
        Set<String> keys = cfg.getKeys(LANGUAGE_NAMESPACE);
        if (keys.isEmpty()) return false; // Return false if no language data is available

        for (String ns : keys) {
            loadRecursively(cfg, ns); // Load language data recursively
        }

        // Log the result and return true
        // Fix: Logger.getLogger("message text") was misused (the argument is a logger name, not a message); use the main class logger instead
        MagicExpansion.getInstance().getLogger().info("MagicExpansionLanguage configuration loaded successfully~");
        return true;
    }

    public static void loadRecursively(Config config, String path) {
        Set<String> keys = config.getKeys(String.join(".", LANGUAGE_NAMESPACE, path));
        for (String key : keys) {
            String nextPath = path + "." + key;
            if (key.endsWith("lan")) {
                LANGUAGE.put(path, config.getValue(String.join(".", LANGUAGE_NAMESPACE, nextPath)));
            } else if (key.endsWith("key") || key.endsWith("enable")) {
                // Ignore these keys
            } else if (key.endsWith("Lore") || key.endsWith("Name")) {
                LANGUAGE.put(newItem.concat(path, ".", key), config.getValue(String.join(".", LANGUAGE_NAMESPACE, path, key)));
            } else {
                loadRecursively(config, nextPath);
            }
        }
    }













}

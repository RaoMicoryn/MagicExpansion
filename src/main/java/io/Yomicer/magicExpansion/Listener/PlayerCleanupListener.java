package io.Yomicer.magicExpansion.Listener;

import io.Yomicer.magicExpansion.Listener.magicItemEffectManager.ItemEffectAttackListener;
import io.Yomicer.magicExpansion.Listener.weaponApply.SlownessManager;
import io.Yomicer.magicExpansion.Listener.worldListener.Events;
import io.Yomicer.magicExpansion.items.misc.magicAlter.RecipeBookManager;
import io.Yomicer.magicExpansion.items.misc.weapon.StarShardsSword;
import io.Yomicer.magicExpansion.items.tools.CustomSequenceTool;
import io.Yomicer.magicExpansion.items.tools.ItemNameTag;
import io.Yomicer.magicExpansion.utils.aiManager.AIManager;
import io.Yomicer.magicExpansion.utils.shop.BlackMarketManager;
import io.Yomicer.magicExpansion.utils.shop.ShopGUI;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Listener that cleans up data when a player leaves:
 * When a player leaves, call each module's static cleanup(UUID) method.
 * Clear session data so offline players aren't left in module maps and cause memory leaks.
 */
public class PlayerCleanupListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Fix: use a separate try-catch for each module so one cleanup error won't stop the others.
        safeCleanup("worldListener.Events", () -> Events.cleanup(uuid));
        safeCleanup("RecipeBookManager", () -> RecipeBookManager.cleanup(uuid));
        // StarShardsSword.cleanup is handled by another provider (this class only calls it).
        safeCleanup("StarShardsSword", () -> StarShardsSword.cleanup(uuid));
        // SlownessManager.cleanup is handled by another provider (this class only calls it).
        safeCleanup("SlownessManager", () -> SlownessManager.cleanup(uuid));
        safeCleanup("CustomSequenceTool", () -> CustomSequenceTool.cleanup(uuid));
        safeCleanup("ShopGUI", () -> ShopGUI.cleanup(uuid));
        safeCleanup("BlackMarketManager", () -> BlackMarketManager.cleanup(uuid));
        safeCleanup("AIManager", () -> AIManager.cleanup(uuid));
//        safeCleanup("ItemNameTag", () -> ItemNameTag.cleanup(uuid));
        safeCleanup("ItemEffectAttackListener", () -> ItemEffectAttackListener.cleanup(uuid));
    }

    // Safely run one cleanup task: log errors without stopping cleanup for other modules.
    private void safeCleanup(String module, Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.WARNING, "Magic Expansion module " + module + " failed to clean up player data after logout: " + t.getMessage());
        }
    }
}

package io.Yomicer.magicExpansion.specialActions.Command;

import io.Yomicer.magicExpansion.items.tools.FishingRodWaterCloud;
import io.Yomicer.magicExpansion.utils.WaterCloudHookManager;
import io.Yomicer.magicExpansion.utils.WaterCloudRodProficiency;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

/**
 * /mxfishmode <true|false>
 * Switch the fishing system for Water Cloud fishing rods:
 * true = New fishing system (state-machine checks, separate from vanilla); false = old system (vanilla checks)
 * After switching, refresh the reward lore on all Water Cloud rods in online players’ inventories
 */
public class FishModeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command,
                             @Nonnull String label, @Nonnull String[] args) {
        // [Fix A] Permission check: non-player senders like the console are allowed; players need magicexpansion.fishmode
        if ((sender instanceof Player) && !sender.hasPermission("magicexpansion.fishmode")) {
            sender.sendMessage("§cYou do not have permission to use this command! (Needs magicexpansion.fishmode)");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("§eUsage: /mxfishmode <true|false>");
            sender.sendMessage("§7true  = Turn on the new Water Cloud fishing system (state-machine checks)");
            sender.sendMessage("§7false = Turn off the new system and use the old one");
            return true;
        }
        boolean enable = Boolean.parseBoolean(args[0]);
        WaterCloudHookManager.setEnabled(enable);

        // Refresh reward lore on Water Cloud rods in online players’ inventories right after switching (the rewards follow the selected system)
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && SlimefunItem.getByItem(item) instanceof FishingRodWaterCloud) {
                    WaterCloudRodProficiency.updateRewardLore(item);
                }
            }
        }

        sender.sendMessage(enable
                ? "§b✦ The new Water Cloud fishing system is now on. All Water Cloud rods now use state-machine checks."
                : "§7The new Water Cloud fishing system is now off. The old system is back.");
        return true;
    }
}

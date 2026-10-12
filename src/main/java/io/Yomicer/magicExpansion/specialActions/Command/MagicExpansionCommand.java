package io.Yomicer.magicExpansion.specialActions.Command;

import io.Yomicer.magicExpansion.utils.MagicExpansionSlimefunItemCache;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MagicExpansionCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show help if no arguments are given
            sender.sendMessage("§a/magicexpansion reload slimefun §f- Reload all Slimefun items");
            return true;
        }

        // Check permission (OP required)
        if (!sender.isOp()) {
            sender.sendMessage("§cOnly admins can use this command!");
            return true;
        }

        // Read the subcommand
        switch (args[0].toLowerCase()) {
            case "reload":
                if (args.length == 2 && args[1].equalsIgnoreCase("slimefun")) {
                    // Clear the cache and reload all Slimefun items
                    MagicExpansionSlimefunItemCache.reloadCache();
                    sender.sendMessage("§aCache cleared and all Slimefun items reloaded!");
                } else {
                    sender.sendMessage("§cUsage: /magicexpansion reload slimefun - Reload MagicExpansion’s Slimefun item cache");
                }
                break;

            default:
                sender.sendMessage("§cUnknown subcommand! Type /magicexpansion for help.");
                break;
        }

        return true;
    }



}

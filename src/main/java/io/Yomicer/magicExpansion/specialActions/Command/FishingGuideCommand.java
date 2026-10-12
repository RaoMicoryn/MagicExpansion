package io.Yomicer.magicExpansion.specialActions.Command;

import io.Yomicer.magicExpansion.MagicExpansionItemSetup;
import io.Yomicer.magicExpansion.core.MagicExpansionItems;
import io.Yomicer.magicExpansion.items.misc.fish.FishingBook;
import io.Yomicer.magicExpansion.utils.FishingGuideMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FishingGuideCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "open_guide":
                // Guide menu: /mxf open_guide <ZhiMeng|ShuiYunJian>
                if (args.length < 2) {
                    player.sendMessage("§eUsage: /mxf open_guide <ZhiMeng|ShuiYunJian>");
                    player.sendMessage("§7ZhiMeng     - Open the Dream Weaver fishing guide");
                    player.sendMessage("§7ShuiYunJian - Open the Water Cloud fishing guide");
                    break;
                }
                switch (args[1].toLowerCase()) {
                    case "zhimeng" -> {
                        FishingGuideMenu.openMainMenu(player);
                        player.sendMessage("§aDream Weaver fishing guide opened!");
                    }
                    case "shuiyunjian" -> player.sendMessage("§bThe Water Cloud fishing guide is still in the works. Stay tuned!");
                    default -> player.sendMessage("§cUnknown guide: " + args[1] + " §7(Options: ZhiMeng / ShuiYunJian)");
                }
                break;

            case "guide":
                // Guide book: /mxf guide <ZhiMeng|ShuiYunJian>
                if (args.length < 2) {
                    player.sendMessage("§eUsage: /mxf guide <ZhiMeng|ShuiYunJian>");
                    player.sendMessage("§7ZhiMeng     - Get the Dream Weaver guide book");
                    player.sendMessage("§7ShuiYunJian - Get the Water Cloud guide book");
                    break;
                }
                switch (args[1].toLowerCase()) {
                    case "zhimeng" -> {
                        giveItem(player, MagicExpansionItems.FISHING_BOOK);
                        player.sendMessage("§aYou got the Dream Weaver guide book!");
                    }
                    case "shuiyunjian" -> {
                        giveItem(player, MagicExpansionItems.FISHING_BOOK_BETWEEN_WATER_CLOUD);
                        player.sendMessage("§aYou got the Water Cloud guide book!");
                    }
                    default -> player.sendMessage("§cUnknown guide book: " + args[1] + " §7(Options: ZhiMeng / ShuiYunJian)");
                }
                break;

            default:
                sendUsage(player);
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Complete the first argument
            String[] subCommands = {"open_guide", "guide"};
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("guide") || args[0].equalsIgnoreCase("open_guide"))) {
            // Complete the second argument: guide or guide book
            String[] guides = {"ZhiMeng", "ShuiYunJian"};
            for (String guide : guides) {
                if (guide.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(guide);
                }
            }
        }

        return completions;
    }

    /**
     * Show command usage
     */
    private void sendUsage(Player player) {
        player.sendMessage("§6=== Fishing Guide Commands ===");
        player.sendMessage("§a/mxf open_guide <ZhiMeng|ShuiYunJian> §7- Open the fishing guide menu");
        player.sendMessage("§a/mxf guide ZhiMeng §7- Get the Dream Weaver guide book");
        player.sendMessage("§a/mxf guide ShuiYunJian §7- Get the Water Cloud guide book");
        player.sendMessage("§6===================");
    }

    /**
     * Give an item (drop it on the ground if the inventory is full)
     */
    private void giveItem(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.sendMessage("§eYour inventory is full, so the item was dropped on the ground!");
        } else {
            player.getInventory().addItem(item);
        }
    }
}
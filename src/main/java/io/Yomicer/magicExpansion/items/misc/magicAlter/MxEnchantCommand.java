package io.Yomicer.magicExpansion.items.misc.magicAlter;

import io.Yomicer.magicExpansion.MagicExpansion;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MxEnchantCommand implements TabExecutor {

    private final MagicExpansion plugin;

    public MxEnchantCommand(MagicExpansion plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§6/mxalter guide §7- Get the recipe book");
            player.sendMessage("§6/mxalter open_guide §7- Open the recipe book");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "guide":
                // Fix (N): if addItem returns leftover items, drop them on the ground, so a full inventory doesn't eat items
                java.util.Map<Integer, ItemStack> overflow =
                        player.getInventory().addItem(plugin.getPluginInitializer().getRecipeBookManager().createRecipeBook());
                for (ItemStack rest : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), rest);
                }
                player.sendMessage("§aYou got the Magic Altar Recipe Guide!");
                break;

            case "open_guide":
                plugin.getPluginInitializer().getRecipeBookManager().openRecipeBook(player);
                break;

            default:
                player.sendMessage("§cUnknown subcommand! Use /mxenchant to see the available commands");
                break;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Tab-complete the first argument
            String[] subCommands = {"open_guide", "guide"};
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        }

        return completions;
    }
}
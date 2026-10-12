package io.Yomicer.magicExpansion.items.misc.magicAlter;

import io.Yomicer.magicExpansion.MagicExpansion;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MagicAltarCommand implements TabExecutor {

    private final MagicExpansion plugin;

    public MagicAltarCommand(MagicExpansion plugin) {
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
            player.sendMessage("§6/mxwand wand §7- Get the Magic Altar Wand");
            return true;
        }

        if (args[0].equalsIgnoreCase("wand") && player.hasPermission("mxwand.wand")) {
            // Fix (N): if addItem returns leftover items, drop them on the ground, so a full inventory doesn't eat items
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(createAltarWand());
            for (ItemStack rest : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
            }
            player.sendMessage("§aYou got the Magic Altar Wand!");
            return true;
        }

        player.sendMessage("§cUnknown subcommand! Use /mxwand to see the available commands");
        return true;
    }

    // Create the special item (Altar Wand)
    private ItemStack createAltarWand() {
        ItemStack wand = new ItemStack(org.bukkit.Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6Magic Altar Wand");
            meta.getPersistentDataContainer().set(
                    plugin.getPluginInitializer().getAltarWandKey(),
                    PersistentDataType.BYTE,
                    (byte) 1
            );

            // Add the lore description
            List<String> lore = new ArrayList<>();
            lore.add("§7Right-click the altar dispenser to start crafting");
            lore.add("§7You need the right block layout and item recipe");
            meta.setLore(lore);

            wand.setItemMeta(meta);
        }

        return wand;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Tab-complete the first argument
            String[] subCommands = {"wand"};
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        }

        return completions;
    }
}
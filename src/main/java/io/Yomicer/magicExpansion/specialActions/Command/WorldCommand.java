package io.Yomicer.magicExpansion.specialActions.Command;

import io.Yomicer.magicExpansion.Listener.worldListener.Events;
import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.utils.MapUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorldCommand implements CommandExecutor, TabCompleter {

    private final MagicExpansion plugin;

    public WorldCommand(MagicExpansion plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        // Check permissions
        if (!player.isOp() && !player.hasPermission("MagicExpansion.use")) {
            player.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§bUsage: /mxw <select|exitselect|exportmap|paste|clear|ctrlv>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "select":
                Events.selectMode.put(player.getUniqueId(), true);
                player.sendMessage("§aYou are now in selection mode.");
                break;

            case "exitselect":
                Events.selectMode.remove(player.getUniqueId());
                player.sendMessage("§aExited selection mode.");
                break;

            case "exportmap":
                MapUtils.exportMap(player);
                break;

            case "paste":
                // [Fix B1] The paste command needs magicexpansion.world.modify permission. Show a message and return if missing.
                if (!player.hasPermission("magicexpansion.world.modify")) {
                    player.sendMessage("§cYou do not have permission to edit the world! (Needs magicexpansion.world.modify)");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /mxw paste <filename>");
                    return true;
                }
                MapUtils.pasteMap(player, args[1]);
                // [Fix B3] Log the player name and map file they pasted
                Bukkit.getLogger().info("[MagicExpansion] " + player.getName()
                        + " pasted map file '" + args[1] + "'");
                break;

            case "clear":
                // [Fix B1] The clear command needs magicexpansion.world.modify permission. Show a message and return if missing.
                if (!player.hasPermission("magicexpansion.world.modify")) {
                    player.sendMessage("§cYou do not have permission to edit the world! (Needs magicexpansion.world.modify)");
                    return true;
                }
                clearSelectedArea(player);
                break;

            case "ctrlv":
                // [Fix B1] ctrlv also changes the world, so it needs magicexpansion.world.modify permission, just like paste
                if (!player.hasPermission("magicexpansion.world.modify")) {
                    player.sendMessage("§cYou do not have permission to edit the world! (Needs magicexpansion.world.modify)");
                    return true;
                }
                pasteSelectedAreaToTargetBlock(player);
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown command!");
        }
        return true;
    }


    /**
     * Clear the blocks in the selected area
     */
    private void clearSelectedArea(Player player) {
        Location p1 = Events.point1.get(player.getUniqueId());
        Location p2 = Events.point2.get(player.getUniqueId());

        if (p1 == null || p2 == null) {
            player.sendMessage(ChatColor.RED + "Please select two points first!");
            return;
        }

        // [Fix B2] Make sure both selected points are in the same world. Otherwise, show a message and return.
        if (p1.getWorld() == null || !p1.getWorld().equals(p2.getWorld())) {
            player.sendMessage(ChatColor.RED + "The two selected points must be in the same world!");
            return;
        }

        // [Fix B2] Check the area size. It must not exceed 32*256*32; otherwise, show a message and return.
        int sizeX = Math.abs(p1.getBlockX() - p2.getBlockX()) + 1;
        int sizeY = Math.abs(p1.getBlockY() - p2.getBlockY()) + 1;
        int sizeZ = Math.abs(p1.getBlockZ() - p2.getBlockZ()) + 1;
        if ((long) sizeX * sizeY * sizeZ > 32L * 256L * 32L) {
            player.sendMessage(ChatColor.RED + "Selected area too large! Max: 32x256x32, current: "
                    + sizeX + "x" + sizeY + "x" + sizeZ);
            return;
        }

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = p1.getWorld().getBlockAt(x, y, z);
                    block.setType(org.bukkit.Material.AIR); // Set the block to air
                }
            }
        }

        player.sendMessage("§aCleared the selected area.");

        // [Fix B3] Log the player name and the area coordinates
        Bukkit.getLogger().info("[MagicExpansion] " + player.getName() + " cleared area in world '"
                + p1.getWorld().getName() + "' from (" + minX + "," + minY + "," + minZ
                + ") to (" + maxX + "," + maxY + "," + maxZ + ")");
    }

    /**
     * Paste the selected area one block above the block the player is looking at
     */
    private void pasteSelectedAreaToTargetBlock(Player player) {
        Location p1 = Events.point1.get(player.getUniqueId());
        Location p2 = Events.point2.get(player.getUniqueId());

        if (p1 == null || p2 == null) {
            player.sendMessage("§cPlease select two points first!");
            return;
        }

        // [Fix B2] Make sure both selected points are in the same world. Otherwise, show a message and return.
        if (p1.getWorld() == null || !p1.getWorld().equals(p2.getWorld())) {
            player.sendMessage("§cThe two selected points must be in the same world!");
            return;
        }

        // [Fix B2] Check the area size. It must not exceed 32*256*32; otherwise, show a message and return.
        int sizeX = Math.abs(p1.getBlockX() - p2.getBlockX()) + 1;
        int sizeY = Math.abs(p1.getBlockY() - p2.getBlockY()) + 1;
        int sizeZ = Math.abs(p1.getBlockZ() - p2.getBlockZ()) + 1;
        if ((long) sizeX * sizeY * sizeZ > 32L * 256L * 32L) {
            player.sendMessage("§cSelected area is too big! Max: 32x256x32, current: "
                    + sizeX + "x" + sizeY + "x" + sizeZ);
            return;
        }

        // Get the block the player is looking at
        Block targetBlock = getTargetBlock(player);
        if (targetBlock == null) {
            player.sendMessage("§cYou need to look at a block!");
            return;
        }

        Location pasteLocation = targetBlock.getLocation().add(0, 1, 0); // Paste one block above the target block

        List<MapUtils.BlockData> blocks = MapUtils.getBlocksInRegion(p1, p2);
        for (MapUtils.BlockData blockData : blocks) {
            Location location = new Location(
                    player.getWorld(),
                    pasteLocation.getBlockX() + blockData.x,
                    pasteLocation.getBlockY() + blockData.y,
                    pasteLocation.getBlockZ() + blockData.z
            );

            if (blockData.blockState != null && !blockData.blockState.isEmpty()) {
                // If blockState is available, use it to set the block data
                try {
                    location.getBlock().setBlockData(Bukkit.createBlockData(blockData.blockState));
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§cError setting block data: " + e.getMessage());
                }
            } else {
                // If blockState is not available, only set the block type
                location.getBlock().setType(Material.valueOf(blockData.type));
            }
        }

        player.sendMessage("§aPasted the selected area above the target block.");

        // [Fix B3] Log the player name, source area coordinates, and target world
        Bukkit.getLogger().info("[MagicExpansion] " + player.getName() + " pasted selected region ("
                + p1.getBlockX() + "," + p1.getBlockY() + "," + p1.getBlockZ() + ") to ("
                + p2.getBlockX() + "," + p2.getBlockY() + "," + p2.getBlockZ() + ") from world '"
                + p1.getWorld().getName() + "' into world '" + player.getWorld().getName() + "'");
    }

    /**
     * Get the block the player is looking at
     */
    private Block getTargetBlock(Player player) {
        BlockIterator iterator = new BlockIterator(player, 10); // Check up to 5 blocks away
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!block.getType().isAir()) {
                return block;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            // Provide suggestions for the first argument: subcommands
            suggestions.addAll(Arrays.asList("select", "exitselect", "exportmap", "paste", "clear", "ctrlv"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("paste")) {
            // If the subcommand is "paste", suggest file names
            File mapsDir = new File(plugin.getDataFolder(), "maps");
            if (mapsDir.exists() && mapsDir.isDirectory()) {
                for (File file : mapsDir.listFiles()) {
                    if (file.getName().endsWith(".json")) {
                        suggestions.add(file.getName().replace(".json", ""));
                    }
                }
            }
        }

        // Filter suggestions so only matches for the current input are shown
        return filterSuggestions(suggestions, args[args.length - 1]);
    }

    private List<String> filterSuggestions(List<String> suggestions, String input) {
        List<String> filtered = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase().startsWith(input.toLowerCase())) {
                filtered.add(suggestion);
            }
        }
        return filtered;
    }

}

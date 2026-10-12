package io.Yomicer.magicExpansion.utils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import io.Yomicer.magicExpansion.Listener.worldListener.Events;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapUtils {
    private static final Gson gson = new Gson();

    public static void exportMap(Player player) {
        Location p1 = Events.point1.get(player.getUniqueId());
        Location p2 = Events.point2.get(player.getUniqueId());

        if (p1 == null || p2 == null) {
            player.sendMessage("Please select two points first!");
            return;
        }

        List<BlockData> blocks;
        try {
            // Fix (Y): getBlocksInRegion checks the region size limit; catch errors and notify the player to prevent huge regions from slowing down the server
            blocks = getBlocksInRegion(p1, p2);
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Failed to export: " + e.getMessage());
            return;
        }
        String fileName = "map_" + System.currentTimeMillis() + ".json";
        File file = new File(player.getServer().getPluginManager().getPlugin("MagicExpansion").getDataFolder(), "maps/" + fileName);

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(blocks, writer);
            player.sendMessage("Map exported successfully: " + fileName);
        } catch (IOException e) {
            player.sendMessage("Failed to export map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void pasteMap(Player player, String fileName) {
        // Fix (Y): Validate filenames against [a-zA-Z0-9_-] to prevent path traversal (such as ../../config) and unauthorized file reads
        if (fileName == null || !fileName.matches("[a-zA-Z0-9_-]+")) {
            player.sendMessage(ChatColor.RED + "Invalid file name!");
            return;
        }
        File file = new File(player.getServer().getPluginManager().getPlugin("MagicExpansion").getDataFolder(), "maps/" + fileName + ".json");

        if (!file.exists()) {
            player.sendMessage(ChatColor.RED + "File not found!");
            return;
        }

        Block targetBlock = getTargetBlock(player);
        if (targetBlock == null) {
            player.sendMessage(ChatColor.RED + "You need to look at a block!");
            return;
        }

        Location pasteLocation = targetBlock.getLocation().add(0, 1, 0);

        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<BlockData>>() {}.getType();
            List<BlockData> blocks = gson.fromJson(reader, listType);

            // Fix (Y): gson.fromJson may return null (empty file or invalid JSON), so check for null to avoid an NPE
            if (blocks == null) {
                player.sendMessage(ChatColor.RED + "Failed to paste map: Map data is empty or has an invalid format!");
                return;
            }

            for (BlockData blockData : blocks) {
                Location location = new Location(
                        player.getWorld(),
                        pasteLocation.getBlockX() + blockData.x,
                        pasteLocation.getBlockY() + blockData.y,
                        pasteLocation.getBlockZ() + blockData.z
                );
                location.getBlock().setBlockData(Bukkit.createBlockData(blockData.blockState));
            }

            player.sendMessage(ChatColor.GREEN + "Map pasted successfully above the target block!");
        } catch (IOException e) {
            player.sendMessage(ChatColor.RED + "Failed to paste map: " + e.getMessage());
            e.printStackTrace();
        } catch (com.google.gson.JsonParseException e) {
            // Fix (Y): Catch JSON parsing errors so malicious or damaged files do not cause uncaught exceptions
            player.sendMessage(ChatColor.RED + "Failed to paste map: The map file is damaged or is not valid JSON!");
        }
    }

    private static Block getTargetBlock(Player player) {
        BlockIterator iterator = new BlockIterator(player, 10);
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!block.getType().isAir()) {
                return block;
            }
        }
        return null;
    }

    public static List<BlockData> getBlocksInRegion(Location p1, Location p2) {
        List<BlockData> blocks = new ArrayList<>();
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        // Fix (Y): Limit region size to 32x256x32 to prevent memory overflow or server freezes
        long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume > 32L * 256L * 32L) {
            throw new IllegalArgumentException("The selected region is too large (volume " + volume + " exceeds the limit of 32x256x32). Please select a smaller area!");
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = p1.getWorld().getBlockAt(x, y, z);

                    if (block.getType() == Material.AIR) {
                        continue;
                    }

                    String blockState = block.getBlockData().getAsString();
                    blocks.add(new BlockData(
                            x - minX,
                            y - minY,
                            z - minZ,
                            block.getType().name(),
                            blockState
                    ));
                }
            }
        }
        return blocks;
    }

    public static class BlockData {
        public int x, y, z;
        public String type;
        public String blockState;

        public BlockData(int x, int y, int z, String type, String blockState) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.type = type;
            this.blockState = blockState;
        }

        // Default constructor
        public BlockData() {}
    }
}

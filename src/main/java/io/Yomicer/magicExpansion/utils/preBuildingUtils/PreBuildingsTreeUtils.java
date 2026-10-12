package io.Yomicer.magicExpansion.utils.preBuildingUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import io.Yomicer.magicExpansion.utils.ItemPermissionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

public class PreBuildingsTreeUtils {

    private static final Gson gson = new Gson();

    /**
     * Fix (Z): Only allow building file names with [a-zA-Z0-9_-] to stop access to files outside the folder.
     */
    private static boolean isValidFileName(String fileName) {
        return fileName != null && fileName.matches("[a-zA-Z0-9_-]+");
    }

    /**
     * Paste the build above the block the player is looking at.
     */
    public static boolean pasteMap(Player player, String fileName, String originName, String replaceName) {
        // Fix (Z): Check the file name and reject invalid names.
        if (!isValidFileName(fileName)) {
            player.sendMessage("§cInvalid file name. Please do not change the plugin settings!");
            return false;
        }

        // Read the JSON file from the resources/buildings folder.
        InputStream inputStream = PreBuildingsTreeUtils.class.getClassLoader().getResourceAsStream("buildings/" + fileName + ".json");

        if (inputStream == null) {
            player.sendMessage("§cWrong file path. Please do not change the plugin settings!");
            return false; // File not found, return false
        }

        // Get the block the player is looking at.
        Block targetBlock = getTargetBlock(player);
        if (targetBlock == null) {
            player.sendMessage("§cYou need to look at a block!");
            return false; // No target block found, return false
        }

        // Paste the build one block above the target block.
        Location pasteLocation = targetBlock.getLocation().add(0, 1, 0);

        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            Type listType = new TypeToken<List<BlockData>>() {}.getType();
            List<BlockData> blocks = gson.fromJson(reader, listType);

            // Fix (Z): gson.fromJson may return null for an empty file or invalid JSON, so check for null to avoid an NPE.
            if (blocks == null) {
                player.sendMessage("§cBuilding data is empty or has the wrong format: " + fileName + ".json");
                return false;
            }

            if (!(originName == null) && !(replaceName == null)) {
                // Replace "OAK" with "CHERRY".
                for (BlockData blockData : blocks) {
                    if (blockData.type != null && blockData.type.contains(originName)) {
                        blockData.type = blockData.type.replace(originName, replaceName);
                    }
                }
            }

            // Check whether the area has permission issues and enough space.
            if (hasPermissionConflicts(blocks, pasteLocation, player.getWorld(),player)) {
                player.sendMessage("§cYou do not have permission for some areas, or there is not enough space. Please choose another spot.");
                return false; // A conflict was found, return false
            }

//            // Check for conflicts.
//            if (hasConflicts(blocks, pasteLocation, player.getWorld())) {
//                player.sendMessage("§cThere is not enough space here. Please choose another spot.");
//                return false; // A conflict was found, return false
//            }

            // If there are no conflicts, start placing the build.
            for (BlockData blockData : blocks) {
                Location location = new Location(
                        player.getWorld(),
                        pasteLocation.getBlockX() + blockData.x,
                        pasteLocation.getBlockY() + blockData.y,
                        pasteLocation.getBlockZ() + blockData.z
                );

                try {
                    if (blockData.blockState != null && !blockData.blockState.isEmpty()) {
                        location.getBlock().setBlockData(Bukkit.createBlockData(blockData.blockState));
                    } else {
                        location.getBlock().setType(Material.valueOf(blockData.type));
                    }
                } catch (IllegalArgumentException e) {
                    location.getBlock().setType(Material.GLASS);
                }
            }


            player.sendMessage("§aThe build is ready!");
            return true; // Build placed successfully, return true
        } catch (IOException e) {
            player.sendMessage("§cFailed to create: " + e.getMessage());
            e.printStackTrace();
            return false; // An error occurred, return false
        } catch (com.google.gson.JsonParseException e) {
            // Fix (Z): Catch JSON parsing errors, including JsonSyntaxException, so a damaged file does not cause an unhandled error.
            player.sendMessage("§cThe building file is damaged or is not valid JSON: " + fileName + ".json");
            return false;
        }
    }

    public static boolean pasteMap(Player player, String fileName) {
        return pasteMap(player, fileName, null, null);
    }

    /**
     * Check for permission issues.
     */
    private static boolean hasPermissionConflicts(List<BlockData> blocks, Location pasteLocation, org.bukkit.World world, Player p) {
        for (BlockData blockData : blocks) {
            Location location = new Location(
                    world,
                    pasteLocation.getBlockX() + blockData.x,
                    pasteLocation.getBlockY() + blockData.y,
                    pasteLocation.getBlockZ() + blockData.z
            );
            if(!ItemPermissionUtils.hasPermissionPoint(p,location)){
                return true; //Conflict found
            }

            Block block = location.getBlock();
            if (!block.getType().isAir()) {
                return true; // Conflict found
            }
        }
        return false; // No conflict found
    }

    /**
     * Check for conflicts.
     */
    private static boolean hasConflicts(List<BlockData> blocks, Location pasteLocation, org.bukkit.World world) {
        for (BlockData blockData : blocks) {
            Location location = new Location(
                    world,
                    pasteLocation.getBlockX() + blockData.x,
                    pasteLocation.getBlockY() + blockData.y,
                    pasteLocation.getBlockZ() + blockData.z
            );

            Block block = location.getBlock();
            if (!block.getType().isAir()) {
                return true; // Conflict found
            }
        }
        return false; // No conflict found
    }

    /**
     * Get the block the player is looking at.
     */
    private static Block getTargetBlock(Player player) {
        BlockIterator iterator = new BlockIterator(player, 10); // Check up to 5 blocks away.
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!block.getType().isAir()) {
                return block;
            }
        }
        return null; // Return null if no solid block is found.
    }

    /**
     * Inner class: stores data for one block.
     */
    private static class BlockData {
        public int x;
        public int y;
        public int z;
        public String type;
        public String blockState; // New field used to store the block state.

        // Default constructor.
        public BlockData() {}

        // Add a constructor with parameters if needed.
        public BlockData(int x, int y, int z, String type, String blockState) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.type = type;
            this.blockState = blockState;
        }
    }


    /**
     * Get the build size (X, Y, Z).
     * @param fileName File name (without .json)
     * @return int[]{xSize, ySize, zSize}，Return null if the file does not exist or an error occurs.
     */
    public static int[] getBuildingDimensions(String fileName) {
        // Fix (Z):Check the file name and return null if it is invalid.
        if (!isValidFileName(fileName)) {
            return null;
        }

        InputStream inputStream = PreBuildingsTreeUtils.class.getClassLoader().getResourceAsStream("buildings/" + fileName + ".json");
        if (inputStream == null) {
            return null;
        }

        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            Type listType = new TypeToken<List<BlockData>>() {}.getType();
            List<BlockData> blocks = gson.fromJson(reader, listType);

            if (blocks == null || blocks.isEmpty()) {
                return null;
            }

            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

            for (BlockData block : blocks) {
                if (block.x < minX) minX = block.x;
                if (block.x > maxX) maxX = block.x;

                if (block.y < minY) minY = block.y;
                if (block.y > maxY) maxY = block.y;

                if (block.z < minZ) minZ = block.z;
                if (block.z > maxZ) maxZ = block.z;
            }

            // Calculate the size (max value - min value + 1).
            int xSize = maxX - minX + 1;
            int ySize = maxY - minY + 1;
            int zSize = maxZ - minZ + 1;

            return new int[]{xSize, ySize, zSize};

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }






}

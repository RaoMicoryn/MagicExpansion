package io.Yomicer.magicExpansion.items.misc;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.Yomicer.magicExpansion.items.electric.entitykillMachinee.EntityKillMachine;
import io.Yomicer.magicExpansion.utils.VirtualPlayerManager; // G1v3: virtual player manager
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

import java.util.*;

import static io.Yomicer.magicExpansion.utils.Utils.doGlow;

public class RightClickMan extends SlimefunItem implements EnergyNetComponent {


    public RightClickMan(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        constructMenu("Interaction Robot");
        addItemHandler(onBreak());
    }

    private final int[] upBorder = {0,18};
    private final int[] downBorder = {1,19};
    private final int[] eastBorder = {2,20};
    private final int[] southBorder = {3,21};
    private final int[] westBorder = {4,22};
    private final int[] northBorder = {5,23};


    private final int[] inputSlots = {};

    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem sf, SlimefunBlockData data) {
                RightClickMan.this.tick(b);
            }

            @Override
            public boolean isSynchronized() {
                return true;
            }
        });
    }
    protected void tick(Block block) {
        BlockMenu menu = StorageCacheUtils.getMenu(block.getLocation());
        if(menu != null) {
            // Define: logical slot → Display slot → Direction
            Map<Integer, Map.Entry<Integer, BlockFace>> directionMap = new HashMap<>();
            directionMap.put(9,  new AbstractMap.SimpleImmutableEntry<>(0, BlockFace.UP));     // 9 → 0 → UP
            directionMap.put(10, new AbstractMap.SimpleImmutableEntry<>(1, BlockFace.DOWN));   // 10→ 1 → DOWN
            directionMap.put(11, new AbstractMap.SimpleImmutableEntry<>(2, BlockFace.EAST));   // 11→ 2 → EAST
            directionMap.put(12, new AbstractMap.SimpleImmutableEntry<>(3, BlockFace.SOUTH));  // 12→ 3 → SOUTH
            directionMap.put(13, new AbstractMap.SimpleImmutableEntry<>(4, BlockFace.WEST));   // 13→ 4 → WEST
            directionMap.put(14, new AbstractMap.SimpleImmutableEntry<>(5, BlockFace.NORTH));  // 14→ 5 → NORTH

            // Go through each direction
            for (Map.Entry<Integer, Map.Entry<Integer, BlockFace>> entry : directionMap.entrySet()) {
                int logicSlot = entry.getKey();           // Slots for the control logic (9~14)
                int displaySlot = entry.getValue().getKey(); // Slots used for display (0~5)
                BlockFace face = entry.getValue().getValue(); // Matching direction

                Block targetBlock = block.getRelative(face);
                Location targetLocation = targetBlock.getLocation();

                boolean isEnabled = isButtonOn(menu, logicSlot);
                String statusText = isEnabled ? "§aEnabled" : "§7Not enabled";

                ItemStack displayItem;

                SlimefunItem sfItem = StorageCacheUtils.getSfItem(targetLocation);
                if (sfItem != null) {
                    displayItem = sfItem.getItem().clone();
                }
                else if (targetBlock.getType() != Material.AIR) {
                    // Check if the material can be an item
                    Material blockType = targetBlock.getType();
                    if (isValidItemMaterial(blockType)) {
                        displayItem = new ItemStack(blockType);

                        BlockState state = targetBlock.getState();
                        ItemMeta meta = displayItem.getItemMeta();
                        if (meta instanceof BlockStateMeta blockStateMeta) {
                            blockStateMeta.setBlockState(state);
                            blockStateMeta.setLore(null);
                            displayItem.setItemMeta(blockStateMeta);
                        }
                    } else {
                        // For blocks that can't be items, use a barrier and show the block info
                        displayItem = createBlockDisplayItem(blockType, targetBlock, statusText);
                    }
                }
                else {
                    displayItem = new ItemStack(Material.BARRIER);
                    ItemMeta meta = displayItem.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName("§7Air");
                        meta.setLore(Arrays.asList(
                                "§7Coordinates: " + targetBlock.getX() + "," + targetBlock.getY() + "," + targetBlock.getZ(),
                                "§7Status: " + statusText,
                                "§8(this is air)"
                        ));
                        displayItem.setItemMeta(meta);
                    }
                    menu.replaceExistingItem(displaySlot, displayItem);
                    continue;
                }

                // ====== 4. For normal non-AIR blocks, set the display info the same way ======
                setBlockDisplayWithInfo(displayItem, targetBlock, statusText);

                // Update GUI
                menu.replaceExistingItem(displaySlot, displayItem);
            }

            // Define the slot to direction mapping (slot → enabled or not)
            boolean up = isButtonOn(menu, 9);
            boolean down = isButtonOn(menu, 10);
            boolean east = isButtonOn(menu, 11);
            boolean south = isButtonOn(menu, 12);
            boolean west = isButtonOn(menu, 13);
            boolean north = isButtonOn(menu, 14);

            // G2 Fix: full getPlayers() distance filter+sorting slowed to once every 10 ticks, result cached and reused
            NearestPlayerCache playerCache = nearestPlayerCache.computeIfAbsent(
                    block.getLocation(), k -> new NearestPlayerCache());
            long now = System.currentTimeMillis();
            if (now >= playerCache.nextScanAt) {
                playerCache.nextScanAt = now + PLAYER_SCAN_INTERVAL_MS;
                playerCache.player = findNearestPlayer(block.getLocation());
            }
            Player nearestPlayer = playerCache.player;

            // G1v3(as needed): use the virtual player first to do the simulated right-click;
            // Fall back when the virtual player isn't available"nearby real player", if neither exists, skip this tick
            boolean hasVirtual = VirtualPlayerManager.obtainVirtual() != null;
            if (!hasVirtual && nearestPlayer == null) return;

            String directions = "";

            if (up) {
                interactTarget(nearestPlayer, block.getLocation().clone().add(0, 1, 0).getBlock());
                directions += "Up ";
            }

            if (down) {
                interactTarget(nearestPlayer, block.getLocation().clone().add(0, -1, 0).getBlock());
                directions += "Down ";
            }

            if (east) {
                interactTarget(nearestPlayer, block.getLocation().clone().add(1, 0, 0).getBlock());
                directions += "East ";
            }

            if (south) {
                interactTarget(nearestPlayer, block.getLocation().clone().add(0, 0, 1).getBlock());
                directions += "South ";
            }

            if (west) {
                interactTarget(nearestPlayer, block.getLocation().clone().add(-1, 0, 0).getBlock());
                directions += "West ";
            }

            if (north) {
                interactTarget(nearestPlayer, block.getLocation().clone().add(0, 0, -1).getBlock());
                directions += "North";
            }

            // Update the status display
            if (menu.hasViewer()) {
                // G1v3: simulated player display——Show the virtual player and its mode first, then the fallback nearby real player
                String actorLine;
                if (hasVirtual) {
                    // Virtual player mode: show the puppet name and how it's implemented (citizens / nms)
                    actorLine = "§d" + VirtualPlayerManager.BOT_NAME + " §7(Virtual·" + VirtualPlayerManager.getMode() + ")";
                } else if (nearestPlayer != null) {
                    // Fallback mode: show the nearby real player's name
                    actorLine = nearestPlayer.getName() + " §7(Fallback)";
                } else {
                    actorLine = null;
                }
                if (actorLine != null) {
                    menu.replaceExistingItem(16, new CustomItemStack(Material.PINK_CANDLE, "§bInteraction Robot",
                            "§bWork type: §eRight-click interact with block",
                            "§bInteraction speed: §e1 time/Slimefun tick",
                            "§bSimulated player: §e" + actorLine,
                            "§bSimulated direction: §e" + directions.trim(),
                            "§bPower use: §eThis robot doesn't use power",
                            "§bStored power: §eThis robot doesn't store power"));
                } else {
                    menu.replaceExistingItem(16, new CustomItemStack(Material.PINK_CANDLE, "§bInteraction Robot",
                            "§bWork type: §eRight-click interact with block",
                            "§bInteraction speed: §e1 time/Slimefun tick",
                            "§cNo virtual player or nearby player detected",
                            "§bPower use: §eThis robot doesn't use power",
                            "§bStored power: §eThis robot doesn't store power"));
                }
            }
        }
    }

    // ==================== G2 Performance fix: nearby player scan throttling ====================
    private static final long PLAYER_SCAN_INTERVAL_MS = 500; // Scan interval ≈ 10 tick
    // Cache the nearest player per machine location: nextScanAt is the next scan time, player is the cached result
    private final Map<Location, NearestPlayerCache> nearestPlayerCache = new HashMap<>();

    private static final class NearestPlayerCache {
        long nextScanAt;
        Player player; // May be null, meaning there are no players nearby
    }

    // G2: Full getPlayers() distance filter + sorting (take the nearest player)
    private Player findNearestPlayer(Location center) {
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player player : center.getWorld().getPlayers()) {
            double dist = player.getLocation().distanceSquared(center);
            if (dist <= 2500 && dist < nearestDist) { // Same as the original logic: take the nearest within 50 blocks
                nearestDist = dist;
                nearest = player;
            }
        }
        return nearest;
    }

    /**
     * G1v3(as needed): the simulated right-click is now done by"Virtual player"initiated.
     * Priority: Citizens NPC (plan A)→ NMS Reflection fake player (plan B)→ nearby real player (fallback).
     * The virtual player is teleported next to the target block before each interaction, so the event handler reads the right location;
     * The event is still PlayerInteractEvent(RIGHT_CLICK_BLOCK), each plugin's interact listener responds on its own.
     *
     * @param fallbackPlayer Fallback target when the virtual player isn't available (nearby real player, may be null)
     * @param targetBlock    Target block
     */
    private void interactTarget(Player fallbackPlayer, Block targetBlock) {
        // 1) Get the executor: virtual player first
        Player actor = VirtualPlayerManager.obtainVirtual();
        boolean isVirtual = actor != null;
        if (!isVirtual) {
            // Fallback mode: use the nearby real player
            actor = fallbackPlayer;
            if (actor == null) return;
        }

        // 2) Land protection check: players without interact permission don't trigger it.
        //    The permission rules for virtual players (NPC/fake players) are decided by the land plugin's NPC group config,
        //    If the check throws an exception: real players are treated as denied, virtual players as allowed (so an NPC group config doesn't break the whole feature)
        try {
            if (!Slimefun.getProtectionManager().hasPermission(actor, targetBlock.getLocation(),
                    Interaction.INTERACT_BLOCK)) {
                return;
            }
        } catch (Throwable t) {
            if (!isVirtual) return;
        }

        // 3) The virtual player teleports above the target block (no real player attached, so the robot can run without a real person)
        if (isVirtual) {
            VirtualPlayerManager.moveVirtual(targetBlock.getLocation().add(0.5, 1.0, 0.5));
        }

        // 4) Simulate a main hand right-click on the target block (holding AIR, BlockFace.SELF, same as the original implementation)
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(
                actor,
                Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.AIR),
                targetBlock,
                BlockFace.SELF
        );
        Bukkit.getPluginManager().callEvent(interactEvent);
    }

    /**
     * Check if the material can be a valid item
     */
    private boolean isValidItemMaterial(Material material) {
        // Wall signs, double slabs, etc. can't be items
        return material.isItem() && !material.name().contains("WALL_") && !material.name().contains("DOUBLE_");
    }

    /**
     * Create a display item for blocks that can't be items
     */
    private ItemStack createBlockDisplayItem(Material blockType, Block block, String statusText) {
        ItemStack displayItem = new ItemStack(Material.BARRIER);
        ItemMeta meta = displayItem.getItemMeta();

        if (meta != null) {
            // Get the block's friendly name
            String blockName = blockType.name().toLowerCase().replace("_", " ");
            meta.setDisplayName("§7" + blockName);

            List<String> lore = new ArrayList<>();
            lore.add("§7Coordinates: " + block.getX() + "," + block.getY() + "," + block.getZ());
            lore.add("§7Status: " + statusText);
            lore.add("§8(this block can't be shown as an item)");

            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }

        return displayItem;
    }

    private void constructMenu(String displayName) {
        new BlockMenuPreset(getId(), displayName) {

            @Override
            public void init() {
                constructMenu(this);
            }

            @Override
            public boolean canOpen(@Nonnull Block b, @Nonnull Player p) {
                return p.hasPermission("slimefun.inventory.bypass")
                        || Slimefun.getProtectionManager().hasPermission(p, b.getLocation(),
                        Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow itemTransportFlow) {
                return new int[]{

                };
            }
        };
    }



    protected void constructMenu(BlockMenuPreset preset) {
        // Button state
        final Material BUTTON_OFF = Material.BARRIER;
        final Material BUTTON_ON = Material.LANTERN;

        int[] controlSlots = {9, 10, 11, 12, 13, 14};
        int[] directionSlots = {6, 7, 8, 24, 25, 26};

        for (int slot : controlSlots) {
            preset.addItem(slot, new CustomItemStack(BUTTON_OFF, "§7Not activated", "§eClick to activate"),
                    (player, slot1, item, action) -> {
                        // G3 Fix: removed the dead code line here whose getTargetBlock result was never used
                        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof BlockMenu menu)) {
                            return false;
                        }
                        ItemStack currentItem = menu.getItemInSlot(slot1);

                        if (currentItem != null && currentItem.getType() == BUTTON_ON) {
                            // ON → OFF
                            menu.replaceExistingItem(slot1, new CustomItemStack(BUTTON_OFF, "§7Not activated", "§eClick to activate"));
                        } else {
                            // OFF or empty → ON
                            menu.replaceExistingItem(slot1, new CustomItemStack(BUTTON_ON, "§aActivated", "§eClick to close"));
                        }

                        return false; // Stop it from being taken
                    });
        }



        for (int i : directionSlots) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack(Material.BLUE_CANDLE)), "§9Decorative candle", "§7Just for looks","§7The center is the machine state"),
                    (p, slot, item, action) -> false);
        }

        preset.addItem(15, new CustomItemStack(Material.BLUE_CANDLE, "§9Decorative candle", "§7Just for looks","§7The center is the machine state"),
                (player, slot, item, action) -> false); // Clicking does nothing

        preset.addItem(17, new CustomItemStack(Material.BLUE_CANDLE, "§9Decorative candle", "§7Just for looks","§7The center is the machine state"),
                (player, slot, item, action) -> false); // Clicking does nothing

        // ====== Border description text (not clickable)======
        for (int i : upBorder) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack(Material.WHITE_CONCRETE)), "§eUp", "§bClick here, the robot interacts with the block above"),
                    (p, slot, item, action) -> false);
        }
        for (int i : downBorder) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack(Material.BLACK_CONCRETE)), "§eDown", "§bClick here, the robot interacts with the block below"),
                    (p, slot, item, action) -> false);
        }
        for (int i : eastBorder) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack(Material.RED_CONCRETE)), "§eEast", "§bClick here, the robot interacts with the block to the east"),
                    (p, slot, item, action) -> false);
        }
        for (int i : southBorder) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack(Material.BLUE_CONCRETE)), "§eSouth", "§bClick here, the robot interacts with the block to the south"),
                    (p, slot, item, action) -> false);
        }
        for (int i : westBorder) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack(Material.GREEN_CONCRETE)), "§eWest", "§bClick here, the robot interacts with the block to the west"),
                    (p, slot, item, action) -> false);
        }
        for (int i : northBorder) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack(Material.YELLOW_CONCRETE)), "§eNorth", "§bClick here, the robot interacts with the block to the north"),
                    (p, slot, item, action) -> false);
        }

        // Status display slot (middle)
        preset.addItem(16, new CustomItemStack(Material.RED_CANDLE, " "),
                (p, slot, item, action) -> false);
    }
    private boolean isButtonOn(BlockMenu menu, int slot) {
        ItemStack item = menu.getItemInSlot(slot);
        return item != null && item.getType() == Material.LANTERN; // Note: here we just use LANTERN to check
    }

    /**
     * Set the item display: inherit the original name and Lore, and add coordinates and status info
     */
    private void setBlockDisplayWithInfo(ItemStack item, Block block, String statusLine) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 1. Set the display name (use ItemStackHelper to get the friendly name)
        String displayName = ItemStackHelper.getDisplayName(item);
        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();

        // 2. Keep the original Lore (if any)
        if (meta.hasLore()) {
            lore.addAll(meta.getLore());
        }

        // 3. Add a divider (if there's already Lore or new info is being added)
        if (!lore.isEmpty() || statusLine != null) {
            lore.add("§8---");
        }

        // 4. Add coordinates
        lore.add("§7Coordinates: " + block.getX() + "," + block.getY() + "," + block.getZ());

        // 5. Add status (optional)
        if (statusLine != null) {
            lore.add("§7Status: " + statusLine);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }



    private BlockBreakHandler onBreak() {
        return new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(@Nonnull BlockBreakEvent e, @Nonnull ItemStack i, @Nonnull List<ItemStack> list) {
                Block b = e.getBlock();
                BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());

                if (inv != null) {
                    inv.dropItems(b.getLocation(), inputSlots);
                }

            }
        };
    }

    @Override
    public @NotNull EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.NONE;
    }

    @Override
    public int getCapacity() {
        return 0;
    }
}
package io.Yomicer.magicExpansion.Listener.worldListener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Events implements Listener {

    // Fix: Use ConcurrentHashMap instead of HashMap to avoid issues when multiple threads access it.
    public static final Map<UUID, Boolean> selectMode = new ConcurrentHashMap<>();
    public static final Map<UUID, Location> point1 = new ConcurrentHashMap<>();
    public static final Map<UUID, Location> point2 = new ConcurrentHashMap<>();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Fix: Off-hand clicks can trigger the event twice, so ignore them to avoid selecting points twice.
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        if (!selectMode.getOrDefault(player.getUniqueId(), false)) {
            return; // The player is not in selection mode.
        }

        if (event.getAction().name().contains("LEFT_CLICK_BLOCK")) {
            Block block = event.getClickedBlock();
            point1.put(player.getUniqueId(), block.getLocation());
            player.sendMessage("Point 1 selected at " + block.getLocation());
        } else if (event.getAction().name().contains("RIGHT_CLICK_BLOCK")) {
            Block block = event.getClickedBlock();
            point2.put(player.getUniqueId(), block.getLocation());
            player.sendMessage("Point 2 selected at " + block.getLocation());
        }

        event.setCancelled(true); // Stop the player from breaking blocks or opening containers.
    }

    /**
     * Fix: Clear the player's selection mode and selected points when they leave, to avoid memory leaks.
     */
    public static void cleanup(UUID uuid) {
        selectMode.remove(uuid);
        point1.remove(uuid);
        point2.remove(uuid);
    }

}

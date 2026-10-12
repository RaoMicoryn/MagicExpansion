package io.Yomicer.magicExpansion.utils;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;

public class ItemPermissionUtils {

    /**
     * Check whether the player has permission to perform this action
     *
     * @param player player object
     * @return Returns true if the player has permission; otherwise returns false
     */
    public static boolean hasPermissionRe(Player player) {
        // Check whether the player has the "slimefun.inventory.bypass" permission
        if (player.hasPermission("slimefun.inventory.bypass")) {
            return true;
        }

        // Check protection-plugin permissions (such as WorldGuard)
        if (Slimefun.getProtectionManager().hasPermission(player, player.getLocation(), Interaction.INTERACT_BLOCK)&&
                Slimefun.getProtectionManager().hasPermission(player, player.getLocation(), Interaction.BREAK_BLOCK)&&
                Slimefun.getProtectionManager().hasPermission(player, player.getLocation(), Interaction.PLACE_BLOCK)) {
            return true;
        }
        player.sendMessage(getGradientName("[Magic]You do not have permission to do this~"));

        return false;
    }

    /**
     * Check whether the player has permission to perform this action
     *
     * @param player player object
     * @return Returns true if the player has permission; otherwise returns false
     */
    public static boolean hasPermissionOnAttack(Player player) {
        // Check whether the player has the "slimefun.inventory.bypass" permission
        if (player.hasPermission("slimefun.inventory.bypass")) {
            return true;
        }

        // Check protection-plugin permissions (such as WorldGuard)
        if (Slimefun.getProtectionManager().hasPermission(player, player.getLocation(), Interaction.ATTACK_PLAYER)&&
                Slimefun.getProtectionManager().hasPermission(player, player.getLocation(), Interaction.ATTACK_ENTITY)&&
                Slimefun.getProtectionManager().hasPermission(player, player.getLocation(), Interaction.INTERACT_BLOCK)&&
                Slimefun.getProtectionManager().hasPermission(player, player.getLocation(), Interaction.BREAK_BLOCK)&&
                Slimefun.getProtectionManager().hasPermission(player, player.getLocation(), Interaction.PLACE_BLOCK)) {
            return true;
        }
        return false;
    }

    public static boolean hasPermissionOnlyOnAttackEntity(Player player) {
        // Check whether the player has the "slimefun.inventory.bypass" permission
        if (player.hasPermission("slimefun.inventory.bypass")) {
            return true;
        }
        // Check protection-plugin permissions (such as WorldGuard)
        if (Slimefun.getProtectionManager().hasPermission(player, player.getLocation(), Interaction.ATTACK_ENTITY)) {
            return true;
        }
        return false;
    }


    /**
     * Check whether the player has permission to perform this action
     *
     * @param player player object
     * @return Returns true if the player has permission; otherwise returns false
     */
    public static boolean hasPermissionPoint(Player player, Location l) {
        // Check whether the player has the "slimefun.inventory.bypass" permission
        if (player.hasPermission("slimefun.inventory.bypass")) {
            return true;
        }

        // Check protection-plugin permissions (such as WorldGuard)
        if (Slimefun.getProtectionManager().hasPermission(player, l, Interaction.INTERACT_BLOCK)&&
                Slimefun.getProtectionManager().hasPermission(player, l, Interaction.BREAK_BLOCK)&&
                Slimefun.getProtectionManager().hasPermission(player, l, Interaction.PLACE_BLOCK)) {
            return true;
        }
//        player.sendMessage(getGradientName("[Magic]You do not have permission to do this~"));

        return false;
    }




}

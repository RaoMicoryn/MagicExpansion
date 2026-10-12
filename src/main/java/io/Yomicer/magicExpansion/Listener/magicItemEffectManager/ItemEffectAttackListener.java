package io.Yomicer.magicExpansion.Listener.magicItemEffectManager;

import io.Yomicer.magicExpansion.Listener.weaponApply.SlownessManager;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.UUID;

import static io.Yomicer.magicExpansion.items.enchantMachine.EnchantingTable.ATTRIBUTE_POOL;
import static io.Yomicer.magicExpansion.utils.ItemPermissionUtils.hasPermissionOnAttack;

public class ItemEffectAttackListener implements Listener {

    // Fix (X1): Add ignoreCancelled = true so cancelled attack events (such as protected-zone hits) do not trigger effects again
    @EventHandler(ignoreCancelled = true)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            ItemStack item = player.getInventory().getItemInMainHand();

            // Get the item PDC
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer() != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();

                // Loop through all attributes
                for (String attribute : ATTRIBUTE_POOL.keySet()) {
                    NamespacedKey key = new NamespacedKey("magicexpansion", attribute.toLowerCase().replace(".", "_"));

                    if (pdc.has(key, PersistentDataType.INTEGER)) {
                        int value = pdc.get(key, PersistentDataType.INTEGER);
                        applyEffect(attribute, value, event.getEntity(),player);
                   } else if (pdc.has(key, PersistentDataType.BOOLEAN)) {
                        boolean value = pdc.get(key, PersistentDataType.BOOLEAN);
                        applyEffect(attribute, value, event.getEntity(),player);
                    }
                }
            }
        }
    }

    private void applyEffect(String attribute, Object value, Entity target, Player player) {
        if (!(target instanceof LivingEntity livingTarget)) return;
        if(!hasPermissionOnAttack(player)) return;
        switch (attribute) {
            case "MagicExpansion.Knockback": // Knockback effect
                if (value instanceof Integer knockbackStrength) {
                    // Fix (X3): Remove teleporting the target upward (it causes teleport flicker and is unsafe asynchronously); use setVelocity for knockback only
                    double knockbackStrengthDouble = (double) knockbackStrength;
                    Vector velocity = calculateKnockbackVelocity(livingTarget, knockbackStrengthDouble);

                    // Check whether the Vector is valid
                    if (isValidVector(velocity)) {
                        livingTarget.setVelocity(velocity);
                    } else {
                    }
                }
                break;

            case "MagicExpansion.Slowness": // Slowness effect
                if (value instanceof Integer slownessLevel) {

                    // Apply slowness (50% slow, lasts 2 seconds)
//                    SlownessManager.applySlowness(livingTarget, slownessLevel, slownessLevel); // 40 ticks = 2 seconds
                    livingTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slownessLevel*3, slownessLevel));

                }
                break;

            case "MagicExpansion.Attraction": // Attraction effect
                if (value instanceof Boolean attractionEnabled && attractionEnabled) {
                    Location targetLoc = target.getLocation();
                    Location playerLoc = player.getLocation();
                    // Fix (X2): Skip when distance < 0.5. Calling normalize when too close can create NaN velocity and kick the client
                    if (targetLoc.distance(playerLoc) < 0.5) {
                        break;
                    }
                    target.setVelocity(playerLoc.toVector().subtract(targetLoc.toVector()).normalize().multiply(2));
                }
                break;

            case "MagicExpansion.FireDamage": // Fire damage
                if (value instanceof Integer fireTicks) {
                    target.setFireTicks(fireTicks * 40 ); // 20 ticks per second; 2 * level seconds
                }
                break;

            case "MagicExpansion.Lightning": // Lightning effect
                if (value instanceof Boolean lightningEnabled && lightningEnabled) {
                    target.getWorld().strikeLightning(target.getLocation());
                }
                break;

            case "MagicExpansion.AttackTpToTp": // Swap positions
                if (value instanceof Boolean AttackTpToTp && AttackTpToTp) {
                    swapPositionsAndDirections(target, player);
                }
                break;
        }
    }


    private Vector calculateKnockbackVelocity(LivingEntity target, double strength) {
        Random random = new Random();
        // Example: Calculate knockback velocity based on the target direction
        Vector direction = target.getLocation().getDirection();
        double speedX = direction.getX() * strength * 0.1;
        double speedY = Math.max(0.1, strength*1.2); // Make sure the Y value is not negative
        double speedZ = direction.getZ() * strength * 0.1;

        return new Vector(speedX, speedY, speedZ);
    }

    private boolean isValidVector(Vector vector) {
        return Double.isFinite(vector.getX()) && Double.isFinite(vector.getY()) && Double.isFinite(vector.getZ());
    }


    private void swapPositionsAndDirections(Entity target, Player player) {
        // Get the player and target locations
        Location playerLocation = player.getLocation();
        Location targetLocation = target.getLocation();

        // Save the player and target directions (yaw and pitch)
        float playerYaw = playerLocation.getYaw();
        float playerPitch = playerLocation.getPitch();
        float targetYaw = targetLocation.getYaw();
        float targetPitch = targetLocation.getPitch();

        // Create new Location objects to avoid reference issues
        Location newPlayerLocation = targetLocation.clone();
        Location newTargetLocation = playerLocation.clone();

        // Set the new directions
        newPlayerLocation.setYaw(targetYaw);
        newPlayerLocation.setPitch(targetPitch);
        newTargetLocation.setYaw(playerYaw);
        newTargetLocation.setPitch(playerPitch);

        // Teleport the player to the target location and update the direction
        player.teleport(newPlayerLocation);

        // Teleport the target to the player location and update the direction
        if (target instanceof LivingEntity) {
            ((LivingEntity) target).teleport(newTargetLocation);
        } else {
            // If the target is not a LivingEntity, only teleport its location
            target.teleport(newTargetLocation);
        }

        // Send a message to the player
        player.sendMessage("§bYou and the target have swapped places!");
    }

    /**
     * Fix (X4): Player quit cleanup hook — this class currently has no per-player static Map,
     * Keep this empty method for PlayerCleanupListener to call; clean up session data here if any is added later
     */
    public static void cleanup(UUID uuid) {
        // There is no per-player state to clean up right now
    }

}
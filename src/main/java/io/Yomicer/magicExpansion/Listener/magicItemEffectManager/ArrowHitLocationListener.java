package io.Yomicer.magicExpansion.Listener.magicItemEffectManager;

import io.Yomicer.magicExpansion.utils.GiveItem;
import io.Yomicer.magicExpansion.utils.entity.EntityEgg;
import net.guizhanss.guizhanlib.minecraft.helper.entity.EntityHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

import static io.Yomicer.magicExpansion.items.enchantMachine.EnchantingTable.ATTRIBUTE_POOL;

public class ArrowHitLocationListener implements Listener {


    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        // Check whether the projectile is an arrow
        if (event.getEntity().getType() == EntityType.ARROW) {
            Arrow arrow = (Arrow) event.getEntity();

            // Get the player who shot the arrow, if any
            if (arrow.getShooter() instanceof Player player) {

                // Fix (W1): Minimal change — on hit, check that the player is still holding a bow-type item (including the same kind of magic bow) before applying the effect,
                // This prevents using another item’s attributes if the player switches items after shooting. The arrow PDC approach was not used to keep changes small.
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || (held.getType() != Material.BOW && held.getType() != Material.CROSSBOW)) {
                    return;
                }

                ItemStack item = held.clone();
                // Get the item PDC
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.getPersistentDataContainer() != null) {
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();

                    // Loop through all attributes
                    for (String attribute : ATTRIBUTE_POOL.keySet()) {
                        NamespacedKey key = new NamespacedKey("magicexpansion", attribute.toLowerCase().replace(".", "_"));

                        if (pdc.has(key, PersistentDataType.INTEGER)) {
                            int value = pdc.get(key, PersistentDataType.INTEGER);
                            applyEffect(attribute, value, event, player, arrow);
                        } else if (pdc.has(key, PersistentDataType.BOOLEAN)) {
                            boolean value = pdc.get(key, PersistentDataType.BOOLEAN);
                            applyEffect(attribute, value, event, player, arrow);
                        }
                    }
                }
            }
        }
    }



    private void applyEffect(String attribute, Object value, ProjectileHitEvent event, Player player, Arrow arrow) {
        switch (attribute) {
            case "MagicExpansion.ArrowTp": // Knockback effect
                if (value instanceof Boolean ArrowTp && ArrowTp) {

                    // Get the arrow landing location
                    Location hitLocation = null;

                    // Check whether the arrow hit a block
                    if (event.getHitBlock() != null) {
                        hitLocation = event.getHitBlock().getLocation();
                    }

                    // Check whether the arrow hit an entity
                    if (event.getHitEntity() != null) {
                        hitLocation = event.getHitEntity().getLocation();
                    }

                    // If the arrow did not hit a block or an entity, it may have hit air
                    if (hitLocation == null) {
                        hitLocation = arrow.getLocation(); // Get the arrow’s final location
                    }

                    // Teleport the player to where the arrow landed
                    if (hitLocation != null) {
                        hitLocation.add(0,1,0);
                        // Fix (W2): Before teleporting, check that the blocks at the destination and above it are both air. Cancel and notify the player if it is unsafe, to avoid getting stuck or suffocating.
                        Block feet = hitLocation.getBlock();
                        Block head = hitLocation.clone().add(0, 1, 0).getBlock();
                        if (feet.getType().isAir() && head.getType().isAir()) {
                            player.teleport(hitLocation);
                            player.sendMessage("§bYou have been teleported to where the arrow landed.");
                        } else {
                            player.sendMessage("§cThe arrow landing spot is unsafe. Teleport cancelled!");
                        }
                    }




                }
                break;


        }


    }








}
package io.Yomicer.magicExpansion.Listener.bossListener;

import io.Yomicer.magicExpansion.core.MagicExpansionItems;
import io.Yomicer.magicExpansion.utils.log.Debug;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class BasicBossDropListener implements Listener {

    // F4: Reuse one shared random number generator instead of creating a new one for every drop.
    private static final Random RANDOM = new Random();

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Get the mob type from its metadata.
        String mobType = "";
        if (entity.hasMetadata("magicMobType")) {
            mobType = entity.getMetadata("magicMobType").get(0).asString();
        } else {
            return; // No mobType metadata, so return right away.
        }

        // Clear the default drops.
//        event.getDrops().clear();

        // Handle drops based on the mob type.
        switch (mobType) {
            case "FireZombie":
                dropFireZombieLoot(entity);
                break;
            case "WindElf":
                dropWindElfLoot(entity);
                break;
            default:
//                Debug.logWarn("Unknown mob type: " + mobType);
                break;
        }
    }

    /**
     * Drops for when the Fire Zombie dies.
     *
     * @param entity The entity that died.
     */
    private void dropWindElfLoot(LivingEntity entity) {
        // F1: The old getEntitiesByClass(Item.class).forEach(Entity::remove) removed dropped items from the whole world, which caused major issues.
        // Now it only removes dropped items within 3 blocks of the death spot that could interfere with this Boss's drops.
        entity.getWorld().getNearbyEntitiesByType(Item.class, entity.getLocation(), 3).forEach(Entity::remove);

        // Get the death location.
        Location dropLocation = entity.getLocation();

        // F3: Only check for rare drops if a player got the kill. Environmental deaths won't drop these items.
        // F4: Use the shared random number generator.
        if (entity.getKiller() == null || RANDOM.nextInt(100000) > 91888) {
            return;
        }
        // F2: Clone the item before dropping it so the shared SlimefunItemStack isn't changed or used up.
        ItemStack DropItem = MagicExpansionItems.WIND_SPIRIT.clone();
        ItemStack DropItem2 = SlimefunItems.ADVANCED_CIRCUIT_BOARD.clone();

        // Drop the items into the world.
        dropLocation.getWorld().dropItemNaturally(dropLocation, DropItem);
        dropLocation.getWorld().dropItemNaturally(dropLocation, DropItem2);
        // Example: drop some experience.
        entity.getWorld().spawnEntity(dropLocation, EntityType.EXPERIENCE_ORB);


    }


    /**
     * Drops for when the Fire Zombie dies.
     *
     * @param entity The entity that died.
     */
    private void dropFireZombieLoot(LivingEntity entity) {
        // F1: The old getEntitiesByClass(Item.class).forEach(Entity::remove) removed dropped items from the whole world, which caused major issues.
        // Now it only removes dropped items within 3 blocks of the death spot that could interfere with this Boss's drops.
        entity.getWorld().getNearbyEntitiesByType(Item.class, entity.getLocation(), 3).forEach(Entity::remove);

        // Get the death location.
        Location dropLocation = entity.getLocation();

        // F3: Only check for rare drops if a player got the kill. Environmental deaths won't drop these items.
        // F4: Use the shared random number generator.
        if (entity.getKiller() == null || RANDOM.nextInt(100000) > 91888) {
            return;
        }
        // F2: Clone the item before dropping it so the shared SlimefunItemStack isn't changed or used up.
        ItemStack DropItem = MagicExpansionItems.BASIC_ENCHANT_STONE.clone();
        ItemStack DropItem2 = SlimefunItems.BASIC_CIRCUIT_BOARD.clone();

        // Drop the items into the world.
        dropLocation.getWorld().dropItemNaturally(dropLocation, DropItem);
        dropLocation.getWorld().dropItemNaturally(dropLocation, DropItem2);

        // Example: drop some experience.
        entity.getWorld().spawnEntity(dropLocation, EntityType.EXPERIENCE_ORB);


    }



}

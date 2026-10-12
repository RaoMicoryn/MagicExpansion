package io.Yomicer.magicExpansion.specialActions;

import io.Yomicer.magicExpansion.utils.ColorGradient;
import net.guizhanss.guizhanlib.minecraft.helper.entity.EntityHelper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import java.util.Random;
import org.bukkit.Location;

public class EntitySpawner {


    private final Random random = new Random();

    /**
     * Spawn a random mob near the player.
     *
     * @param player the player
     */
    public void spawnRandomEntityNearPlayer(Player player) {
        // Get the player’s location
        Location playerLocation = player.getLocation();

        // Pick a random offset around the player
        double x = random.nextDouble(-5, 5); // Random X offset
        double y = random.nextDouble(0, 1); // Random Y offset (keep it from spawning too high or too low)
        double z = random.nextDouble(-5, 5); // Random Z offset

        // Set the spawn location
        Location spawnLocation = playerLocation.clone().add(x, y, z);

        // Pick a random mob type
        EntityType[] entityTypes = EntityType.values();
        EntityType randomEntityType = entityTypes[random.nextInt(entityTypes.length)];

        // Make sure the entity can spawn
        if (randomEntityType.isSpawnable()) {
            Entity spawnedEntity = player.getWorld().spawnEntity(spawnLocation, randomEntityType);
            // Check if the spawned entity is a living mob (LivingEntity)
            if (spawnedEntity instanceof org.bukkit.entity.LivingEntity) {
                // Set a custom name
                String entityName = "Magic Mob ·" + EntityHelper.getName(spawnedEntity); // Use the random entity type’s name
                spawnedEntity.setCustomName(ColorGradient.getGradientName(entityName)); // Apply gradient colors
                spawnedEntity.setCustomNameVisible(true); // Show the name in-game
            }
            player.sendMessage(ColorGradient.getGradientName("Summoned Magic Mob · " + randomEntityType.name()));
        } else {
            player.sendMessage(ColorGradient.getGradientName("Could not summon the mob~"));
        }
    }

}

package io.Yomicer.magicExpansion.utils.entity;

import io.Yomicer.magicExpansion.utils.log.Debug;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class EntityEgg {


    // Check whether an entity type has a spawn egg
    public static Material  hasSpawnEgg(EntityType entityType) {
        try {
            // Convert the entity type name to uppercase and add "_SPAWN_EGG"
            String spawnEggName = entityType.name().toUpperCase() + "_SPAWN_EGG";

            // Check whether the matching Material exists
            return Material.getMaterial(spawnEggName);
        } catch (IllegalArgumentException e) {
            // Return null if the Material doesn't exist
            return null;
        }
    }


}

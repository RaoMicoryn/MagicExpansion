package io.Yomicer.magicExpansion.Listener.weaponApply;

import io.Yomicer.magicExpansion.MagicExpansion;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SlownessManager {

    // Stores slowness modifier IDs (B2: ConcurrentHashMap is thread-safe)
    private static final Map<UUID, UUID> playerSlownessModifierIds = new ConcurrentHashMap<>();

    // B3: Keeps delayed removal tasks so they can all be canceled when the plugin unloads or reloads
    private static final List<BukkitTask> pendingRemoveTasks = new ArrayList<>();

    /**
     * Apply the slowness effect
     *
     * @param livingTarget the target entity
     * @param slownessLevel the slowness level (0 to 1)
     * @param durationTicks how long it lasts (in ticks; 20 ticks = 1 second)
     */
    public static void applySlowness(LivingEntity livingTarget, double slownessLevel, int durationTicks) {
        AttributeInstance movementSpeed = livingTarget.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed == null) {
            // B2: Remove the saved entry if the target has no such attribute, to avoid a memory leak
            playerSlownessModifierIds.remove(livingTarget.getUniqueId());
            return;
        }

        // B1: Only remove this plugin’s slowness modifier, not modifiers from gear, potions, or other sources
        removeAllModifiers(movementSpeed);
        UUID playerId = livingTarget.getUniqueId();
        UUID modifierId = playerSlownessModifierIds.get(playerId);

        // Remove the old slowness modifier first, if there is one
        if (modifierId != null) {
            removeExistingModifier(movementSpeed, modifierId);
        }

        // Calculate the speed reduction
        double baseSpeed = movementSpeed.getBaseValue();
        double reducedSpeed = baseSpeed * (1 - slownessLevel*0.0099); // speed reduction

        // Create a new modifier ID
        modifierId = UUID.randomUUID();
        playerSlownessModifierIds.put(playerId, modifierId);

        // Create the slowness modifier
        AttributeModifier modifier = new AttributeModifier(
                modifierId,
                "SlownessEffect",
                reducedSpeed - baseSpeed, // speed change
                AttributeModifier.Operation.ADD_NUMBER
        );

        // Add the slowness modifier
        movementSpeed.addModifier(modifier);

        // Debug info
//        Debug.logInfo("Slowness applied to " + livingTarget.getName());

        // Remove the slowness effect later
        // B3: Save the task so cancelAll can stop it, and remove finished tasks to keep the list from growing forever
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                removeSlowness(livingTarget);
            }
        }.runTaskLater(MagicExpansion.getInstance(), durationTicks*3);
        pendingRemoveTasks.add(task);
        pendingRemoveTasks.removeIf(BukkitTask::isCancelled);
    }

    /**
     * Remove the slowness effect
     *
     * @param livingTarget the target entity
     */
    public static void removeSlowness(LivingEntity livingTarget) {
        AttributeInstance movementSpeed = livingTarget.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        UUID playerId = livingTarget.getUniqueId();
        if (movementSpeed == null) {
            // B2: Remove the saved entry if the target has no such attribute, to avoid a memory leak
            playerSlownessModifierIds.remove(playerId);
            return;
        }

        UUID modifierId = playerSlownessModifierIds.get(playerId);

        if (modifierId != null) {
            removeExistingModifier(movementSpeed, modifierId);
            playerSlownessModifierIds.remove(playerId);

            // Debug info
//            Debug.logInfo("Slowness removed from " + livingTarget.getName());
        }
    }

    /**
     * Remove a specific modifier
     *
     * @param movementSpeed the movement speed attribute
     * @param modifierId the ID of the modifier to remove
     */
    private static void removeExistingModifier(AttributeInstance movementSpeed, UUID modifierId) {
        Collection<AttributeModifier> modifiers = movementSpeed.getModifiers();
        for (AttributeModifier modifier : modifiers) {
            if (modifier.getUniqueId().equals(modifierId)) {
                movementSpeed.removeModifier(modifier);
                break;
            }
        }
    }

    /**
     * B1: Remove the slowness modifier added by this plugin.
     * The old code removed every modifier, including ones from gear, potions, and other plugins.
     * Now it checks each modifier and only removes this plugin’s "SlownessEffect".
     *
     * @param attributeInstance the attribute instance
     */
    private static void removeAllModifiers(AttributeInstance attributeInstance) {
        Collection<AttributeModifier> modifiers = attributeInstance.getModifiers();
        for (AttributeModifier modifier : modifiers) {
            // Only remove this plugin’s slowness modifier (the name used in applySlowness)
            if ("SlownessEffect".equals(modifier.getName())) {
                attributeInstance.removeModifier(modifier);
            }
        }
    }

    /**
     * B3: Cancel all pending delayed removal tasks when the plugin unloads or reloads.
     */
    public static void cancelAll() {
        for (BukkitTask task : pendingRemoveTasks) {
            task.cancel(); // Cancel each delayed task
        }
        pendingRemoveTasks.clear(); // Clear the task list
    }

    /**
     * B4: Clean up session data when a player leaves (called by PlayerCleanupListener):
     * Remove this player’s saved slowness modifier ID so offline player data does not stay in the map and cause a memory leak.
     * The modifier on the entity stops working when it goes offline, so only the saved entry needs to be cleared.
     */
    public static void cleanup(UUID uuid) {
        playerSlownessModifierIds.remove(uuid);
    }

}

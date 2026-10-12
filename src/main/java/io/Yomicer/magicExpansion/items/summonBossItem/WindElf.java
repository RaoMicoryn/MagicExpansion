package io.Yomicer.magicExpansion.items.summonBossItem;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static io.Yomicer.magicExpansion.items.summonBossItem.bossSkill.FireZombieSkill.*;
import static org.bukkit.inventory.EquipmentSlot.HAND;

public class WindElf extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    public WindElf(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
    }

    @Nonnull
    @Override
    public ItemUseHandler getItemHandler() {
        return e -> {
            // Stop the default action (placing a block or using an item)
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);
            Player player = e.getPlayer();
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            // Check the item in the player's hand
            if (e.getHand()!= HAND) {
                player.sendMessage(ColorGradient.getGradientName("Please use your main hand!"));
                return;
            }

            // Reduce the item stack in hand
            if (itemInHand.getAmount() > 1) {
                itemInHand.setAmount(itemInHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null); // If there's only one left, remove it
            }

            spawnWindZombie(e);

        };
    }


    private void spawnWindZombie(PlayerRightClickEvent e) {

        Player player = e.getPlayer();
        // Get the player's location and spawn the mob
        Location location = player.getLocation();
        LivingEntity mob = (LivingEntity) location.getWorld().spawnEntity(location, EntityType.ALLAY);

        // Set the mob's name
        String zombieName = "§3§lWind Elf";
        mob.setCustomName(zombieName);
        mob.setCustomNameVisible(true);
        // Set custom metadata to identify this as a Wind Elf
        mob.setMetadata("magicMobType", new FixedMetadataValue(MagicExpansion.getInstance(), "WindElf"));
        mob.setMetadata("isInvincibleWindElf", new FixedMetadataValue(MagicExpansion.getInstance(), false)); // Set invincibility metadata
        // Set the max health and starting health
        double maxHealth = 200.0; // Custom max health
        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        mob.setHealth(maxHealth); // Set health to the max


        // Add a lightning effect
        worldStrikeLightningEffect(mob.getLocation());



        // Set up the skill list
        Runnable[] skills = {
                () -> magicAttackSkill(mob, zombieName),
                () -> redstoneParticleAttackSkillWindElf(mob),
                () -> twoWindParticleAttackSkill(mob),
        };

        // Use a random skill every 4–6 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (mob.isDead()) {
                    this.cancel(); // Stop the task if the mob dies
                    return;
                }
                // Change the name and add an invincible prefix
                mob.setMetadata("isInvincibleWindElf", new FixedMetadataValue(MagicExpansion.getInstance(), true));
                mob.setCustomName(zombieName + "§e§l[Invincible]");
                mob.setCustomNameVisible(true);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Restore the original name
                        mob.setMetadata("isInvincibleWindElf", new FixedMetadataValue(MagicExpansion.getInstance(), false));
                        mob.setCustomName(zombieName+ "§e§d[Weakened]");
                        mob.setCustomNameVisible(true);
                    }
                }.runTaskLater(MagicExpansion.getInstance(), 40L+ new Random().nextInt(20)); // Restore after 2–3 seconds

                // Pick a random skill to use
                int randomIndex = new Random().nextInt(skills.length);
                skills[randomIndex].run();
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, 80L + new Random().nextInt(41)); // Run every 4–6 seconds

        // Teleport every 4 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (mob.isDead()) {
                    this.cancel(); // Stop the task if the mob dies
                    return;
                }

                // Get nearby players
                List<Player> nearbyPlayers = getNearbyPlayers(mob);

                // Skip this teleport if no players are nearby
                if (nearbyPlayers.isEmpty()) {
                    return;
                }

                // Pick a random nearby player
                Player nearestPlayer = nearbyPlayers.get(new Random().nextInt(nearbyPlayers.size()));

                // Get the player's location and pick a random teleport spot
                Location playerLocation = nearestPlayer.getLocation();
                Random random = new Random();

                double x = playerLocation.getX() + (random.nextDouble() * 18 - 9); // Random X coordinate within a radius of 5
                double y = playerLocation.getY() + random.nextDouble() * 4;       // Y coordinate from 0 to 4 blocks higher
                double z = playerLocation.getZ() + (random.nextDouble() * 18 - 9); // Random Z coordinate within a radius of 5

                Location newLocation = new Location(mob.getWorld(), x, y, z);

                // Make sure the new spot is safe and not inside blocks
                while (!newLocation.getBlock().getType().isAir() || !newLocation.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                    y += 1; // Move up until there's enough space
                    newLocation.setY(y);
                }

                // Teleport the mob to the new spot
                mob.teleport(newLocation);
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, 80L); // Run every 4 seconds




    }



    // Create a lightning effect
    public void worldStrikeLightningEffect(Location location) {
        location.getWorld().strikeLightningEffect(location); // Visual effect only; it won't deal damage
    }

    private static List<Player> getNearbyPlayers(LivingEntity mob) {
        return mob.getWorld().getNearbyEntities(mob.getLocation(), 15, 8, 15).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .collect(Collectors.toList());
    }

}

package io.Yomicer.magicExpansion.items.misc;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.core.MagicExpansionItems;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.Yomicer.magicExpansion.items.summonBossItem.bossSkill.FireZombieSkill.*;

public class WindElfMB extends MultiBlockMachine {

    // Fix (J3): Spawn cooldowns (key = player UUID, value = last spawn time), 60-second cooldown
    private static final Map<UUID, Long> SPAWN_COOLDOWNS = new ConcurrentHashMap<>();
    // Fix (J1/J2): Keep track of Boss tasks (key = Boss entity UUID) and cancel them when the Boss dies or is removed, to avoid task leaks
    private static final Map<UUID, List<BukkitTask>> BOSS_TASKS = new ConcurrentHashMap<>();

    private static final long SPAWN_COOLDOWN_MS = 60_000L;          // Fix (J3): 60-second spawn cooldown
    private static final long MAX_ALIVE_TIME_MS = 5 * 60 * 1000L;   // Fix (J1): Boss can stay alive for up to 5 minutes
    private static final int MAX_NEARBY_SAME_BOSS = 3;              // Fix (J3): Maximum number of Bosses in the same area

    public WindElfMB(ItemGroup itemGroup, SlimefunItemStack item) {
        super(itemGroup, item, new ItemStack[] {null, MagicExpansionItems.WIND_ELF_HEAD, null, null, MagicExpansionItems.WIND_ELF_BODY, null, null, MagicExpansionItems.WIND_ELF_BODY, null}, BlockFace.SELF);
    }

    @Override
    public void onInteract(@Nonnull Player p, @Nonnull Block b) {
        Location location = b.getLocation().clone();
        Location locationUp = b.getRelative(BlockFace.UP).getLocation().clone();
        Location locationDown = b.getRelative(BlockFace.DOWN).getLocation().clone();
        Block pumpkinHead = b.getRelative(BlockFace.UP);
        Block bottomBlackstone = b.getRelative(BlockFace.DOWN);

        if(!StorageCacheUtils.isBlock(locationUp, "MAGIC_EXPANSION_WIND_ELF_HEAD") || !StorageCacheUtils.isBlock(location, "MAGIC_EXPANSION_WIND_ELF_BODY") || !StorageCacheUtils.isBlock(locationDown, "MAGIC_EXPANSION_WIND_ELF_BODY")) {

            p.sendMessage(ColorGradient.getGradientName("[Magic · Boss Summon] Build it the right way."));
            p.sendMessage(ColorGradient.getGradientName("[Magic · Boss Summon] Check the blocks you placed."));
            p.sendMessage(ColorGradient.getGradientName("[Magic · Boss Summon] This might be a Slimefun item."));
            return;
        }

        // Fix (J3): Check the 60-second cooldown before removing the build blocks
        long now = System.currentTimeMillis();
        Long lastSpawn = SPAWN_COOLDOWNS.get(p.getUniqueId());
        if (lastSpawn != null && now - lastSpawn < SPAWN_COOLDOWN_MS) {
            long remain = (SPAWN_COOLDOWN_MS - (now - lastSpawn)) / 1000 + 1;
            p.sendMessage(ColorGradient.getGradientName("[Magic · Boss Summon] Boss is on cooldown. Wait " + remain + " seconds!"));
            return;
        }

        // Fix (J3): Maximum number of Bosses in the same area —— block summoning if there are 3 or more of the same Boss within 20 blocks
        if (countNearbyBosses(location, "WindElf") >= MAX_NEARBY_SAME_BOSS) {
            p.sendMessage(ColorGradient.getGradientName("[Magic · Boss Summon] Too many of the same Boss nearby (already " + MAX_NEARBY_SAME_BOSS + " within 20 blocks). Cannot summon!"));
            return;
        }

        // Fix (J3): Save the cooldown after all checks pass
        SPAWN_COOLDOWNS.put(p.getUniqueId(), now);

        pumpkinHead.setType(Material.AIR);
        Slimefun.getDatabaseManager().getBlockDataController().removeBlock(b.getLocation().clone().add(0, 1, 0));
        b.setType(Material.AIR);
        Slimefun.getDatabaseManager().getBlockDataController().removeBlock(b.getLocation().clone());
        bottomBlackstone.setType(Material.AIR);
        Slimefun.getDatabaseManager().getBlockDataController().removeBlock(b.getLocation().clone().add(0, -1, 0));
        spawnWindZombie(b.getLocation().clone());
    }

    /**
     * Fix (J3): Count this plugin’s Bosses of a given type within 20 blocks using the magicMobType metadata
     */
    private static int countNearbyBosses(Location center, String mobType) {
        return (int) center.getWorld().getNearbyEntities(center, 20, 20, 20).stream()
                .filter(en -> en.hasMetadata("magicMobType")
                        && !en.getMetadata("magicMobType").isEmpty()
                        && mobType.equals(en.getMetadata("magicMobType").get(0).value()))
                .count();
    }

    /**
     * Fix (J1/J2): Cancel all tasks for the given Boss and remove it from the task list
     */
    private static void cancelBossTasks(UUID bossId) {
        List<BukkitTask> tasks = BOSS_TASKS.remove(bossId);
        if (tasks != null) {
            for (BukkitTask task : tasks) {
                task.cancel();
            }
        }
    }

    /**
     * Fix (J1): Watchdog check — return true if the Boss is dead, has been alive for over 5 minutes, or has no players within 60 blocks
     */
    private static boolean watchdogExpired(LivingEntity mob, long spawnTime) {
        if (mob.isDead()) return true;
        if (System.currentTimeMillis() - spawnTime > MAX_ALIVE_TIME_MS) return true;
        // No players within 60 blocks means it times out
        boolean hasPlayerNearby = mob.getWorld().getNearbyEntities(mob.getLocation(), 60, 60, 60).stream()
                .anyMatch(en -> en instanceof Player);
        return !hasPlayerNearby;
    }

    private void spawnWindZombie(Location location) {

        // Get the player’s location and spawn the mob
        LivingEntity mob = (LivingEntity) location.getWorld().spawnEntity(location, EntityType.ALLAY);

        // Set the mob’s name
        String zombieName = "§3§lWind Spirit";
        mob.setCustomName(zombieName);
        mob.setCustomNameVisible(true);
        // Set custom metadata to identify this as a Wind Spirit
        mob.setMetadata("magicMobType", new FixedMetadataValue(MagicExpansion.getInstance(), "WindElf"));
        mob.setMetadata("isInvincibleWindElf", new FixedMetadataValue(MagicExpansion.getInstance(), false)); // Set the invincible metadata
        // Set the max health and starting health
        double maxHealth = 200.0; // Custom max health
        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        mob.setHealth(maxHealth); // Set starting health to max health

        // Fix (J4): Stop the entity from despawning when far from players (the tasks and watchdog manage the Boss lifetime)
        mob.setRemoveWhenFarAway(false);

        // Add a lightning effect
        worldStrikeLightningEffect(mob.getLocation());

        // Fix (J1): Save the spawn time so the watchdog can check for timeouts
        final long spawnTime = System.currentTimeMillis();

        // Set up the skill list
        Runnable[] skills = {
                () -> magicAttackSkill(mob, zombieName),
                () -> redstoneParticleAttackSkillWindElf(mob),
                () -> twoWindParticleAttackSkill(mob),
        };

        // Fix (J2): Keep references to all tasks for this Boss in one place
        List<BukkitTask> tasks = new ArrayList<>();

        // Use a random skill every 4–6 seconds
        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                // Fix (J1): Watchdog — remove the Boss and cancel all tasks if it dies, times out (5 minutes), or has no players within 60 blocks
                if (watchdogExpired(mob, spawnTime)) {
                    this.cancel();
                    cancelBossTasks(mob.getUniqueId());
                    if (!mob.isDead()) {
                        mob.remove();
                    }
                    return;
                }
                // Change the name and add an “Invincible” tag
                mob.setMetadata("isInvincibleWindElf", new FixedMetadataValue(MagicExpansion.getInstance(), true));
                mob.setCustomName(zombieName + "§e§l[Invincible]");
                mob.setCustomNameVisible(true);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Restore the original name
                        mob.setMetadata("isInvincibleWindElf", new FixedMetadataValue(MagicExpansion.getInstance(), false));
                        mob.setCustomName(zombieName+ "§e§d[Weak]");
                        mob.setCustomNameVisible(true);
                    }
                }.runTaskLater(MagicExpansion.getInstance(), 40L+ new Random().nextInt(20)); // Restore after 2.5 seconds

                // Pick a random skill to use
                int randomIndex = new Random().nextInt(skills.length);
                skills[randomIndex].run();
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, 80L + new Random().nextInt(41))); // Runs every 4–6 seconds

        // Added: teleport every 4 seconds
        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                // Fix (J1): Watchdog — remove the Boss and cancel all tasks if it dies, times out, or has no players nearby
                if (watchdogExpired(mob, spawnTime)) {
                    this.cancel();
                    cancelBossTasks(mob.getUniqueId());
                    if (!mob.isDead()) {
                        mob.remove();
                    }
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

                // Get the player’s location and pick a random teleport spot
                Location playerLocation = nearestPlayer.getLocation();
                Random random = new Random();

                double x = playerLocation.getX() + (random.nextDouble() * 18 - 9); // Random X coordinate within 5 blocks
                double y = playerLocation.getY() + random.nextDouble() * 4;       // Y coordinate from 0 to +4
                double z = playerLocation.getZ() + (random.nextDouble() * 18 - 9); // Random Z coordinate within 5 blocks

                Location newLocation = new Location(mob.getWorld(), x, y, z);

                // Make sure the new spot is safe (so the mob doesn’t get stuck in blocks)
                while (!newLocation.getBlock().getType().isAir() || !newLocation.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                    y += 1; // Move up until there’s enough space
                    newLocation.setY(y);
                }

                // Teleport the mob to the new spot
                mob.teleport(newLocation);
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, 80L)); // Runs every 4 seconds

        // Fix (J2): Add task references to the task list so they can all be canceled when the Boss dies or is removed
        BOSS_TASKS.put(mob.getUniqueId(), tasks);
    }



    // Create a lightning effect
    public void worldStrikeLightningEffect(Location location) {
        location.getWorld().strikeLightningEffect(location); // Visual effect only; it does not deal damage
    }

    private static List<Player> getNearbyPlayers(LivingEntity mob) {
        return mob.getWorld().getNearbyEntities(mob.getLocation(), 10, 8, 10).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .collect(Collectors.toList());
    }

}

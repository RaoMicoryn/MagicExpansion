package io.Yomicer.magicExpansion.items.misc;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.core.MagicExpansionItems;
import io.Yomicer.magicExpansion.utils.ColorGradient;
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
import static io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils.isItemSimilar;

public class FireZombieMB extends MultiBlockMachine {

    // Fix (J3): Summon cooldown table (key=Player UUID, value=last summon timestamp), 60 sec cooldown
    private static final Map<UUID, Long> SPAWN_COOLDOWNS = new ConcurrentHashMap<>();
    // Fix (J1/J2): Boss active task reference table (key=Boss entity UUID), cancelled together on death/removal to prevent task leaks
    private static final Map<UUID, List<BukkitTask>> BOSS_TASKS = new ConcurrentHashMap<>();

    private static final long SPAWN_COOLDOWN_MS = 60_000L;          // Fix (J3): Summon cooldown 60 sec
    private static final long MAX_ALIVE_TIME_MS = 5 * 60 * 1000L;   // Fix (J1): Boss lives at most 5 minutes
    private static final int MAX_NEARBY_SAME_BOSS = 3;              // Fix (J3): Limit of the same kind in one area

    public FireZombieMB(ItemGroup itemGroup, SlimefunItemStack item) {
        super(itemGroup, item, new ItemStack[] {null, MagicExpansionItems.FIREZOMBIE_HEAD, null, null, MagicExpansionItems.FIREZOMBIE_BODY, null, null, MagicExpansionItems.FIREZOMBIE_BODY, null}, BlockFace.SELF);
    }

    @Override
    public void onInteract(@Nonnull Player p, @Nonnull Block b) {
        Location location = b.getLocation().clone();
        Location locationUp = b.getRelative(BlockFace.UP).getLocation().clone();
        Location locationDown = b.getRelative(BlockFace.DOWN).getLocation().clone();
        Block pumpkinHead = b.getRelative(BlockFace.UP);
        Block bottomBlackstone = b.getRelative(BlockFace.DOWN);

        if(!StorageCacheUtils.isBlock(locationUp, "MAGIC_EXPANSION_FIREZOMBIE_HEAD") || !StorageCacheUtils.isBlock(location, "MAGIC_EXPANSION_FIREZOMBIE_BODY") || !StorageCacheUtils.isBlock(locationDown, "MAGIC_EXPANSION_FIREZOMBIE_BODY")) {

            p.sendMessage(ColorGradient.getGradientName("[Magic · BOSS Summon]You need to build it the right way"));
            p.sendMessage(ColorGradient.getGradientName("[Magic · BOSS Summon]Check the blocks you placed"));
            p.sendMessage(ColorGradient.getGradientName("[Magic · BOSS Summon]It might be a Slimefun item"));
            return;
        }

        // Fix (J3): Summon cooldown check (60 sec), done before removing the built blocks
        long now = System.currentTimeMillis();
        Long lastSpawn = SPAWN_COOLDOWNS.get(p.getUniqueId());
        if (lastSpawn != null && now - lastSpawn < SPAWN_COOLDOWN_MS) {
            long remain = (SPAWN_COOLDOWN_MS - (now - lastSpawn)) / 1000 + 1;
            p.sendMessage(ColorGradient.getGradientName("[Magic · BOSS Summon]BOSS summon is on cooldown, you still need to wait " + remain + " sec!"));
            return;
        }

        // Fix (J3): Limit of the same kind in one area —— 20 blocks: same-type Boss count >= 3 then refuse to summon
        if (countNearbyBosses(location, "FireZombie") >= MAX_NEARBY_SAME_BOSS) {
            p.sendMessage(ColorGradient.getGradientName("[Magic · BOSS Summon]Too many of the same BOSS nearby (reached the max of " + MAX_NEARBY_SAME_BOSS + " within 20 blocks), can't summon!"));
            return;
        }

        // Fix (J3): Record the cooldown time after passing the checks
        SPAWN_COOLDOWNS.put(p.getUniqueId(), now);

        pumpkinHead.setType(Material.AIR);
        Slimefun.getDatabaseManager().getBlockDataController().removeBlock(b.getLocation().clone().add(0, 1, 0));
        b.setType(Material.AIR);
        Slimefun.getDatabaseManager().getBlockDataController().removeBlock(b.getLocation().clone());
        bottomBlackstone.setType(Material.AIR);
        Slimefun.getDatabaseManager().getBlockDataController().removeBlock(b.getLocation().clone().add(0, -1, 0));
        spawnFireZombie(b.getLocation().clone());
    }

    /**
     * Fix (J3): Count this plugin's BOSSes of the given type within 20 blocks (identified by the magicMobType metadata)
     */
    private static int countNearbyBosses(Location center, String mobType) {
        return (int) center.getWorld().getNearbyEntities(center, 20, 20, 20).stream()
                .filter(en -> en.hasMetadata("magicMobType")
                        && !en.getMetadata("magicMobType").isEmpty()
                        && mobType.equals(en.getMetadata("magicMobType").get(0).value()))
                .count();
    }

    /**
     * Fix (J1/J2): Cancel all tasks of the given Boss and remove it from the task table
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
     * Fix (J1): Watchdog check —— Boss dead / alive for over 5 minutes / no player within 60 blocks, returns true
     */
    private static boolean watchdogExpired(LivingEntity mob, long spawnTime) {
        if (mob.isDead()) return true;
        if (System.currentTimeMillis() - spawnTime > MAX_ALIVE_TIME_MS) return true;
        // 60 blocks with no player → Timeout
        boolean hasPlayerNearby = mob.getWorld().getNearbyEntities(mob.getLocation(), 60, 60, 60).stream()
                .anyMatch(en -> en instanceof Player);
        return !hasPlayerNearby;
    }

    private void spawnFireZombie(Location location) {

        // Get the player location and spawn the mob
        LivingEntity mob = (LivingEntity) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);

        // Set mob name
        String zombieName = "§c§lFlame Zombie";
        mob.setCustomName(zombieName);
        mob.setCustomNameVisible(true);
        // Set custom metadata: used to mark this as a Fire Zombie
        mob.setMetadata("magicMobType", new FixedMetadataValue(MagicExpansion.getInstance(), "FireZombie"));
        mob.setMetadata("isInvincibleFireZombie", new FixedMetadataValue(MagicExpansion.getInstance(), false)); // Set invincible metadata
        // Adjust max health and set starting health
        double maxHealth = 200.0; // Custom max health
        mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        mob.setHealth(maxHealth); // Set starting health to max health

        // Fix (J4): Don't let the entity get cleaned up automatically for being far from players (the BOSS lifecycle is managed by the tasks and watchdog)
        mob.setRemoveWhenFarAway(false);

        // Add lightning effect
        worldStrikeLightningEffect(mob.getLocation());

        // Fix (J1): Record the spawn time, for the watchdog to check timeout
        final long spawnTime = System.currentTimeMillis();

        // Define the skill list
        Runnable[] skills = {
                () -> magicAttackSkill(mob, zombieName),
                () -> redstoneParticleAttackSkill(mob),
                () -> fireParticleAttackSkill(mob),
        };

        // Fix (J2): Collect all task references of this Boss and manage them together
        List<BukkitTask> tasks = new ArrayList<>();

        // Cast a random skill every 4-8 seconds
        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                // Fix (J1): Watchdog —— dead/timed out(5 min)/60 blocks with no player → remove the Boss and cancel all tasks
                if (watchdogExpired(mob, spawnTime)) {
                    this.cancel();
                    cancelBossTasks(mob.getUniqueId());
                    if (!mob.isDead()) {
                        mob.remove();
                    }
                    return;
                }
                // Change the name, add“Untargetable·”prefix
                mob.setMetadata("isInvincibleFireZombie", new FixedMetadataValue(MagicExpansion.getInstance(), true));
                mob.setCustomName(zombieName + "§e§l[Super Armor]");
                mob.setCustomNameVisible(true);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Restore the original name
                        mob.setMetadata("isInvincibleFireZombie", new FixedMetadataValue(MagicExpansion.getInstance(), false));
                        mob.setCustomName(zombieName);
                        mob.setCustomNameVisible(true);
                    }
                }.runTaskLater(MagicExpansion.getInstance(), 50L); // 2.5 sec, then back to normal

                // Pick a random skill to cast
                int randomIndex = new Random().nextInt(skills.length);
                skills[randomIndex].run();
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, 80L + new Random().nextInt(81))); // Runs once every 4-8 seconds

        // Added: teleport once every 11 seconds
        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                // Fix (J1): Watchdog —— dead/timed out/no player → remove the Boss and cancel all tasks
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

                // If no player is in range, cancel this teleport
                if (nearbyPlayers.isEmpty()) {
                    return;
                }

                // Pick a random nearby player
                Player nearestPlayer = nearbyPlayers.get(new Random().nextInt(nearbyPlayers.size()));

                // Get the player's location and work out a random teleport spot
                Location playerLocation = nearestPlayer.getLocation();
                Random random = new Random();

                double x = playerLocation.getX() + (random.nextDouble() * 18 - 9); // Random X coordinate within a radius of 5
                double y = playerLocation.getY() + random.nextDouble() * 4;       // Y Coordinates+0 to+4
                double z = playerLocation.getZ() + (random.nextDouble() * 18 - 9); // Random Z coordinate within a radius of 5

                Location newLocation = new Location(mob.getWorld(), x, y, z);

                // Make sure the new location is safe (avoid getting stuck in blocks)
                while (!newLocation.getBlock().getType().isAir() || !newLocation.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                    y += 1; // Move up until an open spot is found
                    newLocation.setY(y);
                }

                // Teleport the mob to the new location
                mob.teleport(newLocation);
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, 220L)); // Runs once every 11 seconds

        // Fix (J2): Register the task references in the task table, cancelled together when the Boss dies/is removed
        BOSS_TASKS.put(mob.getUniqueId(), tasks);
    }

    // Spawn lightning effect
    public void worldStrikeLightningEffect(Location location) {
        location.getWorld().strikeLightningEffect(location); // Visual effect only, no damage
    }

    private static List<Player> getNearbyPlayers(LivingEntity mob) {
        return mob.getWorld().getNearbyEntities(mob.getLocation(), 10, 8, 10).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .collect(Collectors.toList());
    }

}
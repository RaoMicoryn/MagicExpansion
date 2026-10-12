package io.Yomicer.magicExpansion.Listener.bossListener;

import io.Yomicer.magicExpansion.utils.log.Debug;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class BasicBossAttackListener implements Listener {

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {


        if (event.getEntity() instanceof LivingEntity mob && mob.getType() == EntityType.ZOMBIE) {

            if (mob.hasMetadata("isInvincibleFireZombie")) {
                boolean isInvincibleFireZombie = mob.getMetadata("isInvincibleFireZombie").get(0).asBoolean();

                if (isInvincibleFireZombie) {
                    event.setDamage(0.01);
                }else{
                    double damage = event.getDamage();
                    double maxHealth = mob.getMaxHealth();
                    double threshold = maxHealth * 0.05;
                    if (damage > threshold) {
                        event.setDamage(threshold);
                    }
                }

            }
        }

    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {

        // G: Quick check 1 — the attacker must be a player. Otherwise, return right away (the old code counted nearby players every time, which wasted work).
        if (!(e.getDamager() instanceof Player player)) return;

        // G: Quick check 2 — the target must be a living entity.
        if (!(e.getEntity() instanceof LivingEntity mob)) return;

        // G: Quick check 3 — the target must have this plugin's Boss metadata, so normal mobs are skipped.
        if (!mob.hasMetadata("isInvincibleWindElf")) return;

        boolean isInvincibleWindElf = mob.getMetadata("isInvincibleWindElf").get(0).asBoolean();

        // G: Count nearby players only after these checks pass (the old code did this even when no Boss was hit).
        Location mobLocation = mob.getLocation();
        World world = mobLocation.getWorld();
        double radius = 20.0; // Range: 20 blocks
        double radiusSquared = radius * radius;
        int playerCount = 0;
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(mobLocation) <= radiusSquared) {
                playerCount++;
            }
        }

        // G: Apply these damage rules only to the Wind Elf Boss (ALLAY type).
        if (mob.getType() == EntityType.ALLAY) {
            ItemStack weapon = player.getInventory().getItemInMainHand();
            if (isInvincibleWindElf || !isSword(weapon) || playerCount < 2 || playerCount > 5) {
                e.setDamage(0.0);
            } else {
                double damage = e.getDamage();
                double maxHealth = mob.getMaxHealth();
                double threshold = maxHealth * 0.33;
                if (damage > threshold) {
                    e.setDamage(threshold);
                }
            }
        }

    }


    /**
     * Checks whether the item is a sword.
     */
    private boolean isSword(ItemStack item) {
        if (item == null) return false;
        Material type = item.getType();
        return type == Material.WOODEN_SWORD ||
                type == Material.STONE_SWORD ||
                type == Material.GOLDEN_SWORD ||
                type == Material.IRON_SWORD ||
                type == Material.DIAMOND_SWORD ||
                type == Material.NETHERITE_SWORD; // If your server version supports Netherite Swords.
    }




    }

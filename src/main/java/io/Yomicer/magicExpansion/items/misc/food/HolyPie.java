package io.Yomicer.magicExpansion.items.misc.food; // Change this to your package name

import io.Yomicer.magicExpansion.MagicExpansion;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HolyPie extends SimpleSlimefunItem<ItemConsumptionHandler> implements Listener {

    // Fix (R): now it's uuid -> protection end time (in milliseconds). If you eat another one, the new time replaces the old one.
    // The removal task only clears the entry if the time is still its own, so eating several in a row doesn't cancel each other's protection
    private static final Map<UUID, Long> holyProtectedPlayers = new ConcurrentHashMap<>();

    public HolyPie(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        Bukkit.getPluginManager().registerEvents(this, MagicExpansion.getInstance());
    }
    public HolyPie(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);
        Bukkit.getPluginManager().registerEvents(this, MagicExpansion.getInstance());
    }

    @Override
    public @NotNull ItemConsumptionHandler getItemHandler() {
        return (e, p, i) -> {
            double maxHealth = p.getMaxHealth();
            p.setHealth(Math.min(p.getHealth() + 12.0, maxHealth)); // Heals 6 hearts
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 30, 254));
            removeNegativeEffects(p);
            // Fix (R): save the time the protection ends (30 ticks = 1500ms); eating another one replaces it with the new time
            long expireAt = System.currentTimeMillis() + 1500L;
            holyProtectedPlayers.put(p.getUniqueId(), expireAt);
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Fix (R): only clear it if the saved end time matches this task's time,
                    // so when you eat a second one, the first one's expired task doesn't cancel the protection early
                    Long current = holyProtectedPlayers.get(p.getUniqueId());
                    if (current != null && current == expireAt) {
                        holyProtectedPlayers.remove(p.getUniqueId());
                        if (p.isOnline()) {
                            p.sendMessage(ChatColor.GRAY + "§7The holy protection has faded...");
                        }
                    }
                }
            }.runTaskLater(MagicExpansion.getInstance(), 30L);
            playHolyEffects(p);
            p.sendMessage(ChatColor.GOLD + "✨ " + ChatColor.BOLD + "Holy Protection activated!" + ChatColor.RESET + ChatColor.GRAY + " (immune to all damage)");
        };
    }

    /**
     * Play the holy effects
     */
    private void playHolyEffects(Player p) {
        Location loc = p.getLocation().add(0, 1, 0);
        World world = p.getWorld();
        p.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 0.8f, 1.5f); // Dreamy feeling
        p.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f); // Achievement feeling
        p.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f); // Level-up feeling
        world.spawnParticle(Particle.END_ROD, loc, 30, 0.5, 0.5, 0.5, 0.05);
        world.spawnParticle(Particle.HEART, loc, 15, 0.4, 0.4, 0.4, 0.1);
        world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 20, 0.5, 0.5, 0.5, 0.1);
        new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    // Fix (R): the collection type is now a Map, so use containsKey to check if the protection is still active
                    if (!p.isOnline() || !holyProtectedPlayers.containsKey(p.getUniqueId())) {
                        this.cancel();
                        return;
                    }
                Location currentLoc = p.getLocation().add(0, 1.5, 0);
                // Draw a circle
                for (double angle = 0; angle < 360; angle += 45) {
                    double rad = Math.toRadians(angle);
                    double x = currentLoc.getX() + (Math.cos(rad) * 0.8);
                    double z = currentLoc.getZ() + (Math.sin(rad) * 0.8);
                    world.spawnParticle(Particle.FLASH, new Location(world, x, currentLoc.getY(), z), 1, 0, 0, 0, 0, Color.WHITE);
                }
                ticks++;
                if (ticks >= 10) this.cancel();
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, 2L);
    }

    /**
     * Remove negative effects
     */
    private void removeNegativeEffects(Player p) {
        List<PotionEffectType> toRemove = new ArrayList<>();
        for (PotionEffect effect : p.getActivePotionEffects()) {
            if (isNegative(effect.getType())) {
                toRemove.add(effect.getType());
            }
        }
        for (PotionEffectType type : toRemove) {
            p.removePotionEffect(type);
        }
    }

    private boolean isNegative(PotionEffectType type) {
        String name = type.getName();
        // Fix (R): explicitly leave out Slow Falling (SLOW_FALLING). It has the word SLOW in it but it's a good effect, so it shouldn't be removed
        if (name.contains("SLOW")) {
            return !name.contains("SLOW_FALLING");
        }
        return name.contains("POISON") || name.contains("WITHER") || name.contains("BLINDNESS")
                || name.contains("CONFUSION") || name.contains("HUNGER") || name.contains("WEAKNESS")
                || name.contains("DIGGING") || name.contains("LEVITATION")
                || name.contains("UNLUCK") || name.contains("BAD_OMEN") || name.contains("DARKNESS");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        if (holyProtectedPlayers.containsKey(p.getUniqueId())) {
            // Fix (R): VOID damage is not cancelled (otherwise you'd be unkillable while hanging in the air and get stuck in the void), only other damage is cancelled
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                return;
            }
            event.setCancelled(true);
            p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().add(0, 0.5, 0), 5, 0.2, 0.2, 0.2, 0.01);
        }
    }

}
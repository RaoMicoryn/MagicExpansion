package io.Yomicer.magicExpansion.items.misc.food; // Change this to your package name

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class CloudBread extends SimpleSlimefunItem<ItemConsumptionHandler> {

    public CloudBread(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }
    public CloudBread(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);
    }

    @Override
    public @NotNull ItemConsumptionHandler getItemHandler() {
        return (e, p, i) -> {
            // --- 1. Basic healing ---
            p.setHealth(Math.min(p.getHealth() + 8.0, p.getMaxHealth())); // Heals 4 hearts

            // --- 2. Main effect: Light Body (15 seconds) ---
            // Jump Boost III: you jump really high
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 300, 2));
            // Slow Falling: you fall very slowly and take no fall damage
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 300, 0));

            // --- 3. Special trick: cancel falling speed right away ---
            // If the player is falling (vertical speed < 0), set the vertical speed to 0 right away, so they "stop in mid-air"
            Vector velocity = p.getVelocity();
            if (velocity.getY() < -0.5) {
                velocity.setY(0);
                p.setVelocity(velocity);
                // Spawn a puff of cloud to show the cushion
                p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().add(0, 0.2, 0), 20, 0.3, 0.1, 0.3, 0.05);
                p.sendMessage("§f☁ A cloud caught you!");
            }

            // --- 4. Visual and sound feedback ---
            playCloudEffects(p);

            p.sendMessage("§f§l☁ You feel light as a feather... (§7Jump Boost & Slow Falling for 15 seconds§f)");
        };
    }

    /**
     * Play the cloud effects
     */
    private void playCloudEffects(Player p) {
        Location loc = p.getLocation().add(0, 0.5, 0);
        World world = p.getWorld();

        // Sounds: a soft wind or bubble sound
        p.playSound(loc, Sound.BLOCK_SNOW_PLACE, 1.0f, 1.5f); // Sounds like stepping on snow / something fluffy
        p.playSound(loc, Sound.ENTITY_GHAST_AMBIENT, 0.5f, 2.0f); // A dreamy background sound

        // Particles: lots of clouds (CLOUD) and smoke (SMOKE_NORMAL)
        world.spawnParticle(Particle.CLOUD, loc, 30, 0.5, 0.2, 0.5, 0.05);
        world.spawnParticle(Particle.SMOKE, loc, 15, 0.4, 0.1, 0.4, 0.02);

        // Little bonus: if you eat it in the air, a ring of white light shows up
        if (!p.isOnGround()) {
            for (int i = 0; i < 360; i += 30) {
                double rad = Math.toRadians(i);
                double x = loc.getX() + Math.cos(rad) * 0.6;
                double z = loc.getZ() + Math.sin(rad) * 0.6;
                world.spawnParticle(Particle.FLASH, new Location(world, x, loc.getY(), z), 1, 0, 0, 0, 0, Color.WHITE);
            }
        }
    }
}
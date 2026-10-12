package io.Yomicer.magicExpansion.items.misc;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.utils.ItemPermissionUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class WordClear extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {
    public WordClear(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return e -> {
            Player p = e.getPlayer();
            Location loc = p.getLocation();
            UUID uuid = p.getUniqueId();


            // --- Check permissions ---
            if (!ItemPermissionUtils.hasPermissionPoint(p, loc)) {
                p.sendMessage("§cYou can’t remove hologram text in this area!");
                p.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                return;
            }

            // --- Find the nearest hologram armor stand ---
            List<Entity> entities = (List<Entity>) (List<?>) loc.getNearbyEntities(3, 3, 3);
            ArmorStand nearestHologram = null;
            double nearestDistance = 999;

            for (Entity en : entities) {
                if (en.getType() == EntityType.ARMOR_STAND) {
                    ArmorStand as = (ArmorStand) en;
                    // Check if this armor stand is used for holograms (invisible, no gravity, marker)
                    // Fix (U): Check that CustomName is not empty, so only hologram armor stands with text are removed,
                    // to avoid removing unnamed armor stands used by other plugins or systems.
                    // Note: The placement code (such as DrawMachine.createTextArmorStand) does not add plugin metadata/PDC tags
                    // and that code isn’t part of this edit, so we identify our holograms by checking for an invisible marker with a custom name
                    if (as.isInvisible() && !as.hasGravity() && as.isMarker() && as.getCustomName() != null) {
                        double distance = loc.distance(as.getLocation());
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestHologram = as;
                        }
                    }
                }
            }

            // --- Handle the logic ---
            String metaKey = "hologramToRemove";
            List<MetadataValue> meta = p.getMetadata(metaKey);
            ArmorStand pendingStand = null;

            // Check if there is a target waiting for confirmation
            if (meta != null && !meta.isEmpty()) {
                // Try to get the saved armor stand reference (this assumes the entity itself was saved)
                // Note: An entity reference may stop working when its chunk unloads, but it should be fine for short-term use
                try {
                    pendingStand = (ArmorStand) meta.get(0).value();
                } catch (Exception ignored) {}
            }

            // If the player already picked one and clicks the same one again, remove it
            if (pendingStand != null && pendingStand.isValid()) {
                // If the nearest one is the same as the previously selected one
                if (nearestHologram != null && nearestHologram.getUniqueId().equals(pendingStand.getUniqueId())) {
                    // Check permissions again
                    if (!ItemPermissionUtils.hasPermissionPoint(p, nearestHologram.getLocation())) {
                        p.sendMessage("§cYou can’t remove this hologram text!");
                        p.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                        p.removeMetadata(metaKey, MagicExpansion.getInstance());
                        return;
                    }

                    // Remove it
                    nearestHologram.remove();
                    p.sendMessage("§6Hologram text removed!");
                    p.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
                    p.removeMetadata(metaKey, MagicExpansion.getInstance());
                    return;
                }
            }

            // If there’s no pending target, the old one is no longer valid, or the player picked a new one
            if (nearestHologram != null) {
                // Set the metadata marker
                p.setMetadata(metaKey, new FixedMetadataValue(MagicExpansion.getInstance(), nearestHologram));

                String content = nearestHologram.getCustomName();
                if (content == null) content = "§7(No text)";

                p.sendMessage("§eNearest hologram text found: " + content);
                p.sendMessage("§7Distance: " + String.format("%.1f", nearestDistance) + " blocks away. Right-click again to remove it!");
                p.playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.0f);
            } else {
                // Nothing found nearby
                p.sendMessage("§bNo hologram text found within 3 blocks!");
                p.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                // Clear invalid metadata
                p.removeMetadata(metaKey, MagicExpansion.getInstance());
            }
        };
    }
}

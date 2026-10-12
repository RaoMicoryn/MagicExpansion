package io.Yomicer.magicExpansion.items.misc;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.utils.FishingGuideMenu;
import io.Yomicer.magicExpansion.utils.ItemPermissionUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.guizhanss.guizhanlib.minecraft.helper.entity.EntityHelper.getDisplayName;

public class DeathLifeBook extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    public DeathLifeBook(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return e -> {
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);
            Player player = e.getPlayer();
            if (!ItemPermissionUtils.hasPermissionOnlyOnAttackEntity(player)){
                return;
            }
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            openEntityMenu(player, 0);
        };
    }

    private void openEntityMenu(Player player, int page) {
        // Scan living creatures within 20 blocks (excluding players, armor stands, dropped items and other non-living entities)
        List<LivingEntity> nearbyEntities = player.getLocation().getWorld()
                .getNearbyEntities(player.getLocation(), 20, 20, 20)
                .stream()
                .filter(entity -> {
                    // Keep only living creatures, exclude players, armor stands, item frames, dropped items, etc.
                    if (!(entity instanceof LivingEntity)) return false;
                    if (entity instanceof Player) return false;
                    if (entity instanceof ArmorStand) return false;
                    if (entity instanceof ItemFrame) return false;
                    if (entity instanceof Item) return false;
                    if (entity instanceof ExperienceOrb) return false;
                    if (entity instanceof Projectile) return false;
                    if (entity instanceof Vehicle) return false;
                    if (entity instanceof Hanging) return false;
                    if (entity instanceof EnderCrystal) return false;
                    if (entity instanceof TNTPrimed) return false;
                    if (entity instanceof FallingBlock) return false;
                    if (entity instanceof AreaEffectCloud) return false;

                    // Make sure it's a living creature
                    LivingEntity living = (LivingEntity) entity;
                    return living.getHealth() > 0;
                })
                .map(entity -> (LivingEntity) entity)
                .collect(Collectors.toList());

        if (nearbyEntities.isEmpty()) {
            player.sendMessage("§cNo creatures found nearby!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            return;
        }

        // Pagination settings
        int entitiesPerPage = 45; // Show 45 creatures per page
        int totalPages = (int) Math.ceil((double) nearbyEntities.size() / entitiesPerPage);

        // Make sure the page number is in the valid range
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        // Calculate the creature range for the current page
        int startIndex = page * entitiesPerPage;
        int endIndex = Math.min(startIndex + entitiesPerPage, nearbyEntities.size());

        // Create menu - save the title for parsing later
        final String menuTitle = "§5Book of Life and Death §7- Page " + (page + 1) + "/" + totalPages + " ";
        ChestMenu menu = new ChestMenu(menuTitle);

        // Use a final variable to avoid lambda issues
        final List<LivingEntity> finalEntities = nearbyEntities;
        final int finalPage = page;

        // Add the current page's creatures to the menu
        for (int i = startIndex; i < endIndex; i++) {
            int slot = i - startIndex;
            LivingEntity entity = nearbyEntities.get(i);
            addEntityToMenu(menu, slot, entity, player, menuTitle);
        }

        // Add the previous page button (if there is a previous page)
        if (page > 0) {
            menu.addItem(45, new CustomItemStack(
                    Material.ARROW,
                    "§6Previous page",
                    "§7Click to see the previous page",
                    "§eCurrent: Page " + (page + 1) + " ",
                    "§aFound: " + nearbyEntities.size() + " creatures"
            ), (p, slot, item, action) -> {
                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.0f);
                openEntityMenu(p, finalPage - 1);
                return false;
            });
        }

        // Add the one-click clear button
        menu.addItem(49, new CustomItemStack(
                Material.TNT,
                "§c§lClear all creatures in one click",
                "§7Click to clear all creatures within 20 blocks",
                "§eAmount: §6" + nearbyEntities.size() + " creatures",
                "§4Warning: This action can't be undone!"
        ), (p, slot, item, action) -> {
            // Add a stronger blindness effect
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 2)); // 3sec stronger blindness
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 2)); // 4sec stronger nausea effect

            // Clear all creatures
            int killedCount = 0;
            for (LivingEntity entity : finalEntities) {
                if (!entity.isDead()) {
                    entity.remove();
                    killedCount++;
                }
            }

            p.sendMessage("§aCleared §e" + killedCount + " §acreatures!");
            p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
            p.closeInventory();
            return false;
        });

        // Add the next page button (if there is a next page)
        if (page < totalPages - 1) {
            menu.addItem(53, new CustomItemStack(
                    Material.ARROW,
                    "§6Next page",
                    "§7Click to see the next page",
                    "§eCurrent: Page " + (page + 1) + " ",
                    "§aFound: " + nearbyEntities.size() + " creatures"
            ), (p, slot, item, action) -> {
                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.0f);
                openEntityMenu(p, finalPage + 1);
                return false;
            });
        }

        // Add page number info
        menu.addItem(48, new CustomItemStack(
                Material.BOOK,
                "§5Book of Life and Death",
                "§7Current page number: §e" + (page + 1) + "§7/§e" + totalPages,
                "§7Creatures found: §a" + nearbyEntities.size() + " pcs",
                "§7Scan radius: §b20 blocks"
        ), (p, slot, item, action) -> false);

        // Open menu
        menu.open(player);
    }

    private void addEntityToMenu(ChestMenu menu, int slot, LivingEntity entity, Player player, String menuTitle) {
        // Get creature info
        String entityName = getDisplayName(entity);
        double health = entity.getHealth();
        double maxHealth = entity.getMaxHealth();
        Location loc = entity.getLocation();
        String worldName = loc.getWorld().getName();
        double distance = player.getLocation().distance(loc);

        // Set the color based on health
        String healthColor = "§a"; // Green - healthy
        if (health < maxHealth * 0.3) {
            healthColor = "§c"; // Red - nearly dead
        } else if (health < maxHealth * 0.7) {
            healthColor = "§6"; // Gold - hurt
        }

        // Create display item
        Material icon = getEntityIcon(entity);
        List<String> lore = Arrays.asList(
                "§7Health: " + healthColor + String.format("%.1f", health) + "§7/§a" + String.format("%.1f", maxHealth),
                "§7Coordinates: §eX: " + (int)loc.getX() + " Y: " + (int)loc.getY() + " Z: " + (int)loc.getZ(),
                "§7World: §b" + worldName,
                "§7Distance: §6" + String.format("%.1f", distance) + " blocks",
                "",
                "§aLeft click §8-> §eTeleport to creature",
                "§cRight click §8-> §4Kill creature"
        );

        CustomItemStack menuItem = new CustomItemStack(icon, "§6" + entityName, lore.toArray(new String[0]));

        // Use a final variable to avoid lambda issues
        final LivingEntity finalEntity = entity;
        final String finalEntityName = entityName;

        // Add click event
        menu.addItem(slot, menuItem, (p, menuSlot, itemStack, clickAction) -> {
            if (!clickAction.isRightClicked()) {
                // Left-click to teleport
                if (!finalEntity.isDead()) {
                    // Add a short blindness effect
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1)); // 2sec blindness (40 ticks)
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 1)); // 3sec nausea effect, makes the teleport feel stronger

                    p.teleport(finalEntity.getLocation().add(0, 1, 0));
                    p.sendMessage("§aTeleported to §e" + finalEntityName + " §a's location!");
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                } else {
                    p.sendMessage("§cThat creature no longer exists!");
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                }
                p.closeInventory();
            } else if (clickAction.isRightClicked()) {
                // Right-click to kill
                if (!finalEntity.isDead()) {
                    // Add a short blindness effect
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1)); // 1sec blindness (20 ticks)

                    // Play death sound
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_DEATH, 0.8f, 1.0f);

                    // Delay by one tick so the sound plays first
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            finalEntity.remove();
                            p.sendMessage("§aErased from the Book of Life and Death §e" + finalEntityName + "§a!");

                            // Refresh current page menu
                            int currentPage = getCurrentPageFromTitle(menuTitle);
                            openEntityMenu(p, currentPage);
                        }
                    }.runTaskLater(MagicExpansion.getInstance(), 1L);
                } else {
                    p.sendMessage("§cThat creature no longer exists!");
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    p.closeInventory();
                }
            }
            return false;
        });
    }

    private Material getEntityIcon(LivingEntity entity) {
        // Get the matching spawn egg from the creature type name
        String entityTypeName = entity.getType().name();

        try {
            // Try to get the matching spawn egg
            Material spawnEgg = Material.valueOf(entityTypeName + "_SPAWN_EGG");
            return spawnEgg;
        } catch (IllegalArgumentException e) {
            // If no matching spawn egg is found, returnNAME_TAG
            return Material.NAME_TAG;
        }
    }

    // Get the current page number from the menu title
    private int getCurrentPageFromTitle(String title) {
        try {
            // Title format: "§5Book of Life and Death §7- Page X/Y"
            String[] parts = title.split(" ");
            for (String part : parts) {
                if (part.contains("/")) {
                    String pageInfo = part;
                    String currentPage = pageInfo.split("/")[0];
                    return Integer.parseInt(currentPage) - 1; // Return 0-based page number
                }
            }
            return 0; // If parsing fails, return the first page
        } catch (Exception e) {
            return 0; // If parsing fails, return the first page
        }
    }
}
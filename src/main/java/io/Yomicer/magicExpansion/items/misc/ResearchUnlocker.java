package io.Yomicer.magicExpansion.items.misc;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class ResearchUnlocker extends SimpleSlimefunItem<ItemUseHandler> {

    private final Random random = new Random();

    public ResearchUnlocker(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
    }

    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return e -> {
            e.setUseItem(Event.Result.DENY);
            e.cancel();

            // Only triggers with the main hand
            if (e.getHand() != EquipmentSlot.HAND) {
                return;
            }

            Player player = e.getPlayer();

            // Get the player profile asynchronously (to avoid lag)
            PlayerProfile.get(player, profile -> {
                Bukkit.getScheduler().runTask(MagicExpansion.getInstance(), () -> {
                    // Get all researches
                    List<Research> allResearches = new ArrayList<>(Slimefun.getRegistry().getResearches());

                    // Get the researches the player has unlocked
                    List<Research> unlockedResearches = new ArrayList<>();
                    for (Research research : allResearches) {
                        if (profile.hasUnlocked(research)) {
                            unlockedResearches.add(research);
                        }
                    }

                    // Check if everything is unlocked
                    if (unlockedResearches.size() >= allResearches.size()) {
                        // All unlocked message
                        sendAllUnlockedMessage(player);
                    } else {
                        // Find the locked research
                        List<Research> lockedResearches = new ArrayList<>(allResearches);
                        lockedResearches.removeAll(unlockedResearches);

                        if (!lockedResearches.isEmpty()) {
                            // Pick a random locked research
                            Research target = lockedResearches.get(random.nextInt(lockedResearches.size()));

                            // Unlock the research with the API (no longer using commands)
                            profile.setResearched(target, true);
                            profile.save(); // Save the changes right away

                            // Play sound
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                            player.sendMessage(ChatColor.GREEN + "✓ Research unlocked: " + ChatColor.YELLOW + target.getName(player));
                        }
                    }

                    // Use up the item (optional)
                    ItemStack item = e.getItem();
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        player.getInventory().setItemInMainHand(null);
                    }
                });
            });
        };
    }

    /**
     * Send the all-unlocked message
     */
    private void sendAllUnlockedMessage(Player player) {
        // Send an eye-catching message
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "════════════════════════════════");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "   You're already an all-knowing god!  ");
        player.sendMessage(ChatColor.GOLD + "════════════════════════════════");
        player.sendMessage("");

        // Play a special sound
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Send title (1.8+supported)
        player.sendTitle(
                ChatColor.GOLD + "All-Knowing",
                ChatColor.YELLOW + "You're already a god",
                10, 70, 20
        );
    }

    /**
     * Unlock the given research
     */
    private void unlockResearch(Player player, Research research) {
        // Method 1: unlock by running a command
        String command = "sf research " + player.getName() + " " + research.getKey().getKey();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

    }
}
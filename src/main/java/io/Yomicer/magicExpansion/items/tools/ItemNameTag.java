package io.Yomicer.magicExpansion.items.tools;

import io.Yomicer.magicExpansion.MagicExpansion; // ← replace with your main class
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.Yomicer.magicExpansion.utils.playerMessage.MainHandMessage;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemNameTag extends SimpleSlimefunItem<ItemUseHandler> implements Listener {

    private final Map<UUID, Long> renameStartTime = new HashMap<>(); // Store System.currentTimeMillis()

    public ItemNameTag(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        Bukkit.getPluginManager().registerEvents(this, MagicExpansion.getInstance());
    }

    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return e -> {
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);
            Player player = e.getPlayer();

            if (e.getHand() != EquipmentSlot.HAND) {
                MainHandMessage.sendMainHandMessage(player);
                return;
            }

            Action action = e.getInteractEvent().getAction();
            if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
                return;
            }

            if (player.isSneaking()) {
                player.sendMessage(ColorGradient.getGradientNameVer2("Type the new name (you can use & color codes or a gradient prefix, within 60 seconds):"));
                player.sendMessage(ColorGradient.getGradientNameVer2("Gradient prefixes:"));
                player.sendMessage(ColorGradient.getGradientNameVer2("(Use Magic Gradient V1)"));
                player.sendMessage(ColorGradient.getGradientNameVer2("(Use Magic Gradient V2)"));
                player.sendMessage(ColorGradient.getGradientNameVer2("Example: (Use Magic Gradient V2)The new item name"));
                UUID uuid = player.getUniqueId();
                renameStartTime.put(uuid, System.currentTimeMillis()); // Save the start time

                // Clear this automatically after 60 seconds (optional, helps avoid memory leaks)
                Bukkit.getScheduler().runTaskLater(MagicExpansion.getInstance(), () -> {
                    renameStartTime.remove(uuid);
                }, 20 * 60);
            } else {
                // === Normal right-click: copy the main-hand display name to the offhand ===
                ItemStack nameTag = player.getInventory().getItemInMainHand();

                // Check whether there's an item in the offhand
                ItemStack offHand = player.getInventory().getItemInOffHand();
                if (offHand == null || offHand.getType() == Material.AIR) {
                    player.sendMessage(ColorGradient.getGradientNameVer2("There's no item in your offhand!"));
                    return;
                }

                // Get the name tag's display name from the main hand
                String displayName = null;
                if (nameTag.hasItemMeta() && nameTag.getItemMeta().hasDisplayName()) {
                    displayName = nameTag.getItemMeta().getDisplayName();
                }
                if (displayName == null || displayName.trim().isEmpty()) {
                    player.sendMessage(ColorGradient.getGradientNameVer2("The name tag has no name set, so it can't be copied!"));
                    return;
                }

                // Use up one name tag
                if (nameTag.getAmount() > 1) {
                    nameTag.setAmount(nameTag.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }

                // Copy the display name to the offhand (keep its NBT, only change the name)
                ItemMeta offMeta = offHand.getItemMeta();
                if (offMeta != null) {
                    offMeta.setDisplayName(displayName); // Copy it directly, including § color codes
                    offHand.setItemMeta(offMeta);
                    player.sendMessage(ColorGradient.getGradientNameVer2("Copied the name to the offhand item!"));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                }
            }
        };
    }

    // ===== Chat-based renaming, only used with Shift + right-click =====
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        Long startTime = renameStartTime.get(uuid);
        if (startTime == null) return; // Not waiting for input

        // ⏱️ Check whether it timed out (60 seconds = 60,000 ms)
        if (System.currentTimeMillis() - startTime > 60_000L) {
            renameStartTime.remove(uuid);
            player.sendMessage(ColorGradient.getGradientNameVer2("The rename request timed out!"));
            e.setCancelled(true);
            return;
        }

        // ✅ Not timed out, keep going
        renameStartTime.remove(uuid); // Clear it after processing
        e.setCancelled(true);

        String input = e.getMessage().trim();
        if (input.isEmpty()) {
            player.sendMessage(ColorGradient.getGradientNameVer2("The name can't be empty!"));
            return;
        }

        String finalName;

        // ===== Check for a gradient prefix =====
        if (input.startsWith("(Use Magic Gradient V2)")) {
            String text = input.substring("(Use Magic Gradient V2)".length());
            if (text.isEmpty()) {
                player.sendMessage(ColorGradient.getGradientNameVer2("The gradient text can't be empty!"));
                return;
            }
            finalName = ColorGradient.getGradientNameVer2(text);
        } else if (input.startsWith("(Use Magic Gradient V1)")) {
            String text = input.substring("(Use Magic Gradient V1)".length());
            if (text.isEmpty()) {
                player.sendMessage(ColorGradient.getGradientNameVer2("The gradient text can't be empty!"));
                return;
            }
            finalName = ColorGradient.getGradientName(text); // Make sure this method exists
        } else {
            finalName = ChatColor.translateAlternateColorCodes('&', input);
        }

        // Apply the name
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.getType() != Material.AIR) {
            ItemMeta meta = mainHand.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(finalName);
                mainHand.setItemMeta(meta);
                player.sendMessage(ColorGradient.getGradientNameVer2("Item renamed!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
        }
    }
}
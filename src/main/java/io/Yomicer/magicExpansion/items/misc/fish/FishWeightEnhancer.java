package io.Yomicer.magicExpansion.items.misc;

import io.Yomicer.magicExpansion.items.misc.fish.Fish;
import io.Yomicer.magicExpansion.items.misc.fish.FishKeys;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FishWeightEnhancer extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    // The weight increase is set to 0.01% ~ 0.5%
    private static final double MIN_PERCENT = 0.0001; // 0.01%
    private static final double MAX_PERCENT = 0.005;  // 0.5%

    public FishWeightEnhancer(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
    }

    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return e -> {
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);

            Player player = e.getPlayer();

            // Only react to the main hand
            if (e.getHand() != EquipmentSlot.HAND) {
                return;
            }

            ItemStack enhancerItem = e.getItem(); // The weight booster bait in the main hand
            ItemStack fishItem = player.getInventory().getItemInOffHand(); // The fish in the off hand

            if (enhancerItem.getAmount() < 1) return;

            if (fishItem == null || fishItem.getType().isAir()) {
                player.sendMessage(ChatColor.RED + "Put the magic fish you want to fatten up in your off hand!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
                return;
            }

            ItemMeta fishMeta = fishItem.getItemMeta();
            if (fishMeta == null) {
                player.sendMessage(ChatColor.RED + "The data of your off-hand item looks wrong!");
                return;
            }

            PersistentDataContainer pdc = fishMeta.getPersistentDataContainer();
            String fishType = pdc.get(FishKeys.FISH_TYPE, PersistentDataType.STRING);
            Double currentWeight = pdc.get(FishKeys.FISH_WEIGHT, PersistentDataType.DOUBLE);

            // Check that it's a magic fish
            if (fishType == null || currentWeight == null) {
                player.sendMessage(ChatColor.RED + "The item in your off hand isn't a valid magic fish!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
                return;
            }

            Fish fish = Fish.fromString(fishType);
            if (fish == null) {
                player.sendMessage(ChatColor.RED + "Can't recognize this fish type!");
                return;
            }

            double maxWeight = fish.getMaxWeight();
            double minWeight = fish.getMinWeight();

            // Check if it has already reached the max weight
            if (currentWeight >= maxWeight) {
                player.sendMessage(ChatColor.YELLOW + "This fish has already reached its max weight of " + String.format("%.3f", maxWeight) + " kg!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Work out the increase: weight range * random percent (0.01%~0.5%)
            double range = maxWeight - minWeight;
            double randomPercent = ThreadLocalRandom.current().nextDouble(MIN_PERCENT, MAX_PERCENT);
            double increaseAmount = range * randomPercent;

            // Don't go over the max weight
            double newWeight = Math.min(currentWeight + increaseAmount, maxWeight);
            double actualIncrease = newWeight - currentWeight;

            // Update the weight in PDC
            pdc.set(FishKeys.FISH_WEIGHT, PersistentDataType.DOUBLE, newWeight);

            // Update the rarity
            Fish.WeightRarity weightRarity = fish.getWeightRarity(newWeight);
            pdc.set(FishKeys.FISH_WEIGHT_RARITY, PersistentDataType.STRING, weightRarity.name());

            // Update the rarity mark in the display name
            String weightRareThis = "";
            if (weightRarity == Fish.WeightRarity.RARE_FISH) {
                weightRareThis = "§e§l⭐";
            } else if (weightRarity == Fish.WeightRarity.SUPER_RARE_FISH) {
                weightRareThis = "§b§l💎";
            } else if (weightRarity == Fish.WeightRarity.MAX_WEIGHT_FISH) {
                weightRareThis = "§c§l🎶";
            }

            fishMeta.setDisplayName(fish.getDisplayName() + " " + weightRareThis);

            // Update the lore
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§dFish Rarity: §r§f" + fish.getRarity().getDisplayName());
            lore.add("§aWeight: §r§f" + String.format("%.3f", newWeight) + " kg");
            lore.add("§eWeight Rarity: §r" + weightRarity.getDisplayName() + " " + weightRareThis);
            if (fish.getLoreLines() != null && fish.getLoreLines().length > 0) {
                lore.add("");
                lore.addAll(Arrays.asList(fish.getLoreLines()));
            }
            fishMeta.setLore(lore);
            fishItem.setItemMeta(fishMeta);

            // Use up the item in the main hand
            if (enhancerItem.getAmount() > 1) {
                enhancerItem.setAmount(enhancerItem.getAmount() - 1);
                player.getInventory().setItemInMainHand(enhancerItem);
            } else {
                player.getInventory().setItemInMainHand(null);
            }

            // Update the fish item in the off hand
            player.getInventory().setItemInOffHand(fishItem);

            // Feedback and sound
            player.sendMessage(ChatColor.GREEN + "✓ Weight boost worked! The fish gained " + ChatColor.GOLD + String.format("%.3f", actualIncrease) + " kg" + ChatColor.GREEN + "!");
            player.sendMessage(ChatColor.GRAY + "Current weight: " + ChatColor.AQUA + String.format("%.3f", newWeight) + " kg" +
                    ChatColor.GRAY + " / " + ChatColor.RED + String.format("%.3f", maxWeight) + " kg");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        };
    }
}
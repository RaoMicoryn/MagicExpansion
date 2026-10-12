package io.Yomicer.magicExpansion.items.preBuildings;

import io.Yomicer.magicExpansion.utils.preBuildingUtils.PreBuildingsTreeUtils;
import io.Yomicer.magicExpansion.utils.ItemPermissionUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.Yomicer.magicExpansion.utils.ColorGradient.*;
import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientNameVer2;
import static org.bukkit.inventory.EquipmentSlot.HAND;

public class PreBuildingTree extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {


    private final String buildingName;
    private final String originName;
    private final String replaceName;

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private final long COOLDOWN_MS;

    public PreBuildingTree(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe,String buildingName,long timeCd) {
        super(category, item, recipeType, recipe);
        this.buildingName = buildingName;
        this.originName = null;
        this.replaceName = null;
        this.COOLDOWN_MS = timeCd*1000;

    }

    public PreBuildingTree(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe,String buildingName, String originName,String replaceName,long timeCd) {
        super(category, item, recipeType, recipe);
        this.buildingName = buildingName;
        this.originName = originName;
        this.replaceName = replaceName;
        this.COOLDOWN_MS = timeCd*1000;

    }


    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return e -> {
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);
            Player player = e.getPlayer();
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            // Check if the player is holding an item
            if (e.getHand()!= HAND) {
                player.sendMessage(getGradientName("Please use this item with your main hand!"));
                return;
            }
            // Check if Shift is being held
            if (player.isSneaking()) {

                String fileName = buildingName;
                // Get the building size
                int[] dims = PreBuildingsTreeUtils.getBuildingDimensions(fileName);
                if (dims == null) {
                    player.sendMessage(getGradientNameVer2("Error: Could not read the building file. Check resources/buildings/" + fileName + ".json"));
                    return;
                }

                int x = dims[0];
                int y = dims[1];
                int z = dims[2];

                // Send info to the player
                player.sendMessage(getGradientNameVer2("========================="));
                player.sendMessage(getGradientNameVer2("Building Preview:"));
                player.sendMessage(getGradientNameVer2("File: " + fileName + ".json"));
                player.sendMessage(getGradientNameVer2("Size: X: " + x + " | Y: " + y + " | Z: " + z));
                player.sendMessage(getGradientNameVer2("Footprint: " + (x * z) + " blocks"));
                player.sendMessage(getGradientNameVer2("Volume: " + (x * y * z) + " blocks"));
                player.sendMessage(getGradientNameVer2("========================="));

                return; // Stop here after Shift + right-click. Do not place the building.
            }

            if(!ItemPermissionUtils.hasPermissionRe(player)){
                return;
            }
            if(player.isOp()){
                // Fix (O): These private messages used to be sent to everyone. Now only this player can see them.
                player.sendMessage(getRandomGradientName("✨ You are the star of this server. No cooldown can dim your light! (This message is for "+player.getName()+" only. Other players should not see it.)"));
                player.sendMessage(getRandomGradientName("✨ Your word is the law. Cooldowns are just chains for ordinary players!(This message is for "+player.getName()+" only. Other players should not see it.)"));
                player.sendMessage(getRandomGradientName("✨ You stand at the top of the server, watching every block move!(This message is for "+player.getName()+" only. Other players should not see it.)"));
                player.sendMessage(getRandomGradientName("⚡ No need to wait. Every step you take is as smooth as teleporting!(This message is for "+player.getName()+" only. Other players should not see it.)"));
                player.sendMessage(getRandomGradientName("⚡ Cooldowns? That’s an ordinary player’s problem. You have god-tier permissions!(This message is for "+player.getName()+" only. Other players should not see it.)"));
                player.sendMessage(getRandomGradientName("⚡ You keep this server in order. No cooldowns for you!(This message is for "+player.getName()+" only. Other players should not see it.)"));
                player.sendMessage(getRandomGradientName("✨ You control the pulse of every dimension. Cooldowns are for Endermen!(This message is for "+player.getName()+" only. Other players should not see it.)"));
                player.sendMessage(getRandomGradientName("✨ Your commands keep the world running. Cooldowns are a joke for survival players!(This message is for "+player.getName()+" only. Other players should not see it.)"));
                player.sendMessage(getRandomGradientName("✨ No cooldowns needed. Every move you make is legendary!(This message is for "+player.getName()+" only. Other players should not see it.)"));
                player.sendMessage(getRandomGradientName("⚡ You sit on the server’s throne, watching everyone else struggle!(This message is for "+player.getName()+" only. Other players should not see it.)"));
                player.sendMessage(getRandomGradientName("⚡ Your powers are beyond time. Cooldowns are just an illusion for mortals!(This message is for "+player.getName()+" only. Other players should not see it.)"));
                player.sendMessage(getRandomGradientName("⚡ This server shines because of you. Cooldowns are just for newbies!(This message is for "+player.getName()+" only. Other players should not see it.)"));
                player.sendMessage(getRandomGradientName("✨ You fill the world with magic. No cooldowns, just glory!(This message is for "+player.getName()+" only. Other players should not see it.)"));
                player.sendMessage(getRandomGradientName("✨ Your commands work right away. Cooldowns are for regular players!(This message is for "+player.getName()+" only. Other players should not see it.)"));
                player.sendMessage(getRandomGradientName("✨ You’re this server’s guardian. Even time stops for you!(This message is for "+player.getName()+" only. Other players should not see it.)"));
                player.sendMessage(getRandomGradientName("[ALL-POWERFUL] Admin "+player.getName()+" has enabled no-cooldown mode. Time bows to you!"));
                player.sendMessage(getRandomGradientName("[SERVER OWNER] "+player.getName()+"’s commands shake the world. No waiting needed!"));
                player.sendMessage(getRandomGradientName("[MORTAL WORLD] "+player.getName()+"’s will is the law of this server!"));
                if(!PreBuildingsTreeUtils.pasteMap(player,buildingName, originName, replaceName)){
                    // Fix (O): Only the admin can see these failure messages too.
                    player.sendMessage(getRandomGradientName("[RULES] Admin "+player.getName()+" seems a bit silly and didn’t read the instructions carefully."));
                    player.sendMessage(getRandomGradientName("[RULES] Admin "+player.getName()+" seems to be affected by some weird rules when placing the item."));
                    return;
                }
                // Fix (O): Keep one simple server-wide success message and remove the duplicate.
                Bukkit.broadcastMessage(getRandomGradientName("[SERVER ANNOUNCEMENT] Admin "+player.getName()+" used the item: "+ ItemStackHelper.getDisplayName(itemInHand)));
                return;
            }

            UUID playerId = player.getUniqueId();
            long now = System.currentTimeMillis();
            // Clear expired cooldowns whenever the item is used
            cooldowns.entrySet().removeIf(entry -> now - entry.getValue() >= COOLDOWN_MS);
            // Check the cooldown
            if (cooldowns.containsKey(playerId)) {
                long lastUsed = cooldowns.get(playerId);
                if (now - lastUsed < COOLDOWN_MS) {
                    long remaining = (COOLDOWN_MS - (now - lastUsed)) / 1000 + 1;
                    player.sendMessage("§cThis item is on cooldown. Please wait " + remaining + " seconds before using it again.");
                    return;
                }
            }


            if(!PreBuildingsTreeUtils.pasteMap(player,buildingName, originName, replaceName)){
                return;
            }
            // ✅ Item used successfully. Update the cooldown.
            cooldowns.put(playerId, now);

            // Reduce the number of items in hand
            if (itemInHand.getAmount() > 1) {
                itemInHand.setAmount(itemInHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null); // If there is only one left, remove it.
            }
        };
    }


}

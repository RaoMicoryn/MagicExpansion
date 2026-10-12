package io.Yomicer.magicExpansion.Listener.magicItemEffectManager;

import io.Yomicer.magicExpansion.utils.GiveItem;
import io.Yomicer.magicExpansion.utils.entity.EntityEgg;
import io.Yomicer.magicExpansion.utils.log.Debug;
import net.guizhanss.guizhanlib.minecraft.helper.entity.EntityHelper;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

import static io.Yomicer.magicExpansion.items.enchantMachine.EnchantingTable.ATTRIBUTE_POOL;

public class ItemEffectKillListener implements Listener {


    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Get the entity that caused the death (the killer)
        Entity killer = event.getEntity().getKiller();

        // Check whether the killer is a player
        if (killer instanceof Player) {
            Player player = (Player) killer;
            ItemStack item = player.getInventory().getItemInMainHand().clone();
            // Get the entity that was killed
            LivingEntity entity = event.getEntity();
            // Get the item PDC
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer() != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();

                // Loop through all attributes
                for (String attribute : ATTRIBUTE_POOL.keySet()) {
                    NamespacedKey key = new NamespacedKey("magicexpansion", attribute.toLowerCase().replace(".", "_"));

                    if (pdc.has(key, PersistentDataType.INTEGER)) {
                        int value = pdc.get(key, PersistentDataType.INTEGER);
                        applyEffect(attribute, value, event.getEntity(), player, item);
                    } else if (pdc.has(key, PersistentDataType.BOOLEAN)) {
                        boolean value = pdc.get(key, PersistentDataType.BOOLEAN);
                        applyEffect(attribute, value, event.getEntity(), player, item);
                    }
                }
            }

        }
    }


    private void applyEffect(String attribute, Object value, Entity target, Player player, ItemStack item) {
        switch (attribute) {
            case "MagicExpansion.EntitySpawn": // Knockback effect
                if (value instanceof Integer EntitySpawn) {

                    EntityType entityType = target.getType();

                    Material spawnEggMaterial = EntityEgg.hasSpawnEgg(entityType);

                    if (spawnEggMaterial != null) {

                        Random random = new Random();
                        double eggRandom = EntitySpawn * 0.05 * 0.01;
                        double randomValue = random.nextDouble(); // Generate a random number in the range [0, 1)

                        if(eggRandom>randomValue) {
                            // Create the spawn egg item
                            ItemStack spawnEgg = new ItemStack(spawnEggMaterial);
                            // Print a message to the console or send it to the player
                            player.sendMessage("§bYou killed §d" + EntityHelper.getName(target));
                            player.sendMessage("§bYour weapon §e[" + item.getItemMeta().getDisplayName() + "§e] §bused §cLife Rebuild §bon §d"+ EntityHelper.getName(target) +"");
                            player.sendMessage("§bYou got a §e" + EntityHelper.getName(target) + " spawn egg");

                            GiveItem.giveOrDropItem(player, spawnEgg);
                        }
                    }
                }
                break;


        }


    }


}
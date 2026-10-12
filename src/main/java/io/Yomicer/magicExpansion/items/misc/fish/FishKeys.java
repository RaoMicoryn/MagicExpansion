package io.Yomicer.magicExpansion.items.misc.fish;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.core.MagicExpansionItems;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

import static io.Yomicer.magicExpansion.items.misc.fish.Fish.*;

public class FishKeys {

    public static final NamespacedKey FISH_TYPE = new NamespacedKey(JavaPlugin.getPlugin(MagicExpansion.class), "fish_type");
    public static final NamespacedKey FISH_WEIGHT = new NamespacedKey(JavaPlugin.getPlugin(MagicExpansion.class), "fish_weight");
    // Optional: stores the weight rarity (as a string)
    public static final NamespacedKey FISH_WEIGHT_RARITY = new NamespacedKey(JavaPlugin.getPlugin(MagicExpansion.class), "fish_weight_rarity");


    // ✅ Definition: item -> its rarity (kept in one place, easy to extend)
    private static final Map<ItemStack, Fish.Rarity> RARITY_ITEM_MAP = new HashMap<>() {{
        put(MagicExpansionItems.RANDOM_FISH_COMMON,           Fish.Rarity.COMMON);
        put(MagicExpansionItems.RANDOM_FISH_UNCOMMON,         Fish.Rarity.UNCOMMON);
        put(MagicExpansionItems.RANDOM_FISH_RARE,             Fish.Rarity.RARE);
        put(MagicExpansionItems.RANDOM_FISH_RARE_POOL_DUST,   Fish.Rarity.RARE_POOL_DUST);
        put(MagicExpansionItems.RANDOM_FISH_RARE_POOL_ORE,    Fish.Rarity.RARE_POOL_ORE);
        put(MagicExpansionItems.RANDOM_FISH_RARE_POOL_INDUSTRY,    Fish.Rarity.RARE_POOL_INDUSTRY);
        put(MagicExpansionItems.RANDOM_FISH_EPIC_POOL_INDUSTRY,    Fish.Rarity.EPIC_POOL_INDUSTRY);
        put(MagicExpansionItems.RANDOM_FISH_EPIC_POOL_ALLOY_INGOT,    Fish.Rarity.EPIC_POOL_ALLOY_INGOT);
        put(MagicExpansionItems.RANDOM_FISH_EPIC,             Fish.Rarity.EPIC);
        put(MagicExpansionItems.RANDOM_FISH_LEGENDARY,        Fish.Rarity.LEGENDARY);
        put(MagicExpansionItems.FISH_LEGENDARY_EEL_POWER,        Fish.Rarity.LEGENDARY_EEL);
        // 👉 Want to add a new rarity? Just add one more put line!
    }};

    private static final Set<ItemStack> MAGIC_FISHING_RODS_NEW = new HashSet<>(Arrays.asList(

            MagicExpansionItems.FISHING_ROD_NEW_PLAYER

    ));
    private static final Set<ItemStack> MAGIC_FISHING_RODS_ADVANCED = new HashSet<>(Arrays.asList(

            MagicExpansionItems.FISHING_ROD_WIND_SPEAKER

    ));
    private static final Set<ItemStack> MAGIC_FISHING_RODS_ULTRA = new HashSet<>(Arrays.asList(

            MagicExpansionItems.FISHING_ROD_FINAL_STICK

    ));
    // Fix (S): Random is now a shared static instance, so we don't create a new Random every time a catch is settled
    private static final Random RANDOM = new Random();

    public static ItemStack enchantDropWithFishData(Player player, ItemStack drop, ItemStack rod) {
        Fish.Rarity targetRarity = null;


        for (Map.Entry<ItemStack, Fish.Rarity> entry : RARITY_ITEM_MAP.entrySet()) {
            if (SlimefunUtils.isItemSimilar(drop, entry.getKey(), true)) {
                targetRarity = entry.getValue();
                break; // Found it, stop looking
            }
        }

        // If it's not one of the target items, return it as it is
        if (targetRarity == null) {
            return drop;
        }

        List<Fish> candidates = getPossibleFishesForRarity(targetRarity); // Make sure the class name is correct
        if (candidates.isEmpty()) {
            // Fallback: return COD
            candidates = Collections.singletonList(Fish.XueFish);
        }

        // Fix (S): use the shared static Random
        Fish chosenFish = candidates.get(RANDOM.nextInt(candidates.size()));

        double weight = 0.0;
        if (isMagicFishingRod(rod, MAGIC_FISHING_RODS_NEW)) {
            weight = chosenFish.rollWeightNew();
        }else if (isMagicFishingRod(rod, MAGIC_FISHING_RODS_ADVANCED)) {
            weight = chosenFish.rollWeightAdvanced();
        }else if (isMagicFishingRod(rod, MAGIC_FISHING_RODS_ULTRA)) {
            weight = chosenFish.rollWeightUltra();
        }

//        double weight = chosenFish.rollWeight();

        ItemMeta meta = drop.getItemMeta();
        if (meta == null) {
            return drop; // Safe return
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        // --- Set the new PDC data ---
        pdc.set(FishKeys.FISH_TYPE, PersistentDataType.STRING, chosenFish.name());
        pdc.set(FishKeys.FISH_WEIGHT, PersistentDataType.DOUBLE, weight);
        Fish.WeightRarity weightRarity = chosenFish.getWeightRarity(weight);
        pdc.set(FishKeys.FISH_WEIGHT_RARITY, PersistentDataType.STRING, weightRarity.name());
        String weightRareThis = "";
        // Fix (S): the server-wide broadcast for rare fish no longer shows exact XYZ coordinates. It now shows the world name + approximate block coordinates (so people can't map out coordinates to find other players' fishing spots)
        String roughLocation = player.getWorld().getName() + " (" + player.getLocation().getBlockX()
                + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ() + ")";
        if (weightRarity == Fish.WeightRarity.RARE_FISH) {
            Bukkit.broadcastMessage("§aCongrats to player §e"+player.getName()+" §afor unexpectedly catching a §eRare Fish §ain §d"+ player.getWorld().getName() + "§a!");
            Bukkit.broadcastMessage("§aCongrats to player §e"+player.getName()+
                    " §afor catching a §eRare Fish "+chosenFish.getDisplayName() +
                    " §anear §d" + roughLocation);
            weightRareThis = "§e§l⭐";
        }
        if (weightRarity == Fish.WeightRarity.SUPER_RARE_FISH) {
            Bukkit.broadcastMessage("§aCongrats to player §e"+player.getName()+" §afor unexpectedly catching a §bSuper Rare Fish §ain §d"+ player.getWorld().getName() + "§a!");
            Bukkit.broadcastMessage("§aCongrats to player §e"+player.getName()+
                    " §afor catching a §bSuper Rare Fish "+chosenFish.getDisplayName() + " §anear §d" + roughLocation);
            weightRareThis = "§b§l\uD83D\uDC8E";
        }
        if (weightRarity == WeightRarity.MAX_WEIGHT_FISH) {
            Bukkit.broadcastMessage("§aCongrats to player §e"+player.getName()+" §afor unexpectedly catching a §b§lFish King §ain §d"+ player.getWorld().getName() + "§a!");
            Bukkit.broadcastMessage("§aCongrats to player §e"+player.getName()+
                    " §afor catching a §c§lFish King "+chosenFish.getDisplayName() + " §anear §d" + roughLocation);
            weightRareThis = "§c§l🎶";
        }

        // --- Change the display name ---
        meta.setDisplayName(chosenFish.getDisplayName() + " " +weightRareThis);

        // --- Change the lore ---
        List<String> lore = new ArrayList<>();
        lore.add(""); // Empty line to separate
        lore.add(("§dFish Rarity: ")+ "§r§f" + chosenFish.getRarity().getDisplayName());
        lore.add(("§aWeight: ")+ "§r§f" + String.format("%.3f", weight) + " kg");
        lore.add(("§eWeight Rarity: ")+ "§r" + weightRarity.getDisplayName() +" "+ weightRareThis);
        if (chosenFish.getLoreLines() != null && chosenFish.getLoreLines().length > 0) {
            lore.add(""); // Empty line to separate
            lore.addAll(Arrays.asList(chosenFish.getLoreLines()));
        }

        meta.setLore(lore);

        drop.setItemMeta(meta);

        return drop;
    }

    private static boolean isMagicFishingRod(ItemStack item, Set<ItemStack> s) {
        if (item == null || item.getType().isAir()) return false;

        return s.stream()
                .anyMatch(rod -> SlimefunUtils.isItemSimilar(item, rod, true));
    }

    public static List<Fish> getPossibleFishesForRarity(Fish.Rarity rarity) {
        switch (rarity) {
            case COMMON:
                return Arrays.asList(Fish.SanWenFish, Fish.XueFish);
            case UNCOMMON:
                return Arrays.asList(Fish.HeTun);
            case RARE:
                return Arrays.asList(Fish.ReDaiFish,Fish.CopperDustFish,Fish.AluminumDustFish,
                Fish.GoldDustFish, Fish.IronDustFish, Fish.LeadDustFish, Fish.TinDustFish,
                        Fish.MagnesiumDustFish, Fish.SilverDustFish, Fish.ZincDustFish);
            case RARE_POOL_ORE:
                // Rare ore fish pool: includes all rare fish that can give ore resources
                return Arrays.asList(Fish.CoalFish,
                        Fish.EmeraldFish, Fish.LapisFish,
                        Fish.DiamondFish, Fish.QuartzFish,
                        Fish.AmethystFish, Fish.IronFish,
                        Fish.GoldFish, Fish.CopperFish,
                        Fish.NetheriteFish,Fish.GlowStoneDustFish,
                        Fish.RedstoneFish);
            case RARE_POOL_DUST:
                // Rare dust fish pool: includes all rare fish that can give dust resources
                return Arrays.asList(Fish.ReDaiFish,
                        Fish.CopperDustFish, Fish.AluminumDustFish,
                        Fish.GoldDustFish, Fish.IronDustFish,
                        Fish.LeadDustFish, Fish.TinDustFish,
                        Fish.MagnesiumDustFish, Fish.SilverDustFish,
                        Fish.ZincDustFish);
            case RARE_POOL_INDUSTRY:
                // Rare dust fish pool: includes all rare fish that can give dust resources
                return Arrays.asList(Fish.ShuLingYu,
                        Fish.UraniumFish,Fish.OilRockFish,
                        Fish.SiliconFish,Fish.SulfateFish

                        );
            case EPIC_POOL_INDUSTRY:
                // Rare dust fish pool: includes all rare fish that can give dust resources
                return Arrays.asList(Fish.FoamCrystalFish,
                        Fish.BlackDiamondFish,Fish.EnchantedBottleFish

                        );
            case EPIC_POOL_ALLOY_INGOT:
                return Arrays.asList(ReinforcedAlloyFish,
                        HardenedMetalFish, DamascusSoulFish,SteelSoulFish,
                        BronzeAncientFish, HardlightAluFish, SilverCopperFish,
                        BrassResonanceFish, AluminumBrassFish, AluminumBronzeFish,
                        CorinthianBronzeFish, SolderFlowFish, NickelSpiritFish,
                        CobaltFlameFish, SiliconIronFish, CarbonSoulFish,
                        GildedIronFish, RedstoneAlloyFish, NeptuniumShadowFish,
                        PlutoniumCoreFish
                );
            case EPIC:
                return Arrays.asList(Fish.MYSTIC_EEL);
            case LEGENDARY:
                return Arrays.asList(Fish.LegendaryLuFish);
            case LEGENDARY_EEL:
                return Arrays.asList(LegendaryEelFish);
            default:
                return Collections.singletonList(Fish.SanWenFish);
        }
    }


}
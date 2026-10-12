package io.Yomicer.magicExpansion.Listener.fishingListener;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.core.MagicExpansionItems;
import io.Yomicer.magicExpansion.items.misc.fish.FishAttributeGenerator;
import io.Yomicer.magicExpansion.items.misc.fish.Gen2Fish;
import io.Yomicer.magicExpansion.items.misc.Lure;
import io.Yomicer.magicExpansion.items.misc.WeightedItem;
import io.Yomicer.magicExpansion.items.misc.baitbag.BaitBagMenu;
import io.Yomicer.magicExpansion.items.misc.moreLure.MoreLure;
import io.Yomicer.magicExpansion.items.tools.FishingRodWaterCloud;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.Yomicer.magicExpansion.utils.WaterCloudHookManager;
import io.Yomicer.magicExpansion.utils.WaterCloudRodEffects;
import io.Yomicer.magicExpansion.utils.WaterCloudRodProficiency;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Fishing listener just for the Water Cloud rod series
 * Works separately from PlayerFishingListener; each handles its own rods
 */
public class PlayerFishingWaterCloudListener implements Listener {

    // D(C5): Use one shared random generator instead of making a new one for every catch
    private static final Random RANDOM = new Random();

    public PlayerFishingWaterCloudListener() {
        // Auto-reel callback when fully charged, triggered by the new system manager
        WaterCloudHookManager.setAutoReelHandler(this::onAutoReel);
    }

    /**
     * When fully charged, auto-reel uses the new catch flow (guaranteed catch + 10% chance for a special item)
     */
    private void onAutoReel(Player player, Location hookLocation) {
        ItemStack rod = player.getInventory().getItemInMainHand();
        SlimefunItem sfItem = SlimefunItem.getByItem(rod);
        if (sfItem instanceof FishingRodWaterCloud fishingRod) {
            processCatch(player, fishingRod, hookLocation, true);
        }
    }

    // IDs of Water Cloud rods with a proficiency system (add new rods here as needed)
    private static final Set<String> PROFICIENCY_ROD_IDS = Set.of(
            "FISHING_ROD_BETWEEN_WATER_CLOUD_REED",
            "FISHING_ROD_BETWEEN_WATER_CLOUD_HANJIANG",
            "FISHING_ROD_BETWEEN_WATER_CLOUD_XIYU"
    );

    // Water Cloud bait types (the active bait decides which special event can happen)
    private static final List<MoreLure> SHUIYUNJIAN_LURES = List.of(
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_CUIXIA, "CuiXia"),   // CuiXia
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_WEICHEN, "WeiChen"), // WeiChen
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_RONGHUO, "RongHuo"), // RongHuo
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_YUEJIN, "YueJin"),   // YueJin
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XINGHE, "XingHe"),   // XingHe
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_REED_JIANJIA, "JianJia"), // JianJia(Item)
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_REED_LUXUE, "LuXue"),   // LuXue(Item)
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_REED_WEILU, "WeiLu"),   // WeiLu(Item)
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_REED_BAILU, "BaiLu"),   // BaiLu(Item)
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_REED_LUYA, "LuYa"),     // LuYa(Item)
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_HANJIANG_NINGSHUANG, "NingShuang"), // NingShuang(Item)
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_HANJIANG_LUOXU, "LuoXu"),           // LuoXu(Item)
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_HANJIANG_BINGPO, "BingPo"),         // BingPo(Item)
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_HANJIANG_CHUJI, "ChuJi"),           // ChuJi(Item)
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_HANJIANG_CHUILUN, "ChuiLun"),       // ChuiLun(Item)
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XIYU_FENGSI, "FengSi"),             // FengSi(Item·Item)
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XIYU_YANYU, "YanYu"),               // YanYu(Item·Item)
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XIYU_LIANBAI, "LianBai"),           // LianBai(Item·Item)
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XIYU_XIAOFENG, "XiaoFeng"),         // XiaoFeng(Item·Item)
            new MoreLure(MagicExpansionItems.FISH_LURE_BETWEEN_WATER_CLOUD_XIYU_XIEYING, "XieYing")            // XieYing(Item·Item)
    );

    // Water Cloud catch messages (normal catches)
    private static final List<String> SHUIYUNJIAN_PHRASES = List.of(
            "The wind picks up and your rod shakes. You caught: ",
            "The water was still, then ripples appeared. It was: ",
            "You cast your line and pulled up a shimmer. It was: ",
            "Cloud shadows drift by. You seem to have caught: ",
            "You gently pull the line in. A gift from an old friend: ",
            "Something magical rises from the water: ",
            "A flick of the rod scatters the stars. You got: ",
            "In the quiet night, something drifts your way: ",
            "A flash of light appears beneath the waves: ",
            "As you reel in under the moonlight, you catch: ",
            "The bobber moves like it knows an old story. You caught: ",
            "Where water meets sky, you somehow pulled out: "
    );

    // Water Cloud messages for special catches
    // Style note: make it feel more epic and vivid.
    private static final List<String> SHUIYUNJIAN_SPECIAL_PHRASES = List.of(
            "The deep whispers, and an ancient secret rises: ",
            "The sky changes and the clouds swirl. You caught: ",
            "A cold flash dims the stars. It is: ",
            "An ancient silence breaks. A legendary treasure appears: ",
            "The sea of clouds shakes. Something is waking up: ",
            "Its arrival turns the rivers around. It is called: ",
            "A powerful aura rises from below. It is: "
    );

    // Water Cloud bait-use messages (prefix + bait name)
    // Style note: keep these short so the bait name fits nicely.
    private static final List<String> SHUIYUNJIAN_CONSUME_PHRASES = List.of(
            "Gently into the water: ",
            "Off with the wind, back to the clouds: ",
            "A little bait, a little moonlight: ",
            "Sink down and seek the unknown: ",
            "Drift with the waves and wait for good news: ",
            "The bait sinks. Everything goes quiet, leaving only ",
            "A small splash, then a sigh: "
    );

    // Water Cloud messages for the last bait in a stack (after the bait name)
    // Style note: give it a slightly sad feeling, like the last goodbye.
    private static final List<String> SHUIYUNJIAN_LAST_PHRASES = List.of(
            ", that was the last gift. Our time ends here.",
            ", scattered into the water, never to return.",
            ", now all gone, with only the river breeze left.",
            ", carried away by the wind. Nothing is left.",
            ", the last one, offered to the moonlit river."
    );

    // Bait-saving messages (old version, Magic v2 gradient)
    private static final List<String> LURE_PRESERVE_PHRASES = List.of(
            "✦ Water Cloud magic saved your bait!",
            "✦ A flash of light returns your bait!",
            "✦ Your bait resonates with Water Cloud and stays safe!"
    );

    // Double-catch messages (for both systems, Magic v2 gradient)
    private static final List<String> DOUBLE_CATCH_PHRASES = List.of(
            "✦ Water Cloud blesses you with double loot!",
            "✦ This rod is magical. One catch becomes two!",
            "✦ Double loot! Water Cloud is smiling!"
    );

    // Junk items in the bait pool (materials affected by junk-rate reduction)
    // When adding junk to the bait pool, add vanilla items to JUNK_MATERIALS and Slimefun items to JUNK_SLIMEFUN_IDS
    private static final Set<String> JUNK_MATERIALS = Set.of(
            "COBBLESTONE",  // Cobblestone
            "FLINT",        // Flint
            "GRAVEL",       // Gravel
            "SAND",         // Sand
            "STRING",       // String
            "BONE"          // Bone
    );

    // Slimefun item IDs counted as junk (can be expanded)
    private static final Set<String> JUNK_SLIMEFUN_IDS = Set.of();

    /**
     * Entry point for Water Cloud rod fishing events
     */
    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.FISHING) {
            // Cast: the old system reduces wait time based on rod level (not an enchantment)
            applyHookSpeed(e.getPlayer(), e.getHook());

            // New fishing system: start a session and let the state machine handle bites
            Player player = e.getPlayer();
            ItemStack rod = player.getInventory().getItemInMainHand();
            SlimefunItem sfItem = SlimefunItem.getByItem(rod);
            if (WaterCloudHookManager.isEnabled() && sfItem instanceof FishingRodWaterCloud) {
                FishHook hook = e.getHook();
                hook.setMaxWaitTime(Integer.MAX_VALUE / 2);
                WaterCloudHookManager.startSession(player, hook, rod);
            }
            return;
        }

        // New fishing system: the state machine handles reeling; CAUGHT_FISH is a fallback
        Player player = e.getPlayer();
        ItemStack rod = player.getInventory().getItemInMainHand();
        SlimefunItem sfItem = SlimefunItem.getByItem(rod);
        if (WaterCloudHookManager.isEnabled() && sfItem instanceof FishingRodWaterCloud fishingRod) {
            if (e.getState() == PlayerFishEvent.State.REEL_IN) {
                handleNewSystemReel(e, player, fishingRod);
            } else if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
                // Fallback: this should not happen after the event is handled
                if (e.getCaught() instanceof Item item) {
                    item.remove();
                }
                processCatch(player, fishingRod, e.getHook().getLocation(), false);
            }
            return;
        }

        fishingUtil(e);
    }

    /**
     * Clear the new fishing session when the player leaves
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        WaterCloudHookManager.onQuit(e.getPlayer());
    }

    /**
     * New system reeling: check whether a fish is on the hook
     * Fish hooked → use the new catch flow; no fish → show the missed-catch effect (already handled by the manager)
     */
    private void handleNewSystemReel(PlayerFishEvent e, Player player, FishingRodWaterCloud fishingRod) {
        if (WaterCloudHookManager.onReel(player)) {
            processCatch(player, fishingRod, e.getHook().getLocation(), false);
        }
    }

    /**
     * Main fishing flow for Water Cloud rods
     * No bait → let the normal Minecraft fishing rod work as usual
     * With bait → pick from the loot pool; marked items trigger a special event, others trigger a normal event
     */
    private void fishingUtil(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = e.getPlayer();
        ItemStack rod = player.getInventory().getItemInMainHand();
        SlimefunItem sfItem = SlimefunItem.getByItem(rod);

        // 1. Check whether this is a Water Cloud rod
        if (!(sfItem instanceof FishingRodWaterCloud fishingRod)) return;

        // 2. Choose the bait (check the bag first, consume it later to avoid losing bait)
        Set<String> supportedKeys = fishingRod.getLootTable().keySet();
        String bagKey = BaitBagMenu.peekFromBag(player, supportedKeys);
        Lure activeLure = bagKey != null
                ? SHUIYUNJIAN_LURES.stream().filter(l -> l.getKey().equals(bagKey)).findFirst().orElse(null)
                : getActiveLure(player, fishingRod);

        // 3. No bait → leave the normal catch alone
        if (activeLure == null) {
            return;
        }
        int rodLevel = WaterCloudRodProficiency.getLevel(rod);
        Random random = RANDOM; // D(C5): Use the shared random generator

        // 4. Pick a catch (old system lowers the weight of junk items)
        ItemStack drop = getCaughtDrop(player, fishingRod, activeLure, rodLevel, true);
        if (drop == null) return;

        // 5. Remove the vanilla catch and drop the chosen loot instead
        Entity caught = e.getCaught();
        if (caught instanceof Item item) {
            item.remove();
        }

        // 6. Use bait (save it if the perk triggers; if the bag is gone, try the same bait in inventory)
        boolean preserved = random.nextDouble() < WaterCloudRodEffects.getOldLurePreserveChance(rodLevel);
        if (!preserved) {
            if (bagKey != null) {
                if (!BaitBagMenu.consumeFromBagByKey(player, bagKey)) {
                    consumeLure(player, activeLure);
                } else {
                    // The bait bag also shows a bait-use message
                    String prefix = SHUIYUNJIAN_CONSUME_PHRASES.get(random.nextInt(SHUIYUNJIAN_CONSUME_PHRASES.size()));
                    player.sendMessage(ColorGradient.getRandomGradientName(prefix) + " §r"
                            + ItemStackHelper.getDisplayName(activeLure.getItem()) + ColorGradient.getRandomGradientName(" ！"));
                }
            } else {
                consumeLure(player, activeLure);
            }
        } else {
            player.sendMessage(ColorGradient.getGradientNameVer2(
                    LURE_PRESERVE_PHRASES.get(random.nextInt(LURE_PRESERVE_PHRASES.size()))));
        }

        // 7. Double catch (old system, with a message)
        if (random.nextDouble() < WaterCloudRodEffects.getOldDoubleCatchChance(rodLevel)) {
            drop.setAmount(Math.min(64, drop.getAmount() * 2));
            player.sendMessage(ColorGradient.getGradientNameVer2(
                    DOUBLE_CATCH_PHRASES.get(random.nextInt(DOUBLE_CATCH_PHRASES.size()))));
        }

        // 8. Drop the item at the hook location
        spawnDrop(player, e.getHook().getLocation(), drop);

        // 9. Give the rod proficiency based on catch quality
        grantProficiency(player, fishingRod, activeLure, drop);

        // 10. Check for a special event
        ItemStack specialCatch = getSpecialCatchForLure(activeLure);
        if (specialCatch != null && SlimefunUtils.isItemSimilar(drop, specialCatch, true)) {
            triggerSpecialEvent(player, activeLure, drop);
        } else {
            handleNormalCatch(player, drop);
        }
    }

    /**
     * New catch flow shared by manual, auto, and fallback reeling; supports a junk pool when no bait is used
     * Bait: check the bag first and consume later; no bait uses the junk pool
     * Rewards: fully charged special catch chance (10%, plus 5% at max level) / double catch
     */
    private void processCatch(Player player, FishingRodWaterCloud fishingRod, Location hookLocation, boolean fullCharge) {
        Set<String> supportedKeys = fishingRod.getLootTable().keySet();
        int rodLevel = WaterCloudRodProficiency.getLevel(player.getInventory().getItemInMainHand());
        Random random = RANDOM; // D(C5): Use the shared random generator

        // 1. Choose bait without consuming it yet, so losing the bag will not affect this catch
        String bagKey = BaitBagMenu.peekFromBag(player, supportedKeys);
        Lure activeLure = bagKey != null
                ? SHUIYUNJIAN_LURES.stream().filter(l -> l.getKey().equals(bagKey)).findFirst().orElse(null)
                : getActiveLure(player, fishingRod);

        // 2. Catch: check the fully charged special chance (10%, plus 5% at max level), then use the loot pool or junk pool
        ItemStack drop;
        double rareChance = WaterCloudRodEffects.getNewRareBonus(rodLevel);
        if (activeLure != null && fullCharge && random.nextDouble() < 0.10 + rareChance) {
            drop = getSpecialCatchForLure(activeLure);
        } else {
            drop = activeLure != null
                    ? getCaughtDrop(player, fishingRod, activeLure, rodLevel, false)
                    : getJunkDrop();
        }
        if (drop == null) return;

        // 3. Use the selected bait; if the bag is gone, try the same bait in inventory
        if (activeLure != null) {
            if (bagKey != null) {
                if (!BaitBagMenu.consumeFromBagByKey(player, bagKey)) {
                    consumeLure(player, activeLure);
                } else {
                    String prefix = SHUIYUNJIAN_CONSUME_PHRASES.get(random.nextInt(SHUIYUNJIAN_CONSUME_PHRASES.size()));
                    player.sendMessage(ColorGradient.getRandomGradientName(prefix) + " §r"
                            + ItemStackHelper.getDisplayName(activeLure.getItem()) + ColorGradient.getRandomGradientName(" ！"));
                }
            } else {
                consumeLure(player, activeLure);
            }
        }

        // 4. Double catch (new system, with a message)
        if (activeLure != null && random.nextDouble() < WaterCloudRodEffects.getNewDoubleCatchChance(rodLevel)) {
            drop.setAmount(Math.min(64, drop.getAmount() * 2));
            player.sendMessage(ColorGradient.getGradientNameVer2(
                    DOUBLE_CATCH_PHRASES.get(random.nextInt(DOUBLE_CATCH_PHRASES.size()))));
        }

        // 5. Drop the item at the hook location
        spawnDrop(player, hookLocation, drop);

        // 6. Proficiency system (Reed fishing rod only)
        grantProficiency(player, fishingRod, activeLure, drop);

        // 7. Check for a special event (no bait means a normal event)
        if (activeLure != null) {
            ItemStack specialCatch = getSpecialCatchForLure(activeLure);
            if (specialCatch != null && SlimefunUtils.isItemSimilar(drop, specialCatch, true)) {
                triggerSpecialEvent(player, activeLure, drop);
                return;
            }
        }
        handleNormalCatch(player, drop);
    }

    /**
     * Junk pool for fishing without bait: junk (cobblestone, gravel, etc.) and vanilla catches have equal chances
     */
    private static final List<WeightedItem> JUNK_POOL = List.of(
            new WeightedItem(new ItemStack(Material.COBBLESTONE), 1),
            new WeightedItem(new ItemStack(Material.GRAVEL), 1),
            new WeightedItem(new ItemStack(Material.CLAY_BALL), 1),
            new WeightedItem(new ItemStack(Material.STRING), 1),
            new WeightedItem(new ItemStack(Material.BONE), 1),
            new WeightedItem(new ItemStack(Material.POTION), 1),
            new WeightedItem(new ItemStack(Material.STICK), 1),
            new WeightedItem(new ItemStack(Material.LILY_PAD), 1),
            new WeightedItem(new ItemStack(Material.VINE), 1),
            new WeightedItem(new ItemStack(Material.LEATHER_BOOTS), 1),
            new WeightedItem(new ItemStack(Material.COD), 1),
            new WeightedItem(new ItemStack(Material.SALMON), 1),
            new WeightedItem(new ItemStack(Material.PUFFERFISH), 1),
            new WeightedItem(new ItemStack(Material.TROPICAL_FISH), 1)
    );

    /**
     * Pick a random item from the junk pool, with equal chances
     */
    private ItemStack getJunkDrop() {
        return getRandomItemFromWeightedPool(JUNK_POOL);
    }

    /**
     * Get the active bait (off-hand first, then inventory)
     */
    private Lure getActiveLure(Player player, FishingRodWaterCloud fishingRod) {
        Set<String> supportedKeys = fishingRod.getLootTable().keySet();

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null) {
            Lure lure = SHUIYUNJIAN_LURES.stream()
                    .filter(l -> supportedKeys.contains(l.getKey()))
                    .filter(l -> SlimefunUtils.isItemSimilar(offHand, l.getItem(), true))
                    .findFirst()
                    .orElse(null);
            if (lure != null) return lure;
        }

        return SHUIYUNJIAN_LURES.stream()
                .filter(l -> supportedKeys.contains(l.getKey()))
                .filter(l -> l.hasLure(player))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get this catch (old system lowers junk weights based on proficiency and adapts to loot pool changes)
     */
    private ItemStack getCaughtDrop(Player player, FishingRodWaterCloud fishingRod, Lure activeLure, int rodLevel, boolean reduceJunk) {
        List<WeightedItem> pool = fishingRod.getLootPoolForLure(activeLure);
        if (pool == null) {
            pool = getDefaultLootPool();
        }
        if (reduceJunk) {
            double reduction = WaterCloudRodEffects.getOldJunkReduction(rodLevel);
            if (reduction > 0) {
                List<WeightedItem> adjusted = new ArrayList<>();
                for (WeightedItem w : pool) {
                    if (isJunkItem(w.getItem())) {
                        // Registered junk items have reduced weight (minimum 1)
                        adjusted.add(new WeightedItem(w.getItem(),
                                Math.max(1, (int) Math.round(w.getWeight() * (1 - reduction)))));
                    } else {
                        adjusted.add(w);
                    }
                }
                pool = adjusted;
            }
        }
        return applyGen2Attributes(getRandomItemFromWeightedPool(pool), fishingRod);
    }

    /**
     * Add v2 fish stats: if the catch is a v2 fish seed, create a finished fish based on the rod rarity; 
     * other items are returned as-is. Stats are made by {@link FishAttributeGenerator}.
     */
    private ItemStack applyGen2Attributes(ItemStack item, FishingRodWaterCloud fishingRod) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }
        // Only v2 fish seeds marked in PDC are recognized (normal items and special catches are ignored)
        if (!FishAttributeGenerator.isGen2Fish(item)) {
            return item;
        }
        // Map the rod weightBoost to a stat tier (0–4)
        int boostIndex = Math.max(0, Math.min(4, (int) Math.round(fishingRod.getWeightBoost())));
        // Read the seed type ID
        String typeId = item.getItemMeta().getPersistentDataContainer()
                .get(FishAttributeGenerator.GEN2_TYPE, PersistentDataType.STRING);
        Gen2Fish type = Gen2Fish.byId(typeId);
        if (type == null) {
            return item;
        }
        return FishAttributeGenerator.generate(type, boostIndex);
    }

    /**
     * Identify junk using registered material names and Slimefun IDs, not whether it is vanilla
     * When adding junk to the bait pool, register it in JUNK_MATERIALS or JUNK_SLIMEFUN_IDS
     */
    private boolean isJunkItem(ItemStack item) {
        SlimefunItem sf = SlimefunItem.getByItem(item);
        if (sf != null) {
            return JUNK_SLIMEFUN_IDS.contains(sf.getId());
        }
        return JUNK_MATERIALS.contains(item.getType().name());
    }

    private ItemStack getRandomItemFromWeightedPool(List<WeightedItem> pool) {
        // D(C2): Return null if the pool is empty or total weight is 0, to avoid nextInt(0) errors (callers already handle null)
        if (pool == null || pool.isEmpty()) {
            return null;
        }
        int total = pool.stream().mapToInt(WeightedItem::getWeight).sum();
        if (total <= 0) {
            return null;
        }
        int r = RANDOM.nextInt(total), current = 0; // D(C5): Use the shared random generator
        for (WeightedItem w : pool) if ((current += w.getWeight()) > r) return w.getItem().clone();
        return pool.get(0).getItem().clone();
    }

    private List<WeightedItem> getDefaultLootPool() {
        return List.of(
                new WeightedItem(new ItemStack(Material.COD), 50),
                new WeightedItem(new ItemStack(Material.INK_SAC), 30)
        );
    }

    /**
     * Special catch linked to the current bait (the item that triggers a special event)
     * Fix: return a clone instead of the shared static MagicExpansionItems item —
     * the full-charge and double-catch logic changes drop with setAmount, so changing a shared item could affect other drops.
     */
    private ItemStack getSpecialCatchForLure(Lure lure) {
        ItemStack special = switch (lure.getKey()) {
            case "CuiXia"  -> MagicExpansionItems.REED_TASSEL;  // CuiXia → Reed Tassel(Item, Item)
            case "WeiChen" -> MagicExpansionItems.REED_TASSEL;  // WeiChen → Reed Tassel
            case "RongHuo" -> MagicExpansionItems.REED_TASSEL;  // RongHuo → Reed Tassel
            case "YueJin"  -> MagicExpansionItems.REED_TASSEL;  // YueJin → Reed Tassel
            case "XingHe"  -> MagicExpansionItems.REED_TASSEL;  // XingHe → Reed Tassel
            case "JianJia", "LuXue", "WeiLu", "BaiLu", "LuYa" -> MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_BAILUYU; // Item → White Reed Feather(Item)
            case "NingShuang", "LuoXu", "BingPo", "ChuJi", "ChuiLun" -> MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_HANJIANG_XUEPOZHU; // Item → Snow Spirit Pearl
            case "FengSi", "YanYu", "LianBai", "XiaoFeng", "XieYing" -> MagicExpansionItems.FISH_SPECIAL_ACTION_BETWEEN_WATER_CLOUD_XIYU_YUPIZHEN;      // Item·Item → Rain Needle
            default -> null;
        };
        return special != null ? special.clone() : null;
    }

    /**
     * Drop the item at the hook location (same as before)
     */
    private void spawnDrop(Player player, Location hookLocation, ItemStack drop) {
        if (drop == null) return;
        Item rewardItem = player.getWorld().dropItem(hookLocation, drop);
        rewardItem.setPickupDelay(0);
        Vector direction = player.getLocation().add(0, 2, 0).toVector()
                .subtract(hookLocation.toVector())
                .normalize()
                .multiply(2.5);
        rewardItem.setVelocity(direction);
        rewardItem.setGlowing(true);
    }

    /**
     * Use bait (Water Cloud message)
     */
    private void consumeLure(Player player, Lure lure) {
        PlayerInventory inv = player.getInventory();
        ItemStack requiredItem = lure.getItem();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && SlimefunUtils.isItemSimilar(item, requiredItem, true)) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                    // D(C5): Use the shared random generator
                    String prefix = SHUIYUNJIAN_CONSUME_PHRASES.get(RANDOM.nextInt(SHUIYUNJIAN_CONSUME_PHRASES.size()));
                    player.sendMessage(ColorGradient.getRandomGradientName(prefix) + " §r" + ItemStackHelper.getDisplayName(item) + ColorGradient.getRandomGradientName(" ！"));
                } else {
                    inv.setItem(i, null);
                    // D(C5): Use the shared random generator
                    String suffix = SHUIYUNJIAN_LAST_PHRASES.get(RANDOM.nextInt(SHUIYUNJIAN_LAST_PHRASES.size()));
                    player.sendMessage(ItemStackHelper.getDisplayName(item) + ColorGradient.getRandomGradientName(suffix));
                }
                player.updateInventory();
                break;
            }
        }
    }

    /**
     * Normal catch → normal event (Water Cloud catch message)
     */
    private void handleNormalCatch(Player player, ItemStack drop) {
        // Play the normal experience pickup sound
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        // D(C5): Use the shared random generator
        String message = SHUIYUNJIAN_PHRASES.get(RANDOM.nextInt(SHUIYUNJIAN_PHRASES.size()));
        player.sendMessage(ColorGradient.getRandomGradientName(message) + " §r" + ItemStackHelper.getDisplayName(drop) + ColorGradient.getRandomGradientName(" ！！"));
    }

    /**
     * Special catch → special event (custom message; effects can be added later)
     */
    private void triggerSpecialEvent(Player player, Lure activeLure, ItemStack drop) {
        // Check which bait triggered the event, then call its matching handler
        switch (activeLure.getKey()) {
            case "CuiXia" -> handleCuiXiaSpecialEvent(player, drop);
            case "WeiChen" -> handleWeiChenSpecialEvent(player, drop);
            case "RongHuo" -> handleRongHuoSpecialEvent(player, drop);
            case "YueJin" -> handleYueJinSpecialEvent(player, drop);
            case "XingHe" -> handleXingHeSpecialEvent(player, drop);
            case "JianJia" -> handleJianJiaSpecialEvent(player, drop);
            case "LuXue" -> handleLuXueSpecialEvent(player, drop);
            case "WeiLu" -> handleWeiLuSpecialEvent(player, drop);
            case "BaiLu" -> handleBaiLuSpecialEvent(player, drop);
            case "LuYa"  -> handleLuYaSpecialEvent(player, drop);
            default -> playSpecialEventEffects(player, drop);
        }
    }

    /**
      * Special event handler for this bait (add its unique effects here later)
     */
    private void handleCuiXiaSpecialEvent(Player player, ItemStack drop) {
        playSpecialEventEffects(player, drop);
    }

    /**
      * Special event handler for this bait (add its unique effects here later)
     */
    private void handleWeiChenSpecialEvent(Player player, ItemStack drop) {
        playSpecialEventEffects(player, drop);
    }

    /**
      * Special event handler for this bait (add its unique effects here later)
     */
    private void handleRongHuoSpecialEvent(Player player, ItemStack drop) {
        playSpecialEventEffects(player, drop);
    }

    /**
      * Special event handler for this bait (add its unique effects here later)
     */
    private void handleYueJinSpecialEvent(Player player, ItemStack drop) {
        playSpecialEventEffects(player, drop);
    }

    /**
      * Special event handler for this bait (add its unique effects here later)
     */
    private void handleXingHeSpecialEvent(Player player, ItemStack drop) {
        playSpecialEventEffects(player, drop);
    }

    /**
      * Special event handler for this bait (add its unique effects here later)
     */
    private void handleJianJiaSpecialEvent(Player player, ItemStack drop) {
        playSpecialEventEffects(player, drop);
    }

    /**
      * Special event handler for this bait (add its unique effects here later)
     */
    private void handleLuXueSpecialEvent(Player player, ItemStack drop) {
        playSpecialEventEffects(player, drop);
    }

    /**
      * Special event handler for this bait (add its unique effects here later)
     */
    private void handleWeiLuSpecialEvent(Player player, ItemStack drop) {
        playSpecialEventEffects(player, drop);
    }

    /**
      * Special event handler for this bait (add its unique effects here later)
     */
    private void handleBaiLuSpecialEvent(Player player, ItemStack drop) {
        playSpecialEventEffects(player, drop);
    }

    /**
      * Special event handler for this bait (add its unique effects here later)
     */
    private void handleLuYaSpecialEvent(Player player, ItemStack drop) {
        playSpecialEventEffects(player, drop);
    }

    /**
     * Shared special event effects: portal sound, celebration fireworks, and a custom message
     */
    private void playSpecialEventEffects(Player player, ItemStack drop) {
        // Play the normal experience pickup sound
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.0f);
        // Launch harmless celebration fireworks from the player’s feet
        spawnCelebrationFirework(player);
        // D(C5): Use the shared random generator
        String message = SHUIYUNJIAN_SPECIAL_PHRASES.get(RANDOM.nextInt(SHUIYUNJIAN_SPECIAL_PHRASES.size()));
        player.sendMessage(ColorGradient.getRandomGradientName(message) + " §r" + ItemStackHelper.getDisplayName(drop) + ColorGradient.getRandomGradientName(" ！！"));
    }

    /**
     * Launch decorative fireworks at the player’s feet (no damage, just for celebration)
     */
    private void spawnCelebrationFirework(Player player) {
        Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BURST)
                .withColor(Color.RED, Color.ORANGE, Color.YELLOW)
                .withFade(Color.WHITE)
                .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
    }

    /**
     * Check whether this Water Cloud rod has a proficiency system
     */
    private boolean isProficiencyRod(SlimefunItem sfItem) {
        if (sfItem == null) return false;
        String id = sfItem.getId();
        // SlimefunItem.getId() returns the full ID with a namespace prefix (like MAGIC_EXPANSION_FISHING_ROD_BETWEEN_WATER_CLOUD_REED),
        // so both plain IDs and prefixed IDs work here
        return PROFICIENCY_ROD_IDS.contains(id)
                || PROFICIENCY_ROD_IDS.stream().anyMatch(suffix -> id.endsWith("_" + suffix));
    }

    /**
     * Reduce the vanilla wait time based on proficiency when casting (old hook-speed system; every 2 levels cuts 60 ticks, from 30s to 18s at max level; not an enchantment)
     */
    private void applyHookSpeed(Player player, FishHook hook) {
        ItemStack rod = player.getInventory().getItemInMainHand();
        SlimefunItem sfItem = SlimefunItem.getByItem(rod);
        if (sfItem instanceof FishingRodWaterCloud && isProficiencyRod(sfItem)) {
            int ticks = WaterCloudRodEffects.getOldHookSpeedTicks(WaterCloudRodProficiency.getLevel(rod));
            if (ticks > 0) {
                hook.setMaxWaitTime(Math.max(100, 600 - ticks));
            }
        }
    }

    /**
     * Proficiency system: gain rod XP based on catch quality (special, rare, or normal)
     * Show the progress bar in the action bar for 2 seconds; play a sound and show a message when leveling up
     */
    private void grantProficiency(Player player, FishingRodWaterCloud fishingRod, Lure activeLure, ItemStack drop) {
        ItemStack rod = player.getInventory().getItemInMainHand();
        SlimefunItem sfItem = SlimefunItem.getByItem(rod);
        if (!(sfItem instanceof FishingRodWaterCloud) || !isProficiencyRod(sfItem)) {
            return;
        }

        // No bait means a junk-pool catch, so there is no special item; avoid a null pointer
        ItemStack specialCatch = activeLure != null ? getSpecialCatchForLure(activeLure) : null;

        int xp;
        if (specialCatch != null && SlimefunUtils.isItemSimilar(drop, specialCatch, true)) {
            xp = WaterCloudRodProficiency.XP_SPECIAL;
        } else {
            xp = WaterCloudRodProficiency.XP_COMMON;
        }

        int oldLevel = WaterCloudRodProficiency.getLevel(rod);
        int newLevel = WaterCloudRodProficiency.addProficiency(rod, xp);
        // Update the proficiency and stat bonus lines together to avoid processing the §x gradient line twice
        WaterCloudRodProficiency.updateLoreWithReward(rod);
        player.getInventory().setItemInMainHand(rod);

        // Action bar progress bar, disappears after 2 seconds
        int level = WaterCloudRodProficiency.getLevel(rod);
        int xpNow = WaterCloudRodProficiency.getXp(rod);
        player.sendActionBar("§bProficiency: §f" + WaterCloudRodProficiency.getLevelName(level)
                + " §e" + WaterCloudRodProficiency.getPipeBar(level, xpNow)
                + " " + WaterCloudRodProficiency.getPercent(level, xpNow) + " §a+" + xp);
        Bukkit.getScheduler().runTaskLater(MagicExpansion.getInstance(), () -> {
            if (player.isOnline()) {
                player.sendActionBar("");
            }
        }, 40L);

        // Level-up feedback
        if (newLevel > oldLevel) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            int index = Math.max(0, Math.min(newLevel - 1, WaterCloudRodProficiency.LEVEL_UP_MESSAGES.length - 1));
            player.sendMessage(ColorGradient.getRandomGradientName("✦ " + WaterCloudRodProficiency.LEVEL_UP_MESSAGES[index]));
        }
    }
}
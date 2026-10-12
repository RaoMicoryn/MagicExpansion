package io.Yomicer.magicExpansion.utils;

import io.Yomicer.magicExpansion.MagicExpansion;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Water Cloud fishing rod proficiency system
 * Proficiency is stored on the rod item (NBT), not the player; 8 levels with an exponential leveling curve
 */
public class WaterCloudRodProficiency {

    public static final NamespacedKey LV_KEY = new NamespacedKey(MagicExpansion.getInstance(), "watercloud_lv");
    public static final NamespacedKey XP_KEY = new NamespacedKey(MagicExpansion.getInstance(), "watercloud_xp");

    public static final int MAX_LEVEL = 8;
    /** Proficiency needed to reach the next level (grows exponentially, with larger steps at higher levels) */
    public static final int[] REQUIREMENTS = {300, 600, 1200, 2200, 3200, 5200, 7200};
    /** Level names (classic Chinese style) */
    public static final String[] LEVEL_NAMES = {
            "First Sight of Water and Clouds", "Watching by the Water", "Joy of Knowing Fish", "The Rod Stirs the Wind",
            "Water and Moon in Hand", "Walking on Waves", "Spirit of Land and Water", "A World on One Rod"
    };
    /** Unique level-up message for each level */
    public static final String[] LEVEL_UP_MESSAGES = {
            "The rod awakens, bringing a little more life to the water and clouds.",
            "The rod hums softly, as if the rivers are answering.",
            "With every cast and pull, you begin to understand the joy of fishing.",
            "The rod stirs the wind; casting feels smooth and easy.",
            "With water and moon in hand, the rod grows more in tune with nature.",
            "Walk the waves, and every river becomes an easy path.",
            "The land and water have a spirit, reflected in this rod.",
            "One rod, one world—no angler in Water Cloud can match you."
    };

    /** XP for normal catches */
    public static final int XP_COMMON = 1;
    /** XP for rare catches */
    public static final int XP_RARE = 2;
    /** XP for special catches */
    public static final int XP_SPECIAL = 5;

    /** Number of bars in the action-bar progress bar (50 | characters) */
    public static final int ACTION_BAR_LENGTH = 50;
    /** Number of bars in the rod lore progress bar (33 | characters) */
    public static final int LORE_BAR_LENGTH = 33;

    private WaterCloudRodProficiency() {
    }

    public static int getLevel(ItemStack rod) {
        if (rod == null || !rod.hasItemMeta()) return 1;
        Integer lv = rod.getItemMeta().getPersistentDataContainer().get(LV_KEY, PersistentDataType.INTEGER);
        return lv == null ? 1 : Math.max(1, Math.min(MAX_LEVEL, lv));
    }

    public static int getXp(ItemStack rod) {
        if (rod == null || !rod.hasItemMeta()) return 0;
        Integer xp = rod.getItemMeta().getPersistentDataContainer().get(XP_KEY, PersistentDataType.INTEGER);
        return xp == null ? 0 : Math.max(0, xp);
    }

    /** XP needed for the next level; returns 0 at max level */
    public static int getRequirement(int level) {
        if (level < 1) return REQUIREMENTS[0];
        if (level >= MAX_LEVEL) return 0;
        return REQUIREMENTS[level - 1];
    }

    public static boolean isMaxLevel(int level) {
        return level >= MAX_LEVEL;
    }

    public static String getLevelName(int level) {
        return LEVEL_NAMES[Math.max(1, Math.min(MAX_LEVEL, level)) - 1];
    }

    /** Proficiency percentage (Magic Expansion v2 gradient), e.g. 42.86%; returns 100.00% at max level */
    public static String getPercent(int level, int xp) {
        int need = getRequirement(level);
        if (need <= 0) return ColorGradient.getGradientNameVer2("100.00%");
        return ColorGradient.getGradientNameVer2(String.format("%.2f%%", Math.min(100.0, xp * 100.0 / need)));
    }

    /** A plain | progress bar: earned part uses the Magic Expansion v2 gradient, the rest is gray; max level returns 'Mastered' */
    public static String getPipeBar(int level, int xp) {
        return buildPipeBar(ACTION_BAR_LENGTH, level, xp);
    }

    /** Build a | progress bar of the given length; earned part uses the Magic Expansion v2 gradient, the rest is gray */
    private static String buildPipeBar(int length, int level, int xp) {
        int need = getRequirement(level);
        if (need <= 0) return "Mastered";
        int filled = Math.max(0, Math.min(length, (int) Math.round(xp * (double) length / need)));
        StringBuilder sb = new StringBuilder();
        if (filled >= 2) {
            sb.append(ColorGradient.getGradientNameVer2("|".repeat(filled)));
        } else if (filled == 1) {
            sb.append("§a|");
        }
        for (int i = filled; i < length; i++) sb.append("§7|");
        return sb.toString();
    }

    /** Rod lore progress bar: gradient | bar plus gradient values; max level returns 'Mastered' */
    public static String getProgressBar(int level, int xp) {
        int need = getRequirement(level);
        if (need <= 0) return "Mastered";
        return buildPipeBar(LORE_BAR_LENGTH, level, xp) + " " + ColorGradient.getGradientNameVer2("(" + xp + "/" + need + ")");
    }

    /**
     * Add proficiency to the rod and return its new level (it may level up)
     */
    public static int addProficiency(ItemStack rod, int amount) {
        if (rod == null || amount <= 0) return getLevel(rod);
        ItemMeta meta = rod.getItemMeta();
        int level = meta.getPersistentDataContainer().getOrDefault(LV_KEY, PersistentDataType.INTEGER, 1);
        int xp = meta.getPersistentDataContainer().getOrDefault(XP_KEY, PersistentDataType.INTEGER, 0);
        level = Math.max(1, Math.min(MAX_LEVEL, level));
        xp += amount;
        while (level < MAX_LEVEL && xp >= REQUIREMENTS[level - 1]) {
            xp -= REQUIREMENTS[level - 1];
            level++;
        }
        if (level >= MAX_LEVEL) {
            xp = 0;
        }
        meta.getPersistentDataContainer().set(LV_KEY, PersistentDataType.INTEGER, level);
        meta.getPersistentDataContainer().set(XP_KEY, PersistentDataType.INTEGER, xp);
        rod.setItemMeta(meta);
        return level;
    }

    /** Strip §x and all § formatting codes (gradient text is split by color codes, so direct contains checks fail) */
    private static String stripColorCodes(String line) {
        return line.replace("§x", "").replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    /**
     * Update the proficiency and bonus lines in one read/write pass (called when fishing levels up)
     * Avoid calling getLore/setLore twice in one tick for §x gradient lines (the old gradient worked because it only made one pass)
     */
    public static void updateLoreWithReward(ItemStack rod) {
        if (rod == null) return;
        int level = getLevel(rod);
        boolean newSystem = WaterCloudHookManager.isEnabled();
        ItemMeta meta = rod.getItemMeta();
        List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());

        // Remove old proficiency and bonus lines (strip color codes before matching text)
        lore.removeIf(line -> {
            String plain = stripColorCodes(line);
            return plain.contains("Proficiency")
                    || REWARD_LINE_PREFIXES.stream().anyMatch(plain::contains);
        });

        // Proficiency line (Magic Expansion v2 gradient) + bonus lines
        lore.add("§bProficiency: §f" + getLevelName(level) + " §e" + getProgressBar(level, getXp(rod)));
        List<String> reward = buildRewardLore(level, newSystem);
        if (!reward.isEmpty()) {
            lore.addAll(reward);
        }
        meta.setLore(lore);
        rod.setItemMeta(meta);
    }

    /**
     * Update the proficiency line in the rod lore (keep existing lore)
     */
    public static void updateLore(ItemStack rod) {
        if (rod == null) return;
        ItemMeta meta = rod.getItemMeta();
        List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
        // Remove the old proficiency line (contains check supports changes to the leading color code)
        lore.removeIf(line -> line.contains("Proficiency"));
        lore.add("§bProficiency: §f" + getLevelName(getLevel(rod)) + " §e" + getProgressBar(getLevel(rod), getXp(rod)));
        meta.setLore(lore);
        rod.setItemMeta(meta);
    }

    /** Bonus-line markers (used to find and remove old bonus lines; text matching works despite leading gradient color codes) */
    private static final List<String> REWARD_LINE_PREFIXES = List.of(
            "Current bonuses ·",  // old title line left over from previous versions (no longer generated; remove old entries)
            "Wait time: ", "Light bite time: ", "Charge speed: ", "Double catch: ", "Special catch: ",
            "Bite speed: ", "Junk chance: ", "Save-bait chance: "
    );

    /**
     * Update bonus lines in the rod lore based on the current fishing system and level, with no heading; keep the proficiency line and existing lore
     * Call after switching systems or leveling up; bonus lines update right away
     */
    public static void updateRewardLore(ItemStack rod) {
        if (rod == null) return;
        int level = getLevel(rod);
        boolean newSystem = WaterCloudHookManager.isEnabled();
        ItemMeta meta = rod.getItemMeta();
        List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());

        // Remove old bonus lines (strip color codes before matching text)
        lore.removeIf(line -> {
            String plain = stripColorCodes(line);
            return REWARD_LINE_PREFIXES.stream().anyMatch(plain::contains);
        });

        List<String> reward = buildRewardLore(level, newSystem);
        if (!reward.isEmpty()) {
            lore.addAll(reward);
        }
        meta.setLore(lore);
        rod.setItemMeta(meta);
    }

    /** Build bonus lines for the current system and level (Magic Expansion v2 gradient, no heading; returns an empty list if there are no bonuses) */
    private static List<String> buildRewardLore(int level, boolean newSystem) {
        List<String> lines = new ArrayList<>();
        if (newSystem) {
            double bite = WaterCloudRodEffects.getNewBiteChanceBonus(level);
            double light = WaterCloudRodEffects.getNewLightBiteChanceBonus(level);
            double charge = WaterCloudRodEffects.getNewChargeSpeedBonus(level);
            double dbl = WaterCloudRodEffects.getNewDoubleCatchChance(level);
            double rare = WaterCloudRodEffects.getNewRareBonus(level);
            if (bite > 0) lines.add(ColorGradient.getGradientNameVer2("Wait time: +" + Math.round(bite * 100) + "%"));
            if (light > 0) lines.add(ColorGradient.getGradientNameVer2("Light bite time: +" + Math.round(light * 100) + "%"));
            if (charge > 0) lines.add(ColorGradient.getGradientNameVer2("Charge speed: +" + (int) charge + "/sec"));
            if (dbl > 0) lines.add(ColorGradient.getGradientNameVer2("Double catch: " + Math.round(dbl * 100) + "%"));
            if (rare > 0) lines.add(ColorGradient.getGradientNameVer2("Special catch: +" + Math.round(rare * 100) + "%(max level)"));
        } else {
            int ticks = WaterCloudRodEffects.getOldHookSpeedTicks(level);
            double junk = WaterCloudRodEffects.getOldJunkReduction(level);
            double preserve = WaterCloudRodEffects.getOldLurePreserveChance(level);
            double dbl = WaterCloudRodEffects.getOldDoubleCatchChance(level);
            if (ticks > 0) lines.add(ColorGradient.getGradientNameVer2("Bite speed: +" + Math.round(ticks / 600.0 * 100) + "%"));
            if (junk > 0) lines.add(ColorGradient.getGradientNameVer2("Junk chance: -" + Math.round(junk * 100) + "%"));
            if (preserve > 0) lines.add(ColorGradient.getGradientNameVer2("Save-bait chance: " + Math.round(preserve * 100) + "%"));
            if (dbl > 0) lines.add(ColorGradient.getGradientNameVer2("Double catch: " + Math.round(dbl * 100) + "%"));
        }
        return lines;
    }
}

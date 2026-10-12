package io.Yomicer.magicExpansion.items.misc.fish;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Attribute generator for Gen 2 fish.
 * <p>
 * Job: when a Gen 2 fish is caught, use the fishing rod's rarity boost (weightBoost) to decide:
 * <ul>
 *   <li>The five element values (a better rod keeps the fish closer to its base main element
 *       and gives less randomness, so the fish is "purer");</li>
 *   <li>The quality multiplier (0.7~2.5, a better rod makes the good range bigger).</li>
 * </ul>
 * All attributes are saved in the item's PDC so they can be read and shown later.
 * The five elements are always in this order: Fire, Water, Thunder, Wind, Earth. The sum is always 1.
 */
public class FishAttributeGenerator {

    // PDC keys just for Gen 2 fish (kept separate from the Dreamweaver FishKeys so data doesn't get mixed up)
    public static final NamespacedKey GEN2_TYPE = new NamespacedKey(MagicExpansion.getInstance(), "gen2_type");
    public static final NamespacedKey GEN2_ELEMENTS = new NamespacedKey(MagicExpansion.getInstance(), "gen2_elements");
    public static final NamespacedKey GEN2_QUALITY = new NamespacedKey(MagicExpansion.getInstance(), "gen2_quality");
    // Layer 1 addition: the fish's own gene type (Speed / Synthesis) and parent tracking (parent fish IDs, comma separated)
    public static final NamespacedKey GEN2_TRAIT = new NamespacedKey(MagicExpansion.getInstance(), "gen2_trait");
    public static final NamespacedKey GEN2_PARENTS = new NamespacedKey(MagicExpansion.getInstance(), "gen2_parents");

    /** Rod rarity boost -> element purity (bigger = closer to the main element) and number feel */
    private static final double[] PURITY_EXP = {1.5, 2.0, 2.5, 3.0, 3.6}; // Green Bamboo / Reed Flower / Cold River Snow / Drizzle & Slanting Wind / Taigong

    /**
     * Make a Gen 2 fish with attributes (wild catch):
     * the fish's gene type = the species' default gene type, no parents.
     *
     * @param type       fish species
     * @param boostIndex rod rarity level (0 = lowest, 4 = highest), see the index of {@link #PURITY_EXP}
     * @return the finished fish item with PDC attributes
     */
    public static ItemStack generate(Gen2Fish type, int boostIndex) {
        return generate(type, boostIndex, type.getDefaultTrait(), null, null);
    }

    /**
     * Make a Gen 2 fish with attributes (lets things like breeding override the gene type and parents).
     *
     * @param type       fish species
     * @param boostIndex rod rarity level (0 = lowest, 4 = highest)
     * @param trait      the fish's gene type (Speed / Synthesis). If null, use the species' default gene type
     * @param parent1Id  father fish species ID (e.g. GEN2_LUXUE). Pass null for wild fish
     * @param parent2Id  mother fish species ID (e.g. GEN2_WEILU). Pass null for wild fish
     * @return the finished fish item with PDC attributes
     */
    public static ItemStack generate(Gen2Fish type, int boostIndex, Gen2Fish.Trait trait, String parent1Id, String parent2Id) {
        double purityExp = PURITY_EXP[Math.max(0, Math.min(boostIndex, PURITY_EXP.length - 1))];
        double[] elements = rollElements(type, purityExp);
        double quality = rollQuality(boostIndex);
        return buildItem(type, elements, quality, trait, parent1Id, parent2Id);
    }

    /**
     * Breeding-only entry: makes the baby fish straight from the calculated elements/quality (no random roll).
     * It clamps negative values and normalizes again (sum = 1) so the data is always valid.
     *
     * @param type     baby fish species (looks like the parents)
     * @param elements baby's five elements (Fire, Water, Thunder, Wind, Earth), will be normalized
     * @param quality  baby's quality multiplier (0.7~2.5, clamped if out of range)
     * @param trait    baby's gene type (result of 30% inherit / 70% random), null falls back to the species' default
     * @param parent1Id father fish species ID
     * @param parent2Id mother fish species ID
     */
    public static ItemStack breed(Gen2Fish type, double[] elements, double quality, Gen2Fish.Trait trait, String parent1Id, String parent2Id) {
        double[] safe = new double[5];
        if (elements != null) {
            double sum = 0;
            for (int i = 0; i < 5; i++) {
                safe[i] = Math.max(0.0, elements[i]);
                sum += safe[i];
            }
            if (sum <= 0) {
                safe = type.getBaseElements().clone(); // Fallback: if the parents' data is broken, go back to the species' base values
            } else {
                for (int i = 0; i < 5; i++) {
                    safe[i] = safe[i] / sum;
                }
            }
        } else {
            safe = type.getBaseElements().clone();
        }
        double q = Math.max(0.7, Math.min(2.5, quality));
        return buildItem(type, safe, q, trait, parent1Id, parent2Id);
    }

    /** Build the finished fish item (name / lore / PDC). The caller must make sure the elements and quality are valid */
    private static ItemStack buildItem(Gen2Fish type, double[] elements, double quality,
                                       Gen2Fish.Trait trait, String parent1Id, String parent2Id) {
        Gen2Fish.Trait effectiveTrait = trait != null ? trait : type.getDefaultTrait();

        // Name + rarity
        String displayName = type.getGradientName() + " " + type.getRarity().getColorCode() + "§l✦";

        // Lore: grade / quality / elements / gene type use fixed colors (a prefix color + a level color),
        // while the description and "used as material" lines keep the full §x gradient.
        // Don't pass a displayName that already has color codes into the gradient method (it breaks the § characters).
        List<String> lore = new ArrayList<>();
        lore.add("");
        // Grade: pink prefix (§d), the end changes color by rarity (Common §f white / Rare §e yellow / Epic §d pink),
        // same layout as the "fish rarity" line on Gen 1 fish
        lore.add("§dGrade·" + type.getRarity().getDisplayName());
        // Quality: green prefix (§a), same as the "weight" line on Gen 1 fish
        lore.add("§aQuality: §r§f" + String.format("%.2f", quality));
        // Elements: shown as percentages, each element has its own bright color (Fire red / Water blue / Thunder purple / Wind green / Earth gold).
        // Later, the crossbreeding machine will add up all five as (element value x quality) to get the final value
        // (this is just for display, the real numbers stay in the PDC)
        lore.add("§eElements: §cFire " + pct(elements[0])
                + " §bWater " + pct(elements[1])
                + " §dThunder " + pct(elements[2])
                + " §aWind " + pct(elements[3])
                + " §6Earth " + pct(elements[4]));
        // Gene type: cyan prefix (§b), the end changes color by gene type (Speed §d pink / Synthesis §a green), decides what the fish is used for
        lore.add("§bGene Type: " + effectiveTrait.getColorCode() + effectiveTrait.getDisplayName());
        lore.add("");
        lore.add(ColorGradient.getGradientNameVer2(type.getLore()));
        // Parent tracking: only shown for bred babies, not for wild fish. Shows the species' display name (not the internal ID)
        if (parent1Id != null && parent2Id != null) {
            String n1 = fishNameOf(parent1Id);
            String n2 = fishNameOf(parent2Id);
            lore.add("");
            lore.add("§7Parents: §r§f" + n1 + " §7× §r§f" + n2);
        }
        lore.add("");
        lore.add(ColorGradient.getGradientNameVer2("Used as material for breeding / element enchanting"));

        // Important: build it with Slimefun's CustomItemStack (the same way as the Gen 1 RANDOM_FISH_* fish).
        // If the material is a fish bucket (COD_BUCKET / PUFFERFISH_BUCKET etc.) and you use a plain "new ItemStack + setItemMeta",
        // the lore gets lost when you write it (in testing, getLore was null both before and after).
        // CustomItemStack sets name/lore properly using its own ItemMeta, and then we add the PDC on top so it shows up reliably.
        ItemStack item = new CustomItemStack(type.getMaterial(), displayName, lore.toArray(new String[0]));

        // Write the attributes to PDC (on top of the CustomItemStack)
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(GEN2_TYPE, PersistentDataType.STRING, type.getId());
            pdc.set(GEN2_ELEMENTS, PersistentDataType.STRING, join(elements));
            pdc.set(GEN2_QUALITY, PersistentDataType.DOUBLE, quality);
            // Layer 1 addition: save the gene type; only bred babies save parents (wild fish don't = no parents)
            pdc.set(GEN2_TRAIT, PersistentDataType.STRING, effectiveTrait.name());
            if (parent1Id != null && parent2Id != null) {
                pdc.set(GEN2_PARENTS, PersistentDataType.STRING, parent1Id + "," + parent2Id);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Roll the five element values.
     * Uses the species' base values as the target. The higher the purity, the closer to the base (less randomness).
     * The total is always normalized to 1.
     */
    private static double[] rollElements(Gen2Fish type, double purityExp) {
        double[] base = type.getBaseElements(); // Fire, Water, Thunder, Wind, Earth, sum = 1
        double[] out = new double[5];
        double sum = 0;

        // Add some randomness around the base values, where the amount depends on purity
        for (int i = 0; i < 5; i++) {
            double noise = (ThreadLocalRandom.current().nextDouble() - 0.5) * (1.0 / purityExp) * 0.4;
            double v = Math.max(0.0, base[i] + noise);
            out[i] = v;
            sum += v;
        }

        // Normalize so the total = 1 (the main element keeps the highest share)
        for (int i = 0; i < 5; i++) {
            out[i] = out[i] / sum;
        }
        return out;
    }

    /**
     * Roll the quality multiplier (0.7 ~ 2.5). The higher the rod rarity, the bigger the good range
     * (this affects speed-up efficiency and synthesis purity).
     */
    private static double rollQuality(int boostIndex) {
        // Minimum is 0.7; the max goes up with rod level: level 0 ≈ 1.3, level 4 ≈ 2.5
        double maxBonus = 0.6 + boostIndex * 0.3;
        return Math.min(2.5, 0.7 + ThreadLocalRandom.current().nextDouble() * maxBonus);
    }

    /** Check if the item is a Gen 2 fish */
    public static boolean isGen2Fish(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(GEN2_TYPE, PersistentDataType.STRING);
    }

    /** Read the quality multiplier (shortcut) */
    public static double getQuality(ItemStack item) {
        if (!isGen2Fish(item)) return 0.0;
        Double q = item.getItemMeta().getPersistentDataContainer()
                .get(GEN2_QUALITY, PersistentDataType.DOUBLE);
        return q == null ? 0.0 : q;
    }

    /** Read the fish's gene type. Old fish (no GEN2_TRAIT) or invalid values return null, so the caller should fall back to the species' default gene type */
    public static Gen2Fish.Trait getTrait(ItemStack item) {
        if (!isGen2Fish(item)) return null;
        String t = item.getItemMeta().getPersistentDataContainer()
                .get(GEN2_TRAIT, PersistentDataType.STRING);
        if (t == null) return null;
        try {
            return Gen2Fish.Trait.valueOf(t);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Read the parent tracking ("fatherID,motherID"). Wild fish / old fish return null */
    public static String getParents(ItemStack item) {
        if (!isGen2Fish(item)) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(GEN2_PARENTS, PersistentDataType.STRING);
    }

    /** Read the fish's five element values (Fire, Water, Thunder, Wind, Earth, sum = 1). Returns null if the data is missing or broken */
    public static double[] getElements(ItemStack item) {
        if (!isGen2Fish(item)) return null;
        String raw = item.getItemMeta().getPersistentDataContainer()
                .get(GEN2_ELEMENTS, PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) return null;
        try {
            String[] parts = raw.split(",");
            if (parts.length != 5) return null;
            double[] out = new double[5];
            for (int i = 0; i < 5; i++) {
                out[i] = Double.parseDouble(parts[i]);
                if (Double.isNaN(out[i]) || Double.isInfinite(out[i])) return null;
            }
            return out;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Read the fish's species (handy for looking up the parent fish). Returns null if it's not a Gen 2 fish */
    public static Gen2Fish getType(ItemStack item) {
        if (!isGen2Fish(item)) return null;
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(GEN2_TYPE, PersistentDataType.STRING);
        return Gen2Fish.byId(id);
    }

    /** Turn an array into a comma separated string */
    private static String join(double[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format("%.4f", arr[i]));
        }
        return sb.toString();
    }

    /** Turn an element value from 0~1 into a whole-number percent string, e.g. 0.29 -> "29%" */
    private static String pct(double v) {
        return String.format("%d%%", Math.round(v * 100));
    }

    /** Look up the display name from a fish species ID (GEN2_xxx). Unknown / invalid IDs fall back to the raw ID string */
    private static String fishNameOf(String fishId) {
        if (fishId == null) return "";
        Gen2Fish fish = Gen2Fish.byId(fishId);
        if (fish == null) return fishId; // Fallback: show the raw ID so it's never empty
        return fish.getDisplayName();
    }
}
package io.Yomicer.magicExpansion.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Water Cloud rod level bonuses (separate reward tables for old and new systems; XP and levels are shared)
 * All level-up bonuses are configured here. Add new effects here later.
 * Old system (vanilla checks): faster bites / less junk / save bait / double catch
 * New system (state machine): shorter wait / shorter light bite / faster charge / double catch / max-level bonus
 */
public class WaterCloudRodEffects {

    /** Effect type (add new effects here later) */
    public enum EffectType {
        /** Old: bite speed; value = ticks removed from the vanilla wait limit (60 fewer ticks every 2 levels) */
        OLD_HOOK_SPEED,
        /** Old: junk chance; value = how much to reduce the weight of vanilla junk in the bait pool (3% per level from level 3) */
        OLD_JUNK_REDUCTION,
        /** Old: save bait; chance to avoid using bait (8% per level from level 4) */
        OLD_LURE_PRESERVE,
        /** Old: double catch; chance to get two catches from one bait (4% per level from level 3) */
        OLD_DOUBLE_CATCH,
        /** New: shorter wait; bonus chance per second to go from waiting to light bite (1.5% per level from level 4) */
        NEW_BITE_SPEED,
        /** New: shorter light bite; bonus chance per second to go from light bite to full bite (2% per level from level 5) */
        NEW_LIGHT_BITE_SPEED,
        /** New: faster charge; extra charge points per second (0.5 per level from level 3) */
        NEW_CHARGE_SPEED,
        /** New: double catch; chance to get two catches from one bait (3% per level from level 3) */
        NEW_DOUBLE_CATCH,
        /** New: max-level special bonus; extra chance for a special catch at full charge (+5% at level 8 only) */
        NEW_RARE_BONUS
    }

    /** A single level effect */
    public static final class LevelEffect {

        private final EffectType type;
        private final double value;

        public LevelEffect(EffectType type, double value) {
            this.type = type;
            this.value = value;
        }

        public EffectType getType() {
            return type;
        }

        public double getValue() {
            return value;
        }
    }

    private WaterCloudRodEffects() {
    }

    /**
     * Get the special bonuses for a level (both systems are generated; the active system decides which ones apply)
     * To add an effect, add a LevelEffect by level here; no need to change where effects are applied
     */
    public static List<LevelEffect> getEffects(int level) {
        List<LevelEffect> effects = new ArrayList<>();

        // ===== Old system =====
        // Bite speed: reduce the wait limit by 60 ticks every 2 levels (max level 8: 240 ticks less, from 30s to 18s)
        effects.add(new LevelEffect(EffectType.OLD_HOOK_SPEED, Math.max(0, level / 2) * 60.0));
        // Junk chance: -3% per level from level 3 (Max level -18%)
        effects.add(new LevelEffect(EffectType.OLD_JUNK_REDUCTION, Math.max(0, level - 2) * 0.03));
        // Save bait: +8% per level from level 4 (Max level +40%)
        effects.add(new LevelEffect(EffectType.OLD_LURE_PRESERVE, Math.max(0, level - 3) * 0.08));
        // Double catch: +4% per level from level 3 (Max level +24%)
        effects.add(new LevelEffect(EffectType.OLD_DOUBLE_CATCH, Math.max(0, level - 2) * 0.04));

        // ===== New system =====
        // Shorter wait: +1.5% per level from level 4 (Max level +7.5%)
        effects.add(new LevelEffect(EffectType.NEW_BITE_SPEED, Math.max(0, level - 3) * 0.015));
        // Shorter light bite: +2% per level from level 5 (Max level +8%)
        effects.add(new LevelEffect(EffectType.NEW_LIGHT_BITE_SPEED, Math.max(0, level - 4) * 0.02));
        // Faster charge: +0.5 points/sec per level from level 3 (max level: +3 points/sec)
        effects.add(new LevelEffect(EffectType.NEW_CHARGE_SPEED, Math.max(0, level - 2) * 0.5));
        // Double catch: +3% per level from level 3 (Max level +18%)
        effects.add(new LevelEffect(EffectType.NEW_DOUBLE_CATCH, Math.max(0, level - 2) * 0.03));
        // Max-level special bonus: Level 8 only: +5%
        effects.add(new LevelEffect(EffectType.NEW_RARE_BONUS, level >= 8 ? 0.05 : 0.0));

        return effects;
    }

    // ===== Old system values =====

    /** Old bite speed: ticks removed from the wait limit */
    public static int getOldHookSpeedTicks(int level) {
        return (int) sum(level, EffectType.OLD_HOOK_SPEED);
    }

    /** Old junk chance: weight reduction for vanilla items in the bait pool */
    public static double getOldJunkReduction(int level) {
        return sum(level, EffectType.OLD_JUNK_REDUCTION);
    }

    /** Old save-bait chance */
    public static double getOldLurePreserveChance(int level) {
        return sum(level, EffectType.OLD_LURE_PRESERVE);
    }

    /** Old double-catch chance */
    public static double getOldDoubleCatchChance(int level) {
        return sum(level, EffectType.OLD_DOUBLE_CATCH);
    }

    // ===== New system values =====

    /** New shorter wait: extra chance per second to go from waiting to light bite */
    public static double getNewBiteChanceBonus(int level) {
        return sum(level, EffectType.NEW_BITE_SPEED);
    }

    /** New shorter light bite: extra chance per second to go from light bite to full bite */
    public static double getNewLightBiteChanceBonus(int level) {
        return sum(level, EffectType.NEW_LIGHT_BITE_SPEED);
    }

    /** New charge speed: extra charge points per second */
    public static double getNewChargeSpeedBonus(int level) {
        return sum(level, EffectType.NEW_CHARGE_SPEED);
    }

    /** New double-catch chance */
    public static double getNewDoubleCatchChance(int level) {
        return sum(level, EffectType.NEW_DOUBLE_CATCH);
    }

    /** New max-level special-catch bonus */
    public static double getNewRareBonus(int level) {
        return sum(level, EffectType.NEW_RARE_BONUS);
    }

    private static double sum(int level, EffectType type) {
        return getEffects(level).stream()
                .filter(e -> e.getType() == type)
                .mapToDouble(LevelEffect::getValue)
                .sum();
    }
}

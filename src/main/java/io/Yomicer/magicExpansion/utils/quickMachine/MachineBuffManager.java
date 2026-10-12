package io.Yomicer.magicExpansion.utils;

import org.bukkit.Location;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks machine speed buffs (Layer 2: speed-up system foundation).
 * <p>
 * Uses each machine block's location as the key to track its speed buff and expiration time:
 * <ul>
 *   <li>Base multiplier {@link #BASE_MULTIPLIER} = 1.5, actual multiplier = 1.5 × fish quality multiplier(0.7~2.5)；</li>
 *   <li>Stored in memory; all buffs expire when the server restarts (as planned for v1)；</li>
 *   <li>Expired entries are cleaned up when queried.</li>
 * </ul>
 * Version 1 only stores and checks buff data. Machine efficiency support (such as aquarium systems) will be added later.
 */
public final class MachineBuffManager {

    /** Base speed multiplier (fixed in Layer 2 of the plan) */
    public static final double BASE_MULTIPLIER = 1.5;

    /** Default duration: 5 minutes (in seconds) */
    public static final long DEFAULT_DURATION_SECONDS = 300;

    private record Buff(long expireAtMillis, double multiplier) {
    }

    private static final Map<Location, Buff> BUFFS = new ConcurrentHashMap<>();

    private MachineBuffManager() {
    }

    /** Apply a speed buff to the machine at the given location */
    public static void apply(Location loc, long durationSeconds, double multiplier) {
        if (loc == null) return;
        long expireAt = System.currentTimeMillis() + Math.max(1, durationSeconds) * 1000L;
        BUFFS.put(loc.clone(), new Buff(expireAt, multiplier));
    }

    /** Get the active multiplier; returns 1.0 (no speed boost) if there is no buff or it has expired */
    public static double getMultiplier(Location loc) {
        Buff buff = BUFFS.get(loc);
        if (buff == null) return 1.0;
        if (System.currentTimeMillis() >= buff.expireAtMillis()) {
            BUFFS.remove(loc);
            return 1.0;
        }
        return buff.multiplier();
    }

    /** Check whether a speed buff is active */
    public static boolean hasBuff(Location loc) {
        Buff buff = BUFFS.get(loc);
        if (buff == null) return false;
        if (System.currentTimeMillis() >= buff.expireAtMillis()) {
            BUFFS.remove(loc);
            return false;
        }
        return true;
    }

    /** Seconds remaining (returns 0 if there is no buff or it has expired) */
    public static long getRemainingSeconds(Location loc) {
        Buff buff = BUFFS.get(loc);
        if (buff == null) return 0;
        long left = (buff.expireAtMillis() - System.currentTimeMillis()) / 1000L;
        if (left <= 0) {
            BUFFS.remove(loc);
            return 0;
        }
        return left;
    }

    /** Remove a machine's buff */
    public static void remove(Location loc) {
        BUFFS.remove(loc);
    }
}
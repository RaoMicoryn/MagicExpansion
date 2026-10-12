package io.Yomicer.magicExpansion.utils;

import io.Yomicer.magicExpansion.MagicExpansion;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Water Cloud fishing state-machine session manager
 * - One session per player (register when casting; clean up on reel or disconnect)
 * - Every second: check state changes, refresh action-bar text and charge bar, and handle light-bite timeout
 * - Every tick (20 Hz): aim during a full bite/charge phase, show the marker on the XP bar, and build charge
 * - Every 5 ticks: shake the hook during a full bite (side-to-side on the water surface with splash particles)
 * - Full charge triggers auto-reel; after 60 seconds idle or beyond 20 blocks, the fish escapes
 * - Global toggle: changed with /mxfishmode and saved in config.yml
 */
public final class WaterCloudHookManager {

    /** Hook shake range during a full bite (blocks, ± value) */
    private static final double SHAKE_RADIUS = 0.9;
    /** Maximum session lifetime in seconds (fallback cleanup to prevent leaks) */
    private static final long MAX_SESSION_SECONDS = 600;
    /** AFK timeout during the charge phase (seconds) */
    private static final long FULL_BITE_LIMIT_SECONDS = 30;
    /** Maximum distance between player and bobber during charging (escape if exceeded; follows vanilla fishing distance limit) */
    private static final double MAX_HOOK_DISTANCE = 20.0;
    /** Center aiming range in degrees (charge builds while the marker is within this range) */
    private static final double AIM_THRESHOLD_DEGREES = 15.0;
    /** Charge rate: +1.25 per tick (20 ticks × 1.25 = 25% per second; full charge in 4 seconds) */
    private static final double CHARGE_PER_TICK = 1.25;
    /** Maximum charge */
    private static final double MAX_CHARGE = 100.0;
    /** Action-bar refresh interval for waiting/light-bite states (every 3 seconds) */
    private static final int ACTION_BAR_REFRESH_SECONDS = 3;
    /** Catch chance when reeling at full bite = 10% + charge% × 0.9 */
    private static final double BASE_CATCH_CHANCE = 0.10;
    private static final double CHARGE_CATCH_FACTOR = 0.9 / MAX_CHARGE;

    private static final Map<UUID, HookSession> SESSIONS = new HashMap<>();
    private static BukkitTask tickTask;
    private static BukkitTask aimTask;
    private static BukkitTask shakeTask;

    /** Callback for auto-reeling at full charge (registered by listener) */
    private static AutoReelHandler autoReelHandler;

    public interface AutoReelHandler {

        /** @param hookLocation hook location (used to spawn drops) */
        void onAutoReel(Player player, Location hookLocation);
    }

    private WaterCloudHookManager() {
    }

    // ==================== Global toggle ====================

    /** Whether the new fishing system is enabled (on by default, toggle via command, saved to config.yml) */
    public static boolean isEnabled() {
        return MagicExpansion.getInstance().getConfig().getBoolean("fishing-system.new-system", true);
    }

    public static void setEnabled(boolean enabled) {
        MagicExpansion.getInstance().getConfig().set("fishing-system.new-system", enabled);
        MagicExpansion.getInstance().saveConfig();
    }

    // ==================== Scheduling ====================

    public static void startTicking(MagicExpansion plugin) {
        stopTicking();
        tickTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, WaterCloudHookManager::tick, 20L, 20L);
        aimTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, WaterCloudHookManager::aimTick, 1L, 1L);
        shakeTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, WaterCloudHookManager::shakeTick, 5L, 5L);
    }

    public static void stopTicking() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (aimTask != null) {
            aimTask.cancel();
            aimTask = null;
        }
        if (shakeTask != null) {
            shakeTask.cancel();
            shakeTask = null;
        }
        // On shutdown/unload, restore all XP bars taken over by this system so players' XP data is not saved as level 0
        for (HookSession session : SESSIONS.values()) {
            Player player = session.player;
            if (player != null && player.isOnline()) {
                restoreExpBar(player, session);
            }
        }
        SESSIONS.clear();
    }

    // ==================== Session lifecycle ====================

    /** Cast rod: create a session (replace and clean up any old session) */
    public static void startSession(Player player, FishHook hook, ItemStack rod) {
        UUID id = player.getUniqueId();
        HookSession old = SESSIONS.remove(id);
        if (old != null) {
            // Fix XP loss: restore the XP bar from the old session before replacing it (it may be in the charge phase)
            if (player.isOnline()) {
                restoreExpBar(player, old);
            }
            if (old.hook.isValid()) {
                old.hook.remove();
            }
        }
        SESSIONS.put(id, new HookSession(player, hook, rod));
    }

    @Nullable
    public static HookSession getSession(Player player) {
        return SESSIONS.get(player.getUniqueId());
    }

    /** Disconnect cleanup: restore the XP bar and remove the session */
    public static void onQuit(Player player) {
        HookSession session = SESSIONS.remove(player.getUniqueId());
        if (session != null) {
            restoreExpBar(player, session);
        }
    }

    /**
     * Reel check: decide whether the fish is caught based on the current state
     * WAITING 1% / LIGHT_BITE 40% / FULL_BITE 10% + charge% × 0.9
     * If the check fails, show the escape feedback and end the session (do not return to waiting)
     *
     * @return true = true = caught; false = escaped (escape feedback already played)
     */
    public static boolean onReel(Player player) {
        HookSession session = SESSIONS.remove(player.getUniqueId());
        if (session == null) {
            return false;
        }
        if (!session.hook.isValid()) {
            // Fix XP loss: restore the taken-over XP bar even if the hook is no longer valid (reeled in or destroyed)
            restoreExpBar(player, session);
            return false;
        }
        // Water check (fix fishing without water): reeling never catches a fish if the hook is not in water
        // (This blocks the 1% catch chance in the waiting state. The XP bar should not be taken over while waiting, but restore it just in case.)
        if (!isHookInWater(session.hook)) {
            restoreExpBar(player, session);
            playEscapedFeedback(player, session.hook);
            return false;
        }
        double chance;
        if (session.state == WaterCloudHookState.FULL_BITE) {
            chance = BASE_CATCH_CHANCE + session.charge * CHARGE_CATCH_FACTOR;
        } else {
            chance = session.state.getCatchRate();
        }
        restoreExpBar(player, session);
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            return true;
        }
        playEscapedFeedback(player, session.hook);
        return false;
    }

    // ==================== State machine tick (every second) ====================

    private static void tick() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, HookSession>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            HookSession session = it.next().getValue();
            Player player = session.player;
            if (player == null || !player.isOnline() || !session.hook.isValid()) {
                // Fix XP loss: restore the taken-over XP bar before the session ends unexpectedly (player offline or hook invalid)
                if (player != null && player.isOnline()) {
                    restoreExpBar(player, session);
                }
                it.remove();
                continue;
            }
            // Timeout fallback (if the player does not reel for a long time, prevent leaks)
            if ((now - session.startTime) / 1000L > MAX_SESSION_SECONDS) {
                restoreExpBar(player, session);
                it.remove();
                continue;
            }
            session.stateTicks++;
            // Water check (fix fishing without water): vanilla bites require the hook to be in water, so the new system must check this too
            if (!isHookInWater(session.hook)) {
                // If the hook is pulled out of the water during light bite/charge (or the water is removed), force the fish to escape, restore XP, and reset the state
                if (session.state != WaterCloudHookState.WAITING) {
                    finishByEscape(player, session);
                }
                // If the hook is out of water while waiting, pause silently: do not check for bites or refresh fishing messages
                // (Same as vanilla behavior: a hook on land never gets a bite; onReel also checks for water)
                continue;
            }
            rollState(session, player);
            refreshActionBar(session, player);
        }
    }

    /** Handle state changes and timeouts (full bite is the charge phase with no local timeout; aimTick handles the 60-second AFK limit) */
    private static void rollState(HookSession session, Player player) {
        WaterCloudHookState state = session.state;
        if (state == WaterCloudHookState.WAITING) {
            // Waiting → light bite: base 10% chance plus the shorter-wait bonus (1.5% per level from level 4)
            double chance = state.getBiteChance() + WaterCloudRodEffects.getNewBiteChanceBonus(session.rodLevel);
            if (ThreadLocalRandom.current().nextDouble() < chance) {
                enterState(session, player, WaterCloudHookState.LIGHT_BITE);
            }
        } else if (state == WaterCloudHookState.LIGHT_BITE) {
            if (ThreadLocalRandom.current().nextDouble() < state.getBiteChance()
                    + WaterCloudRodEffects.getNewLightBiteChanceBonus(session.rodLevel)) {
                enterState(session, player, WaterCloudHookState.FULL_BITE);
            } else if (session.stateTicks >= state.getMaxSeconds()) {
                // If light bite does not advance in 8 seconds, the fish escapes and returns to waiting
                playEscapedFeedback(player, session.hook);
                enterState(session, player, WaterCloudHookState.WAITING);
            }
        }
    }

    /** Enter a new state: reset timer, play sound and particles; on full bite, take over the XP bar and start charging */
    private static void enterState(HookSession session, Player player, WaterCloudHookState next) {
        session.state = next;
        session.stateTicks = 0;
        session.actionBarTicks = 0;
        if (next == WaterCloudHookState.FULL_BITE) {
            // Start charging: save and take over the XP bar (set level to zero; use the bar for the aiming marker)
            if (!session.expTakenOver) {
                session.savedExp = player.getExp();
                session.savedLevel = player.getLevel();
                session.expTakenOver = true;
                player.setLevel(0);
                player.setExp(0.5f);
            }
            session.charge = 0;
            session.fullBiteStartTime = System.currentTimeMillis();
            // On full bite, play a splash sound and show a status message in chat (the action bar is used for the aiming scale)
            player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.9f, 1.0f);
            player.sendMessage(next.getActionBarColor() + "✦ " + WaterCloudHookPhrases.getRandom(next));
        }
        if (next.getSound() != null) {
            player.playSound(player.getLocation(), next.getSound(), 0.6f, 1.0f);
        }
        if (next.getParticle() != null && session.hook.isValid()) {
            player.getWorld().spawnParticle(next.getParticle(),
                    session.hook.getLocation().add(0, 0.2, 0),
                    next.getParticleCount(), 0.15, 0.05, 0.15, 0.01);
        }
    }

    /** Action bar: state color and random message; during full bite, aimTick takes over with the aiming scale */
    private static void refreshActionBar(HookSession session, Player player) {
        WaterCloudHookState state = session.state;
        if (state == WaterCloudHookState.FULL_BITE) {
            return;
        }
        // Waiting/light bite: show immediately on entering the state, then refresh every 3 seconds
        session.actionBarTicks++;
        if (session.actionBarTicks % ACTION_BAR_REFRESH_SECONDS != 1) {
            return;
        }
        player.sendActionBar(state.getActionBarColor() + WaterCloudHookPhrases.getRandom(state));
    }

    /**
     * Aiming scale: 23 characters, symmetric ([ + 21 slots + ]); the █ marker is exactly centered when aimed straight at the bobber
     * The whole scale uses the Magic Expansion v2 gradient (teal → gray-blue → light purple → magenta-purple → pink);
     * The █ marker uses the gradient color at its position and is brightened by 45%
     */
    private static String buildAimBar(double exp) {
        int pos = (int) Math.round(Math.max(0.0, Math.min(1.0, exp)) * 20);
        java.util.List<org.bukkit.Color> colors = ColorGradient.createCustomColorListV2();
        int total = 23; // [ + 21 slots + ]
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < total; i++) {
            double p = (double) i / (total - 1) * (colors.size() - 1);
            int c1 = (int) Math.floor(p);
            int c2 = Math.min(colors.size() - 1, (int) Math.ceil(p));
            double t = p - c1;
            org.bukkit.Color a = colors.get(c1);
            org.bukkit.Color b = colors.get(c2);
            int r = (int) (a.getRed() * (1 - t) + b.getRed() * t);
            int g = (int) (a.getGreen() * (1 - t) + b.getGreen() * t);
            int bl = (int) (a.getBlue() * (1 - t) + b.getBlue() * t);
            boolean isBlock = i >= 1 && i <= 21 && (i - 1) == pos;
            if (isBlock) {
                // Brighten the marker by 45% so it stands out from the background
                r = Math.min(255, r + (255 - r) * 45 / 100);
                g = Math.min(255, g + (255 - g) * 45 / 100);
                bl = Math.min(255, bl + (255 - bl) * 45 / 100);
            }
            sb.append("§x")
                    .append("§").append(ColorGradient.codeColor(r / 16)).append("§").append(ColorGradient.codeColor(r % 16))
                    .append("§").append(ColorGradient.codeColor(g / 16)).append("§").append(ColorGradient.codeColor(g % 16))
                    .append("§").append(ColorGradient.codeColor(bl / 16)).append("§").append(ColorGradient.codeColor(bl % 16));
            if (i == 0) {
                sb.append("[");
            } else if (i == total - 1) {
                sb.append("]");
            } else {
                sb.append((i - 1) == pos ? "█" : "░");
            }
        }
        return sb.toString();
    }

    /** Escape feedback: reel-in sound and an action-bar message */
    private static void playEscapedFeedback(Player player, FishHook hook) {
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 0.7f, 0.9f);
        player.sendActionBar("§7" + WaterCloudHookPhrases.getRandomEscaped());
    }

    /**
     * Water check (fix fishing without water): whether the hook entity is currently in water
     * Vanilla fishing requires the hook to be in water; this method restores that rule after the new system takes over bite checks.
     * Bukkit has no isInWater API, so check the block at the hook (and 0.1 blocks below as a fallback).
     */
    private static boolean isHookInWater(FishHook hook) {
        Location loc = hook.getLocation();
        // Usually the hook sits slightly below the water surface, so its block is water
        if (loc.getBlock().getType() == org.bukkit.Material.WATER) {
            return true;
        }
        // Edge case: the hook may be above the water block when floating at the surface, so check 0.1 blocks below
        Location probe = loc.clone().subtract(0, 0.1, 0);
        return probe.getBlock().getType() == org.bukkit.Material.WATER;
    }

    // ==================== 20 Hz aiming and charging ====================

    /** Splash sound interval during charging (ticks): every 0.5 seconds */
    private static final int SPLASH_SOUND_INTERVAL = 10;

    /** Charging: calculate bobber direction every tick and show it on the XP bar; charge when centered; auto-reel at full charge; escape after 60 seconds idle or too far away */
    private static void aimTick() {
        for (HookSession session : SESSIONS.values()) {
            if (session.state != WaterCloudHookState.FULL_BITE) {
                continue;
            }
            Player player = session.player;
            FishHook hook = session.hook;
            if (player == null || !player.isOnline() || !hook.isValid()) {
                continue;
            }
            // Fish escapes if too far away (uses vanilla fishing distance limit)
            if (player.getLocation().distance(hook.getLocation()) > MAX_HOOK_DISTANCE) {
                finishByEscape(player, session);
                continue;
            }
            // 60-second AFK timeout: escape if charge is not full in time
            if (System.currentTimeMillis() - session.fullBiteStartTime > FULL_BITE_LIMIT_SECONDS * 1000L) {
                finishByEscape(player, session);
                continue;
            }

            // Play a splash sound every 0.5 seconds while charging
            long nowMs = System.currentTimeMillis();
            if (nowMs - session.lastSplashTime >= SPLASH_SOUND_INTERVAL * 50L) {
                session.lastSplashTime = nowMs;
                player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 0.7f, 1.1f);
            }

            // Calculate horizontal direction: bobber angle relative to player minus the player's yaw, normalized to [-180, 180]
            Location playerLoc = player.getEyeLocation();
            Location hookLoc = hook.getLocation();
            double dx = hookLoc.getX() - playerLoc.getX();
            double dz = hookLoc.getZ() - playerLoc.getZ();
            double angle = Math.toDegrees(Math.atan2(dx, dz));
            // Yaw is positive clockwise, while atan2 is positive counterclockwise, so use -(angle + yaw); facing the bobber gives 0 (center of scale)
            double diff = ((-(angle + playerLoc.getYaw()) + 540) % 360) - 180;
            // Marker position on the scale: centered when facing the bobber
            double exp = (diff + 180) / 360.0;

            // XP bar = charge progress (marker direction is shown on the action-bar scale)
            player.setExp((float) Math.max(0.0, Math.min(1.0, session.charge / MAX_CHARGE)));

            // Action-bar aiming scale (own centered line, symmetric; Magic Expansion v2 gradient, marker color changes with position)
            player.sendActionBar(buildAimBar(exp));

            // Charge when within the center range (base rate plus proficiency bonus: +0.5 points/sec per level from level 3)
            if (Math.abs(diff) <= AIM_THRESHOLD_DEGREES) {
                session.charge = Math.min(MAX_CHARGE, session.charge + CHARGE_PER_TICK
                        + WaterCloudRodEffects.getNewChargeSpeedBonus(session.rodLevel) / 20.0);
                if (session.charge >= MAX_CHARGE) {
                    onChargeFull(player, session);
                }
            }
        }
    }

    /** At full charge: restore XP bar, end session, and trigger auto-reel callback */
    private static void onChargeFull(Player player, HookSession session) {
        SESSIONS.remove(player.getUniqueId());
        restoreExpBar(player, session);
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0f, 1.0f);
        Location hookLoc = session.hook.getLocation();
        if (session.hook.isValid()) {
            session.hook.remove();
        }
        if (autoReelHandler != null) {
            autoReelHandler.onAutoReel(player, hookLoc);
        }
    }

    /** AFK timeout / too far away: the fish escapes; show feedback, restore the XP bar, and return to waiting (the bobber stays out and the bite cycle restarts) */
    private static void finishByEscape(Player player, HookSession session) {
        playEscapedFeedback(player, session.hook);
        restoreExpBar(player, session);
        session.state = WaterCloudHookState.WAITING;
        session.stateTicks = 0;
        session.actionBarTicks = 0;
        session.charge = 0;
    }

    /** Restore the player's XP bar (only if this system took it over) */
    private static void restoreExpBar(Player player, HookSession session) {
        if (session.expTakenOver) {
            session.expTakenOver = false;
            player.setExp(session.savedExp);
            player.setLevel(session.savedLevel);
        }
    }

    // ==================== Entity shake (every 5 ticks) ====================

    /** Full bite: shake the hook along the water surface and spawn splash particles to mimic a fish pulling underwater */
    private static void shakeTick() {
        for (HookSession session : SESSIONS.values()) {
            if (session.state != WaterCloudHookState.FULL_BITE) {
                continue;
            }
            Player player = session.player;
            FishHook hook = session.hook;
            if (player == null || !hook.isValid()) {
                continue;
            }
            double dx = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * SHAKE_RADIUS;
            double dz = (ThreadLocalRandom.current().nextDouble() - 0.5) * 2 * SHAKE_RADIUS;
            hook.teleport(hook.getLocation().add(dx, 0, dz));
            player.getWorld().spawnParticle(Particle.SPLASH,
                    hook.getLocation().add(0, 0.1, 0), 2, 0.1, 0, 0.1, 0.01);
        }
    }

    // ==================== Callback registration ====================

    /** Register the auto-reel callback for full charge (registered by the fishing listener during construction) */
    public static void setAutoReelHandler(AutoReelHandler handler) {
        autoReelHandler = handler;
    }

    // ==================== Session data ====================

    public static final class HookSession {

        private final Player player;
        private final FishHook hook;
        private final ItemStack rod;
        private final int rodLevel;
        private final long startTime;
        private WaterCloudHookState state = WaterCloudHookState.WAITING;
        /** Seconds spent in the current state (tick() runs once per second, adding 1 each time) */
        private int stateTicks = 0;
        /** Action-bar message refresh counter (every 3 seconds while waiting/light bite) */
        private int actionBarTicks = 0;
        /** Charge value 0–100 (reset when full bite begins) */
        private double charge = 0;
        /** Timestamp when full bite starts (start of the 60-second AFK timer) */
        private long fullBiteStartTime = 0;
        /** Timestamp of last splash sound (every 0.5 seconds while charging) */
        private long lastSplashTime = 0;
        /** XP bar progress/level before takeover (restored on reel, escape, or disconnect) */
        private float savedExp = 0;
        private int savedLevel = 0;
        private boolean expTakenOver = false;

        private HookSession(Player player, FishHook hook, ItemStack rod) {
            this.player = player;
            this.hook = hook;
            this.rod = rod;
            this.rodLevel = WaterCloudRodProficiency.getLevel(rod);
            this.startTime = System.currentTimeMillis();
        }

        public WaterCloudHookState getState() {
            return state;
        }

        public FishHook getHook() {
            return hook;
        }

        public ItemStack getRod() {
            return rod;
        }

        /** Current charge value 0–100 */
        public double getCharge() {
            return charge;
        }
    }
}

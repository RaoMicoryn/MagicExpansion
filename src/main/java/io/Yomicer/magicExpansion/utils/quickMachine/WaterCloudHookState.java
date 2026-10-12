package io.Yomicer.magicExpansion.utils;

import org.bukkit.Particle;
import org.bukkit.Sound;

/**
 * Water Cloud fishing state machine (separate from vanilla checks)
 * Each state defines its values: chance to advance per second / time limit / catch chance when reeling / feedback (sound and particles)
 */
public enum WaterCloudHookState {

    WAITING(0.10, -1, 0.01, null, null, 0, "§7"),
    LIGHT_BITE(0.20, 8, 0.40, Sound.BLOCK_WATER_AMBIENT, Particle.BUBBLE, 3, "§e"),
    // A full bite starts the charge phase with no time limit (the Manager handles the 60-second AFK timeout); catch chance when reeling is 10% + charge% × 0.9
    FULL_BITE(0.00, -1, 0.00, Sound.ENTITY_FISHING_BOBBER_SPLASH, Particle.SPLASH, 5, "§c");

    private final double biteChance;
    private final int maxSeconds;
    private final double catchRate;
    private final Sound sound;
    private final Particle particle;
    private final int particleCount;
    private final String actionBarColor;

    WaterCloudHookState(double biteChance, int maxSeconds, double catchRate,
                        Sound sound, Particle particle, int particleCount, String actionBarColor) {
        this.biteChance = biteChance;
        this.maxSeconds = maxSeconds;
        this.catchRate = catchRate;
        this.sound = sound;
        this.particle = particle;
        this.particleCount = particleCount;
        this.actionBarColor = actionBarColor;
    }

    public double getBiteChance() {
        return biteChance;
    }

    public int getMaxSeconds() {
        return maxSeconds;
    }

    public double getCatchRate() {
        return catchRate;
    }

    public Sound getSound() {
        return sound;
    }

    public Particle getParticle() {
        return particle;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public String getActionBarColor() {
        return actionBarColor;
    }
}

package io.Yomicer.magicExpansion.items.misc.moreLure;

import io.Yomicer.magicExpansion.MagicExpansion;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ZKBTMPlayer {
    private final MagicExpansion plugin; // Replace with your main class name

    public ZKBTMPlayer(MagicExpansion plugin) {
        this.plugin = plugin;
    }



    /**
     * Music player - plays a relaxing, happy melody that lasts about 30 seconds
     */
    public void playCuteMusic(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        // Friendly message
        player.sendTitle("§e✨", "§eA cute little melody for you~", 10, 60, 20);

        new BukkitRunnable() {
            int beat = 0;
            final int TICKS_PER_BEAT = 12; // ≈100 BPM, a light and cute rhythm

            @Override
            public void run() {
                // === Soft beat: a bell on every beat, plus a shaker on beats 2 and 4 ===
                if (beat % TICKS_PER_BEAT == 0) {
                    world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, 0.8f);
                }
                if (beat % (4 * TICKS_PER_BEAT) == TICKS_PER_BEAT * 1 ||
                        beat % (4 * TICKS_PER_BEAT) == TICKS_PER_BEAT * 3) {
                    world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.2f);
                }

                // === Intro: C major scale going up (C D E F G), bouncy feel ===
                if (beat == 0) {
                    player.sendTitle("§a🌸","§aIntro starts: a little bunny hopping around~");
                }
                int[] melody = {0,2,4,5,7, 7,5,4,2, 0,2,4,5,7}; // C D E F G, G F E D, C D E F G
                float[] volumes = {1.0f, 0.9f, 1.0f, 0.9f, 1.2f, 1.2f, 0.9f, 0.8f, 0.7f, 0.8f, 0.9f, 1.0f, 1.1f, 1.3f};
                for (int i = 0; i < melody.length; i++) {
                    if (beat == i * TICKS_PER_BEAT) {
                        float pitch = (float) Math.pow(2, melody[i] / 12.0) * 1.0f; // C4 is the base
                        world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, volumes[i], pitch);
                        spawnNoteParticle(world, loc, pitch);
                    }
                }

                // === Main melody: a cute bouncy tune ===
                if (beat == 14 * TICKS_PER_BEAT) {
                    player.sendTitle("§d🎀","§d Main melody: a kitten chasing a butterfly~",10,20,20);
                }
                if (beat >= 14 * TICKS_PER_BEAT && beat < 26 * TICKS_PER_BEAT) {
                    int idx = beat / TICKS_PER_BEAT - 14;
                    int[] tune = {7,5,4,2, 0,-1,0,2, 4,5,7,9}; // G E D C, C B C D, E F G A
                    float p = (float) Math.pow(2, tune[idx] / 12.0) * 1.0f;
                    float v = idx % 4 == 0 ? 1.3f : 1.0f; // Stronger on the strong beat
                    world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, v, p);
                    spawnNoteParticle(world, loc, p);
                    if (idx == 0 || idx == 4 || idx == 8) {
                        world.spawnParticle(Particle.HEART, loc.clone().add(0,1,0), 1);
                    }
                }

                // === Chorus: a happy repeating part (the emotional high point) ===
                if (beat == 26 * TICKS_PER_BEAT) {
                    player.sendTitle("§c💖","§cChorus: so happy I'm spinning around!",10,20,20);
                }
                if (beat >= 26 * TICKS_PER_BEAT && beat < 34 * TICKS_PER_BEAT) {
                    int idx = (beat / TICKS_PER_BEAT - 26) % 4;
                    int[] chorus = {7,9,7,5}; // G A G E
                    float p = (float) Math.pow(2, chorus[idx] / 12.0) * 1.0f;
                    world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.4f, p);
                    world.spawnParticle(Particle.FIREWORK, loc.clone().add(0,1.2,0), 2);
                }

                // === Outro, fading out ===
                if (beat == 34 * TICKS_PER_BEAT) {
                    player.sendTitle("§7💫","§7Outro: little stars blinking...",10,20,20);
                    for (int i = 0; i < 3; i++) {
                        int f = i;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            float[] end = {4,2,0}; // E D C
                            float p = (float) Math.pow(2, end[f] / 12.0) * 1.0f;
                            world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f - f * 0.1f, p * 0.9f);
                            world.spawnParticle(Particle.MYCELIUM, loc.clone().add(0,1,0), 1);
                        }, i * 20L);
                    }
                }

                // === The end ===
                if (beat == 40 * TICKS_PER_BEAT) {
                    player.sendTitle("§l🌟🎶\uD83C\uDF39", "You're so cute too!", 0, 60, 20);
                    player.sendMessage("§6🎶 The music is over! Hope you feel better~");
                    this.cancel();
                }

                beat++;
            }

            // Spawn note particles
            private void spawnNoteParticle(World w, Location l, float note) {
                Location p = l.clone().add(Math.random() - 0.5, 1.5, Math.random() - 0.5);
                w.spawnParticle(Particle.NOTE, p, 1, 0, 0, 0, note);
            }
        }.runTaskTimer(plugin, 20L, 1L);
    }



}
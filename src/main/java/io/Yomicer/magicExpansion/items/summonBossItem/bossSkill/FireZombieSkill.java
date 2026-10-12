package io.Yomicer.magicExpansion.items.summonBossItem.bossSkill;

import io.Yomicer.magicExpansion.utils.log.Debug;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class FireZombieSkill {



    // Spawn particle effects
    private static void spawnParticleEffects(Location location,Player player) {
        // Flame particles
        location.getWorld().spawnParticle(Particle.FLAME, location, 1000, 10, 8, 10, 0.1);
//        player.sendMessage("FLAMEtriggered");

        // Smoke particles
        location.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, location, 1000, 10, 8, 10, 0.1);
//        player.sendMessage("CAMPFIRE_COSY_SMOKEtriggered");
        // Magic particles
        // Set the particle color (cyan)
//        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 1.0f);
        location.getWorld().spawnParticle(Particle.ENCHANT, location, 1000, 10, 8, 10, 0.1);
//        player.sendMessage("ENCHANTtriggered");
    }

    // Spawn particle effects
    private static void spawnOneParticle(Location location,Player player, Particle particle, int num) {
        // Flame particles
        location.getWorld().spawnParticle(particle, location, num, 10, 8, 10, 0.1);
//        player.sendMessage("FLAMEtriggered");
    }
    private static void spawnOneParticle(Location location,Player player, Particle particle, int num, double x, double y, double z) {
        // Flame particles
        location.getWorld().spawnParticle(particle, location, num, x, y, z, 0.1);
//        player.sendMessage("FLAMEtriggered");
    }

    private static void spawnOneParticle(Location location, Player player, Particle particle, Particle.DustOptions extraInfo){
        location.getWorld().spawnParticle(particle, location, 1000, 10, 8, 10, 0.1,extraInfo);
    }


    /**
     * Get nearby players
     */
    private static List<Player> getNearbyPlayers(LivingEntity mob, double x,double y) {
        return mob.getWorld().getNearbyEntities(mob.getLocation(), x, y, x).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .collect(Collectors.toList());
    }

    /**
     * Skill: Magic Attack
     */
    public static void magicAttackSkill(LivingEntity mob, String bossName) {
        for (Player nearbyPlayer : getNearbyPlayers(mob,10,5)) {
            nearbyPlayer.damage(10, mob); // Deals 5 damage
            nearbyPlayer.sendMessage(bossName+" used a magic attack on you!");
            spawnOneParticle(nearbyPlayer.getLocation(), nearbyPlayer, Particle.BUBBLE,2500);
        }
    }

    /**
     * Skill: Redstone Particle Attack - Zombie's special mind attack
     */
    public static void redstoneParticleAttackSkill(LivingEntity mob) {
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 1.0f);
        for (Player nearbyPlayer : getNearbyPlayers(mob,10,5)) {
            nearbyPlayer.damage(8, mob); // Deals 5 damage
            nearbyPlayer.sendMessage("§b§lThe Fire Zombie used a mind attack on you!");
            spawnOneParticle(nearbyPlayer.getLocation(), nearbyPlayer, Particle.DUST, dustOptions);
            // Add negative effects (last 2 seconds, level 10)
            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 40, 10)); // Slower attacks
            nearbyPlayer.sendMessage(ChatColor.RED + "§lYour arms feel super heavy, " + ChatColor.YELLOW + "§lyour attack speed is way down!");
            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 10));         // Slower movement
            nearbyPlayer.sendMessage(ChatColor.DARK_BLUE + "§lYour legs feel like they're made of lead, " + ChatColor.GOLD + "§lyou can barely move!");
            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 10));    // Blindness
            nearbyPlayer.sendMessage(ChatColor.BLACK + "§lDarkness fills your vision, " + ChatColor.LIGHT_PURPLE + "§lyou can barely see anything!");
            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 180, 10));    // Nausea
            nearbyPlayer.sendMessage(ChatColor.GREEN + "§lA strong wave of dizziness hits you, " + ChatColor.DARK_GREEN + "§leverything starts spinning!");
            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 40, 10));       // Hunger
            nearbyPlayer.sendMessage(ChatColor.DARK_RED + "§lYour stomach growls painfully, " + ChatColor.GRAY + "§lhunger is draining your energy!");
            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 10));     // Darkness (supported in 1.19+)
            nearbyPlayer.sendMessage(ChatColor.DARK_GRAY + "§lDeep darkness takes over, " + ChatColor.WHITE + "§land you feel lost in endless darkness!");
        }
    }
    /**
     * Skill: Gust
     */
    public static void redstoneParticleAttackSkillWindElf(LivingEntity mob) {
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 105, 180), 5.0f);
        for (Player nearbyPlayer : getNearbyPlayers(mob,15,8)) {
            nearbyPlayer.damage(21, mob); // Deals 21 damage
            nearbyPlayer.sendMessage("§3§lThe Wind Elf hit you with a gust attack!");
            spawnOneParticle(nearbyPlayer.getLocation(), nearbyPlayer, Particle.DUST, dustOptions);
            // 1. Off balance: levitation makes it feel like the wind is lifting you up
            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 80, 5)); // The air lifts you up
            nearbyPlayer.sendMessage(ChatColor.AQUA + "§l↑↑↑ A wild gust sends you flying high!" + ChatColor.WHITE + " §lYou keep floating up and can't control it...");

            // 2. Slowdown: slowness + mining fatigue
            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 3));
            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 3));
            nearbyPlayer.sendMessage(ChatColor.LIGHT_PURPLE + "§l🌀 The wind wraps around you like chains, " + ChatColor.GRAY + "§lyour movements feel slow and weak...");

            // 3. Vision trouble: nausea makes you dizzy and distorts your view
            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 250, 3));
            nearbyPlayer.sendMessage(ChatColor.DARK_AQUA + "§l🌪️ The world spins around you, " + ChatColor.YELLOW + "§lyou can't tell up from down...");

            // 4. Hard to breathe: hunger represents lost stamina
            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 80, 3));
            nearbyPlayer.sendMessage(ChatColor.DARK_RED + "§l💨 The strong wind steals your breath, " + ChatColor.GOLD + "§lyour energy drains with every breath...");

            // 5. Storm's eye: brief blindness, like dust getting in your eyes
            nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 100));
            nearbyPlayer.sendMessage(ChatColor.BLACK + "§l🌫️ A cloud of pale dust hits your face, " + ChatColor.WHITE + "§lyou can barely see...");

            // Optional: DARKNESS is supported in 1.19+ and fits the storm blocking out the light
            if (Bukkit.getVersion().contains("1.19") || Bukkit.getVersion().contains("1.20") || Bukkit.getVersion().contains("1.21")) {
                nearbyPlayer.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 100));
                nearbyPlayer.sendMessage(ChatColor.DARK_GRAY + "🌑 The Wind Elf's fury blocks out the light, " + ChatColor.GRAY + "§leven your soul is swallowed by darkness...");
            }
        }
    }

    /**
     * Skill: Fire Attack
     */
    public static void fireParticleAttackSkill(LivingEntity mob) {

        // Get nearby players
        List<Player> nearbyPlayers = getNearbyPlayers(mob,10,5);

        // If no players are in range, stop here
        if (nearbyPlayers.isEmpty()) {
            return;
        }

        // Pick a random player as the target
        Player targetPlayer = nearbyPlayers.get(new Random().nextInt(nearbyPlayers.size()));

        // Damage the target player
        targetPlayer.damage(88, mob); // Deals 88 damage

        // Send a message to the target player
        targetPlayer.sendMessage("§c§lThe Fire Zombie has locked onto you!");
        targetPlayer.sendMessage("§e§lScorching flames are consuming your soul...");
        targetPlayer.sendMessage("§c§lYou took " +"§e§l88 damage§c§l" +"！");

        // Add a link-line effect
        spawnLinkEffect(mob.getLocation(), targetPlayer.getLocation(),Color.RED);
        // Spawn flame particles at the target player's location
        spawnOneParticle(targetPlayer.getLocation(), targetPlayer, Particle.FLAME, 200, 1, 1, 1);

        // Notify other nearby players (not the target)
        for (Player nearbyPlayer : nearbyPlayers) {
            if (!nearbyPlayer.equals(targetPlayer)) {
                nearbyPlayer.sendMessage("§e§lThe Fire Zombie just used a fire attack!");
                nearbyPlayer.sendMessage("§e§lIt locked onto §b"+ targetPlayer.getName() + "§e§l！");
            }
        }
    }
    /**
     * Skill: Twin Wind Attack
     */
    public static void twoWindParticleAttackSkill(LivingEntity mob) {

        // Get nearby players
        List<Player> nearbyPlayers = getNearbyPlayers(mob,15,8);

        // If no players are in range, stop here
        if (nearbyPlayers.isEmpty()) {
            return;
        }

        // Pick a random player as the target
        Player targetPlayer = nearbyPlayers.get(new Random().nextInt(nearbyPlayers.size()));
        Player targetPlayer2 = nearbyPlayers.get(new Random().nextInt(nearbyPlayers.size()));

        // Damage the target player
        targetPlayer.damage(68, mob); // Deals 78 damage
        targetPlayer2.damage(136, mob); // Deals 78 damage

        // Send a message to the target player
        targetPlayer.sendMessage("§b§lWind X has locked onto you!");
        targetPlayer.sendMessage("§e§lA fierce wind rushes in, trying to break through your defense...");
        targetPlayer.sendMessage("§b§lYou took " + "§e§l68 damage§b§l" + "！");

        targetPlayer2.sendMessage("§b§lWind Y has locked onto you!");
        targetPlayer2.sendMessage("§e§lThe fierce wind cuts through your armor and tears at you...");
        targetPlayer2.sendMessage("§b§lYou took " + "§e§l136 damage§b§l" + "！");

        // Add a link-line effect
        spawnLinkEffect(mob.getLocation(), targetPlayer.getLocation(), Color.AQUA);
        spawnLinkEffect(mob.getLocation(), targetPlayer2.getLocation(), Color.AQUA);
        // Spawn flame particles at the target player's location
        spawnOneParticle(targetPlayer.getLocation(), targetPlayer, Particle.SPLASH, 300, 1, 1, 1);
        spawnOneParticle(targetPlayer2.getLocation(), targetPlayer, Particle.SPLASH, 300, 1, 1, 1);

        // Notify other nearby players (not the target)
        for (Player nearbyPlayer : nearbyPlayers) {
            if (!nearbyPlayer.equals(targetPlayer)&&!nearbyPlayer.equals(targetPlayer2)) {
                nearbyPlayer.sendMessage("§e§lThe Wind Elf just used its Twin Wind attack!");
                nearbyPlayer.sendMessage("§e§lWind X locked onto §b"+ targetPlayer.getName() + "§e§l！");
                nearbyPlayer.sendMessage("§e§lWind Y locked onto §b"+ targetPlayer2.getName() + "§e§l！");
            }
        }
    }



    /**
     * Create a link line between two locations
     *
     * @param start Starting point (for example, the Fire Zombie's location)
     * @param end   End point (for example, the target player's location)
     */
    public static void spawnLinkEffect(Location start, Location end,Color color) {
        // Get the coordinates of both points
        double startX = start.getX();
        double startY = start.getY();
        double startZ = start.getZ();

        double endX = end.getX();
        double endY = end.getY();
        double endZ = end.getZ();

        // Calculate the distance between the two points
        double distance = start.distance(end);

        // Set the number and spacing of particles
        int particleCount = (int) (distance * 5); // Spawn 5 particles per block
        double deltaX = (endX - startX) / particleCount;
        double deltaY = (endY - startY) / particleCount;
        double deltaZ = (endZ - startZ) / particleCount;

        // Spawn particles between the two points
        for (int i = 0; i < particleCount; i++) {
            double x = startX + deltaX * i;
            double y = startY + deltaY * i;
            double z = startZ + deltaZ * i;

            // Create the particle location
            Location particleLocation = new Location(start.getWorld(), x, y, z);

            // Spawn particles (like a red or energy line)
            start.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, new Particle.DustOptions(color, 1.0f));
        }
    }




}

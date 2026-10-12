package io.Yomicer.magicExpansion.items.misc.weapon;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.utils.log.Debug;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StarShardsSword extends SimpleSlimefunItem<ItemUseHandler> implements RecipeDisplayItem, Listener {


    // New custom multiplier
//    public static final double DAMAGE_MULTIPLIER = 61.8;
    // A5: the skill cooldown table is now static + ConcurrentHashMap, so it's thread-safe and can be cleared by a static cleanup
    private static final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    // A5: the cooldown message rate-limit timestamps are now static + ConcurrentHashMap
    private static final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    // A3: Astral Shield invincibility table - saves player UUID → invincibility end timestamp, and invincibility is done by the event cancel logic in onEntityDamage
    private static final Map<UUID, Long> invulnerableUntil = new ConcurrentHashMap<>();
    // A7: attack handling re-entry lock - the target.damage(...) inside this method fires EntityDamageByEntityEvent again and comes back into this handler,
    // and if we don't block it, it causes attack event → damage() → attack event → damage() … infinite recursion (StackOverflowError).
    // All damage events happen synchronously on the main thread, so we use a ThreadLocal to mark the handling window of this attack, and all nested damage events inside it are skipped.
    private static final ThreadLocal<Boolean> PROCESSING_ATTACK = ThreadLocal.withInitial(() -> false);
    // A6: removed the unused dead field bleedingTasks for bleed tasks (bleed tasks are really saved in the target entity's metadata)

    Config cfg = new Config(MagicExpansion.getInstance());
    Double StarShards_Atk_Mix = cfg.getDouble("StarShardsSword.StarShards_Atk_Mix");
    Double StarShards_Atk_Add = cfg.getDouble("StarShardsSword.StarShards_Atk_Add");
    Double StarShards_Atk_Mult = cfg.getDouble("StarShardsSword.StarShards_Atk_Mult");
    Double StarShards_Atk_Speed = cfg.getDouble("StarShardsSword.StarShards_Atk_Speed");
    Double StarShards_Atk_ExtraPercent = cfg.getDouble("StarShardsSword.StarShards_Atk_ExtraPercent");
    Double StarShards_Atk_Blood = cfg.getDouble("StarShardsSword.StarShards_Atk_Blood");
    Double StarShards_Atk_Fire = cfg.contains("StarShardsSword.StarShards_Atk_Fire")
            ? cfg.getDouble("StarShardsSword.StarShards_Atk_Fire") : 0.8;
    Double StarShards_ArcaneBlast_Mult = cfg.contains("StarShardsSword.StarShards_ArcaneBlast_Mult")
            ? cfg.getDouble("StarShardsSword.StarShards_ArcaneBlast_Mult") : 0.6;
    Double StarShards_Health_Add = cfg.getDouble("StarShardsSword.StarShards_Health_Add");
    Double StarShards_Health_Mult = cfg.getDouble("StarShardsSword.StarShards_Health_Mult");
    Double StarShards_MoveSpeed = cfg.getDouble("StarShardsSword.StarShards_MoveSpeed");
    Double StarShards_Armor = cfg.getDouble("StarShardsSword.StarShards_Armor");
    Double StarShards_Toughness = cfg.getDouble("StarShardsSword.StarShards_Toughness");
    Double StarShards_FlySpeed = cfg.getDouble("StarShardsSword.StarShards_FlySpeed");
    Long StarShards_BlazingSlash_CD = cfg.getLong("StarShardsSword.StarShards_BlazingSlash_CD");
    Long StarShards_ArcaneBlast_CD = cfg.getLong("StarShardsSword.StarShards_ArcaneBlast_CD");
    Long StarShards_AstralShield_CD = cfg.getLong("StarShardsSword.StarShards_AstralShield_CD");
    Long StarShards_AstralShield_During = cfg.getLong("StarShardsSword.StarShards_AstralShield_During");
    Long StarShards_InstantBlink_CD = cfg.getLong("StarShardsSword.StarShards_InstantBlink_CD");



    public StarShardsSword(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        ItemMeta meta = getItem().getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);

            String namespace = "star_shards_sword";

            // 💥 Attack damage +1314 (fixed value)
            UUID atk1Id = UUID.nameUUIDFromBytes((namespace + "_atk_add").getBytes());
            meta.addAttributeModifier(
                    Attribute.GENERIC_ATTACK_DAMAGE,
                    new AttributeModifier(atk1Id, "StarShards_Atk_Add", StarShards_Atk_Add, AttributeModifier.Operation.ADD_NUMBER)
            );

            // 💥 Attack damage +618% (multiply)
            UUID atk2Id = UUID.nameUUIDFromBytes((namespace + "_atk_mult").getBytes());
            meta.addAttributeModifier(
                    Attribute.GENERIC_ATTACK_DAMAGE,
                    new AttributeModifier(atk2Id, "StarShards_Atk_Mult", StarShards_Atk_Mult, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            );

            // ⚡ Attack speed +2000% → final speed = base speed × (1 + 20.0) = 21x!
            UUID atkSpeedId = UUID.nameUUIDFromBytes((namespace + "_atk_speed").getBytes());
            meta.addAttributeModifier(
                    Attribute.GENERIC_ATTACK_SPEED,
                    new AttributeModifier(atkSpeedId, "StarShards_AtkSpeed", StarShards_Atk_Speed, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            );

            // ❤️ Health +1314 (fixed value, the unit is "half hearts", so +1314 = +657 hearts!)
            UUID health1Id = UUID.nameUUIDFromBytes((namespace + "_health_add").getBytes());
            meta.addAttributeModifier(
                    Attribute.GENERIC_MAX_HEALTH,
                    new AttributeModifier(health1Id, "StarShards_Health_Add", StarShards_Health_Add, AttributeModifier.Operation.ADD_NUMBER)
            );

            // ❤️ Health +618% (multiply)
            UUID health2Id = UUID.nameUUIDFromBytes((namespace + "_health_mult").getBytes());
            meta.addAttributeModifier(
                    Attribute.GENERIC_MAX_HEALTH,
                    new AttributeModifier(health2Id, "StarShards_Health_Mult", StarShards_Health_Mult, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            );

            // 🏃 Movement speed +1314% → final speed = base speed × (1 + 13.14) = 14.14x!
            UUID moveSpeedId = UUID.nameUUIDFromBytes((namespace + "_move_speed").getBytes());
            meta.addAttributeModifier(
                    Attribute.GENERIC_MOVEMENT_SPEED,
                    new AttributeModifier(moveSpeedId, "StarShards_MoveSpeed", StarShards_MoveSpeed, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            );

            // 🛡️ Armor +200 (fixed value)
            UUID armorId = UUID.nameUUIDFromBytes((namespace + "_armor").getBytes());
            meta.addAttributeModifier(
                    Attribute.GENERIC_ARMOR,
                    new AttributeModifier(armorId, "StarShards_Armor", StarShards_Armor, AttributeModifier.Operation.ADD_NUMBER)
            );

            // 🧱 Armor toughness +200 (fixed value)
            UUID toughnessId = UUID.nameUUIDFromBytes((namespace + "_toughness").getBytes());
            meta.addAttributeModifier(
                    Attribute.GENERIC_ARMOR_TOUGHNESS,
                    new AttributeModifier(toughnessId, "StarShards_Toughness", StarShards_Toughness, AttributeModifier.Operation.ADD_NUMBER)
            );

            // ✈️ Flying speed +1314%
            UUID flySpeedId = UUID.nameUUIDFromBytes((namespace + "_fly_speed").getBytes());
            meta.addAttributeModifier(
                    Attribute.GENERIC_FLYING_SPEED,
                    new AttributeModifier(flySpeedId, "StarShards_FlySpeed", StarShards_FlySpeed, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            );


            getItem().setItemMeta(meta);
        }
        Bukkit.getPluginManager().registerEvents(this, MagicExpansion.getInstance());
    }

    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return e -> {
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);

            if (e.getHand() != EquipmentSlot.HAND) return;

            Player player = e.getPlayer();
            boolean isSneaking = player.isSneaking();
            Action action = e.getInteractEvent().getAction();

            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                if (isSneaking) {
                    useInstantBlink(player);
                } else {
                    useAstralShield(player);
                }
            }
        };
    }

    private static final Set<UUID> holyProtectedPlayers = ConcurrentHashMap.newKeySet();
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        // A3: Astral Shield invincibility check (replaces the old player.setInvulnerable approach)
        Long until = invulnerableUntil.get(p.getUniqueId());
        boolean shieldActive = until != null && System.currentTimeMillis() < until;
        if (shieldActive || holyProtectedPlayers.contains(p.getUniqueId())) {
            event.setCancelled(true);
            if (event.getCause() != EntityDamageEvent.DamageCause.VOID) {
                // A8: Astral Shield reflect - the lore says "ranged attacks are reflected back toward the source automatically", and now it works like this: projectiles get flipped and sent back
                if (event instanceof EntityDamageByEntityEvent byEntity
                        && byEntity.getDamager() instanceof Projectile projectile) {
                    Vector vel = projectile.getVelocity();
                    if (vel.lengthSquared() > 0.001) {
                        projectile.setVelocity(vel.normalize().multiply(2.2).multiply(-1));
                        p.getWorld().spawnParticle(Particle.ENCHANTED_HIT, projectile.getLocation(), 12, 0.2, 0.2, 0.2, 0.1);
                        p.getWorld().playSound(projectile.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 1.8f);
                    }
                } else {
                    p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().add(0, 0.5, 0), 5, 0.2, 0.2, 0.2, 0.01);
                }
            }
        }
        // A3: while we're here, remove entries whose invincibility has expired, so nothing is left in the Map
        if (until != null && !shieldActive) {
            invulnerableUntil.remove(p.getUniqueId());
        }
    }


    // ✅ Attack event listener (the only way in SF9)
    // A1: added ignoreCancelled = true, so attack events cancelled by plugins like land protection don't deal damage again
    @EventHandler(ignoreCancelled = true)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        // A7: inside this attack's handling window, nested damage events fired by this plugin's target.damage(...) are skipped, to prevent infinite recursion
        if (PROCESSING_ATTACK.get()) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // Check if the player's main hand is holding this Slimefun item
        ItemStack hand = player.getInventory().getItemInMainHand();
        SlimefunItem handSfItem = getByItem(hand);
        if (!(handSfItem instanceof StarShardsSword)) return;

        // A7: run it with the handling lock on, so any nested damage event (including the skill's target.damage) gets blocked at the start of this method
        PROCESSING_ATTACK.set(true);
        try {
            // Apply the damage multiplier
            // *New fixed percent damage
            // 1. Work out the true damage to add this time (keeps the original formula)
            double damageToDeal = event.getDamage() * StarShards_Atk_Mix
                    + target.getMaxHealth() * (StarShards_Atk_ExtraPercent);

            // A2: the old target.setHealth(...) took health directly and skipped the event system (ignoring land protection / boss damage caps),
            // so now we use target.damage(damage, player) to go through the normal damage event, and the total damage stays about the same
            target.damage(damageToDeal, player);

            if (target.isDead()) return;

            // --- *New: trigger the bleed effect (Bleed Effect) ---
            // Simplified bleed
            applyBleedEffect(player, target);

            // Trigger the skill (baseDamage is used to work out the skill damage in proportion to this attack's damage)
            double baseDamage = event.getDamage();
            if (player.isSneaking()) {
                castArcaneBlast(player, event.getEntity().getLocation(), baseDamage);
            } else {
                castBlazingSlash(player, event.getEntity().getLocation(), baseDamage);
            }
        } finally {
            // A7: whether it ends normally or throws an exception, we must release the handling lock, so it doesn't affect later attack events
            PROCESSING_ATTACK.set(false);
        }
    }

    // After the change: a bleed effect that supports stacking, takes health directly, and keeps a minimum of 0.1 health
    private void applyBleedEffect(Player damager, LivingEntity target) {
        // 1. Work out the damage per second (StarShards_Atk_Blood percent of the target's max health)
        double damagePerSecond = target.getMaxHealth() * StarShards_Atk_Blood;

        // 2. Create a unique ID for the bleed effect, used to tag the target
        // Format is "MagicExpansion_BLEED_<attacker UUID>", so bleed effects from different players can stack
        String bleedTagKey = "MagicExpansion_BLEED_" + damager.getUniqueId();

        // 3. Start a task that lasts 8 seconds (async)
        BukkitTask bleedTask = new BukkitRunnable() {
            int ticksPassed = 0; // Counts how many game ticks have passed

            // A6: when the task ends, remove itself from the task list in the target's metadata; if the list is empty, remove the whole metadata tag so nothing is left behind
            private void finishAndCleanup() {
                this.cancel();
                if (target.hasMetadata(bleedTagKey)) {
                    List<BukkitTask> tasks = (List<BukkitTask>) target.getMetadata(bleedTagKey).get(0).value();
                    if (tasks != null) {
                        tasks.remove(this);
                        if (tasks.isEmpty()) {
                            target.removeMetadata(bleedTagKey, MagicExpansion.getInstance());
                        }
                    }
                }
            }

            @Override
            public void run() {
                // If the target is dead or invalid, clean up the metadata and cancel the task
                if (!target.isValid() || target.isDead()) {
                    finishAndCleanup();
                    return;
                }

                // Runs once every second (20 game ticks)
                if (ticksPassed % 20 == 0) {
                    // --- Main change: use setHealth to take health directly ---
                    double newHealth = target.getHealth() - damagePerSecond;

                    // --- Main change: if health is 0 or less, force it to 0.1 ---
                    if (newHealth <= 0.0) {
                        newHealth = 0.1;
                    }

                    target.setHealth(newHealth);

                    // Spawn blood particles at the target's location
                    target.getWorld().spawnParticle(
                            Particle.DUST,
                            target.getLocation().add(0, 1, 0),
                            5,
                            0.3, 0.3, 0.3,
                            0,
                            new Particle.DustOptions(Color.RED, 1.5F) // Newly added color and size parameters
                    );
                }

                ticksPassed++;

                // After 8 seconds (160 game ticks), end the task and clean up the metadata
                if (ticksPassed >= 160) {
                    finishAndCleanup();
                }
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, 1L); // Starts right away, checks every 1 game tick

        // 4. Save this task in the target's metadata, so effects can stack
        List<BukkitTask> targetBleedTasks;

        // Check if the target already has this bleed tag
        if (target.hasMetadata(bleedTagKey)) {
            // If it does, safely get the old task list
            targetBleedTasks = (List<BukkitTask>) target.getMetadata(bleedTagKey).get(0).value();
        } else {
            // If it doesn't, create a brand new list
            targetBleedTasks = new ArrayList<>();
        }

        // Add the newly started bleed task to the list
        targetBleedTasks.add(bleedTask);

        // Update the metadata (replace the old data)
        target.setMetadata(bleedTagKey, new FixedMetadataValue(MagicExpansion.getInstance(), targetBleedTasks));
    }


    // ========== Cooldown and skill methods (unchanged) ==========
    private boolean checkCooldown(Player player, String skill, long seconds) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        cooldowns.putIfAbsent(id, new ConcurrentHashMap<>()); // A5: the inner Map is also changed to ConcurrentHashMap
        Map<String, Long> map = cooldowns.get(id);

        if (map.containsKey(skill)) {
            long last = map.get(skill);
            if (now < last + seconds * 1000L) {
                // Stop message spam: no more messages within 500ms
                Long lastMsg = lastMessageTime.getOrDefault(id, 0L);
                if (now - lastMsg > 500) {
                    long remain = ((last + seconds * 1000L - now) + 999) / 1000;
                    player.sendMessage("§cSkill is cooling down, " + remain + " more seconds");
                    lastMessageTime.put(id, now);
                }
                return false;
            }
        }
        map.put(skill, now);
        return true;
    }

    private void castBlazingSlash(Player player, Location hitLoc, double baseDamage) {
        if (!checkCooldown(player, "blazing_slash", StarShards_BlazingSlash_CD)) return;

        player.getWorld().playSound(hitLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.3f);

        // 🔥 Flame Sword Aura: a trail of fire from the player to the hit point
        Vector slashDir = hitLoc.toVector().subtract(player.getLocation().toVector());
        double slashDist = slashDir.length();
        if (slashDist > 0.01) slashDir.normalize();
        for (double d = 0; d <= slashDist; d += 0.35) {
            Location p = player.getLocation().clone().add(slashDir.clone().multiply(d)).add(0, 1, 0);
            player.getWorld().spawnParticle(Particle.FLAME, p, 3, 0.06, 0.06, 0.06, 0.02);
        }

        // 💥 Small explosion (particles only: doesn't break terrain or hurt the player, and damage all goes through the damage event to respect land protection)
        player.getWorld().spawnParticle(Particle.EXPLOSION, hitLoc, 10, 0.2, 0.2, 0.2, 0);
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, hitLoc, 3, 0.1, 0.1, 0.1, 0);
        player.getWorld().spawnParticle(Particle.FLAME, hitLoc, 30, 0.5, 0.5, 0.5, 0.1);

        // 🔥 Extra fire damage: this attack's damage × multiplier (A8: real damage, not just setting on fire)
        double fireDamage = baseDamage * StarShards_Atk_Fire;

        for (Entity e : hitLoc.getWorld().getNearbyEntities(hitLoc, 2.8, 2.8, 2.8)) {
            if (e instanceof LivingEntity le && e != player && le.isValid()) {
                // 🔥 True fire damage
                if (fireDamage > 0 && !le.isDead()) {
                    le.damage(fireDamage, player);
                }
                // Set on fire
                le.setFireTicks(80);

                // 🧨 Safely work out the knockback direction
                Location entityLoc = e.getLocation();
                Vector toEntity = entityLoc.toVector().subtract(hitLoc.toVector());
                double distance = toEntity.length();

                // If it's too close (< 0.1), use a random horizontal direction instead, to avoid NaN
                if (distance < 0.1) {
                    // Random horizontal direction (XZ plane)
                    double angle = Math.random() * 2 * Math.PI;
                    toEntity = new Vector(Math.cos(angle), 0, Math.sin(angle));
                } else {
                    toEntity.normalize();
                }

                // Apply the knockback: horizontal direction + fixed upward
                toEntity.multiply(0.9).setY(0.5);
                le.setVelocity(toEntity); // ✅ Safe now!
            }
        }
    }

    private void castArcaneBlast(Player player, Location origin, double baseDamage) {
        if (!checkCooldown(player, "arcane_blast", StarShards_ArcaneBlast_CD)) return;

        Vector playerForward = player.getEyeLocation().getDirection().normalize();
        Location playerOrigin = player.getEyeLocation();

        double coneAngleCos = Math.cos(Math.toRadians(25)); // ±25 degree cone
        List<LivingEntity> targets = new ArrayList<>();

        for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(playerOrigin, 8.0)) {
            if (entity == player || !entity.isValid()) continue;

            Vector toEntity = entity.getLocation().toVector().subtract(playerOrigin.toVector());
            double distance = toEntity.length();

            if (distance == 0) continue;

            toEntity.normalize();
            double dot = playerForward.dot(toEntity); // Cosine of the angle

            // If it's inside the cone (angle ≤ 25°)
            if (dot >= coneAngleCos) {
                targets.add(entity);
            }
        }

        // 🎵 Sound: magic release
        player.getWorld().playSound(origin, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.7f);

        // ✨ Arcane Beam: moves forward piece by piece (A8: feels more like charging up / a piercing pulse)
        BukkitRunnable beam = new BukkitRunnable() {
            int segment = 0;
            final int maxSegment = 20; // 8 blocks / 0.4

            @Override
            public void run() {
                segment++;
                if (segment > maxSegment) {
                    this.cancel();
                    return;
                }
                Location p = origin.clone().add(playerForward.clone().multiply(segment * 0.4));
                player.getWorld().spawnParticle(Particle.END_ROD, p, 2, 0.05, 0.05, 0.05, 0);
                player.getWorld().spawnParticle(Particle.WITCH, p, 2, 0.05, 0.05, 0.05, 0);
                if (segment % 4 == 0) {
                    player.getWorld().playSound(p, Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.6f);
                }
            }
        };
        beam.runTaskTimer(MagicExpansion.getInstance(), 0L, 1L);

        // 💥 For each target: damage (A8: based on this attack's damage bonus, not a fixed 10) + knockback + weakness + slowness
        double arcaneDamage = baseDamage * StarShards_ArcaneBlast_Mult;
        for (LivingEntity target : targets) {
            target.damage(arcaneDamage, player);

            // Knockback (along the beam direction)
            Vector knockback = playerForward.clone().multiply(1.1).setY(0.3);
            target.setVelocity(knockback);

            // Status effects
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0)); // 4 seconds
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));     // 4 seconds

            // Hit flash
            target.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, target.getLocation(), 5, 0.1, 0.1, 0.1, 0);
            target.getWorld().spawnParticle(Particle.FIREWORK, target.getLocation(), 10, 0.2, 0.2, 0.2, 0.05);
        }

        if (targets.isEmpty()) {
            // Play the ending sound even if nothing was hit
            player.sendMessage("§7Arcane Blast released, but it didn't hit any target.");
        }
    }

    private void useAstralShield(Player player) {
        if (!checkCooldown(player, "astral_shield", StarShards_AstralShield_CD)) return;
        player.sendMessage("§b✨ Astral Shield activated!");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        // A3: removed the old player.setInvulnerable(true/false) approach, and now we save the invincibility end timestamp instead,
        // and the invincibility period is done by the event cancel logic in onEntityDamage (so we don't hold the vanilla invulnerable flag for a long time)
        invulnerableUntil.put(player.getUniqueId(),
                System.currentTimeMillis() + StarShards_AstralShield_During * 1000L);

        holyProtectedPlayers.add(player.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                holyProtectedPlayers.remove(player.getUniqueId());
                if (player.isOnline()) {
                    player.sendMessage(ChatColor.GRAY + "§7Astral Shield has faded...");
                }
            }
        }.runTaskLater(MagicExpansion.getInstance(), StarShards_AstralShield_During*20L);

    }

    private void useInstantBlink(Player player) {
        if (!checkCooldown(player, "instant_blink", StarShards_InstantBlink_CD)) return;
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();
        Location dest = null;
        for (double d = 1.0; d <= 15; d += 0.5) {
            Location point = eye.clone().add(dir.clone().multiply(d));
            if (point.getBlock().getType().isSolid()) {
                dest = point.add(0, 1, 0);
                break;
            }
        }
        if (dest == null) {
            player.sendMessage("§cNo obstacle ahead, can't teleport!");
            return;
        }
        player.teleport(dest);
        // Jump afterimage + landing shockwave
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 40, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().playSound(dest, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 1.2f);
        player.getWorld().spawnParticle(Particle.EXPLOSION, dest, 12, 0.3, 0.3, 0.3, 0);
        player.getWorld().spawnParticle(Particle.PORTAL, dest, 40, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.CLOUD, dest, 15, 0.2, 0.2, 0.2, 0.01);
        for (Entity e : dest.getWorld().getNearbyEntities(dest, 2.0, 2.0, 2.0)) {
            if (e instanceof LivingEntity le && e != player) {
                // A8: brief stun (blindness + slowness)
                le.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20, 0));
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                // Shockwave knockback (away from the landing point)
                Vector k = e.getLocation().toVector().subtract(dest.toVector());
                if (k.lengthSquared() < 0.01) {
                    k = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
                }
                k.normalize().multiply(0.8).setY(0.4);
                le.setVelocity(k);
            }
        }
    }

    /**
     * A4: session data cleanup when a player leaves (called by PlayerCleanupListener):
     * clears per-player state such as skill cooldowns, cooldown message rate-limit timestamps, Astral Shield invincibility, the Holy Shield list, etc.,
     * so the Maps don't keep offline players' data and cause a memory leak.
     */
    public static void cleanup(UUID uuid) {
        cooldowns.remove(uuid);           // Clear the skill cooldown data
        lastMessageTime.remove(uuid);     // Clear the cooldown message rate-limit timestamp
        invulnerableUntil.remove(uuid);   // Clear the Astral Shield invincibility timestamp
        holyProtectedPlayers.remove(uuid);// Clear the Holy Shield player list
    }

    @Override
    public @NotNull List<ItemStack> getDisplayRecipes() {
        return List.of();
    }
}
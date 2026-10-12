package io.Yomicer.magicExpansion.utils;

import io.Yomicer.magicExpansion.MagicExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Virtual player manager (for RightClickMan interaction bots).
 *
 * Two options (A and B, as requested):
 *  Option A: Citizens NPC — if Citizens2 is installed, use reflection to create an invisible NPC,
 *          whose getEntity() is a Player instance, so a PlayerInteractEvent can be created directly;
 *  Option B: NMS fake player — without Citizens, use reflection to add a fake ServerPlayer to the world
 *          (tested on Paper 1.20.4; version-sensitive, so mappings may need updates after server upgrades);
 *  Fallback: if both options fail, return null and let the caller use a nearby real player.
 *
 * Only one virtual player is created globally and shared by all interaction bots. Ticks run one at a time on the main thread,
 * and it is moved next to the target block before each interaction, so there is no concurrency conflict.
 */
public final class VirtualPlayerManager {

    /** Virtual player display name (GameProfile names are limited to 16 ASCII characters; color codes are display-only) */
    public static final String BOT_NAME = "MagicBot";

    /** Active mode: citizens / nms / none (both options failed; use a real player instead) */
    private static String mode = "none";

    /** Cached virtual Bukkit player (NPC Player entity for Citizens; CraftPlayer for NMS) */
    private static Player virtualPlayer;

    /** Citizens NPC object (held via reflection to avoid a compile-time dependency) */
    private static Object citizensNpc;

    /** NMS fake player object (ServerPlayer held via reflection) */
    private static Object nmsFakePlayer;

    /** Whether initialization is complete (only tried once; failures use fallback mode) */
    private static boolean initialized = false;

    /** Whether the join-hide listener is registered (hide the virtual player from current and new players) */
    private static boolean joinListenerRegistered = false;

    private VirtualPlayerManager() {
    }

    /**
     * Get the virtual player; try the three modes in priority order and fall back if they fail.
     *
     * @return Virtual Player instance; returns null if all options fail (caller uses a nearby real player)
     */
    public static synchronized Player obtainVirtual() {
        if (!initialized) {
            initialized = true;
            // Option A: try Citizens NPC first (stable and version-independent)
            if (tryInitCitizens()) {
                mode = "citizens";
            } else if (tryInitNmsFakePlayer()) {
                // Option B: without Citizens, try an NMS fake player via reflection (tested on Paper 1.20.4)
                mode = "nms";
            } else {
                // Both options failed; use fallback mode and let the caller use a nearby real player
                mode = "none";
                log("Virtual player initialization failed (Citizens is not installed and NMS reflection does not fit this server); interaction bots will use a nearby real player");
            }
            if (virtualPlayer != null) {
                log("Virtual player initialized successfully; mode=" + mode + ", name=" + BOT_NAME);
                hideFromAll(); // Hide it from all online players after spawning
                registerJoinHider(); // Hide it from new players when they join
            }
        }
        return virtualPlayer;
    }

    /** Current virtual player mode (for UI and debugging) */
    public static String getMode() {
        return mode;
    }

    /** Whether the virtual player is available */
    public static boolean isAvailable() {
        return obtainVirtual() != null;
    }

    /**
     * Move the virtual player to the target location (call before each interaction so event handlers read the correct location).
     */
    public static void moveVirtual(Location anchor) {
        if (virtualPlayer != null && virtualPlayer.isOnline()) {
            try {
                // Move next to the target block without changing the view angle
                virtualPlayer.teleport(anchor);
            } catch (Throwable ignored) {
                // A failed teleport will not affect this simulated right-click
            }
        }
    }

    // ==================== Option A: Citizens NPC (reflection) ====================

    /**
     * Option A: use reflection to call the Citizens2 API and create an invisible NPC.
     * Reflection call chain: CitizensAPI.getNPCRegistry() → registry.createNPC(PLAYER, BOT_NAME)
     *             → npc.setPersistent(false) → npc.setProtected(true) → npc.spawn(loc) → npc.getEntity()
     */
    private static boolean tryInitCitizens() {
        try {
            // Prerequisite check: the Citizens plugin must be enabled
            if (Bukkit.getPluginManager().getPlugin("Citizens") == null) {
                return false;
            }
            Class<?> citizensApi = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object registry = citizensApi.getMethod("getNPCRegistry").invoke(null);

            // Create an NPC of type PLAYER with the name BOT_NAME
            Method createNpc = findMethod(registry.getClass(), "createNPC", 2);
            citizensNpc = createNpc.invoke(registry, EntityType.PLAYER, BOT_NAME);
            if (citizensNpc == null) return false;
            Class<?> npcClass = citizensNpc.getClass();

            // Do not persist it, so the NPC is not saved to Citizens files or left behind after plugin removal
            invokeIfExists(npcClass, citizensNpc, "setPersistent", boolean.class, false);
            // Protected: cannot be damaged by players or mobs
            invokeIfExists(npcClass, citizensNpc, "setProtected", boolean.class, true);

            // Spawn the NPC near the main world's spawn point (it will be moved beside the target block before each interaction)
            World world = Bukkit.getWorlds().get(0);
            Location spawnLoc = world.getSpawnLocation().add(0, 1, 0);
            Object spawned = findMethod(npcClass, "spawn", 1).invoke(citizensNpc, spawnLoc);
            if (spawned == null || !((Boolean) spawned)) return false;

            // The NPC entity is a Player instance (Citizens player NPCs implement the full Player interface)
            Object entity = findMethod(npcClass, "getEntity", 0).invoke(citizensNpc);
            if (!(entity instanceof Player player)) return false;

            // Invisible + no collision + invulnerable + does not despawn
            setInvisibleAndImmortal(player);

            virtualPlayer = player;
            return true;
        } catch (Throwable t) {
            log("Option A (Citizens NPC) initialization failed: " + t);
            return false;
        }
    }

    // ==================== Option B: NMS fake player via reflection ====================

    /**
     * Option B: use reflection to add an NMS ServerPlayer fake player to the world (tested on Paper 1.20.4).
     *
     * Reflection call chain:
     *   CraftServer.getServer() → MinecraftServer
     *   CraftWorld.getHandle() → ServerLevel
     *   new ServerPlayer(minecraftServer, serverLevel, GameProfile, ClientInformation.createDefault())
     *   fakePlayer.connection = new ServerGamePacketListenerImpl(minecraftServer, new Connection(SERVERBOUND), fakePlayer)
     *   serverLevel.addNewPlayer(fakePlayer)
     *   fakePlayer.getBukkitEntity() → CraftPlayer (the Player instance)
     *
     * ⚠ Version-sensitive: Paper 1.20.4 uses Mojang class names; this may need updates after major server upgrades.
     *   Each reflection call is guarded; any failure triggers fallback.
     */
    private static boolean tryInitNmsFakePlayer() {
        try {
            // 1) Bukkit server → NMS MinecraftServer（CraftServer.getServer()）
            Class<?> craftServerClass = getCraftClass("org.bukkit.craftbukkit.CraftServer");
            Object nmsServer = craftServerClass.getMethod("getServer").invoke(Bukkit.getServer());

            // 2) CraftWorld → NMS ServerLevel
            World world = Bukkit.getWorlds().get(0);
            Class<?> craftWorldClass = getCraftClass("org.bukkit.craftbukkit.CraftWorld");
            Object serverLevel = craftWorldClass.getMethod("getHandle").invoke(world);

            // 3) GameProfile (name limited to 16 ASCII characters)
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Object gameProfile = gameProfileClass
                    .getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), BOT_NAME);

            // 4) ClientInformation.createDefault() (required for ServerPlayer construction in 1.19.3+)
            Class<?> clientInfoClass = Class.forName("net.minecraft.server.level.ClientInformation");
            Object clientInfo = clientInfoClass.getMethod("createDefault").invoke(null);

            // 5) new ServerPlayer(minecraftServer, serverLevel, gameProfile, clientInfo)
            Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
            nmsFakePlayer = serverPlayerClass
                    .getConstructor(
                            Class.forName("net.minecraft.server.MinecraftServer"),
                            Class.forName("net.minecraft.server.level.ServerLevel"),
                            gameProfileClass,
                            clientInfoClass)
                    .newInstance(nmsServer, serverLevel, gameProfile, clientInfo);

            // 6) Create an empty network connection and attach it to the fake player (ServerGamePacketListenerImpl)
            Class<?> connectionClass = Class.forName("net.minecraft.network.Connection");
            Class<?> packetFlowClass = Class.forName("net.minecraft.network.protocol.PacketFlow");
            Object serverboundFlow = packetFlowClass.getField("SERVERBOUND").get(null);
            Object connection = connectionClass.getConstructor(packetFlowClass).newInstance(serverboundFlow);
            Class<?> listenerClass = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl");
            Object packetListener = listenerClass
                    .getConstructor(
                            Class.forName("net.minecraft.server.MinecraftServer"),
                            connectionClass,
                            serverPlayerClass)
                    .newInstance(nmsServer, connection, nmsFakePlayer);
            serverPlayerClass.getField("connection").set(nmsFakePlayer, packetListener);

            // 7) Add the fake player to the world (ServerLevel.addNewPlayer)
            serverLevel.getClass().getMethod("addNewPlayer", serverPlayerClass)
                    .invoke(serverLevel, nmsFakePlayer);

            // 8) Get the Bukkit-side Player entity (CraftPlayer)
            Object bukkitEntity = serverPlayerClass.getMethod("getBukkitEntity").invoke(nmsFakePlayer);
            if (!(bukkitEntity instanceof Player player)) return false;

            // Invisible + no collision + invulnerable
            setInvisibleAndImmortal(player);

            virtualPlayer = player;
            return true;
        } catch (Throwable t) {
            log("Option B (NMS fake player) initialization failed: " + t);
            nmsFakePlayer = null;
            return false;
        }
    }

    // ==================== Shared helper methods ====================

    /** Set invisible, no collision, invulnerable, and prevent despawning (shared by NPC and NMS fake player) */
    private static void setInvisibleAndImmortal(Player player) {
        try {
            player.setInvisible(true);          // Invisible
            player.setCollidable(false);        // No collision; does not block players
            player.setInvulnerable(true);       // Invulnerable
            player.setRemoveWhenFarAway(false); // Does not despawn when players move away
            player.setSilent(true);             // Silent
            player.setAllowFlight(true);        // Allow flight (prevents falling due to gravity after teleporting)
            player.setFlying(true);             // Keep it floating to prevent falling after teleporting
        } catch (Throwable t) {
            log("Some virtual player properties could not be set: " + t);
        }
    }

    /** Hide the virtual player from all online players */
    private static void hideFromAll() {
        if (virtualPlayer == null) return;
        for (Player online : Bukkit.getOnlinePlayers()) {
            try {
                online.hidePlayer(MagicExpansion.getInstance(), virtualPlayer);
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Register a PlayerJoinEvent listener to hide the virtual player from new players joining the server.
     * Register only once, after the virtual player is created successfully.
     */
    private static void registerJoinHider() {
        if (joinListenerRegistered) return;
        joinListenerRegistered = true;
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @org.bukkit.event.EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                if (virtualPlayer != null) {
                    try {
                        event.getPlayer().hidePlayer(MagicExpansion.getInstance(), virtualPlayer);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }, MagicExpansion.getInstance());
    }

    /** Use reflection to find a method by name and parameter count (compatible across Citizens versions) */
    private static Method findMethod(Class<?> clazz, String name, int paramCount) throws NoSuchMethodException {
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
                m.setAccessible(true);
                return m;
            }
        }
        throw new NoSuchMethodException(clazz.getSimpleName() + "." + name + "/" + paramCount);
    }

    /** Call a method via reflection if it exists; skip it if not (such as setPersistent, for version compatibility) */
    private static void invokeIfExists(Class<?> clazz, Object target, String name, Class<?> paramType, Object arg) {
        try {
            Method m = clazz.getMethod(name, paramType);
            m.setAccessible(true);
            m.invoke(target, arg);
        } catch (Throwable ignored) {
            // Method missing or call failed; skip it (only affects non-essential properties)
        }
    }

    /**
     * Get the CraftBukkit class, preferring the versionless package name (Paper 1.20.2+);
     * if that fails, try a versioned package (older Spigot/Paper, such as v1_20_R3).
     */
    private static Class<?> getCraftClass(String path) throws ClassNotFoundException {
        try {
            return Class.forName(path); // Paper 1.20.2+: versionless package
        } catch (ClassNotFoundException ignored) {
            // Older versions: insert the v1_20_R3 version segment (org.bukkit.craftbukkit.v1_20_R3.CraftServer)
            String pkg = path.substring("org.bukkit.craftbukkit.".length());
            String ver = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            return Class.forName("org.bukkit.craftbukkit." + ver + "." + pkg);
        }
    }

    /** Write a log message with the plugin prefix */
    private static void log(String message) {
        Logger logger = MagicExpansion.getInstance().getLogger();
        logger.info("[Virtual Player] " + message);
    }

    /**
     * Cleanup on plugin unload: remove the Citizens NPC (setPersistent(false) prevents saving; this just removes the entity).
     * The NMS fake player is cleaned up when the server shuts down, so no extra cleanup is needed.
     */
    public static synchronized void shutdown() {
        if (citizensNpc != null) {
            try {
                findMethod(citizensNpc.getClass(), "despawn", 0).invoke(citizensNpc);
            } catch (Throwable ignored) {
            }
        }
        virtualPlayer = null;
        citizensNpc = null;
        nmsFakePlayer = null;
        initialized = false;
    }
}

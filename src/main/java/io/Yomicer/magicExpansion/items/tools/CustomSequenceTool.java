package io.Yomicer.magicExpansion.items.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.Yomicer.magicExpansion.MagicExpansion;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CustomSequenceTool extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable, Listener {

    public CustomSequenceTool(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        // Register event listeners
        MagicExpansion.getInstance().getServer().getPluginManager().registerEvents(this, MagicExpansion.getInstance());
    }

    // Keys used to store VoidTouch coordinates
    private static final NamespacedKey KEY_X = new NamespacedKey(MagicExpansion.getInstance(), "touch_x");
    private static final NamespacedKey KEY_Y = new NamespacedKey(MagicExpansion.getInstance(), "touch_y");
    private static final NamespacedKey KEY_Z = new NamespacedKey(MagicExpansion.getInstance(), "touch_z");
    private static final NamespacedKey KEY_WORLD = new NamespacedKey(MagicExpansion.getInstance(), "touch_world");

    // Key used to store sequence data
    private static final NamespacedKey KEY_SLOT_DATA = new NamespacedKey(MagicExpansion.getInstance(), "slot_data");

    // Add this field to the class
    private static final NamespacedKey KEY_SCRIPT = new NamespacedKey(MagicExpansion.getInstance(), "has_script");

    // Gson instance for JSON serialization
    private static final Gson gson = new GsonBuilder().create();

    // Store running sequence tasks
    // Fix: use a static ConcurrentHashMap (this item is registered as a singleton) so static cleanup can clear it
    private static final Map<UUID, SequenceTask> runningTasks = new ConcurrentHashMap<>();

    // Store players waiting for input
    // Fix: use a static ConcurrentHashMap so static cleanup can clear it
    private static final Map<UUID, InputRequest> waitingForInput = new ConcurrentHashMap<>();

    /**
     * Fix: Clear this player's session data when they leave:
     * Cancel running sequence tasks and remove pending input records to avoid memory leaks and tasks running for offline players
     */
    public static void cleanup(UUID uuid) {
        SequenceTask task = runningTasks.remove(uuid);
        if (task != null) {
            task.cancel(); // Cancel this player's running sequence task
        }
        waitingForInput.remove(uuid);
    }

    @Override
    public @NotNull ItemUseHandler getItemHandler() {
        return e -> {
            // Stop the default behavior
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);
            Player player = e.getPlayer();

            if (e.getHand() != EquipmentSlot.HAND) {
                return;
            }

            // Shift + right-click to open the saved settings menu
            if (player.isSneaking() && (e.getInteractEvent().getAction() == Action.RIGHT_CLICK_AIR || e.getInteractEvent().getAction() == Action.RIGHT_CLICK_BLOCK)) {
                openSequenceMenu(player, e.getItem());
            }
            // Right-click to run the sequence
            else if (!player.isSneaking() && (e.getInteractEvent().getAction() == Action.RIGHT_CLICK_AIR || e.getInteractEvent().getAction() == Action.RIGHT_CLICK_BLOCK)) {
                startSequence(player, e.getItem());
            }
        };
    }

    private void openSequenceMenu(Player player, ItemStack item) {
        ChestMenu menu = new ChestMenu("Sequence Setup Menu");

        // Allow clicks in the player's inventory
        menu.setPlayerInventoryClickable(true);

        PlayerSequenceData playerData = getPlayerData(item);

        // Add 54 barrier items to show step info
        for (int i = 0; i < 54; i++) {
            final int slot = i;
            SlotData slotData = playerData.getSlotData(slot);

            ItemStack barrierItem = createBarrierItem(slot, slotData);
            menu.addItem(i, barrierItem);

            menu.addMenuClickHandler(i, (p, slot1, item1, action) -> {
                handleMenuClick(p, slot, action, playerData, item);
                return false;
            });
        }

        menu.open(player);
    }

    private ItemStack createBarrierItem(int slot, SlotData slotData) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Step " + (slot + 1));
        lore.add("");
        // Check whether this slot has a script
        boolean hasScript = checkIfSlotHasScript(slotData);

        // Show the current settings
        if (slotData.leftClickLocation != null) {
            lore.add("§aLeft-click location: " + formatLocation(slotData.leftClickLocation));
        } else {
            lore.add("§cLeft-click location: not set");
        }

        if (slotData.rightClickLocation != null) {
            lore.add("§aRight-click location: " + formatLocation(slotData.rightClickLocation));
        } else {
            lore.add("§cRight-click location: not set");
        }

        if (slotData.teleportLocation != null) {
            lore.add("§aTeleport location: " + formatLocation(slotData.teleportLocation));
        } else {
            lore.add("§cTeleport location: not set");
        }

        if (slotData.interval > 0) {
            lore.add("§bInterval: " + String.format("%.2f", slotData.interval) + "sec");
        } else {
            lore.add("§cInterval: not set");
        }

        if (slotData.message != null && !slotData.message.isEmpty()) {
            int maxLength = 50;
            String displayMessage = slotData.message.length() > maxLength ?
                    slotData.message.substring(0, maxLength) + "..." : slotData.message;

            if (slotData.message.startsWith("/")) {
                lore.add("§dCommand: " + displayMessage);
            } else {
                lore.add("§dMessage: " + displayMessage);
            }
        } else {
            lore.add("§cMessage/command: Not set");
        }

        lore.add("");
        lore.add("§eWhile holding Void Touch:");
        lore.add("§e- Left-click: set the left-click location");
        lore.add("§e- Right-click: set the right-click location");
        lore.add("§e- Shift + left-click: set the teleport location");
        lore.add("§6When your hand is empty:");
        lore.add("§6- Left-click: set the interval");
        lore.add("§6- Right-click: set a message/command");
        lore.add("§6- Shift + left-click: reset this slot's script");
        lore.add("");
        lore.add("§aRight-click the item to start the sequence");

        // Pick a different material depending on whether this slot has a script
        Material itemMaterial = hasScript ? Material.NETHER_STAR : Material.BARRIER;
        String itemName = hasScript ? "§6§lStep " + (slot + 1) + " §e(Script)" : "§6Step " + (slot + 1);

        return new CustomItemStack(itemMaterial, itemName, lore.toArray(new String[0]));
    }

    // Add a method to check for a script
    private boolean checkIfSlotHasScript(SlotData slotData) {
        // Check whether anything is set (location, message, or interval)
        return slotData.leftClickLocation != null ||
                slotData.rightClickLocation != null ||
                slotData.teleportLocation != null ||
                slotData.interval > 0 ||
                (slotData.message != null && !slotData.message.isEmpty());
    }

    private void handleMenuClick(Player player, int slot, ClickAction action, PlayerSequenceData playerData, ItemStack toolItem) {
        ItemStack cursorItem = player.getItemOnCursor();

        // Check whether the cursor holds a VoidTouch item
        boolean hasVoidTouch = isVoidTouchItem(cursorItem);

        if (hasVoidTouch) {
            // Get the location from the VoidTouch item
            Location voidTouchLocation = getVoidTouchLocation(cursorItem);

            if (voidTouchLocation == null) {
                player.sendMessage("§cVoid Touch has no saved coordinates! Use Shift + right-click on a block to link a location first.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            if (action.isShiftClicked()) {
                if (!action.isRightClicked()) {
                    // Shift+left-clickSetteleportcoordinates
                    playerData.setTeleportLocation(slot, voidTouchLocation);
                    player.sendMessage("§aSet up step " + (slot + 1) + "  teleport location: " + formatLocation(voidTouchLocation));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.5f);
                }
            } else {
                if (!action.isRightClicked()) {
                    // left-clickSetleft-clickcoordinates
                    playerData.setLeftClickLocation(slot, voidTouchLocation);
                    player.sendMessage("§aSet up step " + (slot + 1) + "  left-click location: " + formatLocation(voidTouchLocation));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.5f);
                } else if (action.isRightClicked()) {
                    // right-clickSetright-clickcoordinates
                    playerData.setRightClickLocation(slot, voidTouchLocation);
                    player.sendMessage("§aSet up step " + (slot + 1) + "  right-click location: " + formatLocation(voidTouchLocation));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.5f);
                }
            }
        } else {
            // Handle it when there's no item
            if (action.isShiftClicked() && !action.isRightClicked()) {
                // Empty-hand Shift + left-click resets the slot
                resetSlot(player, slot, playerData, toolItem);
            } else {
                // Handle it when there's no item
                if (!action.isRightClicked()) {
                    // Left-click to set the interval
                    setIntervalTime(player, slot, playerData, toolItem);
                } else if (action.isRightClicked()) {
                    // Right-click to set a message/command
                    setMessageOrCommand(player, slot, playerData, toolItem);
                }
            }
        }

        // Save the data and refresh the menu
        savePlayerData(playerData, toolItem);
        openSequenceMenu(player, toolItem);
    }

    // Reset this slot
    private void resetSlot(Player player, int slot, PlayerSequenceData playerData, ItemStack toolItem) {
        SlotData slotData = playerData.getSlotData(slot);

        // Reset all data
        slotData.leftClickLocation = null;
        slotData.rightClickLocation = null;
        slotData.teleportLocation = null;
        slotData.interval = 0;
        slotData.message = "";

        savePlayerData(playerData, toolItem);
        player.sendMessage("§aReset all settings for step " + (slot + 1) + "  ");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.5f);
    }

    private void startSequence(Player player, ItemStack toolItem) {
        UUID playerId = player.getUniqueId();

        // Stop the sequence if it's already running
        if (runningTasks.containsKey(playerId)) {
            stopSequence(player);
            return;
        }

        PlayerSequenceData playerData = getPlayerData(toolItem);

        // Check whether any steps are set
        boolean hasSteps = false;
        for (int i = 0; i < 54; i++) {
            SlotData slotData = playerData.getSlotData(i);
            if (slotData.hasAnyAction()) {
                hasSteps = true;
                break;
            }
        }

        if (!hasSteps) {
            player.sendMessage("§cNo sequence steps are set up yet! Use Shift + right-click to set them up first.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Create and start the sequence task
        SequenceTask task = new SequenceTask(player, toolItem, playerData);
        runningTasks.put(playerId, task);
        task.runTaskTimer(MagicExpansion.getInstance(), 0L, 1L); // Check once every tick

        player.sendMessage("§aSequence started! Right-click again to stop it.");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
    }

    private void stopSequence(Player player) {
        UUID playerId = player.getUniqueId();
        if (runningTasks.containsKey(playerId)) {
            runningTasks.get(playerId).cancel();
            runningTasks.remove(playerId);
            player.sendMessage("§cSequence stopped.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
        }
    }

    // Set the interval - full implementation
    private void setIntervalTime(Player player, int slot, PlayerSequenceData playerData, ItemStack toolItem) {

        new BukkitRunnable() {
            @Override
            public void run() {
                player.closeInventory();
                player.sendMessage("§6Enter the interval in chat (seconds, minimum 0.01):");
                player.sendMessage("§cType 'cancel' to cancel");
                // Track the pending input
                waitingForInput.put(player.getUniqueId(), new InputRequest(InputType.INTERVAL, slot, playerData, toolItem));
            }
        }.runTaskLater(MagicExpansion.getInstance(), 1L);

    }

    // Set a message/command - full implementation
    private void setMessageOrCommand(Player player, int slot, PlayerSequenceData playerData, ItemStack toolItem) {
        // Wait one tick before closing the menu so the click event finishes first
        new BukkitRunnable() {
            @Override
            public void run() {
                player.closeInventory();
                player.sendMessage("§6=== Sequence Tool Settings ===");
                player.sendMessage("§6Enter a message or command in chat:");
                player.sendMessage("§7- Regular text: send a chat message when the sequence runs");
                player.sendMessage("§7- Command starting with /: run the command when the sequence runs");
                player.sendMessage("§cType 'cancel' to cancel");
                player.sendMessage("§eNote: You can type a command (like /help). It'll be saved, not run right away");

                // Track the pending input
                waitingForInput.put(player.getUniqueId(), new InputRequest(InputType.MESSAGE, slot, playerData, toolItem));
            }
        }.runTaskLater(MagicExpansion.getInstance(), 1L);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // If the player is waiting for input, stop command execution and handle the input
        if (waitingForInput.containsKey(playerId)) {
            event.setCancelled(true);

            InputRequest request = waitingForInput.get(playerId);

            // Only handle MESSAGE-type input requests
            if (request.type != InputType.MESSAGE) {
                player.sendMessage("§cFinish the current setup first! Type 'cancel' to cancel");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            String command = event.getMessage(); // This includes the "/" prefix

            // Save the command to the sequence tool
            request.playerData.setMessage(request.slot, command);
            savePlayerData(request.playerData, request.toolItem);

            player.sendMessage("§aSet up step " + (request.slot + 1) + "  command: " + command);
            player.sendMessage("§7Note: This command will run when the sequence runs");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.5f);

            waitingForInput.remove(playerId);

            // Reopen the menu
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        openSequenceMenu(player, request.toolItem);
                    }
                }
            }.runTask(MagicExpansion.getInstance());
        }
    }

    // Handle player chat input
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!waitingForInput.containsKey(playerId)) {
            return;
        }

        // If the message starts with "/", it's a command, so leave it to the command preprocessor
        if (event.getMessage().startsWith("/")) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();

        InputRequest request = waitingForInput.get(playerId);

        // Handle canceling
        if (message.equalsIgnoreCase("cancel")) {
            waitingForInput.remove(playerId);
            player.sendMessage("§cSetup canceled.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);

            // Reopen the menu after canceling
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        openSequenceMenu(player, request.toolItem);
                    }
                }
            }.runTask(MagicExpansion.getInstance());
            return;
        }

        // Handle the input based on its type
        switch (request.type) {
            case INTERVAL:
                try {
                    double interval = Double.parseDouble(message);
                    if (interval < 0.01) {
                        player.sendMessage("§cThe interval must be at least 0.01 seconds!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                    }

                    request.playerData.setInterval(request.slot, interval);
                    savePlayerData(request.playerData, request.toolItem);
                    player.sendMessage("§aSet up step " + (request.slot + 1) + "  interval to " + String.format("%.2f", interval) + " sec");
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.5f);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cPlease enter a valid number!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                break;

            case MESSAGE:
                // Handle regular text (not a command)
                request.playerData.setMessage(request.slot, message);
                savePlayerData(request.playerData, request.toolItem);

                player.sendMessage("§aSet up step " + (request.slot + 1) + "  message: " + message);
                player.sendMessage("§7Note: This message will be sent when the sequence runs");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.5f);
                break;
        }

        waitingForInput.remove(playerId);

        // Reopen the menu
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    openSequenceMenu(player, request.toolItem);
                }
            }
        }.runTask(MagicExpansion.getInstance());
    }

    private class SequenceTask extends BukkitRunnable {
        private final Player player;
        private final ItemStack toolItem;
        private final PlayerSequenceData playerData;
        private int currentStep = 0;
        private int delayTicks = 0;
        private boolean isRunningStep = false;
        private final List<Integer> activeSteps = new ArrayList<>();

        public SequenceTask(Player player, ItemStack toolItem, PlayerSequenceData playerData) {
            this.player = player;
            this.toolItem = toolItem;
            this.playerData = playerData;

            // Pre-calculate which steps are set up
            for (int i = 0; i < 54; i++) {
                if (playerData.getSlotData(i).hasAnyAction()) {
                    activeSteps.add(i);
                }
            }
        }

        @Override
        public void run() {
            // Check whether the player still holds this item (main hand only)
            if (!isPlayerHoldingTool()) {
                stopSequence(player);
                return;
            }

            // If a step is running, wait for it to finish
            if (isRunningStep) {
                return;
            }

            // Delay counter
            if (delayTicks > 0) {
                delayTicks--;
                return;
            }

            // Stop if there are no active steps
            if (activeSteps.isEmpty()) {
                stopSequence(player);
                return;
            }

            // Run the current step
            executeStep(activeSteps.get(currentStep));

            // Move to the next step
            currentStep = (currentStep + 1) % activeSteps.size();
        }

        private boolean isPlayerHoldingTool() {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            return isSameTool(mainHand);
        }

        private boolean isSameTool(ItemStack item) {
            if (item == null || item.getType() == Material.AIR) {
                return false;
            }

            // Compare the item directly instead of checking its SlimefunItem type
            if (item.isSimilar(toolItem)) {
                return true;
            }

            SlimefunItem sfItem = SlimefunItem.getByItem(item);
            return sfItem != null && sfItem instanceof CustomSequenceTool;
        }

        private void executeStep(int step) {
            SlotData slotData = playerData.getSlotData(step);

            if (!slotData.hasAnyAction()) {
                return; // Skip steps that aren't set up
            }

            isRunningStep = true;

            try {
                // Perform the left-click action
                if (slotData.leftClickLocation != null) {
                    simulateLeftClick(slotData.leftClickLocation);
                }

                // Perform the right-click action
                if (slotData.rightClickLocation != null) {
                    simulateRightClick(slotData.rightClickLocation);
                }

                // Perform the teleport action
                if (slotData.teleportLocation != null) {
                    player.teleport(slotData.teleportLocation);
                    player.sendMessage("§eTeleporting to the coordinates for step " + (step + 1));
                }

                // Run the message/command
                if (slotData.message != null && !slotData.message.isEmpty()) {
                    if (slotData.message.startsWith("/")) {
                        // Run the command
                        player.performCommand(slotData.message.substring(1));
                    } else {
                        player.chat(slotData.message);
                    }
                }

                player.sendMessage("§aRunning step " + (step + 1));

            } catch (Exception e) {
                player.sendMessage("§cAn error occurred while running step " + (step + 1) + ": " + e.getMessage());
            } finally {
                // After the step, check if the player still has the item
                if (!isPlayerHoldingTool()) {
                    stopSequence(player);
                    return;
                }
            }

            // Set the delay (convert to ticks; 1 second = 20 ticks)
            double intervalSeconds = slotData.interval > 0 ? slotData.interval : 0.01;
            delayTicks = (int) Math.max(intervalSeconds * 20, 1); // At least a 1-tick delay
            isRunningStep = false;
        }

        private void simulateLeftClick(Location location) {
            Block block = location.getBlock();
            PlayerInteractEvent event = new PlayerInteractEvent(
                    player,
                    Action.LEFT_CLICK_BLOCK,
                    new ItemStack(Material.AIR),
                    block,
                    BlockFace.SELF
            );
            Bukkit.getPluginManager().callEvent(event);
        }

        private void simulateRightClick(Location location) {
            Block block = location.getBlock();
            PlayerInteractEvent event = new PlayerInteractEvent(
                    player,
                    Action.RIGHT_CLICK_BLOCK,
                    new ItemStack(Material.AIR),
                    block,
                    BlockFace.SELF
            );
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    private boolean isVoidTouchItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        SlimefunItem sfItem = SlimefunItem.getByItem(item);
        return sfItem != null && sfItem instanceof VoidTouchScript;
    }

    private Location getVoidTouchLocation(ItemStack voidTouchItem) {
        ItemMeta meta = voidTouchItem.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (container.has(KEY_X, PersistentDataType.INTEGER) &&
                container.has(KEY_Y, PersistentDataType.INTEGER) &&
                container.has(KEY_Z, PersistentDataType.INTEGER) &&
                container.has(KEY_WORLD, PersistentDataType.STRING)) {

            String worldName = container.get(KEY_WORLD, PersistentDataType.STRING);
            World world = Bukkit.getWorld(worldName);

            if (world != null) {
                int x = container.get(KEY_X, PersistentDataType.INTEGER);
                int y = container.get(KEY_Y, PersistentDataType.INTEGER);
                int z = container.get(KEY_Z, PersistentDataType.INTEGER);
                return new Location(world, x, y, z);
            }
        }

        return null;
    }

    private String formatLocation(Location loc) {
        if (loc == null) return "null";
        return String.format("X:%d Y:%d Z:%d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    // Data management methods - fixed data storage issues
    private PlayerSequenceData getPlayerData(ItemStack item) {
        PlayerSequenceData data = new PlayerSequenceData();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            if (container.has(KEY_SLOT_DATA, PersistentDataType.STRING)) {
                String jsonData = container.get(KEY_SLOT_DATA, PersistentDataType.STRING);
                try {
                    // Use Gson to deserialize JSON data
                    Type type = new TypeToken<Map<Integer, SerializableSlotData>>(){}.getType();
                    Map<Integer, SerializableSlotData> serializedData = gson.fromJson(jsonData, type);

                    if (serializedData != null) {
                        for (Map.Entry<Integer, SerializableSlotData> entry : serializedData.entrySet()) {
                            int slot = entry.getKey();
                            SerializableSlotData serializedSlot = entry.getValue();
                            SlotData slotData = data.getSlotData(slot);

                            // Deserialize location data
                            if (serializedSlot.leftClickLocation != null) {
                                slotData.leftClickLocation = deserializeLocation(serializedSlot.leftClickLocation);
                            }
                            if (serializedSlot.rightClickLocation != null) {
                                slotData.rightClickLocation = deserializeLocation(serializedSlot.rightClickLocation);
                            }
                            if (serializedSlot.teleportLocation != null) {
                                slotData.teleportLocation = deserializeLocation(serializedSlot.teleportLocation);
                            }

                            // Deserialize other data
                            slotData.interval = serializedSlot.interval;
                            slotData.message = serializedSlot.message;
                        }
                    }
                } catch (Exception e) {
                    MagicExpansion.getInstance().getLogger().warning("Failed to deserialize item data: " + e.getMessage());
                }
            }
        }
        return data;
    }

    private void savePlayerData(PlayerSequenceData data, ItemStack item) {
        // Save to the item's NBT
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            try {
                // Create a serializable data structure
                Map<Integer, SerializableSlotData> serializedData = new HashMap<>();

                for (int i = 0; i < 54; i++) {
                    SlotData slotData = data.getSlotData(i);
                    SerializableSlotData serializedSlot = new SerializableSlotData();

                    // Serialize location data
                    if (slotData.leftClickLocation != null) {
                        serializedSlot.leftClickLocation = serializeLocation(slotData.leftClickLocation);
                    }
                    if (slotData.rightClickLocation != null) {
                        serializedSlot.rightClickLocation = serializeLocation(slotData.rightClickLocation);
                    }
                    if (slotData.teleportLocation != null) {
                        serializedSlot.teleportLocation = serializeLocation(slotData.teleportLocation);
                    }

                    // Serialize other data
                    serializedSlot.interval = slotData.interval;
                    serializedSlot.message = slotData.message;

                    serializedData.put(i, serializedSlot);
                }

                // Use Gson to serialize data into a JSON string
                String jsonData = gson.toJson(serializedData);
                container.set(KEY_SLOT_DATA, PersistentDataType.STRING, jsonData);
                item.setItemMeta(meta);
            } catch (Exception e) {
                MagicExpansion.getInstance().getLogger().warning("Failed to serialize item data: " + e.getMessage());
            }
        }
    }

    // Location serialization method
    private String serializeLocation(Location location) {
        if (location == null) return null;
        return location.getWorld().getName() + ":" + location.getX() + ":" + location.getY() + ":" + location.getZ() + ":" + location.getYaw() + ":" + location.getPitch();
    }

    // Location deserialization method
    private Location deserializeLocation(String locationString) {
        if (locationString == null || locationString.isEmpty()) return null;
        try {
            String[] parts = locationString.split(":");
            if (parts.length == 6) {
                World world = Bukkit.getWorld(parts[0]);
                if (world != null) {
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);
                    float yaw = Float.parseFloat(parts[4]);
                    float pitch = Float.parseFloat(parts[5]);
                    return new Location(world, x, y, z, yaw, pitch);
                }
            }
        } catch (Exception e) {
            MagicExpansion.getInstance().getLogger().warning("Failed to deserialize location: " + locationString);
        }
        return null;
    }

    // Data storage class
    private static class PlayerSequenceData {
        private final SlotData[] slots = new SlotData[54];

        public PlayerSequenceData() {
            for (int i = 0; i < 54; i++) {
                slots[i] = new SlotData();
            }
        }

        public SlotData getSlotData(int slot) {
            if (slot >= 0 && slot < 54) {
                return slots[slot];
            }
            return new SlotData();
        }

        public void setLeftClickLocation(int slot, Location location) {
            if (slot >= 0 && slot < 54) {
                slots[slot].leftClickLocation = location.clone();
            }
        }

        public void setRightClickLocation(int slot, Location location) {
            if (slot >= 0 && slot < 54) {
                slots[slot].rightClickLocation = location.clone();
            }
        }

        public void setTeleportLocation(int slot, Location location) {
            if (slot >= 0 && slot < 54) {
                slots[slot].teleportLocation = location.clone();
            }
        }

        public void setInterval(int slot, double seconds) {
            if (slot >= 0 && slot < 54) {
                slots[slot].interval = seconds;
            }
        }

        public void setMessage(int slot, String message) {
            if (slot >= 0 && slot < 54) {
                slots[slot].message = message;
            }
        }
    }

    private static class SlotData {
        public Location leftClickLocation;
        public Location rightClickLocation;
        public Location teleportLocation;
        public double interval = 0;
        public String message = "";

        public boolean hasAnyAction() {
            return leftClickLocation != null ||
                    rightClickLocation != null ||
                    teleportLocation != null ||
                    (message != null && !message.isEmpty());
        }
    }

    // Serializable data class
    private static class SerializableSlotData {
        public String leftClickLocation;
        public String rightClickLocation;
        public String teleportLocation;
        public double interval = 0;
        public String message = "";
    }

    // Input request type
    private enum InputType {
        INTERVAL, MESSAGE
    }

    // Input request class
    private static class InputRequest {
        public final InputType type;
        public final int slot;
        public final PlayerSequenceData playerData;
        public final ItemStack toolItem;

        public InputRequest(InputType type, int slot, PlayerSequenceData playerData, ItemStack toolItem) {
            this.type = type;
            this.slot = slot;
            this.playerData = playerData;
            this.toolItem = toolItem;
        }
    }
}
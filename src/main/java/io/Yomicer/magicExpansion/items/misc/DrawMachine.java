package io.Yomicer.magicExpansion.items.misc;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.Yomicer.magicExpansion.MagicExpansion;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSpawnReason;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.libraries.dough.chat.ChatInput;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ClickAction;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap; // C1: Needed for the template deserialization cache

import static io.Yomicer.magicExpansion.utils.Utils.doGlow;

public class DrawMachine extends SlimefunItem implements EnergyNetComponent {

    private static final int TEMPLATE_SLOT = 10;
    private static final int SETTINGS_SLOT = 37;
    private static final int INFO_SLOT = 1;
    private static final int OUTPUT_SLOT = 19;

    // Floating display storage - changed to store item entities and armor stands
    private final Map<Location, Entity> itemDisplayMap = new HashMap<>();
    private final Map<Location, ArmorStand> textDisplayMap = new HashMap<>();
    // Ticket projection drop-style animation task(Managed by machine location)
    private static final Map<Location, BukkitTask> HOLOGRAM_ANIMATIONS = new HashMap<>();

    // Reward slot range
    private static final int[] REWARD_SLOTS = {
            3, 4, 5, 6, 7, 8,
            12, 13, 14, 15, 16, 17,
            21, 22, 23, 24, 25, 26,
            30, 31, 32, 33, 34, 35,
            39, 40, 41, 42, 43, 44,
            48, 49, 50, 51, 52, 53
    };

    private final int[] pinkBorder = {0, 1, 2, 9, 11, 18, 20};
    private final int[] blueBorder = {27, 28, 29, 36, 38, 45, 46, 47}; // Changed to light blue glass pane

    // Auto output state storage
    private final Map<Location, Boolean> autoOutputMap = new HashMap<>();


    public DrawMachine(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);

        constructMenu("Draw Machine");
        addItemHandler(onBlockPlace(), onBlockBreak());
    }

    @Override
    public void preRegister() {
        addItemHandler(new me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker() {
            @Override
            public void tick(Block b, SlimefunItem sf, SlimefunBlockData data) {
                DrawMachine.this.tick(b);
            }

            @Override
            public boolean isSynchronized() {
                return true; // Changed to sync to avoid async problems
            }
        });
    }

    @Nonnull
    private BlockPlaceHandler onBlockPlace() {
        return new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                // Set up the auto output state
                autoOutputMap.put(e.getBlock().getLocation(), false);
                // Create the initial floating display
                createHologram(e.getBlock());
            }
        };
    }

    // Also remove the floating display when the machine is broken - changed BlockBreakHandler
    @Nonnull
    protected BlockBreakHandler onBlockBreak() {
        return new SimpleBlockBreakHandler() {
            public void onBlockBreak(Block b) {
                BlockMenu menu = StorageCacheUtils.getMenu(b.getLocation());
                if (menu != null) {
                    // Drop all items when broken
                    for (int slot : REWARD_SLOTS) {
                        ItemStack item = menu.getItemInSlot(slot);
                        if (item != null && item.getType() != Material.AIR) {
                            b.getWorld().dropItemNaturally(b.getLocation(), item);
                        }
                    }

                    // Drop the template item, the amount is the stored amount - changed to batch dropping
                    int storedAmount = getStoredTemplateAmount(b.getLocation());
                    if (storedAmount > 0) {
                        ItemStack originalItem = getStoredTemplateItemData(b);
                        if (originalItem != null) {
                            // Use the batch drop method
                            dropTemplateItemsInBatches(b.getLocation(), originalItem, storedAmount);
                        }
                    }

                    ItemStack outputItem = menu.getItemInSlot(OUTPUT_SLOT);
                    if (outputItem != null && outputItem.getType() != Material.AIR) {
                        b.getWorld().dropItemNaturally(b.getLocation(), outputItem);
                    }
                }
                // Clean up the auto output state and stored data
                autoOutputMap.remove(b.getLocation());
                // C1/C2 Fix: when the machine is broken, also clean up the template cache and scan throttle records so nothing is left behind
                TEMPLATE_CACHE.remove(b.getLocation());
                lastDrawScanMap.remove(b.getLocation());
                BlockStorage.addBlockInfo(b, "templateAmount", "0");
                BlockStorage.addBlockInfo(b, "templateItemData", ""); // Clear item data

                // Remove floating display
                removeHologram(b);
            }
        };
    }

    // Method to drop template items in batches
    private void dropTemplateItemsInBatches(Location location, ItemStack templateItem, int totalAmount) {
        int maxDropSize = 64; // Max drop amount at once

        while (totalAmount > 0) {
            // Create a new item copy and set the amount for the current batch
            ItemStack dropItem = templateItem.clone();
            int currentBatchSize = Math.min(totalAmount, maxDropSize);
            dropItem.setAmount(currentBatchSize);

            // Drop the items of the current batch
            location.getWorld().dropItemNaturally(location, dropItem);

            // Update remaining amount
            totalAmount -= currentBatchSize;
        }
    }

    protected void tick(Block block) {
        BlockMenu menu = StorageCacheUtils.getMenu(block.getLocation());
        if (menu == null) return;

        updateMenu(menu, block);

        // Handle auto output
        if (autoOutputMap.getOrDefault(block.getLocation(), false)) {
            handleAutoOutput(menu, block);
        }

        // Handle the draw logic
        processDraw(block, menu);

        // Update floating display
        updateHologram(block);
    }

    public static final String HOLOGRAM_PREFIX = "§dDraw Machine §3Show - §e";
    // Create floating display - changed to create an item entity and text display
    private void createHologram(Block block) {

        removeHologram(block);

        Location baseLoc = block.getLocation().clone().add(0.5, 1.2, 0.5);
        Location itemLoc = baseLoc.clone().add(0, 1.6, 0);   // Item in +2.4
        Location line1Loc = baseLoc.clone().add(0, 0.7, 0);  // First line +1.9
        Location line2Loc = baseLoc.clone().add(0, 0.4, 0);  // Second line +1.6
        Location line3Loc = baseLoc.clone().add(0, 0.1, 0);  // Third line +1.3

        // === Create item floating display(Use ItemDisplay to show the entity, Won't be picked up as a dropped item by cleanup plugins) ===
        ItemStack placeholder = new CustomItemStack(Material.BARRIER, HOLOGRAM_PREFIX + "ITEM");
        ItemDisplay itemEntity = itemLoc.getWorld().spawn(itemLoc, ItemDisplay.class);
        itemEntity.setItemStack(placeholder);
        // Scaled to dropped item size(About 0.25 blocks), Keep the look of the earlier dropped items
        itemEntity.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                new Vector3f(0.25f, 0.25f, 0.25f),
                new Quaternionf()
        ));
        itemEntity.setInvulnerable(true);
        itemEntity.setPersistent(true);
        itemEntity.setMetadata("draw-machine-hologram", new org.bukkit.metadata.FixedMetadataValue(MagicExpansion.getInstance(), true));
        itemDisplayMap.put(block.getLocation(), itemEntity);
        BlockStorage.addBlockInfo(block, "hologram_item", itemEntity.getUniqueId().toString());
        startHologramAnimation(block.getLocation(), itemEntity);

//        // Create item display entity
//        Item displayItem = block.getWorld().dropItem(itemLoc, new ItemStack(Material.BARRIER));
//        displayItem.setPickupDelay(Integer.MAX_VALUE);
//        displayItem.setInvulnerable(true);
//        displayItem.setGravity(false);
//        displayItem.setVelocity(new Vector(0, 0, 0));
//        displayItem.setMetadata("draw-machine-hologram", new org.bukkit.metadata.FixedMetadataValue(MagicExpansion.getInstance(), true));

        // Create armor stands that show three lines of text
        ArmorStand line1 = createTextArmorStand(line1Loc, "§cNo template set");
        ArmorStand line2 = createTextArmorStand(line2Loc, "§7Please set the draw currency first");
        ArmorStand line3 = createTextArmorStand(line3Loc, "§7Prize pool left: 0 pcs");

//        itemDisplayMap.put(block.getLocation(), displayItem);
        textDisplayMap.put(block.getLocation(), line1); // Main text storage

        // Store the extra text lines
//        BlockStorage.addBlockInfo(block, "hologram_line", displayItem.getUniqueId().toString());
        BlockStorage.addBlockInfo(block, "hologram_line1", line1.getUniqueId().toString());
        BlockStorage.addBlockInfo(block, "hologram_line2", line2.getUniqueId().toString());
        BlockStorage.addBlockInfo(block, "hologram_line3", line3.getUniqueId().toString());
    }

    /** Drop-style animation: Slow rotation + Float up and down slightly, Still using ItemDisplay so cleanup plugins don't pick it up by mistake */
    private void startHologramAnimation(Location location, ItemDisplay display) {
        cancelHologramAnimation(location);
        final float[] angle = {0f};
        final int[] tick = {0};
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (display.isDead() || !display.isValid()) {
                    cancel();
                    HOLOGRAM_ANIMATIONS.remove(location);
                    return;
                }
                tick[0]++;
                angle[0] += 0.12f; // Slow rotation, About 7°/tick
                float bob = (float) Math.sin(tick[0] * 0.08) * 0.06f; // Float up and down slightly
                display.setTransformation(new Transformation(
                        new Vector3f(0, bob, 0),
                        new Quaternionf().rotateY(angle[0]),
                        new Vector3f(0.25f, 0.25f, 0.25f),
                        new Quaternionf()
                ));
            }
        }.runTaskTimer(MagicExpansion.getInstance(), 0L, 1L);
        HOLOGRAM_ANIMATIONS.put(location, task);
    }

    private static void cancelHologramAnimation(Location location) {
        BukkitTask task = HOLOGRAM_ANIMATIONS.remove(location);
        if (task != null) {
            task.cancel();
        }
    }
    // Helper method to create a text armor stand
    private ArmorStand createTextArmorStand(Location loc, String text) {
        ArmorStand armorStand = loc.getWorld().spawn(loc, ArmorStand.class);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setInvulnerable(true);
        armorStand.setPersistent(true);
        armorStand.setCustomNameVisible(true);
        armorStand.setCustomName(text);
        armorStand.setAI(false);
        armorStand.setCollidable(false);
        armorStand.setMarker(true);
        return armorStand;
    }

    private void updateHologram(Block block) {
        Entity itemEntity = itemDisplayMap.get(block.getLocation());
//        ArmorStand textEntity = textDisplayMap.get(block.getLocation());

        // Get the armor stands of the other lines
        ArmorStand line1 = getHologramLine(block, "hologram_line1");
        ArmorStand line2 = getHologramLine(block, "hologram_line2");
        ArmorStand line3 = getHologramLine(block, "hologram_line3");

        if (itemEntity == null || itemEntity.isDead() ||
                line1 == null || line1.isDead() ||
                line2 == null || line2.isDead() ||
                line3 == null || line3.isDead()) {
            createHologram(block);
            return;
        }

//        if (itemEntity == null || textEntity == null || itemEntity.isDead() || textEntity.isDead()) {
//            createHologram(block);
//            itemEntity = itemDisplayMap.get(block.getLocation());
//            textEntity = textDisplayMap.get(block.getLocation());
//            line2 = getHologramLine(block, "hologram_line2");
//            line3 = getHologramLine(block, "hologram_line3");
//            if (itemEntity == null || textEntity == null) return;
//        }

        ItemStack originalTemplateItem = getStoredTemplateItemData(block);
        int storedAmount = getStoredTemplateAmount(block.getLocation());

        // Get the needed amount
        int requiredAmount = 1;
        try {
            String reqStr = BlockStorage.getLocationInfo(block.getLocation(), "requiredAmount");
            if (reqStr != null) requiredAmount = Integer.parseInt(reqStr);
        } catch (NumberFormatException ignored) {
            requiredAmount = 1;
        }

        // Work out the amount of prizes left
        int totalRewards = 0;
        BlockMenu menu = StorageCacheUtils.getMenu(block.getLocation());
        if (menu != null) {
            for (int slot : REWARD_SLOTS) {
                ItemStack reward = menu.getItemInSlot(slot);
                if (reward != null && reward.getType() != Material.AIR) {
                    totalRewards += reward.getAmount();
                }
            }
        }

        if (originalTemplateItem != null) {
            String itemName = ItemStackHelper.getDisplayName(originalTemplateItem);

            // Update item display - show the amount needed
            if (itemEntity instanceof ItemDisplay itemDisplay) {
                ItemStack displayStack = originalTemplateItem.clone();
                displayStack.setAmount(Math.min(requiredAmount, displayStack.getMaxStackSize()));
                itemDisplay.setItemStack(displayStack);
            }

            // Update the three-line text display
//            textEntity.setCustomName("§aNeeded per draw: "+ itemName + "§r§b x " + requiredAmount);
            if (line1 != null) line1.setCustomName("§aNeeded per draw: " + itemName + " §b x " + requiredAmount);
            if (line2 != null) line2.setCustomName("§bCurrency collected in the Draw Machine: " + storedAmount + " pcs");
            if (line3 != null) line3.setCustomName("§dPrizes left in the pool: " + totalRewards + " pcs");
        } else {
            // Update item display to a barrier
            if (itemEntity instanceof ItemDisplay itemDisplay) {
                itemDisplay.setItemStack(new ItemStack(Material.BARRIER));
            }

            // Update the three-line text display
//            textEntity.setCustomName("§cNo template set");
            if (line1 != null) line1.setCustomName("§cNo template set");
            if (line2 != null) line2.setCustomName("§7Please set the draw currency first");
            if (line3 != null) line3.setCustomName("§7Prize pool left: " + totalRewards + " pcs");
        }
    }

    // Get the armor stand of a specific line
    private ArmorStand getHologramLine(Block block, String storageKey) {
        String uuidStr = BlockStorage.getLocationInfo(block.getLocation(), storageKey);
        if (uuidStr == null) return null;

        try {
            UUID uuid = UUID.fromString(uuidStr);
            Entity entity = Bukkit.getEntity(uuid);
            if (entity instanceof ArmorStand && !entity.isDead()) {
                return (ArmorStand) entity;
            }
        } catch (IllegalArgumentException e) {
            // UUID Invalid format
        }
        return null;
    }
    private Entity getHologramEntityLine(Block block, String storageKey) {
        String uuidStr = BlockStorage.getLocationInfo(block.getLocation(), storageKey);
        if (uuidStr == null) return null;

        try {
            UUID uuid = UUID.fromString(uuidStr);
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && !entity.isDead()) {
                return entity;
            }
        } catch (IllegalArgumentException e) {
            // UUID Invalid format
        }
        return null;
    }


    // Remove floating display - changed to remove two entities
    private void removeHologram(Block block) {
        cancelHologramAnimation(block.getLocation());
        Entity itemEntity = itemDisplayMap.remove(block.getLocation());
        ArmorStand textEntity = textDisplayMap.remove(block.getLocation());

        // Remove the armor stands of the other lines
        Entity line = getHologramEntityLine(block, "hologram_item");
        ArmorStand line1 = getHologramLine(block, "hologram_line1");
        ArmorStand line2 = getHologramLine(block, "hologram_line2");
        ArmorStand line3 = getHologramLine(block, "hologram_line3");

        if (itemEntity != null && !itemEntity.isDead()) {
            itemEntity.remove();
        }
        if (textEntity != null && !textEntity.isDead()) {
            textEntity.remove();
        }
        if (line != null && !line.isDead()) {
            line.remove();
        }
        if (line1 != null && !line1.isDead()) {
            line1.remove();
        }
        if (line2 != null && !line2.isDead()) {
            line2.remove();
        }
        if (line3 != null && !line3.isDead()) {
            line3.remove();
        }

        // Clear the stored UUID
        BlockStorage.addBlockInfo(block, "hologram_item", null);
        BlockStorage.addBlockInfo(block, "hologram_line1", null);
        BlockStorage.addBlockInfo(block, "hologram_line2", null);
        BlockStorage.addBlockInfo(block, "hologram_line3", null);
    }

    private void handleAutoOutput(BlockMenu menu, Block block) {
        ItemStack originalTemplateItem = getStoredTemplateItemData(block);
        if (originalTemplateItem == null) {
            return;
        }

        int storedAmount = getStoredTemplateAmount(block.getLocation());
        if (storedAmount <= 0) {
            return;
        }

        outputToSlot(menu, block, originalTemplateItem);
    }

    // Get the stored template item amount
    private int getStoredTemplateAmount(Location location) {
        String amountStr = BlockStorage.getLocationInfo(location, "templateAmount");
        if (amountStr == null) {
            return 0;
        }
        try {
            return Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Set the stored template item amount
    private void setStoredTemplateAmount(Block block, int amount) {
        BlockStorage.addBlockInfo(block, "templateAmount", String.valueOf(Math.max(0, amount)));
    }

    // Store the original item data in BlockStorage (using the full serialization method)
    private void storeTemplateItemData(Block block, ItemStack item) {
        String serializedItem = itemToBase64(item);
        if (serializedItem != null) {
            BlockStorage.addBlockInfo(block, "templateItemData", serializedItem);
        }
    }

    // ==================== C1 Performance fix: template item deserialization cache ====================
    // In the original version, updateMenu/processDraw/updateHologram/handleAutoOutput each did this once every tick
    // Base64 deserialize. Now the parsed result is cached per machine location: if the data string hasn't changed and hasn't expired (about 20 ticks), it's reused directly.
    private static final long TEMPLATE_CACHE_TTL_MS = 1000; // Cache lifetime ≈ 20 tick
    private static final ConcurrentHashMap<Location, CachedTemplate> TEMPLATE_CACHE = new ConcurrentHashMap<>();

    // Template cache entry: original serialized string + Parsed result + Expiry time
    private static final class CachedTemplate {
        String rawData;   // The original serialized string for the cache
        ItemStack item;   // Deserialized result (may be null, meaning parsing failed)
        long expireAt;    // Expiry timestamp
    }

    // Get the original item data from BlockStorage (using the full deserialization method)
    private ItemStack getStoredTemplateItemData(Block block) {
        String serializedItem = BlockStorage.getLocationInfo(block.getLocation(), "templateItemData");
        if (serializedItem == null || serializedItem.isEmpty()) {
            // No template data: clear any leftover cache
            TEMPLATE_CACHE.remove(block.getLocation());
            return null;
        }
        // C1 Fix: on a cache hit (original string matches and not expired), reuse the parsed result directly to avoid deserializing every tick
        long now = System.currentTimeMillis();
        CachedTemplate cached = TEMPLATE_CACHE.get(block.getLocation());
        if (cached == null || now >= cached.expireAt || !serializedItem.equals(cached.rawData)) {
            cached = new CachedTemplate();
            cached.rawData = serializedItem;
            cached.item = itemFromBase64(serializedItem);
            cached.expireAt = now + TEMPLATE_CACHE_TTL_MS;
            TEMPLATE_CACHE.put(block.getLocation(), cached);
        }
        // All callers treat the return value as read-only or clone it before changing it, so the shared instance can be returned directly here
        return cached.item;
    }

    // Get item display name
    private String getItemDisplayName(ItemStack item) {
        if (item == null) return "Unknown item";

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        } else {
            // Convert the English name to Chinese display
            String typeName = item.getType().toString().toLowerCase();
            String[] words = typeName.split("_");
            StringBuilder displayName = new StringBuilder();
            for (String word : words) {
                if (!word.isEmpty()) {
                    displayName.append(Character.toUpperCase(word.charAt(0)))
                            .append(word.substring(1))
                            .append(" ");
                }
            }
            return displayName.toString().trim();
        }
    }

    // Create template item display
    private ItemStack createTemplateDisplayItem(ItemStack baseItem, int storedAmount, boolean isAutoOutput, Block block) {
        ItemStack displayItem = baseItem.clone();
        ItemMeta meta = displayItem.getItemMeta();

        List<String> lore = new ArrayList<>();

        if (meta.hasLore()) {
            lore.addAll(meta.getLore());
        }

        // Get the amount needed per draw
        String requiredAmountStr = BlockStorage.getLocationInfo(block.getLocation(), "requiredAmount");
        int requiredAmount = 1;
        try {
            requiredAmount = Integer.parseInt(requiredAmountStr);
        } catch (NumberFormatException e) {
            requiredAmount = 1;
        }

        // Work out the amount of prizes left
        int totalRewards = 0;
        BlockMenu menu = StorageCacheUtils.getMenu(block.getLocation());
        if (menu != null) {
            for (int slot : REWARD_SLOTS) {
                ItemStack reward = menu.getItemInSlot(slot);
                if (reward != null && reward.getType() != Material.AIR) {
                    totalRewards += reward.getAmount();
                }
            }
        }

        // Add a divider and amount display
        lore.add("");
        lore.add("§7Needed per draw: §e" + requiredAmount + " pcs");
        lore.add("§7Current stored amount: §e" + storedAmount + " pcs");
        lore.add("§7Prize pool amount left: §e" + totalRewards + " pcs");
        lore.add("");
        lore.add("§eLeft click: §7Set template item");
        lore.add("§eShift+Left-click: §7" + (isAutoOutput ? "Close" : "On") + "Auto output");
        lore.add("§eRight click: §7Take out one stack of items");
        lore.add("§eShift+Right-click: §7Fill inventory");
        lore.add("");
        lore.add("§cAuto output: " + (isAutoOutput ? "§aOn" : "§7Close"));

        meta.setLore(lore);
        displayItem.setItemMeta(meta);
        displayItem.setAmount(Math.min(requiredAmount, displayItem.getMaxStackSize()));

        return displayItem;
    }


    private void constructMenu(String displayName) {
        new BlockMenuPreset(getId(), displayName) {
            @Override
            public void init() {
                constructMenu(this);
            }

            @Override
            public void newInstance(BlockMenu menu, Block b) {
                updateMenu(menu, b);
            }

            @Override
            public boolean canOpen(@Nonnull Block b, @Nonnull Player p) {
                return p.hasPermission("slimefun.inventory.bypass")
                        || Slimefun.getProtectionManager().hasPermission(p, b.getLocation(),
                        Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.INSERT) {
                    return getInputSlots();
                } else {
                    return getOutputSlots();
                }
            }
        };
    }

    private int[] getInputSlots() {
        return REWARD_SLOTS;
    }
    private int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    protected void constructMenu(BlockMenuPreset preset) {
        for (int i : pinkBorder) {
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack(Material.PINK_STAINED_GLASS_PANE)), " "),
                    (p, slot, item, action) -> false);
        }

        for (int i : blueBorder) {
            // Changed to light blue glass pane
            preset.addItem(i, new CustomItemStack(doGlow(new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE)), " "),
                    (p, slot, item, action) -> false);
        }

        // Template slot click handling
        preset.addMenuClickHandler(TEMPLATE_SLOT, (player, slot, item, action) -> {
            handleTemplateSlotClick(player, slot, item, action);
            return false;
        });

        // Set button click handling
        preset.addMenuClickHandler(SETTINGS_SLOT, (player, slot, item, action) -> {
            handleSettingsSlotClick(player, slot, item, action);
            return false;
        });

        // Output slots - let the player take items out
        preset.addMenuClickHandler(OUTPUT_SLOT, (player, slot, item, action) -> {
            // Let the player take items out of the output slots
            return true;
        });
    }

    private void handleTemplateSlotClick(Player player, int slot, ItemStack item, ClickAction action) {
//        Block targetBlock = player.getTargetBlockExact(5);
//        if (targetBlock == null) return;
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof BlockMenu menu)){
            return;
        }
//        Block targetBlock = inv.getLocation().getBlock();
//        BlockMenu menu = StorageCacheUtils.getMenu(targetBlock.getLocation());
        Block targetBlock = menu.getLocation().getBlock();
        if (menu == null) return;

        int storedAmount = getStoredTemplateAmount(targetBlock.getLocation());
        boolean isTemplateSet = getStoredTemplateItemData(targetBlock) != null;

        // Shift+Left-click - toggle auto output
        if (action.isShiftClicked() && !action.isRightClicked()) {
            Location loc = menu.getLocation();
            boolean currentState = autoOutputMap.getOrDefault(loc, false);
            autoOutputMap.put(loc, !currentState);

            if (!currentState) {
                player.sendMessage(ChatColor.GREEN + "Auto output mode turned on!");
            } else {
                player.sendMessage(ChatColor.YELLOW + "Auto output mode turned off!");
            }
            updateMenu(menu, targetBlock);
        }
        // Normal left-click - set template (can only be reset when the stored amount is 0)
        else if (!action.isRightClicked() && !action.isShiftClicked()) {
            ItemStack cursorItem = player.getItemOnCursor();
            if (!cursorItem.getType().isAir() && (storedAmount == 0 || !isTemplateSet)) {
                // Store the original item data
                storeTemplateItemData(targetBlock, cursorItem);
                // Set the stored amount to 0
                setStoredTemplateAmount(targetBlock, 0);
                player.sendMessage(ChatColor.GREEN + "Draw template item set!");
                updateMenu(menu, targetBlock);
                updateHologram(targetBlock); // Update floating display
            } else if (storedAmount > 0) {
                player.sendMessage(ChatColor.RED + "Take out all template items first before you can reset the template!");
            }
        }
        // Right-click - take out items
        else if (action.isRightClicked()) {
            ItemStack originalItem = getStoredTemplateItemData(targetBlock);
            if (originalItem != null && storedAmount > 0) {
                // Shift+Right-click - fill the inventory as much as possible
                if (action.isShiftClicked()) {
                    fillPlayerInventory(player, originalItem, targetBlock, menu);
                }
                // Normal right-click - take out one stack of items
                else {
                    extractOneStack(player, originalItem, targetBlock, menu);
                }
                updateMenu(menu, targetBlock);
                updateHologram(targetBlock); // Update floating display
            } else if (originalItem != null && storedAmount == 0) {
                player.sendMessage(ChatColor.RED + "No items in the template to take out!");
            }
        }
    }

    // Take out one stack of items (up to 64)
    private void extractOneStack(Player player, ItemStack templateItem, Block block, BlockMenu menu) {
        int storedAmount = getStoredTemplateAmount(block.getLocation());
        int maxStackSize = templateItem.getMaxStackSize();
        int canTake = Math.min(maxStackSize, storedAmount);

        // Work out how much the player's inventory can hold
        int playerCanTake = getMaxCanTake(player, templateItem);
        if (playerCanTake <= 0) {
            player.sendMessage(ChatColor.RED + "Inventory is full, can't take out items!");
            return;
        }

        int actualTake = Math.min(canTake, playerCanTake);
        ItemStack toGive = templateItem.clone();
        toGive.setAmount(actualTake);

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);

        if (leftover.isEmpty()) {
            // Successfully took out
            setStoredTemplateAmount(block, storedAmount - actualTake);
            player.sendMessage(ChatColor.GREEN + "Took out " + actualTake + " template items!");
        } else {
            // Partly taken out
            int actuallyTaken = actualTake - leftover.get(0).getAmount();
            if (actuallyTaken > 0) {
                setStoredTemplateAmount(block, storedAmount - actuallyTaken);
                player.sendMessage(ChatColor.YELLOW + "Took out " + actuallyTaken + " template items (inventory is full)!");
            } else {
                player.sendMessage(ChatColor.RED + "Inventory is full, can't take out items!");
            }
        }
    }

    // Fill the player's inventory as much as possible
    private void fillPlayerInventory(Player player, ItemStack templateItem, Block block, BlockMenu menu) {
        int storedAmount = getStoredTemplateAmount(block.getLocation());
        if (storedAmount <= 0) {
            player.sendMessage(ChatColor.RED + "No items in the template to take out!");
            return;
        }

        int totalTaken = 0;
        int playerCanTake = getMaxCanTake(player, templateItem);

        while (playerCanTake > 0 && storedAmount > 0) {
            int thisTake = Math.min(playerCanTake, storedAmount);
            ItemStack toGive = templateItem.clone();
            toGive.setAmount(thisTake);

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);

            if (leftover.isEmpty()) {
                totalTaken += thisTake;
                storedAmount -= thisTake;
                playerCanTake = getMaxCanTake(player, templateItem);
            } else {
                int actuallyTaken = thisTake - leftover.get(0).getAmount();
                if (actuallyTaken > 0) {
                    totalTaken += actuallyTaken;
                    storedAmount -= actuallyTaken;
                }
                break;
            }
        }

        if (totalTaken > 0) {
            setStoredTemplateAmount(block, storedAmount);
            player.sendMessage(ChatColor.GREEN + "Took out " + totalTaken + " template items!");
        } else {
            player.sendMessage(ChatColor.RED + "Inventory is full, can't take out items!");
        }
    }

    // Put the template item into the output slots
    private void outputToSlot(BlockMenu menu, Block block, ItemStack templateItem) {
        ItemStack outputItem = menu.getItemInSlot(OUTPUT_SLOT);
        int storedAmount = getStoredTemplateAmount(block.getLocation());

        if (storedAmount <= 0) {
            return;
        }

        // If the output slot is empty, put it in directly
        if (outputItem == null || outputItem.getType() == Material.AIR) {
            int toOutputAmount = Math.min(storedAmount, templateItem.getMaxStackSize());
            ItemStack toOutput = templateItem.clone();
            toOutput.setAmount(toOutputAmount);
            menu.replaceExistingItem(OUTPUT_SLOT, toOutput);

            // Reduce the stored template amount
            setStoredTemplateAmount(block, storedAmount - toOutputAmount);
            updateHologram(block); // Update floating display
        }
        // If the output slot already has the same item, try to stack it
        else if (outputItem.isSimilar(templateItem)) {
            int maxStackSize = outputItem.getMaxStackSize();
            int currentAmount = outputItem.getAmount();
            int canAdd = maxStackSize - currentAmount;

            if (canAdd > 0) {
                int toAdd = Math.min(canAdd, storedAmount);
                outputItem.setAmount(currentAmount + toAdd);
                menu.replaceExistingItem(OUTPUT_SLOT, outputItem);

                // Reduce the stored template amount
                setStoredTemplateAmount(block, storedAmount - toAdd);
                updateHologram(block); // Update floating display
            }
            // If the output slot is full, do nothing
        }
        // If the output slot has a different item, do nothing
    }

    // Work out the max amount of the given item the player's inventory can hold
    private int getMaxCanTake(Player player, ItemStack item) {
        int totalSpace = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() == Material.AIR) {
                totalSpace += item.getMaxStackSize();
            } else if (stack.isSimilar(item)) {
                totalSpace += stack.getMaxStackSize() - stack.getAmount();
            }
        }
        return totalSpace;
    }

    private void handleSettingsSlotClick(Player player, int slot, ItemStack item, ClickAction action) {
//        Block targetBlock = player.getTargetBlockExact(5);
//        if (targetBlock == null) return;
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof BlockMenu menu)){
            return;
        }
//        Block targetBlock = inv.getLocation().getBlock();

//        BlockMenu menu = StorageCacheUtils.getMenu(targetBlock.getLocation());
        if (menu == null) return;

        // Left-click - set amount
        if (!action.isRightClicked()) {
            player.closeInventory(); // Close the current container
            player.sendMessage(ChatColor.YELLOW + "Type in chat how many items each draw needs (≥1 whole number): ");

            ChatInput.waitForPlayer(Slimefun.instance(), player, message -> {
                try {
                    int amount = Integer.parseInt(message);
                    if (amount < 1) {
                        player.sendMessage(ChatColor.RED + "Amount must be 1 or more! Set to the default value 1.");
                        amount = 1; // Force set to 1
                    }

                    BlockStorage.addBlockInfo(menu.getBlock(), "requiredAmount", String.valueOf(amount));
                    updateMenu(menu, menu.getBlock());
                    player.sendMessage(ChatColor.GREEN + "Items needed per draw set to: " + amount);

                    // Reopen the menu
                    Bukkit.getScheduler().runTask(Slimefun.instance(), () -> {
                        menu.open(player);
                    });
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Please enter a valid whole number! Set to the default value 1.");
                    BlockStorage.addBlockInfo(menu.getBlock(), "requiredAmount", "1");
                    updateMenu(menu, menu.getBlock());

                    // Reopen the menu
                    Bukkit.getScheduler().runTask(Slimefun.instance(), () -> {
                        menu.open(player);
                    });
                }
            });
        }
        // Right-click - reset to default
        else if (action.isRightClicked()) {
            BlockStorage.addBlockInfo(menu.getBlock(), "requiredAmount", "1");
            updateMenu(menu, menu.getBlock());
            player.sendMessage(ChatColor.GREEN + "Default amount restored: 1");
        }
    }

    private void updateMenu(BlockMenu menu, Block block) {
        String requiredAmount = BlockStorage.getLocationInfo(block.getLocation(), "requiredAmount");
        if (requiredAmount == null) {
            requiredAmount = "1";
        }

        // Update the settings button display
        menu.replaceExistingItem(SETTINGS_SLOT, new CustomItemStack(
                Material.COMPARATOR,
                "§6Draw settings",
                "§eLeft click: §7Set the item amount needed per draw",
                "§eRight click: §7Reset to default amount (1)",
                "",
                "§aCurrent setting: §e" + requiredAmount + " items/draw"
        ));

        // Update template slot display
        ItemStack originalTemplateItem = getStoredTemplateItemData(block);
        boolean isAutoOutput = autoOutputMap.getOrDefault(menu.getLocation(), false);
        int storedAmount = getStoredTemplateAmount(block.getLocation());

        if (originalTemplateItem == null) {
            List<String> lore = new ArrayList<>();
            lore.add("§7Put in the item to check as the template");
            lore.add("");
            lore.add("§eLeft click: §7Set template item");
            lore.add("§eShift+Left-click: §7" + (isAutoOutput ? "Close" : "On") + "Auto output");
            lore.add("§eRight click: §7Take out one stack of items");
            lore.add("§eShift+Right-click: §7Fill inventory");
            lore.add("");
            lore.add("§cAuto output: " + (isAutoOutput ? "§aOn" : "§7Close"));

            menu.replaceExistingItem(TEMPLATE_SLOT, new CustomItemStack(
                    Material.BARRIER,
                    "§cDraw template item",
                    lore.toArray(new String[0])
            ));
        } else {
            // Create display item (with usage hints)
            ItemStack displayItem = createTemplateDisplayItem(originalTemplateItem, storedAmount, isAutoOutput, menu.getBlock());
            menu.replaceExistingItem(TEMPLATE_SLOT, displayItem);
        }

        // Update info display
        updateInfoDisplay(menu, block);
    }

    private void updateInfoDisplay(BlockMenu menu, Block block) {
        ItemStack originalTemplateItem = getStoredTemplateItemData(block);
        String requiredAmount = BlockStorage.getLocationInfo(block.getLocation(), "requiredAmount");
        if (requiredAmount == null) {
            requiredAmount = "1";
        }

        int storedAmount = getStoredTemplateAmount(block.getLocation());
        boolean isAutoOutput = autoOutputMap.getOrDefault(menu.getLocation(), false);

        // First line: template item name and the amount needed
        String templateInfo;
        if (originalTemplateItem != null) {
            String itemName = getItemDisplayName(originalTemplateItem);
            templateInfo = "§aTemplate: §e" + itemName + " §aneeds: §e" + requiredAmount + " pcs";
        } else {
            templateInfo = "§cNo template item set";
        }

        // Second line: amount of prizes left
        int totalRewards = 0;
        for (int slot : REWARD_SLOTS) {
            ItemStack reward = menu.getItemInSlot(slot);
            if (reward != null && reward.getType() != Material.AIR) {
                totalRewards += reward.getAmount();
            }
        }
        String rewardInfo = "§bPrizes left: §e" + totalRewards + " pcs";

        // Auto output state
        String autoOutputInfo = "§6Auto output: " + (isAutoOutput ? "§aOn" : "§7Close");

        // Update info display
        List<String> lore = new ArrayList<>();
        lore.add("§6§lDraw Machine Info");
        lore.add("");

        if (originalTemplateItem != null) {
            String itemName = getItemDisplayName(originalTemplateItem);
            lore.add("§aTemplate item: §e" + itemName);
            lore.add("§aStored amount: §e" + storedAmount);
        } else {
            lore.add("§cNo template item set");
        }

        lore.add("§aNeeded per draw: §e" + requiredAmount + " items");
        lore.add("");
        lore.add(templateInfo);
        lore.add(rewardInfo);
        lore.add(autoOutputInfo);
        lore.add("");
        lore.add("§7Detection range: §b5 block radius");
        lore.add("§7Reward slot: §e" + REWARD_SLOTS.length + " pcs");

        menu.replaceExistingItem(INFO_SLOT, new CustomItemStack(
                Material.GHAST_TEAR,
                "§a※※※ Draw Machine Info ※※※",
                lore.toArray(new String[0])
        ));
    }

    // C2 Fix: nearby entity scan throttle record (per machine location), scans about once every 20 ticks
    private static final long DRAW_SCAN_INTERVAL_MS = 1000; // Scan interval ≈ 20 tick
    private final Map<Location, Long> lastDrawScanMap = new HashMap<>();

    private void processDraw(Block block, BlockMenu menu) {
        // C2 Fix: getNearbyEntities(5 blocks) Scanning slowed to once every 20 ticks, skipped if the interval hasn't passed
        long now = System.currentTimeMillis();
        Long lastScan = lastDrawScanMap.get(block.getLocation());
        if (lastScan != null && now - lastScan < DRAW_SCAN_INTERVAL_MS) {
            return;
        }
        lastDrawScanMap.put(block.getLocation(), now);

        // Get the original template item data
        ItemStack originalTemplateItem = getStoredTemplateItemData(block);
        if (originalTemplateItem == null) {
            return;
        }

        // Get the amount needed
        String requiredAmountStr = BlockStorage.getLocationInfo(block.getLocation(), "requiredAmount");
        int requiredAmount = 1;
        try {
            requiredAmount = Integer.parseInt(requiredAmountStr);
            if (requiredAmount < 1) requiredAmount = 1;
        } catch (NumberFormatException e) {
            requiredAmount = 1;
        }

        // Work out the amount of prizes left
        int totalRewards = 0;
        for (int slot : REWARD_SLOTS) {
            ItemStack reward = menu.getItemInSlot(slot);
            if (reward != null && reward.getType() != Material.AIR) {
                totalRewards += reward.getAmount();
            }
        }

        // If there are no prizes, return right away
        if (totalRewards <= 0) {
            return;
        }

        // Detect items within 5 blocks around - using the sync way
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        int totalFound = 0;
        List<Item> matchedItems = new ArrayList<>();

        // Since the tick is sync now, it can be called directly getNearbyEntities
        for (Entity entity : block.getWorld().getNearbyEntities(center, 5, 5, 5)) {
            if (!(entity instanceof Item)) continue;

            Item item = (Item) entity;
            if (item.isDead()) continue;

            // Skip the floating display item entities
            if (item.hasMetadata("draw-machine-hologram")) {
                continue;
            }

            ItemStack itemStack = item.getItemStack();
            // Compare using the original item data, ignoring the amount
            if (isSimilarSafe(itemStack, originalTemplateItem)) {
                totalFound += itemStack.getAmount();
                matchedItems.add(item);
            }
        }

        // Check if there's enough
        if (totalFound >= requiredAmount) {
            // Work out the max number of draws (considering prize amount and currency amount)
            int maxPossibleByCurrency = totalFound / requiredAmount; // Max draws based on currency
            int maxPossibleByRewards = totalRewards; // Max draws based on prizes
            int multiplier = Math.min(maxPossibleByCurrency, maxPossibleByRewards);

            // Amount of items that actually need to be collected
            int itemsToCollect = multiplier * requiredAmount;

            // Check if the stored amount would go over the limit
            int currentStoredAmount = getStoredTemplateAmount(block.getLocation());
            if (currentStoredAmount > Integer.MAX_VALUE - itemsToCollect) {
                // If it would go over the limit, cancel this draw
                return;
            }

            // Collect items from the ground
            int collected = collectItems(matchedItems, itemsToCollect);

            if (collected > 0) {
                // Check the storage amount limit again (to prevent concurrency problems)
                if (currentStoredAmount > Integer.MAX_VALUE - collected) {
                    return;
                }

                // Update the stored template amount
                int newStoredAmount = currentStoredAmount + collected;
                setStoredTemplateAmount(block, newStoredAmount);
                updateMenu(menu, block); // Update the menu to show the new amount
                updateHologram(block); // Update floating display

                // Trigger the reward spray
                for (int i = 0; i < multiplier; i++) {
                    triggerReward(block, menu);
                }
            }
        }
    }

    public static void cleanupAllHolograms() {
        for (BukkitTask task : HOLOGRAM_ANIMATIONS.values()) {
            task.cancel();
        }
        HOLOGRAM_ANIMATIONS.clear();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.hasMetadata("draw-machine-hologram")) {
                    entity.remove();
                }
            }
        }
    }

    private int collectItems(List<Item> matchedItems, int itemsToCollect) {
        int collected = 0;

        for (Item item : matchedItems) {
            if (collected >= itemsToCollect) break;
            if (item.isDead()) continue;

            ItemStack itemStack = item.getItemStack();
            int amount = itemStack.getAmount();
            int canCollect = Math.min(amount, itemsToCollect - collected);

            if (canCollect >= amount) {
                // Collect the whole item stack
                collected += amount;
                item.remove();
            } else {
                // Collect part of the items
                collected += canCollect;
                itemStack.setAmount(amount - canCollect);
                item.setItemStack(itemStack);
            }
        }

        return collected;
    }

    private void triggerReward(Block block, BlockMenu menu) {
        // Get all non-empty reward slots
        List<Integer> availableSlots = new ArrayList<>();
        for (int slot : REWARD_SLOTS) {
            ItemStack reward = menu.getItemInSlot(slot);
            if (reward != null && reward.getType() != Material.AIR) {
                availableSlots.add(slot);
            }
        }

        if (availableSlots.isEmpty()) {
            return;
        }

        // Pick a random reward slot
        Random random = new Random();
        int selectedSlot = availableSlots.get(random.nextInt(availableSlots.size()));
        ItemStack rewardItem = menu.getItemInSlot(selectedSlot).clone();

        // Set to 1 so a whole stack isn't sprayed out
        rewardItem.setAmount(1);

        // Remove the item from the menu
        ItemStack current = menu.getItemInSlot(selectedSlot);
        if (current.getAmount() > 1) {
            current.setAmount(current.getAmount() - 1);
            menu.replaceExistingItem(selectedSlot, current);
        } else {
            menu.replaceExistingItem(selectedSlot, new ItemStack(Material.AIR));
        }

        // Spray out the reward item
        Location dropLocation = block.getLocation().add(0.5, 1, 0.5);
        Item droppedItem = block.getWorld().dropItem(dropLocation, rewardItem);

        // Give the item a random speed so it looks like it's sprayed out
        double x = (random.nextDouble() - 0.5) * 0.5;
        double y = random.nextDouble() * 0.8 + 0.3;
        double z = (random.nextDouble() - 0.5) * 0.5;
        droppedItem.setVelocity(new Vector(x, y, z));

        // Play draw sound
        playDrawSound(block.getLocation());
    }

    // Method to play the draw sound
    private void playDrawSound(Location location) {
        // Use random sounds for more variety
        Random random = new Random();
        int soundChoice = random.nextInt(3);

        switch(soundChoice) {
            case 0:
                // Success sound 1 - level up sound
                location.getWorld().playSound(location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                break;
            case 1:
                // Success sound 2 - XP sound
                location.getWorld().playSound(location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.8f);
                break;
            case 2:
                // Success sound 3 - bell sound
                location.getWorld().playSound(location, org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                break;
        }

        // Also add a small firework explosion sound
        location.getWorld().playSound(location, org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.5f, 1.5f);
    }

    @Override
    public @NotNull EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.NONE;
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    // Here is the item serialization method you gave
    /**
     * ✅ Serialize an ItemStack fully into a Base64 string
     * Supports: PDC, enchantments, lore, custom models, Slimefun items, NBT and all other data
     */
    private String itemToBase64(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        try (java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
             org.bukkit.util.io.BukkitObjectOutputStream dataOutput = new org.bukkit.util.io.BukkitObjectOutputStream(outputStream)) {

            // Make sure the amount is written correctly first
            dataOutput.writeObject(item.clone()); // Use a clone to avoid outside changes
            dataOutput.flush();

            byte[] serializedBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(serializedBytes);

        } catch (java.io.IOException e) {
            System.err.println("Item serialization failed: " + item.getType());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ✅ Deserialize a Base64 string into ItemStack
     * Fully restore the original item (including PDC, enchantments, amount, NBT, etc.)
     */
    private ItemStack itemFromBase64(String data) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }

        try (java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(Base64.getDecoder().decode(data));
             org.bukkit.util.io.BukkitObjectInputStream dataInput = new org.bukkit.util.io.BukkitObjectInputStream(inputStream)) {

            ItemStack item = (ItemStack) dataInput.readObject();
            // Important: force refresh the ItemMeta reference to prevent PDC cache problems
            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                item.setItemMeta(meta); // Set again to make sure it's consistent
            }
            return item;

        } catch (java.io.IOException | ClassNotFoundException e) {
            System.err.println("Item deserialization failed");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ✅ Safely check whether two ItemStacks are"the same"(can be used for stacking checks)
     * Use Bukkit's built-in isSimilar method, and stay compatible with Slimefun items
     */
    private boolean isSimilarSafe(ItemStack item1, ItemStack item2) {
        if (item1 == item2) return true;
        if (item1 == null || item2 == null) return false;

        // Check if it's air first
        if (item1.getType() == Material.AIR || item2.getType() == Material.AIR) {
            return item1.getType() == item2.getType();
        }

        return item1.isSimilar(item2);
    }
}
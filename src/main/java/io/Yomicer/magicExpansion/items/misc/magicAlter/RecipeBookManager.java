package io.Yomicer.magicExpansion.items.misc.magicAlter;

import io.Yomicer.magicExpansion.MagicExpansion;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import net.guizhanss.guizhanlib.minecraft.helper.inventory.ItemStackHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.persistence.PersistentDataType;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RecipeBookManager {

    private final MagicExpansion plugin;
    // Fix: changed to a static ConcurrentHashMap, so a static cleanup can clear session data of offline players
    private static final ConcurrentHashMap<UUID, String> currentRecipeIds = new ConcurrentHashMap<>();

    public RecipeBookManager(MagicExpansion plugin) {
        this.plugin = plugin;
    }

    /**
     * Fix: when a player leaves, clear that player's current recipe ID record to prevent a memory leak
     */
    public static void cleanup(UUID uuid) {
        currentRecipeIds.remove(uuid);
    }

    // Create the recipe book item
    public ItemStack createRecipeBook() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6Magic Altar Recipe Guide");

            List<String> lore = new ArrayList<>();
            lore.add("§7Right-click to see all available recipes");
            lore.add("§7Learn how to build the altar and craft items");
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(
                    plugin.getPluginInitializer().getRecipeBookKey(),
                    PersistentDataType.BYTE,
                    (byte) 1
            );

            book.setItemMeta(meta);
        }

        return book;
    }

    // Check if it's the recipe book
    public boolean isRecipeBook(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(
                plugin.getPluginInitializer().getRecipeBookKey(),
                PersistentDataType.BYTE
        );
    }

    // Open the recipe book main menu
    public void openRecipeBook(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6Magic Altar Recipe Guide");

        // Clear the current recipe ID
        currentRecipeIds.remove(player.getUniqueId());

        int slot = 10;
        for (Map.Entry<String, MagicAltarRecipe> entry : plugin.getPluginInitializer().getAltarManager().getRecipes().entrySet()) {
            if (slot % 9 == 8) slot += 2;

            String recipeId = entry.getKey();
            ItemStack result = entry.getValue().getResult().clone();
            ItemMeta meta = result.getItemMeta();

            if (meta != null) {
                List<String> lore = new ArrayList<>();
                if(meta.hasLore()) {
                    lore.addAll(Objects.requireNonNull(meta.getLore()));
                }
                lore.add("§7Click to see the crafting recipe");
                lore.add("§eRecipe ID: " + recipeId);
                meta.setLore(lore);

                // Add the recipe ID to PDC
                meta.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "recipe_id"),
                        PersistentDataType.STRING,
                        recipeId
                );

                result.setItemMeta(meta);
            }

            gui.setItem(slot, result);
            slot++;
        }

        // Add a decorative border
        addBorder(gui);

        player.openInventory(gui);
    }

    // Method to work out the real amount
    private int calculateRealAmount(ItemStack item) {
        // Fix: the old batch simulation loop just added the amount up again, and the result always equaled item.getAmount(), so we return it directly to remove the extra code
        return item.getAmount();
    }

    // Open the recipe details menu
    public void openRecipeDetail(Player player, String recipeId, ItemStack resultItem) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6Recipe Details - " + recipeId);

        MagicAltarRecipe recipe = plugin.getPluginInitializer().getAltarManager().getRecipes().get(recipeId);
        if (recipe == null) return;

        // Save the current recipe ID
        currentRecipeIds.put(player.getUniqueId(), recipeId);

        // Show the result item in the center
        ItemStack displayResult = resultItem.clone();
        ItemMeta meta = displayResult.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ItemStackHelper.getDisplayName(displayResult) + "§r§b x " + calculateRealAmount(displayResult));
            List<String> lore = new ArrayList<>();
            if(meta.hasLore()) {
                lore.addAll(Objects.requireNonNull(meta.getLore()));
            }
            lore.add("§6Crafting Result");
            lore.add("§7Recipe: " + recipeId);
            meta.setLore(lore);
            displayResult.setItemMeta(meta);
        }
        gui.setItem(13, displayResult);

        // Add the view button
        ItemStack dispenserButton = createButton(
                Material.DISPENSER,
                "§bDispenser Item Setup",
                Arrays.asList("§7Click to see the 9 dispensers", "§7and the item setup of each one")
        );
        // Add the button tag and recipe ID
        ItemMeta dispenserMeta = dispenserButton.getItemMeta();
        dispenserMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "button_type"),
                PersistentDataType.STRING,
                "dispenser_list"
        );
        dispenserMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "recipe_id"),
                PersistentDataType.STRING,
                recipeId
        );
        dispenserButton.setItemMeta(dispenserMeta);
        gui.setItem(29, dispenserButton);

        ItemStack baseButton = createButton(
                Material.OBSIDIAN,
                "§6Altar Base Layout",
                Arrays.asList("§7Click to see the 5x5 base", "§7and how the blocks are placed")
        );
        // Add the button tag and recipe ID
        ItemMeta baseMeta = baseButton.getItemMeta();
        baseMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "button_type"),
                PersistentDataType.STRING,
                "base_layout"
        );
        baseMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "recipe_id"),
                PersistentDataType.STRING,
                recipeId
        );
        baseButton.setItemMeta(baseMeta);
        gui.setItem(31, baseButton);

        ItemStack frameButton = createButton(
                Material.ITEM_FRAME,
                "§eItem Frame",
                Arrays.asList("§7Place it above the center dispenser", "§7It's used to output the crafting result", "§dIf the amount is more than 1, the extra will shoot out from the middle of the altar", "§cIt must be empty!")
        );
        gui.setItem(33, frameButton);

        // Back button
        ItemStack backButton = createHeadButton(
                "MHF_ArrowLeft",
                "§cBack to Recipe List",
                Arrays.asList("§7Click to go back to the main menu")
        );
        // Add the button tag
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "button_type"),
                PersistentDataType.STRING,
                "back_to_main"
        );
        backButton.setItemMeta(backMeta);
        gui.setItem(45, backButton);

        // Add a decorative border
        addBorder(gui);

        player.openInventory(gui);
    }

    // Open the dispenser list menu
    public void openDispenserList(Player player, String recipeId) {
        Inventory gui = Bukkit.createInventory(null, 54, "§bDispenser List - " + recipeId);

        MagicAltarRecipe recipe = plugin.getPluginInitializer().getAltarManager().getRecipes().get(recipeId);
        if (recipe == null) return;

        // Save the current recipe ID
        currentRecipeIds.put(player.getUniqueId(), recipeId);

        // Show the dispensers in a 3x3 grid
        int[] dispenserSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        String[] dispenserNames = {
                "§bTop Left Dispenser", "§bTop Center Dispenser", "§bTop Right Dispenser",
                "§bMiddle Left Dispenser", "§bCenter Dispenser", "§bMiddle Right Dispenser",
                "§bBottom Left Dispenser", "§bBottom Center Dispenser", "§bBottom Right Dispenser"
        };

        for (int i = 0; i < 9; i++) {
            ItemStack dispenserButton = recipe.getDisplayItem(i);

            if (dispenserButton == null) {
                // Empty dispenser
                dispenserButton = createButton(
                        Material.DROPPER,
                        dispenserNames[i],
                        Arrays.asList("§7This dispenser is empty", "§7Click to see the details")
                );
            } else {
                // Make sure it's a dispenser material and update the display info
                if (dispenserButton.getType() != Material.DISPENSER) {
                    ItemStack newDispenser = new ItemStack(Material.DISPENSER);
                    ItemMeta meta = newDispenser.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(dispenserNames[i]);
                        List<String> lore = new ArrayList<>();
                        lore.add("§7Click to see the details");
                        meta.setLore(lore);
                        newDispenser.setItemMeta(meta);
                    }
                    dispenserButton = newDispenser;
                }
            }

            // Add the dispenser index and recipe ID to PDC
            ItemMeta meta = dispenserButton.getItemMeta();
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "dispenser_index"),
                    PersistentDataType.INTEGER,
                    i
            );
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "recipe_id"),
                    PersistentDataType.STRING,
                    recipeId
            );
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "button_type"),
                    PersistentDataType.STRING,
                    "dispenser_detail"
            );
            dispenserButton.setItemMeta(meta);

            gui.setItem(dispenserSlots[i], dispenserButton);
        }

        // Add the description
        ItemStack infoButton = createButton(
                Material.OAK_SIGN,
                "§eDispenser Setup Info",
                Arrays.asList(
                        "§7Click each dispenser to see what's inside",
                        "§7The exact setup of each item slot",
                        "§7Amounts must match exactly!"
                )
        );
        gui.setItem(49, infoButton);

        // Add the description
        ItemStack dispenserInfoButton = createButton(
                Material.NETHER_STAR,
                "§eClick a dispenser to see the details",
                Arrays.asList(
                        "§7Each dispenser must match its exact position",
                        "§7Rotated layouts aren't supported yet",
                        ColorGradient.getRandomGradientName("When crafting, right-click the center dispenser with the Magic Altar Wand"),
                        "§bTop left corner is coordinate (-1,-1)",
                        "§bTop right corner is coordinate (1,-1)",
                        "§bBottom left corner is coordinate (-1,1)",
                        "§bBottom right corner is coordinate (1,1)"
                )
        );
        gui.setItem(24, dispenserInfoButton);
        ItemStack pinkGlass = createButton(
                Material.PINK_STAINED_GLASS_PANE,
                "§e",
                Arrays.asList(
                        "§e"
                )
        );
        gui.setItem(14, pinkGlass);
        gui.setItem(15, pinkGlass);
        gui.setItem(16, pinkGlass);
        gui.setItem(23, pinkGlass);
        gui.setItem(25, pinkGlass);
        gui.setItem(32, pinkGlass);
        gui.setItem(33, pinkGlass);
        gui.setItem(34, pinkGlass);






        // Back button
        ItemStack backButton = createHeadButton(
                "MHF_ArrowLeft",
                "§cBack to Recipe Details",
                Arrays.asList("§7Click to go back to the recipe details")
        );
        // Add the button tag and recipe ID
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "button_type"),
                PersistentDataType.STRING,
                "back_to_detail"
        );
        backMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "recipe_id"),
                PersistentDataType.STRING,
                recipeId
        );
        backButton.setItemMeta(backMeta);
        gui.setItem(45, backButton);

        addBorder(gui);
        player.openInventory(gui);
    }

    // Open the single dispenser items menu
    public void openDispenserItems(Player player, String recipeId, int dispenserIndex) {
        Inventory gui = Bukkit.createInventory(null, 54, "§bDispenser Items - " + recipeId + " #" + (dispenserIndex + 1));

        MagicAltarRecipe recipe = plugin.getPluginInitializer().getAltarManager().getRecipes().get(recipeId);
        if (recipe == null) return;

        // Save the current recipe ID
        currentRecipeIds.put(player.getUniqueId(), recipeId);

        String[] dispenserNames = {
                "Top Left Dispenser", "Top Center Dispenser", "Top Right Dispenser",
                "Middle Left Dispenser", "Center Dispenser", "Middle Right Dispenser",
                "Bottom Left Dispenser", "Bottom Center Dispenser", "Bottom Right Dispenser"
        };

        // Show the dispenser name
        ItemStack infoButton = createButton(
                Material.OAK_SIGN,
                "§e" + dispenserNames[dispenserIndex] + " Setup",
                Arrays.asList(
                        "§7This is the inside setup of " + dispenserNames[dispenserIndex],
                        "§7All slots must match exactly!",
                        "§7Empty slots must stay empty"
                )
        );
        gui.setItem(4, infoButton);

        // Show the dispenser slots in a 3x3 grid
        int[] slotPositions = {10, 11, 12, 19, 20, 21, 28, 29, 30};

        for (int slotIndex = 0; slotIndex < 9; slotIndex++) {
            ItemStack requiredItem = recipe.getDispenserSlotItem(dispenserIndex, slotIndex);
            int guiSlot = slotPositions[slotIndex];

            if (requiredItem != null) {
                // Show the required items
                ItemStack displayItem = requiredItem.clone();
                ItemMeta meta = displayItem.getItemMeta();
                if (meta != null) {
                    List<String> lore = new ArrayList<>();
                    if(meta.hasLore()){
                        lore.addAll(meta.getLore());
                    }
                    lore.add("§7--------Divider--------");
                    lore.add("§6Required Item");
                    lore.add("§7Slot: " + (slotIndex + 1));
                    lore.add("§7Amount: " + requiredItem.getAmount());
                    lore.add("§cMust match exactly!");
                    meta.setLore(lore);
                    displayItem.setItemMeta(meta);
                }
                gui.setItem(guiSlot, displayItem);
            } else {
                // This slot should be empty
                ItemStack emptyMarker = createButton(
                        Material.GRAY_STAINED_GLASS_PANE,
                        "§7Slot " + (slotIndex + 1),
                        Arrays.asList("§7This slot must be empty", "§cNo items allowed!")
                );
                gui.setItem(guiSlot, emptyMarker);
            }
        }

        // Back button
        ItemStack backButton = createHeadButton(
                "MHF_ArrowLeft",
                "§cBack to Dispenser List",
                Arrays.asList("§7Click to go back to the dispenser list")
        );
        // Add the button tag and recipe ID
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "button_type"),
                PersistentDataType.STRING,
                "back_to_dispenser_list"
        );
        backMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "recipe_id"),
                PersistentDataType.STRING,
                recipeId
        );
        backButton.setItemMeta(backMeta);
        gui.setItem(45, backButton);

        // No border on this menu, because it needs to show a 3x3 grid
        player.openInventory(gui);
    }

    // Open the base layout menu
    public void openBaseLayout(Player player, String recipeId) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6Altar Base - " + recipeId);

        MagicAltarRecipe recipe = plugin.getPluginInitializer().getAltarManager().getRecipes().get(recipeId);
        if (recipe == null) return;

        // Save the current recipe ID
        currentRecipeIds.put(player.getUniqueId(), recipeId);

        // Show the 5x5 base layout
        int[] baseSlots = {
                10, 11, 12, 13, 14,
                19, 20, 21, 22, 23,
                28, 29, 30, 31, 32,
                37, 38, 39, 40, 41,
                46, 47, 48, 49, 50
        };

        Material[][] baseLayout = recipe.getBaseLayout();

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int index = row * 5 + col;
                Material material = baseLayout[row][col];

                ItemStack blockItem = new ItemStack(material);
                ItemMeta meta = blockItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§f" + ItemStackHelper.getDisplayName(blockItem));
                    meta.setLore(Arrays.asList(
                            "§7Altar base block",
                            "§7Position: §e(" + (col-2) + ", " + (row-2) + ")"
                    ));
                    blockItem.setItemMeta(meta);
                }
                gui.setItem(baseSlots[index], blockItem);
            }
        }

        // Add the layout description
        ItemStack infoButton = createButton(
                Material.OAK_SIGN,
                "§eBase Layout Info",
                Arrays.asList(
                        "§7This is the 5x5 altar base layout:",
                        "§7- The layout is centered under the center dispenser",
                        "§7- Coordinate (0,0) is the center dispenser's spot",
                        "§7- Other coordinates are relative to the center",
                        "§7- You must build it exactly like this layout"
                )
        );
        gui.setItem(8, infoButton);

        // Back button
        ItemStack backButton = createHeadButton(
                "MHF_ArrowLeft",
                "§cBack to Recipe Details",
                Arrays.asList("§7Click to go back to the recipe details")
        );
        // Add the button tag and recipe ID
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "button_type"),
                PersistentDataType.STRING,
                "back_to_detail"
        );
        backMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "recipe_id"),
                PersistentDataType.STRING,
                recipeId
        );
        backButton.setItemMeta(backMeta);
        gui.setItem(0, backButton);

        // No border on this menu, because 5x5 won't fit
        player.openInventory(gui);
    }

    // Create button item
    private ItemStack createButton(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    // Create custom head button
    private ItemStack createHeadButton(String texture, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);

            try {
                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                PlayerTextures textures = profile.getTextures();
                URL url = new URL("http://textures.minecraft.net/texture/" + texture);
                textures.setSkin(url);
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            head.setItemMeta(meta);
        }

        return head;
    }

    // Add GUI border
    private void addBorder(Inventory gui) {
        ItemStack border = createButton(
                Material.BLACK_STAINED_GLASS_PANE,
                " ",
                new ArrayList<>()
        );

        // Top and bottom border
        for (int i = 0; i < 9; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, border);
            }
            if (gui.getItem(i + 45) == null) {
                gui.setItem(i + 45, border);
            }
        }

        // Side border
        for (int i = 0; i < 45; i += 9) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, border);
            }
            if (gui.getItem(i + 8) == null) {
                gui.setItem(i + 8, border);
            }
        }
    }

    // Get the material display name
    private String getMaterialName(Material material) {
        String name = material.name().toLowerCase();
        String[] words = name.split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    // Handle GUI click events - fully rewritten version
    public void handleInventoryClick(Player player, int slot, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        // Check the button type
        if (meta.getPersistentDataContainer().has(
                new NamespacedKey(plugin, "button_type"),
                PersistentDataType.STRING
        )) {
            String buttonType = meta.getPersistentDataContainer().get(
                    new NamespacedKey(plugin, "button_type"),
                    PersistentDataType.STRING
            );

            // Get the recipe ID (from PDC or the one currently saved)
            String recipeId = null;
            if (meta.getPersistentDataContainer().has(
                    new NamespacedKey(plugin, "recipe_id"),
                    PersistentDataType.STRING
            )) {
                recipeId = meta.getPersistentDataContainer().get(
                        new NamespacedKey(plugin, "recipe_id"),
                        PersistentDataType.STRING
                );
            } else {
                recipeId = currentRecipeIds.get(player.getUniqueId());
            }

            // Handle the button click
            switch (buttonType) {
                case "dispenser_list":
                    if (recipeId != null) {
                        openDispenserList(player, recipeId);
                    }
                    break;

                case "dispenser_detail":
                    if (meta.getPersistentDataContainer().has(
                            new NamespacedKey(plugin, "dispenser_index"),
                            PersistentDataType.INTEGER
                    )) {
                        int dispenserIndex = meta.getPersistentDataContainer().get(
                                new NamespacedKey(plugin, "dispenser_index"),
                                PersistentDataType.INTEGER
                        );
                        if (recipeId != null) {
                            openDispenserItems(player, recipeId, dispenserIndex);
                        }
                    }
                    break;

                case "base_layout":
                    if (recipeId != null) {
                        openBaseLayout(player, recipeId);
                    }
                    break;

                case "back_to_main":
                    openRecipeBook(player);
                    break;

                case "back_to_detail":
                    if (recipeId != null) {
                        MagicAltarRecipe recipe = plugin.getPluginInitializer().getAltarManager().getRecipes().get(recipeId);
                        if (recipe != null) {
                            openRecipeDetail(player, recipeId, recipe.getResult());
                        }
                    }
                    break;

                case "back_to_dispenser_list":
                    if (recipeId != null) {
                        openDispenserList(player, recipeId);
                    }
                    break;
            }
            return;
        }

        // Check the recipe item click
        if (meta.getPersistentDataContainer().has(
                new NamespacedKey(plugin, "recipe_id"),
                PersistentDataType.STRING
        )) {
            String recipeId = meta.getPersistentDataContainer().get(
                    new NamespacedKey(plugin, "recipe_id"),
                    PersistentDataType.STRING
            );
            MagicAltarRecipe recipe = plugin.getPluginInitializer().getAltarManager().getRecipes().get(recipeId);
            if (recipe != null) {
                openRecipeDetail(player, recipeId, recipe.getResult());
            }
        }
    }
}
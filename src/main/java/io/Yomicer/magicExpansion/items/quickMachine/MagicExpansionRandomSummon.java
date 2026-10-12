package io.Yomicer.magicExpansion.items.quickMachine;

import io.Yomicer.magicExpansion.specialActions.EntitySpawner;
import io.Yomicer.magicExpansion.utils.ItemPermissionUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;
import static org.bukkit.inventory.EquipmentSlot.HAND;

public class MagicExpansionRandomSummon extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    private final EntitySpawner entitySpawner;

    public MagicExpansionRandomSummon(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
        this.entitySpawner = new EntitySpawner();
    }

    @Nonnull
    @Override
    public ItemUseHandler getItemHandler() {
        return e->{
            e.setUseItem(Event.Result.DENY);
            e.setUseBlock(Event.Result.DENY);

                Player player = e.getPlayer();
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                // Check the item in the player's handitem
                if (e.getHand()!= HAND) {
                    player.sendMessage(getGradientName("Please use this with your main hand!", createColorList()));
                    return;
                }
                if(!ItemPermissionUtils.hasPermissionRe(player)){
                    return;
                }

                    // Reduce the amount of the item in handitemamount
                    if (itemInHand.getAmount() > 1) {
                        itemInHand.setAmount(itemInHand.getAmount() - 1);
                    } else {
                        player.getInventory().setItemInMainHand(null); // If there's only one, remove it
                    }
                    // Use entitySpawner
                    // Spawn a random mob 3 blocks away from the player
                    // Call the EntitySpawner instance method
                    entitySpawner.spawnRandomEntityNearPlayer(player);


        };
    }





    /*Override colors*/
    /**
     * Convert an RGB value to a Minecraft color code character
     *
     * @param value RGB value (0-255)
     * @return Hex character (0-9 or A-F)
     */
    public static char codeColor(int value) {
        if (value < 0 || value > 15) {
            throw new IllegalArgumentException("Invalid color value: " + value);
        }
        return "0123456789ABCDEF".charAt(value);
    }

    /**
     * Create a string with gradient colors (Minecraft §x format)
     *
     * @param text       Input text
     * @param colorList  List of gradient colors
     * @return String with gradient colors
     */
    public static String getGradientName(String text, List<Color> colorList) {
        StringBuilder stringBuilder = new StringBuilder();

        // Add spaces if the text is empty or too short
        if (text.length() == 0) {
            text += " ";
        }
        if (text.length() == 1) {
            text += " ";
        }

        int length = text.length();
        for (int i = 0; i < length; i++) {
            double p = ((double) i) / (length - 1) * (colorList.size() - 1); // Interpolation progress
            Color color1 = colorList.get((int) Math.floor(p)); // Start color
            Color color2 = colorList.get((int) Math.ceil(p));  // End color

            // Calculate the interpolated RGB value
            int red = (int) (color1.getRed() * (1 - p + Math.floor(p)) + color2.getRed() * (p - Math.floor(p)));
            int green = (int) (color1.getGreen() * (1 - p + Math.floor(p)) + color2.getGreen() * (p - Math.floor(p)));
            int blue = (int) (color1.getBlue() * (1 - p + Math.floor(p)) + color2.getBlue() * (p - Math.floor(p)));

            // Build the Minecraft color code
            stringBuilder.append("§x")
                    .append("§").append(codeColor(red / 16))
                    .append("§").append(codeColor(red % 16))
                    .append("§").append(codeColor(green / 16))
                    .append("§").append(codeColor(green % 16))
                    .append("§").append(codeColor(blue / 16))
                    .append("§").append(codeColor(blue % 16));

            // Add the current character
            stringBuilder.append(text.charAt(i));
        }

        return stringBuilder.toString();
    }

    private static List<Color> createColorList() {
        List<Color> colorList = new ArrayList<>();
        colorList.add(Color.fromRGB(253, 183, 212)); // Light pink
        colorList.add(Color.fromRGB(250, 126, 179)); // Middle color
        colorList.add(Color.fromRGB(255, 105, 180)); // Bright pink
        return colorList;
    }


}

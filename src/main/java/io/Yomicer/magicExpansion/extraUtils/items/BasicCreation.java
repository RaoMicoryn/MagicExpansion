package io.Yomicer.magicExpansion.extraUtils.items;

import io.Yomicer.magicExpansion.core.MagicExpansionItems;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import io.Yomicer.magicExpansion.utils.itemUtils.sfItemUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

import static io.Yomicer.magicExpansion.utils.ConvertItem.stoneCreateItem;

public class BasicCreation {



    /*
    Magic 2.0 Gen 2 adaptive gradient text
    JS script example:
    var BasicCreation= Java.type("io.Yomicer.magicExpansion.extraUtils.items.BasicCreation")
    var s = BasicCreation.MagicExpansionVer2ColorBuild("your text");
    ---> s is the string with the gradient already applied
    Usage example:
    For example, call it inside onUse(e)
    let p = e.getPlayer();
    p.sendMessage(s);
    ----------------------
    Java example:
    import io.Yomicer.magicExpansion.extraUtils.items;
    String s = BasicCreation.MagicExpansionVer2ColorBuild("your text");
    Usage example:
    Java needs little explanation. s is just a string, so use it wherever you like, as long as it's inside this method
    */
    public static String MagicExpansionVer2ColorBuild(String s) {
        return ColorGradient.getGradientNameVer2(s);
    }



    /*
    * Creates a stack of the given amount from an sfid
    * If the sfid doesn't exist, the item becomes stone
    * */
    public static ItemStack SfItemWithAmount(String s, int amount) {
        ItemStack i = stoneCreateItem(s);
        i.setAmount(amount);
        return i;
    }

    /*
    * Generates an ID card: a random Chinese character ("gibberish tech")
    * */
    public static ItemStack IDCard() {
        char ans;
        int start = 0x3400;
        int end = 0x9FFF;
        int range = end - start;
        int randomOffset = new Random().nextInt(range);
        ans = (char) (start + randomOffset);
        ItemStack it = new CustomItemStack(Material.SUGAR, ColorGradient.getGradientNameVer2("Symbol"), "&f" + ans, ("§7The basis for making matter out of nothing"));
        ItemStack s = new SlimefunItemStack("MOMOTECH_LETTER", it);
        return s;
    }






}

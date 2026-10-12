package io.Yomicer.magicExpansion.utils.itemUtils;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import org.bukkit.Material;

import java.math.BigInteger;

import static io.Yomicer.magicExpansion.utils.ColorGradient.getGradientName;
import static io.Yomicer.magicExpansion.utils.Utils.doGlow;

public class MagicSugarBuilder {

    private static final char[] SUPERSCRIPT = {'⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹'};

    /**
     * Gets the Magic Sugar item for power n (from 1 to 39)
     * The item name and lore use superscript text, like Magic Sugar*9², with 9² Magic Sugar items stacked
     */
    public static SlimefunItemStack getMagicSugar(int power) {
        if (power < 1 || power > 39) {
            throw new IllegalArgumentException("Power must be between 1 and 39");
        }

        String id = "MAGIC_EXPANSION_MAGIC_SUGAR_" + power;
        String displayName = "Magic Sugar*9" + toSuperscript(power);
        // Calculate the actual value of 9^power
        BigInteger base = BigInteger.valueOf(9);
        BigInteger result = base.pow(power); // Calculate 9^power
        String lore = "Stacked " + result + " Magic Sugar items";

        return new SlimefunItemStack(
                id,
                doGlow(Material.SUGAR),
                getGradientName(displayName),
                "",
                getGradientName(lore),
                getGradientName("What is this even for?"),
                getGradientName("Maybe it'll be useful someday")
        );
    }

    /**
     * Converts an integer to a Unicode superscript string (like 123 -> ¹²³)
     */
    private static String toSuperscript(int n) {
        StringBuilder sb = new StringBuilder();
        for (char digit : String.valueOf(n).toCharArray()) {
            sb.append(SUPERSCRIPT[digit - '0']);
        }
        return sb.toString();
    }
}

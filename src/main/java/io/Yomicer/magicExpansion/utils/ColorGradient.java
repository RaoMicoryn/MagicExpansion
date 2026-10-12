package io.Yomicer.magicExpansion.utils;

import org.bukkit.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ColorGradient {
    /*Override colors*/
    /**
     * Convert RGB values to Minecraft color-code characters
     *
     * @param value RGB component value (0–255)
     * @return Hexadecimal character (0–9 or A–F)
     */
    public static char codeColor(int value) {
        if (value < 0 || value > 15) {
            throw new IllegalArgumentException("Invalid color value: " + value);
        }
        return "0123456789ABCDEF".charAt(value);
    }

    /**
     * Generate a gradient-colored string (Minecraft §x format)
     *
     * @param text       Input string
     * @param colorList  Gradient color list
     * @return Gradient-colored string
     */
    public static String getGradientName(String text, List<Color> colorList) {
        // Use the default color list if none is provided
        if (colorList == null || colorList.isEmpty()) {
            colorList = createColorList();
        }

        StringBuilder stringBuilder = new StringBuilder();

        // Pad with spaces if the text is empty or too short
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

            // Calculate the interpolated RGB values
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

            // Append the current character
            stringBuilder.append(text.charAt(i));
        }

        return stringBuilder.toString();
    }

    /**
     * Generate a gradient-colored string (Minecraft §x format)
     *
     * @param text       Input string
     * @param colorList  Gradient color list
     * @return Gradient-colored string
     */
    public static String getGradientNameVer2(String text, List<Color> colorList) {
        // Use the default color list if none is provided
        if (colorList == null || colorList.isEmpty()) {
            colorList = createCustomColorListV2();
        }

        StringBuilder stringBuilder = new StringBuilder();

        // Pad with spaces if the text is empty or too short
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

            // Calculate the interpolated RGB values
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

            // Append the current character
            stringBuilder.append(text.charAt(i));
        }

        return stringBuilder.toString();
    }


    /**
     * Generate a vertical gradient Lore 列表
     * - Automatically calculate the total number of lines
     * - Automatically use the default color list
     * - Keep colors uniform within each line and apply a gradient between lines
     *
     * @param lore Original text list
     * @return Processed colored text list
     */
    public static List<String> getVerticalGradientLineV2(List<String> lore) {
        // Handle an empty list
        if (lore == null || lore.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. Automatically get the default color list
        List<Color> colorList = createCustomColorListV2();

        // 2. Automatically calculate the total number of lines
        int totalLines = lore.size();
        List<String> result = new ArrayList<>();

        // 3. Process each line
        for (int lineIndex = 0; lineIndex < totalLines; lineIndex++) {
            String text = lore.get(lineIndex);

            if (text == null || text.isEmpty()) {
                result.add("");
                continue;
            }

            // 计算进度 p (0.0 ~ 1.0)
            double p;
            if (totalLines <= 1) {
                p = 0;
            } else {
                p = ((double) lineIndex) / (totalLines - 1);
            }

            // Determine the start and end color indexes
            int maxIndex = colorList.size() - 1;
            int index1 = (int) Math.floor(p * maxIndex);
            int index2 = (int) Math.ceil(p * maxIndex);

            // Bounds check
            if (index1 > maxIndex) index1 = maxIndex;
            if (index2 > maxIndex) index2 = maxIndex;

            Color color1 = colorList.get(index1);
            Color color2 = colorList.get(index2);

            Color finalColor;

            // Calculate the interpolated color
            if (index1 == index2) {
                finalColor = color1;
            } else {
                double localP = (p * maxIndex) - index1;

                int r1 = color1.getRed();
                int g1 = color1.getGreen();
                int b1 = color1.getBlue();

                int r2 = color2.getRed();
                int g2 = color2.getGreen();
                int b2 = color2.getBlue();

                int red = (int) (r1 * (1 - localP) + r2 * localP);
                int green = (int) (g1 * (1 - localP) + g2 * localP);
                int blue = (int) (b1 * (1 - localP) + b2 * localP);

                finalColor = Color.fromRGB(red, green, blue);
            }

            // 应用颜色并添加结果
            result.add(applySingleColor(text, finalColor));
        }

        return result;
    }

    private static String applySingleColor(String text, Color color) {
        StringBuilder sb = new StringBuilder();
        sb.append("§x")
                .append("§").append(codeColor(color.getRed() / 16))
                .append("§").append(codeColor(color.getRed() % 16))
                .append("§").append(codeColor(color.getGreen() / 16))
                .append("§").append(codeColor(color.getGreen() % 16))
                .append("§").append(codeColor(color.getBlue() / 16))
                .append("§").append(codeColor(color.getBlue() % 16));

        return sb.toString() + text;
    }







    /**
     * Generate a gradient-colored string (Minecraft § format)
     *
     * @param text Input string
     * @return Gradient-colored string
     */
    public static String getGradientName(String text) {
        return getGradientName(text, null); // Call the original method using the default color list
    }

    public static String getGradientNameVer2(String text) {
        return getGradientNameVer2(text, null); // Call the original method using the default color list
    }



    //default color
    private static List<Color> createColorList() {
        List<Color> colorList = new ArrayList<>();
        colorList.add(Color.fromRGB(253, 183, 212)); // Light pink
        colorList.add(Color.fromRGB(250, 126, 179)); // Intermediate color
        colorList.add(Color.fromRGB(255, 105, 180)); // Bright pink
        return colorList;
    }

    public static List<Color> createCustomColorListV2() {
        List<Color> colorList = new ArrayList<>();

        colorList.add(Color.fromRGB(0x6b, 0xee, 0xd1)); // #6beed1
        colorList.add(Color.fromRGB(0xaa, 0xcd, 0xd0)); // #aacdd0
        colorList.add(Color.fromRGB(0xbf, 0xba, 0xd0)); // #bfbad0
        colorList.add(Color.fromRGB(0x9c, 0x52, 0x8b)); // #9c528b
        colorList.add(Color.fromRGB(0xff, 0x32, 0xce)); // #ff32ce

        return colorList;
    }



    /**
     * Generate a random color list of the specified size
     *
     * @param count Number of random colors
     * @return Random color list
     */
    private static List<Color> createRandomColorList(int count) {
        List<Color> colorList = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            int red = random.nextInt(256);
            int green = random.nextInt(256);
            int blue = random.nextInt(256);
            colorList.add(Color.fromRGB(red, green, blue));
        }
        return colorList;
    }

    /**
     * Generate a string with a random gradient (Minecraft §x format)
     *
     * @param text  Input string
     * @param count Number of gradient colors
     * @return String with a random gradient
     */
    public static String getRandomGradientName(String text, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be greater than zero");
        }
        List<Color> randomColorList = createRandomColorList(count);
        return getGradientName(text, randomColorList);
    }

    // 如果需要直接调用，不指定颜色数量，默认使用3种颜色进行渐变
    public static String getRandomGradientName(String text) {
        return getRandomGradientName(text, 3); // Default three-color gradient
    }



}

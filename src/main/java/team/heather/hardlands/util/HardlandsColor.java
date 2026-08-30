package team.heather.hardlands.util;

import net.kyori.adventure.text.format.TextColor;

public record HardlandsColor(
        TextColor color1,
        TextColor color2,
        TextColor color3
) {

    public static final TextColor PRIMARY = TextColor.color(0xA4133C);
    public static final TextColor LIGHT_GRAY = TextColor.color(0xBDBDBD);

    public static final HardlandsColor FRAMED = new HardlandsColor(0xE5383B, 0x8C2F39, 0x800E13);

    public HardlandsColor(int base, int pastel, int variation) {
        this(
                TextColor.color(base),
                TextColor.color(pastel),
                TextColor.color(variation)
        );
    }
}
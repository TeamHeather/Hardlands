package team.heather.hardlands.common.ui;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.DyeColor;

public record HardlandsColor(TextColor primary, TextColor secondary, TextColor tertiary) {

    public static final TextColor HARDLANDS = TextColor.color(0xA4133C);
    public static final TextColor LIGHT_GRAY = TextColor.color(0xBDBDBD);

    public static final HardlandsColor RED = new HardlandsColor(0xE5383B, 0x8C2F39, 0x800E13);
    public static final HardlandsColor GREEN = new HardlandsColor(0x52B788, 0x40916C, 0x2D6A4F);
    public static final HardlandsColor BLUE = new HardlandsColor(0x4CC9F0, 0x4895EF, 0x4361EE);
    public static final HardlandsColor YELLOW = new HardlandsColor(0xF9C74F, 0xF4A261, 0xE9C46A);
    public static final HardlandsColor GRAY = new HardlandsColor(0xBDBDBD, 0x6C757D, 0x495057);

    public HardlandsColor(int primary, int secondary, int tertiary) {
        this(TextColor.color(primary), TextColor.color(secondary), TextColor.color(tertiary));
    }

    public static TextColor profile(DyeColor color) {
        return TextColor.color(switch (color) {
            case WHITE -> 0xF1F3F5;
            case ORANGE -> 0xD9772A;
            case MAGENTA -> 0xC04BAA;
            case LIGHT_BLUE -> 0x62A7D8;
            case YELLOW -> 0xD8B23C;
            case LIME -> 0x82B83D;
            case PINK -> 0xD76D91;
            case GRAY -> 0x7A8087;
            case LIGHT_GRAY -> 0xB7BCC2;
            case CYAN -> 0x39A5AA;
            case PURPLE -> 0x8A55B5;
            case BLUE -> 0x536CC7;
            case BROWN -> 0x9A6B43;
            case GREEN -> 0x4D9567;
            case RED -> 0xA4133C;
            case BLACK -> 0x5B6068;
        });
    }

    public static String profileHex(DyeColor color) {
        return "#%06X".formatted(profile(color).value());
    }
}
package team.heather.hardlands.ui;

import net.kyori.adventure.text.format.TextColor;

public record HardlandsColor(
        TextColor primary,
        TextColor secondary,
        TextColor tertiary
) {

    public static final TextColor HARDLANDS = TextColor.color(0xA4133C);
    public static final TextColor LIGHT_GRAY = TextColor.color(0xBDBDBD);

    public static final HardlandsColor RED =
            new HardlandsColor(0xE5383B, 0x8C2F39, 0x800E13);

    public static final HardlandsColor GREEN =
            new HardlandsColor(0x52B788, 0x40916C, 0x2D6A4F);

    public static final HardlandsColor BLUE =
            new HardlandsColor(0x4CC9F0, 0x4895EF, 0x4361EE);

    public static final HardlandsColor YELLOW =
            new HardlandsColor(0xF9C74F, 0xF4A261, 0xE9C46A);

    public static final HardlandsColor GRAY =
            new HardlandsColor(0xBDBDBD, 0x6C757D, 0x495057);

    public HardlandsColor(int primary, int secondary, int tertiary) {
        this(
                TextColor.color(primary),
                TextColor.color(secondary),
                TextColor.color(tertiary)
        );
    }
}
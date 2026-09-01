package team.heather.hardlands.ui;

import net.kyori.adventure.text.format.TextColor;

public record HardlandsColor(
        TextColor primary,
        TextColor secondary,
        TextColor tertiary
) {

    public static final TextColor HARDLANDS = TextColor.color(0xA4133C);
    public static final TextColor LIGHT_GRAY = TextColor.color(0xBDBDBD);

    public static final HardlandsColor NEUTRAL = new HardlandsColor(0xADB5BD, 0x6C757D, 0x495057);
    public static final HardlandsColor INFO = new HardlandsColor(0x4CC9F0, 0x4895EF, 0x4361EE);
    public static final HardlandsColor GAMEPLAY = new HardlandsColor(0x52B788, 0x40916C, 0x2D6A4F);
    public static final HardlandsColor DANGER = new HardlandsColor(0xE5383B, 0x8C2F39, 0x800E13);

    public HardlandsColor(int primary, int secondary, int tertiary) {
        this(TextColor.color(primary), TextColor.color(secondary), TextColor.color(tertiary));
    }
}
package team.heather.hardlands.common.tablist;

import java.util.UUID;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import team.heather.hardlands.common.ui.HardlandsColor;
import team.heather.hardlands.game.GameData;
import team.heather.hardlands.util.TextFormatters;

public final class TabListFormatter {

    public static final int GLINT_FRAMES = 40;

    private static final int FRAME_LENGTH = 65;
    private static final int HEADER_GAP_LENGTH = 44;

    private static final double GLINT_MARGIN = 6.0;
    private static final double GLINT_SIGMA = 3.0;

    private static final String DEFAULT_HOST = "MrPepe3012";
    private static final String LABEL = TextFormatters.TINY_CAPS.format("Hardlands");

    private static final TextColor KILL_COLOR = HardlandsColor.LIGHT_GRAY;

    private static final Component KILL_SPRITE = sprite(
            "minecraft:items",
            "minecraft:item/iron_sword"
    );

    private static final Component HARDCORE_HEART_SPRITE = sprite(
            "minecraft:gui",
            "minecraft:hud/heart/hardcore_full"
    );

    private static final Component ABSORPTION_HEART_SPRITE = sprite(
            "minecraft:gui",
            "minecraft:hud/heart/absorbing_hardcore_full"
    );

    private static TextColor baseColor = HardlandsColor.profile(DyeColor.RED);

    private TabListFormatter() {}

    public static TextColor getBaseColor() {
        return baseColor;
    }

    public static void setBaseColor(TextColor color) {
        if (color == null) {
            throw new IllegalArgumentException("Tab list base color cannot be null");
        }

        baseColor = color;
    }

    public static HeaderLayout headerLayout(GameData game) {
        Component host = game.getHost() == null
                ? host(DEFAULT_HOST)
                : host(game.getHost());

        Component prefix = frame(HEADER_GAP_LENGTH)
                .append(Component.newline())
                .append(Component.newline());

        Component suffix = Component.newline()
                .append(Component.newline())
                .append(Component.text(
                        TextFormatters.TINY_CAPS.format("hosted by: "),
                        HardlandsColor.LIGHT_GRAY
                ))
                .append(host)
                .append(Component.newline())
                .append(Component.newline());

        return new HeaderLayout(prefix, suffix);
    }

    public static Component label(int gameNumber) {
        return Component.text(labelText(gameNumber), baseColor)
                .decoration(TextDecoration.BOLD, false);
    }

    public static Component glintLabel(int gameNumber, int frame, int activation) {
        String text = labelText(gameNumber);
        double progress = glintProgress(frame);

        if ((activation & 1) != 0) {
            progress = 1.0 - progress;
        }

        double start = -GLINT_MARGIN;
        double end = text.length() - 1 + GLINT_MARGIN;
        double position = start + (end - start) * progress;

        TextComponent.Builder result = Component.text()
                .decoration(TextDecoration.BOLD, false);

        for (int index = 0; index < text.length(); index++) {
            double distance = index - position;
            double strength = Math.exp(
                    -(distance * distance) / (2.0 * GLINT_SIGMA * GLINT_SIGMA)
            );

            result.append(Component.text(
                    String.valueOf(text.charAt(index)),
                    blend(baseColor, NamedTextColor.WHITE, strength)
            ));
        }

        return result.build();
    }

    public static Component footer(int alivePlayers, int onlinePlayers) {
        return Component.newline()
                .append(Component.newline())
                .append(Component.text(alivePlayers, baseColor))
                .append(Component.text(" of ", NamedTextColor.WHITE))
                .append(Component.text(onlinePlayers, baseColor))
                .append(Component.text(" players alive.", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.newline())
                .append(frame(0));
    }

    public static Component player(
            Player player,
            Component prefix,
            Component name,
            Component suffix,
            GameData game
    ) {
        return Component.text("[", HardlandsColor.LIGHT_GRAY)
                .append(playerState(player, game))
                .append(Component.text("] ", HardlandsColor.LIGHT_GRAY))
                .append(prefix)
                .append(name)
                .append(suffix)
                .append(killCounter(game.getKillCount(player.getUniqueId())))
                .append(healthCounter(player));
    }

    private static Component playerState(Player player, GameData game) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return PlayerState.SPECTATING.component();
        }

        UUID playerId = player.getUniqueId();

        if (matches(game.getPaperMan(), playerId)) {
            return PlayerState.PAPER_MAN.component();
        }

        if (matches(game.getIronMan(), playerId)) {
            return PlayerState.IRON_MAN.component();
        }

        return PlayerState.ALIVE.component();
    }

    private static Component killCounter(int kills) {
        return Component.text(" [", HardlandsColor.LIGHT_GRAY)
                .append(Component.text(kills, KILL_COLOR))
                .append(KILL_SPRITE)
                .append(Component.text("] ", HardlandsColor.LIGHT_GRAY));
    }

    private static Component healthCounter(Player player) {
        double absorption = player.getAbsorptionAmount();
        double health = player.getHealth();
        double totalHealth = health + absorption;

        return Component.text(
                        formatHealth(totalHealth),
                        healthColor(totalHealth, maxHealth(player))
                )
                .append(absorption > 0.0
                        ? ABSORPTION_HEART_SPRITE
                        : HARDCORE_HEART_SPRITE);
    }

    private static TextColor healthColor(double health, double maxHealth) {
        if (maxHealth <= 0.0) {
            return NamedTextColor.DARK_GRAY;
        }

        double progress = Math.clamp(health / maxHealth, 0.0, 1.0);

        return blend(
                NamedTextColor.DARK_GRAY,
                baseColor,
                progress
        );
    }

    private static double maxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);

        return attribute == null
                ? 20.0
                : attribute.getValue();
    }

    private static Component host(GameData.Host host) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(host.playerId());
        String name = player.getName();

        return host(
                ObjectContents.playerHead(host.playerId()),
                name == null ? "Unknown" : name
        );
    }

    private static Component host(String name) {
        return host(
                ObjectContents.playerHead(name),
                name
        );
    }

    private static Component host(ObjectContents head, String name) {
        return Component.object(head)
                .color(NamedTextColor.WHITE)
                .append(Component.space())
                .append(Component.text(name, NamedTextColor.WHITE));
    }

    private static Component frame(int gapLength) {
        Component start = Component.text("♢", baseColor)
                .append(Component.text("»", HardlandsColor.LIGHT_GRAY));

        Component end = Component.text("«", HardlandsColor.LIGHT_GRAY)
                .append(Component.text("♢", baseColor));

        if (gapLength == 0) {
            return start
                    .append(line(FRAME_LENGTH))
                    .append(end);
        }

        int separatorLength = FRAME_LENGTH - gapLength;
        int leftLength = separatorLength / 2;
        int rightLength = separatorLength - leftLength;

        return start
                .append(line(leftLength))
                .append(Component.text(" ".repeat(gapLength)))
                .append(line(rightLength))
                .append(end);
    }

    private static TextComponent line(int length) {
        return Component.text(
                        " ".repeat(length),
                        HardlandsColor.LIGHT_GRAY
                )
                .decorate(TextDecoration.STRIKETHROUGH);
    }

    private static Component sprite(String atlas, String sprite) {
        return Component.object(ObjectContents.sprite(
                        Key.key(atlas),
                        Key.key(sprite)
                ))
                .color(NamedTextColor.WHITE);
    }

    private static boolean matches(GameData.FirstDamage damage, UUID playerId) {
        return damage != null && damage.playerId().equals(playerId);
    }

    private static String labelText(int gameNumber) {
        return LABEL + " #" + gameNumber;
    }

    private static double glintProgress(int frame) {
        return (double) Math.floorMod(frame, GLINT_FRAMES)
                / (GLINT_FRAMES - 1);
    }

    private static TextColor blend(TextColor from, TextColor to, double amount) {
        double progress = Math.clamp(amount, 0.0, 1.0);

        return TextColor.color(
                interpolate(from.red(), to.red(), progress),
                interpolate(from.green(), to.green(), progress),
                interpolate(from.blue(), to.blue(), progress)
        );
    }

    private static int interpolate(int from, int to, double amount) {
        return (int) Math.round(
                from + (to - from) * amount
        );
    }

    private static String formatHealth(double health) {
        double rounded = Math.round(health * 10.0) / 10.0;

        return rounded == Math.rint(rounded)
                ? Integer.toString((int) rounded)
                : Double.toString(rounded);
    }

    public record HeaderLayout(
            Component prefix,
            Component suffix
    ) {

        public Component format(Component label) {
            return this.prefix
                    .append(label)
                    .append(this.suffix);
        }
    }

    private enum PlayerState {

        ALIVE("●", TextColor.color(0x55FF55)),
        IRON_MAN("◆", TextColor.color(0xFFAA00)),
        PAPER_MAN("◆", null),
        SPECTATING("○", TextColor.color(0xAAAAAA));

        private final String symbol;
        private final TextColor color;

        PlayerState(String symbol, TextColor color) {
            this.symbol = symbol;
            this.color = color;
        }

        private Component component() {
            return Component.text(
                    this.symbol,
                    this.color == null
                            ? baseColor
                            : this.color
            );
        }
    }
}
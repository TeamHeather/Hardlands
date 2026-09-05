package team.heather.hardlands.common.ui.tablist;

import java.util.UUID;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.object.ObjectContents;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import team.heather.hardlands.common.player.HardlandsPlayer;
import team.heather.hardlands.common.ui.HardlandsColor;
import team.heather.hardlands.game.GameData;
import team.heather.hardlands.util.TextFormatters;

public final class TabListRenderer {

    private static final Component ABSORPTION_HEART = sprite("minecraft:gui", "minecraft:hud/heart/absorbing_hardcore_full");
    private static final Component HARDCORE_HEART = sprite("minecraft:gui", "minecraft:hud/heart/hardcore_full");
    private static final Component KILL_ICON = sprite("minecraft:items", "minecraft:item/iron_sword");

    public static final int GLINT_FRAMES = 40;
    private static final double GLINT_MARGIN = 6.0;
    private static final double GLINT_SIGMA = 3.0;

    private static final int FRAME_LENGTH = 65;
    private static final int HEADER_GAP_LENGTH = 44;

    private static final String LABEL = TextFormatters.TINY_CAPS.format("Hardlands");

    private final String defaultHost;

    private TextColor baseColor;

    public TabListRenderer(String defaultHost, TextColor baseColor) {
        if (defaultHost == null || defaultHost.isBlank()) {
            throw new IllegalArgumentException("Default tab list host cannot be null or blank");
        }

        if (baseColor == null) {
            throw new IllegalArgumentException("Tab list base color cannot be null");
        }

        this.defaultHost = defaultHost;
        this.baseColor = baseColor;
    }

    public Component footer(int alivePlayers, int onlinePlayers) {
        return Component.newline()
                .append(Component.newline())
                .append(Component.text(alivePlayers, this.baseColor))
                .append(Component.text(" of ", NamedTextColor.WHITE))
                .append(Component.text(onlinePlayers, this.baseColor))
                .append(Component.text(" players alive.", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.newline())
                .append(frame(0));
    }

    public Component glintLabel(int gameNumber, int frame, int activation) {
        String text = LABEL + " #" + gameNumber;
        double progress = (double) Math.floorMod(frame, GLINT_FRAMES) / (GLINT_FRAMES - 1);

        if ((activation & 1) != 0) {
            progress = 1.0 - progress;
        }

        double position = -GLINT_MARGIN + (text.length() - 1 + GLINT_MARGIN * 2.0) * progress;

        TextComponent.Builder result = Component.text()
                .decoration(TextDecoration.BOLD, false);

        for (int index = 0; index < text.length(); index++) {
            double distance = index - position;
            double strength = Math.exp(-(distance * distance) / (2.0 * GLINT_SIGMA * GLINT_SIGMA));

            result.append(Component.text(
                    String.valueOf(text.charAt(index)),
                    blend(this.baseColor, NamedTextColor.WHITE, strength)
            ));
        }

        return result.build();
    }

    public Component label(int gameNumber) {
        return Component.text(LABEL + " #" + gameNumber, this.baseColor)
                .decoration(TextDecoration.BOLD, false);
    }

    public Component player(Player player, Component prefix, Component name, Component suffix, GameData game) {
        return Component.text("[", HardlandsColor.LIGHT_GRAY)
                .append(playerState(player, game))
                .append(Component.text("] ", HardlandsColor.LIGHT_GRAY))
                .append(prefix)
                .append(name)
                .append(suffix)
                .append(Component.text(" [", HardlandsColor.LIGHT_GRAY))
                .append(Component.text(game.killCount(player), HardlandsColor.LIGHT_GRAY))
                .append(KILL_ICON)
                .append(Component.text("] ", HardlandsColor.LIGHT_GRAY))
                .append(health(player));
    }

    public HeaderLayout headerLayout(GameData game) {
        GameData.Host gameHost = game.host();
        Component host;

        if (gameHost == null) {
            host = host(this.defaultHost);
        } else {
            OfflinePlayer player = Bukkit.getOfflinePlayer(gameHost.hoster());
            String name = player.getName();

            host = Component.object(ObjectContents.playerHead(gameHost.hoster()))
                    .color(NamedTextColor.WHITE)
                    .append(Component.space())
                    .append(Component.text(name == null ? "Unknown" : name, NamedTextColor.WHITE));
        }

        Component prefix = frame(HEADER_GAP_LENGTH)
                .append(Component.newline())
                .append(Component.newline());

        Component suffix = Component.newline()
                .append(Component.newline())
                .append(Component.text(TextFormatters.TINY_CAPS.format("hosted by: "), HardlandsColor.LIGHT_GRAY))
                .append(host)
                .append(Component.newline())
                .append(Component.newline());

        return new HeaderLayout(prefix, suffix);
    }

    public TextColor getBaseColor() {
        return this.baseColor;
    }

    public void setBaseColor(TextColor baseColor) {
        if (baseColor == null) {
            throw new IllegalArgumentException("Tab list base color cannot be null");
        }

        this.baseColor = baseColor;
    }

    private Component frame(int gapLength) {
        Component start = Component.text("♢", this.baseColor)
                .append(Component.text("»", HardlandsColor.LIGHT_GRAY));

        Component end = Component.text("«", HardlandsColor.LIGHT_GRAY)
                .append(Component.text("♢", this.baseColor));

        if (gapLength == 0) {
            return start
                    .append(line(FRAME_LENGTH))
                    .append(end);
        }

        int separatorLength = FRAME_LENGTH - gapLength;
        int leftLength = separatorLength / 2;

        return start
                .append(line(leftLength))
                .append(Component.text(" ".repeat(gapLength)))
                .append(line(separatorLength - leftLength))
                .append(end);
    }

    private Component health(Player player) {
        double absorption = player.getAbsorptionAmount();
        double health = player.getHealth() + absorption;

        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = attribute == null ? 20.0 : attribute.getValue();

        TextColor color = maxHealth <= 0.0
                ? NamedTextColor.DARK_GRAY
                : blend(NamedTextColor.DARK_GRAY, this.baseColor, Math.clamp(health / maxHealth, 0.0, 1.0));

        double rounded = Math.round(health * 10.0) / 10.0;
        String value = rounded == Math.rint(rounded) ? Integer.toString((int) rounded) : Double.toString(rounded);

        return Component.text(value, color)
                .append(absorption > 0.0 ? ABSORPTION_HEART : HARDCORE_HEART);
    }

    private Component playerState(Player player, GameData game) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return PlayerState.SPECTATING.component(this.baseColor);
        }

        UUID playerId = player.getUniqueId();
        HardlandsPlayer paperMan = game.paperMan();

        if (paperMan != null && paperMan.getUniqueId().equals(playerId)) {
            return PlayerState.PAPER_MAN.component(this.baseColor);
        }

        HardlandsPlayer ironMan = game.ironMan();

        if (ironMan != null && ironMan.getUniqueId().equals(playerId)) {
            return PlayerState.IRON_MAN.component(this.baseColor);
        }

        return PlayerState.ALIVE.component(this.baseColor);
    }

    private TextColor blend(TextColor from, TextColor to, double amount) {
        double progress = Math.clamp(amount, 0.0, 1.0);
        return TextColor.color(
                (int) Math.round(from.red() + (to.red() - from.red()) * progress),
                (int) Math.round(from.green() + (to.green() - from.green()) * progress),
                (int) Math.round(from.blue() + (to.blue() - from.blue()) * progress)
        );
    }

    private static Component host(String name) {
        return Component.object(ObjectContents.playerHead(name))
                .color(NamedTextColor.WHITE)
                .append(Component.space())
                .append(Component.text(name, NamedTextColor.WHITE));
    }

    private static Component sprite(String atlas, String sprite) {
        return Component.object(ObjectContents.sprite(Key.key(atlas), Key.key(sprite)))
                .color(NamedTextColor.WHITE);
    }

    private static TextComponent line(int length) {
        return Component.text(" ".repeat(length), HardlandsColor.LIGHT_GRAY)
                .decorate(TextDecoration.STRIKETHROUGH);
    }

    public record HeaderLayout(Component prefix, Component suffix) {

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

        private Component component(TextColor fallbackColor) {
            return Component.text(this.symbol, this.color == null ? fallbackColor : this.color);
        }
    }
}
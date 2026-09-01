package team.heather.hardlands.ui.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.ui.HardlandsColor;

public final class ChatMessenger {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String FORMAT = "<dark_gray>[%s<dark_gray>] <gray>» <white>%s";

    private ChatMessenger() {}

    public static void broadcastFramed(String icon, HardlandsColor colors, String message) {
        Component component = Component.newline()
                .append(Component.newline())
                .append(Component.newline())
                .append(frame(icon, colors))
                .append(Component.newline())
                .append(MINI_MESSAGE.deserialize("<white>" + message))
                .append(Component.newline())
                .append(bottomFrame(colors));

        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(component));
    }

    public static void broadcast(String message) {
        Component component = MINI_MESSAGE.deserialize(FORMAT.formatted(Hardlands.LABEL, message));
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(component));
    }

    public static void send(Player player, String message) {
        player.sendMessage(MINI_MESSAGE.deserialize(FORMAT.formatted(Hardlands.LABEL, message)));
    }

    private static Component frame(String icon, HardlandsColor colors) {
        return Component.text("♢", colors.primary())
                .append(Component.text("»", colors.tertiary()))
                .append(line(30, colors.tertiary()))
                .append(Component.text("{ ", colors.tertiary()))
                .append(Component.text(icon, colors.primary()))
                .append(Component.text(" ", colors.secondary()))
                .append(Component.text("}", colors.tertiary()))
                .append(line(30, colors.tertiary()))
                .append(Component.text("«", colors.tertiary()))
                .append(Component.text("♢", colors.primary()));
    }

    private static Component bottomFrame(HardlandsColor colors) {
        return Component.text("♢", colors.primary())
                .append(Component.text("»", colors.tertiary()))
                .append(line(66, colors.tertiary()))
                .append(Component.text("«", colors.tertiary()))
                .append(Component.text("♢", colors.primary()));
    }

    private static TextComponent line(int length, net.kyori.adventure.text.format.TextColor color) {
        return Component.text(" ".repeat(length), color).decorate(TextDecoration.STRIKETHROUGH);
    }
}
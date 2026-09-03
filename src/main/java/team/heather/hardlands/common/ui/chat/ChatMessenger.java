package team.heather.hardlands.common.ui.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.common.ui.HardlandsColor;
import team.heather.hardlands.util.TextFormatters;

public final class ChatMessenger {

    private static final int FRAME_LENGTH = 26;
    private static final String FRAME_LINE = "━".repeat(FRAME_LENGTH);
    private static final Component PREFIX = Component.text("[", NamedTextColor.DARK_GRAY)
            .append(Hardlands.LABEL)
            .append(Component.text("]", NamedTextColor.DARK_GRAY))
            .append(Component.text(" » ", NamedTextColor.GRAY));

    private ChatMessenger() {}

    public static void send(Player player, String message) {
        player.sendMessage(format(message));
    }

    public static void broadcast(Component component) {
        Bukkit.getOnlinePlayers().forEach(player ->
                player.sendMessage(component));
    }

    public static void broadcast(String message) {
        broadcast(format(message));
    }

    public static void broadcastFramed(String icon, HardlandsColor colors, String message) {
        broadcast(framed(icon, colors, message));
    }

    private static Component format(String message) {
        return PREFIX.append(TextFormatters.MINI_MESSAGE.format("<white>" + message));
    }

    private static Component framed(
            String icon,
            HardlandsColor colors,
            String message
    ) {
        return Component.newline()
                .append(topFrame(icon, colors))
                .append(Component.newline())
                .append(Component.text("  "))
                .append(TextFormatters.MINI_MESSAGE.format(
                        "<white>" + indent(message)
                ))
                .append(Component.newline())
                .append(bottomFrame(colors))
                .append(Component.newline());
    }

    private static Component topFrame(
            String icon,
            HardlandsColor colors
    ) {
        return Component.text("♢", colors.primary())
                .append(Component.text("»", colors.secondary()))
                .append(Component.text(FRAME_LINE, colors.tertiary()))
                .append(Component.text("〔 ", colors.tertiary()))
                .append(Component.text(icon, colors.primary()))
                .append(Component.text(" ʜᴀʀᴅʟᴀɴᴅꜱ ", colors.secondary()))
                .append(Component.text("〕", colors.tertiary()))
                .append(Component.text(FRAME_LINE, colors.tertiary()))
                .append(Component.text("«", colors.secondary()))
                .append(Component.text("♢", colors.primary()));
    }

    private static Component bottomFrame(HardlandsColor colors) {
        return Component.text("♢", colors.primary())
                .append(Component.text("»", colors.secondary()))
                .append(Component.text(
                        "━".repeat(FRAME_LENGTH * 2 + 15),
                        colors.tertiary()
                ))
                .append(Component.text("«", colors.secondary()))
                .append(Component.text("♢", colors.primary()));
    }

    private static String indent(String message) {
        return message.stripIndent()
                .strip()
                .replace("\n", "\n  ");
    }
}
package team.heather.hardlands.ui.feedback;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import team.heather.hardlands.Hardlands;

public final class ChatMessenger {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final String FORMAT = "<dark_gray>[%s<dark_gray>] <gray>» <white>%s";

    private static final String FRAMED_FORMAT = """
            <#e5383b>♢<#800E13>»%s{ <#e5383b>❣ <#8c2f39>☠ <#e5383b>❣ <#800E13>}%s«<#e5383b>♢
            <white>%s
            <#e5383b>♢<#800E13>»%s«<#e5383b>♢
            """;

    private ChatMessenger() {}

    public static void broadcastFramed(String message) {
        Component component = MINI_MESSAGE.deserialize(FRAMED_FORMAT.formatted(
                line(30),
                line(30),
                message,
                line(71)
        ));

        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(component));
    }

    public static void broadcast(String message) {
        Component component = MINI_MESSAGE.deserialize(FORMAT.formatted(Hardlands.LABEL, message));
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(component));
    }

    public static void send(Player player, String message) {
        player.sendMessage(MINI_MESSAGE.deserialize(FORMAT.formatted(Hardlands.LABEL, message)));
    }

    private static String line(int length) {
        return "<st>" + " ".repeat(length) + "<!st>";
    }
}
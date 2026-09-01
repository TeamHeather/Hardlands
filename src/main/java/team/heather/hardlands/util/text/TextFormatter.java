package team.heather.hardlands.util.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import team.heather.hardlands.ui.HardlandsColor;

public final class TextFormatter {

    private static final Pattern HIGHLIGHT_PATTERN = Pattern.compile("\\{([^{}]+)}|\\[([^\\[\\]]+)]");

    private TextFormatter() {}

    public static String formatUsername(Player player) {
        return "<white><head:%s></white> %s".formatted(player.getUniqueId(), player.getName());
    }

    public static String formatUsernameAtText(String text, Player player) {
        return text.replace(player.getName(), formatUsername(player));
    }

    public static Component parse(String text) {
        return MiniMessage.miniMessage().deserialize(text);
    }

    public static String toPlainText(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    public static Component formatHighlighted(String text) {
        TextComponent.Builder result = Component.text().color(HardlandsColor.LIGHT_GRAY);

        Matcher matcher = HIGHLIGHT_PATTERN.matcher(text);
        int position = 0;
        while (matcher.find()) {
            result.append(parse(text.substring(position, matcher.start())));

            String primary = matcher.group(1);
            String highlighted = primary != null ? primary : matcher.group(2);

            result.append(Component.text(highlighted, primary != null
                    ? HardlandsColor.HARDLANDS
                    : NamedTextColor.WHITE));

            position = matcher.end();
        }

        result.append(parse(text.substring(position)));
        return result.build();
    }

    public static Component tinyCaps(String text) {
        return Component.text(TinyCaps.format(text), HardlandsColor.HARDLANDS);
    }
}
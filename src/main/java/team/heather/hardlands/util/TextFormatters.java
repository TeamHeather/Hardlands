package team.heather.hardlands.util;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import team.heather.hardlands.feature.ui.HardlandsColor;

public final class TextFormatters {

    public static final DurationFormatter DURATION = new DurationFormatter();
    public static final HighlightFormatter HIGHLIGHT = new HighlightFormatter();
    public static final LocalTimeFormatter LOCAL_TIME = new LocalTimeFormatter();
    public static final MiniMessageFormatter MINI_MESSAGE = new MiniMessageFormatter();
    public static final PlainTextFormatter PLAIN_TEXT = new PlainTextFormatter();
    public static final RomanNumeralFormatter ROMAN_NUMERAL = new RomanNumeralFormatter();
    public static final TinyCapsFormatter TINY_CAPS = new TinyCapsFormatter();
    public static final UsernameFormatter USERNAME = new UsernameFormatter();

    private TextFormatters() {}

    public static final class DurationFormatter implements Formatter<Duration, String> {

        private DurationFormatter() {}

        @Override
        public String format(Duration duration) {
            long totalSeconds = Math.max(0L, duration.toSeconds());
            long hours = totalSeconds / 3_600;
            long minutes = totalSeconds % 3_600 / 60;
            long seconds = totalSeconds % 60;

            List<String> parts = new ArrayList<>(3);

            if (hours > 0) {
                parts.add(hours + "ʜ");
            }

            if (minutes > 0) {
                parts.add(minutes + "ᴍ");
            }

            if (seconds > 0 || parts.isEmpty()) {
                parts.add(seconds + "ꜱ");
            }

            return String.join(" ", parts);
        }
    }

    @FunctionalInterface
    public interface Formatter<I, O> {

        O format(I input);
    }

    public static final class HighlightFormatter implements Formatter<String, Component> {

        private static final Pattern HIGHLIGHT_PATTERN = Pattern.compile("\\{([^{}]+)}|\\[([^\\[\\]]+)]");

        private HighlightFormatter() {}

        @Override
        public Component format(String text) {
            TextComponent.Builder result = Component.text().color(HardlandsColor.LIGHT_GRAY);

            Matcher matcher = HIGHLIGHT_PATTERN.matcher(text);
            int position = 0;

            while (matcher.find()) {
                result.append(MINI_MESSAGE.format(text.substring(position, matcher.start())));

                String primary = matcher.group(1);
                String highlighted = primary != null
                        ? primary
                        : matcher.group(2);

                result.append(Component.text(
                        highlighted,
                        primary != null
                                ? HardlandsColor.HARDLANDS
                                : NamedTextColor.WHITE));

                position = matcher.end();
            }

            result.append(MINI_MESSAGE.format(text.substring(position)));

            return result.build();
        }
    }

    public static final class LocalTimeFormatter implements Formatter<LocalTime, String> {

        private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
        private static final DateTimeFormatter HHMMSS = DateTimeFormatter.ofPattern("HH:mm:ss");

        private LocalTimeFormatter() {}

        @Override
        public String format(LocalTime time) {
            return HHMM.format(time);
        }

        public String formatWithSeconds(LocalTime time) {
            return HHMMSS.format(time);
        }

        public LocalTime parse(String text) {
            return LocalTime.parse(text, HHMM);
        }

        public LocalTime parseWithSeconds(String text) {
            return LocalTime.parse(text, HHMMSS);
        }
    }

    public static final class MiniMessageFormatter implements Formatter<String, Component> {

        private MiniMessageFormatter() {}

        @Override
        public Component format(String text) {
            return MiniMessage.miniMessage().deserialize(text);
        }
    }

    public static final class PlainTextFormatter implements Formatter<Component, String> {

        private PlainTextFormatter() {}

        @Override
        public String format(Component component) {
            return PlainTextComponentSerializer.plainText().serialize(component);
        }
    }

    public static final class RomanNumeralFormatter implements Formatter<Integer, String> {

        private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};

        private RomanNumeralFormatter() {}

        @Override
        public String format(Integer number) {
            if (number == null || number <= 0) {
                throw new IllegalArgumentException("Number must be greater than 0");
            }

            StringBuilder result = new StringBuilder();
            int remaining = number;

            for (int index = 0; index < VALUES.length; index++) {
                while (remaining >= VALUES[index]) {
                    remaining -= VALUES[index];
                    result.append(SYMBOLS[index]);
                }
            }

            return result.toString();
        }
    }

    public static final class TinyCapsFormatter implements Formatter<String, String> {

        private static final Map<Character, Character> CHARACTERS = Map.ofEntries(
                Map.entry('a', 'ᴀ'),
                Map.entry('b', 'ʙ'),
                Map.entry('c', 'ᴄ'),
                Map.entry('d', 'ᴅ'),
                Map.entry('e', 'ᴇ'),
                Map.entry('f', 'ꜰ'),
                Map.entry('g', 'ɢ'),
                Map.entry('h', 'ʜ'),
                Map.entry('i', 'ɪ'),
                Map.entry('j', 'ᴊ'),
                Map.entry('k', 'ᴋ'),
                Map.entry('l', 'ʟ'),
                Map.entry('m', 'ᴍ'),
                Map.entry('n', 'ɴ'),
                Map.entry('o', 'ᴏ'),
                Map.entry('p', 'ᴘ'),
                Map.entry('q', 'ǫ'),
                Map.entry('r', 'ʀ'),
                Map.entry('s', 'ꜱ'),
                Map.entry('t', 'ᴛ'),
                Map.entry('u', 'ᴜ'),
                Map.entry('v', 'ᴠ'),
                Map.entry('w', 'ᴡ'),
                Map.entry('x', 'x'),
                Map.entry('y', 'ʏ'),
                Map.entry('z', 'ᴢ')
        );

        private static final Pattern LOWERCASE_TEXT = Pattern.compile("[a-z]+");

        private TinyCapsFormatter() {}

        public Component format(Component component) {
            return component.replaceText(config -> config
                    .match(LOWERCASE_TEXT)
                    .replacement((match, builder) ->
                            builder.content(format(match.group()))));
        }

        @Override
        public String format(String text) {
            StringBuilder result = new StringBuilder(text.length());

            for (char character : text.toCharArray()) {
                result.append(CHARACTERS.getOrDefault(character, character));
            }

            return result.toString();
        }

        public Component formatColored(String text) {
            return Component.text(format(text), HardlandsColor.HARDLANDS);
        }
    }

    public static final class UsernameFormatter implements Formatter<Player, String> {

        private UsernameFormatter() {}

        public String format(String text, Player player) {
            return text.replace(player.getName(), format(player));
        }

        @Override
        public String format(Player player) {
            return "<white><head:%s></white> %s".formatted(
                    player.getUniqueId(),
                    player.getName()
            );
        }
    }
}
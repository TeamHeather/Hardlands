package team.heather.hardlands.util.text;

import java.util.Map;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;

public final class TinyCaps {

    private static final Pattern LOWERCASE_TEXT = Pattern.compile("[a-z]+");
    private static final Map<Character, Character> MAP = Map.ofEntries(
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
            Map.entry('z', 'ᴢ'));

    private TinyCaps() {}

    public static Component format(Component component) {
        return component.replaceText(config -> config
            .match(LOWERCASE_TEXT)
            .replacement((match, builder) -> builder.content(format(match.group()))));
    }

    public static String format(String text) {
        StringBuilder result = new StringBuilder(text.length());

        for (char character : text.toCharArray()) {
            result.append(MAP.getOrDefault(character, character));
        }

        return result.toString();
    }
}

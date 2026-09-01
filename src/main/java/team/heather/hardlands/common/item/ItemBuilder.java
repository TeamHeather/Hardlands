package team.heather.hardlands.common.item;

import java.util.*;
import java.util.function.Function;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import team.heather.hardlands.internal.data.PersistentDataAccess;
import team.heather.hardlands.util.TextFormatters;

// Vaya puta mierda
public final class ItemBuilder {

    private static final ComponentFlattener COMPONENT_FLATTENER = ComponentFlattener.basic();
    private static final NamespacedKey ID_KEY = new NamespacedKey("hardlands", "id");
    private static final int MAX_LORE_LINE_LENGTH = 40;

    private final ItemStack item;

    public ItemBuilder(Material material) {
        this.item = ItemStack.of(material);
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
    }

    public ItemBuilder addFormattedLore(String... lines) {
        return this.addLore(
                lines,
                TextFormatters.HIGHLIGHT::format
        );
    }

    public ItemBuilder addLore(Component... lines) {
        List<Component> lore = this.currentLore();

        for (Component line : lines) {
            lore.add(nonItalic(line));
        }

        return this.setLore(lore);
    }

    public ItemBuilder addLore(String... lines) {
        return this.addLore(
                lines,
                TextFormatters.MINI_MESSAGE::format
        );
    }

    public ItemBuilder addLoreFirst(Component... lines) {
        List<Component> lore = new ArrayList<>();

        for (Component line : lines) {
            lore.add(nonItalic(line));
        }

        lore.addAll(this.currentLore());

        return this.setLore(lore);
    }

    public ItemBuilder addLoreFirst(String... lines) {
        return this.addLoreFirst(
                lines,
                TextFormatters.MINI_MESSAGE::format
        );
    }

    public ItemBuilder addTinyCapsLore(String... lines) {
        return this.addLore(
                lines,
                TextFormatters.TINY_CAPS::formatColored
        );
    }

    public ItemStack build() {
        return this.item.clone();
    }

    public ItemBuilder enchant(
            Enchantment enchantment,
            int level
    ) {
        this.item.addUnsafeEnchantment(enchantment, level);

        return this;
    }

    public Optional<String> findId() {
        return this.getData(
                ID_KEY,
                PersistentDataType.STRING
        );
    }

    public ItemBuilder formattedLore(String... lines) {
        return this.setLore(
                lines,
                TextFormatters.HIGHLIGHT::format
        );
    }

    public ItemBuilder formattedLore(
            TextColor highlightColor,
            String... lines
    ) {
        return this.setLore(
                lines,
                line -> TextFormatters.HIGHLIGHT.format(
                        line,
                        highlightColor
                )
        );
    }

    public ItemBuilder footerLore(String... lines) {
        List<Component> lore = this.currentLore();

        for (String line : lines) {
            lore.add(Component.text(
                    line.toLowerCase(Locale.ROOT),
                    NamedTextColor.GRAY
            ));
        }

        return this.setLore(lore);
    }

    public <P, C> Optional<C> getData(
            NamespacedKey key,
            PersistentDataType<P, C> type
    ) {
        return PersistentDataAccess.find(
                this.item.getItemMeta(),
                key,
                type
        );
    }

    public ItemBuilder glint(boolean glint) {
        this.item.setData(
                DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE,
                glint
        );

        return this;
    }

    public ItemBuilder hideTooltip(
            DataComponentType... components
    ) {
        TooltipDisplay currentDisplay =
                this.item.getData(DataComponentTypes.TOOLTIP_DISPLAY);

        TooltipDisplay.Builder builder =
                TooltipDisplay.tooltipDisplay();

        if (currentDisplay != null) {
            builder.hideTooltip(currentDisplay.hideTooltip());
            builder.hiddenComponents(
                    currentDisplay.hiddenComponents()
            );
        }

        this.item.setData(
                DataComponentTypes.TOOLTIP_DISPLAY,
                builder.addHiddenComponents(components).build()
        );

        return this;
    }

    public ItemBuilder instantAttack() {
        this.item.setData(
                DataComponentTypes.MINIMUM_ATTACK_CHARGE,
                0.0F
        );

        return this;
    }

    public ItemBuilder lore(String... lines) {
        return this.setLore(
                lines,
                TextFormatters.MINI_MESSAGE::format
        );
    }

    public ItemBuilder name(Component name) {
        this.item.setData(
                DataComponentTypes.CUSTOM_NAME,
                nonItalic(name)
        );

        return this;
    }

    public ItemBuilder name(String name) {
        return this.name(
                TextFormatters.MINI_MESSAGE.format(name)
        );
    }

    public ItemBuilder profile(PlayerProfile profile) {
        if (!(this.item.getItemMeta() instanceof SkullMeta skullMeta)) {
            throw new IllegalStateException(
                    "Player profiles can only be applied to player heads"
            );
        }

        skullMeta.setPlayerProfile(profile);
        this.item.setItemMeta(skullMeta);

        return this;
    }

    public <P, C> ItemBuilder setData(
            NamespacedKey key,
            PersistentDataType<P, C> type,
            C value
    ) {
        this.item.editMeta(meta ->
                PersistentDataAccess.set(
                        meta,
                        key,
                        type,
                        value
                )
        );

        return this;
    }

    public ItemBuilder setId(String id) {
        return this.setData(
                ID_KEY,
                PersistentDataType.STRING,
                id
        );
    }

    public ItemBuilder skullOwner(String owner) {
        this.item.editMeta(
                SkullMeta.class,
                meta -> meta.setPlayerProfile(
                        Bukkit.createProfile(owner)
                )
        );

        return this.hideTooltip(
                DataComponentTypes.PROFILE
        );
    }

    public ItemBuilder tinyCapsLore(String... lines) {
        return this.setLore(
                lines,
                TextFormatters.TINY_CAPS::formatColored
        );
    }

    public ItemBuilder unbreakable() {
        this.item.editMeta(meta ->
                meta.setUnbreakable(true)
        );

        return this;
    }

    private ItemBuilder addLore(
            String[] lines,
            Function<String, Component> formatter
    ) {
        List<Component> lore = this.currentLore();

        lore.addAll(formatLore(lines, formatter));

        return this.setLore(lore);
    }

    private ItemBuilder addLoreFirst(
            String[] lines,
            Function<String, Component> formatter
    ) {
        List<Component> lore =
                new ArrayList<>(formatLore(lines, formatter));

        lore.addAll(this.currentLore());

        return this.setLore(lore);
    }

    private List<Component> currentLore() {
        ItemLore lore =
                this.item.getData(DataComponentTypes.LORE);

        if (lore == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(lore.lines());
    }

    private ItemBuilder setLore(List<Component> lore) {
        this.item.setData(
                DataComponentTypes.LORE,
                ItemLore.lore(lore)
        );

        return this;
    }

    private ItemBuilder setLore(
            String[] lines,
            Function<String, Component> formatter
    ) {
        return this.setLore(
                formatLore(lines, formatter)
        );
    }

    private static void appendWord(
            List<Component> lines,
            List<StyledCodePoint> currentLine,
            List<StyledCodePoint> word,
            boolean spaceBeforeWord
    ) {
        if (word.isEmpty()) {
            return;
        }

        boolean addSpace =
                spaceBeforeWord && !currentLine.isEmpty();

        int requiredLength =
                word.size() + (addSpace ? 1 : 0);

        if (!currentLine.isEmpty()
                && currentLine.size() + requiredLength
                > MAX_LORE_LINE_LENGTH) {
            flushLine(lines, currentLine);
            addSpace = false;
        }

        if (addSpace) {
            currentLine.add(
                    new StyledCodePoint(
                            ' ',
                            word.getFirst().styles()
                    )
            );
        }

        while (!word.isEmpty()) {
            int available =
                    MAX_LORE_LINE_LENGTH
                            - currentLine.size();

            if (available == 0) {
                flushLine(lines, currentLine);
                available = MAX_LORE_LINE_LENGTH;
            }

            int length = Math.min(
                    available,
                    word.size()
            );

            currentLine.addAll(
                    word.subList(0, length)
            );

            word.subList(0, length).clear();

            if (!word.isEmpty()) {
                flushLine(lines, currentLine);
            }
        }
    }

    private static Component applyStyles(
            String text,
            List<Style> styles
    ) {
        Component component = Component.text(text);

        for (int index = styles.size() - 1;
             index >= 0;
             index--) {
            component = Component.empty()
                    .style(styles.get(index))
                    .append(component);
        }

        return component;
    }

    private static Component buildComponent(
            List<StyledCodePoint> characters
    ) {
        if (characters.isEmpty()) {
            return Component.empty();
        }

        Component result = Component.empty();
        int start = 0;

        while (start < characters.size()) {
            StyledCodePoint first =
                    characters.get(start);

            int end = start + 1;

            while (end < characters.size()
                    && first.styles().equals(
                    characters.get(end).styles()
            )) {
                end++;
            }

            StringBuilder text =
                    new StringBuilder(end - start);

            for (int index = start;
                 index < end;
                 index++) {
                text.appendCodePoint(
                        characters.get(index).codePoint()
                );
            }

            result = result.append(
                    applyStyles(
                            text.toString(),
                            first.styles()
                    )
            );

            start = end;
        }

        return result;
    }

    private static List<StyledCodePoint> flatten(
            Component component
    ) {
        List<StyledCodePoint> characters =
                new ArrayList<>();

        Deque<Style> styles =
                new ArrayDeque<>();

        COMPONENT_FLATTENER.flatten(
                component,
                new FlattenerListener() {

                    @Override
                    public void component(String text) {
                        List<Style> activeStyles =
                                List.copyOf(styles);

                        text.codePoints().forEach(codePoint ->
                                characters.add(
                                        new StyledCodePoint(
                                                codePoint,
                                                activeStyles
                                        )
                                )
                        );
                    }

                    @Override
                    public void popStyle(Style style) {
                        styles.removeLast();
                    }

                    @Override
                    public void pushStyle(Style style) {
                        styles.addLast(style);
                    }
                }
        );

        return characters;
    }

    private static void flushLine(
            List<Component> lines,
            List<StyledCodePoint> characters
    ) {
        lines.add(buildComponent(characters));
        characters.clear();
    }

    private static List<Component> formatLore(
            String[] lines,
            Function<String, Component> formatter
    ) {
        List<Component> result = new ArrayList<>();

        for (String line : lines) {
            Component component = formatter.apply(line);

            for (Component wrappedLine : wrapLore(component)) {
                result.add(nonItalic(wrappedLine));
            }
        }

        return result;
    }

    private static boolean isLineBreak(int codePoint) {
        return codePoint == '\n'
                || codePoint == '\r';
    }

    private static Component nonItalic(Component component) {
        return component.decoration(
                TextDecoration.ITALIC,
                false
        );
    }

    private static List<Component> wrapLore(
            Component component
    ) {
        List<StyledCodePoint> characters =
                flatten(component);

        if (characters.isEmpty()) {
            return List.of(Component.empty());
        }

        List<Component> lines = new ArrayList<>();
        List<StyledCodePoint> currentLine =
                new ArrayList<>();
        List<StyledCodePoint> currentWord =
                new ArrayList<>();

        boolean spaceBeforeWord = false;

        for (StyledCodePoint character : characters) {
            int codePoint = character.codePoint();

            if (isLineBreak(codePoint)) {
                appendWord(
                        lines,
                        currentLine,
                        currentWord,
                        spaceBeforeWord
                );

                flushLine(lines, currentLine);
                spaceBeforeWord = false;

                continue;
            }

            if (Character.isWhitespace(codePoint)) {
                appendWord(
                        lines,
                        currentLine,
                        currentWord,
                        spaceBeforeWord
                );

                spaceBeforeWord =
                        !currentLine.isEmpty();

                continue;
            }

            currentWord.add(character);
        }

        appendWord(
                lines,
                currentLine,
                currentWord,
                spaceBeforeWord
        );

        if (!currentLine.isEmpty()) {
            flushLine(lines, currentLine);
        }

        return lines.isEmpty()
                ? List.of(Component.empty())
                : lines;
    }

    private record StyledCodePoint(
            int codePoint,
            List<Style> styles
    ) {}
}
package team.heather.hardlands.common.item;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import com.destroystokyo.paper.profile.PlayerProfile;
import team.heather.hardlands.internal.PersistentDataAccess;
import team.heather.hardlands.util.TextFormatters;

/**
 * Fluent builder for creating and modifying ItemStack instances with support
 * for custom names, lore, enchantments, enchantment glints, persistent data,
 * and more. This class provides a clean and intuitive API for item
 * construction that can be used across any Minecraft plugin.
 *
 * <p>Usage example:</p>
 * <pre>
 * ItemStack sword = new ItemBuilder(Material.DIAMOND_SWORD)
 *     .name("<gold>Legendary Sword")
 *     .addLore("A powerful weapon", "Sharpness V")
 *     .enchant(Enchantment.SHARPNESS, 5)
 *     .unbreakable()
 *     .build();
 * </pre>
 *
 * @author Hardlands Team
 * @since 1.0.0
 */
public final class ItemBuilder {

    private static final ComponentFlattener COMPONENT_FLATTENER = ComponentFlattener.basic();
    private static final NamespacedKey ID_KEY = new NamespacedKey("hardlands", "id");
    private static final int MAX_LORE_LINE_LENGTH = 40;
    private static final int MIN_ENCHANTMENT_LEVEL = 1;
    private static final int MAX_CUSTOM_MODEL_DATA = 0x7FFFFFFF;

    private final ItemStack item;


    /**
     * Creates a new ItemBuilder with the specified material.
     *
     * @param material the material for the new item
     * @throws NullPointerException if material is null
     */
    public ItemBuilder(Material material) {
        Objects.requireNonNull(material, "Material cannot be null");
        this.item = ItemStack.of(material);
    }

    /**
     * Creates a new ItemBuilder from an existing ItemStack. The provided
     * ItemStack is cloned to ensure independence.
     *
     * @param item the item to clone
     * @throws NullPointerException if item is null
     */
    public ItemBuilder(ItemStack item) {
        Objects.requireNonNull(item, "ItemStack cannot be null");
        this.item = item.clone();
    }

    // ============================================================================
    // NAME & DISPLAY METHODS
    // ============================================================================

    /**
     * Sets the display name of this item using MiniMessage format.
     *
     * @param name the display name in MiniMessage format
     * @return this builder for chaining
     * @throws NullPointerException if name is null
     */
    public ItemBuilder name(String name) {
        Objects.requireNonNull(name, "Name cannot be null");
        return this.name(TextFormatters.MINI_MESSAGE.format(name));
    }

    /**
     * Sets the display name of this item using a pre-formatted Component.
     *
     * @param name the display name component
     * @return this builder for chaining
     * @throws NullPointerException if name is null
     */
    public ItemBuilder name(Component name) {
        Objects.requireNonNull(name, "Name component cannot be null");
        this.item.setData(
                DataComponentTypes.CUSTOM_NAME,
                nonItalic(name)
        );
        return this;
    }

    /**
     * Sets the amount of items in this stack.
     *
     * @param amount the amount (1-64)
     * @return this builder for chaining
     * @throws IllegalArgumentException if amount is out of valid range
     */
    public ItemBuilder amount(int amount) {
        if (amount < 1 || amount > 64) {
            throw new IllegalArgumentException(
                    "Amount must be between 1 and 64, got: " + amount
            );
        }
        this.item.setAmount(amount);
        return this;
    }

    // ============================================================================
    // LORE METHODS
    // ============================================================================

    /**
     * Gets the current lore as a mutable list. Modifications to the returned
     * list will not affect the item until {@link #setLore(List)} is called.
     *
     * @return a new list containing the current lore, or an empty list
     */
    public List<Component> currentLore() {
        ItemLore lore = this.item.getData(DataComponentTypes.LORE);

        if (lore == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(lore.lines());
    }

    /**
     * Sets the lore, replacing any existing lore.
     *
     * @param lore the new lore lines
     * @return this builder for chaining
     * @throws NullPointerException if lore is null
     */
    public ItemBuilder setLore(List<Component> lore) {
        Objects.requireNonNull(lore, "Lore cannot be null");
        this.item.setData(
                DataComponentTypes.LORE,
                ItemLore.lore(lore)
        );
        return this;
    }

    /**
     * Adds lore lines formatted with MiniMessage at the end.
     *
     * @param lines the lore lines in MiniMessage format
     * @return this builder for chaining
     */
    public ItemBuilder addLore(String... lines) {
        return this.addLore(
                lines,
                TextFormatters.MINI_MESSAGE::format
        );
    }

    /**
     * Adds pre-formatted Component lore lines at the end.
     *
     * @param lines the lore components
     * @return this builder for chaining
     */
    public ItemBuilder addLore(Component... lines) {
        List<Component> lore = this.currentLore();

        for (Component line : lines) {
            lore.add(nonItalic(line));
        }

        return this.setLore(lore);
    }

    /**
     * Adds lore lines formatted with the highlight formatter at the end.
     *
     * @param lines the lore lines to highlight
     * @return this builder for chaining
     */
    public ItemBuilder addFormattedLore(String... lines) {
        return this.addLore(
                lines,
                TextFormatters.HIGHLIGHT::format
        );
    }

    /**
     * Adds lore lines formatted with tiny caps at the end.
     *
     * @param lines the lore lines in tiny caps format
     * @return this builder for chaining
     */
    public ItemBuilder addTinyCapsLore(String... lines) {
        return this.addLore(
                lines,
                TextFormatters.TINY_CAPS::formatColored
        );
    }

    /**
     * Adds lore lines formatted with MiniMessage at the beginning.
     *
     * @param lines the lore lines in MiniMessage format
     * @return this builder for chaining
     */
    public ItemBuilder addLoreFirst(String... lines) {
        return this.addLoreFirst(
                lines,
                TextFormatters.MINI_MESSAGE::format
        );
    }

    /**
     * Adds pre-formatted Component lore lines at the beginning.
     *
     * @param lines the lore components
     * @return this builder for chaining
     */
    public ItemBuilder addLoreFirst(Component... lines) {
        List<Component> lore = new ArrayList<>();

        for (Component line : lines) {
            lore.add(nonItalic(line));
        }

        lore.addAll(this.currentLore());

        return this.setLore(lore);
    }

    /**
     * Sets lore lines, replacing any existing lore, using MiniMessage format.
     *
     * @param lines the lore lines in MiniMessage format
     * @return this builder for chaining
     */
    public ItemBuilder lore(String... lines) {
        return this.setLore(
                lines,
                TextFormatters.MINI_MESSAGE::format
        );
    }

    /**
     * Sets lore lines using the highlight formatter, replacing any existing
     * lore.
     *
     * @param lines the lore lines to highlight
     * @return this builder for chaining
     */
    public ItemBuilder formattedLore(String... lines) {
        return this.setLore(
                lines,
                TextFormatters.HIGHLIGHT::format
        );
    }

    /**
     * Sets lore lines using the highlight formatter with a custom color,
     * replacing any existing lore.
     *
     * @param highlightColor the color for the highlight
     * @param lines the lore lines to highlight
     * @return this builder for chaining
     * @throws NullPointerException if highlightColor is null
     */
    public ItemBuilder formattedLore(
            TextColor highlightColor,
            String... lines
    ) {
        Objects.requireNonNull(highlightColor, "Color cannot be null");
        return this.setLore(
                lines,
                line -> TextFormatters.HIGHLIGHT.format(
                        line,
                        highlightColor
                )
        );
    }

    /**
     * Sets lore lines using tiny caps format, replacing any existing lore.
     *
     * @param lines the lore lines in tiny caps format
     * @return this builder for chaining
     */
    public ItemBuilder tinyCapsLore(String... lines) {
        return this.setLore(
                lines,
                TextFormatters.TINY_CAPS::formatColored
        );
    }

    /**
     * Adds footer lines formatted as gray text at the end of the lore.
     *
     * @param lines the footer lines
     * @return this builder for chaining
     */
    public ItemBuilder addFooterLore(String... lines) {
        List<Component> lore = this.currentLore();

        for (String line : lines) {
            lore.add(Component.text(
                    line.toLowerCase(Locale.ROOT),
                    NamedTextColor.GRAY
            ));
        }

        return this.setLore(lore);
    }

    // ============================================================================
    // ENCHANTMENT METHODS
    // ============================================================================

    /**
     * Adds an enchantment to this item with the specified level.
     *
     * @param enchantment the enchantment to add
     * @param level the enchantment level
     * @return this builder for chaining
     * @throws NullPointerException if enchantment is null
     * @throws IllegalArgumentException if level is less than 1
     */
    public ItemBuilder enchant(
            Enchantment enchantment,
            int level
    ) {
        Objects.requireNonNull(enchantment, "Enchantment cannot be null");
        if (level < MIN_ENCHANTMENT_LEVEL) {
            throw new IllegalArgumentException(
                    "Enchantment level must be at least 1, got: " + level
            );
        }

        this.item.addUnsafeEnchantment(enchantment, level);
        return this;
    }

    /**
     * Removes an enchantment from this item.
     *
     * @param enchantment the enchantment to remove
     * @return this builder for chaining
     * @throws NullPointerException if enchantment is null
     */
    public ItemBuilder removeEnchantment(Enchantment enchantment) {
        Objects.requireNonNull(enchantment, "Enchantment cannot be null");
        this.item.removeEnchantment(enchantment);
        return this;
    }

    /**
     * Clears all enchantments from this item.
     *
     * @return this builder for chaining
     */
    public ItemBuilder clearEnchantments() {
        this.item.getEnchantments()
                .keySet()
                .forEach(this.item::removeEnchantment);
        return this;
    }

    /**
     * Sets whether this item has the enchantment glint effect.
     *
     * @param glint true to show the glint, false to hide it
     * @return this builder for chaining
     */
    public ItemBuilder glint(boolean glint) {
        this.item.setData(
                DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE,
                glint
        );
        return this;
    }

    // ============================================================================
    // UNBREAKABILITY & DURABILITY METHODS
    // ============================================================================

    /**
     * Marks this item as unbreakable.
     *
     * @return this builder for chaining
     */
    public ItemBuilder unbreakable() {
        this.item.editMeta(meta ->
                meta.setUnbreakable(true)
        );
        return this;
    }

    /**
     * Marks this item as breakable.
     *
     * @return this builder for chaining
     */
    public ItemBuilder breakable() {
        this.item.editMeta(meta ->
                meta.setUnbreakable(false)
        );
        return this;
    }

    // ============================================================================
    // APPEARANCE METHODS
    // ============================================================================

    /**
     * Sets custom model data for this item.
     *
     * @param modelData the custom model data value
     * @return this builder for chaining
     * @throws IllegalArgumentException if modelData is invalid
     */
    public ItemBuilder customModelData(int modelData) {
        if (modelData < 0 || modelData > MAX_CUSTOM_MODEL_DATA) {
            throw new IllegalArgumentException(
                    String.format(
                            "Custom model data must be between 0 and %d, got: %d",
                            MAX_CUSTOM_MODEL_DATA,
                            modelData
                    )
            );
        }

        this.item.editMeta(meta -> {
            ItemMeta itemMeta = Objects.requireNonNull(meta);
            itemMeta.setCustomModelData(modelData);
        });

        return this;
    }

    /**
     * Hides specific tooltips from this item.
     *
     * @param components the tooltip components to hide
     * @return this builder for chaining
     */
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

    /**
     * Hides all item flags (e.g., enchantments, attributes).
     *
     * @return this builder for chaining
     */
    public ItemBuilder hideFlags() {
        this.item.editMeta(meta -> {
            ItemMeta itemMeta = Objects.requireNonNull(meta);
            for (ItemFlag flag : ItemFlag.values()) {
                itemMeta.addItemFlags(flag);
            }
        });
        return this;
    }

    /**
     * Sets minimum attack charge speed for this item (instant attack).
     *
     * @return this builder for chaining
     */
    public ItemBuilder instantAttack() {
        this.item.setData(
                DataComponentTypes.MINIMUM_ATTACK_CHARGE,
                0.0F
        );
        return this;
    }

    // ============================================================================
    // SKULL & PLAYER HEAD METHODS
    // ============================================================================

    /**
     * Sets the owner of this player head using a PlayerProfile.
     *
     * @param profile the player profile
     * @return this builder for chaining
     * @throws NullPointerException if profile is null
     * @throws IllegalStateException if item is not a player head
     */
    public ItemBuilder profile(PlayerProfile profile) {
        Objects.requireNonNull(profile, "Profile cannot be null");

        if (!(this.item.getItemMeta() instanceof SkullMeta skullMeta)) {
            throw new IllegalStateException(
                    "Player profiles can only be applied to player heads"
            );
        }

        skullMeta.setPlayerProfile(profile);
        this.item.setItemMeta(skullMeta);

        return this;
    }

    /**
     * Sets the owner of this player head using a player name.
     *
     * @param owner the player name
     * @return this builder for chaining
     * @throws NullPointerException if owner is null
     */
    public ItemBuilder skullOwner(String owner) {
        Objects.requireNonNull(owner, "Owner name cannot be null");

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

    // ============================================================================
    // PERSISTENT DATA METHODS
    // ============================================================================

    /**
     * Retrieves persistent data from this item by key and type.
     *
     * @param key the persistent data key
     * @param type the persistent data type
     * @param <P> the primitive type
     * @param <C> the complex type
     * @return an Optional containing the data, or empty if not found
     * @throws NullPointerException if key or type is null
     */
    public <P, C> Optional<C> getData(
            NamespacedKey key,
            PersistentDataType<P, C> type
    ) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(type, "Type cannot be null");

        return PersistentDataAccess.find(
                this.item.getItemMeta(),
                key,
                type
        );
    }

    /**
     * Sets persistent data on this item.
     *
     * @param key the persistent data key
     * @param type the persistent data type
     * @param value the value to store
     * @param <P> the primitive type
     * @param <C> the complex type
     * @return this builder for chaining
     * @throws NullPointerException if key, type, or value is null
     */
    public <P, C> ItemBuilder setData(
            NamespacedKey key,
            PersistentDataType<P, C> type,
            C value
    ) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(type, "Type cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");

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

    /**
     * Retrieves the item ID from persistent data.
     *
     * @return an Optional containing the ID, or empty if not set
     */
    public Optional<String> findId() {
        return this.getData(
                ID_KEY,
                PersistentDataType.STRING
        );
    }

    /**
     * Sets the item ID in persistent data.
     *
     * @param id the item ID
     * @return this builder for chaining
     * @throws NullPointerException if id is null
     */
    public ItemBuilder setId(String id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return this.setData(
                ID_KEY,
                PersistentDataType.STRING,
                id
        );
    }

    // ============================================================================
    // BUILD METHOD
    // ============================================================================

    /**
     * Builds and returns the ItemStack. The returned ItemStack is a clone to
     * ensure independence from the builder.
     *
     * @return the constructed ItemStack
     */
    public ItemStack build() {
        return this.item.clone();
    }

    // ============================================================================
    // PRIVATE HELPER METHODS - LORE FORMATTING
    // ============================================================================

    private ItemBuilder setLore(
            String[] lines,
            Function<String, Component> formatter
    ) {
        return this.setLore(
                formatLore(lines, formatter)
        );
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

    private static List<Component> wrapLore(
            Component component
    ) {
        List<StyledCodePoint> characters = flatten(component);

        if (characters.isEmpty()) {
            return List.of(Component.empty());
        }

        List<Component> lines = new ArrayList<>();
        List<StyledCodePoint> currentLine = new ArrayList<>();
        List<StyledCodePoint> currentWord = new ArrayList<>();

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

                spaceBeforeWord = !currentLine.isEmpty();

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

    private static void appendWord(
            List<Component> lines,
            List<StyledCodePoint> currentLine,
            List<StyledCodePoint> word,
            boolean spaceBeforeWord
    ) {
        if (word.isEmpty()) {
            return;
        }

        boolean addSpace = spaceBeforeWord && !currentLine.isEmpty();

        int requiredLength = word.size() + (addSpace ? 1 : 0);

        if (!currentLine.isEmpty()
                && currentLine.size() + requiredLength > MAX_LORE_LINE_LENGTH) {
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
            int available = MAX_LORE_LINE_LENGTH - currentLine.size();

            if (available == 0) {
                flushLine(lines, currentLine);
                available = MAX_LORE_LINE_LENGTH;
            }

            int length = Math.min(available, word.size());

            currentLine.addAll(word.subList(0, length));

            word.subList(0, length).clear();

            if (!word.isEmpty()) {
                flushLine(lines, currentLine);
            }
        }
    }

    private static void flushLine(
            List<Component> lines,
            List<StyledCodePoint> characters
    ) {
        lines.add(buildComponent(characters));
        characters.clear();
    }

    private static List<StyledCodePoint> flatten(
            Component component
    ) {
        List<StyledCodePoint> characters = new ArrayList<>();

        Deque<Style> styles = new ArrayDeque<>();

        COMPONENT_FLATTENER.flatten(
                component,
                new FlattenerListener() {

                    @Override
                    public void component(String text) {
                        List<Style> activeStyles = List.copyOf(styles);

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

    private static Component buildComponent(
            List<StyledCodePoint> characters
    ) {
        if (characters.isEmpty()) {
            return Component.empty();
        }

        Component result = Component.empty();
        int start = 0;

        while (start < characters.size()) {
            StyledCodePoint first = characters.get(start);

            int end = start + 1;

            while (end < characters.size()
                    && first.styles().equals(
                    characters.get(end).styles()
            )) {
                end++;
            }

            StringBuilder text = new StringBuilder(end - start);

            for (int index = start; index < end; index++) {
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

    private static boolean isLineBreak(int codePoint) {
        return codePoint == '\n' || codePoint == '\r';
    }

    private static Component nonItalic(Component component) {
        return component.decoration(
                TextDecoration.ITALIC,
                false
        );
    }

    // ============================================================================
    // INNER RECORDS & CLASSES
    // ============================================================================

    /**
     * Internal record representing a styled code point for lore wrapping.
     *
     * @param codePoint the unicode code point
     * @param styles the applied text styles
     */
    private record StyledCodePoint(
            int codePoint,
            List<Style> styles
    ) {}
}
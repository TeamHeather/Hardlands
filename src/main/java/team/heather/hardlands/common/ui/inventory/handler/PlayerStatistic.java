package team.heather.hardlands.common.ui.inventory.handler;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.common.item.ItemBuilder;
import team.heather.hardlands.common.player.HardlandsPlayer;
import team.heather.hardlands.util.TextFormatters;

public enum PlayerStatistic {

    GAMES_PLAYED(
            "games-played",
            "Partidas jugadas",
            Group.GENERAL,
            Material.BOOK,
            "item/book",
            player -> player.getGamesPlayedOption().getValue()
    ),

    TOTAL_WINS(
            "total-wins",
            "Victorias",
            Group.GENERAL,
            Material.NETHER_STAR,
            "item/nether_star",
            player -> player.getTotalWinsOption().getValue()
    ),

    AVERAGE_SURVIVAL_TIME(
            "average-survival-time",
            "Supervivencia media",
            Group.GENERAL,
            Material.CLOCK,
            "item/clock_00",
            player -> player.getAverageSurvivalTimeOption().getValue()
    ),

    TOTAL_KILLS(
            "total-kills",
            "Eliminaciones",
            Group.COMBAT,
            Material.IRON_SWORD,
            "item/iron_sword",
            player -> player.getTotalKillsOption().getValue()
    ),

    TOTAL_DEATHS(
            "total-deaths",
            "Muertes",
            Group.COMBAT,
            Material.BONE,
            "item/bone",
            player -> player.getTotalDeathsOption().getValue()
    ),

    IRON_MAN_AWARDS(
            "iron-man-awards",
            "Iron Man",
            Group.AWARDS,
            Material.IRON_CHESTPLATE,
            "item/iron_chestplate",
            player -> player.getIronManAwardsOption().getValue()
    ),

    KILL_TOP_AWARDS(
            "kill-top-awards",
            "Kill Top",
            Group.AWARDS,
            Material.DIAMOND_SWORD,
            "item/diamond_sword",
            player -> player.getKillTopAwardsOption().getValue()
    ),

    PAPER_MAN_AWARDS(
            "paper-man-awards",
            "Paper Man",
            Group.AWARDS,
            Material.PAPER,
            "item/paper",
            player -> player.getPaperManAwardsOption().getValue()
    ),

    BEST_FRIEND(
            "best-friend",
            "Mejor amigo",
            Group.CAREER,
            player -> player.getBestFriendOption().getValue()
    ),

    PRIMARY_KILLER(
            "primary-killer",
            "Asesino principal",
            Group.CAREER,
            player -> player.getPrimaryKillerOption().getValue()
    ),

    PRIMARY_VICTIM(
            "primary-victim",
            "Víctima principal",
            Group.CAREER,
            player -> player.getPrimaryVictimOption().getValue()
    ),

    MOST_PLAYED_HOST(
            "most-played-host",
            "Hoster con más partidas",
            Group.CAREER,
            player -> player.getMostPlayedHostOption().getValue()
    ),

    MOST_PLAYED_PRESET(
            "most-played-preset",
            "Plantilla más jugada",
            Group.CAREER,
            Material.WRITTEN_BOOK,
            "item/written_book",
            player -> player.getMostPlayedPresetOption().getValue()
    );

    private static final List<PlayerStatistic> ALL = List.of(values());

    private final String key;
    private final String label;
    private final Group group;
    private final Material material;
    private final Component icon;
    private final Function<HardlandsPlayer, Object> valueProvider;
    private final boolean playerReference;

    PlayerStatistic(
            String key,
            String label,
            Group group,
            Material material,
            String sprite,
            Function<HardlandsPlayer, Object> valueProvider
    ) {
        this.key = key;
        this.label = label;
        this.group = group;
        this.material = material;
        this.icon = sprite(sprite);
        this.valueProvider = valueProvider;
        this.playerReference = false;
    }

    PlayerStatistic(
            String key,
            String label,
            Group group,
            Function<HardlandsPlayer, Object> valueProvider
    ) {
        this.key = key;
        this.label = label;
        this.group = group;
        this.material = Material.PLAYER_HEAD;
        this.icon = Component.empty();
        this.valueProvider = valueProvider;
        this.playerReference = true;
    }

    public String key() {
        return this.key;
    }

    public Group group() {
        return this.group;
    }

    public static List<PlayerStatistic> all() {
        return ALL;
    }

    public Component line(HardlandsPlayer player, TextColor color, boolean pinned) {
        Object value = this.valueProvider.apply(player);
        Component statisticIcon = this.playerReference ? playerIcon(value) : this.icon;

        return Component.empty()
                .append(pinned ? Component.text("✦ ", color) : Component.empty())
                .append(statisticIcon)
                .append(Component.space())
                .append(Component.text(this.label + ": ", color))
                .append(Component.text(this.formatValue(value), NamedTextColor.WHITE));
    }

    public ItemStack createSelectionItem(HardlandsPlayer player, TextColor color, boolean pinned) {
        Object value = this.valueProvider.apply(player);
        ItemBuilder builder = new ItemBuilder(this.material).glint(pinned);

        if (this.playerReference && value instanceof UUID uuid) {
            HardlandsPlayer referencedPlayer = Hardlands.getInstance().getPlayerManager().get(uuid);

            if (referencedPlayer != null) {
                builder.skullOwner(referencedPlayer.getName());
            }
        }

        builder
                .name(Component.text(TextFormatters.TINY_CAPS.format(this.label), color))
                .addLore(TextFormatters.HIGHLIGHT.format("{Valor}: [%s]".formatted(this.formatValue(value)), color));

        if (pinned) {
            builder.addLore(TextFormatters.HIGHLIGHT.format("{Fijada}", color));
        }

        builder
                .addLore(Component.empty())
                .addLore(TextFormatters.HIGHLIGHT.format(pinned ? "{Clic} para quitar." : "{Clic} para fijar.", color));

        return builder.build();
    }

    private String formatValue(Object value) {
        if (value instanceof UUID uuid) {
            HardlandsPlayer player = Hardlands.getInstance().getPlayerManager().get(uuid);
            return player == null ? "Desconocido" : player.getName();
        }

        if (value instanceof Double number) {
            return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
        }

        if (value == null) {
            return this.playerReference ? "Ninguno" : "Ninguna";
        }

        return String.valueOf(value);
    }

    private static Component playerIcon(Object value) {
        if (value instanceof UUID uuid) {
            return TextFormatters.MINI_MESSAGE.format("<white><head:%s></white>".formatted(uuid));
        }

        return sprite("item/player_head");
    }

    private static Component sprite(String sprite) {
        return TextFormatters.MINI_MESSAGE.format(
                "<white><sprite:\"minecraft:items\":\"minecraft:%s\"></white>".formatted(sprite)
        );
    }

    public enum Group {

        GENERAL("General"),
        COMBAT("Combate"),
        AWARDS("Reconocimientos"),
        CAREER("Trayectoria");

        private final String label;

        Group(String label) {
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }
}
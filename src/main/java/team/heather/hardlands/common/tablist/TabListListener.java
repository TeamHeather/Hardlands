package team.heather.hardlands.common.tablist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.game.GameData;

public final class TabListListener implements Listener {

    private static final long GLINT_UPDATE_TICKS = 1L;

    private static final int GLINT_ANIMATION_TICKS = 16;
    private static final int GLINT_CYCLE_TICKS = 80;

    private final Map<UUID, Entry> playerEntries = new HashMap<>();

    private Component header = Component.empty();
    private Component footer = Component.empty();

    private int gameNumber;
    private int glintCycleTick;
    private int glintActivation;

    @Nullable private UUID paperManId;
    @Nullable private UUID ironManId;
    @Nullable private TabListFormatter.HeaderLayout headerLayout;
    @Nullable private BukkitTask glintTask;

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        this.updateAll();
        this.startGlint();
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        this.playerEntries.remove(event.getPlayer().getUniqueId());

        this.runNextTick(() -> {
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                this.stopGlint();
                return;
            }

            this.updateAll();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onDeath(PlayerDeathEvent event) {
        this.runNextTick(this::updateAll);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        this.runNextTick(() -> this.updateAfterDamage(playerId));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        this.runNextTick(() -> this.updateEntry(playerId, getGame()));
    }

    @EventHandler
    private void onRespawn(PlayerRespawnEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        this.runNextTick(() -> this.updateEntry(playerId, getGame()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onGameModeChange(PlayerGameModeChangeEvent event) {
        this.runNextTick(this::updateAll);
    }

    public void setEntry(
            Player player,
            Component prefix,
            Component name,
            Component suffix
    ) {
        this.playerEntries.put(
                player.getUniqueId(),
                new Entry(prefix, name, suffix)
        );

        this.updateEntry(player, getGame());
    }

    public void setPrefix(Player player, Component prefix) {
        Entry entry = this.getEntry(player);

        this.setEntry(
                player,
                prefix,
                entry.name(),
                entry.suffix()
        );
    }

    public void setName(Player player, Component name) {
        Entry entry = this.getEntry(player);

        this.setEntry(
                player,
                entry.prefix(),
                name,
                entry.suffix()
        );
    }

    public void setSuffix(Player player, Component suffix) {
        Entry entry = this.getEntry(player);

        this.setEntry(
                player,
                entry.prefix(),
                entry.name(),
                suffix
        );
    }

    public void update(Player player) {
        GameData game = getGame();

        this.applyHeaderFooter(player);
        this.updateEntry(player, game);
    }

    public void updateAll() {
        GameData game = getGame();
        List<Player> players = getOnlinePlayers();

        this.updateHeaderFooter(game, players);
        this.updateSpecialPlayers(game);
        this.updateEntries(players, game);
        this.updateOrder(players, game);
    }

    public void updateOrder() {
        this.updateOrder(
                getOnlinePlayers(),
                getGame()
        );
    }

    private void updateHeaderFooter(GameData game, List<Player> players) {
        GameData.Host host = game.host();

        this.gameNumber = host == null
                ? 0
                : host.number();

        this.headerLayout = TabListFormatter.headerLayout(game);
        this.header = this.headerLayout.format(
                TabListFormatter.label(this.gameNumber)
        );

        this.footer = TabListFormatter.footer(
                countAlivePlayers(players),
                players.size()
        );
    }

    private void updateEntries(List<Player> players, GameData game) {
        for (Player player : players) {
            this.applyHeaderFooter(player);
            this.updateEntry(player, game);
        }
    }

    private void updateAfterDamage(UUID playerId) {
        GameData game = getGame();

        UUID previousPaperManId = this.paperManId;
        UUID previousIronManId = this.ironManId;

        this.updateSpecialPlayers(game);
        this.updateEntry(playerId, game);

        this.updateChangedState(
                previousPaperManId,
                this.paperManId,
                playerId,
                game
        );

        this.updateChangedState(
                previousIronManId,
                this.ironManId,
                playerId,
                game
        );
    }

    private void updateSpecialPlayers(GameData game) {
        this.paperManId = playerId(game.paperMan());
        this.ironManId = playerId(game.ironMan());
    }

    private void updateChangedState(
            @Nullable UUID previousId,
            @Nullable UUID currentId,
            UUID updatedPlayerId,
            GameData game
    ) {
        if (Objects.equals(previousId, currentId)) {
            return;
        }

        if (previousId != null && !previousId.equals(updatedPlayerId)) {
            this.updateEntry(previousId, game);
        }

        if (currentId != null
                && !currentId.equals(updatedPlayerId)
                && !currentId.equals(previousId)) {
            this.updateEntry(currentId, game);
        }
    }

    private void updateOrder(List<Player> players, GameData game) {
        players.sort(
                Comparator.comparingInt(
                                (Player player) -> game.killCount(player.getUniqueId())
                        )
                        .reversed()
                        .thenComparing(
                                Player::getName,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        for (int index = 0; index < players.size(); index++) {
            players.get(index).setPlayerListOrder(index);
        }
    }

    private void updateEntry(UUID playerId, GameData game) {
        Player player = Bukkit.getPlayer(playerId);

        if (player != null) {
            this.updateEntry(player, game);
        }
    }

    private void updateEntry(Player player, GameData game) {
        if (!player.isOnline()) {
            return;
        }

        Entry entry = this.getEntry(player);

        player.playerListName(TabListFormatter.player(
                player,
                entry.prefix(),
                entry.name(),
                entry.suffix(),
                game
        ));
    }

    private Entry getEntry(Player player) {
        return this.playerEntries.computeIfAbsent(
                player.getUniqueId(),
                _ -> Entry.of(player)
        );
    }

    private void applyHeaderFooter(Player player) {
        player.sendPlayerListHeaderAndFooter(
                this.header,
                this.footer
        );
    }

    private void startGlint() {
        if (this.glintTask != null) {
            return;
        }

        this.glintTask = Bukkit.getScheduler().runTaskTimer(
                Hardlands.getInstance(),
                this::tickGlint,
                0L,
                GLINT_UPDATE_TICKS
        );
    }

    private void stopGlint() {
        if (this.glintTask == null) {
            return;
        }

        this.glintTask.cancel();
        this.glintTask = null;
        this.glintCycleTick = 0;
    }

    private void tickGlint() {
        TabListFormatter.HeaderLayout layout = this.headerLayout;

        if (layout == null) {
            return;
        }

        if (this.glintCycleTick < GLINT_ANIMATION_TICKS) {
            this.sendGlintFrame(layout);
        } else if (this.glintCycleTick == GLINT_ANIMATION_TICKS) {
            this.restoreHeader(layout);
        }

        this.advanceGlintCycle();
    }

    private void sendGlintFrame(TabListFormatter.HeaderLayout layout) {
        Component label = TabListFormatter.glintLabel(
                this.gameNumber,
                getGlintFrame(this.glintCycleTick),
                this.glintActivation
        );

        this.sendHeader(layout.format(label));
    }

    private void restoreHeader(TabListFormatter.HeaderLayout layout) {
        this.header = layout.format(
                TabListFormatter.label(this.gameNumber)
        );

        this.sendHeader(this.header);
    }

    private void advanceGlintCycle() {
        this.glintCycleTick++;

        if (this.glintCycleTick < GLINT_CYCLE_TICKS) {
            return;
        }

        this.glintCycleTick = 0;
        this.glintActivation++;
    }

    private void sendHeader(Component header) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(
                    header,
                    this.footer
            );
        }
    }

    private void runNextTick(Runnable task) {
        Bukkit.getScheduler().runTask(
                Hardlands.getInstance(),
                task
        );
    }

    private static int getGlintFrame(int animationTick) {
        return Math.round(
                (float) animationTick
                        * (TabListFormatter.GLINT_FRAMES - 1)
                        / (GLINT_ANIMATION_TICKS - 1)
        );
    }

    private static int countAlivePlayers(List<Player> players) {
        int alivePlayers = 0;

        for (Player player : players) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                alivePlayers++;
            }
        }

        return alivePlayers;
    }

    private static @Nullable UUID playerId(@Nullable GameData.FirstDamage damage) {
        return damage == null
                ? null
                : damage.playerId();
    }

    private static List<Player> getOnlinePlayers() {
        return new ArrayList<>(Bukkit.getOnlinePlayers());
    }

    private static GameData getGame() {
        return Hardlands.getInstance()
                .getGameManager()
                .getData();
    }

    private record Entry(
            Component prefix,
            Component name,
            Component suffix
    ) {

        private static Entry of(Player player) {
            return new Entry(
                    Component.empty(),
                    Component.text(player.getName(), NamedTextColor.WHITE),
                    Component.empty()
            );
        }
    }
}
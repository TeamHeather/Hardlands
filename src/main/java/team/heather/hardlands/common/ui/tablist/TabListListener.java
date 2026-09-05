package team.heather.hardlands.common.ui.tablist;

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
import team.heather.hardlands.common.player.HardlandsPlayer;
import team.heather.hardlands.game.GameData;
import team.heather.hardlands.game.GameManager;

public final class TabListListener implements Listener {

    private static final int GLINT_ANIMATION_TICKS = 16;
    private static final int GLINT_CYCLE_TICKS = 80;

    private final GameManager gameManager;
    private final Map<UUID, Entry> playerEntries = new HashMap<>();
    private final TabListRenderer renderer;

    private Component footer = Component.empty();
    private Component header = Component.empty();
    @Nullable private BukkitTask glintTask;
    @Nullable private TabListRenderer.HeaderLayout headerLayout;
    @Nullable private UUID ironManId;
    @Nullable private UUID paperManId;
    private int gameNumber;
    private int glintActivation;
    private int glintCycleTick;

    public TabListListener(GameManager gameManager, TabListRenderer renderer) {
        this.gameManager = gameManager;
        this.renderer = renderer;
    }

    public void setEntry(Player player, Component prefix, Component name, Component suffix) {
        this.playerEntries.put(player.getUniqueId(), new Entry(prefix, name, suffix));
        this.updateEntry(player, this.gameManager.getData());
    }

    public void setName(Player player, Component name) {
        Entry entry = this.getEntry(player);
        this.setEntry(player, entry.prefix(), name, entry.suffix());
    }

    public void setPrefix(Player player, Component prefix) {
        Entry entry = this.getEntry(player);
        this.setEntry(player, prefix, entry.name(), entry.suffix());
    }

    public void setSuffix(Player player, Component suffix) {
        Entry entry = this.getEntry(player);
        this.setEntry(player, entry.prefix(), entry.name(), suffix);
    }

    public void update(Player player) {
        GameData game = this.gameManager.getData();

        player.sendPlayerListHeaderAndFooter(this.header, this.footer);
        this.updateEntry(player, game);
    }

    public void updateAll() {
        GameData game = this.gameManager.getData();
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        this.updateHeaderFooter(game, players);

        this.paperManId = playerId(game.paperMan());
        this.ironManId = playerId(game.ironMan());

        for (Player player : players) {
            player.sendPlayerListHeaderAndFooter(this.header, this.footer);
            this.updateEntry(player, game);
        }

        this.updateOrder(players, game);
    }

    public void updateOrder() {
        this.updateOrder(new ArrayList<>(Bukkit.getOnlinePlayers()), this.gameManager.getData());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        this.runNextTick(() -> this.updateAfterDamage(playerId));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onDeath(PlayerDeathEvent event) {
        this.runNextTick(this::updateAll);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onGameModeChange(PlayerGameModeChangeEvent event) {
        this.runNextTick(this::updateAll);
    }

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        this.runNextTick(() -> this.updateEntry(playerId, this.gameManager.getData()));
    }

    @EventHandler
    private void onRespawn(PlayerRespawnEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        this.runNextTick(() -> this.updateEntry(playerId, this.gameManager.getData()));
    }

    private Entry getEntry(Player player) {
        return this.playerEntries.computeIfAbsent(
                player.getUniqueId(),
                _ -> new Entry(Component.empty(), Component.text(player.getName(), NamedTextColor.WHITE), Component.empty())
        );
    }

    private void runNextTick(Runnable task) {
        Bukkit.getScheduler().runTask(Hardlands.getInstance(), task);
    }

    private void startGlint() {
        if (this.glintTask != null) {
            return;
        }

        this.glintTask = Bukkit.getScheduler().runTaskTimer(Hardlands.getInstance(), this::tickGlint, 0L, 1L);
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
        TabListRenderer.HeaderLayout layout = this.headerLayout;

        if (layout == null) {
            return;
        }

        Component currentHeader = null;

        if (this.glintCycleTick < GLINT_ANIMATION_TICKS) {
            int frame = Math.round(
                    (float) this.glintCycleTick
                            * (TabListRenderer.GLINT_FRAMES - 1)
                            / (GLINT_ANIMATION_TICKS - 1)
            );

            currentHeader = layout.format(this.renderer.glintLabel(this.gameNumber, frame, this.glintActivation));
        } else if (this.glintCycleTick == GLINT_ANIMATION_TICKS) {
            this.header = layout.format(this.renderer.label(this.gameNumber));
            currentHeader = this.header;
        }

        if (currentHeader != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendPlayerListHeaderAndFooter(currentHeader, this.footer);
            }
        }

        this.glintCycleTick++;

        if (this.glintCycleTick >= GLINT_CYCLE_TICKS) {
            this.glintCycleTick = 0;
            this.glintActivation++;
        }
    }

    private void updateAfterDamage(UUID playerId) {
        GameData game = this.gameManager.getData();

        UUID previousPaperManId = this.paperManId;
        UUID previousIronManId = this.ironManId;

        this.paperManId = playerId(game.paperMan());
        this.ironManId = playerId(game.ironMan());

        this.updateEntry(playerId, game);
        this.updateChangedState(previousPaperManId, this.paperManId, playerId, game);
        this.updateChangedState(previousIronManId, this.ironManId, playerId, game);
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

        if (currentId != null && !currentId.equals(updatedPlayerId) && !currentId.equals(previousId)) {
            this.updateEntry(currentId, game);
        }
    }

    private void updateEntry(Player player, GameData game) {
        if (!player.isOnline()) {
            return;
        }

        Entry entry = this.getEntry(player);

        player.playerListName(
                this.renderer.player(player, entry.prefix(), entry.name(), entry.suffix(), game)
        );
    }

    private void updateEntry(UUID playerId, GameData game) {
        Player player = Bukkit.getPlayer(playerId);

        if (player != null) {
            this.updateEntry(player, game);
        }
    }

    private void updateHeaderFooter(GameData game, List<Player> players) {
        GameData.Host host = game.host();
        int alivePlayers = 0;

        for (Player player : players) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                alivePlayers++;
            }
        }

        this.gameNumber = host == null ? 0 : host.number();
        this.headerLayout = this.renderer.headerLayout(game);
        this.header = this.headerLayout.format(this.renderer.label(this.gameNumber));
        this.footer = this.renderer.footer(alivePlayers, players.size());
    }

    private void updateOrder(List<Player> players, GameData game) {
        players.sort(
                Comparator.comparingInt((Player player) -> game.killCount(player))
                        .reversed()
                        .thenComparing(Player::getName, String.CASE_INSENSITIVE_ORDER)
        );

        for (int index = 0; index < players.size(); index++) {
            players.get(index).setPlayerListOrder(index);
        }
    }

    private static @Nullable UUID playerId(@Nullable HardlandsPlayer player) {
        return player == null ? null : player.getUniqueId();
    }

    private record Entry(Component prefix, Component name, Component suffix) {}
}
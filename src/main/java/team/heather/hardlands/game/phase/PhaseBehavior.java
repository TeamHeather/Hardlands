package team.heather.hardlands.game.phase;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.internal.data.json.LocalTimeAdapter;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.game.world.PregenerationManager;
import team.heather.hardlands.game.world.ScatterManager;
import team.heather.hardlands.game.world.WorldManager;
import team.heather.hardlands.feature.ui.HardlandsColor;
import team.heather.hardlands.feature.ui.chat.AnnouncementType;
import team.heather.hardlands.util.TextFormatters;

final class PhaseBehavior {

    static final Handler OFF_GAME = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            AnnouncementType.NEUTRAL.broadcast(
                    "La partida ha terminado. El servidor ha vuelto al estado fuera de partida.",
                    List.of(Sound.BLOCK_BEACON_DEACTIVATE)
            );
        }
    };

    static final Handler PREPARATION = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            AnnouncementType.NEUTRAL.broadcast(
                    """
                    La preparación de la partida ha comenzado. Se configurará el mundo y se pregenerarán los chunks \
                    necesarios antes de continuar.
                    """,
                    List.of(Sound.BLOCK_BEACON_ACTIVATE)
            );

            WorldManager worldManager = plugin.getWorldManager();
            PregenerationManager pregenerationManager = worldManager.getPregenerationManager();

            pregenerationManager.setProgressUpdatesEnabled(true);
            worldManager.applyConfiguration();
            worldManager.pregenerate();

            plugin.getGameManager().setPreparationProgress(pregenerationManager.getProgress());
        }

        @Override
        public void onStop(Hardlands plugin, Phase phase) {
            PregenerationManager pregenerationManager = plugin.getWorldManager().getPregenerationManager();

            pregenerationManager.setProgressUpdatesEnabled(false);

            if (!pregenerationManager.isCompleted()) pregenerationManager.pause();
        }
    };

    static final Handler WAITING = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            LocalTime startTime = plugin.getGameManager().getStartTimeOption().getValue();
            String formattedStartTime = startTime == null
                    ? "sin configurar"
                    : TextFormatters.LOCAL_TIME.format(startTime);

            AnnouncementType.NEUTRAL.broadcast(
                    "El mundo está preparado y listo para comenzar. La partida iniciará a las %s.",
                    List.of(Sound.BLOCK_NOTE_BLOCK_CHIME),
                    highlight(formattedStartTime, HardlandsColor.GRAY.primary())
            );
        }
    };

    static final Handler SCATTER = new Handler() {

        private StartCountdown countdown;

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            GameManager gameManager = plugin.getGameManager();
            ScatterManager scatterManager = plugin.getWorldManager().getScatterManager();
            int playerCount = Bukkit.getOnlinePlayers().size();

            AnnouncementType.NEUTRAL.broadcast(
                    """
                    La dispersión de %s jugadores ha comenzado. Todos serán enviados a sus posiciones iniciales antes \
                    de comenzar la cuenta regresiva.
                    """,
                    List.of(Sound.ENTITY_ENDERMAN_TELEPORT),
                    highlight(String.valueOf(playerCount), HardlandsColor.GRAY.primary())
            );

            Bukkit.getOnlinePlayers().forEach(scatterManager::enqueue);
            scatterManager.setProgressUpdatesEnabled(true);

            scatterManager.scatterAllAsync().thenRun(() -> {
                if (gameManager.getPhase() != phase) return;

                this.countdown = new StartCountdown(plugin, scatterManager);
                this.countdown.start();
            });
        }

        @Override
        public void onStop(Hardlands plugin, Phase phase) {
            if (this.countdown != null) {
                this.countdown.close();
                this.countdown = null;
            }

            ScatterManager scatterManager = plugin.getWorldManager().getScatterManager();

            scatterManager.cancelScatter();
            scatterManager.setProgressUpdatesEnabled(false);

            Bukkit.getOnlinePlayers().forEach(LivingEntity::clearActivePotionEffects);
        }
    };

    static final Handler SURVIVAL = new Handler() {

        private ScheduledFuture<?> pvpAnnouncementTask;

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            GameManager gameManager = plugin.getGameManager();
            WorldManager worldManager = plugin.getWorldManager();

            int pvpMinute = gameManager.getPvpMinuteOption().getValue();

            String borderSize = highlight(
                    formatBorderSize(worldManager.getSurvivalSizeOption().getValue()),
                    HardlandsColor.GREEN.primary()
            );
            String pvpTime = highlight(formatGameTime(gameManager, pvpMinute), HardlandsColor.GREEN.primary());
            String borderShrinkTime = highlight(
                    formatGameTime(gameManager, gameManager.getBorderShrinkMinuteOption().getValue()),
                    HardlandsColor.GREEN.primary()
            );
            String meetupTime = highlight(
                    formatGameTime(gameManager, gameManager.getMeetupMinuteOption().getValue()),
                    HardlandsColor.GREEN.primary()
            );
            String deathmatchTime = highlight(
                    formatGameTime(gameManager, gameManager.getDeathmatchMinuteOption().getValue()),
                    HardlandsColor.GREEN.primary()
            );

            AnnouncementType.GAMEPLAY.broadcast(
                    """
                    La partida ha comenzado con un borde de %s. El PvP se activará en %s, la reducción del borde \
                    comenzará en %s, el Meetup en %s y el Deathmatch en %s.
                    """,
                    List.of(Sound.ITEM_GOAT_HORN_SOUND_2),
                    borderSize,
                    pvpTime,
                    borderShrinkTime,
                    meetupTime,
                    deathmatchTime
            );

            this.schedulePvpAnnouncement(plugin, gameManager, phase, pvpMinute);
        }

        @Override
        public void onStop(Hardlands plugin, Phase phase) {
            if (this.pvpAnnouncementTask == null) return;

            this.pvpAnnouncementTask.cancel(false);
            this.pvpAnnouncementTask = null;
        }

        private void schedulePvpAnnouncement(Hardlands plugin, GameManager gameManager, Phase phase, int pvpMinute) {
            this.pvpAnnouncementTask = plugin.getSingleThreadScheduler().schedule(
                    () -> Bukkit.getScheduler().runTask(plugin, () -> {
                        if (gameManager.getPhase() != phase) return;

                        AnnouncementType.DANGER.broadcast(
                                "El PvP ha comenzado. El combate entre jugadores está habilitado a partir de este momento.",
                                List.of(Sound.ENTITY_ELDER_GUARDIAN_CURSE)
                        );

                        this.pvpAnnouncementTask = null;
                    }),
                    Duration.ofMinutes(pvpMinute)
            );
        }
    };

    static final Handler BORDER_SHRINK = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            WorldManager worldManager = plugin.getWorldManager();
            Duration duration = phase.getDuration(plugin.getGameManager());

            String initialSize = highlight(
                    formatBorderSize(worldManager.getSurvivalSizeOption().getValue()),
                    HardlandsColor.RED.primary()
            );
            String targetSize = highlight(
                    formatBorderSize(worldManager.getMeetupSizeOption().getValue()),
                    HardlandsColor.RED.primary()
            );
            String shrinkDuration = highlight(TextFormatters.DURATION.format(duration), HardlandsColor.RED.primary());

            AnnouncementType.DANGER.broadcast(
                    "El borde ha comenzado a reducirse de %s a %s. La reducción durará %s y finalizará al comenzar el Meetup.",
                    List.of(Sound.BLOCK_PISTON_EXTEND),
                    initialSize,
                    targetSize,
                    shrinkDuration
            );

            if (!duration.isZero()) worldManager.shrinkForMeetup(duration);
        }
    };

    static final Handler MEETUP = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            WorldManager worldManager = plugin.getWorldManager();
            Duration duration = phase.getDuration(plugin.getGameManager());

            String borderSize = highlight(
                    formatBorderSize(worldManager.getMeetupSizeOption().getValue()),
                    HardlandsColor.GREEN.primary()
            );
            String finalShrinkTime = highlight(TextFormatters.DURATION.format(duration), HardlandsColor.GREEN.primary());

            AnnouncementType.GAMEPLAY.broadcast(
                    "El Meetup ha comenzado con un borde de %s. La reducción final comenzará en %s.",
                    List.of(Sound.BLOCK_BELL_USE),
                    borderSize,
                    finalShrinkTime
            );
        }
    };

    static final Handler FINAL_SHRINK = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            WorldManager worldManager = plugin.getWorldManager();
            Duration duration = phase.getDuration(plugin.getGameManager());

            String initialSize = highlight(
                    formatBorderSize(worldManager.getMeetupSizeOption().getValue()),
                    HardlandsColor.RED.primary()
            );
            String targetSize = highlight(
                    formatBorderSize(worldManager.getDeathmatchSizeOption().getValue()),
                    HardlandsColor.RED.primary()
            );
            String shrinkDuration = highlight(TextFormatters.DURATION.format(duration), HardlandsColor.RED.primary());

            AnnouncementType.DANGER.broadcast(
                    """
                    La reducción final del borde ha comenzado. El límite pasará de %s a %s durante %s. \
                    El Deathmatch comenzará al finalizar.
                    """,
                    List.of(Sound.BLOCK_RESPAWN_ANCHOR_CHARGE),
                    initialSize,
                    targetSize,
                    shrinkDuration
            );

            if (!duration.isZero()) worldManager.shrinkForDeathmatch(duration);
        }
    };

    static final Handler DEATHMATCH = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            String borderSize = highlight(
                    formatBorderSize(plugin.getWorldManager().getDeathmatchSizeOption().getValue()),
                    HardlandsColor.RED.primary()
            );

            AnnouncementType.DANGER.broadcast(
                    "El Deathmatch ha comenzado con un borde final de %s. No habrá más fases después de esta.",
                    List.of(Sound.ENTITY_WITHER_SPAWN),
                    borderSize
            );
        }
    };

    private PhaseBehavior() {}

    private static String highlight(String text, TextColor color) {
        return "<color:%s>%s</color>".formatted(color.asHexString(), text);
    }

    private static String formatGameTime(GameManager gameManager, Integer minute) {
        Integer startMinute = Phase.SURVIVAL.getMinute(gameManager);

        if (minute == null || startMinute == null) return "sin configurar";

        return TextFormatters.DURATION.format(Duration.ofMinutes(Math.max(0, minute - startMinute)));
    }

    private static String formatBorderSize(Integer size) {
        return size == null ? "un tamaño sin configurar" : "%d × %d bloques".formatted(size, size);
    }

    interface Handler {

        default void onStart(Hardlands plugin, Phase phase) {}

        default void onStop(Hardlands plugin, Phase phase) {}
    }
}
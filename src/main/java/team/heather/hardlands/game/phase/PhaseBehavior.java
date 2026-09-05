package team.heather.hardlands.game.phase;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.common.ui.HardlandsColor;
import team.heather.hardlands.common.ui.chat.AnnouncementType;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.game.world.PregenerationManager;
import team.heather.hardlands.game.world.ScatterManager;
import team.heather.hardlands.game.world.WorldManager;
import team.heather.hardlands.util.TextFormatters;

final class PhaseBehavior {

    static final Handler OFF_GAME = new Handler() {
        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            AnnouncementType.NEUTRAL.broadcast(
                    """
										La partida ha terminado. El servidor ha vuelto al
										estado fuera de partida.
										""",
                    List.of(Sound.BLOCK_BEACON_DEACTIVATE)
            );
        }
    };

    static final Handler PREPARATION = new Handler() {
        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            AnnouncementType.NEUTRAL.broadcast(
                    """
										La preparación de la partida ha comenzado. Se
										configurará el mundo y se pregenerarán los chunks
										necesarios antes de continuar.
										""",
                    List.of(Sound.BLOCK_BEACON_ACTIVATE)
            );

            WorldManager worldManager = plugin.getWorldManager();
            PregenerationManager pregenerationManager = worldManager.getPregenerationManager();

            pregenerationManager.progressUpdatesEnabled(true);
            worldManager.applyConfiguration();
            worldManager.pregenerate();

            plugin.getGameManager().setPreparationProgress(pregenerationManager.progress());
        }

        @Override
        public void onStop(Hardlands plugin, Phase phase) {
            PregenerationManager pregenerationManager = plugin.getWorldManager().getPregenerationManager();
            pregenerationManager.progressUpdatesEnabled(false);

            if (!pregenerationManager.completed()) {
                pregenerationManager.pause();
            }
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
                    """
										El mundo está preparado y listo para comenzar. La
										partida iniciará a las %s.
										""",
                    List.of(Sound.BLOCK_NOTE_BLOCK_CHIME),
                    highlight(formattedStartTime, HardlandsColor.GRAY.primary())
            );
        }
    };

    static final Handler SCATTER = new Handler() {

        private static final long COUNTDOWN_DELAY = 80L;
        private StartCountdown countdown;

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            ScatterManager scatterManager = plugin.getWorldManager().getScatterManager();
            int playerCount = Bukkit.getOnlinePlayers().size();

            AnnouncementType.NEUTRAL.broadcast(
                    """
										La dispersión de %s jugadores ha comenzado. Todos
										serán enviados a sus posiciones iniciales antes de
										comenzar la cuenta regresiva.
										""",
                    List.of(Sound.ENTITY_ENDERMAN_TELEPORT),
                    highlight(String.valueOf(playerCount), HardlandsColor.GRAY.primary())
            );

            Bukkit.getOnlinePlayers().forEach(scatterManager::enqueue);
            scatterManager.progressUpdatesEnabled(true);

            scatterManager.scatterAllAsync().thenRun(() ->
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (plugin.getGameManager().getPhase() != phase) return;

                        Bukkit.getOnlinePlayers().forEach(player -> {
                            player.removePotionEffect(PotionEffectType.BLINDNESS);
                            player.getWorld().playSound(
                                    player.getLocation(),
                                    Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR,
                                    1.0F,
                                    1.25F
                            );
                        });

                        this.countdown = new StartCountdown(plugin, Bukkit.getPlayer("MrPepe3012"), 1);
                        this.countdown.start();
                    }, COUNTDOWN_DELAY));
        }

        @Override
        public void onStop(Hardlands plugin, Phase phase) {
            if (this.countdown != null) {
                this.countdown.close();
                this.countdown = null;
            }

            ScatterManager scatterManager = plugin.getWorldManager().getScatterManager();
            scatterManager.cancelScatter();
            scatterManager.progressUpdatesEnabled(false);

            Bukkit.getOnlinePlayers().forEach(LivingEntity::clearActivePotionEffects);
        }
    };

    static final Handler SURVIVAL = new Handler() {

        private ScheduledFuture<?> pvpAnnouncementTask;

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            GameManager gameManager = plugin.getGameManager();
            WorldManager worldManager = plugin.getWorldManager();
            TextColor highlightColor = HardlandsColor.GREEN.primary();
            Integer pvpMinute = gameManager.getPvpMinuteOption().getValue();

            this.announceSurvivalStart(gameManager, worldManager, highlightColor);

            if (pvpMinute != null) {
                this.schedulePvpAnnouncement(plugin, gameManager, phase, pvpMinute);
            }
        }

        @Override
        public void onStop(Hardlands plugin, Phase phase) {
            if (this.pvpAnnouncementTask != null) {
                this.pvpAnnouncementTask.cancel(false);
                this.pvpAnnouncementTask = null;
            }
        }

        private void announceSurvivalStart(GameManager gameManager, WorldManager worldManager, TextColor color) {
            String borderSize = highlightBorderSize(worldManager.getSurvivalSizeOption().getValue(), color);
            String pvpTime = highlightGameTime(gameManager, gameManager.getPvpMinuteOption().getValue(), color);
            String borderShrinkTime = highlightGameTime(gameManager, gameManager.getBorderShrinkMinuteOption().getValue(), color);
            String meetupTime = highlightGameTime(gameManager, gameManager.getMeetupMinuteOption().getValue(), color);
            String deathmatchTime = highlightGameTime(gameManager, gameManager.getDeathmatchMinuteOption().getValue(), color);

            AnnouncementType.GAMEPLAY.broadcast(
                    """
										La partida ha comenzado con un borde de
										%s. El PvP se activará en %s, la reducción del
										borde comenzará en %s, el Meetup en %s y el
										Deathmatch en %s.
										""",
                    List.of(Sound.ITEM_GOAT_HORN_SOUND_2),
                    borderSize,
                    pvpTime,
                    borderShrinkTime,
                    meetupTime,
                    deathmatchTime
            );
        }

        private void schedulePvpAnnouncement(Hardlands plugin, GameManager gameManager, Phase phase, int pvpMinute) {
            this.pvpAnnouncementTask = plugin.getThreadScheduler().schedule(() ->
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (gameManager.getPhase() != phase) return;

                                AnnouncementType.DANGER.broadcast(
                                        """
																				El PvP ha comenzado. El combate entre jugadores
																				está habilitado a partir de este momento.
																				""",
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

            startBorderShrink(
                    worldManager.getSurvivalSizeOption().getValue(),
                    worldManager.getMeetupSizeOption().getValue(),
                    phase.getDuration(plugin.getGameManager()),
                    Sound.BLOCK_PISTON_EXTEND,
                    """
                    El borde ha comenzado a reducirse de
                    %s a %s.
                    La reducción durará %s y finalizará al comenzar
                    el Meetup.
                    """,
                    worldManager::shrinkForMeetup
            );
        }
    };

    static final Handler MEETUP = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            WorldManager worldManager = plugin.getWorldManager();
            Duration duration = phase.getDuration(plugin.getGameManager());
            TextColor highlightColor = HardlandsColor.GREEN.primary();

            String borderSize = highlightBorderSize(worldManager.getMeetupSizeOption().getValue(), highlightColor);
            String finalShrinkTime = highlight(TextFormatters.DURATION.format(duration), highlightColor);

            AnnouncementType.GAMEPLAY.broadcast(
                    """
										El Meetup ha comenzado con un borde de %s. La
										reducción final comenzará en %s.
										""",
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
            startBorderShrink(
                    worldManager.getMeetupSizeOption().getValue(),
                    worldManager.getDeathmatchSizeOption().getValue(),
                    phase.getDuration(plugin.getGameManager()),
                    Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
                    """
                    La reducción final del borde ha comenzado. El
                    límite pasará de %s a %s
                    durante %s. El Deathmatch comenzará al finalizar.
                    """,
                    worldManager::shrinkForDeathmatch
            );
        }
    };

    static final Handler DEATHMATCH = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            String borderSize = highlightBorderSize(
                    plugin.getWorldManager().getDeathmatchSizeOption().getValue(),
                    HardlandsColor.RED.primary()
            );

            AnnouncementType.GAMEPLAY.broadcast(
                    """
										El Deathmatch ha comenzado con un borde final de
										%s. No habrá más fases después de
										esta.
										""",
                    List.of(Sound.ENTITY_WITHER_SPAWN),
                    borderSize
            );
        }
    };

    private PhaseBehavior() {}

    interface Handler {
        default void onStart(Hardlands plugin, Phase phase) {}

        default void onStop(Hardlands plugin, Phase phase) {}
    }

    private static void startBorderShrink(
            Integer initialSize,
            Integer targetSize,
            Duration duration,
            Sound sound,
            String message,
            Consumer<Duration> shrinkAction
    ) {
        TextColor highlightColor = HardlandsColor.RED.primary();

        AnnouncementType.DANGER.broadcast(
                message,
                List.of(sound),
                highlightBorderSize(initialSize, highlightColor),
                highlightBorderSize(targetSize, highlightColor),
                highlight(TextFormatters.DURATION.format(duration), highlightColor)
        );

        if (!duration.isZero()) {
            shrinkAction.accept(duration);
        }
    }

    private static String highlightGameTime(GameManager gameManager, Integer minute, TextColor color) {
        return highlight(formatGameTime(gameManager, minute), color);
    }

    private static String highlightBorderSize(Integer size, TextColor color) {
        return highlight(formatBorderSize(size), color);
    }

    private static String highlight(String text, TextColor color) {
        return "<color:%s>%s</color>".formatted(color.asHexString(), text);
    }

    private static String formatGameTime(GameManager gameManager, Integer minute) {
        Integer startMinute = Phase.SURVIVAL.getMinute(gameManager);

        if (minute == null || startMinute == null) {
            return "sin configurar";
        }

        return TextFormatters.DURATION.format(Duration.ofMinutes(Math.max(0, minute - startMinute)));
    }

    private static String formatBorderSize(Integer size) {
        return size == null
                ? "un tamaño sin configurar"
                : "%d × %d bloques".formatted(size, size);
    }
}
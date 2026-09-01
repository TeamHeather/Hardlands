package team.heather.hardlands.game.phase;

import java.time.Duration;
import java.time.LocalTime;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.core.data.json.LocalTimeAdapter;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.game.world.PregenerationManager;
import team.heather.hardlands.game.world.ScatterManager;
import team.heather.hardlands.game.world.WorldManager;
import team.heather.hardlands.ui.HardlandsColor;
import team.heather.hardlands.ui.chat.ChatMessenger;
import team.heather.hardlands.util.text.TimeFormatter;

final class PhaseBehavior {

    static final Handler OFF_GAME = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            announce(
                    "☠",
                    HardlandsColor.NEUTRAL,
                    "La partida ha terminado. El servidor ha vuelto al estado fuera de partida.",
                    Sound.BLOCK_BEACON_DEACTIVATE,
                    0.8F
            );
        }
    };

    static final Handler PREPARATION = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            announce(
                    "✦",
                    HardlandsColor.INFO,
                    "La preparación de la partida ha comenzado. Se aplicará la configuración del mundo y se pregenerarán "
                            + "los chunks necesarios; el progreso aparecerá en la barra superior.",
                    Sound.BLOCK_BEACON_ACTIVATE,
                    1.0F
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

            if (!pregenerationManager.isCompleted()) {
                pregenerationManager.pause();
            }
        }
    };

    static final Handler WAITING = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            LocalTime startTime = plugin.getGameManager().getStartTimeOption().getValue();
            String formattedStartTime = startTime == null ? "sin configurar" : LocalTimeAdapter.HHMM_FORMATTER.format(startTime);

            announce(
                    "◷",
                    HardlandsColor.INFO,
                    "La preparación ha terminado y el mundo está listo. La partida comenzará a las " + formattedStartTime + ".",
                    Sound.BLOCK_NOTE_BLOCK_CHIME,
                    1.2F
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

            announce(
                    "✧",
                    HardlandsColor.INFO,
                    "La dispersión de " + playerCount + " jugadores ha comenzado. Cada jugador será enviado a su posición "
                            + "inicial y, cuando termine el proceso, comenzará la cuenta regresiva.",
                    Sound.ENTITY_ENDERMAN_TELEPORT,
                    1.0F
            );

            Bukkit.getOnlinePlayers().forEach(scatterManager::enqueue);

            scatterManager.setProgressUpdatesEnabled(true);
            gameManager.setScatterProgress(scatterManager.getScatterPercentage());
            scatterManager.scatterAll();

            this.countdown = new StartCountdown(plugin, scatterManager);
            this.countdown.start();
        }

        @Override
        public void onStop(Hardlands plugin, Phase phase) {
            if (this.countdown != null) {
                this.countdown.close();
                this.countdown = null;
            }

            ScatterManager scatterManager = plugin.getWorldManager().getScatterManager();

            scatterManager.setProgressUpdatesEnabled(false);
            Bukkit.getOnlinePlayers().forEach(LivingEntity::clearActivePotionEffects);
        }
    };

    static final Handler SURVIVAL = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            GameManager gameManager = plugin.getGameManager();
            WorldManager worldManager = plugin.getWorldManager();

            String pvpTime = formatGameTime(gameManager, gameManager.getPvpMinuteOption().getValue());
            String borderShrinkTime = formatGameTime(gameManager, gameManager.getBorderShrinkMinuteOption().getValue());
            String meetupTime = formatGameTime(gameManager, gameManager.getMeetupMinuteOption().getValue());
            String deathmatchTime = formatGameTime(gameManager, gameManager.getDeathmatchMinuteOption().getValue());
            String borderSize = formatBorderSize(worldManager.getSurvivalSizeOption().getValue());

            announce(
                    "❣",
                    HardlandsColor.GAMEPLAY,
                    "La partida ha comenzado con un borde de " + borderSize + ". El PvP se activará en " + pvpTime
                            + " y la primera reducción del borde comenzará en " + borderShrinkTime + ". El Meetup comenzará en "
                            + meetupTime + " y el Deathmatch en " + deathmatchTime + ".",
                    Sound.UI_TOAST_CHALLENGE_COMPLETE,
                    1.0F
            );
        }
    };

    static final Handler BORDER_SHRINK = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            GameManager gameManager = plugin.getGameManager();
            WorldManager worldManager = plugin.getWorldManager();
            Duration duration = phase.getDuration(gameManager);

            announce(
                    "◆",
                    HardlandsColor.DANGER,
                    "El borde ha comenzado a reducirse de " + formatBorderSize(worldManager.getSurvivalSizeOption().getValue())
                            + " a " + formatBorderSize(worldManager.getMeetupSizeOption().getValue()) + ". La reducción durará "
                            + TimeFormatter.format(duration) + " y terminará al comenzar el Meetup.",
                    Sound.BLOCK_PISTON_EXTEND,
                    0.8F
            );

            if (!duration.isZero()) {
                worldManager.shrinkForMeetup(duration);
            }
        }
    };

    static final Handler MEETUP = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            GameManager gameManager = plugin.getGameManager();
            WorldManager worldManager = plugin.getWorldManager();
            Duration duration = phase.getDuration(gameManager);

            announce(
                    "♢",
                    HardlandsColor.GAMEPLAY,
                    "El Meetup ha comenzado y el borde se encuentra en "
                            + formatBorderSize(worldManager.getMeetupSizeOption().getValue()) + ". La reducción final comenzará en "
                            + TimeFormatter.format(duration) + ".",
                    Sound.BLOCK_BELL_USE,
                    1.0F
            );
        }
    };

    static final Handler FINAL_SHRINK = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            GameManager gameManager = plugin.getGameManager();
            WorldManager worldManager = plugin.getWorldManager();
            Duration duration = phase.getDuration(gameManager);

            announce(
                    "♦",
                    HardlandsColor.DANGER,
                    "La reducción final del borde ha comenzado. El límite pasará de "
                            + formatBorderSize(worldManager.getMeetupSizeOption().getValue()) + " a "
                            + formatBorderSize(worldManager.getDeathmatchSizeOption().getValue()) + " durante "
                            + TimeFormatter.format(duration) + "; el Deathmatch comenzará al finalizar.",
                    Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
                    0.8F
            );

            if (!duration.isZero()) {
                worldManager.shrinkForDeathmatch(duration);
            }
        }
    };

    static final Handler DEATHMATCH = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            String borderSize = formatBorderSize(plugin.getWorldManager().getDeathmatchSizeOption().getValue());

            announce(
                    "☠",
                    HardlandsColor.DANGER,
                    "El Deathmatch ha comenzado. El borde final es de " + borderSize + " y no habrá más fases después de esta.",
                    Sound.ENTITY_WITHER_SPAWN,
                    0.9F
            );
        }
    };

    private PhaseBehavior() {}

    private static void announce(String icon, HardlandsColor colors, String message, Sound sound, float pitch) {
        ChatMessenger.broadcastFramed(icon, colors, message);
        Bukkit.getOnlinePlayers().forEach(player -> player.playSound(player.getLocation(), sound, 1.0F, pitch));
    }

    private static String formatGameTime(GameManager gameManager, Integer minute) {
        Integer startMinute = Phase.SURVIVAL.getMinute(gameManager);

        if (minute == null || startMinute == null) {
            return "sin configurar";
        }

        return TimeFormatter.format(Duration.ofMinutes(Math.max(0, minute - startMinute)));
    }

    private static String formatBorderSize(Integer size) {
        return size == null ? "un tamaño sin configurar" : "%d × %d bloques".formatted(size, size);
    }

    interface Handler {

        default void onStart(Hardlands plugin, Phase phase) {}

        default void onStop(Hardlands plugin, Phase phase) {}
    }
}
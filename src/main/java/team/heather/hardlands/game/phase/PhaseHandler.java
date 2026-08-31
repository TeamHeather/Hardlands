package team.heather.hardlands.game.phase;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.game.world.PregenerationManager;
import team.heather.hardlands.game.world.ScatterManager;
import team.heather.hardlands.game.world.WorldManager;
import team.heather.hardlands.ui.feedback.ChatMessenger;

import java.time.Duration;

public interface PhaseHandler {

    PhaseHandler OFF_GAME = new PhaseHandler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {}

        @Override
        public void onStop(Hardlands plugin, Phase phase) {}
    };

    PhaseHandler PRE_GENERATION = new PhaseHandler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            WorldManager worldManager = plugin.getWorldManager();
            PregenerationManager pregenerationManager = worldManager.getPregenerationManager();

            plugin.getGameManager().getTimerManager().setProgressCondition(pregenerationManager::isCompleted);
            pregenerationManager.setProgressUpdatesEnabled(true);

            worldManager.applyConfiguration();
            worldManager.pregenerate();
        }

        @Override
        public void onStop(Hardlands plugin, Phase phase) {
            PregenerationManager pregenerationManager = plugin.getWorldManager().getPregenerationManager();

            pregenerationManager.setProgressUpdatesEnabled(false);
            plugin.getGameManager().getTimerManager().resetProgressCondition();

            if (!pregenerationManager.isCompleted()) {
                pregenerationManager.pause();
            }
        }
    };

    PhaseHandler WAITING = debug("WAITING");

    PhaseHandler SCATTER = new PhaseHandler() {

        private static final int START_DELAY_SECONDS = 10;

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            ScatterManager scatterManager = plugin.getWorldManager().getScatterManager();

            Bukkit.getOnlinePlayers().forEach(scatterManager::enqueue);

            scatterManager.setProgressUpdatesEnabled(true);
            scatterManager.scatterAll();

            startCountdown(plugin, scatterManager);
        }

        @Override
        public void onStop(Hardlands plugin, Phase phase) {
            ScatterManager scatterManager = plugin.getWorldManager().getScatterManager();

            scatterManager.setProgressUpdatesEnabled(false);

            Bukkit.getOnlinePlayers().forEach(LivingEntity::clearActivePotionEffects);
        }

        private static void startCountdown(Hardlands plugin, ScatterManager scatterManager) {
            for (int seconds = START_DELAY_SECONDS; seconds >= 1; seconds--) {
                int remaining = seconds;
                long delay = (START_DELAY_SECONDS - seconds) * 20L;

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (plugin.getGameManager().getPhase() != Phase.SCATTER) return;
                    if (!scatterManager.isCompleted()) return;

                    Component title = Component.text(remaining);
                    Component subtitle = Component.text("La partida comienza en");

                    Bukkit.getOnlinePlayers().forEach(player ->
                            player.showTitle(Title.title(
                                    title,
                                    subtitle,
                                    Title.Times.times(
                                            Duration.ZERO,
                                            Duration.ofSeconds(2),
                                            Duration.ZERO
                                    )
                            ))
                    );
                }, delay);
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (plugin.getGameManager().getPhase() != Phase.SCATTER) return;
                if (!scatterManager.isCompleted()) return;

                plugin.getGameManager().changePhase(Phase.SURVIVAL);
            }, START_DELAY_SECONDS * 20L);
        }
    };

    PhaseHandler SURVIVAL = new PhaseHandler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            plugin.getGameManager().getTimerManager().resetChronometer();

            ChatMessenger.broadcastFramed("ᴇʟ ᴊᴜᴇɢᴏ ʜᴀ ᴄᴏᴍᴇɴᴢᴀᴅᴏ.");
        }

        @Override
        public void onStop(Hardlands plugin, Phase phase) {}
    };


    PhaseHandler BORDER_SHRINK = debug("BORDER_SHRINK");

    PhaseHandler MEETUP = debug("MEETUP");

    PhaseHandler FINAL_SHRINK = debug("FINAL_SHRINK");

    PhaseHandler DEATHMATCH = debug("DEATHMATCH");

    void onStart(Hardlands plugin, Phase phase);

    void onStop(Hardlands plugin, Phase phase);

    private static PhaseHandler debug(String name) {
        return new PhaseHandler() {

            @Override
            public void onStart(Hardlands plugin, Phase phase) {
                Bukkit.getServer()
                        .getConsoleSender()
                        .sendMessage("[Hardlands] " + name + " phase started.");
            }

            @Override
            public void onStop(Hardlands plugin, Phase phase) {
                Bukkit.getServer()
                        .getConsoleSender()
                        .sendMessage("[Hardlands] " + name + " phase stopped.");
            }
        };
    }
}
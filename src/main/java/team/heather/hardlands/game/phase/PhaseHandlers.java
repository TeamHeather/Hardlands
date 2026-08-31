package team.heather.hardlands.game.phase;

import java.time.Duration;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.game.world.PregenerationManager;
import team.heather.hardlands.game.world.ScatterManager;
import team.heather.hardlands.game.world.WorldManager;
import team.heather.hardlands.ui.chat.ChatMessenger;

final class PhaseHandlers {

    static final PhaseHandler OFF_GAME = new PhaseHandler() {};

    static final PhaseHandler PRE_GENERATION = new PhaseHandler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            GameManager gameManager = plugin.getGameManager();
            WorldManager worldManager = plugin.getWorldManager();
            PregenerationManager pregenerationManager = worldManager.getPregenerationManager();

            gameManager.setTimerProgressCondition(pregenerationManager::isCompleted);

            pregenerationManager.setProgressUpdatesEnabled(true);
            gameManager.updatePregenerationProgress(pregenerationManager.getProgress());

            worldManager.applyConfiguration();
            worldManager.pregenerate();
        }

        @Override
        public void onStop(Hardlands plugin, Phase phase) {
            GameManager gameManager = plugin.getGameManager();
            PregenerationManager pregenerationManager = plugin
                    .getWorldManager()
                    .getPregenerationManager();

            pregenerationManager.setProgressUpdatesEnabled(false);
            gameManager.resetTimerProgressCondition();

            if (!pregenerationManager.isCompleted()) {
                pregenerationManager.pause();
            }
        }
    };

    static final PhaseHandler WAITING = new PhaseHandler() {};

    static final PhaseHandler SCATTER = new PhaseHandler() {

        private StartCountdown countdown;

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            GameManager gameManager = plugin.getGameManager();
            ScatterManager scatterManager = plugin.getWorldManager().getScatterManager();

            Bukkit.getOnlinePlayers().forEach(scatterManager::enqueue);

            scatterManager.setProgressUpdatesEnabled(true);
            gameManager.updateScatterProgress(scatterManager.getScatterPercentage());

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

    static final PhaseHandler SURVIVAL = new PhaseHandler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            plugin.getGameManager().resetChronometer();

            ChatMessenger.broadcastFramed("ᴇʟ ᴊᴜᴇɢᴏ ʜᴀ ᴄᴏᴍᴇɴᴢᴀᴅᴏ.");
        }
    };

    static final PhaseHandler BORDER_SHRINK = new PhaseHandler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            Duration duration = phase.getDuration(plugin.getGameManager());
            if (duration.isZero()) return;

            plugin.getWorldManager().shrinkForMeetup(duration);
        }
    };

    static final PhaseHandler MEETUP = new PhaseHandler() {};

    static final PhaseHandler FINAL_SHRINK = new PhaseHandler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            Duration duration = phase.getDuration(plugin.getGameManager());
            if (duration.isZero()) return;

            plugin.getWorldManager().shrinkForDeathmatch(duration);
        }
    };

    static final PhaseHandler DEATHMATCH = new PhaseHandler() {};

    private PhaseHandlers() {}
}
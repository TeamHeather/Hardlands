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

final class PhaseBehavior {

    static final Handler OFF_GAME = new Handler() {};

    static final Handler PREPARATION = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            WorldManager worldManager = plugin.getWorldManager();
            PregenerationManager pregenerationManager = worldManager.getPregenerationManager();

            pregenerationManager.setProgressUpdatesEnabled(true);

            worldManager.applyConfiguration();
            worldManager.pregenerate();

            plugin.getGameManager().updatePreparationProgress(
                    pregenerationManager.getProgress()
            );
        }

        @Override
        public void onStop(Hardlands plugin, Phase phase) {
            PregenerationManager pregenerationManager = plugin
                    .getWorldManager()
                    .getPregenerationManager();

            pregenerationManager.setProgressUpdatesEnabled(false);

            if (!pregenerationManager.isCompleted()) {
                pregenerationManager.pause();
            }
        }
    };

    static final Handler WAITING = new Handler() {};

    static final Handler SCATTER = new Handler() {

        private StartCountdown countdown;

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            GameManager gameManager = plugin.getGameManager();
            ScatterManager scatterManager = plugin
                    .getWorldManager()
                    .getScatterManager();

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

            ScatterManager scatterManager = plugin
                    .getWorldManager()
                    .getScatterManager();

            scatterManager.setProgressUpdatesEnabled(false);
            Bukkit.getOnlinePlayers().forEach(LivingEntity::clearActivePotionEffects);
        }
    };

    static final Handler SURVIVAL = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            plugin.getGameManager().resetChronometer();
            ChatMessenger.broadcastFramed("ᴇʟ ᴊᴜᴇɢᴏ ʜᴀ ᴄᴏᴍᴇɴᴢᴀᴅᴏ.");
        }
    };

    static final Handler BORDER_SHRINK = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            Duration duration = phase.getDuration(plugin.getGameManager());

            if (!duration.isZero()) {
                plugin.getWorldManager().shrinkForMeetup(duration);
            }
        }
    };

    static final Handler MEETUP = new Handler() {};

    static final Handler FINAL_SHRINK = new Handler() {

        @Override
        public void onStart(Hardlands plugin, Phase phase) {
            Duration duration = phase.getDuration(plugin.getGameManager());

            if (!duration.isZero()) {
                plugin.getWorldManager().shrinkForDeathmatch(duration);
            }
        }
    };

    static final Handler DEATHMATCH = new Handler() {};

    private PhaseBehavior() {}

    interface Handler {

        default void onStart(Hardlands plugin, Phase phase) {}

        default void onStop(Hardlands plugin, Phase phase) {}
    }
}
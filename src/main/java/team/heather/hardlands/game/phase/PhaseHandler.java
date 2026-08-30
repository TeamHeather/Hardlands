package team.heather.hardlands.game.phase;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.game.world.PregenerationManager;
import team.heather.hardlands.game.world.ScatterManager;
import team.heather.hardlands.game.world.WorldManager;
import team.heather.hardlands.ui.feedback.ChatMessenger;

public interface PhaseHandler {

    PhaseHandler IDLE = new PhaseHandler() {

        @Override
        public void onStart(Phase phase) {
        }

        @Override
        public void onStop(Phase phase) {

        }
    };

    PhaseHandler PRE_GENERATION = new PhaseHandler() {

        @Override
        public void onStart(Phase phase) {
            Hardlands plugin = Hardlands.getInstance();
            WorldManager worldManager = plugin.getWorldManager();
            PregenerationManager pregenerationManager = worldManager.getPregenerationManager();

            plugin.getGameManager().setTimerTickCondition(pregenerationManager::isCompleted);
            pregenerationManager.setProgressUpdatesEnabled(true);

            worldManager.applyConfiguration();
            worldManager.pregenerate();
        }

        @Override
        public void onStop(Phase phase) {
            Hardlands plugin = Hardlands.getInstance();
            PregenerationManager pregenerationManager = plugin.getWorldManager().getPregenerationManager();

            if (!pregenerationManager.isCompleted()) pregenerationManager.pause();

            pregenerationManager.setProgressUpdatesEnabled(false);
            plugin.getGameManager().resetTimerTickCondition();
        }
    };

    PhaseHandler WAITING = debug("WAITING");

    PhaseHandler SCATTER = new PhaseHandler() {

        @Override
        public void onStart(Phase phase) {
            Hardlands plugin = Hardlands.getInstance();
            ScatterManager scatterManager = plugin.getWorldManager().getScatterManager();

            Bukkit.getOnlinePlayers().forEach(scatterManager::enqueue);

            plugin.getGameManager().setTimerTickCondition(scatterManager::isCompleted);
            scatterManager.setProgressUpdatesEnabled(true);

            scatterManager.scatterAll();
        }

        @Override
        public void onStop(Phase phase) {
            Hardlands plugin = Hardlands.getInstance();
            ScatterManager scatterManager = plugin.getWorldManager().getScatterManager();

            Bukkit.getOnlinePlayers().forEach(LivingEntity::clearActivePotionEffects);

            scatterManager.setProgressUpdatesEnabled(false);
            plugin.getGameManager().resetTimerTickCondition();
        }
    };

    PhaseHandler SURVIVAL = new PhaseHandler() {

        @Override
        public void onStart(Phase phase) {
            ChatMessenger.broadcastFramed("ᴇʟ ᴊᴜᴇɢᴏ ʜᴀ ᴄᴏᴍᴇɴᴢᴀᴅᴏ.");
        }

        @Override
        public void onStop(Phase phase) {

        }
    };

    PhaseHandler BORDER_SHRINK = debug("BORDER_SHRINK");

    PhaseHandler MEETUP = debug("MEETUP");

    PhaseHandler FINAL_SHRINK = debug("FINAL_SHRINK");

    PhaseHandler DEATHMATCH = debug("DEATHMATCH");

    PhaseHandler POST_GAME = debug("POST_GAME");

    void onStart(Phase phase);

    void onStop(Phase phase);

    private static PhaseHandler debug(String name) {
        return new PhaseHandler() {

            @Override
            public void onStart(Phase phase) {
                Bukkit.getServer().getConsoleSender().sendMessage("[Hardlands] " + name + " phase started.");
            }

            @Override
            public void onStop(Phase phase) {
                Bukkit.getServer().getConsoleSender().sendMessage("[Hardlands] " + name + " phase stopped.");
            }
        };
    }
}
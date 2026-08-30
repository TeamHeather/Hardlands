package team.heather.hardlands.game.phase;

import org.bukkit.Bukkit;
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
            ChatMessenger.broadcastFramed("ᴇʟ ᴊᴜᴇɢᴏ ʜᴀ ᴄᴏᴍᴇɴᴢᴀᴅᴏ.");
        }

        @Override
        public void onStop(Phase phase) {

        }
    };

    PhaseHandler WAITING = debug("WAITING");

    PhaseHandler SCATTER = debug("SCATTER");

    PhaseHandler GRACE_PERIOD = new PhaseHandler() {

        @Override
        public void onStart(Phase phase) {
        }

        @Override
        public void onStop(Phase phase) {

        }
    };

    PhaseHandler PVP = debug("PVP");

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
package team.heather.hardlands.game.phase;

import org.bukkit.Bukkit;

public final class WaitingHandler implements PhaseHandler {

        @Override
        public void onStart(Phase phase) {
                Bukkit.getServer().getConsoleSender().sendMessage("[Hardlands] WAITING phase started.");
        }

        @Override
        public void onStop(Phase phase) {
                Bukkit.getServer().getConsoleSender().sendMessage("[Hardlands] WAITING phase stopped.");
        }
}
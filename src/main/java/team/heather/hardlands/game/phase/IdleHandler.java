package team.heather.hardlands.game.phase;

import org.bukkit.Bukkit;

public final class IdleHandler implements PhaseHandler {

        @Override
        public void onStart(Phase phase) {
                Bukkit.getServer().getConsoleSender().sendMessage("[Hardlands] IDLE phase started.");
        }

        @Override
        public void onStop(Phase phase) {
                Bukkit.getServer().getConsoleSender().sendMessage("[Hardlands] IDLE phase started.");
        }
}
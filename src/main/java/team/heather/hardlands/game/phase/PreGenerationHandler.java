package team.heather.hardlands.game.phase;

import org.bukkit.Bukkit;

public final class PreGenerationHandler implements PhaseHandler {

        @Override
        public void onStart(Phase phase) {
                Bukkit.getServer().getConsoleSender().sendMessage("[Hardlands] PREGEN phase started.");
        }

        @Override
        public void onStop(Phase phase) {
                Bukkit.getServer().getConsoleSender().sendMessage("[Hardlands] PREGEN phase started.");
        }
}
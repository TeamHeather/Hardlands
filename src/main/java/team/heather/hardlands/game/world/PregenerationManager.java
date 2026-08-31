package team.heather.hardlands.game.world;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.popcraft.chunky.api.ChunkyAPI;
import org.popcraft.chunky.api.event.task.GenerationCompleteEvent;
import org.popcraft.chunky.api.event.task.GenerationProgressEvent;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.ui.inventory.HardlandsInventory;

public final class PregenerationManager {

    private final Map<String, PregenerationTask> pregenerating;
    private final ChunkyAPI chunky;

    private volatile boolean progressUpdatesEnabled;

    public PregenerationManager(ChunkyAPI chunky) {
        this.pregenerating = new HashMap<>();
        this.chunky = chunky;

        this.chunky.onGenerationProgress(this::handleGenerationProgress);
        this.chunky.onGenerationComplete(this::handleGenerationComplete);
    }

    public synchronized void review(PregenerationRequest request) {
        String worldName = request.worldName();

        if (!request.reviewAndStart(this.chunky)) {
            throw new IllegalStateException("Chunky is already pregenerating world: " + worldName);
        }

        this.pregenerating.put(worldName, PregenerationTask.running());
        this.refreshPreparationItem();
    }

    public synchronized void pause() {
        this.pregenerating.replaceAll((worldName, task) -> {
            if (task.state() != State.RUNNING) return task;

            if (!this.chunky.pauseTask(worldName)) {
                throw new IllegalStateException("Unable to pause pregeneration for world: " + worldName);
            }

            return task.withState(State.PAUSED);
        });

        this.refreshPreparationItem();
    }

    public synchronized void resume() {
        this.pregenerating.replaceAll((worldName, task) -> {
            if (task.state() != State.PAUSED) return task;

            if (!this.chunky.continueTask(worldName)) {
                throw new IllegalStateException("Unable to resume pregeneration for world: " + worldName);
            }

            return task.withState(State.RUNNING);
        });

        this.refreshPreparationItem();
    }

    public synchronized void setProgressUpdatesEnabled(boolean enabled) {
        this.progressUpdatesEnabled = enabled;
    }

    public synchronized State getState() {
        if (this.pregenerating.isEmpty()) return State.IDLE;

        for (PregenerationTask task : this.pregenerating.values()) {
            if (task.state() == State.RUNNING) return State.RUNNING;
            if (task.state() == State.PAUSED) return State.PAUSED;
        }

        return State.COMPLETED;
    }

    public synchronized float getProgress() {
        if (this.pregenerating.isEmpty()) return 0.0F;

        float progress = 0.0F;

        for (PregenerationTask task : this.pregenerating.values()) {
            progress += task.progress();
        }

        return progress / this.pregenerating.size();
    }

    private synchronized void handleGenerationProgress(GenerationProgressEvent event) {
        PregenerationTask task = this.pregenerating.get(event.world());
        if (task == null || task.state() == State.COMPLETED) return;

        float progress = Math.clamp(event.progress(), 0.0F, 100.0F);

        this.pregenerating.put(
                event.world(),
                progress >= 100.0F ? task.completed() : task.withProgress(progress)
        );

        if (this.progressUpdatesEnabled) this.updateGameProgress();

        this.refreshPreparationItem();
    }

    private synchronized void handleGenerationComplete(GenerationCompleteEvent event) {
        PregenerationTask task = this.pregenerating.get(event.world());
        if (task == null || task.state() == State.COMPLETED) return;

        this.pregenerating.put(event.world(), task.completed());

        if (this.progressUpdatesEnabled) this.updateGameProgress();

        this.refreshPreparationItem();
    }

    private void updateGameProgress() {
        float progress = this.getProgress();

        Bukkit.getScheduler().runTask(
                Hardlands.getInstance(),
                () -> Hardlands.getInstance()
                        .getGameManager()
                        .getTimerManager()
                        .updatePregenerationProgress(progress)
        );
    }

    private void refreshPreparationItem() {
        Bukkit.getScheduler().runTask(
                Hardlands.getInstance(),
                HardlandsInventory::refreshPreparationItems
        );
    }

    private record PregenerationTask(State state, float progress) {

        private PregenerationTask withState(State state) {
            return new PregenerationTask(state, this.progress);
        }

        private PregenerationTask withProgress(float progress) {
            return new PregenerationTask(this.state, progress);
        }

        private PregenerationTask completed() {
            return new PregenerationTask(State.COMPLETED, 100.0F);
        }

        private static PregenerationTask running() {
            return new PregenerationTask(State.RUNNING, 0.0F);
        }
    }

    public enum State {

        IDLE("Sin iniciar", Material.BEDROCK),
        RUNNING("En progreso", Material.DIRT),
        PAUSED("Pausado", Material.STONE),
        COMPLETED("Completado", Material.GRASS_BLOCK);

        private final String name;
        private final Material material;

        State(String name, Material material) {
            this.name = name;
            this.material = material;
        }

        public String getName() {
            return this.name;
        }

        public Material getMaterial() {
            return this.material;
        }
    }

    public synchronized boolean isCompleted() {
        return !this.pregenerating.isEmpty()
                && this.pregenerating.values().stream()
                .allMatch(task -> task.state() == State.COMPLETED);
    }
}
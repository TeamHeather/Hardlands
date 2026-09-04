package team.heather.hardlands.game.world;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.popcraft.chunky.api.ChunkyAPI;
import org.popcraft.chunky.api.event.task.GenerationCompleteEvent;
import org.popcraft.chunky.api.event.task.GenerationProgressEvent;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.common.ui.inventory.HardlandsInventory;

public final class PregenerationManager {

    private final Map<String, PregenerationTask> pregenerating;
    private final ChunkyAPI chunky;

    private boolean progressUpdatesEnabled;

    public PregenerationManager(ChunkyAPI chunky) {
        this.pregenerating =  new HashMap<>();
        this.chunky = chunky;
        this.chunky.onGenerationProgress(this::handleGenerationProgress);
        this.chunky.onGenerationComplete(this::handleGenerationComplete);
    }

    public synchronized void progressUpdatesEnabled(boolean enabled) {
        this.progressUpdatesEnabled = enabled;
        if (enabled) {
            this.updateGameProgress();
        }
    }

    public synchronized void review(Request request) {
        String worldName = request.worldName();

        if (!request.reviewAndStart(this.chunky)) {
            throw new IllegalStateException("Chunky is already pregenerating world: " + worldName);
        }

        this.pregenerating.put(worldName, PregenerationTask.running());
        this.refreshPreparationItem();
    }

    public synchronized void pause() {
        this.pregenerating.replaceAll((worldName, task) -> {
                    if (task.state() != State.RUNNING) {
                        return task;
                    }

                    if (!this.chunky.pauseTask(worldName)) {
                        throw new IllegalStateException("Unable to pause pregeneration for world: " + worldName);
                    }

                    return task.withState(State.PAUSED);
                }
        );
        this.refreshPreparationItem();
    }

    public synchronized void resume() {
        this.pregenerating.replaceAll((worldName, task) -> {
                    if (task.state() != State.PAUSED) {
                        return task;
                    }

                    if (!this.chunky.continueTask(worldName)) {
                        throw new IllegalStateException("Unable to resume pregeneration for world: " + worldName);
                    }

                    return task.withState(State.RUNNING);
                }
        );
        this.refreshPreparationItem();
    }

    public synchronized State state() {
        if (this.pregenerating.isEmpty()) {
            return State.IDLE;
        }

        boolean paused = false;

        for (PregenerationTask task : this.pregenerating.values()) {
            if (task.state() == State.RUNNING) {
                return State.RUNNING;
            }

            if (task.state() == State.PAUSED) {
                paused = true;
            }
        }

        return paused
                ? State.PAUSED
                : State.COMPLETED;
    }

    public synchronized float progress() {
        if (this.pregenerating.isEmpty()) {
            return 0.0F;
        }

        float progress = 0.0F;

        for (PregenerationTask task
                : this.pregenerating.values()) {

            progress += task.progress();
        }

        return progress / this.pregenerating.size();
    }

    public synchronized boolean completed() {
        if (this.pregenerating.isEmpty()) {
            return false;
        }

        for (PregenerationTask task : this.pregenerating.values()) {
            if (task.state() != State.COMPLETED) {
                return false;
            }
        }

        return true;
    }

    private synchronized void handleGenerationProgress(GenerationProgressEvent event) {
        PregenerationTask task = this.pregenerating.get(event.world());

        if (task == null || task.state() == State.COMPLETED) {
            return;
        }

        float progress = Math.clamp(event.progress(), 0.0F, 100.0F);

        this.pregenerating.put(event.world(), progress >= 100.0F
                ? task.completed()
                : task.withProgress(progress));

        if (this.progressUpdatesEnabled) {
            this.updateGameProgress();
        }

        this.refreshPreparationItem();
    }

    private synchronized void handleGenerationComplete(GenerationCompleteEvent event) {
        PregenerationTask task = this.pregenerating.get(event.world());

        if (task == null || task.state() == State.COMPLETED) {
            return;
        }

        this.pregenerating.put(event.world(), task.completed());

        if (this.progressUpdatesEnabled) {
            this.updateGameProgress();
        }

        this.refreshPreparationItem();
    }

    private void updateGameProgress() {
        this.runOnMainThread(() -> Hardlands.getInstance()
                .getGameManager()
                .setPreparationProgress(this.progress()));
    }

    private void refreshPreparationItem() {
        this.runOnMainThread(HardlandsInventory::refreshPreparationItems);
    }

    private void runOnMainThread(Runnable action) {
        if (Bukkit.isPrimaryThread()) {
            action.run();
            return;
        }

        Bukkit.getScheduler().runTask(Hardlands.getInstance(), action);
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

    public record Request(
            String worldName,
            double centerX,
            double centerZ,
            double worldSize
    ) {

        public boolean reviewAndStart(ChunkyAPI chunky) {
            double radius = this.worldSize / 2.0D;
            return chunky.startTask(
                    this.worldName,
                    "square",
                    this.centerX,
                    this.centerZ,
                    radius,
                    radius,
                    "concentric");
        }
    }

    public enum State {

        IDLE("Sin iniciar", Material.BEDROCK),
        RUNNING("En progreso", Material.DIRT),
        PAUSED("Pausado", Material.STONE),
        COMPLETED("Completado", Material.GRASS_BLOCK);

        private final String label;
        private final Material material;

        State(String label, Material material) {
            this.label = label;
            this.material = material;
        }

        public String label() {
            return this.label;
        }

        public Material material() {
            return this.material;
        }
    }
}
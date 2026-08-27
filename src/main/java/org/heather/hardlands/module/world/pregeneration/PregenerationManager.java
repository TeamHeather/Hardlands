package org.heather.hardlands.module.world.pregeneration;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.common.ui.inventory.HardlandsInventory;
import org.popcraft.chunky.api.ChunkyAPI;
import org.popcraft.chunky.api.event.task.GenerationCompleteEvent;
import org.popcraft.chunky.api.event.task.GenerationProgressEvent;

public final class PregenerationManager {

    private final Map<String, PregenerationTask> pregenerating = new HashMap<>();
    private final ChunkyAPI chunky;

    public PregenerationManager(ChunkyAPI chunky) {
        this.chunky = chunky;
        this.chunky.onGenerationProgress(this::handleGenerationProgress);
        this.chunky.onGenerationComplete(this::handleGenerationComplete);
    }

    public synchronized void reviewAndAccept(PregenerationRequest request) {
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

        if (task == null) return;

        this.pregenerating.put(
                event.world(),
                event.progress() >= 100.0F ? task.completed() : task.withProgress(event.progress()));

        this.refreshPreparationItem();
    }

    private synchronized void handleGenerationComplete(GenerationCompleteEvent event) {
        PregenerationTask task = this.pregenerating.get(event.world());

        if (task == null) return;

        this.pregenerating.put(event.world(), task.completed());
        this.refreshPreparationItem();
    }

    private void refreshPreparationItem() {
        Bukkit.getScheduler().runTask(Hardlands.getInstance(), HardlandsInventory::refreshPreparationItems);
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

        private State(String name, Material material) {
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
}
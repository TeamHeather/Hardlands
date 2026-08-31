package team.heather.hardlands.game.timeline;

import java.time.LocalTime;
import java.util.function.IntUnaryOperator;

import org.bukkit.entity.Player;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.game.phase.Phase;

public final class GameTimeline {

    private final GameManager gameManager;
    private final TimelineCounterTracker counter;
    private final TimelineClockProgress clockProgress;
    private final TimelineBossBarManager bossBar;

    public GameTimeline(GameManager gameManager) {
        this.gameManager = gameManager;
        this.counter = new TimelineCounterTracker(gameManager);
        this.clockProgress = new TimelineClockProgress();
        this.bossBar = new TimelineBossBarManager(gameManager);
    }

    public synchronized void updateProgress() {
        Phase phase = this.gameManager.getPhase();

        if (phase.isClockProgressionEnabled()) {
            this.updateClockProgression(phase);
            return;
        }

        if (!phase.isCounterAdvanceEnabled()) return;

        this.counter.increment();
        this.bossBar.updateForTimedProgression(phase, this.counter);

        if (phase.isAutomaticAdvanceEnabled() && this.counter.hasReachedEnd(phase)) {
            this.advance(phase);
        }
    }

    public synchronized void updateState() {
        Phase phase = this.gameManager.getPhase();

        this.clockProgress.reset();
        this.bossBar.reset(phase);

        if (phase.isCounterResetEnabled()) {
            this.counter.reset();
        }

        if (phase.isClockProgressionEnabled()) {
            this.applyClockProgression(
                    phase,
                    this.clockProgress.initialize(this.getStartTime())
            );
            return;
        }

        if (phase.isCounterAdvanceEnabled()) {
            this.bossBar.updateForTimedProgression(phase, this.counter);
            return;
        }

        this.bossBar.updateForStaticProgression(phase, this.counter);
    }

    public synchronized void refreshStartTime() {
        Phase phase = this.gameManager.getPhase();

        if (!phase.isClockProgressionEnabled()) return;

        this.applyClockProgression(
                phase,
                this.clockProgress.refresh(this.getStartTime())
        );
    }

    public synchronized void updatePreparationProgress(float progress) {
        Phase phase = this.gameManager.getPhase();

        if (!phase.isPreparationProgressEnabled()) return;

        float percentage = Math.clamp(progress, 0.0F, 100.0F);

        this.bossBar.updateForPercentageProgression(
                phase,
                percentage,
                this.counter
        );

        if (percentage >= 100.0F) {
            this.advance(phase);
        }
    }

    public synchronized void updateScatterProgress(float progress) {
        Phase phase = this.gameManager.getPhase();

        if (!phase.isScatterProgressEnabled()) return;

        this.bossBar.updateForPercentageProgression(
                phase,
                Math.clamp(progress, 0.0F, 100.0F),
                this.counter
        );
    }

    public synchronized void completeCurrentPhase() {
        Phase phase = this.gameManager.getPhase();

        if (!phase.isForcedAdvanceEnabled()) return;

        if (phase.isCounterAdvanceEnabled()) {
            Integer endMinute = phase.getNextMinute(this.gameManager);

            if (endMinute != null) {
                this.counter.setFromMinute(endMinute);
            }
        }

        this.advance(phase);
    }

    public synchronized void setCounter(int seconds) {
        this.counter.set(seconds);

        Phase phase = this.gameManager.getPhase();

        if (phase.isClockProgressionEnabled()) return;

        if (phase.isCounterAdvanceEnabled()) {
            this.bossBar.updateForTimedProgression(phase, this.counter);
        } else {
            this.bossBar.updateLabel(phase, this.counter);
        }
    }

    public synchronized void resetCounter() {
        this.setCounter(0);
    }

    public synchronized void modifyCounter(IntUnaryOperator operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }

        this.setCounter(operation.applyAsInt(this.counter.get()));
    }

    public synchronized void setLabelPrefix(String labelPrefix) {
        this.bossBar.setLabelPrefix(
                labelPrefix,
                this.gameManager.getPhase(),
                this.counter
        );
    }

    public synchronized void setLabelSuffix(String labelSuffix) {
        this.bossBar.setLabelSuffix(
                labelSuffix,
                this.gameManager.getPhase(),
                this.counter
        );
    }

    public void addViewer(Player player) {
        this.bossBar.addViewer(player);
    }

    public void removeViewer(Player player) {
        this.bossBar.removeViewer(player);
    }

    private void updateClockProgression(Phase phase) {
        this.applyClockProgression(
                phase,
                this.clockProgress.update(this.getStartTime())
        );
    }

    private void applyClockProgression(
            Phase phase,
            TimelineClockProgress.State state
    ) {
        this.bossBar.updateForClockProgression(phase, state);

        if (state.complete()) {
            this.advance(phase);
        }
    }

    private void advance(Phase phase) {
        if (this.gameManager.getPhase() != phase) return;

        phase.next().ifPresent(this.gameManager::changePhase);
    }

    private LocalTime getStartTime() {
        LocalTime startTime = this.gameManager.getStartTimeOption().getValue();

        if (startTime == null) {
            throw new IllegalStateException("Start time has not been configured");
        }

        return startTime;
    }
}
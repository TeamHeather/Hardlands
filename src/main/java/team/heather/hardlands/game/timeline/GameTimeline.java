package team.heather.hardlands.game.timeline;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.function.IntUnaryOperator;

import org.bukkit.entity.Player;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.game.phase.Phase;

public final class GameTimeline {

    private final GameManager gameManager;
    private final TimelineBossBar bossBar;
    private final Clock clock;

    private int seconds;
    private float percentageProgress;

    private Instant clockStartedAt;
    private Instant clockTarget;

    public GameTimeline(GameManager gameManager) {
        this(gameManager, Clock.systemDefaultZone());
    }

    GameTimeline(GameManager gameManager, Clock clock) {
        this.gameManager = gameManager;
        this.clock = clock;
        this.bossBar = new TimelineBossBar(gameManager);
    }

    public synchronized void tick() {
        Phase phase = this.gameManager.getPhase();

        switch (phase.getProgressSource()) {
            case STATIC -> {
            }

            case PERCENTAGE -> {
                if (phase.isAutomaticAdvanceEnabled()
                        && this.percentageProgress >= 100.0F) {
                    this.advance(phase);
                }
            }

            case CLOCK -> {
                if (this.updateClock(phase)
                        && phase.isAutomaticAdvanceEnabled()) {
                    this.advance(phase);
                }
            }

            case COUNTER -> {
                Integer minute = phase.getMinute(this.gameManager);

                this.seconds = minute == null
                        ? 0
                        : toSeconds(minute);

                this.bossBar.updateCounter(
                        phase,
                        this.seconds
                );
            }
        }
    }

    public synchronized void syncPhase() {
        Phase phase = this.gameManager.getPhase();

        this.percentageProgress = 0.0F;
        this.clockStartedAt = null;
        this.clockTarget = null;

        this.bossBar.reset(phase);

        switch (phase.getProgressSource()) {
            case STATIC -> {
                if (phase == Phase.OFF_GAME) {
                    this.seconds = 0;
                }

                this.bossBar.updateStatic(phase);
            }

            case PERCENTAGE ->
                    this.bossBar.updatePercentage(phase, 0.0F);

            case CLOCK -> {
                this.initializeClock();
                this.updateClock(phase);
            }

            case COUNTER -> {
                Integer minute = phase.getMinute(this.gameManager);

                if (minute != null) {
                    this.seconds = toSeconds(minute);
                }

                this.bossBar.updateCounter(phase, this.seconds);
            }
        }
    }

    public synchronized void refreshStartTime() {
        Phase phase = this.gameManager.getPhase();

        if (phase.getProgressSource() != Phase.ProgressSource.CLOCK) {
            return;
        }

        Instant now = this.clock.instant();

        if (this.clockStartedAt == null) {
            this.clockStartedAt = now;
        }

        this.clockTarget = this.resolveTarget(now, this.getStartTime());
        this.updateClock(phase);
    }

    public synchronized void updatePercentageProgress(
            Phase expectedPhase,
            float progress
    ) {
        Phase phase = this.gameManager.getPhase();

        if (phase != expectedPhase
                || phase.getProgressSource() != Phase.ProgressSource.PERCENTAGE) {
            return;
        }

        this.percentageProgress = Math.clamp(progress, 0.0F, 100.0F);
        this.bossBar.updatePercentage(phase, this.percentageProgress);
    }

    public synchronized void completeCurrentPhase() {
        Phase phase = this.gameManager.getPhase();

        if (phase == Phase.SCATTER) {
            return;
        }

        if (phase.getProgressSource() == Phase.ProgressSource.COUNTER) {
            Integer endMinute = phase.getNextMinute(this.gameManager);

            if (endMinute != null) {
                this.seconds = toSeconds(endMinute);
                this.bossBar.updateCounter(phase, this.seconds);
            }
        }

        this.advance(phase);
    }

    public synchronized void setCounter(int seconds) {
        this.seconds = Math.max(0, seconds);

        Phase phase = this.gameManager.getPhase();

        if (phase.getProgressSource() == Phase.ProgressSource.COUNTER) {
            this.bossBar.updateCounter(phase, this.seconds);
        }
    }

    public synchronized void resetCounter() {
        Phase phase = this.gameManager.getPhase();
        Integer minute = phase.getMinute(this.gameManager);

        this.setCounter(
                phase.getProgressSource() == Phase.ProgressSource.COUNTER
                        && minute != null
                        ? toSeconds(minute)
                        : 0
        );
    }

    public synchronized void modifyCounter(IntUnaryOperator operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }

        this.setCounter(operation.applyAsInt(this.seconds));
    }

    public synchronized int getCounter() {
        return this.seconds;
    }

    public synchronized void addViewer(Player player) {
        this.bossBar.addViewer(player);
    }

    public synchronized void removeViewer(Player player) {
        this.bossBar.removeViewer(player);
    }

    private void tickCounter(Phase phase) {
        this.seconds++;
        this.bossBar.updateCounter(phase, this.seconds);

        if (phase.isAutomaticAdvanceEnabled()
                && this.hasReachedEnd(phase)) {
            this.advance(phase);
        }
    }

    private boolean hasReachedEnd(Phase phase) {
        Integer endMinute = phase.getNextMinute(this.gameManager);

        return endMinute != null
                && this.seconds >= toSeconds(endMinute);
    }

    private void initializeClock() {
        Instant now = this.clock.instant();

        this.clockStartedAt = now;
        this.clockTarget = this.resolveTarget(now, this.getStartTime());
    }

    private boolean updateClock(Phase phase) {
        if (this.clockStartedAt == null || this.clockTarget == null) {
            this.initializeClock();
        }

        Instant now = this.clock.instant();

        long totalMillis = Duration.between(
                this.clockStartedAt,
                this.clockTarget
        ).toMillis();

        long elapsedMillis = Duration.between(
                this.clockStartedAt,
                now
        ).toMillis();

        boolean complete = !now.isBefore(this.clockTarget);

        float progress = complete || totalMillis <= 0L
                ? 1.0F
                : Math.clamp(
                (float) elapsedMillis / totalMillis,
                0.0F,
                1.0F
        );

        this.bossBar.updateClock(
                phase,
                progress,
                LocalTime.ofInstant(now, this.clock.getZone()),
                this.getStartTime()
        );

        return complete;
    }

    private Instant resolveTarget(Instant reference, LocalTime targetTime) {
        ZoneId zone = this.clock.getZone();
        LocalDate date = reference.atZone(zone).toLocalDate();

        return date.atTime(targetTime)
                .atZone(zone)
                .toInstant();
    }

    private LocalTime getStartTime() {
        LocalTime startTime = this.gameManager
                .getStartTimeOption()
                .getValue();

        if (startTime == null) {
            throw new IllegalStateException(
                    "Start time has not been configured"
            );
        }

        return startTime;
    }

    private void advance(Phase phase) {
        if (this.gameManager.getPhase() != phase) {
            return;
        }

        phase.next().ifPresent(this.gameManager::transitionTo);
    }

    private static int toSeconds(int minute) {
        return minute * 60;
    }
}
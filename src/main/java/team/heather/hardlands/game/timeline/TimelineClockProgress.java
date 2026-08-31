package team.heather.hardlands.game.timeline;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

final class TimelineClockProgress {

    private final Clock clock = Clock.systemDefaultZone();

    private Instant startInstant;
    private Instant targetInstant;

    void reset() {
        this.startInstant = null;
        this.targetInstant = null;
    }

    State initialize(LocalTime targetTime) {
        Instant now = this.clock.instant();

        this.startInstant = now;
        this.targetInstant = this.resolveTarget(now, targetTime);

        return this.createState(now, targetTime);
    }

    State update(LocalTime targetTime) {
        if (this.startInstant == null || this.targetInstant == null) {
            return this.initialize(targetTime);
        }

        return this.createState(this.clock.instant(), targetTime);
    }

    State refresh(LocalTime targetTime) {
        Instant now = this.clock.instant();

        if (this.startInstant == null) {
            this.startInstant = now;
        }

        this.targetInstant = this.resolveTarget(now, targetTime);

        return this.createState(now, targetTime);
    }

    record State(
            float progress,
            LocalTime currentTime,
            LocalTime targetTime,
            boolean complete
    ) {}

    private State createState(Instant now, LocalTime targetTime) {
        boolean complete = !now.isBefore(this.targetInstant);

        long totalMillis = Duration.between(this.startInstant, this.targetInstant).toMillis();
        long remainingMillis = Duration.between(now, this.targetInstant).toMillis();

        float progress = complete || totalMillis <= 0L
                ? 0.0F
                : Math.clamp((float) remainingMillis / totalMillis, 0.0F, 1.0F);

        return new State(
                progress,
                LocalTime.ofInstant(now, this.clock.getZone()),
                targetTime,
                complete
        );
    }

    private Instant resolveTarget(Instant reference, LocalTime targetTime) {
        ZoneId zone = this.clock.getZone();
        LocalDate date = reference.atZone(zone).toLocalDate();

        return date.atTime(targetTime).atZone(zone).toInstant();
    }
}
package team.heather.hardlands.game.timeline;

import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.game.phase.Phase;

final class TimelineCounterTracker {

    private final GameManager gameManager;

    private volatile int seconds;

    TimelineCounterTracker(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    synchronized void increment() {
        this.seconds++;
    }

    void reset() {
        this.seconds = 0;
    }

    void set(int seconds) {
        this.seconds = Math.max(0, seconds);
    }

    void setFromMinute(int minute) {
        this.set(this.toSeconds(minute));
    }

    int get() {
        return this.seconds;
    }

    float getProgress(int startMinute, int endMinute) {
        int start = this.toSeconds(startMinute);
        int end = this.toSeconds(endMinute);

        if (end <= start) {
            return 1.0F;
        }

        return Math.clamp(
                (float) (this.seconds - start) / (end - start),
                0.0F,
                1.0F
        );
    }

    boolean hasReachedEnd(Phase phase) {
        Integer endMinute = phase.getNextMinute(this.gameManager);
        return endMinute != null && this.seconds >= this.toSeconds(endMinute);
    }

    boolean isPvpEnabled() {
        return this.seconds >= this.toSeconds(this.gameManager.getPvpMinuteOption().getValue());
    }

    int getSurvivalTargetMinute() {
        return this.isPvpEnabled()
                ? this.gameManager.getBorderShrinkMinuteOption().getValue()
                : this.gameManager.getPvpMinuteOption().getValue();
    }

    private int toSeconds(int minute) {
        return (minute - this.gameManager.getGracePeriodMinuteOption().getValue()) * 60;
    }
}
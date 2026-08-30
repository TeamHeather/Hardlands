package team.heather.hardlands.util.text;

import java.time.Duration;

public final class TimeFormatter {

    private TimeFormatter() {}

    public static String format(Duration duration) {
        long totalSeconds = Math.max(0, duration.toSeconds());
        long hours = totalSeconds / 3_600;
        long minutes = totalSeconds % 3_600 / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) return "%dh %dm %ds".formatted(hours, minutes, seconds);
        if (minutes > 0) return "%dm %ds".formatted(minutes, seconds);
        return "%ds".formatted(seconds);
    }
}
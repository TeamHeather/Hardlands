package team.heather.hardlands.util.text;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class TimeFormatter {

    private TimeFormatter() {}

    public static String format(Duration duration) {
        long totalSeconds = Math.max(0L, duration.toSeconds());

        long hours = totalSeconds / 3_600;
        long minutes = totalSeconds % 3_600 / 60;
        long seconds = totalSeconds % 60;

        List<String> parts = new ArrayList<>(3);

        if (hours > 0) parts.add(hours + "h");
        if (minutes > 0) parts.add(minutes + "m");
        if (seconds > 0 || parts.isEmpty()) parts.add(seconds + "s");

        return String.join(" ", parts);
    }
}
package team.heather.hardlands.core.data.json;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public final class LocalTimeAdapter extends TypeAdapter<LocalTime> {

    public static final DateTimeFormatter HHMMSS_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);
    public static final DateTimeFormatter HHMM_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    public static String format(LocalTime value) {
        return HHMM_FORMATTER.format(value);
    }

    @Override
    public void write(JsonWriter writer, LocalTime value) throws IOException {
        writer.value(format(value));
    }

    @Override
    public LocalTime read(JsonReader reader) throws IOException {
        return LocalTime.parse(reader.nextString(), HHMM_FORMATTER);
    }
}
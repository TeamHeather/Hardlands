package team.heather.hardlands.core.data.json;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public final class LocalTimeAdapter extends TypeAdapter<LocalTime> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    public static String format(LocalTime value) {
        return FORMATTER.format(value);
    }

    @Override
    public void write(JsonWriter writer, LocalTime value) throws IOException {
        writer.value(format(value));
    }

    @Override
    public LocalTime read(JsonReader reader) throws IOException {
        return LocalTime.parse(reader.nextString(), FORMATTER);
    }
}
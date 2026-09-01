package team.heather.hardlands.internal.data.json;

import java.io.IOException;
import java.time.LocalTime;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import team.heather.hardlands.util.TextFormatters;

public final class LocalTimeAdapter extends TypeAdapter<LocalTime> {

    @Override
    public LocalTime read(JsonReader reader) throws IOException {
        return TextFormatters.LOCAL_TIME.parse(reader.nextString());
    }

    @Override
    public void write(JsonWriter writer, LocalTime value) throws IOException {
        writer.value(TextFormatters.LOCAL_TIME.format(value));
    }
}
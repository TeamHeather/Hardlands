package org.heather.hardlands.core.data.json;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class JsonDataManager<T> {

    private final Gson gson;
    private final Path path;
    private final Class<T> dataType;

    public JsonDataManager(Gson gson, Path path, Class<T> dataType) {
        this.gson = gson;
        this.path = path;
        this.dataType = dataType;
    }

    public void write(T data) {
        try {
            Path parent = this.path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            String json = this.gson.toJson(data);
            Files.writeString(this.path, json);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write JSON data to " + this.path, exception);
        }
    }

    public Optional<T> read() {
        if (Files.notExists(this.path)) {
            return Optional.empty();
        }

        try {
            String json = Files.readString(this.path);
            T data = this.gson.fromJson(json, this.dataType);

            return Optional.ofNullable(data);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read JSON data from " + this.path, exception);
        }
    }
}

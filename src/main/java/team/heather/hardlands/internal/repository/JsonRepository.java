package team.heather.hardlands.internal.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import team.heather.hardlands.Hardlands;
import team.heather.hardlands.internal.json.JsonDataManager;

public abstract class JsonRepository<K> {

    private static final String FILE_EXTENSION = ".json";
    private final Path directory;

    protected JsonRepository(Hardlands hardlands, String directory) {
        this.directory = hardlands.getDataPath().resolve(directory);
    }

    public void delete(K key) {
        try {
            Files.deleteIfExists(this.pathFor(key));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public boolean exists(K key) {
        return Files.isRegularFile(this.pathFor(key));
    }

    protected final <T> JsonDataManager<T> managerFor(K key, Class<T> dataType) {
        return new JsonDataManager<>(
                Hardlands.GSON,
                this.pathFor(key),
                dataType
        );
    }

    protected final List<String> entryNames() {
        if (Files.notExists(this.directory)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.list(this.directory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(FILE_EXTENSION))
                    .map(name -> name.substring(0, name.length() - FILE_EXTENSION.length()))
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private Path pathFor(K key) {
        return this.directory.resolve(key + FILE_EXTENSION);
    }
}
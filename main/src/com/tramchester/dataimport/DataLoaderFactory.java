package com.tramchester.dataimport;

import java.nio.file.Path;

public class DataLoaderFactory {
    private final Path path;
    private final String extension;

    public DataLoaderFactory(Path path, String extension) {
        this.path = path;
        this.extension = extension;
    }

    public <T> DataLoader<T> getLoaderFor(TransportDataReader.InputFiles inputfileType, Class<T> targetType) {
        return new DataLoader<>(formPath(inputfileType), targetType);
    }

    private Path formPath(TransportDataReader.InputFiles theType) {
        String filename = theType.name() + extension;
        return path.resolve(filename);
    }
}

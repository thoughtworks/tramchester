package com.tramchester.dataimport;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;

import java.nio.file.Path;

public class DataLoaderFactory {
    private final static String extension = ".txt";

    private final Path path;
    private final CsvMapper mapper;

    public DataLoaderFactory(Path path, CsvMapper mapper) {
        this.path = path;
        this.mapper = mapper;
    }

    public <T> DataLoader<T> getLoaderFor(TransportDataReader.InputFiles inputfileType, Class<T> targetType) {
        return new DataLoader<>(formPath(inputfileType), targetType, mapper);
    }

    private Path formPath(TransportDataReader.InputFiles theType) {
        String filename = theType.name() + extension;
        return path.resolve(filename);
    }
}

package com.tramchester.dataimport;

import com.tramchester.dataimport.parsers.CSVEntryMapper;

import java.nio.file.Path;

public class DataLoaderFactory {
    private final Path path;

    public DataLoaderFactory(Path path) {
        this.path = path;
    }

    public <T> DataLoader<T> getLoaderFor(TransportDataReader.InputFiles theType, CSVEntryMapper<T> mapper) {
        return new DataLoader<>(formPath(theType), mapper);
    }

    private String formPath(TransportDataReader.InputFiles theType) {
        String filename = theType.name()+".txt";
        return path.resolve(filename).toAbsolutePath().toString();
    }
}

package com.tramchester.dataimport.loader.files;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.tramchester.dataimport.loader.TransportDataReader;

import java.nio.file.Path;

public class TransportDataFromFileFactory {
    private final static String extension = ".txt";

    private final Path path;
    private final CsvMapper mapper;

    public TransportDataFromFileFactory(Path path, CsvMapper mapper) {
        this.path = path;
        this.mapper = mapper;
    }

    public <T> TransportDataFromCSVFile<T,T> getLoaderFor(TransportDataReader.InputFiles inputfileType, Class<T> targetType) {
        return new TransportDataFromCSVFile<>(formPath(inputfileType), targetType, mapper);
    }

    private Path formPath(TransportDataReader.InputFiles theType) {
        String filename = theType.name() + extension;
        return path.resolve(filename);
    }
}

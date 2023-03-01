package com.tramchester.caching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataexport.CsvDataSaver;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataexport.JsonDataSaver;
import com.tramchester.dataimport.loader.files.TransportDataFromCSVFile;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.dataimport.loader.files.TransportDataFromJSONFile;
import org.jetbrains.annotations.NotNull;

import javax.annotation.PostConstruct;
import java.nio.file.Path;

@LazySingleton
public class LoaderSaverFactory {
    private CsvMapper csvMapper;
    private ObjectMapper objectMapper;

    enum FileType {
        csv, json
    }

    public LoaderSaverFactory() {
    }

    @PostConstruct
    public void start() {
        this.csvMapper = CsvMapper.builder().addModule(new AfterburnerModule()).build();
        objectMapper = new ObjectMapper();
    }

    public <CACHETYPE extends CachableData> TransportDataFromFile<CACHETYPE> getDataLoaderFor(Class<CACHETYPE> theClass, Path path) {
        FileType type = getFileTypeFor(path);
        switch (type) {
            case csv -> {
                return new TransportDataFromCSVFile<>(path, theClass, csvMapper);
            }
            case json -> {
                return new TransportDataFromJSONFile<>(path, theClass, objectMapper);
            }
            default -> throw new RuntimeException("unexepected file type " +type);
        }
    }

    public <CACHETYPE> DataSaver<CACHETYPE> getDataSaverFor(Class<CACHETYPE> theClass, Path path) {
        FileType type = getFileTypeFor(path);
        switch (type) {
            case csv -> {
                return new CsvDataSaver<>(theClass, path, csvMapper);
            }
            case json -> {
                return new JsonDataSaver<>(path, objectMapper);
            }
            default -> throw new RuntimeException("unexpected file type " +type);
        }

    }

    private FileType getFileTypeFor(Path path) {
        String filename = path.getFileName().toString();
        if (filename.endsWith("csv")) {
            return FileType.csv;
        }
        if (filename.endsWith("json")) {
            return FileType.json;
        }
        throw new RuntimeException("Unrecognised extension for " + filename);
    }

}

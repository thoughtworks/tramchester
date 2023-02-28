package com.tramchester.caching;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataexport.CsvDataSaver;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.loader.files.TransportDataFromCSVFile;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import org.jetbrains.annotations.NotNull;

import javax.annotation.PostConstruct;
import java.nio.file.Path;

@LazySingleton
public class LoaderSaverFactory {
    private CsvMapper csvMapper;

    public LoaderSaverFactory() {
    }

    @PostConstruct
    public void start() {
        this.csvMapper = CsvMapper.builder().addModule(new AfterburnerModule()).build();
    }

    @NotNull
    public <CACHETYPE extends CachableData> TransportDataFromFile<CACHETYPE> getDataLoaderFor(Class<CACHETYPE> theClass, Path cacheFile) {
        return new TransportDataFromCSVFile<>(cacheFile, theClass, csvMapper);
    }

    public @NotNull <CACHETYPE> DataSaver<CACHETYPE> getDataSaverFor(Class<CACHETYPE> theClass, Path path) {
        return new CsvDataSaver<>(theClass, path, csvMapper);
    }

}

package com.tramchester.dataimport;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceInfo;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TransportDataReaderFactory implements TransportDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataReaderFactory.class);

    private final TramchesterConfig config;
    private final List<TransportDataReader> dataReaders;
    private final FetchFileModTime fetchFileModTime;

    public TransportDataReaderFactory(TramchesterConfig config, FetchFileModTime fetchFileModTime) {
        this.fetchFileModTime = fetchFileModTime;
        dataReaders = new ArrayList<>();
        this.config = config;
    }

    public List<TransportDataReader> getReaders() {
        if (dataReaders.isEmpty()) {
            config.getDataSourceConfig().forEach(config -> {
                logger.info("Creating reader for config " + config.getName());
                Path path = config.getDataPath().resolve(config.getUnzipPath());
                DataSourceInfo.NameAndVersion nameAndVersion = getNameAndVersion(config);

                DataLoaderFactory factory = new DataLoaderFactory(path, ".txt");
                TransportDataReader transportLoader = new TransportDataReader(nameAndVersion, factory, config.getHasFeedInfo());
                dataReaders.add(transportLoader);
            });
        }
        return dataReaders;

    }

    @NotNull
    private DataSourceInfo.NameAndVersion getNameAndVersion(DataSourceConfig config) {
        LocalDateTime modTime = fetchFileModTime.getFor(config);
        return new DataSourceInfo.NameAndVersion(config.getName(), modTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

}

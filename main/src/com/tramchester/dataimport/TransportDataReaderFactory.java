package com.tramchester.dataimport;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.reference.TransportMode;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class TransportDataReaderFactory implements TransportDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataReaderFactory.class);

    private final TramchesterConfig config;
    private final List<TransportDataReader> dataReaders;
    private final FetchFileModTime fetchFileModTime;

    @Inject
    public TransportDataReaderFactory(TramchesterConfig config, FetchFileModTime fetchFileModTime) {
        this.fetchFileModTime = fetchFileModTime;
        dataReaders = new ArrayList<>();
        this.config = config;
    }

    // TODO Move populate into a Start Method
    public List<TransportDataReader> getReaders() {
        if (dataReaders.isEmpty()) {
            config.getDataSourceConfig().forEach(config -> {
                logger.info("Creating reader for config " + config.getName());
                Path path = config.getDataPath().resolve(config.getUnzipPath());
                DataSourceInfo dataSourceInfo = getNameAndVersion(config);

                DataLoaderFactory factory = new DataLoaderFactory(path, ".txt");
                TransportDataReader transportLoader = new TransportDataReader(dataSourceInfo, factory, config);
                dataReaders.add(transportLoader);
            });
        }
        return dataReaders;

    }

    @NotNull
    private DataSourceInfo getNameAndVersion(DataSourceConfig config) {
        LocalDateTime modTime = fetchFileModTime.getFor(config);
        return new DataSourceInfo(config.getName(), modTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), modTime,
                TransportMode.fromGTFS(config.getTransportModes()));
    }

}

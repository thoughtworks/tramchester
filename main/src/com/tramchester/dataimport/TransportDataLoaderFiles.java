package com.tramchester.dataimport;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.reference.TransportMode;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@LazySingleton
public class TransportDataLoaderFiles implements TransportDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataLoaderFiles.class);

    private final TramchesterConfig tramchesterConfig;
    private final List<TransportDataReader> dataReaders;
    private final FetchFileModTime fetchFileModTime;
    private final CsvMapper mapper;

    @Inject
    public TransportDataLoaderFiles(TramchesterConfig tramchesterConfig, FetchFileModTime fetchFileModTime, CsvMapper mapper) {
        this.fetchFileModTime = fetchFileModTime;
        this.mapper = mapper;
        dataReaders = new ArrayList<>();
        this.tramchesterConfig = tramchesterConfig;
    }
    
    @PostConstruct
    public void start() {
        logger.info("start");
        tramchesterConfig.getGTFSDataSource().forEach(sourceConfig -> {
            logger.info("Creating reader for config " + sourceConfig.getName());
            Path path = sourceConfig.getDataPath();

            final DataSourceID dataSourceId = sourceConfig.getDataSourceId();
            if (tramchesterConfig.hasRemoteDataSourceConfig(dataSourceId)) {
                Path remoteLoadPath = tramchesterConfig.getDataRemoteSourceConfig(dataSourceId).getDataPath();
                if (!remoteLoadPath.equals(path)) {
                    throw new RuntimeException("Pass mismatch for gtfs and remote source configs: " + dataSourceId);
                }
            } else {
                logger.warn("Not remote source config found for " + dataSourceId);
            }

            DataSourceInfo dataSourceInfo = createSourceInfoFrom(sourceConfig);

            DataLoaderFactory factory = new DataLoaderFactory(path, mapper);
            TransportDataReader transportLoader = new TransportDataReader(dataSourceInfo, factory, sourceConfig);
            dataReaders.add(transportLoader);
        });
        logger.info("started");
    }

    public List<TransportDataReader> getReaders() {
        return dataReaders;
    }

    @NotNull
    private DataSourceInfo createSourceInfoFrom(GTFSSourceConfig config) {
        LocalDateTime modTime = fetchFileModTime.getFor(config);
        DataSourceID dataSourceId = config.getDataSourceId();
        return new DataSourceInfo(dataSourceId, modTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), modTime,
                TransportMode.fromGTFS(config.getTransportGTFSModes()));
    }

}

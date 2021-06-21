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

    private final TramchesterConfig config;
    private final List<TransportDataReader> dataReaders;
    private final FetchFileModTime fetchFileModTime;
    private final CsvMapper mapper;

    @Inject
    public TransportDataLoaderFiles(TramchesterConfig config, FetchFileModTime fetchFileModTime, CsvMapper mapper) {
        this.fetchFileModTime = fetchFileModTime;
        this.mapper = mapper;
        dataReaders = new ArrayList<>();
        this.config = config;
    }
    
    @PostConstruct
    public void start() {
        logger.info("start");
        config.getGTFSDataSource().forEach(config -> {
            logger.info("Creating reader for config " + config.getName());
            Path path = config.getDataPath(); //.resolve(config.getUnzipPath());
            DataSourceInfo dataSourceInfo = createSourceInfoFrom(config);

            DataLoaderFactory factory = new DataLoaderFactory(path, mapper);
            TransportDataReader transportLoader = new TransportDataReader(dataSourceInfo, factory, config);
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

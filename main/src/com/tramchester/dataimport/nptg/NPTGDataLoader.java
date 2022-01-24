package com.tramchester.dataimport.nptg;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.loader.files.TransportDataFromCSVFile;
import com.tramchester.domain.DataSourceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.nio.file.Path;
import java.util.stream.Stream;

@LazySingleton
public class NPTGDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(NPTGDataLoader.class);
    private final TramchesterConfig config;
    private boolean enabled;
    private final CsvMapper mapper;

    @Inject
    public NPTGDataLoader(TramchesterConfig config, UnzipFetchedData.Ready dataIsReady) {
        this.config = config;
        mapper = new CsvMapper();
    }

    @PostConstruct
    private void start() {
        enabled = config.hasRemoteDataSourceConfig(DataSourceID.nptg);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Stream<NPTGData> getData() {
        if (!enabled) {
            logger.warn("Is not enabled");
            return Stream.empty();
        }
        RemoteDataSourceConfig sourceConfig = config.getDataRemoteSourceConfig(DataSourceID.nptg);
        Path dataPath = sourceConfig.getDataPath();
        return loadFor(dataPath, "Localities.csv");
    }

    private Stream<NPTGData> loadFor(Path dataPath, String filename) {
        Path filePath = dataPath.resolve(filename);
        logger.info("Loading data from " + filePath.toAbsolutePath());
        TransportDataFromCSVFile<NPTGData, NPTGData> dataLoader = new TransportDataFromCSVFile<>(filePath, NPTGData.class, mapper);

        Stream<NPTGData> stream = dataLoader.load();

        //noinspection ResultOfMethodCallIgnored
        stream.onClose(this::streamClosed);
        return stream;
    }

    private void streamClosed() {
        logger.info("Closed stream");
    }
}

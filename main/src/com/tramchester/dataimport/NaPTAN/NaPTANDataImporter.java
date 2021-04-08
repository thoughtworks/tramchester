package com.tramchester.dataimport.NaPTAN;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.DataLoader;
import com.tramchester.dataimport.UnzipFetchedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

/***
 * See
 * https://data.gov.uk/dataset/ff93ffc1-6656-47d8-9155-85ea0b8f2251/national-public-transport-access-nodes-naptan
 * https://www.gov.uk/government/publications/national-public-transport-access-node-schema/naptan-and-nptg-data-sets-and-schema-guides
 */
@LazySingleton
public class NaPTANDataImporter {
    private static final Logger logger = LoggerFactory.getLogger(NaPTANDataImporter.class);

    private final CsvMapper mapper;
    private final RemoteDataSourceConfig sourceConfig;
    private Stream<StopsData> stream;
    private boolean open;

    @Inject
    public NaPTANDataImporter(TramchesterConfig config, CsvMapper mapper, UnzipFetchedData.Ready dataIsReady) {
        sourceConfig = config.getDataSourceConfig("naptan");
        this.mapper = mapper;
        open = false;
    }

    @PostConstruct
    public void start() {
        if (open) {
            logger.warn("Already started");
            return;
        }
        logger.info("starting");
        Path dataPath = sourceConfig.getDataPath();
        Path filePath = dataPath.resolve("Stops.csv");
        logger.info("Loading data from " + filePath.toAbsolutePath());
        DataLoader<StopsData> dataLoader = new DataLoader<>(filePath, StopsData.class, mapper);

        stream = dataLoader.load();
        open = true;
        stream.onClose(this::streamClosed);

        logger.info("started");
    }

    private void streamClosed() {
        logger.info("Stream closed");
        open = false;
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        if (open) {
            logger.warn("Stream was not closed, closing");
            stream.close();
        }
        logger.info("Stopped");
    }

    public Stream<StopsData> getAll() {
        return stream;
    }
}

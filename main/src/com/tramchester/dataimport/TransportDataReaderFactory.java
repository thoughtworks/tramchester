package com.tramchester.dataimport;

import com.tramchester.config.TramchesterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TransportDataReaderFactory implements TransportDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataReaderFactory.class);

    private final TramchesterConfig config;
    private final List<TransportDataReader> dataReaders;

    public TransportDataReaderFactory(TramchesterConfig config) {
        dataReaders = new ArrayList<>();
        this.config = config;
    }

    public List<TransportDataReader> getReaders() {

        if (dataReaders.isEmpty()) {
            config.getDataSourceConfig().forEach(config -> {
                logger.info("Creating reader for config " + config);
                Path path = config.getDataPath().resolve(config.getUnzipPath());
                DataLoaderFactory factory = new DataLoaderFactory(path, ".txt");
                TransportDataReader transportLoader = new TransportDataReader(factory, true);
                dataReaders.add(transportLoader);
            });
        }
        return dataReaders;

    }

    private void createTrainReaders() {
        // TODO Highly experiemental
        logger.warn("Trains enabled");
        Path dataPath = Path.of("data", "gb-rail-latest");
        DataLoaderFactory trainReaderFactory = new DataLoaderFactory(dataPath, ".txt");
        TransportDataReader trainLoader = new TransportDataReader(trainReaderFactory, false);
        dataReaders.add(trainLoader);
    }
}

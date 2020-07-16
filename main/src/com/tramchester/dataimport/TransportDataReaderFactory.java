package com.tramchester.dataimport;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.GTFSTransportationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TransportDataReaderFactory implements TransportDataLoader {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataReaderFactory.class);

    private final TramchesterConfig config;
    private TransportDataReader readerForCleanser;
    private final List<TransportDataReader> dataReaders;

    public TransportDataReaderFactory(TramchesterConfig config) {
        dataReaders = new ArrayList<>();
        this.config = config;
    }

    @Deprecated
    public TransportDataReader getForCleanser() {
        if (readerForCleanser==null) {
            Path path = config.getDataPath().resolve(config.getUnzipPath());
            DataLoaderFactory factory = new DataLoaderFactory(path, ".txt");
            readerForCleanser = new TransportDataReader(factory, true);
        }
        return readerForCleanser;
    }

    public List<TransportDataReader> getReaders() {
        if (dataReaders.isEmpty()) {
            //Path path = config.getDataPath();
            Path path = config.getDataPath().resolve(config.getUnzipPath());
            DataLoaderFactory factory = new DataLoaderFactory(path, ".txt");
            TransportDataReader transportLoader = new TransportDataReader(factory, true);
            dataReaders.add(transportLoader);
            if (config.getTransportModes().contains(GTFSTransportationType.train)) {
                createTrainReaders();
            }
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

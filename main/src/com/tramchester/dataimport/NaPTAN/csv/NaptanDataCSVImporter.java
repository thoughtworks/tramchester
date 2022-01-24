package com.tramchester.dataimport.NaPTAN.csv;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.NaPTAN.NaptanDataImporter;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.loader.files.TransportDataFromCSVFile;
import com.tramchester.domain.DataSourceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.stream.Stream;

import static java.lang.String.format;

public class NaptanDataCSVImporter<T, R extends T> implements NaptanDataImporter<T> {
    private static final Logger logger = LoggerFactory.getLogger(NaptanDataCSVImporter.class);

    private final TramchesterConfig config;
    private final CsvMapper mapper;
    private final DataSourceID dataSourceID;
    private final Class<R> concreteType;

    private Stream<T> dataStream;
    private boolean open;

    public NaptanDataCSVImporter(TramchesterConfig config, CsvMapper mapper, Class<R> concreteType, DataSourceID dataSourceID,
                                 UnzipFetchedData.Ready dataIsReady) {

        this.config = config;
        this.mapper = mapper;
        this.concreteType = concreteType;
        this.dataSourceID = dataSourceID;
        open = false;
    }

    @Override
    public void start() {
        if (!isEnabled()) {
            logger.warn(format("Naptan for %s is disabled, no config section found ", dataSourceID));
            dataStream = Stream.empty();
            return;
        }

        logger.info("start");
        RemoteDataSourceConfig sourceConfig = config.getDataRemoteSourceConfig(dataSourceID);
        loadForConfig(sourceConfig, concreteType);
        logger.info("started");
    }

    @Override
    public boolean isEnabled() {
        return config.hasRemoteDataSourceConfig(dataSourceID);
    }

    private void loadForConfig(RemoteDataSourceConfig sourceConfig, Class<R> dataClass) {
        if (open) {
            logger.warn("Already started");
            return;
        }

        Path dataPath = sourceConfig.getDataPath();

        dataStream = loadFor(dataPath, sourceConfig.getDownloadFilename(), dataClass);

        open = true;
    }

    private Stream<T> loadFor(Path dataPath, String filename, Class<R> readerClass) {
        Path filePath = dataPath.resolve(filename);
        logger.info("Loading data from " + filePath.toAbsolutePath());
        TransportDataFromCSVFile<T, R> dataLoader = new TransportDataFromCSVFile<>(filePath, readerClass, mapper);

        Stream<T> stream = dataLoader.load();

        //noinspection ResultOfMethodCallIgnored
        stream.onClose(() ->  this.streamClosed(readerClass.getSimpleName()));
        return stream;
    }

    private void streamClosed(String className) {
        logger.info(className + " stream closed");
        open = false;
    }

    @Override
    public void stop() {
        if (open) {
            logger.warn("Stream was not closed, closing");
            dataStream.close();
        }
        logger.info("Stopped");
    }

    @Override
    public Stream<T> getDataStream() {
        return dataStream;
    }
}
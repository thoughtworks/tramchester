package com.tramchester.dataimport.NaPTAN;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.domain.DataSourceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.stream.Stream;

import static java.lang.String.format;

public class NaptanDataCSVImporter<R> {
    private static final Logger logger = LoggerFactory.getLogger(NaptanDataCSVImporter.class);

    private final TramchesterConfig config;
    private final CsvMapper mapper;
    private final DataSourceID dataSourceID;
    private final Class<R> dataClass;

    private Stream<R> dataStream;
    private boolean open;

    protected NaptanDataCSVImporter(TramchesterConfig config, CsvMapper mapper, Class<R> dataClass, DataSourceID dataSourceID,
                                    UnzipFetchedData.Ready dataIsReady) {

        this.config = config;
        this.mapper = mapper;
        this.dataClass = dataClass;
        this.dataSourceID = dataSourceID;
        open = false;
    }

    protected void start() {
        if (!isEnabled()) {
            logger.warn(format("Naptan for %s is disabled, no config section found", dataSourceID));
            dataStream = Stream.empty();
            return;
        }

        RemoteDataSourceConfig sourceConfig = config.getDataRemoteSourceConfig(dataSourceID);
        loadForConfig(sourceConfig, dataClass);
    }

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

    private Stream<R> loadFor(Path dataPath, String filename, Class<R> dataClass) {
        Path filePath = dataPath.resolve(filename);
        logger.info("Loading data from " + filePath.toAbsolutePath());
        TransportDataFromFile<R> dataLoader = new TransportDataFromFile<>(filePath, dataClass, mapper);

        Stream<R> stream = dataLoader.load();

        //noinspection ResultOfMethodCallIgnored
        stream.onClose(() ->  this.streamClosed(dataClass.getSimpleName()));
        return stream;
    }

    private void streamClosed(String className) {
        logger.info(className + " stream closed");
        open = false;
    }

    protected void stop() {
        if (open) {
            logger.warn("Stream was not closed, closing");
            dataStream.close();
        }
        logger.info("Stopped");
    }

    protected Stream<R> getDataStream() {
        return dataStream;
    }
}

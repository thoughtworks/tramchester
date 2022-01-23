package com.tramchester.dataimport.NaPTAN.xml;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.NaPTAN.NaptanDataImporter;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.dataimport.loader.files.TransportDataFromXMLFile;
import com.tramchester.domain.DataSourceID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.lang.String.format;

public class NaptanDataXMLImporter<T, R extends T> implements NaptanDataImporter<T> {
    private static final Logger logger = LoggerFactory.getLogger(NaptanDataXMLImporter.class);

    private final DataSourceID dataSourceID = DataSourceID.naptanxml;

    private final TramchesterConfig config;
    private final Class<R> dataClass;

    private Stream<T> dataStream;
    private boolean open;

    public NaptanDataXMLImporter(TramchesterConfig config, Class<R> dataClass, UnzipFetchedData.Ready dataIsReady) {

        this.config = config;
        this.dataClass = dataClass;
        open = false;
    }

    @Override
    public void start() {
        if (!isEnabled()) {
            logger.warn(format("Naptan for %s is disabled, no config section found", dataSourceID));
            dataStream = Stream.empty();
            return;
        }

        logger.info("start");
        RemoteDataSourceConfig sourceConfig = config.getDataRemoteSourceConfig(dataSourceID);
        loadForConfig(sourceConfig, dataClass);
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

    private Stream<T> loadFor(Path dataPath, String filename, Class<R> dataClass) {
        Path filePath = dataPath.resolve(filename);
        logger.info("Loading data from " + filePath.toAbsolutePath());
        TransportDataFromFile<T> dataLoader = new TransportDataFromXMLFile<>(filePath, StandardCharsets.UTF_8, dataClass);

        Stream<T> stream = dataLoader.load();

        //noinspection ResultOfMethodCallIgnored
        stream.onClose(() ->  this.streamClosed(dataClass.getSimpleName()));
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

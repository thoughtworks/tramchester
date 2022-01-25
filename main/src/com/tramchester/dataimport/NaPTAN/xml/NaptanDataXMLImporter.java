package com.tramchester.dataimport.NaPTAN.xml;

import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
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

public class NaptanDataXMLImporter {
    private static final Logger logger = LoggerFactory.getLogger(NaptanDataXMLImporter.class);

    private final DataSourceID dataSourceID = DataSourceID.naptanxml;

    private final TramchesterConfig config;
    private final Class<NaptanStopXMLData> dataClass;
    private final boolean enabled;

    private RemoteDataSourceConfig sourceConfig;

    public NaptanDataXMLImporter(TramchesterConfig config, UnzipFetchedData.Ready dataIsReady) {
        this.config = config;
        this.dataClass = NaptanStopXMLData.class;
        enabled = config.hasRemoteDataSourceConfig(dataSourceID);
    }

    public void start() {
        if (!enabled) {
            logger.warn(format("Naptan for %s is disabled, no config section found", dataSourceID));
            return;
        }

        logger.info("start");
        sourceConfig = config.getDataRemoteSourceConfig(dataSourceID);
        logger.info("started");
    }

    public boolean isEnabled() {
        return enabled;
    }

    private Stream<NaptanStopXMLData> loadData() {
        Path dataPath = sourceConfig.getDataPath();
        String filename = sourceConfig.getDownloadFilename();
        Path filePath = dataPath.resolve(filename);
        logger.info("Loading data from " + filePath.toAbsolutePath());
        TransportDataFromFile<NaptanStopXMLData> dataLoader = new TransportDataFromXMLFile<>(filePath, StandardCharsets.UTF_8, dataClass);

        Stream<NaptanStopXMLData> stream = dataLoader.load();

        //noinspection ResultOfMethodCallIgnored
        stream.onClose(() ->  this.streamClosed(dataClass.getSimpleName()));
        return stream;
    }

    private void streamClosed(String className) {
        logger.info(className + " stream closed");
    }

    public void stop() {
        logger.info("Stopped");
    }

    public Stream<NaptanStopXMLData> getDataStream() {
        return loadData();
    }
}

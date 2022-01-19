package com.tramchester.dataimport.NaPTAN;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.reference.TransportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.nio.file.Path;
import java.util.stream.Stream;

/***
 * See
 * https://data.gov.uk/dataset/ff93ffc1-6656-47d8-9155-85ea0b8f2251/national-public-transport-access-nodes-naptan
 * https://www.gov.uk/government/publications/national-public-transport-access-node-schema/naptan-and-nptg-data-sets-and-schema-guides
 */
@LazySingleton
public class NaPTANDataImporter {
    private static final Logger logger = LoggerFactory.getLogger(NaPTANDataImporter.class);

    private final TramchesterConfig config;
    private final CsvMapper mapper;
    private Stream<NaptanStopData> stopsDataStream;
    private Stream<RailStationData> railStationDataStream;
    private boolean open;

    @Inject
    public NaPTANDataImporter(TramchesterConfig config, CsvMapper mapper, UnzipFetchedData.Ready dataIsReady) {
        this.config = config;
        this.mapper = mapper;
        open = false;
    }

    @PostConstruct
    public void start() {
        logger.info("starting");

        if (!isEnabled()) {
            logger.warn("Naptan is disabled, no config section found for " + DataSourceID.naptan);
            stopsDataStream = Stream.empty();
            railStationDataStream = Stream.empty();
            return;
        }

        RemoteDataSourceConfig sourceConfig = config.getDataRemoteSourceConfig(DataSourceID.naptan);
        loadForConfig(sourceConfig);

        logger.info("started");
    }

    public boolean isEnabled() {
        return config.hasRemoteDataSourceConfig(DataSourceID.naptan);
    }

    private void loadForConfig(RemoteDataSourceConfig sourceConfig) {
        if (open) {
            logger.warn("Already started");
            return;
        }

        Path dataPath = sourceConfig.getDataPath();

        stopsDataStream = loadFor(dataPath, "Stops.csv", NaptanStopData.class);

        if (config.getTransportModes().contains(TransportMode.Train)) {
            railStationDataStream = loadFor(dataPath, "RailReferences.csv", RailStationData.class);
        }

        open = true;
    }

    private <T> Stream<T> loadFor(Path dataPath, String filename, Class<T> dataClass) {
        Path filePath = dataPath.resolve(filename);
        logger.info("Loading data from " + filePath.toAbsolutePath());
        TransportDataFromFile<T> dataLoader = new TransportDataFromFile<>(filePath, dataClass, mapper);

        Stream<T> stream = dataLoader.load();

        //noinspection ResultOfMethodCallIgnored
        stream.onClose(() ->  this.streamClosed(dataClass.getSimpleName()));
        return stream;
    }

    private void streamClosed(String className) {
        logger.info(className + " stream closed");
        open = false;
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        if (open) {
            logger.warn("Stream was not closed, closing");
            stopsDataStream.close();
            if (config.getTransportModes().contains(TransportMode.Train)) {
                railStationDataStream.close();
            }
        }
        logger.info("Stopped");
    }

    public Stream<NaptanStopData> getStopsData() {
        return stopsDataStream;
    }

    public Stream<RailStationData> getRailStationData() { return railStationDataStream; }
}

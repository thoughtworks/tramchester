package com.tramchester.dataimport.postcodes;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.loader.files.TransportDataFromCSVFile;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.domain.DataSourceID;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class PostcodeDataImporter {
    // NOTE:
    // Filters load postcodes with the bounds given by current set of StationLocations

    // Useful geographic tram map at https://tfgm.com/public-transport/tram/geographical/network-map

    private static final Logger logger = LoggerFactory.getLogger(PostcodeDataImporter.class);
    public static final String CSV = ".csv";

    private static final Path dataFolder = Paths.get("Data", "CSV");

    private final StationLocations stationLocations;
    private final PostcodeBoundingBoxs postcodeBounds;
    private final FetchFileModTime fetchFileModTime;
    private final CsvMapper mapper;

    private final boolean enabled;
    private final TramchesterConfig config;

    @Inject
    public PostcodeDataImporter(TramchesterConfig config, StationLocations stationLocations,
                                PostcodeBoundingBoxs postcodeBounds, FetchFileModTime fetchFileModTime,
                                CsvMapper mapper,  UnzipFetchedData.Ready dataIsReady) {
        this.fetchFileModTime = fetchFileModTime;
        this.mapper = mapper;
        this.config = config;

        this.stationLocations = stationLocations;
        this.postcodeBounds = postcodeBounds;

        this.enabled = config.hasRemoteDataSourceConfig(DataSourceID.postcode);
    }

    public List<PostcodeDataStream> loadLocalPostcodes() {
        if (!enabled) {
            logger.warn("load postcodes attempted when data source not present");
            return Collections.emptyList();
        }

        RemoteDataSourceConfig dataSourceConfig = config.getDataRemoteSourceConfig(DataSourceID.postcode);
        Path dataFilesDirectory = dataSourceConfig.getDataPath().resolve(dataFolder);

        logger.info("Load postcode files files from " + dataFilesDirectory.toAbsolutePath());

        if (!Files.isDirectory(dataFilesDirectory)) {
            logger.error("Cannot load postcode data, location is not a directory " + dataFilesDirectory);
            return Collections.emptyList();
        }

        BoundingBox stationBounds = stationLocations.getBounds();
        Set<Path> csvFiles;
        try {
            csvFiles = Files.list(dataFilesDirectory).
                    filter(Files::isRegularFile).
                    filter(path -> path.getFileName().toString().toLowerCase().endsWith(CSV)).
                    collect(Collectors.toSet());
        } catch (IOException e) {
            logger.error("Cannot list files in postcode data location " + dataFilesDirectory.toAbsolutePath(), e);
            return Collections.emptyList();
        }

        if (csvFiles.isEmpty()) {
            logger.error("Found no matching files in " + dataFilesDirectory.toAbsolutePath());
        } else {
            logger.info("Found " + csvFiles.size() + " files in " + dataFilesDirectory.toAbsolutePath());
        }

        return csvFiles.stream().
                map(file -> loadDataFromFile(file, stationBounds)).
                filter(postcodeDataStream -> postcodeDataStream.wasLoaded).
                collect(Collectors.toList());
    }

    private PostcodeDataStream loadDataFromFile(Path file, BoundingBox loadedStationsBounds) {
        logger.debug("Load postcode data from " + file.toAbsolutePath());

        MarginInMeters walkingDistance = MarginInMeters.of(config.getNearestStopForWalkingRangeKM());

        TransportDataFromCSVFile<PostcodeData, PostcodeData> loader = new TransportDataFromCSVFile<>(file, PostcodeData.class, PostcodeData.CVS_HEADER, mapper);
        Stream<PostcodeData> stream = getPostcodesFor(loader);

        String code = postcodeBounds.convertPathToCode(file);

        if (postcodeBounds.isLoaded() && postcodeBounds.hasBoundsFor(file)) {
            BoundingBox boundsForPostcodeFile = postcodeBounds.getBoundsFor(file);
            if (boundsForPostcodeFile.overlapsWith(loadedStationsBounds)) {
                logger.info("Postcode in file match bounds " + file);
                return new PostcodeDataStream(code, true,
                        stream.
                        filter(postcode -> loadedStationsBounds.within(walkingDistance, postcode.getGridPosition())).
                        filter(postcode -> stationLocations.anyStationsWithinRangeOf(postcode.getGridPosition(), walkingDistance)));
            } else {
                logger.debug("Skipping " + file + " as contains no positions overlapping with current bounds");
                return PostcodeDataStream.empty(code);
            }
        } else {
            logger.info("Loading without pre-cached bounds " + file);
            return new PostcodeDataStream(code, true,
                stream.
                    filter(postcode -> postcodeBounds.checkOrRecord(file, postcode)).
                    filter(postcode -> loadedStationsBounds.within(walkingDistance, postcode.getGridPosition())).
                    filter(postcode -> stationLocations.anyStationsWithinRangeOf(postcode.getGridPosition(), walkingDistance)));
        }
    }

    private Stream<PostcodeData> getPostcodesFor(TransportDataFromCSVFile<PostcodeData,PostcodeData> loader) {
        return loader.load().
                filter(postcode -> postcode.getGridPosition().isValid());
    }

    public LocalDateTime getTargetFolderModTime() {
        RemoteDataSourceConfig dataSourceConfig = config.getDataRemoteSourceConfig(DataSourceID.postcode);
        return fetchFileModTime.getFor(dataSourceConfig.getDataPath());
    }

    public static class PostcodeDataStream {
        private final String code;
        private final boolean wasLoaded;
        private final Stream<PostcodeData> dataStream;

        public PostcodeDataStream(String code, boolean wasLoaded, Stream<PostcodeData> dataStream) {
            this.code = code;
            this.wasLoaded = wasLoaded;
            this.dataStream = dataStream;
        }

        public static PostcodeDataStream empty(String code) {
            return new PostcodeDataStream(code, false, Stream.empty());
        }

        public boolean wasLoaded() {
            return wasLoaded;
        }

        public String getCode() {
            return code;
        }

        public Stream<PostcodeData> getDataStream() {
            return dataStream;
        }
    }
}

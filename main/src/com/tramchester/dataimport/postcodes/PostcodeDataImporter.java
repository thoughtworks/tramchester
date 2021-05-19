package com.tramchester.dataimport.postcodes;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.DataLoader;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.geo.BoundingBox;
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
    public static final String POSTCODES_CONFIG_NAME = "postcodes";

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

        this.enabled = config.hasDataSourceConfig(POSTCODES_CONFIG_NAME);
    }

    public List<Stream<PostcodeData>> loadLocalPostcodes() {
        if (!enabled) {
            logger.warn("load postcodes attempted when data source not present");
            return Collections.emptyList();
        }

        RemoteDataSourceConfig dataSourceConfig = config.getDataSourceConfig(POSTCODES_CONFIG_NAME);
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

        return csvFiles.stream().map(file -> loadDataFromFile(file, stationBounds)).collect(Collectors.toList());
    }

    private Stream<PostcodeData> loadDataFromFile(Path file, BoundingBox loadedStationsBounds) {
        logger.info("Load postcode data from " + file.toAbsolutePath());

        Double rangeInKm = config.getNearestStopForWalkingRangeKM();
        long marginInM = Math.round(rangeInKm * 1000D);

        DataLoader<PostcodeData> loader = new DataLoader<>(file, PostcodeData.class, PostcodeData.CVS_HEADER, mapper);
        Stream<PostcodeData> postcodeDataStream = getPostcodesFor(loader);

        if (postcodeBounds.isLoaded() && postcodeBounds.hasBoundsFor(file)) {
            BoundingBox boundsForPostcodeFile = postcodeBounds.getBoundsFor(file);
            if (boundsForPostcodeFile.overlapsWith(loadedStationsBounds)) {
                return postcodeDataStream.
                        filter(postcode -> loadedStationsBounds.within(marginInM, postcode.getGridPosition())).
                        filter(postcode -> stationLocations.hasAnyNearby(postcode.getGridPosition(), rangeInKm));
            } else {
                logger.info("Skipping " + file + " as contains no positions overlapping with current bounds");
                return Stream.empty();
            }
        } else {
            return postcodeDataStream.
                    filter(postcode -> postcodeBounds.checkOrRecord(file, postcode)).
                    filter(postcode -> loadedStationsBounds.within(marginInM, postcode.getGridPosition())).
                    filter(postcode -> stationLocations.hasAnyNearby(postcode.getGridPosition(), rangeInKm));
        }
    }

    private Stream<PostcodeData> getPostcodesFor(DataLoader<PostcodeData> loader) {
        return loader.load().
                filter(postcode -> postcode.getGridPosition().isValid());
    }

    public LocalDateTime getTargetFolderModTime() {
        RemoteDataSourceConfig dataSourceConfig = config.getDataSourceConfig(POSTCODES_CONFIG_NAME);
        return fetchFileModTime.getFor(dataSourceConfig.getDataPath());
    }
}

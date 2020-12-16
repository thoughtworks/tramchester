package com.tramchester.dataimport;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.PostcodeData;
import com.tramchester.dataimport.parsers.PostcodeDataMapper;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.StationLocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
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
    private static final String CSV = ".csv";

    private final Path directory;
    private final TramchesterConfig config;
    private final StationLocations stationLocations;
    private final Unzipper unzipper;
    private final long margin;
    private final Double range;
    private final PostcodeBoundingBoxs postcodeBounds;
    private final FetchFileModTime fetchFileModTime;
    private BoundingBox requiredBounds;

    @Inject
    public PostcodeDataImporter(TramchesterConfig config, StationLocations stationLocations, Unzipper unzipper,
                                PostcodeBoundingBoxs postcodeBounds, FetchFileModTime fetchFileModTime) {
        this.directory = config.getPostcodeDataPath();
        range = config.getNearestStopForWalkingRangeKM();
        this.fetchFileModTime = fetchFileModTime;
        // meters
        margin = Math.round(range * 1000D);
        this.config = config;
        this.stationLocations = stationLocations;
        this.unzipper = unzipper;
        this.postcodeBounds = postcodeBounds;
    }

    public List<Stream<PostcodeData>> loadLocalPostcodes() {
        requiredBounds = stationLocations.getBounds();

        unzipIfRequired();

        logger.info("Load postcode files files from " + directory.toAbsolutePath());
        if (!Files.isDirectory(directory)) {
            logger.error("Cannot load postcode data, location is not a directory " + directory);
            return Collections.emptyList();
        }

        Set<Path> csvFiles;
        try {
            csvFiles = Files.list(directory).
                    filter(Files::isRegularFile).
                    filter(path -> path.getFileName().toString().toLowerCase().endsWith(CSV)).
                    filter(path -> !path.endsWith(PostcodeBoundingBoxs.HINTS_FILES)). // skip hints file
                    collect(Collectors.toSet());
        } catch (IOException e) {
            logger.error("Cannot list files in postcode data location " + directory.toAbsolutePath(), e);
            return Collections.emptyList();
        }

        if (csvFiles.isEmpty()) {
            logger.error("Found no matching files in " + directory.toAbsolutePath());
        } else {
            logger.info("Found " + csvFiles.size() + " files in " + directory.toAbsolutePath());
        }

        PostcodeDataMapper mapper = new PostcodeDataMapper();

        return csvFiles.stream().map(file -> loadDataFromFile(mapper, file)).collect(Collectors.toList());
    }

    private void unzipIfRequired() {
        Path postcodeZip = config.getPostcodeZip();
        Path unzipTarget = getTargetPath();
        if (refreshNeeded(postcodeZip, unzipTarget)) {
            unzipper.unpack(postcodeZip, unzipTarget);
        }
    }

    private Path getTargetPath() {
        return Path.of(config.getPostcodeZip().toAbsolutePath().toString().replace(".zip", ""));
    }

    private boolean refreshNeeded(Path postcodeZip, Path unzipTarget)  {
        try {
            if (Files.exists(unzipTarget)) {
                FileTime targetModTime = Files.getLastModifiedTime(unzipTarget);
                FileTime zipModTime = Files.getLastModifiedTime(postcodeZip);

                if (targetModTime.compareTo(zipModTime) <= 0) {
                    logger.info("Not unpacking, output directory is newer or same mod time");
                    return false;
                }
            }
        }
        catch (IOException exception) {
            logger.warn("Cannot check mod time, will unzip", exception);
        }
        return true;
    }

    private Stream<PostcodeData> loadDataFromFile(PostcodeDataMapper mapper, Path file) {
        logger.info("Load postcode data from " + file.toAbsolutePath());

        DataLoaderApacheCSV<PostcodeData> loader = new DataLoaderApacheCSV<>(file, mapper);

        if (postcodeBounds.hasData()) {
            BoundingBox fileBounds = postcodeBounds.getBoundsFor(file);
            if (fileBounds!=null && !fileBounds.overlapsWith(requiredBounds)) {
                logger.info("Skipping " + file + " as contains no positions overlapping with current bounds");
                return Stream.empty();
            } else {
                Stream<PostcodeData> postcodeDataStream = loader.load();

                return postcodeDataStream.
                        filter(postcode -> requiredBounds.within(margin, postcode)).
                        filter(postcode -> stationLocations.hasAnyNearby(postcode, range));
            }
        }

        Stream<PostcodeData> postcodeDataStream = loader.load();
        return postcodeDataStream.filter(postcode -> postcodeBounds.checkOrRecord(file, postcode)).
                filter(postcode -> requiredBounds.within(margin, postcode)).
                filter(postcode -> stationLocations.hasAnyNearby(postcode, range));

    }

    public LocalDateTime getTargetFolderModTime() {
        return fetchFileModTime.getFor(getTargetPath());
    }
}

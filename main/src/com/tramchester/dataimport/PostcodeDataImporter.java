package com.tramchester.dataimport;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.PostcodeData;
import com.tramchester.dataimport.parsers.PostcodeDataMapper;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.StationLocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
    private BoundingBox bounds;

    public PostcodeDataImporter(TramchesterConfig config, StationLocations stationLocations, Unzipper unzipper) {
        this.directory = config.getPostcodeDataPath();
        // meters
        margin = Math.round(config.getNearestStopRangeKM() * 1000D);
        this.config = config;
        this.stationLocations = stationLocations;
        this.unzipper = unzipper;
        range = config.getNearestStopRangeKM();
    }

    public Stream<PostcodeData> loadLocalPostcodes() {
        bounds = stationLocations.getBounds();

        Path postcodeZip = config.getPostcodeZip();
        String unzipTarget = config.getPostcodeZip().toAbsolutePath().toString().replace(".zip","");
        unzipper.unpack(postcodeZip, Path.of(unzipTarget));

        logger.info("Load postcode files files from " + directory.toAbsolutePath());
        if (!Files.isDirectory(directory)) {
            logger.error("Cannot load postcode data, location is not a directory " + directory);
            return Stream.empty();
        }

        Set<Path> csvFiles;
        try {
            csvFiles = Files.list(directory).
                    filter(Files::isRegularFile).
                    filter(path -> path.getFileName().toString().toLowerCase().endsWith(CSV)).
                    collect(Collectors.toSet());
        } catch (IOException e) {
            logger.error("Cannot list files in postcode data location " + directory.toAbsolutePath(), e);
            return Stream.empty();
        }

        if (csvFiles.isEmpty()) {
            logger.error("Found no matching files in " + directory.toAbsolutePath());
        } else {
            logger.info("Found " + csvFiles.size() + " files in " + directory.toAbsolutePath());
        }

        PostcodeDataMapper mapper = new PostcodeDataMapper();
        return csvFiles.stream().flatMap(file -> loadDataFromFile(mapper, file));

    }

    private Stream<PostcodeData> loadDataFromFile(PostcodeDataMapper mapper, Path file) {
        logger.info("Load postcode data from " + file.toAbsolutePath());

        DataLoader<PostcodeData> loader = new DataLoader<>(file, mapper);
        Stream<PostcodeData> postcodeDataStream = loader.loadFiltered(true);
        return postcodeDataStream.filter(postcode -> bounds.within(margin, postcode)).
                filter(postcode -> stationLocations.hasAnyNearby(postcode, range));
    }

}

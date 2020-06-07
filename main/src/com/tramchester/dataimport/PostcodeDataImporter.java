package com.tramchester.dataimport;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.PostcodeData;
import com.tramchester.dataimport.parsers.PostcodeDataMapper;
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
    private long eastingsMin;
    private long eastingsMax;
    private long northingsMin;
    private long northingsMax;
    private final long margin;

    public PostcodeDataImporter(TramchesterConfig config, StationLocations stationLocations, Unzipper unzipper) {
        this.directory = config.getPostcodeDataPath();
        // meters
        margin = Math.round(config.getNearestStopRangeKM() * 1000D);
        this.config = config;
        this.stationLocations = stationLocations;
        this.unzipper = unzipper;

    }

    public Set<PostcodeData> loadLocalPostcodes() {
        eastingsMin = stationLocations.getEastingsMin() - margin;
        eastingsMax = stationLocations.getEastingsMax() + margin;
        northingsMin = stationLocations.getNorthingsMin() - margin;
        northingsMax = stationLocations.getNorthingsMax() + margin;

        Path postcodeZip = config.getPostcodeZip();
        String unzipTarget = config.getPostcodeZip().toAbsolutePath().toString().replace(".zip","");
        unzipper.unpack(postcodeZip, Path.of(unzipTarget));

        logger.info("Load postcode files files from " + directory.toAbsolutePath());
        if (!Files.isDirectory(directory)) {
            logger.error("Cannot load postcode data, location is not a directory " + directory);
            return Collections.emptySet();
        }

        Set<Path> csvFiles;
        try {
            csvFiles = Files.list(directory).
                    filter(Files::isRegularFile).
                    filter(path -> path.getFileName().toString().toLowerCase().endsWith(CSV)).
                    collect(Collectors.toSet());
        } catch (IOException e) {
            logger.error("Cannot list files in postcode data location " + directory.toAbsolutePath(), e);
            return Collections.emptySet();
        }

        if (csvFiles.isEmpty()) {
            logger.error("Found no matching files in " + directory.toAbsolutePath());
        } else {
            logger.info("Found " + csvFiles.size() + " files in " + directory.toAbsolutePath());
        }

        Set<PostcodeData> loaded = new HashSet<>();
        PostcodeDataMapper mapper = new PostcodeDataMapper();
        csvFiles.forEach(file -> loadDataFromFile(loaded, mapper, file));

        if (loaded.isEmpty()) {
            logger.error("Failed to load any postcode data from files in directory " + directory.toAbsolutePath());
        } else {
            logger.info("Loaded " + loaded.size() + " postcodes");
        }
        csvFiles.clear();

        return loaded;
    }

    private void loadDataFromFile(Set<PostcodeData> target, PostcodeDataMapper mapper, Path file) {
        logger.debug("Load postcode data from " + file.toAbsolutePath());
        int sizeBefore = target.size();

        DataLoader<PostcodeData> loader = new DataLoader<>(file, mapper);
        Stream<PostcodeData> stream = loader.loadFiltered(false);
        filterDataByBoundedBox(stream).filter(this::hasNearbyStation).forEach(target::add);

        int loaded = target.size()-sizeBefore;
        if (loaded>0) {
            logger.info("Loaded " + loaded + " records from " + file.toAbsolutePath());
        }
        stream.close();
    }

    private boolean hasNearbyStation(PostcodeData postcodeData) {
        Double range = config.getNearestStopRangeKM();
        StationLocations.GridPosition gridPosition = new StationLocations.GridPosition(postcodeData.getEastings(),
                postcodeData.getNorthings());
        return !stationLocations.nearestStations(gridPosition,1, range).isEmpty();
    }

    private Stream<PostcodeData> filterDataByBoundedBox(Stream<PostcodeData> stream) {
        return stream.
                filter(postcode -> postcode.getEastings() >= eastingsMin).
                filter(postcode -> postcode.getEastings() <= eastingsMax).
                filter(postcode -> postcode.getNorthings() >= northingsMin).
                filter(postcode -> postcode.getNorthings() <= northingsMax);
    }

}

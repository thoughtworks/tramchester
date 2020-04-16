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
    // Useful geographic tram map at https://tfgm.com/public-transport/tram/geographical/network-map

    private static final Logger logger = LoggerFactory.getLogger(PostcodeDataImporter.class);
    private static final String CSV = ".csv";

    private final Path directory;
    private final StationLocations stationLocations;
    private final long margin; // meters

    public PostcodeDataImporter(TramchesterConfig config, StationLocations stationLocations) {
        this.directory = config.getPostcodeDataPath();
        margin = Math.round(config.getNearestStopRangeKM() * 1000D);
        this.stationLocations = stationLocations;
    }

    public Set<PostcodeData> loadLocalPostcodes() {
        logger.info("Load postcode files files from " + directory.toAbsolutePath());
        if (!Files.isDirectory(directory)) {
            logger.error("Cannot load postcode data, location is not a directory " + directory);
            return Collections.emptySet();
        }

        Set<Path> csvFiles = null;
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
        }
        csvFiles.clear();

        return loaded;
    }

    private void loadDataFromFile(Set<PostcodeData> target, PostcodeDataMapper mapper, Path file) {
        logger.debug("Load postcode data from " + file.toAbsolutePath());

        DataLoader<PostcodeData> loader = new DataLoader<>(file, mapper);
        Stream<PostcodeData> stream = loader.loadAll(false);
        Set<PostcodeData> postcodeData = filterData(stream);

        if (postcodeData.size()>0) {
            logger.info("Loaded " + postcodeData.size() + " records from " + file.toAbsolutePath());
        }
        target.addAll(postcodeData);
        stream.close();
    }

    private Set<PostcodeData> filterData(Stream<PostcodeData> stream) {
        return stream.
                filter(postcode -> postcode.getEastings() >= (stationLocations.getEastingsMin()-margin)).
                filter(postcode -> postcode.getEastings() <= (stationLocations.getEastingsMax()+margin)).
                filter(postcode -> postcode.getNorthings() >= (stationLocations.getNorthingsMin()-margin)).
                filter(postcode -> postcode.getNorthings() <= (stationLocations.getNorthingsMax()+margin)).
                collect(Collectors.toSet());
    }

}

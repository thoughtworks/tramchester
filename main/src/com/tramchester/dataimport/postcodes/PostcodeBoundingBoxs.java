package com.tramchester.dataimport.postcodes;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.DataLoader;
import com.tramchester.dataimport.data.PostcodeHintData;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.GridPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.dataimport.postcodes.PostcodeDataImporter.POSTCODES_CONFIG_NAME;

@LazySingleton
public class PostcodeBoundingBoxs {
    private static final Logger logger = LoggerFactory.getLogger(PostcodeBoundingBoxs.class);

    public final static String HINTS_FILES = "postcode_hints.csv";
    private final Map<String, BoundingBox> postcodeBounds;
    private final boolean enabled;
    private final CsvMapper mapper;
    private boolean playback;

    private Path hintsFilePath;

    @Inject
    public PostcodeBoundingBoxs(TramchesterConfig config, CsvMapper mapper) {
        this.mapper = mapper;
        postcodeBounds = new HashMap<>();
        enabled = config.hasDataSourceConfig(POSTCODES_CONFIG_NAME);
        if (enabled) {
            RemoteDataSourceConfig sourceConfig = config.getDataSourceConfig(POSTCODES_CONFIG_NAME);
            Path directory = sourceConfig.getDataPath();

            hintsFilePath = directory.resolve(HINTS_FILES).toAbsolutePath();
        }
    }

    @PreDestroy
    public void dispose() {
        stop();
        postcodeBounds.clear();
    }

    @PostConstruct
    public void start() {
        if (!enabled) {
            logger.info("Postcode load disabled in config");
            return;
        }

        playback = Files.exists(hintsFilePath);

        if (playback) {
            loadDataFromFile();
        } else {
            logger.info("File "+hintsFilePath+" missing, in record mode");
        }
    }

    public void stop() {
        if (!enabled) {
            logger.info("Postcode load disabled in config");
            return;
        }

        if (playback) {
            logger.info("Not recording");
        } else {
            recordBoundsForPostcodes();
        }
    }

    private void loadDataFromFile() {
        logger.info("File "+hintsFilePath+" existed, in playback mode");

        DataLoader<PostcodeHintData> loader = new DataLoader<>(hintsFilePath, PostcodeHintData.class, mapper);

        Stream<PostcodeHintData> data = loader.load();
        data.forEach(item -> postcodeBounds.put(item.getCode(),
                new BoundingBox(item.getMinEasting(), item.getMinNorthing(), item.getMaxEasting(), item.getMaxNorthing())));
        data.close();
        logger.info("Loaded " + postcodeBounds.size() +" bounding boxes");
    }

    private void recordBoundsForPostcodes() {
        logger.info("Recording bounds for postcode files in " + hintsFilePath.toAbsolutePath());

        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(PostcodeHintData.class).withHeader();
        ObjectWriter myObjectWriter = mapper.writer(schema);

        List<PostcodeHintData> hints = postcodeBounds.entrySet().stream().
                map((entry) -> new PostcodeHintData(entry.getKey(), entry.getValue())).collect(Collectors.toList());

        try(FileOutputStream outputStream = new FileOutputStream(hintsFilePath.toFile())) {
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            OutputStreamWriter writerOutputStream = new OutputStreamWriter(bufferedOutputStream, StandardCharsets.UTF_8);
            myObjectWriter.writeValue(writerOutputStream, hints);
        } catch (IOException fileNotFoundException) {
            logger.error("Exception when saving hints", fileNotFoundException);
        }
    }

    public boolean checkOrRecord(Path sourceFilePath, PostcodeData postcode) {
        if (!postcode.getGridPosition().isValid()) {
            logger.warn("Bad position for " + postcode);
            return false;
        }

        String code = convertPathToCode(sourceFilePath);

        if (playback) {
            if (postcodeBounds.containsKey(code)) {
                return postcodeBounds.get(code).contained(postcode.getGridPosition());
            }
            logger.warn("Missing file when in playback mode: " + sourceFilePath);
        } else {
            if (postcodeBounds.containsKey(code)) {
                BoundingBox boundingBox = postcodeBounds.get(code);
                if (!boundingBox.contained(postcode.getGridPosition())) {
                    updateFor(code, postcode, boundingBox);
                }
            } else {
                // initially just the first one
                GridPosition gridPosition = postcode.getGridPosition();
                postcodeBounds.put(code, new BoundingBox(gridPosition.getEastings(), gridPosition.getNorthings(),
                        gridPosition.getEastings(), gridPosition.getNorthings()));
            }
        }
        return true;
    }

    public String convertPathToCode(Path sourceFilePath) {
        String name = sourceFilePath.getFileName().toString().toLowerCase();
        return name.replace(PostcodeDataImporter.CSV, "");
    }

    private void updateFor(String code, PostcodeData postcode, BoundingBox boundingBox) {
        logger.debug("Upadating bounds for " + code + " from " + postcode.getId());
        GridPosition gridPosition = postcode.getGridPosition();
        long postcodeEastings = gridPosition.getEastings();
        long postcodeNorthings = gridPosition.getNorthings();

        long newMinEasting = Math.min(postcodeEastings, boundingBox.getMinEastings());
        long newMinNorthing = Math.min(postcodeNorthings, boundingBox.getMinNorthings());
        long newMaxEasting = Math.max(postcodeEastings, boundingBox.getMaxEasting());
        long newMaxNorthing = Math.max(postcodeNorthings, boundingBox.getMaxNorthings());

        postcodeBounds.put(code, new BoundingBox(newMinEasting, newMinNorthing, newMaxEasting, newMaxNorthing));
    }

    public BoundingBox getBoundsFor(Path file) {
        return postcodeBounds.get(convertPathToCode(file));
    }

    public boolean isLoaded() {
        return playback;
    }

    public boolean hasBoundsFor(Path file) {
        return postcodeBounds.containsKey(convertPathToCode(file));
    }

    /***
     * Uses bounded boxes and not the actual postcode area, so can produce some unexpected results as bounding boxes
     * cover significantly more area and overlap, which postcodes themselves don't
     * @param location location for find area code for
     * @param marginInKM Margin in Kilometers
     * @return the areas found
     */
    public Set<String> getCodesFor(GridPosition location, int marginInKM) {
        return postcodeBounds.entrySet().stream().
                filter(entry -> entry.getValue().within(marginInKM, location)).
                map(Map.Entry::getKey).
                collect(Collectors.toSet());
    }
}

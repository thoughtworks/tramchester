package com.tramchester.dataimport;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.PostcodeData;
import com.tramchester.dataimport.data.PostcodeHintData;
import com.tramchester.geo.BoundingBox;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class PostcodeBoundingBoxs {
    private static final Logger logger = LoggerFactory.getLogger(PostcodeBoundingBoxs.class);

    public final static String HINTS_FILES = "postcode_hints.csv";
    private final Map<Path, BoundingBox> postcodeBounds;
    private final Path hintsFilePath;
    private final boolean enabled;
    private final CsvMapper mapper;
    private boolean playback;

    @Inject
    public PostcodeBoundingBoxs(TramchesterConfig config, CsvMapper mapper) {
        this.mapper = mapper;
        postcodeBounds = new HashMap<>();
        Path directory = config.getPostcodeDataPath();
        enabled = config.getLoadPostcodes();
        hintsFilePath = directory.resolve(HINTS_FILES).toAbsolutePath();
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

        if (!playback) {
            recordBoundsForPostcodes();
        } else {
            logger.info("Not recording");
        }
    }

    private void loadDataFromFile() {
        logger.info("File "+hintsFilePath+" existed, in playback mode");

        DataLoader<PostcodeHintData> loader = new DataLoader<>(hintsFilePath, PostcodeHintData.class, mapper);

        Stream<PostcodeHintData> data = loader.load();

        data.forEach(item -> postcodeBounds.put(Path.of(item.getFile()),
                new BoundingBox(item.getMinEasting(), item.getMinNorthing(), item.getMaxEasting(), item.getMaxNorthing())));

        data.close();
    }

    private void recordBoundsForPostcodes() {
        logger.info("Recording bounds for postcode files in " + hintsFilePath.toAbsolutePath().toString());

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
        if (postcode.getEastings()==0 || postcode.getNorthings()==0) {
            logger.warn("Bad positional data for " + postcode);
            return false;
        }

        if (playback) {
            if (postcodeBounds.containsKey(sourceFilePath)) {
                return postcodeBounds.get(sourceFilePath).contained(postcode);
            }
            logger.warn("Missing file when in playback mode: " + sourceFilePath);
        } else {
            if (postcodeBounds.containsKey(sourceFilePath)) {
                BoundingBox boundingBox = postcodeBounds.get(sourceFilePath);
                if (!boundingBox.contained(postcode)) {
                    updateFor(sourceFilePath, postcode, boundingBox);
                }
            } else {
                postcodeBounds.put(sourceFilePath, new BoundingBox(postcode.getEastings(), postcode.getNorthings(),
                        postcode.getEastings(), postcode.getNorthings()));
            }
        }
        return true;
    }

    private void updateFor(Path path, PostcodeData postcode, BoundingBox boundingBox) {
        logger.debug("Upadating bounds for " + path + " from " + postcode.getId());
        long postcodeEastings = postcode.getEastings();
        long postcodeNorthings = postcode.getNorthings();

        long newMinEasting = Math.min(postcodeEastings, boundingBox.getMinEastings());
        long newMinNorthing = Math.min(postcodeNorthings, boundingBox.getMinNorthings());
        long newMaxEasting = Math.max(postcodeEastings, boundingBox.getMaxEasting());
        long newMaxNorthing = Math.max(postcodeNorthings, boundingBox.getMaxNorthings());

        postcodeBounds.put(path, new BoundingBox(newMinEasting, newMinNorthing, newMaxEasting, newMaxNorthing));
    }


    public BoundingBox getBoundsFor(Path file) {
        return postcodeBounds.get(file);
    }

    public boolean hasData() {
        return playback;
    }
}

package com.tramchester.dataimport;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.PostcodeData;
import com.tramchester.dataimport.data.PostcodeHintData;
import com.tramchester.dataimport.parsers.PostcodeHintsDataMapper;
import com.tramchester.geo.BoundingBox;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class PostcodeBoundingBoxs implements Startable, Disposable {
    private static final Logger logger = LoggerFactory.getLogger(PostcodeBoundingBoxs.class);

    public final static String HINTS_FILES = "postcode_hints.csv";
    private final Map<Path, BoundingBox> postcodeBounds;
    private final Path hintsFilePath;
    private final boolean enabled;
    private boolean playback;

    public PostcodeBoundingBoxs(TramchesterConfig config) {
        postcodeBounds = new HashMap<>();
        Path directory = config.getPostcodeDataPath();
        enabled = config.getLoadPostcodes();
        hintsFilePath = directory.resolve(HINTS_FILES).toAbsolutePath();
    }

    @Override
    public void dispose() {
        postcodeBounds.clear();
    }

    @Override
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

    @Override
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

    public void loadDataFromFile() {
        logger.info("File "+hintsFilePath+" existed, in playback mode");
        PostcodeHintsDataMapper mapper = new PostcodeHintsDataMapper();

        DataLoader<PostcodeHintData> loader = new DataLoader<>(hintsFilePath, mapper);

        Stream<PostcodeHintData> data = loader.loadFiltered(true);

        data.forEach(item -> postcodeBounds.put(Path.of(item.getFile()),
                new BoundingBox(item.getMinEasting(), item.getMinNorthing(), item.getMaxEasting(), item.getMaxNorthing())));

        data.close();
    }


    private void recordBoundsForPostcodes() {
        String filename = hintsFilePath.toAbsolutePath().toString();

        logger.info("Recording bounds for postcode files in " + filename);
        try (Writer writer = new FileWriter(hintsFilePath.toFile())) {
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
            // Header
            bufferedWriter.write(String.format("%s,%s,%s,%s,%s", PostcodeHintsDataMapper.Columns.file.name(),
                    PostcodeHintsDataMapper.Columns.minEasting.name(), PostcodeHintsDataMapper.Columns.minNorthing.name(),
                    PostcodeHintsDataMapper.Columns.maxEasting.name(), PostcodeHintsDataMapper.Columns.maxNorthing.name()));
            bufferedWriter.newLine();
            // entries
            for (Map.Entry<Path, BoundingBox> entry : postcodeBounds.entrySet()) {
                Path file = entry.getKey();
                BoundingBox box = entry.getValue();
                bufferedWriter.write(String.format("%s,%s,%s,%s,%s", file, box.getMinEastings(), box.getMinNorthings(),
                        box.getMaxEasting(), box.getMaxNorthings()));
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException exception) {
            logger.error("Unable to save to " + filename, exception);
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

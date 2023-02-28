package com.tramchester.dataimport.postcodes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.caching.DataCache;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataexport.CsvDataSaver;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.data.PostcodeHintData;
import com.tramchester.domain.DataSourceID;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.MarginInMeters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class PostcodeBoundingBoxs {
    private static final Logger logger = LoggerFactory.getLogger(PostcodeBoundingBoxs.class);

    public final static String POSTCODE_HINTS_CSV = "postcode_hints.csv";

    private final PostcodeBounds postcodeBounds;

    private final boolean enabled;
    private final DataCache dataCache;
    private boolean cacheAvailable;

    @Inject
    public PostcodeBoundingBoxs(TramchesterConfig config, DataCache dataCache) {
        this.dataCache = dataCache;
        postcodeBounds = new PostcodeBounds();
        enabled = config.hasRemoteDataSourceConfig(DataSourceID.postcode);
    }

    @PreDestroy
    public void dispose() {
        logger.info("stopping");
        stop();
        postcodeBounds.clear();
        logger.info("stopped");
    }

    @PostConstruct
    public void start() {
        if (!enabled) {
            logger.info("Postcode load disabled in config");
            return;
        }

        cacheAvailable = dataCache.has(postcodeBounds);

        if (cacheAvailable) {
            dataCache.loadInto(postcodeBounds, PostcodeHintData.class);
        } else {
            logger.info("No cached data, in record mode");
        }
    }

    public void stop() {
        if (!enabled) {
            logger.info("Postcode load disabled in config");
            return;
        }

        if (!cacheAvailable) {
            logger.info("Caching postcode bounds");
            dataCache.save(postcodeBounds, PostcodeHintData.class);
        }
    }

    public boolean checkOrRecord(Path sourceFilePath, PostcodeData postcode) {
        if (!postcode.getGridPosition().isValid()) {
            logger.warn("Bad position for " + postcode);
            return false;
        }

        String code = convertPathToCode(sourceFilePath);

        if (cacheAvailable) {
            if (postcodeBounds.contains(code)) {
                return postcodeBounds.get(code).contained(postcode.getGridPosition());
            }
            logger.warn("Missing file when in playback mode: " + sourceFilePath);
        } else {
            if (postcodeBounds.contains(code)) {
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
        return cacheAvailable;
    }

    public boolean hasBoundsFor(Path file) {
        return postcodeBounds.contains(convertPathToCode(file));
    }

    /***
     * Uses bounded boxes and not the actual postcode area, so can produce some unexpected results as bounding boxes
     * cover significantly more area and overlap, which postcodes themselves don't
     */
    public Set<String> getCodesFor(GridPosition location, MarginInMeters margin) {
        return postcodeBounds.entrySet().stream().
                filter(entry -> entry.getValue().within(margin, location)).
                map(Map.Entry::getKey).
                collect(Collectors.toSet());
    }

    private static class PostcodeBounds implements DataCache.Cacheable<PostcodeHintData> {
        private final Map<String, BoundingBox> theMap;

        public PostcodeBounds() {
            theMap = new HashMap<>();
        }

        @Override
        public String getFilename() {
            return POSTCODE_HINTS_CSV;
        }

        @Override
        public void cacheTo(DataSaver<PostcodeHintData> saver) {
            saver.open();

            theMap.entrySet().stream().
                    map((entry) -> new PostcodeHintData(entry.getKey(), entry.getValue())).
                    forEach(saver::write);

            saver.close();
        }

        @Override
        public void loadFrom(Stream<PostcodeHintData> data) {
            logger.info("Loading bounds from cache");
            data.forEach(item -> theMap.put(item.getCode(),
                    new BoundingBox(item.getMinEasting(), item.getMinNorthing(), item.getMaxEasting(), item.getMaxNorthing())));
        }

        public void clear() {
            theMap.clear();
        }

        public Set<Map.Entry<String, BoundingBox>> entrySet() {
            return theMap.entrySet();
        }

        public boolean contains(String code) {
            return theMap.containsKey(code);
        }

        public BoundingBox get(String code) {
            return theMap.get(code);
        }

        public void put(String code, BoundingBox box) {
            theMap.put(code, box);
        }
    }
}

package com.tramchester.repository.postcodes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.postcodes.PostcodeBoundingBoxs;
import com.tramchester.dataimport.postcodes.PostcodeData;
import com.tramchester.dataimport.postcodes.PostcodeDataImporter;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.CompositeIdMap;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.id.PostcodeLocationId;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.mappers.Geography;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class PostcodeRepository {
    private static final Logger logger = LoggerFactory.getLogger(PostcodeRepository.class);

    private final PostcodeDataImporter importer;
    private final TramchesterConfig config;
    private final PostcodeBoundingBoxs boundingBoxs;
    private final Geography geography;

    private final Map<String, IdMap<PostcodeLocation>> postcodesByArea; // Area Id -> PostcodeLocations

    @Inject
    public PostcodeRepository(PostcodeDataImporter importer, TramchesterConfig config, PostcodeBoundingBoxs boundingBoxs, Geography geography) {
        this.importer = importer;
        this.config = config;
        this.boundingBoxs = boundingBoxs;
        this.geography = geography;
        postcodesByArea = new HashMap<>();
    }

    public PostcodeLocation getPostcode(PostcodeLocationId postcodeId) {
        Optional<PostcodeLocation> maybeFound = postcodesByArea.values().stream().
                filter(map -> map.hasId(postcodeId)).
                map(matchingMap -> matchingMap.get(postcodeId)).findFirst();
        return maybeFound.orElse(null);
    }

    @PostConstruct
    public void start() {
        logger.info("starting");

        if (!config.hasRemoteDataSourceConfig(DataSourceID.postcode)) {
            logger.warn("Not loading postcodes");
            return;
        }

        List<PostcodeDataImporter.PostcodeDataStream> sources = importer.loadLocalPostcodes();

        logger.info("Processing " + sources.size() + " postcode streams");
        sources.forEach(this::load);
        logger.info("started");
    }


    @PreDestroy
    public void stop() {
        logger.info("stopping");
        postcodesByArea.values().forEach(CompositeIdMap::clear);
        postcodesByArea.clear();
        logger.info("stopped");
    }


    private void load(PostcodeDataImporter.PostcodeDataStream source) {
        if (!source.wasLoaded()) {
            logger.warn("Data was not loaded for " + source.getCode());
        }
        String postcodeArea = source.getCode();
        Stream<PostcodeData> stream = source.getDataStream();

        final IdMap<PostcodeLocation> postcodesFromFile = stream.
                map(this::createPostcodeFor).
                collect(IdMap.collector());
        stream.close();
        this.postcodesByArea.put(postcodeArea, postcodesFromFile);

        logger.info("Added " + postcodesFromFile.size() + " postcodes for " + source.getCode());
    }

    @NotNull
    private PostcodeLocation createPostcodeFor(PostcodeData postcodeData) {
        return new PostcodeLocation(postcodeData.getGridPosition(), PostcodeLocation.createId(postcodeData.getId()));
    }

    public boolean hasPostcode(PostcodeLocationId postcode) {
        return postcodesByArea.values().stream().
                anyMatch(map -> map.hasId(postcode));
    }

    public Collection<PostcodeLocation> getPostcodes() {
        return postcodesByArea.values().stream().
                flatMap(CompositeIdMap::getValuesStream).collect(Collectors.toSet());
    }

    public Stream<PostcodeLocation> getPostcodesNear(GridPosition location, MarginInMeters meters) {
        Set<String> codes = boundingBoxs.getCodesFor(location, meters);
        return postcodesByArea.entrySet().stream().
                filter(entry -> codes.contains(entry.getKey())).
                map(entry -> entry.getValue().getValuesStream()).
                flatMap(postcodeLocations -> geography.getNearToUnsorted(() -> postcodeLocations, location, meters));
    }
}

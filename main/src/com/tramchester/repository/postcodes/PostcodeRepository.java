package com.tramchester.repository.postcodes;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.postcodes.PostcodeBoundingBoxs;
import com.tramchester.dataimport.postcodes.PostcodeData;
import com.tramchester.dataimport.postcodes.PostcodeDataImporter;
import com.tramchester.domain.id.CaseInsensitiveId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.geo.FindNear;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.MarginInMeters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.dataimport.postcodes.PostcodeDataImporter.POSTCODES_CONFIG_NAME;

@LazySingleton
public class PostcodeRepository {
    private static final Logger logger = LoggerFactory.getLogger(PostcodeRepository.class);

    private final PostcodeDataImporter importer;
    private final TramchesterConfig config;
    private final PostcodeBoundingBoxs boundingBoxs;

    private final Map<String, IdMap<PostcodeLocation>> postcodesAreas; // Id -> PostcodeLocation

    @Inject
    public PostcodeRepository(PostcodeDataImporter importer, TramchesterConfig config, PostcodeBoundingBoxs boundingBoxs) {
        this.importer = importer;
        this.config = config;
        this.boundingBoxs = boundingBoxs;
        postcodesAreas = new HashMap<>();
    }

    public PostcodeLocation getPostcode(CaseInsensitiveId<PostcodeLocation> postcodeId) {
        Optional<PostcodeLocation> maybeFound = postcodesAreas.values().stream().
                filter(map -> map.hasId(postcodeId)).
                map(matchingMap -> matchingMap.get(postcodeId)).findFirst();
        return maybeFound.orElse(null);
    }

    @PostConstruct
    public void start() {
        logger.info("starting");

        if (!config.hasDataSourceConfig(POSTCODES_CONFIG_NAME)) {
            logger.warn("Not loading postcodes");
            return;
        }

        List<PostcodeDataImporter.PostcodeDataStream> sources = importer.loadLocalPostcodes();

        logger.info("Processing " + sources.size() + " postcode streams");
        sources.forEach(this::load);
        logger.info("Loaded " + postcodesAreas.size() + " postcodes");
        logger.info("started");
    }


    @PreDestroy
    public void stop() {
        logger.info("stopping");
        postcodesAreas.values().forEach(IdMap::clear);
        postcodesAreas.clear();
        logger.info("stopped");
    }


    private void load(PostcodeDataImporter.PostcodeDataStream source) {
        if (!source.wasLoaded()) {
            logger.warn("Data was not loaded for " + source.getCode());
        }
        String postcodeArea = source.getCode();
        Stream<PostcodeData> stream = source.getDataStream();

        final IdMap<PostcodeLocation> postcodes = stream.
                map(postcodeData -> new PostcodeLocation(postcodeData.getGridPosition(),
                        CaseInsensitiveId.createIdFor(postcodeData.getId()), postcodeArea)).
                collect(IdMap.collector());
        stream.close();
        postcodesAreas.put(postcodeArea, postcodes);

        if (!postcodes.isEmpty()) {
            logger.info("Added " + postcodes.size() + " postcodes for " + source.getCode());
        }
    }


    public boolean hasPostcode(CaseInsensitiveId<PostcodeLocation> postcode) {
        return postcodesAreas.values().stream().
                anyMatch(map -> map.hasId(postcode));
    }

    public Collection<PostcodeLocation> getPostcodes() {
        return postcodesAreas.values().stream().
                flatMap(IdMap::getValuesStream).collect(Collectors.toSet());
    }

    public Stream<PostcodeLocation> getPostcodesNear(GridPosition location, MarginInMeters meters) {
        Set<String> codes = boundingBoxs.getCodesFor(location, meters);
        return postcodesAreas.entrySet().stream().
                filter(entry -> codes.contains(entry.getKey())).
                map(entry -> entry.getValue().getValuesStream()).
                flatMap(postcodeLocations -> FindNear.getNearTo(postcodeLocations, location, meters));
    }
}

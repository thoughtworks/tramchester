package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.PostcodeDataImporter;
import com.tramchester.dataimport.data.PostcodeData;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdMap;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.geo.CoordinateTransforms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@LazySingleton
public class PostcodeRepository {
    private static final Logger logger = LoggerFactory.getLogger(PostcodeRepository.class);

    private final PostcodeDataImporter importer;
    private final TramchesterConfig config;

    private final IdMap<PostcodeLocation> postcodes; // Id -> PostcodeLocation
    private LocalDateTime lastModifiedTime;

    @Inject
    public PostcodeRepository(PostcodeDataImporter importer, TramchesterConfig config) {
        this.importer = importer;
        this.config = config;
        postcodes = new IdMap<>();
    }

    public PostcodeLocation getPostcode(IdFor<PostcodeLocation> postcodeId) {
        return postcodes.get(postcodeId);
    }

    @PostConstruct
    public void start() {
        if (!config.getLoadPostcodes()) {
            logger.warn("Not loading postcodes");
            return;
        }

        List<Stream<PostcodeData>> sources = importer.loadLocalPostcodes();

        sources.forEach(source-> {
            source.forEach(code -> {
                postcodes.add(new PostcodeLocation(CoordinateTransforms.getLatLong(code.getGridPosition()), code.getId()));
            });

            source.close();
        });

        this.lastModifiedTime = importer.getTargetFolderModTime();
    }

    @PreDestroy
    public void dispose() {
        postcodes.clear();
    }

    public boolean hasPostcode(IdFor<PostcodeLocation> postcode) {
        return postcodes.hasId(postcode);
    }

    public Collection<PostcodeLocation> getPostcodes() {
        return Collections.unmodifiableCollection(postcodes.getValues());
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedTime;
    }
}

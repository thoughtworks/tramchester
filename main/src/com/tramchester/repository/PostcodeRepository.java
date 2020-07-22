package com.tramchester.repository;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.PostcodeDataImporter;
import com.tramchester.dataimport.data.PostcodeData;
import com.tramchester.domain.HasId;
import com.tramchester.domain.IdMap;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.geo.CoordinateTransforms;
import org.opengis.referencing.operation.TransformException;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

public class PostcodeRepository implements Disposable, Startable {
    private static final Logger logger = LoggerFactory.getLogger(PostcodeRepository.class);

    private final PostcodeDataImporter importer;
    private final TramchesterConfig config;

    private final IdMap<PostcodeLocation> postcodes; // Id -> PostcodeLocation

    public PostcodeRepository(PostcodeDataImporter importer, TramchesterConfig config) {
        this.importer = importer;
        this.config = config;
        postcodes = new IdMap<>();
    }

    public PostcodeLocation getPostcode(String postcodeId) {
        return postcodes.get(postcodeId);
    }

    @Override
    public void start() {
        if (!config.getLoadPostcodes()) {
            logger.warn("Not loading postcodes");
            return;
        }

        Set<PostcodeData> rawCodes = importer.loadLocalPostcodes();

        rawCodes.forEach(code -> {
            String id = code.getId();
            try {
                postcodes.add(new PostcodeLocation(CoordinateTransforms.getLatLong(code.getEastings(), code.getNorthings()), id));
            } catch (TransformException e) {
                logger.warn("Unable to convert position of postcode to lat/long " + code);
            }
        });
    }

    @Override
    public void dispose() {
        postcodes.clear();
    }

    @Override
    public void stop() {
        // no op
    }

    public boolean hasPostcode(String postcode) {
        return postcodes.hasId(postcode);
    }

    public Collection<PostcodeLocation> getPostcodes() {
        return Collections.unmodifiableCollection(postcodes.getValues());
    }

    public int getNumberOf() {
        return postcodes.size();
    }
}

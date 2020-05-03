package com.tramchester.repository;

import com.tramchester.dataimport.PostcodeDataImporter;
import com.tramchester.dataimport.data.PostcodeData;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.geo.CoordinateTransforms;
import org.opengis.referencing.operation.TransformException;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Set;

public class PostcodeRepository implements Disposable, Startable {
    private static final Logger logger = LoggerFactory.getLogger(PostcodeRepository.class);

    private final PostcodeDataImporter importer;
    private final CoordinateTransforms coordinateTransforms;

    private final HashMap<String, PostcodeLocation> postcodes; // Id -> PostcodeLocation

    public PostcodeRepository(PostcodeDataImporter importer, CoordinateTransforms coordinateTransforms) {
        this.importer = importer;
        this.coordinateTransforms = coordinateTransforms;
        postcodes = new HashMap<>();
    }

    @Override
    public void dispose() {
        postcodes.clear();
    }

    public PostcodeLocation getPostcode(String postcodeId) {
        return postcodes.get(postcodeId);
    }

    @Override
    public void start() {
        Set<PostcodeData> rawCodes = importer.loadLocalPostcodes();

        rawCodes.forEach(code -> {
            String id = code.getId();
            try {
                postcodes.put(id, new PostcodeLocation(coordinateTransforms.getLatLong(code.getEastings(), code.getNorthings()), id));
            } catch (TransformException e) {
                logger.warn("Unable to convert position of postcode to lat/long " + code);
            }
        });
    }

    @Override
    public void stop() {

    }

    public boolean hasPostcode(String postcode) {
        return postcodes.containsKey(postcode);
    }
}

package com.tramchester.geo;

import com.tramchester.domain.presentation.LatLong;
import org.geotools.geometry.GeneralDirectPosition;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory;
import org.jetbrains.annotations.NotNull;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoordinateTransforms {
    private static final Logger logger = LoggerFactory.getLogger(CoordinateTransforms.class);

    private CoordinateOperation gridToLatLong;
    private CoordinateOperation latLongToGrid;

    public CoordinateTransforms() {
        CRSAuthorityFactory authorityFactory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", null);

        try {
            CoordinateReferenceSystem nationalGridRefSys = authorityFactory.createCoordinateReferenceSystem("27700");
            CoordinateReferenceSystem latLongRef = authorityFactory.createCoordinateReferenceSystem("4326");
            latLongToGrid = new DefaultCoordinateOperationFactory().createOperation(latLongRef, nationalGridRefSys);
            gridToLatLong = new DefaultCoordinateOperationFactory().createOperation(nationalGridRefSys, latLongRef);
        } catch (FactoryException e) {
            logger.error("Unable to init geotools factory or transform", e);
        }
    }

    @NotNull
    public StationLocations.GridPosition getGridPosition(LatLong position) throws TransformException {
        DirectPosition directPositionLatLong = new GeneralDirectPosition(position.getLat(), position.getLon());

        DirectPosition directPositionGrid = latLongToGrid.getMathTransform().transform(directPositionLatLong, null);
        long easting = Math.round(directPositionGrid.getOrdinate(0));
        long northing = Math.round(directPositionGrid.getOrdinate(1));
        return new StationLocations.GridPosition(easting, northing);
    }

    public LatLong getLatLong(long eastings, long northings) throws TransformException {
        DirectPosition directPositionGrid = new GeneralDirectPosition(eastings, northings);

        DirectPosition directPositionLatLong = gridToLatLong.getMathTransform().transform(directPositionGrid, null);
        double lat = directPositionLatLong.getOrdinate(0);
        double lon = directPositionLatLong.getOrdinate(1);
        return new LatLong(lat, lon);
    }
}

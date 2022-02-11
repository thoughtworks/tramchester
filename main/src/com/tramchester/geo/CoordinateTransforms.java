package com.tramchester.geo;

import com.tramchester.domain.presentation.LatLong;
import com.tramchester.mappers.Geography;
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

import static com.tramchester.mappers.Geography.AUTHORITY;

public class CoordinateTransforms {
    private static final Logger logger;

    private static final CoordinateOperation gridToLatLong;
    private static final CoordinateOperation latLongToGrid;

    static {
        logger = LoggerFactory.getLogger(CoordinateTransforms.class);

        String latLongCode = Geography.getLatLongCode();

        try {
            CRSAuthorityFactory authorityFactory = ReferencingFactoryFinder.getCRSAuthorityFactory(AUTHORITY, null);

            CoordinateReferenceSystem nationalGridRefSys = authorityFactory.createCoordinateReferenceSystem("27700");
            CoordinateReferenceSystem latLongRef = authorityFactory.createCoordinateReferenceSystem(latLongCode);

            latLongToGrid = new DefaultCoordinateOperationFactory().createOperation(latLongRef, nationalGridRefSys);
            gridToLatLong = new DefaultCoordinateOperationFactory().createOperation(nationalGridRefSys, latLongRef);

        } catch (FactoryException e) {
            String msg = "Unable to init geotools factory or transforms";
            logger.error(msg, e);
            throw new RuntimeException(msg);
        }
    }

    private CoordinateTransforms() {

    }

    @NotNull
    public static GridPosition getGridPosition(LatLong position) {
        if (!position.isValid()) {
            logger.warn("Position invalid " + position);
            return GridPosition.Invalid;
        }

        try {
            // note the lat(y) lon(x) ordering here
            DirectPosition directPositionLatLong = new GeneralDirectPosition(position.getLat(), position.getLon());
            DirectPosition directPositionGrid = latLongToGrid.getMathTransform().transform(directPositionLatLong, null);

            long easting = Math.round(directPositionGrid.getOrdinate(0));
            long northing = Math.round(directPositionGrid.getOrdinate(1));

            return new GridPosition(easting, northing);
        }
        catch (TransformException transformException) {
            logger.warn("Could not transform " + position, transformException);
            return GridPosition.Invalid;
        }
    }

    public static LatLong getLatLong(GridPosition gridPosition) {
        if (!gridPosition.isValid()) {
            logger.warn("Position invalid " + gridPosition);
            return LatLong.Invalid;
        }

        try {
            DirectPosition directPositionGrid = new GeneralDirectPosition(gridPosition.getEastings(), gridPosition.getNorthings());
            DirectPosition directPositionLatLong = gridToLatLong.getMathTransform().transform(directPositionGrid, null);
            double lat = directPositionLatLong.getOrdinate(0);
            double lon = directPositionLatLong.getOrdinate(1);
            return new LatLong(lat, lon);
        }
        catch (TransformException transformException) {
            logger.warn("Could not transform " + gridPosition, transformException);
            return LatLong.Invalid;
        }
    }

}

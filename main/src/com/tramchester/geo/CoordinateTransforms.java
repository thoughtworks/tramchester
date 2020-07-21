package com.tramchester.geo;

import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.Station;
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
    private static final Logger logger;

    private static final double EARTH_RADIUS = 3958.75;
    private static final CoordinateOperation gridToLatLong;
    private static final CoordinateOperation latLongToGrid;

    static {
        logger = LoggerFactory.getLogger(CoordinateTransforms.class);
        CRSAuthorityFactory authorityFactory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", null);

        try {
            CoordinateReferenceSystem nationalGridRefSys = authorityFactory.createCoordinateReferenceSystem("27700");
            CoordinateReferenceSystem latLongRef = authorityFactory.createCoordinateReferenceSystem("4326");
            latLongToGrid = new DefaultCoordinateOperationFactory().createOperation(latLongRef, nationalGridRefSys);
            gridToLatLong = new DefaultCoordinateOperationFactory().createOperation(nationalGridRefSys, latLongRef);
        } catch (FactoryException e) {
            String msg = "Unable to init geotools factory or transform";
            logger.error(msg, e);
            throw new RuntimeException(msg);
        }
    }

    private CoordinateTransforms() {

    }

    @NotNull
    public static HasGridPosition getGridPosition(LatLong position) throws TransformException {
        DirectPosition directPositionLatLong = new GeneralDirectPosition(position.getLat(), position.getLon());
        DirectPosition directPositionGrid = latLongToGrid.getMathTransform().transform(directPositionLatLong, null);

        long easting = Math.round(directPositionGrid.getOrdinate(0));
        long northing = Math.round(directPositionGrid.getOrdinate(1));

        return new GridPosition(easting, northing);
    }

    public static LatLong getLatLong(long eastings, long northings) throws TransformException {
        DirectPosition directPositionGrid = new GeneralDirectPosition(eastings, northings);

        DirectPosition directPositionLatLong = gridToLatLong.getMathTransform().transform(directPositionGrid, null);
        double lat = directPositionLatLong.getOrdinate(0);
        double lon = directPositionLatLong.getOrdinate(1);
        return new LatLong(lat, lon);
    }

    private static double distanceInMiles(LatLong point1, LatLong point2) {
        double lat1 = point1.getLat();
        double lat2 = point2.getLat();
        double diffLat = Math.toRadians(lat2-lat1);
        double diffLong = Math.toRadians(point2.getLon()-point1.getLon());
        double sineDiffLat = Math.sin(diffLat / 2D);
        double sineDiffLong = Math.sin(diffLong / 2D);

        double a = Math.pow(sineDiffLat, 2) + Math.pow(sineDiffLong, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));

        double fractionOfRadius = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return EARTH_RADIUS * fractionOfRadius;
    }

    // crude but good enough for distance ranking during searches
    public static double distanceFlat(LatLong point1, LatLong point2) {
        double deltaLat = Math.abs(point1.getLat()-point2.getLat());
        double deltaLon = Math.abs(point1.getLon()-point2.getLon());

        return Math.sqrt((deltaLat*deltaLat)+(deltaLon*deltaLon));

    }

    public static int calcCostInMinutes(Station stationA, Station stationB, double mph) {
        return calcCostInMinutes(stationA.getLatLong(), stationB, mph);
    }

    // TODO Use Grid Position instead of LatLong??
    public static int calcCostInMinutes(LatLong latLong, Location station, double mph) {

        double distanceInMiles = distanceInMiles(latLong, station.getLatLong());
        double hours = distanceInMiles / mph;
        return (int)Math.ceil(hours * 60D);
    }

}

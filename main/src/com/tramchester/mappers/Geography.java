package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.LatLong;
import org.geotools.metadata.iso.citation.CitationImpl;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.*;
import tec.units.ri.quantity.Quantities;
import tec.units.ri.unit.Units;

import javax.inject.Inject;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Time;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.SECONDS;
import static tec.units.ri.unit.Units.METRE_PER_SECOND;
import static tec.units.ri.unit.Units.SECOND;

@LazySingleton
public class Geography {
    private final static double KILO_PER_MILE = 1.609344D;

    public static final String AUTHORITY = "EPSG";

    private static final String latLongCode = DefaultGeographicCRS.WGS84.getIdentifier(new CitationImpl(AUTHORITY)).getCode();

    private final GeometryFactory geometryFactoryLatLong;
    private final Quantity<Speed> walkingSpeed;

    @Inject
    public Geography(TramchesterConfig config) {
        final double metersPerSecond = (config.getWalkingMPH() * KILO_PER_MILE * 1000) / 3600D;
        walkingSpeed = Quantities.getQuantity(metersPerSecond, METRE_PER_SECOND);

        int srid = Integer.parseInt(latLongCode);
        geometryFactoryLatLong = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), srid);
    }

    public Quantity<Time> getWalkingTime(Quantity<Length> distance) {
        return distance.divide(walkingSpeed).asType(Time.class);
    }

    private Duration getWalkingDuration(Quantity<Length> distance) {
        Double seconds = getWalkingTime(distance).to(SECOND).getValue().doubleValue();
        return Duration.of(seconds.longValue(), SECONDS);
    }

    public Duration getWalkingDuration(Location<?> locationA, Location<?> locationB) {
        Quantity<Length> distance = getDistanceBetweenInMeters(locationA, locationB);
        return getWalkingDuration(distance);
    }

    public Quantity<Length> getDistanceBetweenInMeters(Location<?> placeA, Location<?> placeB) {
        Point pointA = geometryFactoryLatLong.createPoint(placeA.getLatLong().getCoordinate());
        Point pointB = geometryFactoryLatLong.createPoint(placeB.getLatLong().getCoordinate());

        GeodeticCalculator geodeticCalculator = new GeodeticCalculator(DefaultGeographicCRS.WGS84);

        geodeticCalculator.setStartingGeographicPoint(pointA.getX(), pointA.getY());
        geodeticCalculator.setDestinationGeographicPoint(pointB.getX(), pointB.getY());

        return Quantities.getQuantity(geodeticCalculator.getOrthodromicDistance(), Units.METRE);
    }

    public Geometry createBoundaryFor(List<LatLong> locations) {
        List<Coordinate> points = locations.stream().
                map(latLong -> new Coordinate(latLong.getLat(), latLong.getLon())).
                collect(Collectors.toList());

        Coordinate[] asArray = points.toArray(new Coordinate[]{});

        MultiPoint multiPoint = geometryFactoryLatLong.createMultiPointFromCoords(asArray);

        return multiPoint.convexHull();
    }
    
    public static String getLatLongCode() {
        return latLongCode;
    }

}

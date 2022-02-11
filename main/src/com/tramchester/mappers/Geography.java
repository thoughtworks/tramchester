package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;
import com.tramchester.geo.GridPositions;
import com.tramchester.geo.MarginInMeters;
import org.apache.commons.lang3.stream.Streams;
import org.geotools.metadata.iso.citation.CitationImpl;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tec.units.ri.quantity.Quantities;
import tec.units.ri.unit.Units;

import javax.inject.Inject;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import javax.measure.quantity.Speed;
import javax.measure.quantity.Time;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.SECONDS;
import static tec.units.ri.unit.Units.METRE_PER_SECOND;
import static tec.units.ri.unit.Units.SECOND;

@LazySingleton
public class Geography {
    private static final Logger logger = LoggerFactory.getLogger(Geography.class);

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

    public List<LatLong> createBoundaryFor(Stream<LatLong> locations) {
        Coordinate[] coords = locations.
                map(latLong -> new Coordinate(latLong.getLat(), latLong.getLon())).
                collect(Streams.toArray(Coordinate.class));

        MultiPoint multiPoint = geometryFactoryLatLong.createMultiPointFromCoords(coords);
        
        Geometry boundary = multiPoint.convexHull().getBoundary();

        if (boundary.getNumPoints()==0) {
            logger.warn("Created a boundary with zero points");
        }

        return Arrays.stream(boundary.getCoordinates()).map(LatLong::of).collect(Collectors.toList());
    }
    
    public static String getLatLongCode() {
        return latLongCode;
    }

    private <T extends Location<T>> Stream<T> getNearbyCrude(LocationsSource<T> locationsSource,
                                                             GridPosition otherPosition, MarginInMeters rangeInMeters) {

        return locationsSource.get().
                filter(entry -> entry.getGridPosition().isValid()).
                filter(entry -> GridPositions.withinDistEasting(otherPosition, entry.getGridPosition(), rangeInMeters)).
                filter(entry -> GridPositions.withinDistNorthing(otherPosition, entry.getGridPosition(), rangeInMeters));
    }

    public <T extends Location<T>> Stream<T> getNearToUnsorted(LocationsSource<T> locationsSource, GridPosition otherPosition,
                                                               MarginInMeters rangeInMeters) {
        return getNearbyCrude(locationsSource, otherPosition, rangeInMeters).
                filter(entry -> GridPositions.withinDist(otherPosition, entry.getGridPosition(), rangeInMeters));
    }

    public <T extends Location<T>> Stream<T> getNearToSorted(LocationsSource<T> locationsSource,
                                                             GridPosition gridPosition, MarginInMeters rangeInMeters) {
        return getNearToUnsorted(locationsSource, gridPosition, rangeInMeters).
                sorted((a, b) -> chooseNearestToGrid(gridPosition, a.getGridPosition(), b.getGridPosition()));
    }

    public int chooseNearestToGrid(GridPosition grid, GridPosition first, GridPosition second) {
        long firstDist = GridPositions.distanceTo(grid, first);
        long secondDist = GridPositions.distanceTo(grid, second);
        return Long.compare(firstDist, secondDist);
    }

    public interface LocationsSource<T extends Location<T>> {
        Stream<T> get();
    }

}

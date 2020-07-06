package com.tramchester.geo;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import org.jetbrains.annotations.NotNull;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class StationLocations implements StationLocationsRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationLocations.class);
    private final CoordinateTransforms coordinateTransforms;

    private long minEastings;
    private long maxEasting;
    private long minNorthings;
    private long maxNorthings;

    private final HashMap<Station, HasGridPosition> positions;

    public StationLocations(CoordinateTransforms coordinateTransforms) {
        this.coordinateTransforms = coordinateTransforms;
        positions = new HashMap<>();

        // bounding box for all stations
        minEastings = Long.MAX_VALUE;
        maxEasting = Long.MIN_VALUE;
        minNorthings = Long.MAX_VALUE;
        maxNorthings = Long.MIN_VALUE;
    }

    public void addStation(Station station) {
        LatLong position = station.getLatLong();
        try {
            GridPosition gridPosition = coordinateTransforms.getGridPosition(position);
            positions.put(station, gridPosition);
            updateBoundingBox(gridPosition);
            logger.debug("Added station " + station.getId() + " at grid " + gridPosition);
        } catch (TransformException e) {
            logger.error("Unable to store station as cannot convert location", e);
        }
    }

    @Override
    public LatLong getStationPosition(Station station) throws TransformException {
        HasGridPosition gridPostition = positions.get(station);
        return coordinateTransforms.getLatLong(gridPostition.getEastings(), gridPostition.getNorthings());
    }

    private void updateBoundingBox(GridPosition gridPosition) {
        long eastings = gridPosition.getEastings();
        long northings = gridPosition.getNorthings();

        if (eastings<minEastings) {
            minEastings = eastings;
        }
        if (eastings>maxEasting) {
            maxEasting = eastings;
        }
        if (northings<minNorthings) {
            minNorthings = northings;
        }
        if (northings>maxNorthings) {
            maxNorthings = northings;
        }
    }

    @Override
    public HasGridPosition getStationGridPosition(Station station) {
        return positions.get(station);
    }

    @Override
    public List<Station> nearestStationsSorted(LatLong latLong, int maxToFind, double rangeInKM) {
        try {
            @NotNull GridPosition gridPosition = coordinateTransforms.getGridPosition(latLong);
            return nearestStationsSorted(gridPosition, maxToFind, rangeInKM);
        } catch (TransformException e) {
            logger.error("Unable to convert latlong to grid position", e);
            return Collections.emptyList();
        }
    }

    @Override
    @NotNull
    public List<Station> nearestStationsSorted(@NotNull HasGridPosition gridPosition, int maxToFind, double rangeInKM) {
        long rangeInMeters = Math.round(rangeInKM * 1000D);

        Stream<Map.Entry<Station, HasGridPosition>> unsorted = getNearbyStream(gridPosition, rangeInMeters);

        @NotNull List<Station> stationList;
        if (maxToFind>1) {
            // only sort if more than one, as sorting expensive
            stationList = unsorted.sorted((a, b) -> compareDistances(gridPosition, a.getValue(), b.getValue())).
                    limit(maxToFind).
                    map(Map.Entry::getKey).collect(Collectors.toList());
            logger.debug(format("Found %s stations within %s meters of grid %s", stationList.size(), rangeInMeters, gridPosition));
        } else {
            stationList = unsorted.limit(maxToFind).map(Map.Entry::getKey).
                    collect(Collectors.toList());
        }

        return stationList;
    }

    @Override
    public List<Station> nearestStationsUnsorted(Station station, double rangeInKM) {
        long rangeInMeters = Math.round(rangeInKM * 1000D);
        return getNearbyStream(getStationGridPosition(station), rangeInMeters).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    public List<Station> getNearestStationsTo(LatLong latLong, int numberOfNearest, double rangeInKM) {
        List<Station> result = nearestStationsSorted(latLong, numberOfNearest, rangeInKM);
        logger.info(format("Found %s stations close to %s", result.size(), latLong));
        return result;
    }

    @NotNull
    private Stream<Map.Entry<Station, HasGridPosition>> getNearbyStream(@NotNull HasGridPosition otherPosition, long rangeInMeters) {
        return positions.entrySet().stream().
                // crude filter initially
                        filter(entry -> GridPosition.withinDistEasting(otherPosition, entry.getValue(), rangeInMeters)).
                        filter(entry -> GridPosition.withinDistNorthing(otherPosition, entry.getValue(), rangeInMeters)).
                // now filter on actual distance
                        filter(entry -> GridPosition.withinDist(otherPosition, entry.getValue(), rangeInMeters));
    }

    private int compareDistances(HasGridPosition origin, HasGridPosition first, HasGridPosition second) {
        long firstDist = GridPosition.distanceTo(origin, first);
        long secondDist = GridPosition.distanceTo(origin, second);
        return Long.compare(firstDist, secondDist);
    }

    public BoundingBox getBounds() {
        return new BoundingBox(minEastings, minNorthings, maxEasting, maxNorthings);
    }

}

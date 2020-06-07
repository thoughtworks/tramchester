package com.tramchester.geo;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import org.jetbrains.annotations.NotNull;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class StationLocations {
    private static final Logger logger = LoggerFactory.getLogger(StationLocations.class);
    private final CoordinateTransforms coordinateTransforms;

    private long minEastings;
    private long maxEasting;
    private long minNorthings;
    private long maxNorthings;

    private final HashMap<Station, GridPosition> positions;

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

    public LatLong getStationPosition(Station station) throws TransformException {
        GridPosition gridPostition = positions.get(station);
        return coordinateTransforms.getLatLong(gridPostition.eastings, gridPostition.northings);
    }

    private void updateBoundingBox(GridPosition gridPosition) {
        if (gridPosition.eastings<minEastings) {
            minEastings = gridPosition.eastings;
        }
        if (gridPosition.eastings>maxEasting) {
            maxEasting = gridPosition.eastings;
        }
        if (gridPosition.northings<minNorthings) {
            minNorthings = gridPosition.northings;
        }
        if (gridPosition.northings>maxNorthings) {
            maxNorthings = gridPosition.northings;
        }
    }

    public GridPosition getStationGridPosition(Station station) {
        return positions.get(station);
    }

    public List<Station> nearestStations(LatLong latLong, int maxToFind, double rangeInKM) {
        try {
            @NotNull StationLocations.GridPosition gridPosition = coordinateTransforms.getGridPosition(latLong);
            return nearestStations(gridPosition, maxToFind, rangeInKM);
        } catch (TransformException e) {
            logger.error("Unable to convert latlong to grid position", e);
            return Collections.emptyList();
        }
    }

    @NotNull
    public List<Station> nearestStations(@NotNull GridPosition gridPosition, int maxToFind, double rangeInKM) {
        long rangeInMeters = Math.round(rangeInKM * 1000D);

        Stream<Map.Entry<Station, GridPosition>> unsorted = positions.entrySet().stream().
                // crude filter initially
                filter(entry -> entry.getValue().withinDistEasting(gridPosition, rangeInMeters)).
                filter(entry -> entry.getValue().withinDistNorthing(gridPosition, rangeInMeters)).
                // now filter on actual distance
                filter(entry -> entry.getValue().withinDist(gridPosition, rangeInMeters));

        @NotNull List<Station> stationList;
        if (maxToFind>1) {
            // only sort if more than one, as sorting expensive
            stationList = unsorted.sorted((a, b) -> compareDistances(gridPosition, a.getValue(), b.getValue())).
                    limit(maxToFind).
                    map(Map.Entry::getKey).collect(Collectors.toList());
            logger.info(format("Found %s stations within %s meters of grid %s", stationList.size(), rangeInMeters, gridPosition));
        } else {
            stationList = unsorted.limit(maxToFind).map(Map.Entry::getKey).
                    collect(Collectors.toList());
        }

        return stationList;
    }

    private int compareDistances(GridPosition origin, GridPosition first, GridPosition second) {
        long firstDist = origin.distanceTo(first);
        long secondDist = origin.distanceTo(second);
        return Long.compare(firstDist, secondDist);
    }

    public long getEastingsMax() {
        return maxEasting;
    }

    public long getEastingsMin() {
        return minEastings;
    }

    public long getNorthingsMax() {
        return maxNorthings;
    }

    public long getNorthingsMin() {
        return minNorthings;
    }


    public static class GridPosition  {

        private final long eastings;
        private final long northings;

        public GridPosition(long eastings, long northings) {

            this.eastings = eastings;
            this.northings = northings;
        }

        public long getEastings() {
            return eastings;
        }

        public long getNorthings() {
            return northings;
        }

        public boolean withinDistEasting(GridPosition gridPosition, long rangeInMeters) {
            return rangeInMeters >= getDistEasting(gridPosition) ;
        }

        public boolean withinDistNorthing(GridPosition gridPosition, long rangeInMeters) {
            return rangeInMeters >= getDistNorthing(gridPosition);
        }

        private long getDistNorthing(GridPosition gridPosition) {
            return Math.abs(gridPosition.northings - this.northings);
        }

        private long getDistEasting(GridPosition gridPosition) {
            return Math.abs(gridPosition.eastings - this.eastings);
        }

        private long getSumSquaresDistance(GridPosition gridPosition) {
            long distHorz = getDistEasting(gridPosition);
            long distVert = getDistNorthing(gridPosition);
            return (distHorz * distHorz) + (distVert * distVert);
        }

        public boolean withinDist(GridPosition gridPosition, long rangeInMeters) {
            long hypSquared = rangeInMeters*rangeInMeters;
            long sum = getSumSquaresDistance(gridPosition);
            return sum<=hypSquared;
        }

        public long distanceTo(GridPosition gridPosition) {
            long sum = getSumSquaresDistance(gridPosition);
            return Math.round(Math.sqrt(sum));
        }

        @Override
        public String toString() {
            return "GridPosition{" +
                    "easting=" + eastings +
                    ", northing=" + northings +
                    '}';
        }
    }
}

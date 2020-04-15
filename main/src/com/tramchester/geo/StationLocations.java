package com.tramchester.geo;

import com.tramchester.domain.Station;
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

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class StationLocations {
    private static final Logger logger = LoggerFactory.getLogger(StationLocations.class);
    private CoordinateOperation latLongToGrid;

    private final HashMap<Station, GridPosition> positions;

    public StationLocations() {
        CRSAuthorityFactory authorityFactory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", null);

        positions = new HashMap<>();
        try {
            CoordinateReferenceSystem nationalGridRefSys = authorityFactory.createCoordinateReferenceSystem("27700");
            CoordinateReferenceSystem latLongRef = authorityFactory.createCoordinateReferenceSystem("4326");
            latLongToGrid = new DefaultCoordinateOperationFactory().createOperation(latLongRef, nationalGridRefSys);
            //gridToLatLong = new DefaultCoordinateOperationFactory().createOperation(nationalGridRefSys, latLongRef);
        } catch (FactoryException e) {
            logger.error("Unable to init geotools factory or transform", e);
        }
    }

    public void addStation(Station station) {
        LatLong position = station.getLatLong();
        try {
            GridPosition gridPosition = getGridPosition(position);
            positions.put(station, gridPosition);
            logger.info("Added station " + station.getId() + " at grid " + gridPosition);
        } catch (TransformException e) {
            logger.error("Unable to store station as cannot convert location", e);
        }
    }

    public GridPosition getStationGridPosition(Station station) {
        return positions.get(station);
    }

    public List<Station> nearestStations(LatLong latLong, int maxToFind, double rangeInKM) {
        long rangeInMeters = Math.round(rangeInKM * 1000D);

        try {
            @NotNull StationLocations.GridPosition gridPosition = getGridPosition(latLong);
            List<Station> stationList = positions.entrySet().stream().
                    filter(entry -> entry.getValue().withinDistEasting(gridPosition, rangeInMeters)).
                    filter(entry -> entry.getValue().withinDistNorthing(gridPosition, rangeInMeters)).
                    filter(entry -> entry.getValue().withinDist(gridPosition, rangeInMeters)).
                    sorted((a, b) -> compareDistances(gridPosition, a.getValue(), b.getValue())).
                    limit(maxToFind).
                    map(Map.Entry::getKey).collect(Collectors.toList());
            logger.info(format("Found %s stations within %s meters of grid %s", stationList.size(), rangeInMeters, gridPosition));
            return stationList;
        } catch (TransformException e) {
            logger.error("Unable to convert latlong to grid position", e);
            return Collections.emptyList();
        }
    }

    private int compareDistances(GridPosition origin, GridPosition first, GridPosition second) {
        long firstDist = origin.distanceTo(first);
        long secondDist = origin.distanceTo(second);
        return Long.compare(firstDist, secondDist);
    }

    @NotNull
    private StationLocations.GridPosition getGridPosition(LatLong position) throws TransformException {
        DirectPosition directPositionLatLong = new GeneralDirectPosition(position.getLat(), position.getLon());

        DirectPosition directPositionGrid = latLongToGrid.getMathTransform().transform(directPositionLatLong, null);
        long easting = Math.round(directPositionGrid.getOrdinate(0));
        long northing = Math.round(directPositionGrid.getOrdinate(1));
        return new GridPosition(easting, northing);
    }

    public static class GridPosition  {

        private final long easting;
        private final long northing;

        public GridPosition(long easting, long northing) {

            this.easting = easting;
            this.northing = northing;
        }

        public long getEasting() {
            return easting;
        }

        public long getNorthing() {
            return northing;
        }

        public boolean withinDistEasting(GridPosition gridPosition, long rangeInMeters) {
            return rangeInMeters >= getDistEasting(gridPosition) ;
        }

        public boolean withinDistNorthing(GridPosition gridPosition, long rangeInMeters) {
            return rangeInMeters >= getDistNorthing(gridPosition);
        }

        private long getDistNorthing(GridPosition gridPosition) {
            return Math.abs(gridPosition.northing - this.northing);
        }

        private long getDistEasting(GridPosition gridPosition) {
            return Math.abs(gridPosition.easting - this.easting);
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
                    "easting=" + easting +
                    ", northing=" + northing +
                    '}';
        }
    }
}

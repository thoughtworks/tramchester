package com.tramchester.geo;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.CompositeStationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.lang.String.format;

/***
 * Note: will return composites instead of stations if composite is within range
 */
@LazySingleton
public class StationLocations implements StationLocationsRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationLocations.class);

    private final CompositeStationRepository stationRepository;
    private final TramchesterConfig config;

    private long minEastings;
    private long maxEasting;
    private long minNorthings;
    private long maxNorthings;

    private final Set<Station> positions;

    @Inject
    public StationLocations(CompositeStationRepository stationRepository, TramchesterConfig config) {
        this.stationRepository = stationRepository;
        this.config = config;
        positions = new HashSet<>();

        // bounding box for all stations
        minEastings = Long.MAX_VALUE;
        maxEasting = Long.MIN_VALUE;
        minNorthings = Long.MAX_VALUE;
        maxNorthings = Long.MIN_VALUE;
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        for (TransportMode transportMode : config.getTransportModes()) {
            logger.info("Adding stations for " + transportMode);
            stationRepository.getStationsForMode(transportMode).forEach(this::addStation);
        }
        logger.info("started");
    }

    @PreDestroy
    public void dispose() {
        logger.info("Clear positions");
        positions.clear();
    }

    private void addStation(Station station) {
        logger.debug("Adding station " + HasId.asId(station));
        if (!positions.contains(station)) {
            GridPosition position = station.getGridPosition();
            if (position.isValid()) {
                positions.add(station);
                updateBoundingBox(position);
                logger.debug("Added station " + station.getId() + " at grid " + position);
            } else {
                logger.warn("Invalid grid for " + station.getId());
            }
        }
    }

    private void updateBoundingBox(GridPosition gridPosition) {
        long eastings = gridPosition.getEastings();
        long northings = gridPosition.getNorthings();

        if (eastings < minEastings) {
            minEastings = eastings;
        }
        if (eastings > maxEasting) {
            maxEasting = eastings;
        }
        if (northings < minNorthings) {
            minNorthings = northings;
        }
        if (northings > maxNorthings) {
            maxNorthings = northings;
        }
    }

    @Override
    public List<Station> nearestStationsSorted(GridPosition gridPosition, int maxToFind, double rangeInKM) {
        long rangeInMeters = Math.round(rangeInKM * 1000D);

        if (maxToFind > 1) {
            // only sort if more than one, as sorting potentially expensive
           return FindNear.getNearToSorted(positions, gridPosition, rangeInMeters).
                    limit(maxToFind).
                    collect(Collectors.toList());
        } else {
            return FindNear.getNearTo(positions, gridPosition, rangeInMeters).
                    limit(maxToFind).collect(Collectors.toList());
        }
    }

    @Override
    public Stream<Station> nearestStationsUnsorted(Station station, double rangeInKM) {
        long rangeInMeters = Math.round(rangeInKM * 1000D);
        return FindNear.getNearTo(positions, station.getGridPosition(), rangeInMeters);
    }

    @Override
    public List<Station> nearestStationsSorted(LatLong latLong, int maxToFind, double rangeInKM) {
        GridPosition gridPosition = CoordinateTransforms.getGridPosition(latLong);
        return nearestStationsSorted(gridPosition, maxToFind, rangeInKM);
    }

    public List<Station> getNearestStationsTo(LatLong latLong, int maxNumberToFind, double rangeInKM) {
        List<Station> result = nearestStationsSorted(latLong, maxNumberToFind, rangeInKM);
        logger.info(format("Found %s (of max %s) stations close to %s", result.size(), maxNumberToFind, latLong));
        return result;
    }

    public BoundingBox getBounds() {
        return new BoundingBox(minEastings, minNorthings, maxEasting, maxNorthings);
    }

    public boolean hasAnyNearby(GridPosition hasGridPosition, double rangeInKM) {
        long rangeInMeters = Math.round(rangeInKM * 1000D);
        return  FindNear.getNearTo(positions, hasGridPosition, rangeInMeters).findAny().isPresent();
    }

    public Stream<BoundingBoxWithStations> getGroupedStations(long gridSize) {
        if (gridSize <= 0) {
            throw new RuntimeException("Invalid grid size of " + gridSize);
        }

        logger.info("Getting groupded stations for grid size " + gridSize);


        return getBoundingBoxsFor(gridSize).
                map(box -> new BoundingBoxWithStations(box, getStationsWithin(box))).
                filter(BoundingBoxWithStations::hasStations);
    }


    public Stream<BoundingBox> getBoundingBoxsFor(long gridSize) {
        // addresses performance and memory usages on very large grids
        return getEastingsStream(gridSize).
                flatMap(x -> getNorthingsStream(gridSize).
                        map(y -> new BoundingBox(x, y, x + gridSize, y + gridSize)));
    }

    private Stream<Long> getEastingsStream(long gridSize) {
        return LongStream.
                iterate(minEastings, current -> current <= maxEasting, current -> current + gridSize).boxed();
    }

    private Stream<Long> getNorthingsStream(long gridSize) {
        return LongStream.iterate(minNorthings, current -> current <= maxNorthings, current -> current + gridSize).boxed();
    }

    private Set<Station> getStationsWithin(BoundingBox box) {
        // TODO need more efficient way to do this?
        return positions.stream().
                filter(entry -> box.contained(entry.getGridPosition())).
                collect(Collectors.toSet());
    }

}

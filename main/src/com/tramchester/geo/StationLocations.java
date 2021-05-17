package com.tramchester.geo;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/***
 * Note: will return composites instead of stations if composite is within range
 */
@LazySingleton
public class StationLocations implements StationLocationsRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationLocations.class);

    private final StationRepository stationRepository;
    private final CompositeStationRepository compositeRepository;

    private long minEastings;
    private long maxEasting;
    private long minNorthings;
    private long maxNorthings;

    // split comp and non comp?
//    private final Set<Station> positions;

    @Inject
    public StationLocations(StationRepository stationRepository, CompositeStationRepository compositeRepository) {
        this.stationRepository = stationRepository;
        this.compositeRepository = compositeRepository;

        // bounding box for all stations
        minEastings = Long.MAX_VALUE;
        maxEasting = Long.MIN_VALUE;
        minNorthings = Long.MAX_VALUE;
        maxNorthings = Long.MIN_VALUE;
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        updateBoundingBox();
        logger.info("started");
    }

    @PreDestroy
    public void dispose() {
        logger.info("Stopped");
    }

    private void updateBoundingBox() {
        stationRepository.getStationStream().
                map(Station::getGridPosition).
                filter(GridPosition::isValid).
                forEach(this::updateBoundingBox);
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
    public List<Station> nearestStationsSorted(LatLong latLong, int maxToFind, double rangeInKM) {
        GridPosition gridPosition = CoordinateTransforms.getGridPosition(latLong);
        return nearestStationsSorted(gridPosition, maxToFind, rangeInKM);
    }

    private List<Station> nearestStationsSorted(GridPosition gridPosition, int maxToFind, double rangeInKM) {
        long rangeInMeters = Math.round(rangeInKM * 1000D);

        if (maxToFind > 1) {
            // only sort if more than one, as sorting potentially expensive
           return FindNear.getNearToSorted(compositeRepository.getStationStream(), gridPosition, rangeInMeters).
                    limit(maxToFind).
                    collect(Collectors.toList());
        } else {
            return FindNear.getNearTo(compositeRepository.getStationStream(), gridPosition, rangeInMeters).
                    limit(maxToFind).collect(Collectors.toList());
        }
    }

    @Override
    public Stream<Station> nearestStationsUnsorted(Station station, double rangeInKM) {
        long rangeInMeters = Math.round(rangeInKM * 1000D);
        return FindNear.getNearTo(stationRepository.getStationStream(), station.getGridPosition(), rangeInMeters);
    }

    public BoundingBox getBounds() {
        return new BoundingBox(minEastings, minNorthings, maxEasting, maxNorthings);
    }

    public boolean hasAnyNearby(GridPosition hasGridPosition, double rangeInKM) {
        long rangeInMeters = Math.round(rangeInKM * 1000D);
        return  FindNear.getNearTo(stationRepository.getStationStream(), hasGridPosition, rangeInMeters).findAny().isPresent();
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
        return stationRepository.getStationStream().
                filter(entry -> box.contained(entry.getGridPosition())).
                collect(Collectors.toSet());
    }

}

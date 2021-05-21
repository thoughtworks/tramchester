package com.tramchester.geo;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/***
 * Note: will return composites instead of stations if composite is within range
 */
@LazySingleton
public class StationLocations implements StationLocationsRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationLocations.class);
    private static final int DEPTH_LIMIT = 20;
    private static final int GRID_SIZE_METERS = 1000;

    private final StationRepository allStationRepository;
    private final CompositeStationRepository compositeStationRepository;
    private final Set<BoundingBox> quadrants;
    private final MarginInMeters walkingDistance;

    private final Map<BoundingBox, Set<String >> stations;
    private BoundingBox bounds;

    @Inject
    public StationLocations(StationRepository stationRepository, CompositeStationRepository compositeRepository, TramchesterConfig config) {
        this.allStationRepository = stationRepository;
        this.compositeStationRepository = compositeRepository;
        this.walkingDistance = MarginInMeters.of(config.getNearestStopForWalkingRangeKM());
        quadrants = new HashSet<>();
        stations = new HashMap<>();

    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        bounds = new CreateBoundingBox().createBoundingBox(allStationRepository.getStationStream());
        createQuadrants();
        logger.info("started");
    }

    private void createQuadrants() {
        BoundingBox box = getBounds();
        populateQuadrants(box, DEPTH_LIMIT);
        logger.info("Added " + quadrants.size() + " quadrants");
    }

    private void populateQuadrants(BoundingBox box, int depthLimit) {
        int currentLimit = depthLimit - 1;

        if (currentLimit<=0 || box.width() <= GRID_SIZE_METERS || box.height() <= GRID_SIZE_METERS) {
            logger.debug("Added " + box);
            quadrants.add(box);
            return;
        }

        Set<BoundingBox> newQuadrants = box.quadrants();
        newQuadrants.forEach(quadrant -> {
            if (containsAnyStations(box, quadrant)) {
                populateQuadrants(quadrant, currentLimit);
            }
        });
    }

    private boolean containsAnyStations(BoundingBox parent, BoundingBox quadrant) {
        return getNonComposites().
                map(Station::getGridPosition).
                filter(parent::contained).
                anyMatch(quadrant::contained);
    }

    private Stream<Station> getNonComposites() {
        return allStationRepository.getStationStream().
                filter(station -> station.getGridPosition().isValid());
    }

    @PreDestroy
    public void dispose() {
        logger.info("Stopped");
    }

    @Override
    public List<Station> nearestStationsSorted(LatLong latLong, int maxToFind, MarginInMeters rangeInMeters) {
        return nearestStationsSorted(CoordinateTransforms.getGridPosition(latLong), maxToFind, rangeInMeters);
    }

    private List<Station> nearestStationsSorted(GridPosition gridPosition, int maxToFind, MarginInMeters rangeInMeters) {

        if (maxToFind > 1) {
            // only sort if more than one, as sorting potentially expensive
           return FindNear.getNearToSorted(compositeStationRepository.getStationStream(), gridPosition, rangeInMeters).
                    limit(maxToFind).
                    collect(Collectors.toList());
        } else {
            return FindNear.getNearTo(compositeStationRepository.getStationStream(), gridPosition, rangeInMeters).
                    limit(maxToFind).
                    collect(Collectors.toList());
        }
    }

    @Override
    public Stream<Station> nearestStationsUnsorted(Station station, MarginInMeters rangeInMeters) {
        return FindNear.getNearTo(getNonComposites(), station.getGridPosition(), rangeInMeters);
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public boolean withinWalkingDistance(GridPosition position) {

        // find if within range of a box, if we then need to check if also within range of an actual station
        Set<BoundingBox> quadrantsWithinWalk = getQuadrantsWithinRange(position, walkingDistance);

        if (quadrantsWithinWalk.isEmpty()) {
            logger.debug("No quadrant contains " + position);
            return false;
        }

        Stream<Station> candidateStations = getNonComposites().
                filter(station -> quadrantsWithinWalk.stream().anyMatch(quad -> quad.contained(station.getGridPosition())));

        return FindNear.getNearTo(candidateStations, position, walkingDistance).
                findAny().isPresent();
    }

    @NotNull
    private Set<BoundingBox> getQuadrantsWithinRange(GridPosition position, MarginInMeters range) {
        return this.quadrants.stream().
                filter(quadrant -> quadrant.within(range, position)).collect(Collectors.toSet());
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

    private Set<Station> getStationsWithin(BoundingBox box) {
        // TODO need more efficient way to do this?
        return getNonComposites().
                filter(entry -> box.contained(entry.getGridPosition())).
                collect(Collectors.toSet());
    }

    public Stream<BoundingBox> getBoundingBoxsFor(long gridSize) {
        // addresses performance and memory usages on very large grids
        return getEastingsStream(gridSize).
                flatMap(x -> getNorthingsStream(gridSize).
                        map(y -> new BoundingBox(x, y, x + gridSize, y + gridSize)));
    }

    private Stream<Long> getEastingsStream(long gridSize) {
        long minEastings = bounds.getMinEastings();
        long maxEasting = bounds.getMaxEasting();
        return LongStream.
                iterate(minEastings, current -> current <= maxEasting, current -> current + gridSize).boxed();
    }

    private Stream<Long> getNorthingsStream(long gridSize) {
        long minNorthings = bounds.getMinNorthings();
        long maxNorthings = bounds.getMaxNorthings();
        return LongStream.iterate(minNorthings, current -> current <= maxNorthings, current -> current + gridSize).boxed();
    }

    public MarginInMeters getWalkingDistance() {
        return walkingDistance;
    }

    public Set<BoundingBox> getQuadrants() {
        return quadrants;
    }

    private static class CreateBoundingBox {
        private long minEastings = Long.MAX_VALUE;
        private long maxEasting = Long.MIN_VALUE;
        private long minNorthings = Long.MAX_VALUE;
        private long maxNorthings = Long.MIN_VALUE;

        private BoundingBox createBoundingBox(Stream<Station> stations) {
            stations.map(Station::getGridPosition).
                    filter(GridPosition::isValid).
                    forEach(gridPosition -> {
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
                    });
            return new BoundingBox(minEastings, minNorthings, maxEasting, maxNorthings);
        }
    }
}

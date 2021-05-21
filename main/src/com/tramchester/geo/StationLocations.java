package com.tramchester.geo;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashSet;
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
    private static final int DEPTH_LIMIT = 20;
    private static final int GRID_SIZE_METERS = 1000;

    private final StationRepository stationRepository;
    private final CompositeStationRepository compositeRepository;
    private final Set<BoundingBox> populatedQuadrants;
    private final MarginInMeters walkingDistance;

    private long minEastings;
    private long maxEasting;
    private long minNorthings;
    private long maxNorthings;

    @Inject
    public StationLocations(StationRepository stationRepository, CompositeStationRepository compositeRepository, TramchesterConfig config) {
        this.stationRepository = stationRepository;
        this.compositeRepository = compositeRepository;
        this.walkingDistance = MarginInMeters.of(config.getNearestStopForWalkingRangeKM());
        populatedQuadrants = new HashSet<>();

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
        createQuadrantsList();
        logger.info("started");
    }

    private void createQuadrantsList() {
        BoundingBox box = getBounds();
        populateQuadrants(box, DEPTH_LIMIT);
        logger.info("Added " + populatedQuadrants.size() + " quadrants");
    }

    private void populateQuadrants(BoundingBox box, int depthLimit) {
        int currentLimit = depthLimit - 1;

        if (currentLimit<=0 || box.width() <= GRID_SIZE_METERS || box.height() <= GRID_SIZE_METERS) {
            logger.debug("Added " + box);
            populatedQuadrants.add(box);
            return;
        }

        Set<BoundingBox> newQuadrants = box.quadrants();
        newQuadrants.forEach(quadrant -> {
            if (containsAny(box, quadrant)) {
                populateQuadrants(quadrant, currentLimit);
            }
        });
    }

    private boolean containsAny(BoundingBox parent, BoundingBox quadrant) {
        return getStationStream().
                filter(station -> parent.within(walkingDistance, station.getGridPosition())).
                anyMatch(station -> quadrant.within(walkingDistance, station.getGridPosition()));
    }

    private Stream<Station> getStationStream() {
        return stationRepository.getStationStream().
                filter(station -> station.getGridPosition().isValid());
    }

    @PreDestroy
    public void dispose() {
        logger.info("Stopped");
    }

    private void updateBoundingBox() {
        getStationStream().
                map(Station::getGridPosition).
                filter(GridPosition::isValid).
                forEach(this::updateBoundingBox);
        logger.info("Found bounds as " + getBounds());
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
    public List<Station> nearestStationsSorted(LatLong latLong, int maxToFind, MarginInMeters rangeInMeters) {
        GridPosition gridPosition = CoordinateTransforms.getGridPosition(latLong);
        return nearestStationsSorted(gridPosition, maxToFind, rangeInMeters);
    }

    private List<Station> nearestStationsSorted(GridPosition gridPosition, int maxToFind, MarginInMeters rangeInMeters) {

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
    public Stream<Station> nearestStationsUnsorted(Station station, MarginInMeters rangeInMeters) {
        return FindNear.getNearTo(getStationStream(), station.getGridPosition(), rangeInMeters);
    }

    public BoundingBox getBounds() {
        return new BoundingBox(minEastings, minNorthings, maxEasting, maxNorthings);
    }

    public boolean withinWalkingDistance(GridPosition position) {

        Set<BoundingBox> quadrants = populatedQuadrants.stream().
                filter(quadrant -> quadrant.within(walkingDistance, position)).
                collect(Collectors.toSet());

        if (quadrants.isEmpty()) {
            logger.debug("No quadrant contains " + position);
            return false;
        }

        Stream<Station> candidateStations = getStationStream().
                filter(station -> quadrants.stream().anyMatch(quad -> quad.contained(station.getGridPosition())));

        return FindNear.getNearTo(candidateStations, position, walkingDistance).
                findAny().isPresent();
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
        return getStationStream().
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
        return LongStream.
                iterate(minEastings, current -> current <= maxEasting, current -> current + gridSize).boxed();
    }

    private Stream<Long> getNorthingsStream(long gridSize) {
        return LongStream.iterate(minNorthings, current -> current <= maxNorthings, current -> current + gridSize).boxed();
    }

    public MarginInMeters getWalkingDistance() {
        return walkingDistance;
    }

    public Set<BoundingBox> getQuadrants() {
        return populatedQuadrants;
    }
}

package com.tramchester.geo;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.mappers.Geography;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.naptan.NaptanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@LazySingleton
public class StationLocations implements StationLocationsRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationLocations.class);

    private static final int DEPTH_LIMIT = 20;
    private static final int GRID_SIZE_METERS = 1000;

    private final StationRepository stationRepository;
    private final PlatformRepository platformRepository;
    private final NaptanRepository naptanRespository;
    private final Geography geography;
    private final Set<BoundingBox> quadrants;

    private final Map<IdFor<NaptanArea>, LocationSet> locationsInNaptanArea;
    private final Map<BoundingBox, Set<Station>> stationBoxes;

    private BoundingBox bounds;

    @Inject
    public StationLocations(StationRepository stationRepository, PlatformRepository platformRepository,
                            NaptanRepository naptanRespository, Geography geography) {
        this.stationRepository = stationRepository;
        this.platformRepository = platformRepository;
        this.naptanRespository = naptanRespository;
        this.geography = geography;

        quadrants = new HashSet<>();
        stationBoxes = new HashMap<>();
        locationsInNaptanArea = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        bounds = new CreateBoundingBox().createBoundingBox(stationRepository.getActiveStationStream());
        logger.info("Created bounds for active stations: " + bounds);
        createQuadrants();
        if (naptanRespository.isEnabled()) {
            populateAreas();
        } else {
            logger.warn("Naptan repository is disabled, no area data will be populated");
        }
        logger.info("started");
    }

    @PreDestroy
    public void dispose() {
        logger.info("Stopping");
        stationBoxes.values().forEach(Set::clear);
        stationBoxes.clear();
        quadrants.clear();
        locationsInNaptanArea.clear();
        logger.info("Stopped");
    }

    private void createQuadrants() {
        populateQuadrants(bounds, DEPTH_LIMIT);
        logger.info("Added " + quadrants.size() + " quadrants");
        quadrants.forEach(quadrant -> {
            // NOTE assumption - composite stations cannot be outside bounds defined by stations themselves since
            // composite location defined to be middle of composed stations
            Set<Station> foundInQuadrant = stationRepository.getActiveStationStream().
                    filter(station -> station.getGridPosition().isValid()).
                    filter(station -> quadrant.contained(station.getGridPosition())).
                    collect(Collectors.toSet());
            stationBoxes.put(quadrant, foundInQuadrant);
        });
    }

    private void populateQuadrants(BoundingBox box, int depthLimit) {
        int currentLimit = depthLimit - 1;

        if (currentLimit<=0 || box.width() <= GRID_SIZE_METERS || box.height() <= GRID_SIZE_METERS) {
            logger.debug("Added " + box);
            quadrants.add(box);
            return;
        }

        Set<GridPosition> insideBox = stationRepository.getActiveStationStream().
                map(Station::getGridPosition).
                filter(GridPosition::isValid).
                filter(box::contained).
                collect(Collectors.toSet());

        Set<BoundingBox> newQuadrants = box.quadrants();
        newQuadrants.forEach(quadrant -> {
            if (insideBox.stream().anyMatch(quadrant::contained)) {
                // TODO if need could inject list of station into recursive step, save recomputing
                populateQuadrants(quadrant, currentLimit);
            }
        });
    }

    private void populateAreas() {
        stationRepository.getActiveStationStream().
                filter(location -> location.getAreaId().isValid()).
                collect(Collectors.groupingBy(Location::getAreaId)).entrySet()
                .stream().
                filter(entry -> !entry.getValue().isEmpty()).
                forEach(entry -> locationsInNaptanArea.put(entry.getKey(), new LocationSet(entry.getValue())));

        platformRepository.getPlaformStream().
                filter(Location::isActive).
                collect(Collectors.groupingBy(Location::getAreaId)).entrySet()
                .stream().
                filter(entry -> !entry.getValue().isEmpty()).
                forEach(entry -> updateLocations(entry.getKey(), new LocationSet(entry.getValue())));

        logger.info("Added " + locationsInNaptanArea.size() + " areas which have stations");
    }

    private void updateLocations(IdFor<NaptanArea> areaId, LocationSet toAdd) {
        if (locationsInNaptanArea.containsKey(areaId)) {
            locationsInNaptanArea.get(areaId).addAll(toAdd);
        } else {
            locationsInNaptanArea.put(areaId, toAdd);
        }
    }

    public BoundingBox getActiveStationBounds() {
        return bounds;
    }

    @Override
    public LocationSet getLocationsWithin(IdFor<NaptanArea> areaId) {
        return locationsInNaptanArea.get(areaId);
    }

    /***
     * Uses Latitude/Longitude and EPSG
     * @param areaId the area id
     * @return A list of points on convex hull containing the points within the given area
     */
    @Override
    public List<LatLong> getBoundaryFor(IdFor<NaptanArea> areaId) {

        Set<NaptanRecord> records = naptanRespository.getRecordsFor(areaId);

        Stream<LatLong> points = records.stream().
                map(NaptanRecord::getGridPosition).
                map(CoordinateTransforms::getLatLong);

        return geography.createBoundaryFor(points);
    }

    /***
     * Uses Latitude/Longitude and EPSG
     * @return A list of points on convex hull containing all stations
     */
    public List<LatLong> getBoundaryForStations() {
        Stream<LatLong> points = stationRepository.getAllStationStream().
                map(Station::getLatLong);

        return geography.createBoundaryFor(points);
    }

    @Override
    public boolean hasStationsOrPlatformsIn(IdFor<NaptanArea> areaId) {
        // map only populated if an area does contain a station or platform
        return locationsInNaptanArea.containsKey(areaId);
    }

    @Override
    public boolean withinBounds(Location<?> location) {
        return getActiveStationBounds().contained(location);
    }

    public Set<BoundingBox> getQuadrants() {
        return quadrants;
    }

    @Override
    public List<Station> nearestStationsSorted(Location<?> location, int maxToFind, MarginInMeters rangeInMeters,
                                               EnumSet<TransportMode> modes) {
        return nearestStationsSorted(location.getGridPosition(), maxToFind, rangeInMeters, modes);
    }

    // TODO Use quadrants for this search?
    // TODO Station Groups here?
    public List<Station> nearestStationsSorted(GridPosition gridPosition, int maxToFind, MarginInMeters rangeInMeters,
                                               EnumSet<TransportMode> modes) {

        final Geography.LocationsSource<Station> source;
        if (modes.isEmpty()) {
            logger.warn("No station modes provided, will not filter stations by mode");
            source = stationRepository::getActiveStationStream;
        } else {
            source = () -> getStationModeFilteredStations(modes);
        }

        return geography.getNearToSorted(source, gridPosition, rangeInMeters).
                limit(maxToFind).
                collect(Collectors.toList());
    }

    private Stream<Station> getStationModeFilteredStations(EnumSet<TransportMode> modes) {
        return stationRepository.getActiveStationStream().
                filter(station -> TransportMode.intersects(modes, station.getTransportModes()));
    }

    @Override
    public Stream<Station> nearestStationsUnsorted(Station station, MarginInMeters rangeInMeters) {
        return geography.getNearToUnsorted(stationRepository::getActiveStationStream, station.getGridPosition(), rangeInMeters);
    }

    public boolean anyStationsWithinRangeOf(Location<?> position, MarginInMeters margin) {
        return anyStationsWithinRangeOf(position.getGridPosition(), margin);
    }

    public boolean anyStationsWithinRangeOf(GridPosition gridPosition, MarginInMeters margin) {
        // find if within range of a box, if we then need to check if also within range of an actual station
        Set<BoundingBox> quadrantsWithinRange = getQuadrantsWithinRange(gridPosition, margin);

        if (quadrantsWithinRange.isEmpty()) {
            logger.debug("No quadrant within range " + margin + " of " + gridPosition);
            return false;
        }

        Stream<Station> candidateStations = quadrantsWithinRange.stream().flatMap(quadrant -> stationBoxes.get(quadrant).stream());

        return geography.getNearToUnsorted(() -> candidateStations, gridPosition, margin).findAny().isPresent();
    }

    public Stream<BoundingBoxWithStations> getStationsInGrids(long gridSize) {
        if (gridSize <= 0) {
            throw new RuntimeException("Invalid grid size of " + gridSize);
        }

        logger.info("Getting groupded stations for grid size " + gridSize);

        return createBoundingBoxsFor(gridSize).
                map(box -> new BoundingBoxWithStations(box, getStationsWithin(box))).
                filter(BoundingBoxWithStations::hasStations);
    }

    public Stream<BoundingBox> createBoundingBoxsFor(long gridSize) {
        // addresses performance and memory usages on very large grids
        return getEastingsStream(gridSize).
                flatMap(x -> getNorthingsStream(gridSize).
                        map(y -> new BoundingBox(x, y, x + gridSize, y + gridSize)));
    }

    private Set<BoundingBox> getQuadrantsWithinRange(GridPosition position, MarginInMeters range) {
        return this.quadrants.stream().
                filter(quadrant -> quadrant.within(range, position)).collect(Collectors.toSet());
    }

    private Set<Station> getStationsWithin(BoundingBox box) {
        Stream<BoundingBox> overlaps = quadrants.stream().filter(box::overlapsWith);

        Stream<Station> candidateStations = overlaps.flatMap(quadrant -> stationBoxes.get(quadrant).stream());

        return candidateStations.filter(box::contained).collect(Collectors.toSet());
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

//    private boolean containsAnyStations(BoundingBox parent, BoundingBox quadrant) {
//        return stationRepository.getActiveStationStream().
//                map(Station::getGridPosition).
//                filter(GridPosition::isValid).
//                filter(parent::contained).
//                anyMatch(quadrant::contained);
//    }

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

            if (minEastings==Long.MAX_VALUE || maxEasting==Long.MIN_VALUE ||
                    minNorthings==Long.MIN_VALUE || maxNorthings==Long.MIN_VALUE) {
                String message = "Could not form bounded box for active stations, are any stations loaded?";
                logger.error(message);
                throw new RuntimeException(message);
            }

            return new BoundingBox(minEastings, minNorthings, maxEasting, maxNorthings);
        }
    }
}

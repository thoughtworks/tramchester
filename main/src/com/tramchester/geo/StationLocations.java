package com.tramchester.geo;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.naptan.NaptanRespository;
import org.geotools.metadata.iso.citation.CitationImpl;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.*;
import org.opengis.metadata.citation.Citation;
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
    private final NaptanRespository naptanRespository;
    private final Set<BoundingBox> quadrants;

    private final Map<IdFor<NaptanArea>, LocationSet> locationsInNaptanArea;

    private final Map<BoundingBox, Set<Station>> stations;
    private final GeometryFactory geometryFactory;
    private BoundingBox bounds;

    @Inject
    public StationLocations(StationRepository stationRepository, PlatformRepository platformRepository, NaptanRespository naptanRespository) {
        this.stationRepository = stationRepository;
        this.platformRepository = platformRepository;
        this.naptanRespository = naptanRespository;

        quadrants = new HashSet<>();
        stations = new HashMap<>();
        locationsInNaptanArea = new HashMap<>();

        // feels like there ought to be a better way of doing this.....
        Citation model = new CitationImpl("EPSG");
        int srid = Integer.parseInt(DefaultGeographicCRS.WGS84.getIdentifier(model).getCode());

        geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), srid);
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        bounds = new CreateBoundingBox().createBoundingBox(stationRepository.getActiveStationStream());
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
        stations.values().forEach(Set::clear);
        stations.clear();
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
            Set<Station> foundInQuadrant = getNonComposites().
                    filter(station -> quadrant.contained(station.getGridPosition())).
                    collect(Collectors.toSet());
            stations.put(quadrant, foundInQuadrant);
        });
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

    public BoundingBox getBounds() {
        return bounds;
    }

    @Override
    public LocationSet getLocationsWithin(IdFor<NaptanArea> areaId) {
        return locationsInNaptanArea.get(areaId);
    }

    @Override
    public Geometry getGeometryForArea(IdFor<NaptanArea> areaId) {

        // potentially slow

        Set<NaptanRecord> records = naptanRespository.getRecordsFor(areaId);

        List<Coordinate> points = records.stream().
                map(NaptanRecord::getGridPosition).
                map(CoordinateTransforms::getLatLong).
                map(latLong -> new Coordinate(latLong.getLat(), latLong.getLon())).
                collect(Collectors.toList());

        Coordinate[] asArray = points.toArray(new Coordinate[]{});

        MultiPoint multiPoint = geometryFactory.createMultiPointFromCoords(asArray);

        return multiPoint.convexHull();
    }

    @Override
    public List<LatLong> getBoundaryFor(IdFor<NaptanArea> areaId) {
        Geometry geometry = getGeometryForArea(areaId);

        if (geometry.getNumPoints()==0) {
            logger.error("Zero points for boundary for area " + areaId);
        }

        Geometry boundary = geometry.getBoundary();

        return Arrays.stream(boundary.getCoordinates()).map(LatLong::of).collect(Collectors.toList());
    }

    @Override
    public boolean hasStationsOrPlatformsIn(IdFor<NaptanArea> areaId) {
        // map only populated if an area does contain a station or platform
        return locationsInNaptanArea.containsKey(areaId);
    }

    public Set<BoundingBox> getQuadrants() {
        return quadrants;
    }

    @Override
    public List<Station> nearestStationsSorted(LatLong latLong, int maxToFind, MarginInMeters rangeInMeters) {
        return nearestStationsSorted(CoordinateTransforms.getGridPosition(latLong), maxToFind, rangeInMeters);
    }

    // TODO Use quadrants for this search
    // NOTE: uses composite stations
    // TODO Station Groups here?
    private List<Station> nearestStationsSorted(GridPosition gridPosition, int maxToFind, MarginInMeters rangeInMeters) {

        final Stream<Station> stationStream = stationRepository.getActiveStationStream();
        if (maxToFind > 1) {
            // only sort if more than one, as sorting potentially expensive
           return FindNear.getNearToSorted(stationStream, gridPosition, rangeInMeters).
                    limit(maxToFind).
                    collect(Collectors.toList());
        } else {
            return FindNear.getNearTo(stationStream, gridPosition, rangeInMeters).
                    limit(maxToFind).
                    collect(Collectors.toList());
        }
    }

    @Override
    public Stream<Station> nearestStationsUnsorted(Station station, MarginInMeters rangeInMeters) {
        return FindNear.getNearTo(getNonComposites(), station.getGridPosition(), rangeInMeters);
    }

    public boolean withinRangeOfStation(GridPosition position, MarginInMeters margin) {

        // find if within range of a box, if we then need to check if also within range of an actual station
        Set<BoundingBox> quadrantsWithinRange = getQuadrantsWithinRange(position, margin);

        if (quadrantsWithinRange.isEmpty()) {
            logger.debug("No quadrant within range " + margin + " of " + position);
            return false;
        }

        Stream<Station> candidateStations = quadrantsWithinRange.stream().flatMap(quadrant -> stations.get(quadrant).stream());

        return FindNear.getNearTo(candidateStations, position, margin).findAny().isPresent();
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

    @NotNull
    private Set<BoundingBox> getQuadrantsWithinRange(GridPosition position, MarginInMeters range) {
        return this.quadrants.stream().
                filter(quadrant -> quadrant.within(range, position)).collect(Collectors.toSet());
    }

    private Set<Station> getStationsWithin(BoundingBox box) {
        Stream<BoundingBox> overlaps = quadrants.stream().filter(box::overlapsWith);

        Stream<Station> candidateStations = overlaps.flatMap(quadrant -> stations.get(quadrant).stream());

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

    private boolean containsAnyStations(BoundingBox parent, BoundingBox quadrant) {
        return getNonComposites().
                map(Station::getGridPosition).
                filter(parent::contained).
                anyMatch(quadrant::contained);
    }

    private Stream<Station> getNonComposites() {
        return stationRepository.getActiveStationStream().
                filter(station -> station.getGridPosition().isValid());
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

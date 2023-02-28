package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.NeighbourConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocationsRepository;
import com.tramchester.mappers.Geography;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.measure.Quantity;
import javax.measure.quantity.Length;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Walk;
import static java.lang.String.format;

// TODO Location<?> not Station

@LazySingleton
public class Neighbours implements NeighboursRepository {
    private static final Logger logger = LoggerFactory.getLogger(Neighbours.class);

    // ONLY link stations of different types
    private static final boolean DIFF_MODES_ONLY = true;

    private final StationRepository stationRepository;
    private final StationLocationsRepository stationLocations;
    private final Geography geography;

    private final Map<IdFor<Station>, Set<StationLink>> neighbours;
    private final boolean enabled;
    private final NeighbourConfig config;

    @Inject
    public Neighbours(StationRepository stationRepository, StationLocationsRepository stationLocations, TramchesterConfig config, Geography geography) {
        this.stationRepository = stationRepository;
        this.stationLocations = stationLocations;
        this.geography = geography;

        enabled = config.hasNeighbourConfig();

        if (enabled) {
            this.config = config.getNeighbourConfig();
        } else {
            this.config = null;
        }
        neighbours = new HashMap<>();
    }

    @PostConstruct
    private void start() {
        if (!enabled) {
            logger.warn("Disabled in config");
            return;
        }
        logger.info("Starting");
        createNeighbours();
        addFromConfig();
        logger.info("Started");
    }

    private void addFromConfig() {
        List<StationIdPair> additional = config.getAdditional();
        if (additional.isEmpty()) {
            logger.info("No additional neighbours found in config");
        } else {
            logger.info("Attempt to add neighbours for " + additional);
        }

        List<StationPair> toAdd = additional.stream().
                filter(this::bothValid).
                map(stationRepository::getStationPair).
                collect(Collectors.toList());

        if (additional.size() != toAdd.size()) {
            logger.warn("Not adding all of the requested additional neighbours, some were invalid, check the logs above");
        }

        toAdd.forEach(this::addAsNeighbours);
    }

    private void addAsNeighbours(StationPair pair) {
        Station begin = pair.getBegin();
        Station end = pair.getEnd();

        if (areNeighbours(begin, end)) {
            logger.warn("Config contains pair that were already present as neighbours, skipping " + pair);
            return;
        }

        logger.info("Adding " + pair + " as neighbours");

        Quantity<Length> distance = geography.getDistanceBetweenInMeters(begin, end);
        final Duration walkingDuration = geography.getWalkingDuration(begin, end);

        addNeighbour(begin, new StationLink(begin, end, Collections.singleton(Walk), distance, walkingDuration));
        addNeighbour(end, new StationLink(end, begin, Collections.singleton(Walk), distance, walkingDuration));
    }

    void addNeighbour(Station station, StationLink link) {
        IdFor<Station> id = station.getId();
        if (neighbours.containsKey(id)) {
            neighbours.get(id).add(link);
        } else {
            HashSet<StationLink> links = new HashSet<>();
            links.add(link);
            neighbours.put(id, links);
        }
    }

    private boolean bothValid(StationIdPair stationIdPair) {
        if (!stationRepository.hasStationId(stationIdPair.getBeginId())) {
            logger.warn(format("begin station id for pair %s is invalid", stationIdPair));
            return false;
        }
        if (!stationRepository.hasStationId(stationIdPair.getEndId())) {
            logger.warn(format("end station id for pair %s is invalid", stationIdPair));
            return false;
        }
        return true;
    }

    @PreDestroy
    private void stop() {
        logger.info("Stopping");
        neighbours.clear();
        logger.info("stopped");
    }

    @Override
    public Set<StationLink> getAll() {
        return neighbours.values().stream().flatMap(Collection::stream).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<Station> getNeighboursFor(IdFor<Station> id) {
        return neighbours.get(id).stream().map(StationLink::getEnd).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<StationLink> getNeighbourLinksFor(IdFor<Station> id) {
        return neighbours.get(id);
    }

    @Override
    public boolean hasNeighbours(IdFor<Station> id) {
        return neighbours.containsKey(id);
    }

    @Override
    public boolean areNeighbours(Location<?> start, Location<?> destination) {
        if (start.getLocationType() == LocationType.Station && destination.getLocationType()==LocationType.Station) {
            IdFor<Station> stationId = StringIdFor.convert(start.getId(), Station.class);
            IdFor<Station> destinationId = StringIdFor.convert(destination.getId(), Station.class);
            if (!hasNeighbours(stationId)) {
                return false;
            }
            IdSet<Station> neighbours = getNeighboursFor(stationId).stream().collect(IdSet.collector());
            return neighbours.contains(destinationId);
        }
        return false;
    }

    @Override
    public boolean areNeighbours(LocationSet starts, LocationSet destinations) {
        return starts.stationsOnlyStream().
                map(Location::getId).
                filter(this::hasNeighbours).
                map(this::getNeighboursFor).
                anyMatch(neighbours -> destinations.stationsOnlyStream().anyMatch(neighbours::contains));
    }

    @Override
    public boolean differentModesOnly() {
        return DIFF_MODES_ONLY;
    }

    private void createNeighbours() {
        MarginInMeters marginInMeters = MarginInMeters.of(config.getDistanceToNeighboursKM());

        logger.info(format("Adding neighbouring stations for range %s and diff modes only %s",
                marginInMeters, DIFF_MODES_ONLY));

        final Set<TransportMode> walk = Collections.singleton(Walk);

        stationRepository.getActiveStationStream().
            filter(station -> station.getGridPosition().isValid()).
            forEach(begin -> {
                Set<TransportMode> beginModes = begin.getTransportModes();
                // nearby could be any transport mode
                Set<StationLink> links = stationLocations.nearestStationsUnsorted(begin, marginInMeters).
                    filter(nearby -> !nearby.equals(begin)).
                    filter(nearby -> DIFF_MODES_ONLY && noOverlapModes(beginModes, nearby.getTransportModes())).
                    map(nearby -> StationLink.create(begin, nearby, walk, geography)).
                    collect(Collectors.toSet());
                if (!links.isEmpty()) {
                    neighbours.put(begin.getId(), links);
                }
            });

        logger.info("Added " + neighbours.size() + " station with neighbours");

    }

    private boolean noOverlapModes(Set<TransportMode> modesA, Set<TransportMode> modesB) {
        boolean aNotInB = modesA.stream().noneMatch(modesB::contains);
        boolean bNotInA = modesB.stream().noneMatch(modesA::contains);
        return aNotInB && bNotInA;
    }
}

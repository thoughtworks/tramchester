package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.StationLink;
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
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

// TODO Location<?> not Station

@LazySingleton
public class Neighbours implements NeighboursRepository {
    private static final Logger logger = LoggerFactory.getLogger(Neighbours.class);

    // ONLY link stations of different types
    private static final boolean DIFF_MODES_ONLY = true;

    private final StationRepository stationRepository;
    private final StationLocationsRepository stationLocations;
    private final MarginInMeters marginInMeters;
    private final Geography geography;


    private final Map<IdFor<Station>, Set<StationLink>> neighbours;
    private final boolean enabled;

    @Inject
    public Neighbours(StationRepository stationRepository, StationLocationsRepository stationLocations, TramchesterConfig config, Geography geography) {
        this.stationRepository = stationRepository;
        this.stationLocations = stationLocations;
        this.geography = geography;

        this.marginInMeters = MarginInMeters.of(config.getDistanceToNeighboursKM());
        enabled = config.getCreateNeighbours();
        neighbours = new HashMap<>();
    }

    @PostConstruct
    private void start() {
        if (!enabled) {
            logger.warn("Disabled in config");
            return;
        }
        logger.info("Starting");
        createNeighboursFor();
        logger.info("Started");
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
        if (start.getLocationType()== LocationType.Station && destination.getLocationType()==LocationType.Station) {
            IdFor<Station> stationId = StringIdFor.convert(start.getId());
            IdFor<Station> destinationId = StringIdFor.convert(destination.getId());
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

    private void createNeighboursFor() {
        logger.info(format("Adding neighbouring stations for range %s and diff modes only %s",
                marginInMeters, DIFF_MODES_ONLY));

        final Set<TransportMode> walk = Collections.singleton(TransportMode.Walk);

        stationRepository.getActiveStationStream().
            filter(station -> station.getGridPosition().isValid()).
            forEach(begin -> {
                Set<TransportMode> beginModes = begin.getTransportModes();
                // nearby could be any transport mode
                Set<StationLink> links = stationLocations.nearestStationsUnsorted(begin, marginInMeters).
                    filter(nearby -> !nearby.equals(begin)).
                    filter(nearby -> DIFF_MODES_ONLY && noOverlapModes(beginModes, nearby.getTransportModes())).
                    map(nearby -> StationLink.create(begin, nearby, walk, stationLocations, geography)).
                    collect(Collectors.toUnmodifiableSet());
                neighbours.put(begin.getId(), links);
            });

        logger.info("Added " + neighbours.size() + " station with neighbours");

    }

    private boolean noOverlapModes(Set<TransportMode> modesA, Set<TransportMode> modesB) {
        boolean aNotInB = modesA.stream().noneMatch(modesB::contains);
        boolean bNotInA = modesB.stream().noneMatch(modesA::contains);
        return aNotInB && bNotInA;
    }
}

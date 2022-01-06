package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.JourneyRequest;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.RunningRoutesAndServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class JourneyConstraints {

    private static final int TRAMS_MAX_PATH_LENGTH = 400; // likely bigger than needed, but does not impact performance

    private static final int BUSES_MAX_PATH_LENGTH = 1000; // todo right value?
    private static final int TRAINS_MAX_PATH_LENGTH = 2000; // todo right value?
    private static final int FERRY_MAX_PATH_LENGTH = 200; // todo right value?
    private static final int SUBWAY_MAX_PATH_LENGTH = 400; // todo right value?

    private static final Logger logger = LoggerFactory.getLogger(JourneyConstraints.class);

    private final RunningRoutesAndServices.FilterForDate routesAndServicesFilter;
    private final TramchesterConfig config;
    private final int maxPathLength;
    private final Set<Station> endStations;
    private final IdSet<Station> closedStations;
    private final int maxJourneyDuration;
    private final int maxWalkingConnections;
    private final int maxNeighbourConnections;
    private final LowestCostsForDestRoutes lowestCostForDestinations;

    public JourneyConstraints(TramchesterConfig config, RunningRoutesAndServices.FilterForDate routesAndServicesFilter,
                              JourneyRequest journeyRequest,
                              ClosedStationsRepository closedStationsRepository, Set<Station> endStations,
                              LowestCostsForDestRoutes lowestCostForDestinations, int maxJourneyDuration) {
        this(config, routesAndServicesFilter, closedStationsRepository.getClosedStationsFor(journeyRequest.getDate()),
                endStations, lowestCostForDestinations, maxJourneyDuration);
    }

    public JourneyConstraints(TramchesterConfig config, RunningRoutesAndServices.FilterForDate routesAndServicesFilter,
                              IdSet<Station> closedStations, Set<Station> endStations,
                              LowestCostsForDestRoutes lowestCostForDestinations, int maxJourneyDuration) {
        this.config = config;
        this.lowestCostForDestinations = lowestCostForDestinations;
        this.routesAndServicesFilter = routesAndServicesFilter;
        this.maxPathLength = computeMaxPathLength();

        this.endStations = endStations;
        this.maxJourneyDuration = maxJourneyDuration;
        this.maxWalkingConnections = config.getMaxWalkingConnections();
        this.maxNeighbourConnections = config.getMaxNeighbourConnections();

        this.closedStations = closedStations;

        if (!closedStations.isEmpty()) {
            logger.info("Have closed stations " + closedStations);
        }

    }

    private int computeMaxPathLength() {
        return config.getTransportModes().stream().map(this::getPathMaxFor).max(Integer::compareTo).orElseThrow();
    }

    private int getPathMaxFor(TransportMode mode) {
        return switch (mode) {
            case Tram -> TRAMS_MAX_PATH_LENGTH;
            case RailReplacementBus, Bus -> BUSES_MAX_PATH_LENGTH;
            case Train -> TRAINS_MAX_PATH_LENGTH;
            case Subway -> SUBWAY_MAX_PATH_LENGTH;
            case Ferry -> FERRY_MAX_PATH_LENGTH;
            default -> throw new RuntimeException("Unexpected transport mode " + mode);
        };
    }


    public int getMaxPathLength() {
        return maxPathLength;
    }

    public Set<Station> getEndStations() {
        return endStations;
    }

    public int getMaxJourneyDuration() {
        return maxJourneyDuration;
    }

    public boolean isClosed(Station station) {
        return closedStations.contains(station.getId());
    }

    public int getMaxWalkingConnections() {
        return maxWalkingConnections;
    }

    public int getMaxNeighbourConnections() {
        return maxNeighbourConnections;
    }

    public LowestCostsForDestRoutes getFewestChangesCalculator() {
        return lowestCostForDestinations;
    }

    @Override
    public String toString() {
        return "JourneyConstraints{" +
                "runningServices=" + routesAndServicesFilter +
                ", maxPathLength=" + maxPathLength +
                ", endStations=" + HasId.asIds(endStations) +
                ", closedStations=" + closedStations +
                ", maxJourneyDuration=" + maxJourneyDuration +
                ", maxWalkingConnections=" + maxWalkingConnections +
                ", maxNeighbourConnections=" + maxNeighbourConnections +
                '}';
    }

    public boolean isUnavailable(Route route, TramTime visitTime) {
        return !routesAndServicesFilter.isRouteRunning(route.getId(), visitTime);
    }

    public boolean isRunning(IdFor<Service> serviceId, TramTime visitTime) {
        return routesAndServicesFilter.isServiceRunning(serviceId, visitTime);
    }

}

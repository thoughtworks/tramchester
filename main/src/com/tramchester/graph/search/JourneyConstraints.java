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
import com.tramchester.repository.RouteRepository;
import com.tramchester.repository.RunningRoutesAndServices;
import com.tramchester.repository.ServiceRepository;
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

    private final RunningRoutesAndServices runningRoutesAndServices;
    private final TramchesterConfig config;
    private final int maxPathLength;
    private final Set<Station> endStations;
    private final IdSet<Station> closedStations;
    private final int maxJourneyDuration;
    private final int maxWalkingConnections;
    private final int maxNeighbourConnections;
    private final LowestCostsForRoutes lowestCostForDestinations;

    public JourneyConstraints(TramchesterConfig config, RouteRepository routeRepository, ServiceRepository serviceRepository,
                              JourneyRequest journeyRequest,
                              ClosedStationsRepository closedStationsRepository, Set<Station> endStations,
                              LowestCostsForRoutes lowestCostForDestinations) {
        this(config, new RunningRoutesAndServices(journeyRequest.getDate(), serviceRepository, routeRepository),
                journeyRequest, closedStationsRepository.getClosedStationsFor(journeyRequest.getDate()),
                endStations,
                lowestCostForDestinations);
//        this.config = config;
//        this.lowestCostForDestinations = lowestCostForDestinations;
//        this.runningRoutesAndServices = new RunningRoutesAndServices(journeyRequest.getDate(), serviceRepository,
//                routeRepository);
//        this.maxPathLength = computeMaxPathLength();
//
//        this.endStations = endStations;
//        this.maxJourneyDuration = journeyRequest.getMaxJourneyDuration();
//        this.maxWalkingConnections = config.getMaxWalkingConnections();
//        this.maxNeighbourConnections = config.getMaxNeighbourConnections();
//
//        this.closedStations = closedStationsRepository.getClosedStationsFor(journeyRequest.getDate());
//
//        if (!closedStations.isEmpty()) {
//            logger.info("Have closed stations " + closedStations);
//        }
    }

    public JourneyConstraints(TramchesterConfig config, RunningRoutesAndServices runningRoutesAndServices,
                              JourneyRequest journeyRequest,
                              IdSet<Station> closedStations, Set<Station> endStations,
                              LowestCostsForRoutes lowestCostForDestinations) {
        this.config = config;
        this.lowestCostForDestinations = lowestCostForDestinations;
        this.runningRoutesAndServices = runningRoutesAndServices;
        this.maxPathLength = computeMaxPathLength();

        this.endStations = endStations;
        this.maxJourneyDuration = journeyRequest.getMaxJourneyDuration();
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

    public boolean isRunning(IdFor<Service> serviceId) {
        return runningRoutesAndServices.isRunning(serviceId);
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

    public LowestCostsForRoutes getFewestChangesCalculator() {
        return lowestCostForDestinations;
    }

    @Override
    public String toString() {
        return "JourneyConstraints{" +
                "runningServices=" + runningRoutesAndServices.size() +
                ", maxPathLength=" + maxPathLength +
                ", endStations=" + HasId.asIds(endStations) +
                ", closedStations=" + closedStations +
                ", maxJourneyDuration=" + maxJourneyDuration +
                ", maxWalkingConnections=" + maxWalkingConnections +
                ", maxNeighbourConnections=" + maxNeighbourConnections +
                '}';
    }

    // TODO next day?
    public boolean isUnavailable(Route route, TramTime currentElapsed) {
        return !runningRoutesAndServices.isRouteRunning(route.getId());
    }
}

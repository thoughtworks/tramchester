package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.RunningRoutesAndServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

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
    private final LocationSet endStations;
    private final IdSet<Station> closedStationsIds;
    private final Duration maxJourneyDuration;
    private final int maxWalkingConnections;
    private final int maxNumberWalkingConnections;
    private final LowestCostsForDestRoutes lowestCostForDestinations;

    public JourneyConstraints(TramchesterConfig config, RunningRoutesAndServices.FilterForDate routesAndServicesFilter,
                              IdSet<Station> closedStationsIds, LocationSet endStations,
                              LowestCostsForDestRoutes lowestCostForDestinations, Duration maxJourneyDuration) {
        this.config = config;
        this.lowestCostForDestinations = lowestCostForDestinations;
        this.routesAndServicesFilter = routesAndServicesFilter;
        this.maxPathLength = computeMaxPathLength();

        this.endStations = endStations;
        this.maxJourneyDuration = maxJourneyDuration;
        this.maxWalkingConnections = config.getMaxWalkingConnections();

        this.maxNumberWalkingConnections = config.getMaxWalkingConnections();

        this.closedStationsIds = closedStationsIds;

        if (!closedStationsIds.isEmpty()) {
            logger.info("Have closed stations " + closedStationsIds);
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

    public LocationSet getEndStations() {
        return endStations;
    }

    public Duration getMaxJourneyDuration() {
        return maxJourneyDuration;
    }

    public boolean isClosed(Station station) {
        return closedStationsIds.contains(station.getId());
    }

    public int getMaxWalkingConnections() {
        return maxWalkingConnections;
    }

//    public int getMaxNumberWalkingConnections() {
//        return maxNumberWalkingConnections;
//    }

    public LowestCostsForDestRoutes getFewestChangesCalculator() {
        return lowestCostForDestinations;
    }

    @Override
    public String toString() {
        return "JourneyConstraints{" +
                "runningServices=" + routesAndServicesFilter +
                ", maxPathLength=" + maxPathLength +
                ", endStations=" + endStations +
                ", closedStations=" + closedStationsIds +
                ", maxJourneyDuration=" + maxJourneyDuration +
                ", maxWalkingConnections=" + maxWalkingConnections +
                ", maxNeighbourConnections=" + maxNumberWalkingConnections +
                '}';
    }

    public boolean isUnavailable(Route route, TramTime visitTime) {
        return !routesAndServicesFilter.isRouteRunning(route.getId(), visitTime.isNextDay());
    }

    // time and date separate for performance reasons
    public boolean isRunningOnDate(IdFor<Service> serviceId, TramTime visitTime) {
        return routesAndServicesFilter.isServiceRunningByDate(serviceId, visitTime.isNextDay());
    }

    // time and date separate for performance reasons
    public boolean isRunningAtTime(IdFor<Service> serviceId, TramTime time, int maxWait) {
        return routesAndServicesFilter.isServiceRunningByTime(serviceId, time, maxWait);
    }
}

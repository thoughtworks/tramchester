package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.CachedNodeOperations;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TramReachabilityRepository;
import com.tramchester.repository.RunningServices;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.graph.GraphStaticKeys.ID;

public class ServiceHeuristics {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHeuristics.class);
    private static final boolean debugEnabled = logger.isDebugEnabled();

    private final RunningServices runningServices;
    private final List<Station> endTramStations;
    private final TramTime queryTime;
    private final ServiceReasons reasons;
    private final TramReachabilityRepository tramReachabilityRepository;
    private final int maxPathLength;
//    private final List<Route> busRoutesSeen;

    private final StationRepository stationRepository;
    private final CachedNodeOperations nodeOperations;
    private final int maxJourneyDuration;

    private final int maxWaitMinutes;
    private final int changesLimit;

    public ServiceHeuristics(StationRepository stationRepository, CachedNodeOperations nodeOperations,
                             TramReachabilityRepository tramReachabilityRepository, TramchesterConfig config,
                             TramTime queryTime, RunningServices runningServices, List<Station> endStations,
                             ServiceReasons reasons, int maxPathLength, int maxChanges) {
        this.stationRepository = stationRepository;
        this.nodeOperations = nodeOperations;
        this.tramReachabilityRepository = tramReachabilityRepository;

        this.maxWaitMinutes = config.getMaxWait();
        this.maxJourneyDuration = config.getMaxJourneyDuration();
        this.queryTime = queryTime;
        this.changesLimit = maxChanges;
        this.runningServices = runningServices;
        this.reasons = reasons;

        endTramStations = endStations.stream().filter(Station::isTram).collect(Collectors.toList());
        this.maxPathLength = maxPathLength;

//        busRoutesSeen = new ArrayList<>();
    }
    
    public ServiceReason checkServiceDate(Node node, Path path) {
        reasons.incrementTotalChecked();

        String nodeServiceId = nodeOperations.getServiceId(node);

        if (runningServices.isRunning(nodeServiceId)) {
            return valid(path);
        }

        return notOnQueryDate(path, nodeServiceId);
    }

    public ServiceReason checkServiceTime(Path path, Node node, TramTime currentClock) {
        reasons.incrementTotalChecked();

        String serviceId = nodeOperations.getServiceId(node);

        // prepared to wait up to max wait for start of a service...
        TramTime serviceStart = runningServices.getServiceEarliest(serviceId).minusMinutes(maxWaitMinutes);

        // BUT if arrive after service finished there is nothing to be done...
        TramTime serviceEnd = runningServices.getServiceLatest(serviceId);

        if (!currentClock.between(serviceStart, serviceEnd)) {
            return reasons.recordReason(ServiceReason.ServiceNotRunningAtTime(currentClock, path));
        }

        return valid(path);
    }

    public ServiceReason checkNumberChanges(int currentNumChanges, Path path) {
       reasons.incrementTotalChecked();

       if (currentNumChanges>changesLimit) {
         return reasons.recordReason(ServiceReason.TooManyChanges(path));
       }
       return valid(path);
    }

    public ServiceReason checkTime(Path path, Node node, TramTime currentElapsed) {
        reasons.incrementTotalChecked();

        TramTime nodeTime = nodeOperations.getTime(node);
        if (currentElapsed.isAfter(nodeTime)) { // already departed
            return reasons.recordReason(ServiceReason.AlreadyDeparted(currentElapsed, path));
        }

        if (operatesWithinTime(nodeTime, currentElapsed)) {
            return valid(path);
        }
        return reasons.recordReason(ServiceReason.DoesNotOperateOnTime(currentElapsed, path));
    }

    private boolean operatesWithinTime(TramTime nodeTime, TramTime elapsedTimed) {
        TramTime earliest = nodeTime.minusMinutes(maxWaitMinutes);
        return elapsedTimed.between(earliest, nodeTime);
    }

    public ServiceReason interestedInHour(Path path, int hour, TramTime journeyClockTime) {
        reasons.incrementTotalChecked();

        int queryTimeHour = journeyClockTime.getHourOfDay();
        if (hour == queryTimeHour) {
            // quick win
            return valid(path);
        }

        // this only works if maxWaitMinutes<60
        int previousHour = hour - 1;
        if (previousHour==-1) {
            previousHour = 23;
        }
        if (queryTimeHour == previousHour) {
            int timeUntilNextHour = 60 - maxWaitMinutes;
            if (journeyClockTime.getMinuteOfHour() >= timeUntilNextHour) {
                return valid(path);
            }
        }

        return reasons.recordReason(ServiceReason.DoesNotOperateAtHour(journeyClockTime, path));
    }

    // TODO will need re-working once interchange between tram/bus is defined
    public ServiceReason canReachDestination(Node endNode, Path path) {

        String routeStationId = endNode.getProperty(ID).toString();
        RouteStation routeStation = stationRepository.getRouteStation(routeStationId);

        if (routeStation==null) {
            throw new RuntimeException("Missing routestation " + routeStationId);
        }

        if (routeStation.isTram()) {
            for(Station endStation : endTramStations) {
                if (tramReachabilityRepository.stationReachable(routeStation, endStation)) {
                    return valid(path);
                }
            }
            return reasons.recordReason(ServiceReason.StationNotReachable(path));
        }

        // On bus
//        Route route = routeStation.getRoute();
//        if (busRoutesSeen.contains(route)) {
//            return reasons.recordReason(ServiceReason.RouteAlreadySeen(path));
//        }
//        busRoutesSeen.add(route);

        // TODO can't exclude unless we know for sure not reachable, so include all for buses
        return valid(path);
    }

    public ServiceReason journeyDurationUnderLimit(final int totalCost, final Path path) {
        if (totalCost>maxJourneyDuration) {
            return reasons.recordReason(ServiceReason.TookTooLong(queryTime.plusMinutes(totalCost), path));
        }

        return valid(path);
    }

    private ServiceReason valid(final Path path) {
        if (debugEnabled) {
            return reasons.recordReason(ServiceReason.IsValid(path));
        }
        return reasons.recordReason(ServiceReason.isValid);
    }

    private ServiceReason notOnQueryDate(Path path, String nodeServiceId) {
        if (debugEnabled) {
            return reasons.recordReason(ServiceReason.DoesNotRunOnQueryDate(nodeServiceId, path));
        }
        return reasons.recordReason(ServiceReason.DoesNotRunOnQueryDate());
    }

    public TramTime getQueryTime() {
        return queryTime;
    }

    public int getMaxPathLength() {
        return maxPathLength;
    }

}

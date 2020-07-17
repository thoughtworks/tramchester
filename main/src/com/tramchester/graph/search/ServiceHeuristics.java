package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.repository.RunningServices;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TramReachabilityRepository;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.graph.GraphStaticKeys.ID;

public class ServiceHeuristics {

    private static final Logger logger;
    private static final boolean debugEnabled;

    static {
        logger = LoggerFactory.getLogger(ServiceReasons.class);
        debugEnabled = logger.isDebugEnabled();
    }

    private final RunningServices runningServices;
    private final Set<Station> endTramStations;
    private final TramTime queryTime;
    private final TramReachabilityRepository tramReachabilityRepository;
    private final int maxPathLength;

    private final StationRepository stationRepository;
    private final NodeContentsRepository nodeOperations;
    private final int maxJourneyDuration;

    private final int maxWaitMinutes;
    private final int changesLimit;
    private final boolean tramOnly;

    public ServiceHeuristics(StationRepository stationRepository, NodeContentsRepository nodeOperations,
                             TramReachabilityRepository tramReachabilityRepository, TramchesterConfig config,
                             TramTime queryTime, RunningServices runningServices, Set<Station> endStations,
                             int maxPathLength, JourneyRequest journeyRequest) {
        this.stationRepository = stationRepository;
        this.nodeOperations = nodeOperations;
        this.tramReachabilityRepository = tramReachabilityRepository;

        this.maxWaitMinutes = config.getMaxWait();
        this.queryTime = queryTime;
        this.changesLimit = journeyRequest.getMaxChanges();
        this.maxJourneyDuration = journeyRequest.getMaxJourneyDuration();
        this.runningServices = runningServices;

        endTramStations = endStations.stream().
                filter(TransportMode::isTram).
                collect(Collectors.toSet());

        tramOnly = (endTramStations.size() == endStations.size());
        if (tramOnly) {
            logger.info("Checking only for tram destinations");
        }

        this.maxPathLength = maxPathLength;
    }
    
    public ServiceReason checkServiceDate(Node node, Path path, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        String nodeServiceId = nodeOperations.getServiceId(node);

        if (runningServices.isRunning(nodeServiceId)) {
            return valid(path, reasons);
        }

        return notOnQueryDate(path, reasons);
    }

    public ServiceReason checkServiceTime(Path path, Node node, TramTime currentClock, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        String serviceId = nodeOperations.getServiceId(node);

        // prepared to wait up to max wait for start of a service...
        ServiceTime serviceStart = runningServices.getServiceEarliest(serviceId).minusMinutes(maxWaitMinutes);

        // BUT if arrive after service finished there is nothing to be done...
        ServiceTime serviceEnd = runningServices.getServiceLatest(serviceId);

        if (!currentClock.between(serviceStart, serviceEnd)) {
            return reasons.recordReason(ServiceReason.ServiceNotRunningAtTime(currentClock, path));
        }

        return valid(path, reasons);
    }

    public ServiceReason checkNumberChanges(int currentNumChanges, Path path, ServiceReasons reasons) {
       reasons.incrementTotalChecked();

       if (currentNumChanges>changesLimit) {
         return reasons.recordReason(ServiceReason.TooManyChanges(path));
       }
       return valid(path, reasons);
    }

    public ServiceReason checkTime(Path path, Node node, TramTime currentElapsed, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        TramTime nodeTime = nodeOperations.getTime(node);
        if (currentElapsed.isAfter(nodeTime)) { // already departed
            return reasons.recordReason(ServiceReason.AlreadyDeparted(currentElapsed, path));
        }

        if (operatesWithinTime(nodeTime, currentElapsed)) {
            return valid(path, reasons);
        }
        return reasons.recordReason(ServiceReason.DoesNotOperateOnTime(currentElapsed, path));
    }

    private boolean operatesWithinTime(TramTime nodeTime, TramTime elapsedTimed) {
        TramTime earliest = nodeTime.minusMinutes(maxWaitMinutes);
        return elapsedTimed.between(earliest, nodeTime);
    }

    public ServiceReason interestedInHour(Path path, Node node, TramTime journeyClockTime, ServiceReasons reasons) {
        int hour = nodeOperations.getHour(node);

        reasons.incrementTotalChecked();

        int queryTimeHour = journeyClockTime.getHourOfDay();
        if (hour == queryTimeHour) {
            // quick win
            return valid(path, reasons);
        }

        // this only works if maxWaitMinutes<60
        int previousHour = hour - 1;
        if (previousHour==-1) {
            previousHour = 23;
        }
        if (queryTimeHour == previousHour) {
            int timeUntilNextHour = 60 - maxWaitMinutes;
            if (journeyClockTime.getMinuteOfHour() >= timeUntilNextHour) {
                return valid(path, reasons);
            }
        }

        return reasons.recordReason(ServiceReason.DoesNotOperateAtHour(journeyClockTime, path));
    }

    public ServiceReason canReachDestination(Node endNode, Path path, ServiceReasons reasons) {

        // can only safely does this if uniquely looking at tram journeys
        // TODO Build full reachability matrix??
        if (tramOnly) {
            String routeStationId = endNode.getProperty(ID).toString();
            RouteStation routeStation = stationRepository.getRouteStation(routeStationId);

            if (routeStation==null) {
                String message = "Missing routestation " + routeStationId;
                logger.warn(message);
                throw new RuntimeException(message);
            }

            if (TransportMode.isTram(routeStation)) {
                for(Station endStation : endTramStations) {
                    if (tramReachabilityRepository.stationReachable(routeStation, endStation)) {
                        return valid(path, reasons);
                    }
                }
                return reasons.recordReason(ServiceReason.StationNotReachable(path));
            }
        }

        // TODO can't exclude unless we know for sure not reachable, so include all for buses
        return valid(path, reasons);
    }

    public ServiceReason journeyDurationUnderLimit(final int totalCost, final Path path, ServiceReasons reasons) {
        if (totalCost>maxJourneyDuration) {
            return reasons.recordReason(ServiceReason.TookTooLong(queryTime.plusMinutes(totalCost), path));
        }
        return valid(path, reasons);
    }

    private ServiceReason valid(final Path path, ServiceReasons reasons) {
        if (debugEnabled) {
            return reasons.recordReason(ServiceReason.IsValid(path));
        }
        return reasons.recordReason(ServiceReason.isValid);
    }

    private ServiceReason notOnQueryDate(Path path, ServiceReasons reasons) {
        if (debugEnabled) {
            return reasons.recordReason(ServiceReason.DoesNotRunOnQueryDate(path));
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

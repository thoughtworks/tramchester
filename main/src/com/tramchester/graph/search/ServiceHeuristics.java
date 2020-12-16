package com.tramchester.graph.search;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.search.states.HowIGotHere;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TramReachabilityRepository;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceHeuristics {

    private static final Logger logger;

    static {
        logger = LoggerFactory.getLogger(ServiceHeuristics.class);
    }

    private final JourneyConstraints journeyConstraints;
    private final TramTime queryTime;
    private final TramReachabilityRepository tramReachabilityRepository;
    private final StationRepository stationRepository;
    private final NodeContentsRepository nodeOperations;
    private final int changesLimit;

    public ServiceHeuristics(StationRepository stationRepository, NodeContentsRepository nodeOperations,
                             TramReachabilityRepository tramReachabilityRepository,
                             JourneyConstraints journeyConstraints, TramTime queryTime,
                             int changesLimit) {
        this.stationRepository = stationRepository;
        this.nodeOperations = nodeOperations;
        this.tramReachabilityRepository = tramReachabilityRepository;

        this.journeyConstraints = journeyConstraints;
        this.queryTime = queryTime;
        this.changesLimit = changesLimit;
    }
    
    public ServiceReason checkServiceDate(Node node, HowIGotHere howIGotHere, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        IdFor<Service> nodeServiceId = nodeOperations.getServiceId(node);

        if (journeyConstraints.isRunning(nodeServiceId)) {
            return valid(ServiceReason.ReasonCode.ServiceDateOk, howIGotHere, reasons);
        }

        return reasons.recordReason(ServiceReason.DoesNotRunOnQueryDate(howIGotHere, nodeServiceId));
    }

    public ServiceReason checkServiceTime(HowIGotHere howIGotHere, Node node, TramTime currentClock, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        IdFor<Service> serviceId = nodeOperations.getServiceId(node);

        // prepared to wait up to max wait for start of a service...
        // TODO Push DOWN
        TramTime serviceStart = journeyConstraints.getServiceEarliest(serviceId).minusMinutes(journeyConstraints.getMaxWait());

        // BUT if arrive after service finished there is nothing to be done...
        TramTime serviceEnd = journeyConstraints.getServiceLatest(serviceId);

        if (!currentClock.between(serviceStart, serviceEnd)) {
            return reasons.recordReason(ServiceReason.ServiceNotRunningAtTime(currentClock, howIGotHere));
        }

        return valid(ServiceReason.ReasonCode.ServiceTimeOk, howIGotHere, reasons);
    }

    public ServiceReason checkNumberChanges(int currentNumChanges, HowIGotHere howIGotHere, ServiceReasons reasons) {
       reasons.incrementTotalChecked();

       if (currentNumChanges>changesLimit) {
         return reasons.recordReason(ServiceReason.TooManyChanges(howIGotHere));
       }
       return valid(ServiceReason.ReasonCode.NumChangesOK, howIGotHere, reasons);
    }

    public ServiceReason checkNumberConnections(int currentNumConnections, HowIGotHere howIGotHere, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        if (currentNumConnections>changesLimit) {
            return reasons.recordReason(ServiceReason.TooManyConnections(howIGotHere));
        }
        return valid(ServiceReason.ReasonCode.NumConnectionsOk, howIGotHere, reasons);
    }

    public ServiceReason checkTime(HowIGotHere howIGotHere, Node node, TramTime currentElapsed, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        TramTime nodeTime = nodeOperations.getTime(node);
        if (currentElapsed.isAfter(nodeTime)) { // already departed
            return reasons.recordReason(ServiceReason.AlreadyDeparted(currentElapsed, howIGotHere));
        }

        if (operatesWithinTime(nodeTime, currentElapsed)) {
            return valid(ServiceReason.ReasonCode.TimeOk, howIGotHere, reasons);
        }
        return reasons.recordReason(ServiceReason.DoesNotOperateOnTime(currentElapsed, howIGotHere));
    }

    private boolean operatesWithinTime(TramTime nodeTime, TramTime elapsedTimed) {
        int maxWait = journeyConstraints.getMaxWait();
        TramTime earliest = (nodeTime.getMinuteOfDay()>maxWait) ? nodeTime.minusMinutes(maxWait) : TramTime.of(0,0);
        return elapsedTimed.between(earliest, nodeTime);
    }

    public ServiceReason interestedInHour(HowIGotHere howIGotHere, Node node, TramTime journeyClockTime, ServiceReasons reasons) {
        int hour = nodeOperations.getHour(node);

        reasons.incrementTotalChecked();

        int queryTimeHour = journeyClockTime.getHourOfDay();
        if (hour == queryTimeHour) {
            // quick win
            return valid(ServiceReason.ReasonCode.HourOk, howIGotHere, reasons);
        }

        // this only works if maxWaitMinutes<60
        int previousHour = hour - 1;
        if (previousHour==-1) {
            previousHour = 23;
        }
        if (queryTimeHour == previousHour) {
            // TODO Breaks if max wait > 60
            int timeUntilNextHour = 60 - journeyConstraints.getMaxWait();
            if (journeyClockTime.getMinuteOfHour() >= timeUntilNextHour) {
                return valid(ServiceReason.ReasonCode.HourOk, howIGotHere, reasons);
            }
        }

        return reasons.recordReason(ServiceReason.DoesNotOperateAtHour(journeyClockTime, howIGotHere));
    }


    public ServiceReason checkStationOpen(Node node, HowIGotHere howIGotHere, ServiceReasons reasons) {
        IdFor<RouteStation> routeStationId = IdFor.getRouteStationIdFrom(node);
        RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        Station associatedStation = routeStation.getStation();

        if (journeyConstraints.isClosed(associatedStation)) {
           return reasons.recordReason(ServiceReason.StationClosed(howIGotHere, associatedStation));
        }

        return valid(ServiceReason.ReasonCode.StationOpen, howIGotHere, reasons);

    }

    public ServiceReason canReachDestination(Node endNode, HowIGotHere howIGotHere, ServiceReasons reasons) {

        // can only safely does this if uniquely looking at tram journeys
        // TODO Build full reachability matrix??
        if (journeyConstraints.getIsTramOnlyDestinations()) {
            IdFor<RouteStation> routeStationId = IdFor.getRouteStationIdFrom(endNode);
            RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

            if (routeStation==null) {
                String message = "Missing routestation " + routeStationId;
                logger.warn(message);
                throw new RuntimeException(message);
            }

            if (TransportMode.isTram(routeStation)) {
                for(Station endStation : journeyConstraints.getEndTramStations()) {
                    if (tramReachabilityRepository.stationReachable(routeStation, endStation)) {
                        return valid(ServiceReason.ReasonCode.Reachable, howIGotHere, reasons);
                    }
                }
                return reasons.recordReason(ServiceReason.StationNotReachable(howIGotHere));
            }
        }

        // TODO can't exclude unless we know for sure not reachable, so include all for buses
        return valid(ServiceReason.ReasonCode.ReachableNoCheck, howIGotHere, reasons);
    }

    public ServiceReason journeyDurationUnderLimit(final int totalCost, final HowIGotHere howIGotHere, ServiceReasons reasons) {
        if (totalCost>journeyConstraints.getMaxJourneyDuration()) {
            return reasons.recordReason(ServiceReason.TookTooLong(queryTime.plusMinutes(totalCost), howIGotHere));
        }
        return valid(ServiceReason.ReasonCode.DurationOk, howIGotHere, reasons);
    }

    private ServiceReason valid(ServiceReason.ReasonCode code, final HowIGotHere howIGotHere, ServiceReasons reasons) {
        return reasons.recordReason(ServiceReason.IsValid(code, howIGotHere));
    }

    public TramTime getQueryTime() {
        return queryTime;
    }

    public int getMaxPathLength() {
        return journeyConstraints.getMaxPathLength();
    }

}

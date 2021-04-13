package com.tramchester.graph.search;

import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.states.HowIGotHere;
import com.tramchester.repository.ReachabilityRepository;
import com.tramchester.repository.StationRepository;
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
    private final ReachabilityRepository reachabilityRepository;
    private final StationRepository stationRepository;
    private final NodeContentsRepository nodeOperations;
    private final int currentChangesLimit;

    public ServiceHeuristics(StationRepository stationRepository, NodeContentsRepository nodeOperations,
                             ReachabilityRepository reachabilityRepository,
                             JourneyConstraints journeyConstraints, TramTime queryTime,
                             int currentChangesLimit) {
        this.stationRepository = stationRepository;
        this.nodeOperations = nodeOperations;
        this.reachabilityRepository = reachabilityRepository;

        this.journeyConstraints = journeyConstraints;
        this.queryTime = queryTime;
        this.currentChangesLimit = currentChangesLimit; // NOTE: this changes from 1->num , which is set in journeyConstraints
    }
    
    public ServiceReason checkServiceDate(Node node, HowIGotHere howIGotHere, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        IdFor<Service> nodeServiceId = nodeOperations.getServiceId(node);

        if (journeyConstraints.isRunning(nodeServiceId)) {
            return valid(ServiceReason.ReasonCode.ServiceDateOk, howIGotHere, reasons);
        }

        return reasons.recordReason(ServiceReason.DoesNotRunOnQueryDate(howIGotHere, nodeServiceId));
    }

    public ServiceReason checkNumberChanges(int currentNumChanges, HowIGotHere howIGotHere, ServiceReasons reasons) {
       reasons.incrementTotalChecked();

       if (currentNumChanges> currentChangesLimit) {
         return reasons.recordReason(ServiceReason.TooManyChanges(howIGotHere));
       }
       return valid(ServiceReason.ReasonCode.NumChangesOK, howIGotHere, reasons);
    }

    public ServiceReason checkNumberWalkingConnections(int currentNumConnections, HowIGotHere howIGotHere, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        if (currentNumConnections > journeyConstraints.getMaxWalkingConnections()) {
            return reasons.recordReason(ServiceReason.TooManyWalkingConnections(howIGotHere));
        }
        return valid(ServiceReason.ReasonCode.NumWalkingConnectionsOk, howIGotHere, reasons);
    }

    public ServiceReason checkTime(HowIGotHere howIGotHere, Node node, TramTime currentElapsed, ServiceReasons reasons, int maxWait) {
        reasons.incrementTotalChecked();

        TramTime nodeTime = nodeOperations.getTime(node);
        if (currentElapsed.isAfter(nodeTime)) { // already departed
            return reasons.recordReason(ServiceReason.AlreadyDeparted(currentElapsed, howIGotHere));
        }

        if (currentElapsed.withinInterval(maxWait, nodeTime)) {
            return valid(ServiceReason.ReasonCode.TimeOk, howIGotHere, reasons);
        }
        return reasons.recordReason(ServiceReason.DoesNotOperateOnTime(currentElapsed, howIGotHere));
    }

    public ServiceReason interestedInHour(HowIGotHere howIGotHere, Node node, TramTime journeyClockTime, ServiceReasons reasons, int maxWait) {
        reasons.incrementTotalChecked();

        int hour = nodeOperations.getHour(node);

        int queryTimeHour = journeyClockTime.getHourOfDay();
        if (hour == queryTimeHour) {
            // quick win
            return valid(ServiceReason.ReasonCode.HourOk, howIGotHere, reasons);
        }

        TramTime currentHour = hour==0 ? TramTime.midnight() : TramTime.of(hour, 0);
        if (journeyClockTime.withinInterval(maxWait, currentHour)) {
            return valid(ServiceReason.ReasonCode.HourOk, howIGotHere, reasons);
        }

        return reasons.recordReason(ServiceReason.DoesNotOperateAtHour(journeyClockTime, howIGotHere));
    }


    public ServiceReason checkStationOpen(Node node, HowIGotHere howIGotHere, ServiceReasons reasons) {
        IdFor<RouteStation> routeStationId = GraphProps.getRouteStationIdFrom(node);
        RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        Station associatedStation = routeStation.getStation();

        if (journeyConstraints.isClosed(associatedStation)) {
           return reasons.recordReason(ServiceReason.StationClosed(howIGotHere, associatedStation));
        }

        return valid(ServiceReason.ReasonCode.StationOpen, howIGotHere, reasons);

    }

    public ServiceReason canReachDestination(Node endNode, HowIGotHere howIGotHere, ServiceReasons reasons) {

        IdFor<RouteStation> routeStationId = GraphProps.getRouteStationIdFrom(endNode);
        RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        if (routeStation==null) {
            String message = "Missing routestation " + routeStationId;
            logger.error(message);
            throw new RuntimeException(message);
        }

        for(Station endStation : journeyConstraints.getEndStations()) {
            // TODO Verison of below that takes the list of end stations
            if (reachabilityRepository.stationReachable(routeStation, endStation)) {
                return valid(ServiceReason.ReasonCode.Reachable, howIGotHere, reasons);
            }
        }
        return reasons.recordReason(ServiceReason.StationNotReachable(howIGotHere));

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

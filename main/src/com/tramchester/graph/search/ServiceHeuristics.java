package com.tramchester.graph.search;

import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.stateMachine.HowIGotHere;
import com.tramchester.repository.RouteInterchanges;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

public class ServiceHeuristics {

    private static final Logger logger;

    static {
        logger = LoggerFactory.getLogger(ServiceHeuristics.class);
    }

    private final JourneyConstraints journeyConstraints;
    private final TramTime actualQueryTime;
    private final StationRepository stationRepository;
    private final NodeContentsRepository nodeOperations;
    private final int currentChangesLimit;
    private final LowestCostsForRoutes lowestCostsForRoutes;
    private final RouteInterchanges routeInterchanges;

    public ServiceHeuristics(StationRepository stationRepository, RouteInterchanges routeInterchanges, NodeContentsRepository nodeOperations,
                             JourneyConstraints journeyConstraints, TramTime actualQueryTime,
                             int currentChangesLimit) {
        this.stationRepository = stationRepository;
        this.routeInterchanges = routeInterchanges;
        this.nodeOperations = nodeOperations;

        this.journeyConstraints = journeyConstraints;
        this.actualQueryTime = actualQueryTime;
        this.currentChangesLimit = currentChangesLimit;
        this.lowestCostsForRoutes = journeyConstraints.getFewestChangesCalculator();
    }
    
    public ServiceReason checkServiceDate(Node node, HowIGotHere howIGotHere, ServiceReasons reasons, TramTime visitTime) {
        reasons.incrementTotalChecked();

        IdFor<Service> nodeServiceId = nodeOperations.getServiceId(node);

        if (journeyConstraints.isRunning(nodeServiceId, visitTime)) {
            return valid(ServiceReason.ReasonCode.ServiceDateOk, howIGotHere, reasons);
        }

        return reasons.recordReason(ServiceReason.DoesNotRunOnQueryDate(howIGotHere, nodeServiceId));
    }

    public ServiceReason checkNumberChanges(int currentNumChanges, HowIGotHere howIGotHere, ServiceReasons reasons) {
       reasons.incrementTotalChecked();

       if (currentNumChanges > currentChangesLimit) {
         return reasons.recordReason(ServiceReason.TooManyChanges(howIGotHere));
       }
       return valid(ServiceReason.ReasonCode.NumChangesOK, howIGotHere, reasons);
    }

    public ServiceReason checkNumberNeighbourConnections(int currentNumberConnections, HowIGotHere howIGotHere, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        if (currentNumberConnections > journeyConstraints.getMaxNeighbourConnections()) {
            return reasons.recordReason(ServiceReason.TooManyNeighbourConnections(howIGotHere));
        }
        return valid(ServiceReason.ReasonCode.NeighbourConnectionsOk, howIGotHere, reasons);
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

    public ServiceReason interestedInHour(HowIGotHere howIGotHere, TramTime journeyClockTime,
                                          ServiceReasons reasons, int maxWait, EnumSet<GraphLabel> labels) {
        reasons.incrementTotalChecked();

        int queryTimeHour = journeyClockTime.getHourOfDay();
        //noinspection SuspiciousMethodCalls
        if (labels.contains(GraphLabel.getHourLabel(queryTimeHour))) {
            // quick win
            return valid(ServiceReason.ReasonCode.HourOk, howIGotHere, reasons);
        }

        int hour = GraphLabel.getHourFrom(labels);

        TramTime currentHour = hour==0 ? TramTime.midnight() : TramTime.of(hour, 0);
        if (journeyClockTime.withinInterval(maxWait, currentHour)) {
            return valid(ServiceReason.ReasonCode.HourOk, howIGotHere, reasons);
        }

        return reasons.recordReason(ServiceReason.DoesNotOperateAtHour(journeyClockTime, howIGotHere));
    }


    public ServiceReason checkStationOpen(Node node, HowIGotHere howIGotHere, ServiceReasons reasons) {

        IdFor<RouteStation> routeStationId = nodeOperations.getRouteStationId(node);
        RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        Station associatedStation = routeStation.getStation();

        if (journeyConstraints.isClosed(associatedStation)) {
           return reasons.recordReason(ServiceReason.StationClosed(howIGotHere, associatedStation));
        }

        return valid(ServiceReason.ReasonCode.StationOpen, howIGotHere, reasons);

    }

    public ServiceReason canReachDestination(Node endNode, int currentNumberOfChanges, HowIGotHere howIGotHere,
                                             ServiceReasons reasons, TramTime currentElapsed) {

        IdFor<RouteStation> routeStationId = nodeOperations.getRouteStationId(endNode);
        RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        if (routeStation==null) {
            String message = "Missing routestation " + routeStationId;
            logger.error(message);
            throw new RuntimeException(message);
        }

        Route currentRoute = routeStation.getRoute();

        if (journeyConstraints.isUnavailable(currentRoute, currentElapsed)) {
            return reasons.recordReason(ServiceReason.RouteNotToday(howIGotHere, currentRoute.getId()));
        }

        int fewestChanges = lowestCostsForRoutes.getFewestChanges(currentRoute);

        if (fewestChanges > currentChangesLimit) {
            return reasons.recordReason(ServiceReason.StationNotReachable(howIGotHere, ServiceReason.ReasonCode.RouteChanges));
        }

        if ((fewestChanges+currentNumberOfChanges) > currentChangesLimit) {
            return reasons.recordReason(ServiceReason.StationNotReachable(howIGotHere, ServiceReason.ReasonCode.ChangesRequired));
        }

        return valid(ServiceReason.ReasonCode.Reachable, howIGotHere, reasons);
    }

    public ServiceReason lowerCostIncludingInterchange(Node nextNode, int totalCostSoFar, LowestCostSeen bestSoFar,
                                                       HowIGotHere howIGotHere, ServiceReasons reasons) {
        IdFor<RouteStation> routeStationId = nodeOperations.getRouteStationId(nextNode);
        RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        if  (lowestCostsForRoutes.getFewestChanges(routeStation.getRoute())==0) {
            // on same route our destination
            return valid(ServiceReason.ReasonCode.ReachableSameRoute, howIGotHere, reasons);
        }
        // otherwise, a change to a different route is needed

        int costToFirstInterchange = routeInterchanges.costToInterchange(routeStation);
        //logger.info("Cost to first interchange " + costToFirstInterchange);

        if (costToFirstInterchange==Integer.MAX_VALUE) {
            // change required from current route, but no interchange is available for this station/route combination
            return reasons.recordReason(ServiceReason.ExchangeNotReachable(howIGotHere));
        }

        if (bestSoFar.everArrived()) {
            int lowestCost = bestSoFar.getLowestCost();
            if (totalCostSoFar + costToFirstInterchange > lowestCost) {
                // cost of getting to interchange, plus current total cost, is greater than existing best effort
                return reasons.recordReason(ServiceReason.LongerViaExchange(howIGotHere));
            }
        }

        return valid(ServiceReason.ReasonCode.Reachable, howIGotHere, reasons);
    }

    public ServiceReason journeyDurationUnderLimit(final int totalCost, final HowIGotHere howIGotHere, ServiceReasons reasons) {
        if (totalCost > journeyConstraints.getMaxJourneyDuration()) {
            return reasons.recordReason(ServiceReason.TookTooLong(actualQueryTime.plusMinutes(totalCost), howIGotHere));
        }
        return valid(ServiceReason.ReasonCode.DurationOk, howIGotHere, reasons);
    }

    private ServiceReason valid(ServiceReason.ReasonCode code, final HowIGotHere howIGotHere, ServiceReasons reasons) {
        return reasons.recordReason(ServiceReason.IsValid(code, howIGotHere));
    }

    public int getMaxPathLength() {
        return journeyConstraints.getMaxPathLength();
    }

}

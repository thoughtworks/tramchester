package com.tramchester.graph.search;

import com.google.common.collect.Sets;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.stateMachine.HowIGotHere;
import com.tramchester.repository.RouteInterchanges;
import com.tramchester.repository.StationRepository;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

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
    private final LowestCostsForDestRoutes lowestCostsForDestRoutes;
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
        this.lowestCostsForDestRoutes = journeyConstraints.getFewestChangesCalculator();
    }
    
    public ServiceReason checkServiceDateAndTime(Node node, HowIGotHere howIGotHere, ServiceReasons reasons,
                                                 TramTime visitTime, int maxWait) {
        reasons.incrementTotalChecked();

        IdFor<Service> nodeServiceId = nodeOperations.getServiceId(node);

        if (!journeyConstraints.isRunningOnDate(nodeServiceId, visitTime)) {
            return reasons.recordReason(ServiceReason.DoesNotRunOnQueryDate(howIGotHere, nodeServiceId));

        }

        if (!journeyConstraints.isRunningAtTime(nodeServiceId, visitTime, maxWait)) {
            return reasons.recordReason(ServiceReason.ServiceNotRunningAtTime(howIGotHere, nodeServiceId, visitTime));
        }

        return valid(ServiceReason.ReasonCode.ServiceDateOk, howIGotHere, reasons);
    }

    public ServiceReason checkNumberChanges(int currentNumChanges, HowIGotHere howIGotHere, ServiceReasons reasons) {
       reasons.incrementTotalChecked();

       if (currentNumChanges > currentChangesLimit) {
         return reasons.recordReason(ServiceReason.TooManyChanges(howIGotHere, currentNumChanges));
       }
       return valid(ServiceReason.ReasonCode.NumChangesOK, howIGotHere, reasons);
    }

    public ServiceReason checkNumberNeighbourConnections(int currentNumberConnections, HowIGotHere howIGotHere, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        if (currentNumberConnections > journeyConstraints.getMaxWalkingConnections()) {
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

    public ServiceReason checkTime(HowIGotHere howIGotHere, Node node, TramTime currentTime, ServiceReasons reasons, int maxWait) {
        reasons.incrementTotalChecked();

        TramTime nodeTime = nodeOperations.getTime(node);
        if (currentTime.isAfter(nodeTime)) { // already departed
            return reasons.recordReason(ServiceReason.AlreadyDeparted(currentTime, howIGotHere));
        }

        if (currentTime.withinInterval(maxWait, nodeTime)) {
            return valid(ServiceReason.ReasonCode.TimeOk, howIGotHere, reasons);
        }

        return reasons.recordReason(ServiceReason.DoesNotOperateOnTime(currentTime, howIGotHere));
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
        reasons.incrementTotalChecked();

        IdFor<RouteStation> routeStationId = nodeOperations.getRouteStationId(node);
        RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        Station associatedStation = routeStation.getStation();

        if (journeyConstraints.isClosed(associatedStation)) {
           return reasons.recordReason(ServiceReason.StationClosed(howIGotHere, associatedStation));
        }

        return valid(ServiceReason.ReasonCode.StationOpen, howIGotHere, reasons);

    }

    public ServiceReason checkModes(Node node, final Set<TransportMode> requestedModes, HowIGotHere howIGotHere, ServiceReasons reasons) {
        IdFor<RouteStation> routeStationId = nodeOperations.getRouteStationId(node);
        RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        if (Sets.intersection(requestedModes, routeStation.getTransportModes()).isEmpty()) {
            return reasons.recordReason(ServiceReason.TransportModeWrong(howIGotHere, routeStation));
        }
        return valid(ServiceReason.ReasonCode.TransportModeOk, howIGotHere, reasons);
    }

    public ServiceReason canReachDestination(Node endNode, int currentNumberOfChanges, HowIGotHere howIGotHere,
                                             ServiceReasons reasons, TramTime currentElapsed) {
        reasons.incrementTotalChecked();

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

        int fewestChanges = lowestCostsForDestRoutes.getFewestChanges(currentRoute);

        if (fewestChanges > currentChangesLimit) {
            return reasons.recordReason(ServiceReason.StationNotReachable(howIGotHere, ServiceReason.ReasonCode.TooManyRouteChangesRequired));
        }

        if ((fewestChanges+currentNumberOfChanges) > currentChangesLimit) {
            return reasons.recordReason(ServiceReason.StationNotReachable(howIGotHere, ServiceReason.ReasonCode.TooManyInterchangesRequired));
        }

        return valid(ServiceReason.ReasonCode.Reachable, howIGotHere, reasons);
    }

    public ServiceReason lowerCostIncludingInterchange(Node nextNode, Duration totalCostSoFar, LowestCostSeen bestSoFar,
                                                       HowIGotHere howIGotHere, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        IdFor<RouteStation> routeStationId = nodeOperations.getRouteStationId(nextNode);
        RouteStation routeStation = stationRepository.getRouteStationById(routeStationId);

        if  (lowestCostsForDestRoutes.getFewestChanges(routeStation.getRoute())==0) {
            // on same route our destination
            return valid(ServiceReason.ReasonCode.ReachableSameRoute, howIGotHere, reasons);
        }
        // otherwise, a change to a different route is needed

        Duration costToFirstInterchange = routeInterchanges.costToInterchange(routeStation);
        //logger.info("Cost to first interchange " + costToFirstInterchange);

        if (costToFirstInterchange.isNegative()) {
            // change required from current route, but no interchange is available for this station/route combination
            return reasons.recordReason(ServiceReason.InterchangeNotReachable(howIGotHere));
        }

        // TODO Useful??
        if (bestSoFar.everArrived()) {
            Duration lowestCost = bestSoFar.getLowestDuration();
            Duration costToNextInterchange = totalCostSoFar.plus(costToFirstInterchange);
            //if (costToNextInterchange.compareTo(lowestCost) > 0) {
            if (Durations.greaterThan(costToNextInterchange, lowestCost)) {
                // cost of getting to interchange, plus current total cost, is greater than existing best effort
                return reasons.recordReason(ServiceReason.LongerViaInterchange(howIGotHere));
            }
        }

        return valid(ServiceReason.ReasonCode.Reachable, howIGotHere, reasons);
    }

    public ServiceReason journeyDurationUnderLimit(final Duration totalDuration, final HowIGotHere howIGotHere, ServiceReasons reasons) {
        reasons.incrementTotalChecked();

        //if (totalDuration.compareTo(journeyConstraints.getMaxJourneyDuration()) > 0) {
        if (Durations.greaterThan(totalDuration, journeyConstraints.getMaxJourneyDuration())) {
            return reasons.recordReason(ServiceReason.TookTooLong(actualQueryTime.plus(totalDuration), howIGotHere));
        }
        return valid(ServiceReason.ReasonCode.DurationOk, howIGotHere, reasons);
    }

    private ServiceReason valid(ServiceReason.ReasonCode code, final HowIGotHere howIGotHere, ServiceReasons reasons) {
        return reasons.recordReason(ServiceReason.IsValid(code, howIGotHere));
    }

    public int getMaxPathLength() {
        return journeyConstraints.getMaxPathLength();
    }

    public ServiceReason notAlreadySeen(ImmutableJourneyState journeyState, Node nextNode, final HowIGotHere howIGotHere,
                                        ServiceReasons reasons) {
        IdFor<Station> stationId = GraphProps.getStationId(nextNode);
        if (journeyState.hasVisited(stationId)) {
            return reasons.recordReason(ServiceReason.AlreadySeenStation(stationId, howIGotHere));
        }
        return valid(ServiceReason.ReasonCode.Continue, howIGotHere, reasons);
    }


}

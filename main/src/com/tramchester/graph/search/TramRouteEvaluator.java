package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.stateMachine.HowIGotHere;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.PathEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;
import static java.lang.String.format;

public class TramRouteEvaluator implements PathEvaluator<JourneyState> {
    private static final Logger logger = LoggerFactory.getLogger(TramRouteEvaluator.class);

    private final ServiceHeuristics serviceHeuristics;
    private final NodeContentsRepository nodeContentsRepository;
    private final ProvidesNow providesNow;

    private final Set<Long> destinationNodeIds;
    private final ServiceReasons reasons;
    private final PreviousVisits previousVisits;
    private final LowestCostSeen bestResultSoFar;

    private final int maxWait;
    private final int maxInitialWait;
    private final long startNodeId;
    private final Instant begin;
    private final long timeout;

    public TramRouteEvaluator(ServiceHeuristics serviceHeuristics, Set<Long> destinationNodeIds,
                              NodeContentsRepository nodeContentsRepository, ServiceReasons reasons,
                              PreviousVisits previousVisits, LowestCostSeen bestResultSoFar, TramchesterConfig config,
                              long startNodeId, Instant begin, ProvidesNow providesNow) {
        this.serviceHeuristics = serviceHeuristics;
        this.destinationNodeIds = destinationNodeIds;
        this.nodeContentsRepository = nodeContentsRepository;
        this.reasons = reasons;
        this.previousVisits = previousVisits;
        this.bestResultSoFar = bestResultSoFar;
        maxWait = config.getMaxWait();
        maxInitialWait = config.getMaxInitialWait();
        timeout = config.getCalcTimeoutMillis();
        this.startNodeId = startNodeId;
        this.begin = begin;
        this.providesNow = providesNow;
    }

    @Override
    public Evaluation evaluate(Path path) {
        return null;
    }

    @Override
    public Evaluation evaluate(Path path, BranchState<JourneyState> state) {
        final ImmutableJourneyState journeyState = state.getState();
        final Node nextNode = path.endNode();

        final EnumSet<GraphLabel> labels = nodeContentsRepository.getLabels(nextNode);

        // NOTE: This makes a significant impact on performance, without it algo explore the same
        // path again and again for the same time in the case where it is a valid time.
        ServiceReason.ReasonCode previousResult = previousVisits.getPreviousResult(nextNode, journeyState, labels);
        final HowIGotHere howIGotHere = new HowIGotHere(path, journeyState);
        if (previousResult != ServiceReason.ReasonCode.PreviousCacheMiss) {
            final TramTime journeyClock = journeyState.getJourneyClock();
            reasons.recordReason(ServiceReason.Cached(previousResult, journeyClock, howIGotHere));
            return Evaluation.EXCLUDE_AND_PRUNE;
        } else {
            reasons.recordReason(ServiceReason.CacheMiss(howIGotHere));
        }

        final ServiceReason.ReasonCode reasonCode = doEvaluate(path, journeyState, nextNode, labels);
        final Evaluation result = decideEvaluationAction(reasonCode);

        previousVisits.recordVisitIfUseful(reasonCode, nextNode, journeyState, labels);

        return result;
    }

    public static Evaluation decideEvaluationAction(ServiceReason.ReasonCode code) {
        return switch (code) {
            case ServiceDateOk, ServiceTimeOk, NumChangesOK, NumConnectionsOk, TimeOk, HourOk, Reachable, ReachableNoCheck,
                    DurationOk, WalkOk, StationOpen, Continue, ReachableSameRoute
                    -> Evaluation.INCLUDE_AND_CONTINUE;
            case Arrived
                    -> Evaluation.INCLUDE_AND_PRUNE;
            case HigherCost, ReturnedToStart, PathTooLong, TooManyChanges, TooManyWalkingConnections, NotReachable,
                    TookTooLong, ServiceNotRunningAtTime, NotAtHour, DoesNotOperateOnTime, NotOnQueryDate, MoreChanges,
                    AlreadyDeparted, StationClosed, TooManyNeighbourConnections, TimedOut, RouteNotOnQueryDate, HigherCostViaExchange,
                    ExchangeNotReachable, TooManyRouteChangesRequired, TooManyInterchangesRequired, AlreadySeenStation
                    -> Evaluation.EXCLUDE_AND_PRUNE;
            case OnTram, OnBus, OnTrain, NotOnVehicle, CachedUNKNOWN, PreviousCacheMiss, NumWalkingConnectionsOk,
                    NeighbourConnectionsOk, OnShip, OnSubway, OnWalk, CachedNotAtHour,
                    CachedDoesNotOperateOnTime, CachedTooManyRouteChangesRequired, CachedRouteNotOnQueryDate,
                    CachedNotOnQueryDate, CachedTooManyInterchangesRequired
                    -> throw new RuntimeException("Unexpected reason-code during evaluation: " + code.name());
        };
    }

    private ServiceReason.ReasonCode doEvaluate(Path thePath, ImmutableJourneyState journeyState, Node nextNode,
                                                EnumSet<GraphLabel> nodeLabels) {

        final long nextNodeId = nextNode.getId();

        final HowIGotHere howIGotHere = new HowIGotHere(thePath, journeyState);
        final int totalCostSoFar = journeyState.getTotalCostSoFar();
        final int numberChanges = journeyState.getNumberChanges();

        if (destinationNodeIds.contains(nextNodeId)) { // We've Arrived
            return processArrivalAtDest(journeyState, howIGotHere, numberChanges);
        } else if (bestResultSoFar.everArrived()) { // Not arrived, but we have seen at least one successful route
            final int lowestCostSeen = bestResultSoFar.getLowestCost();
            if (totalCostSoFar > lowestCostSeen) {
                // already longer that current shortest, no need to continue
                reasons.recordReason(ServiceReason.HigherCost(howIGotHere));
                return ServiceReason.ReasonCode.HigherCost;
            }

            final long durationMillis = begin.until(providesNow.getInstant(), ChronoUnit.MILLIS);
            if (durationMillis > timeout) {
                Map<String, Object> allProps = nextNode.getAllProperties();
                logger.warn(format("Timed out %s ms, current cost %s, changes %s, path len %s, state: %s, labels %s, best %s",
                        durationMillis, totalCostSoFar, numberChanges, thePath.length(), howIGotHere.getTraversalStateName(),
                        nodeLabels, bestResultSoFar));
                logger.warn(format("Timed out: Props for node %s were %s", nextNodeId, allProps));
                reasons.recordReason(ServiceReason.TimedOut(howIGotHere));
                return ServiceReason.ReasonCode.TimedOut;
            }
        }

        reasons.recordStat(journeyState);

        // no journey longer than N nodes
        // TODO check length based on current transport mode??
        if (thePath.length() > serviceHeuristics.getMaxPathLength()) {
            logger.warn("Hit max path length");
            reasons.recordReason(ServiceReason.PathToLong(howIGotHere));
            return ServiceReason.ReasonCode.PathTooLong;
        }

        // number of changes?
        if (!serviceHeuristics.checkNumberChanges(journeyState.getNumberChanges(), howIGotHere, reasons).isValid()) {
            return ServiceReason.ReasonCode.TooManyChanges;
        }

        // number of walks connections, usually just 2, beginning and end
        if (!serviceHeuristics.checkNumberWalkingConnections(journeyState.getNumberWalkingConnections(), howIGotHere, reasons).isValid()) {
            return ServiceReason.ReasonCode.TooManyWalkingConnections;
        }

        // number of walks between stations aka Neighbours
        if (!serviceHeuristics.checkNumberNeighbourConnections(journeyState.getNumberNeighbourConnections(), howIGotHere, reasons).isValid()) {
            return ServiceReason.ReasonCode.TooManyNeighbourConnections;
        }

        // journey duration too long?
        if (!serviceHeuristics.journeyDurationUnderLimit(totalCostSoFar, howIGotHere, reasons).isValid()) {
            return ServiceReason.ReasonCode.TookTooLong;
        }

        // returned to the start?
        if ((thePath.length() > 1) && nextNodeId==startNodeId) {
            reasons.recordReason(ServiceReason.ReturnedToStart(howIGotHere));
            return ServiceReason.ReasonCode.ReturnedToStart;
        }

        final TramTime visitingTime = journeyState.getJourneyClock();
        final int timeToWait = journeyState.hasBegunJourney() ? maxWait : maxInitialWait;
        // --> Minute
        // check time
        if (nodeLabels.contains(GraphLabel.MINUTE)) {
            ServiceReason serviceReason = serviceHeuristics.checkTime(howIGotHere, nextNode, visitingTime, reasons, timeToWait);
            if (!serviceReason.isValid()) {
                return serviceReason.getReasonCode();
            }
        }

        // SPIKE!
        if (nodeLabels.contains(GraphLabel.STATION)) {
            if (!serviceHeuristics.notAlreadySeen(journeyState, nextNode, howIGotHere, reasons).isValid()) {
                return ServiceReason.ReasonCode.AlreadySeenStation;
            }
        }

        /////
        // these next are ordered by frequency / number of nodes of type

        // -->Hour
        // check time, just hour first
        if (nodeLabels.contains(GraphLabel.HOUR)) {
            if (!serviceHeuristics.interestedInHour(howIGotHere, visitingTime, reasons, timeToWait, nodeLabels).isValid()) {
                return ServiceReason.ReasonCode.NotAtHour;
            }
        }

        // -->Service
        final boolean isService = nodeLabels.contains(GraphLabel.SERVICE);
        if (isService) {
            if (!serviceHeuristics.checkServiceDateAndTime(nextNode, howIGotHere, reasons, visitingTime, timeToWait).isValid()) {
                return ServiceReason.ReasonCode.NotOnQueryDate;
            }
        }

        // -->Route Station
        // is reachable from here and is route operating today?
        // is the station open?
        if (nodeLabels.contains(GraphLabel.ROUTE_STATION)) {
            final ServiceReason reachDestination = serviceHeuristics.canReachDestination(nextNode, journeyState.getNumberChanges(),
                    howIGotHere, reasons, visitingTime);
            if (!reachDestination.isValid()) {
                return reachDestination.getReasonCode();
            }

            if (!serviceHeuristics.checkStationOpen(nextNode, howIGotHere, reasons).isValid()) {
                // NOTE: might still reach the closed station via a walk, which is not via the RouteStation
                return ServiceReason.ReasonCode.StationClosed;
            }

            final ServiceReason serviceReason = serviceHeuristics.lowerCostIncludingInterchange(nextNode,
                    journeyState.getTotalCostSoFar(), bestResultSoFar, howIGotHere, reasons);
            if (!serviceReason.isValid()) {
                return serviceReason.getReasonCode();
            }

        }

        // TODO is this still needed, should drop through via continue anyway?
        final Relationship inboundRelationship = thePath.lastRelationship();
        if (inboundRelationship != null) {
            // for walking routes we do want to include them all even if at same time
            if (inboundRelationship.isType(WALKS_TO)) {
                reasons.recordReason(ServiceReason.IsValid(ServiceReason.ReasonCode.WalkOk, howIGotHere));
                return ServiceReason.ReasonCode.WalkOk;
            }
        }

        reasons.recordReason(ServiceReason.Continue(howIGotHere));
        return ServiceReason.ReasonCode.Continue;
    }

    @NotNull
    private ServiceReason.ReasonCode processArrivalAtDest(ImmutableJourneyState journeyState, HowIGotHere howIGotHere, int numberChanges) {
        if (bestResultSoFar.isLower(journeyState)) {
            // a better route than seen so far
            bestResultSoFar.setLowestCost(journeyState);
            reasons.recordSuccess();
            return ServiceReason.ReasonCode.Arrived;
        } else if (numberChanges < bestResultSoFar.getLowestNumChanges()) {
            // fewer hops can be a useful option
            reasons.recordSuccess();
            return ServiceReason.ReasonCode.Arrived;
        } else {
            // found a route, but longer or more hops than current shortest
            reasons.recordReason(ServiceReason.HigherCost(howIGotHere));
            return ServiceReason.ReasonCode.HigherCost;
        }
    }

}

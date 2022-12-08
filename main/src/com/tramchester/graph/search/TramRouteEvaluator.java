package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.Durations;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.diagnostics.*;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.PathEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO_STATION;
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

    private final int maxWaitMins;
    private final int maxInitialWaitMins;
    private final long startNodeId;
    private final Instant begin;
    private final long timeout;
    private final Set<GraphLabel> requestedLabels;

    public TramRouteEvaluator(ServiceHeuristics serviceHeuristics, Set<Long> destinationNodeIds,
                              NodeContentsRepository nodeContentsRepository, ServiceReasons reasons,
                              PreviousVisits previousVisits, LowestCostSeen bestResultSoFar, TramchesterConfig config,
                              long startNodeId, Instant begin, ProvidesNow providesNow, Set<TransportMode> requestedModes, Duration maxInitialWait) {
        this.serviceHeuristics = serviceHeuristics;
        this.destinationNodeIds = destinationNodeIds;
        this.nodeContentsRepository = nodeContentsRepository;
        this.reasons = reasons;
        this.previousVisits = previousVisits;
        this.bestResultSoFar = bestResultSoFar;
        maxWaitMins = config.getMaxWait();
        timeout = config.getCalcTimeoutMillis();
        this.startNodeId = startNodeId;
        this.begin = begin;
        this.providesNow = providesNow;
        //this.requestedModes = requestedModes;
        this.requestedLabels = GraphLabel.forMode(requestedModes);
        this.maxInitialWaitMins = Math.toIntExact(maxInitialWait.toMinutes());
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
        ReasonCode previousResult = previousVisits.getPreviousResult(nextNode, journeyState, labels);
        final HowIGotHere howIGotHere = new HowIGotHere(path, journeyState);
        if (previousResult != ReasonCode.PreviousCacheMiss) {
            final TramTime journeyClock = journeyState.getJourneyClock();
            reasons.recordReason(ServiceReason.Cached(previousResult, journeyClock, howIGotHere));
            return Evaluation.EXCLUDE_AND_PRUNE;
        } else {
            reasons.recordReason(ServiceReason.CacheMiss(howIGotHere));
        }

        final ReasonCode reasonCode = doEvaluate(path, journeyState, nextNode, labels);
        final Evaluation result = reasonCode.getEvaluationAction();

        previousVisits.recordVisitIfUseful(reasonCode, nextNode, journeyState, labels);

        return result;
    }



    private ReasonCode doEvaluate(Path thePath, ImmutableJourneyState journeyState, Node nextNode,
                                  EnumSet<GraphLabel> nodeLabels) {

        final long nextNodeId = nextNode.getId();

        final HowIGotHere howIGotHere = new HowIGotHere(thePath, journeyState);
        final Duration totalCostSoFar = journeyState.getTotalDurationSoFar();
        final int numberChanges = journeyState.getNumberChanges();

        if (destinationNodeIds.contains(nextNodeId)) { // We've Arrived
            return processArrivalAtDest(journeyState, howIGotHere, numberChanges);
        } else if (bestResultSoFar.everArrived()) { // Not arrived, but we have seen at least one successful route
            final Duration lowestCostSeen = bestResultSoFar.getLowestDuration();
            //if (totalCostSoFar.compareTo(lowestCostSeen) > 0 ) {
            if (Durations.greaterThan(totalCostSoFar, lowestCostSeen)) {
                // already longer that current shortest, no need to continue
                reasons.recordReason(ServiceReason.HigherCost(howIGotHere));
                return ReasonCode.HigherCost;
            }

            final long durationMillis = begin.until(providesNow.getInstant(), ChronoUnit.MILLIS);
            if (durationMillis > timeout) {
                Map<String, Object> allProps = nextNode.getAllProperties();
                logger.warn(format("Timed out %s ms, current cost %s, changes %s, path len %s, state: %s, labels %s, best %s",
                        durationMillis, totalCostSoFar, numberChanges, thePath.length(), howIGotHere.getTraversalStateName(),
                        nodeLabels, bestResultSoFar));
                logger.warn(format("Timed out: Props for node %s were %s", nextNodeId, allProps));
                reasons.recordReason(ServiceReason.TimedOut(howIGotHere));
                return ReasonCode.TimedOut;
            }
        }

        reasons.recordStat(journeyState);

        // no journey longer than N nodes
        // TODO check length based on current transport mode??
        if (thePath.length() > serviceHeuristics.getMaxPathLength()) {
            logger.warn("Hit max path length");
            reasons.recordReason(ServiceReason.PathToLong(howIGotHere));
            return ReasonCode.PathTooLong;
        }

        // number of changes?
        if (!serviceHeuristics.checkNumberChanges(journeyState.getNumberChanges(), howIGotHere, reasons).isValid()) {
            return ReasonCode.TooManyChanges;
        }

        // number of walks connections, usually just 2, beginning and end
        if (!serviceHeuristics.checkNumberWalkingConnections(journeyState.getNumberWalkingConnections(), howIGotHere, reasons).isValid()) {
            return ReasonCode.TooManyWalkingConnections;
        }

        // number of walks between stations aka Neighbours
        if (!serviceHeuristics.checkNumberNeighbourConnections(journeyState.getNumberNeighbourConnections(), howIGotHere, reasons).isValid()) {
            return ReasonCode.TooManyNeighbourConnections;
        }

        // journey duration too long?
        if (!serviceHeuristics.journeyDurationUnderLimit(totalCostSoFar, howIGotHere, reasons).isValid()) {
            return ReasonCode.TookTooLong;
        }

        // returned to the start?
        if ((thePath.length() > 1) && nextNodeId==startNodeId) {
            reasons.recordReason(ServiceReason.ReturnedToStart(howIGotHere));
            return ReasonCode.ReturnedToStart;
        }

        final TramTime visitingTime = journeyState.getJourneyClock();
        final int timeToWait = journeyState.hasBegunJourney() ? maxWaitMins : maxInitialWaitMins;
        // --> Minute
        // check time
        if (nodeLabels.contains(GraphLabel.MINUTE)) {
            HeuristicsReason serviceReasonTripCheck = serviceHeuristics.checkNotBeenOnTripBefore(howIGotHere, nextNode, journeyState, reasons);
            if (!serviceReasonTripCheck.isValid()) {
                return serviceReasonTripCheck.getReasonCode();
            }

            HeuristicsReason serviceReasonTimeCheck = serviceHeuristics.checkTime(howIGotHere, nextNode, visitingTime, reasons, timeToWait);
            if (!serviceReasonTimeCheck.isValid()) {
                return serviceReasonTimeCheck.getReasonCode();
            }
        }

        // SPIKE!
        if (nodeLabels.contains(GraphLabel.STATION)) {
            if (!serviceHeuristics.notAlreadySeen(journeyState, nextNode, howIGotHere, reasons).isValid()) {
                return ReasonCode.AlreadySeenStation;
            }
        }

        /////
        // these next are ordered by frequency / number of nodes of type

        // -->Hour
        // check time, just hour first
        if (nodeLabels.contains(GraphLabel.HOUR)) {
//            ServiceReason forMode = serviceHeuristics.checkModes(nodeLabels, requestedLabels, howIGotHere, reasons);
//            if (!forMode.isValid()) {
//                return forMode.getReasonCode();
//            }

            if (!serviceHeuristics.interestedInHour(howIGotHere, visitingTime, reasons, timeToWait, nodeLabels).isValid()) {
                return ReasonCode.NotAtHour;
            }
        }

        // -->Service
        final boolean isService = nodeLabels.contains(GraphLabel.SERVICE);
        if (isService) {
            if (!serviceHeuristics.checkServiceDateAndTime(nextNode, howIGotHere, reasons, visitingTime, timeToWait).isValid()) {
                return ReasonCode.NotOnQueryDate;
            }
        }

        // -->Route Station
        // is reachable from here and is route operating today?
        // is the station open?
        if (nodeLabels.contains(GraphLabel.ROUTE_STATION)) {

            final HeuristicsReason forMode = serviceHeuristics.checkModes(nodeLabels, requestedLabels, howIGotHere, reasons);
            if (!forMode.isValid()) {
                return forMode.getReasonCode();
            }

            final HeuristicsReason reachDestination = serviceHeuristics.canReachDestination(nextNode, journeyState.getNumberChanges(),
                    howIGotHere, reasons, visitingTime);
            if (!reachDestination.isValid()) {
                return reachDestination.getReasonCode();
            }

            if (!serviceHeuristics.checkStationOpen(nextNode, howIGotHere, reasons).isValid()) {
                // NOTE: might still reach the closed station via a walk, which is not via the RouteStation
                return ReasonCode.StationClosed;
            }

            final HeuristicsReason serviceReason = serviceHeuristics.lowerCostIncludingInterchange(nextNode,
                    journeyState.getTotalDurationSoFar(), bestResultSoFar, howIGotHere, reasons);
            if (!serviceReason.isValid()) {
                return serviceReason.getReasonCode();
            }

        }

        // TODO is this still needed, should drop through via continue anyway?
        final Relationship inboundRelationship = thePath.lastRelationship();
        if (inboundRelationship != null) {
            // for walking routes we do want to include them all even if at same time
            if (inboundRelationship.isType(WALKS_TO_STATION)) {
                reasons.recordReason(ServiceReason.IsValid(ReasonCode.WalkOk, howIGotHere));
                return ReasonCode.WalkOk;
            }
        }

        reasons.recordReason(ServiceReason.Continue(howIGotHere));
        return ReasonCode.Continue;
    }

    @NotNull
    private ReasonCode processArrivalAtDest(ImmutableJourneyState journeyState, HowIGotHere howIGotHere, int numberChanges) {
        if (bestResultSoFar.isLower(journeyState)) {
            // a better route than seen so far
            bestResultSoFar.setLowestCost(journeyState);
            reasons.recordSuccess();
            return ReasonCode.Arrived;
        } else if (numberChanges < bestResultSoFar.getLowestNumChanges()) {
            // fewer hops can be a useful option
            reasons.recordSuccess();
            return ReasonCode.Arrived;
        } else {
            // found a route, but longer or more hops than current shortest
            reasons.recordReason(ServiceReason.HigherCost(howIGotHere));
            return ReasonCode.HigherCost;
        }
    }

}

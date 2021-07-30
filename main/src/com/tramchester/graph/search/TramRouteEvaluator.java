package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.LowestCostSeen;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.stateMachine.HowIGotHere;
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
    private final LowestCostSeen lowestCostSeen;

    private final int maxWait;
    private final int maxInitialWait;
    private final long startNodeId;
    private final Instant begin;
    private final long timeout;

    public TramRouteEvaluator(ServiceHeuristics serviceHeuristics, Set<Long> destinationNodeIds,
                              NodeContentsRepository nodeContentsRepository, ServiceReasons reasons,
                              PreviousVisits previousVisits, LowestCostSeen lowestCostSeen, TramchesterConfig config,
                              long startNodeId, Instant begin, ProvidesNow providesNow) {
        this.serviceHeuristics = serviceHeuristics;
        this.destinationNodeIds = destinationNodeIds;
        this.nodeContentsRepository = nodeContentsRepository;
        this.reasons = reasons;
        this.previousVisits = previousVisits;
        this.lowestCostSeen = lowestCostSeen;
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

        // NOTE: This makes a very(!) significant impact on performance, without it algo explore the same
        // path again and again for the same time in the case where it is a valid time.
        ServiceReason.ReasonCode previousResult = previousVisits.getPreviousResult(nextNode, journeyState, labels);
        if (previousResult != ServiceReason.ReasonCode.PreviousCacheMiss) {
            final HowIGotHere howIGotHere = new HowIGotHere(path, journeyState);
            final TramTime journeyClock = journeyState.getJourneyClock();
            reasons.recordReason(ServiceReason.Cached(journeyClock, howIGotHere));
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        final ServiceReason.ReasonCode reasonCode = doEvaluate(path, journeyState, nextNode, labels);
        final Evaluation result = decideEvaluationAction(reasonCode);

        previousVisits.recordVisitIfUseful(reasonCode, nextNode, journeyState, labels);

        return result;
    }

    public static Evaluation decideEvaluationAction(ServiceReason.ReasonCode code) {
        return switch (code) {
            case ServiceDateOk, ServiceTimeOk, NumChangesOK, NumConnectionsOk, TimeOk, HourOk, Reachable, ReachableNoCheck,
                    DurationOk, WalkOk, StationOpen, Continue -> Evaluation.INCLUDE_AND_CONTINUE;
            case Arrived -> Evaluation.INCLUDE_AND_PRUNE;
            case LongerPath, ReturnedToStart, PathTooLong, TooManyChanges, TooManyWalkingConnections, NotReachable,
                    TookTooLong, ServiceNotRunningAtTime, NotAtHour, DoesNotOperateOnTime, NotOnQueryDate,
                    AlreadyDeparted, StationClosed, TooManyNeighbourConnections, TimedOut -> Evaluation.EXCLUDE_AND_PRUNE;
            default -> throw new RuntimeException("Unexpected reasoncode during evaluation: " + code.name());
        };
    }

    private ServiceReason.ReasonCode doEvaluate(Path thePath, ImmutableJourneyState journeyState, Node nextNode, EnumSet<GraphLabel> labels) {

        final long nextNodeId = nextNode.getId();

        final HowIGotHere howIGotHere = new HowIGotHere(thePath, journeyState);
        final int totalCostSoFar = journeyState.getTotalCostSoFar();
        final int numberChanges = journeyState.getNumberChanges();

        if (destinationNodeIds.contains(nextNodeId)) {
            // we've arrived
            if (lowestCostSeen.isLower(journeyState)) {
                // a better route than seen so far
                lowestCostSeen.setLowestCost(journeyState);
                reasons.recordSuccess();
                return ServiceReason.ReasonCode.Arrived;
            } else if (numberChanges < lowestCostSeen.getLowestNumChanges()) {
                // fewer hops can be a useful option
                reasons.recordSuccess();
                return ServiceReason.ReasonCode.Arrived;
            } else {
                // found a route, but longer or more hops than current shortest
                reasons.recordReason(ServiceReason.Longer(howIGotHere));
                return ServiceReason.ReasonCode.LongerPath;
            }
        } else if (lowestCostSeen.everArrived()) { // Not arrived, but we have seen at least one successful route
            if (totalCostSoFar > lowestCostSeen.getLowestCost()) {
                // already longer that current shortest, no need to continue
                reasons.recordReason(ServiceReason.Longer(howIGotHere));
                return ServiceReason.ReasonCode.LongerPath;
            }
            final long durationMillis = begin.until(providesNow.getInstant(), ChronoUnit.MILLIS);
            if (durationMillis > timeout) {
                logger.warn(format("Timed out after %s ms, current cost %s, changes %s",
                        durationMillis, totalCostSoFar, numberChanges));
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
        if (labels.contains(GraphLabel.MINUTE)) {
            ServiceReason serviceReason = serviceHeuristics.checkTime(howIGotHere, nextNode, visitingTime, reasons, timeToWait);
            if (!serviceReason.isValid()) {
                return serviceReason.getReasonCode();
            }
        }

        /////
        // these next are ordered by frequency / number of nodes of type

        // -->Hour
        // check time, just hour first
        if (labels.contains(GraphLabel.HOUR)) {
            if (!serviceHeuristics.interestedInHour(howIGotHere, nextNode, visitingTime, reasons, timeToWait, labels).isValid()) {
                return ServiceReason.ReasonCode.NotAtHour;
            }
        }

        // -->Service
        final boolean isService = labels.contains(GraphLabel.SERVICE); //nodeTypeRepository.isService(nextNode);
        if (isService) {
            if (!serviceHeuristics.checkServiceDate(nextNode, howIGotHere, reasons).isValid()) {
                return ServiceReason.ReasonCode.NotOnQueryDate;
            }
        }

        // -->Route Station
        // is reachable from here?
        // is the station open?
        if (labels.contains(GraphLabel.ROUTE_STATION)) {
            if (!serviceHeuristics.canReachDestination(nextNode, journeyState.getNumberChanges(), howIGotHere, reasons).isValid()) {
                return ServiceReason.ReasonCode.NotReachable;
            }
            if (!serviceHeuristics.checkStationOpen(nextNode, howIGotHere, reasons).isValid()) {
                // NOTE: might still reach the closed station via a walk, which is not via the RouteStation
                return ServiceReason.ReasonCode.StationClosed;
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

}

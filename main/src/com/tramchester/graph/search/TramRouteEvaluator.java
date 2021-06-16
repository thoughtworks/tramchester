package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.caches.PreviousVisits;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.stateMachine.HowIGotHere;
import com.tramchester.graph.search.stateMachine.states.TraversalState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.PathEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;

public class TramRouteEvaluator implements PathEvaluator<JourneyState> {
    private static final Logger logger = LoggerFactory.getLogger(TramRouteEvaluator.class);

    private final Set<Long> destinationNodeIds;
    private final ServiceHeuristics serviceHeuristics;
    private final NodeContentsRepository nodeContentsRepository;
    private final ServiceReasons reasons;
    private final PreviousVisits previousVisits;

    private final int maxWait;
    private final int maxInitialWait;
    private final long startNodeId;

    private int success;

    public TramRouteEvaluator(ServiceHeuristics serviceHeuristics, Set<Long> destinationNodeIds,
                              NodeContentsRepository nodeContentsRepository, ServiceReasons reasons,
                              PreviousVisits previousVisits, TramchesterConfig config, long startNodeId) {
        this.serviceHeuristics = serviceHeuristics;
        this.destinationNodeIds = destinationNodeIds;
        this.nodeContentsRepository = nodeContentsRepository;
        this.reasons = reasons;
        this.previousVisits = previousVisits;
        maxWait = config.getMaxWait();
        maxInitialWait = config.getMaxInitialWait();
        this.startNodeId = startNodeId;

        success = 0;
    }

    @Override
    public Evaluation evaluate(Path path) {
        return null;
    }

    @Override
    public Evaluation evaluate(Path path, BranchState<JourneyState> state) {
        ImmutableJourneyState journeyState = state.getState();
        TramTime journeyClock = journeyState.getJourneyClock();
        Node nextNode = path.endNode();

        // NOTE: This makes a very(!) significant impact on performance, without it algo explore the same
        // path again and again for the same time in the case where it is a valid time.
        ServiceReason.ReasonCode previousResult = previousVisits.getPreviousResult(nextNode, journeyState);
        if (previousResult != ServiceReason.ReasonCode.PreviousCacheMiss) {
            HowIGotHere howIGotHere = new HowIGotHere(path);
            reasons.recordReason(ServiceReason.Cached(journeyClock, howIGotHere));
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        ServiceReason.ReasonCode reasonCode = doEvaluate(path, journeyState, nextNode);
        Evaluation result = decideEvaluationAction(reasonCode);
        previousVisits.recordVisitIfUseful(reasonCode, nextNode, journeyState);

        return result;
    }

    public static Evaluation decideEvaluationAction(ServiceReason.ReasonCode code) {
        return switch (code) {
            case ServiceDateOk, ServiceTimeOk, NumChangesOK, NumConnectionsOk, TimeOk, HourOk, Reachable, ReachableNoCheck,
                    DurationOk, WalkOk, StationOpen, Continue -> Evaluation.INCLUDE_AND_CONTINUE;
            case Arrived -> Evaluation.INCLUDE_AND_PRUNE;
            case LongerPath, ReturnedToStart, PathTooLong, TooManyChanges, TooManyWalkingConnections, NotReachable,
                    TookTooLong, ServiceNotRunningAtTime, NotAtHour, NotAtQueryTime, NotOnQueryDate,
                    AlreadyDeparted, StationClosed -> Evaluation.EXCLUDE_AND_PRUNE;
            default -> throw new RuntimeException("Unexpected reasoncode during evaluation: " + code.name());
        };
    }

    private ServiceReason.ReasonCode doEvaluate(Path thePath, ImmutableJourneyState journeyState, Node nextNode) {

        long nextNodeId = nextNode.getId();

        TraversalState previousTraversalState = journeyState.getTraversalState();
        HowIGotHere howIGotHere = new HowIGotHere(thePath);

        if (destinationNodeIds.contains(nextNodeId)) {
            // we've arrived
            int totalCost = previousTraversalState.getTotalCost();
            if (totalCost <= previousVisits.getLowestCost()) {
                // a better route than seen so far
                // <= equals so we include multiple options and routes in the results
                // An alternative to this would be to search over a finer grained list of times and catch alternatives
                // that way
                success = success + 1;
                previousVisits.setLowestCost(totalCost);
                reasons.recordSuccess();
                return ServiceReason.ReasonCode.Arrived;
            } else {
                // found a route, but longer than current shortest
                reasons.recordReason(ServiceReason.Longer(howIGotHere));
                return ServiceReason.ReasonCode.LongerPath;
            }
        } else if (success>0) { // Not arrived, at least one successful route
            int totalCost = previousTraversalState.getTotalCost();
            if (totalCost > previousVisits.getLowestCost()) {
                // already longer that current shortest, no need to continue
                reasons.recordReason(ServiceReason.Longer(howIGotHere));
                return ServiceReason.ReasonCode.LongerPath;
            }
        }

        reasons.recordStat(journeyState);

        // no journey longer than N nodes
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

        // journey too long?
        if (!serviceHeuristics.journeyDurationUnderLimit(previousTraversalState.getTotalCost(), howIGotHere, reasons).isValid()) {
            return ServiceReason.ReasonCode.TookTooLong;
        }

        // returned to the start?
        if (journeyState.hasBegunJourney() && nextNodeId==startNodeId) {
            reasons.recordReason(ServiceReason.ReturnedToStart(howIGotHere));
            return ServiceReason.ReasonCode.ReturnedToStart;
        }

        // these next are ordered by frequency / number of nodes of type
        EnumSet<GraphLabel> labels = nodeContentsRepository.getLabels(nextNode);

        TramTime visitingTime = journeyState.getJourneyClock();
        int timeToWait = journeyState.hasBegunJourney() ? maxWait : maxInitialWait;
        // --> Minute
        // check time
        if (labels.contains(GraphLabel.MINUTE)) {
            ServiceReason serviceReason = serviceHeuristics.checkTime(howIGotHere, nextNode, visitingTime, reasons, timeToWait);
            if (!serviceReason.isValid()) {
                return serviceReason.getReasonCode(); // valid, or not at time
            }
        }

        // -->Hour
        // check time, just hour first
        if (labels.contains(GraphLabel.HOUR)) {
            if (!serviceHeuristics.interestedInHour(howIGotHere, nextNode, visitingTime, reasons, timeToWait).isValid()) {
                return ServiceReason.ReasonCode.NotAtHour;
            }
        }

        // -->Service
        boolean isService = labels.contains(GraphLabel.SERVICE); //nodeTypeRepository.isService(nextNode);
        if (isService) {
            if (!serviceHeuristics.checkServiceDate(nextNode, howIGotHere, reasons).isValid()) {
                return ServiceReason.ReasonCode.NotOnQueryDate;
            }
        }

        // -->Route Station
        // is even reachable from here? is the station open?
        if (labels.contains(GraphLabel.ROUTE_STATION)) {
            if (!serviceHeuristics.canReachDestination(nextNode, journeyState.getNumberChanges(), howIGotHere, reasons).isValid()) {
                return ServiceReason.ReasonCode.NotReachable;
            }
            if (!serviceHeuristics.checkStationOpen(nextNode, howIGotHere, reasons).isValid()) {
                return ServiceReason.ReasonCode.StationClosed;
            }
        }

        // TOOD is this still needed, should drop through via continue anyway?
        Relationship inboundRelationship = thePath.lastRelationship();
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

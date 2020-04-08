package com.tramchester.graph.search;

import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.*;
import com.tramchester.graph.states.TraversalState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.PathEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;

public class TramRouteEvaluator implements PathEvaluator<JourneyState> {
    private static final Logger logger = LoggerFactory.getLogger(TramRouteEvaluator.class);

    private final long destinationNodeId;
    private final ServiceHeuristics serviceHeuristics;
    private final CachedNodeOperations nodeOperations;
    private final ServiceReasons reasons;
    private int success;
    private int currentLowestCost;

    private final Map<Long, TramTime> previousSuccessfulVisit;

    public TramRouteEvaluator(ServiceHeuristics serviceHeuristics, CachedNodeOperations nodeOperations, long destinationNodeId,
                              ServiceReasons reasons) {
        this.serviceHeuristics = serviceHeuristics;
        this.nodeOperations = nodeOperations;
        this.destinationNodeId = destinationNodeId;
        this.reasons = reasons;
        success = 0;
        previousSuccessfulVisit = new HashMap<>();
        currentLowestCost = Integer.MAX_VALUE;
    }

    public void dispose() {
        previousSuccessfulVisit.clear();
    }

    @Override
    public Evaluation evaluate(Path path) {
        return null;
    }

    @Override
    public Evaluation evaluate(Path path, BranchState<JourneyState> state) {
        ImmutableJourneyState journeyState = state.getState();
        TramTime journeyClock = journeyState.getJourneyClock();

        Node endNode = path.endNode();
        long nodeId = endNode.getId();

        if (previousSuccessfulVisit.containsKey(nodeId)) {
            // can *only* safely exclude previous nodes if there is only one outbound path

            TramTime previousVisitTime = previousSuccessfulVisit.get(nodeId);
            if (nodeOperations.isTime(nodeId)) {
                // no way to get different response for same service/minute - boarding time has to be same
                // since time nodes encode a specific time, so the previous time *must* match for this node id
                reasons.recordReason(ServiceReason.Cached(previousVisitTime, path));
                return Evaluation.EXCLUDE_AND_PRUNE;
            }

            // NOTE: We only cache previous for certian node types
            if (nodeOperations.isHour(nodeId) && previousVisitTime.equals(journeyClock)) {
                reasons.recordReason(ServiceReason.Cached(previousVisitTime, path));
                return Evaluation.EXCLUDE_AND_PRUNE; // been here before at exact same time, so no need to continue
            }
        }

        Evaluation result = doEvaluate(path, journeyState, endNode, nodeId);

        if (result.continues() && (nodeOperations.isTime(nodeId) || nodeOperations.isHour(nodeId))) {
                previousSuccessfulVisit.put(nodeId, journeyClock);
        }
        return result;
    }

    private Evaluation doEvaluate(Path path, ImmutableJourneyState journeyState, Node endNode, long endNodeId) {

        TraversalState traversalState = journeyState.getTraversalState();
        if (endNodeId==destinationNodeId) {
            // we've arrived
            int totalCost = traversalState.getTotalCost();
            if (totalCost <= currentLowestCost) {
                // a better route than seen so far
                // <= equals so we include multiple options and routes in the results
                // An alternative to this would be to search over a finer grained list of times and catch alternatives
                // that way
                success = success + 1;
                currentLowestCost = totalCost;
                reasons.recordSuccess();
                return Evaluation.INCLUDE_AND_PRUNE;
            } else {
                // found a route, but longer than current shortest
                reasons.recordReason(ServiceReason.Longer(path));
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        } else if (success>0) {
            // Not arrived, but we do have at least one successful route to our destination
            int totalCost = traversalState.getTotalCost();
            if (totalCost>currentLowestCost) {
                // already longer that current shortest, no need to continue
                reasons.recordReason(ServiceReason.Longer(path));
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        reasons.record(journeyState);

        // no journey longer than N nodes
        if (path.length()>serviceHeuristics.getMaxPathLength()) {
            logger.warn("Hit max path length");
            reasons.recordReason(ServiceReason.PathToLong(path));
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        // is even reachable from here?
        if (nodeOperations.isRouteStation(endNodeId)) {
            // Note: journeyState.onTram() not true for all tram journeys as we might just be boarding....
            if (!serviceHeuristics.canReachDestination(endNode, path).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        // is the service running today
        boolean isService = nodeOperations.isService(endNodeId);
        if (isService) {
            if (!serviceHeuristics.checkServiceDate(endNode, path).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        Relationship inboundRelationship = path.lastRelationship();

        if (inboundRelationship != null) {
            // for walking routes we do want to include them all even if at same time
            if (inboundRelationship.isType(WALKS_TO)) {
                return Evaluation.INCLUDE_AND_CONTINUE;
            }
        }

        TramTime visitingTime = journeyState.getJourneyClock();

        // journey too long?
        if (!serviceHeuristics.journeyDurationUnderLimit(traversalState.getTotalCost(), path).isValid()) {
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        // service available to catch?
        if (isService) {
            if (!serviceHeuristics.checkServiceTime(path, endNode, visitingTime).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        // check time, just hour first
        if (nodeOperations.isHour(endNodeId)) {
            int hour = nodeOperations.getHour(endNode);
            if (!serviceHeuristics.interestedInHour(path, hour, visitingTime).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        // check time
        if (nodeOperations.isTime(endNodeId)) {
            if (!serviceHeuristics.checkTime(path, endNode, visitingTime).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        return Evaluation.INCLUDE_AND_CONTINUE;
    }

}

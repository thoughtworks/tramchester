package com.tramchester.graph;

import com.tramchester.domain.TramTime;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.PathEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;

public class TramRouteEvaluator implements PathEvaluator<JourneyState> {
    private static final Logger logger = LoggerFactory.getLogger(TramRouteEvaluator.class);

    private final int maxPathLength = 400; // path length limit, includes *all* edges

    private final long destinationNodeId;
    private final ServiceHeuristics serviceHeuristics;
    private final CachedNodeOperations nodeOperations;
    private int success;
    private int currentLowestCost;

    private final Map<Long, Pair<TramTime, Evaluation>> previousSuccessfulVisit;

    public TramRouteEvaluator(ServiceHeuristics serviceHeuristics, CachedNodeOperations nodeOperations, long destinationNodeId) {
        this.serviceHeuristics = serviceHeuristics;
        this.nodeOperations = nodeOperations;
        this.destinationNodeId = destinationNodeId;
        success = 0;
        previousSuccessfulVisit = new HashMap<>();
        currentLowestCost = Integer.MAX_VALUE;
    }

    @Override
    public Evaluation evaluate(Path path) {
        return null;
    }

    @Override
    public Evaluation evaluate(Path path, BranchState<JourneyState> state) {
        JourneyState journeyState = state.getState();
        TramTime journeyClock = journeyState.getJourneyClock();

        Node endNode = path.endNode();
        long nodeId = endNode.getId();

        if (previousSuccessfulVisit.containsKey(nodeId)) {

            if (nodeOperations.isTime(endNode)) {
                // no way to get different response for same service/minute - boarding time has to be same
                return Evaluation.EXCLUDE_AND_PRUNE;
            }

            Pair<TramTime, Evaluation> previous = previousSuccessfulVisit.get(nodeId);
            TramTime previousVisitTime = previous.getLeft();

            if (previousVisitTime.equals(journeyClock)) {
                return Evaluation.EXCLUDE_AND_PRUNE; // been here before at exact same time, so no need to continue
            }

//            if (!serviceHeuristics.overMaxWait(journeyClock, previousVisitTime, path).isValid()) {
//                // over max wait time later, so no need to consider
//                return Evaluation.EXCLUDE_AND_PRUNE;
//            }

        }

        Evaluation result = doEvaluate(path, journeyState, endNode, nodeId);
        if (result.continues()) {
            previousSuccessfulVisit.put(nodeId, Pair.of(journeyClock, result));
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Stopped at len: " + path.length());
            }
        }

        return result;
    }

    private Evaluation doEvaluate(Path path, JourneyState journeyState, Node endNode, long endNodeId) {

        // TODO RISK this won't always surface fatest paths?
//        if (success>=RouteCalculator.MAX_NUM_GRAPH_PATHS) {
//            return Evaluation.EXCLUDE_AND_PRUNE;
//        }

        if (endNodeId==destinationNodeId) {
            // we've arrived
            int totalCost = journeyState.getTraversalState().getTotalCost();
            if (totalCost < currentLowestCost) {
                success = success + 1;
                currentLowestCost = totalCost;
                return Evaluation.INCLUDE_AND_PRUNE;
            } else {
                // found a route, but longer than current shortest
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        } else if (success>0) {
            // already longer that current shortest, no need to continue
            int totalCost = journeyState.getTraversalState().getTotalCost();
            if (totalCost>currentLowestCost) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        // no journey longer than N stages
        if (path.length()>maxPathLength) {
            logger.warn("Hit max path length");
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        // is the service running today
        boolean isService = nodeOperations.isService(endNode);
        if (isService) {
            if (!serviceHeuristics.checkServiceDate(endNode, path).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        // is even reachable from here?
        if (nodeOperations.isRouteStation(endNode)) {
            if (!serviceHeuristics.canReachDestination(endNode, path).isValid()) {
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
        if (!serviceHeuristics.journeyDurationUnderLimit(visitingTime, path).isValid()) {
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        // service available to catch?
        if (isService) {
            if (!serviceHeuristics.checkServiceTime(path, endNode, visitingTime).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        // check time, just hour first
        if (nodeOperations.isHour(endNode)) {
            int hour = nodeOperations.getHour(endNode);
            if (!serviceHeuristics.interestedInHour(path, hour, visitingTime).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        // check time
        if (nodeOperations.isTime(endNode)) {
            if (!serviceHeuristics.checkTime(path, endNode, visitingTime).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        return Evaluation.INCLUDE_AND_CONTINUE;
    }

}

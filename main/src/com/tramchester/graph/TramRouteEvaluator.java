package com.tramchester.graph;

import com.tramchester.domain.TramTime;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.PathEvaluator;

import java.time.LocalTime;
import java.util.*;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;

public class TramRouteEvaluator implements PathEvaluator<JourneyState> {
    private final Map<Arrow, Set<TramTime>> visited;
    private final int maxPathLength = 400; // path length limit, includes *all* edges

    private final long destinationNodeId;
    private final ServiceHeuristics serviceHeuristics;
    private final CachedNodeOperations nodeOperations;

    public TramRouteEvaluator(ServiceHeuristics serviceHeuristics, CachedNodeOperations nodeOperations, long destinationNodeId) {
        this.serviceHeuristics = serviceHeuristics;
        this.nodeOperations = nodeOperations;
        this.destinationNodeId = destinationNodeId;
        visited = new HashMap<>();
    }

    @Override
    public Evaluation evaluate(Path path) {
        return null;
    }

    @Override
    public Evaluation evaluate(Path path, BranchState<JourneyState> state) {

//        if (success>=pathLimit) {
//            return Evaluation.EXCLUDE_AND_PRUNE;
//        }

        Node endNode = path.endNode();
        long endNodeId = endNode.getId();

        if (endNodeId==destinationNodeId) {
            //success = success + 1;
            return Evaluation.INCLUDE_AND_PRUNE;
        }

        // no journey longer than N stages
        if (path.length()>maxPathLength) {
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        // is the service running today
        if (nodeOperations.isService(endNode)) {
            if (!serviceHeuristics.checkServiceDate(endNode, path).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        if (nodeOperations.isRouteStation(endNode)) {
            if (!serviceHeuristics.canReachDestination(endNode, path).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        Relationship inboundRelationship = path.lastRelationship();

        if (inboundRelationship != null) {
//            // for walking routes we do want to include them all even if at same time
            if (inboundRelationship.isType(WALKS_TO)) {
                return Evaluation.INCLUDE_AND_CONTINUE;
            }

            // if already tried this node at this time from this edge, don't expect a differing outcome so exclude
//            Arrow arrow = new Arrow(inboundRelationship.getId(), endNodeId);
//            if (visited.containsKey(arrow)) {
//                if (visited.get(arrow).contains(visitingTime)) {
//                    return Evaluation.EXCLUDE_AND_PRUNE;
//                }
//            } else {
//                visited.put(arrow, new HashSet<>());
//            }
//            visited.get(arrow).add(visitingTime);
        }

        JourneyState journeyState = state.getState();
        LocalTime currentElapsed = journeyState.getJourneyClock();
        TramTime visitingTime = TramTime.of(currentElapsed);

        // journey too long?
        if (serviceHeuristics.journeyTookTooLong(visitingTime)) {
//                TramTime.diffenceAsMinutes( TramTime.of(queryTime), visitingTime)>maxJourneyMins) {
            return Evaluation.EXCLUDE_AND_PRUNE; // TODO EXCLUDE??
        }


        // service available to catch?
        if (nodeOperations.isService(endNode)) {
            if (!serviceHeuristics.checkServiceTime(path, endNode, currentElapsed).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        // check time, just hour first
        if (nodeOperations.isHour(endNode)) {
            int hour = nodeOperations.getHour(endNode);
            if (!serviceHeuristics.interestedInHour(path, hour, currentElapsed).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        // check time
        if (nodeOperations.isTime(endNode)) {
            if (!serviceHeuristics.checkTime(path, endNode, currentElapsed).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        return Evaluation.INCLUDE_AND_CONTINUE;
    }


    private class Arrow {
        private final long relationshipId;
        private final long endNodeId;

        Arrow(long relationshipId, long endNodeId) {
            this.relationshipId = relationshipId;
            this.endNodeId = endNodeId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Arrow arrow = (Arrow) o;
            return relationshipId == arrow.relationshipId &&
                    endNodeId == arrow.endNodeId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(relationshipId, endNodeId);
        }
    }

}

package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.NodeTypeRepository;
import com.tramchester.graph.PreviousSuccessfulVisits;
import com.tramchester.graph.search.states.TraversalState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.PathEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.WALKS_TO;

public class TramRouteEvaluator implements PathEvaluator<JourneyState> {
    private static final Logger logger = LoggerFactory.getLogger(TramRouteEvaluator.class);

    private final Set<Long> destinationNodeIds;
    private final ServiceHeuristics serviceHeuristics;
    private final NodeTypeRepository nodeTypeRepository;
    private final ServiceReasons reasons;
    private final PreviousSuccessfulVisits previousSuccessfulVisit;
    private int success;
    private int currentLowestCost;
    private final Set<Long> busStationNodes;
    private final boolean bus;

    public TramRouteEvaluator(ServiceHeuristics serviceHeuristics, Set<Long> destinationNodeIds,
                              NodeTypeRepository nodeTypeRepository, ServiceReasons reasons, PreviousSuccessfulVisits previousSuccessfulVisit, TramchesterConfig config) {
        this.serviceHeuristics = serviceHeuristics;
        this.destinationNodeIds = destinationNodeIds;
        this.nodeTypeRepository = nodeTypeRepository;
        this.reasons = reasons;
        this.previousSuccessfulVisit = previousSuccessfulVisit;
        bus = config.getBus();
        success = 0;
        currentLowestCost = Integer.MAX_VALUE;
        busStationNodes = new HashSet<>();
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
        long endNodeId = endNode.getId();

        if (previousSuccessfulVisit.hasUsableResult(endNode, journeyClock)) {
            reasons.recordReason(ServiceReason.Cached(journeyClock, path));
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        Evaluation result = doEvaluate(path, journeyState, endNode, endNodeId);

        previousSuccessfulVisit.recordVisitIfUseful(result, endNode, journeyClock);

        return result;
    }

    private Evaluation doEvaluate(Path path, ImmutableJourneyState journeyState, Node endNode, long endNodeId) {

        TraversalState traversalState = journeyState.getTraversalState();
        if (destinationNodeIds.contains(endNodeId)) {
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

        if (bus) {
            if (nodeTypeRepository.isBusStation(endNode)) {
                if (busStationNodes.contains(endNodeId)) {
                    reasons.recordReason(ServiceReason.SeenBusStationBefore(path));
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }
                busStationNodes.add(endNodeId);
            }
        }

        // no journey longer than N nodes
        if (path.length()>serviceHeuristics.getMaxPathLength()) {
            logger.warn("Hit max path length");
            reasons.recordReason(ServiceReason.PathToLong(path));
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        if (!serviceHeuristics.checkNumberChanges(journeyState.getNumberChanges(), path, reasons).isValid()) {
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        // is even reachable from here?
        if (nodeTypeRepository.isRouteStation(endNode)) {
            if (!serviceHeuristics.canReachDestination(endNode, path, reasons).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        // is the service running today
        boolean isService = nodeTypeRepository.isService(endNode);
        if (isService) {
            if (!serviceHeuristics.checkServiceDate(endNode, path, reasons).isValid()) {
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
        if (!serviceHeuristics.journeyDurationUnderLimit(traversalState.getTotalCost(), path, reasons).isValid()) {
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        // service available to catch?
        if (isService) {
            if (!serviceHeuristics.checkServiceTime(path, endNode, visitingTime, reasons).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        // check time, just hour first
        if (nodeTypeRepository.isHour(endNode)) {
            if (!serviceHeuristics.interestedInHour(path, endNode, visitingTime, reasons).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        // check time
        if (nodeTypeRepository.isTime(endNode)) {
            if (!serviceHeuristics.checkTime(path, endNode, visitingTime, reasons).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        return Evaluation.INCLUDE_AND_CONTINUE;
    }

}

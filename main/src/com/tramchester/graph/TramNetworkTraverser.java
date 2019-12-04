package com.tramchester.graph;

import com.tramchester.graph.states.NotStartedState;
import com.tramchester.graph.states.TraversalState;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.BranchOrderingPolicies;
import org.neo4j.graphdb.traversal.BranchState;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static org.neo4j.graphdb.traversal.Uniqueness.NONE;

public class TramNetworkTraverser implements PathExpander<JourneyState> {
    private static final Logger logger = LoggerFactory.getLogger(TramNetworkTraverser.class);

    private final ServiceHeuristics serviceHeuristics;
    private final CachedNodeOperations nodeOperations;
    private final LocalTime queryTime;
    private final long destinationNodeId;
    private final String endStationId;

    public TramNetworkTraverser(ServiceHeuristics serviceHeuristics,
                                CachedNodeOperations nodeOperations, LocalTime queryTime, Node destinationNode, String endStationId) {
        this.serviceHeuristics = serviceHeuristics;
        this.nodeOperations = nodeOperations;
        this.queryTime = queryTime;
        this.destinationNodeId = destinationNode.getId();
        this.endStationId = endStationId;
    }

    public Stream<WeightedPath> findPaths(Node startNode) {

        // TODO Move this
        TramRouteEvaluator tramRouteEvaluator = new TramRouteEvaluator(serviceHeuristics, nodeOperations,
                destinationNodeId);
        NotStartedState traversalState = new NotStartedState(nodeOperations, destinationNodeId, endStationId);

        logger.info("Begin traversal");

        Traverser traverser = new MonoDirectionalTraversalDescription().
                relationships(TRAM_GOES_TO, Direction.OUTGOING).
                relationships(BOARD, Direction.OUTGOING).
                relationships(DEPART, Direction.OUTGOING).
                relationships(INTERCHANGE_BOARD, Direction.OUTGOING).
                relationships(INTERCHANGE_DEPART, Direction.OUTGOING).
                relationships(WALKS_TO, Direction.OUTGOING).
                relationships(ENTER_PLATFORM, Direction.OUTGOING).
                relationships(LEAVE_PLATFORM, Direction.OUTGOING).
                relationships(TO_SERVICE, Direction.OUTGOING).
                relationships(TO_HOUR, Direction.OUTGOING).
                relationships(TO_MINUTE, Direction.OUTGOING).
                expand(this, JourneyState.initialState(queryTime, traversalState)).
                evaluator(tramRouteEvaluator).
                uniqueness(NONE).
                order(BranchOrderingPolicies.PREORDER_BREADTH_FIRST). //DEPTH FIRST causes visiting all stages??
                traverse(startNode);

        ResourceIterator<Path> iterator = traverser.iterator();

        logger.info("Return traversal stream");
        return iterator.stream().filter(path -> path.endNode().getId()==destinationNodeId)
                .map(this::calculateWeight);

    }

    private WeightedPath calculateWeight(Path path) {
        int result = getTotalCost(path);
        return new WeightedPathImpl(result, path);
    }

    private int getTotalCost(Path path) {
        int result = 0;
        for (Relationship relat: path.relationships()) {
            result = result + nodeOperations.getCost(relat);
        }
        return result;
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<JourneyState> graphState) {

        JourneyState currentState = graphState.getState();
        TraversalState traversalState = currentState.getTraversalState();
        Node endNode = path.endNode();

        if (path.lastRelationship()!=null) {
            int cost = nodeOperations.getCost(path.lastRelationship());
            if (cost>0) {
                currentState.updateJourneyClock(getTotalCost(path));
            }
        }

        Label firstLabel = endNode.getLabels().iterator().next();
        TransportGraphBuilder.Labels nodeLabel = TransportGraphBuilder.Labels.valueOf(firstLabel.toString());

        JourneyState stateForChildren = JourneyState.fromPrevious(currentState);
        TraversalState newTraversalState = traversalState.nextState(path, nodeLabel, endNode, stateForChildren);

        stateForChildren.updateTraversalState(newTraversalState);
        graphState.setState(stateForChildren);

//        logger.info(stateForChildren.toString());

        Iterable<Relationship> relationships = newTraversalState.getRelationships();

        return relationships;
    }

    @Override
    public PathExpander<JourneyState> reverse() {
        return null;
    }


}

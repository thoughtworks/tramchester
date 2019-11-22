package com.tramchester.graph;

import com.tramchester.graph.states.NotStartedState;
import com.tramchester.graph.states.TraversalState;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.traversal.Uniqueness.NONE;

public class TramNetworkTraverser implements PathExpander<JourneyState> {
    private static final Logger logger = LoggerFactory.getLogger(TramNetworkTraverser.class);

    private final ServiceHeuristics serviceHeuristics;
    private final CachedNodeOperations nodeOperations;
    private final LocalTime queryTime;
    private final long destinationNodeId;
    private final String endStationId;

    private final Map<JourneyState, List<Long>> visited;

    public TramNetworkTraverser(ServiceHeuristics serviceHeuristics,
                                CachedNodeOperations nodeOperations, LocalTime queryTime, Node destinationNode, String endStationId) {
        this.serviceHeuristics = serviceHeuristics;
        this.nodeOperations = nodeOperations;
        this.queryTime = queryTime;
        this.destinationNodeId = destinationNode.getId();
        this.endStationId = endStationId;

        visited = new HashMap<>();
    }

    public Stream<WeightedPath> findPaths(Node startNode) {

        // TODO Move this
        TramRouteEvaluator tramRouteEvaluator = new TramRouteEvaluator(serviceHeuristics, nodeOperations,
                destinationNodeId);

        NotStartedState traversalState = new NotStartedState(nodeOperations, destinationNodeId, endStationId);

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
                order(BranchOrderingPolicies.PREORDER_BREADTH_FIRST). //DEPTH FIRST causes visiting all stages
                traverse(startNode);

        ResourceIterator<Path> iterator = traverser.iterator();

        return iterator.stream().filter(path -> path.endNode().getId()==destinationNodeId)
                .map(path -> calculateWeight(path));


//        // TODO uniqueness && size limit?
//        List<WeightedPath> results = new ArrayList<>();
//        while (iterator.hasNext()) {
//            Path path = iterator.next();
//            if (path.endNode().getId()==destinationNodeId) {
//                results.add(calculateWeight(path));
//            }
////            if (results.size()>=MAX_NUM_GRAPH_PATHS) {
////                break;
////            }
//        }
//
//        results.sort(Comparator.comparingDouble(WeightedPath::weight));
//        logger.info(format("Found %s paths from node %s to %s", results.size(), startNode.getId(), destinationNodeId));
//
//        return results;
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

        long endNodeId = endNode.getId();
        if (haveVisited(currentState, endNodeId)) {
            return Collections.EMPTY_LIST;
        }
        recordVisit(currentState, endNodeId);

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

        return newTraversalState.getRelationships();
    }

    private boolean haveVisited(JourneyState currentState, long nodeId) {
        if (visited.containsKey(currentState)) {
            visited.get(currentState).contains(nodeId);
        }
        return false;
    }

//        int cost;
//        JourneyState currentState = graphState.getState();
//        if (path.lastRelationship()!=null) {
//            cost = nodeOperations.getCost(path.lastRelationship());
//            if (cost>0) {
//                // only updates state for children, not for this node
////                JourneyState stateForChildren = new JourneyState(state.getState(), cost);
//                JourneyState stateForChildren = JourneyState.fromPrevious(currentState).updateJourneyClock(getTotalCost(path));
//                graphState.setState(stateForChildren);
//            }
//        }

//        try {
//            switch (nodeLabel) {
////                case QUERY_NODE:
////                    return costOrdered(outboundRelationships);
//                case STATION:
////                    if (endNode.getId() == destinationNodeId) {
////                        return new LinkedList<>();
////                    }
////                    return outboundRelationships;
//                case MINUTE:
//                    //state.setState(updateStateToNodeTime(endNode));
//                    LocalTime time = nodeOperations.getTime(endNode);
//                    String tripId = nodeOperations.getTrip(endNode);
//                    graphState.setState(JourneyState.fromPrevious(currentState).recordTramDetails(time, getTotalCost(path), tripId));
//                    // TODO TRIP ID check?
//                    return outboundRelationships;
//                case PLATFORM:
//                    // remove trip id from state, we are not on a tram
////                    LocalTime newTime = currentState.getTime().plusMinutes(cost);
////                    JourneyState newStateForChildren = new JourneyState(newTime, "");
//                    if (TransportRelationshipTypes.isDeparting(path.lastRelationship().getType())) {
//                        JourneyState newStateForChildren = JourneyState.fromPrevious(currentState).leaveTram(getTotalCost(path));
//                        graphState.setState(newStateForChildren);
//                    }
//                    return fromPlatformOrderedDeparts(path.lastRelationship(), outboundRelationships);
//                case SERVICE:
//                    return hourOrdered(outboundRelationships);
//                case HOUR:
//                    return timeOrdered(outboundRelationships);
//                case ROUTE_STATION:
//                    if (TransportRelationshipTypes.isBoarding(path.lastRelationship().getType())) {
//                        JourneyState newState = JourneyState.fromPrevious(currentState).boardTram();
//                        graphState.setState(newState);
//                    }
//                    return fromRSfilteredByTripAndDepart(currentState, outboundRelationships);
//                default:
//                    throw new RuntimeException("Unexpected node type " + nodeLabel.name());
//            }
//        }
//        catch(TramchesterException exception) {
//            logger.error("Unable to process node", exception);
//            throw new RuntimeException(exception);
//        }

    private void recordVisit(JourneyState currentState, long nodeId) {
        if (!visited.containsKey(currentState)) {
            visited.put(currentState, new ArrayList<>());
        }
        visited.get(currentState).add(nodeId);

    }

//    private Iterable<Relationship> fromRSfilteredByTripAndDepart(JourneyState journeyState,
//                                                                 Iterable<Relationship> outboundRelationships) {
//        LinkedList<Relationship> results = new LinkedList<>();
//
//        if (journeyState.isOnTram()) {
//            // If we have trip then just "arrived" on a tram
//            String tripId = journeyState.getTripId();
//            for (Relationship outboundRelationship : outboundRelationships) {
//                if (outboundRelationship.isType(TO_SERVICE)) {
//                    // only follow outbound if trip id matches inbound tram
//                    String trips = nodeOperations.getTrips(outboundRelationship);
//                    if (trips.contains(tripId)) {
//                        results.add(outboundRelationship);
//                    }
//                } else if (TransportRelationshipTypes.isForPlanning(outboundRelationship.getType())){
//                    // departing current tram
//                    if (serviceHeuristics.toEndStation(outboundRelationship)) {
//                        return Collections.singleton(outboundRelationship);
//                    }
//                    results.addLast(outboundRelationship);
//                }
//            }
//        } else {
//            // No trip id means we have just boarded
//            for (Relationship outboundRelationship : outboundRelationships) {
//                if (outboundRelationship.isType(TO_SERVICE)) {
//                    String routeId = nodeOperations.getRoute(outboundRelationship);
//                    if (serviceHeuristics.matchesRoute(routeId)) {
//                        results.addFirst(outboundRelationship);
//                    } else {
//                        results.addLast(outboundRelationship);
//                    }
//                } else if (TransportRelationshipTypes.isForPlanning(outboundRelationship.getType())){
//                    // ONLY depart again if at actual destination node
//                    if (serviceHeuristics.toEndStation(outboundRelationship)) {
//                        return Collections.singleton(outboundRelationship);
//                    }
//                }
//            }
//        }
//        return results;
//    }

//    private JourneyState updateStateToNodeTime(Node endNode) {
//        LocalTime time = nodeOperations.getTime(endNode);
//        String tripId = endNode.getProperty(GraphStaticKeys.TRIP_ID).toString();
//        return new JourneyState(time, tripId);
//    }
//
//    private Iterable<Relationship> fromPlatformOrderedDeparts(Relationship inbound,
//                                                              Iterable<Relationship> outboundRelationships) {
//        LinkedList<Relationship> result = new LinkedList<>();
//
//        for (Relationship outboundRelationship : outboundRelationships) {
//            if (outboundRelationship.isType(LEAVE_PLATFORM)) {
//                if (serviceHeuristics.toEndStation(outboundRelationship)) {
//                    // destination
//                    return Collections.singleton(outboundRelationship);
//                }
//                //else
//                if (!inbound.isType(ENTER_PLATFORM)) {
//                    // dont immediately enter then leave a platform
//                    result.addLast(outboundRelationship);
//                }
//            } else {
//                result.addLast(outboundRelationship);
//            }
//
//        }
//        return result;
//
//    }

    @Override
    public PathExpander<JourneyState> reverse() {
        return null;
    }


}

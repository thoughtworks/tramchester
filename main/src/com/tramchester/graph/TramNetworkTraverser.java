package com.tramchester.graph;

import com.tramchester.domain.TramTime;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static java.lang.String.format;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static org.neo4j.graphdb.traversal.Uniqueness.NONE;

public class TramNetworkTraverser implements PathEvaluator<JourneyState>, PathExpander<JourneyState> {
    private static final Logger logger = LoggerFactory.getLogger(TramNetworkTraverser.class);

    private final ServiceHeuristics serviceHeuristics;
    private final CachedNodeOperations nodeOperations;
    private final LocalTime queryTime;
    private final long destinationNodeId;

    private final Map<Arrow, Set<TramTime>> visited;

    // TODO CALC these
    private final int pathLimit = 400;
    private final int maxJourneyMins = 170; // longest end to end is 163?

    public TramNetworkTraverser(ServiceHeuristics serviceHeuristics,
                                CachedNodeOperations nodeOperations, LocalTime queryTime, Node destinationNode) {
        this.serviceHeuristics = serviceHeuristics;
        this.nodeOperations = nodeOperations;
        this.queryTime = queryTime;
        this.destinationNodeId = destinationNode.getId();

        visited = new HashMap<>();
    }

    public Iterable<WeightedPath> findPaths(Node startNode) {

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
                expand(this, JourneyState.initialState(queryTime)).
                evaluator(this).
                uniqueness(NONE).
                order(BranchOrderingPolicies.PREORDER_BREADTH_FIRST).
                traverse(startNode);

        ResourceIterator<Path> iterator = traverser.iterator();

        List<WeightedPath> results = new ArrayList<>();
        while (iterator.hasNext()) {
            Path path = iterator.next();
            if (path.endNode().getId()==destinationNodeId) {
                results.add(calculateWeight(path));
            }
//            if (results.size()>=MAX_NUM_GRAPH_PATHS) {
//                break;
//            }
        }

        results.sort(Comparator.comparingDouble(WeightedPath::weight));

        return results;
    }

    private WeightedPath calculateWeight(Path path) {
        Integer result = 0;
        for (Relationship relat: path.relationships()) {
            result = result + nodeOperations.getCost(relat);
        }
        return new WeightedPathImpl(result.doubleValue(), path);
    }

    @Override
    public Evaluation evaluate(Path path) {
        return null;
    }

    @Override
    public Evaluation evaluate(Path path, BranchState<JourneyState> state) {

        Node endNode = path.endNode();
        long endNodeId = endNode.getId();

        if (endNodeId==destinationNodeId) {
            return Evaluation.INCLUDE_AND_PRUNE;
        }

        // no journey longer than N stages
        if (path.length()>pathLimit) {
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        // is the service running today
        if (nodeOperations.isService(endNode)) {
            if (!serviceHeuristics.checkServiceDate(endNode, path).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        JourneyState journeyState = state.getState();
        LocalTime currentElapsed = journeyState.getTime();
        //LocalTime currentElapsed = calculateElapsedTimeForPath(path);
        TramTime visitingTime = TramTime.of(currentElapsed);

        // if already tried this node at this time from this edge, don't expect a differing outcome
        Relationship inboundRelationship = path.lastRelationship();
        if (inboundRelationship !=null) {
            Arrow arrow = new Arrow(inboundRelationship.getId(), endNodeId);
            if (visited.containsKey(arrow)) {
                if (visited.get(arrow).contains(visitingTime)) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }
            } else {
                visited.put(arrow, new HashSet<>());
            }
            visited.get(arrow).add(visitingTime);
        }

        // journey too long?
        if (TramTime.diffenceAsMinutes( TramTime.of(queryTime), visitingTime)>maxJourneyMins) {
            return Evaluation.INCLUDE_AND_PRUNE;
        }

        if (nodeOperations.isService(endNode)) {
            // service available to catch?
            if (!serviceHeuristics.checkServiceTime(path, endNode, currentElapsed).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        if (nodeOperations.isHour(endNode)) {
            int hour = nodeOperations.getHour(endNode);
            if (!serviceHeuristics.interestedInHour(path, hour, currentElapsed).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        if (nodeOperations.isTime(endNode)) {
            if (!serviceHeuristics.checkTime(path, endNode, currentElapsed).isValid()) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }
        }

        return Evaluation.INCLUDE_AND_CONTINUE;
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<JourneyState> state) {

        Node endNode = path.endNode();

        Iterable<Relationship> outboundRelationships = endNode.getRelationships(OUTGOING);

        Label firstLabel = endNode.getLabels().iterator().next();
        TransportGraphBuilder.Labels nodeLabel = TransportGraphBuilder.Labels.valueOf(firstLabel.toString());

        int cost = 0;
        if (path.lastRelationship()!=null) {
            cost = nodeOperations.getCost(path.lastRelationship());
            if (cost>0) {
                JourneyState stateForChildren = new JourneyState(state.getState(), cost);
                state.setState(stateForChildren);
            }
        }
        //JourneyState currentState = state.getState();

        switch (nodeLabel) {
            case QUERY_NODE:
                return costOrdered(outboundRelationships);
            case STATION:
                if (endNode.getId()==destinationNodeId) {
                    return new LinkedList<>();
                }
                return outboundRelationships;
            case MINUTE:
                state.setState(updateStateToNodeTime(endNode));
                return outboundRelationships;
            case PLATFORM:
                // add cost?
                LocalTime newTime = state.getState().getTime().plusMinutes(cost);
                JourneyState newStateForChildren = new JourneyState(newTime,"");
                state.setState(newStateForChildren); // remove tripId
                return fromPlatformOrderedDeparts(path.lastRelationship(), outboundRelationships);
            case SERVICE:
                return hourOrdered(outboundRelationships);
            case HOUR:
                return timeOrdered(outboundRelationships);
            case ROUTE_STATION:
                return fromRSfilteredByTripAndDepart(state.getState(), outboundRelationships);
            default:
                throw new RuntimeException("Unexpected node type " + nodeLabel.name());
        }
    }

    private Iterable<Relationship> fromRSfilteredByTripAndDepart(JourneyState journeyState, Iterable<Relationship> outboundRelationships) {
        LinkedList<Relationship> results = new LinkedList<>();

        if (journeyState.hasIdTrip()) {
            // If we have trip then just "arrived" on a tram
            String tripId = journeyState.getTripId();
            for (Relationship outboundRelationship : outboundRelationships) {
                if (outboundRelationship.isType(TO_SERVICE)) {
                    String trips = nodeOperations.getTrips(outboundRelationship);
//                    String trips = outboundRelationship.getProperty(GraphStaticKeys.TRIPS).toString();
                    if (trips.contains(tripId)) {
                        results.add(outboundRelationship);
                    }
                } else {
                    if (serviceHeuristics.toEndStation(outboundRelationship)) {
                        return Collections.singleton(outboundRelationship);
                    } else {
                        results.addLast(outboundRelationship); // change tram
                    }
                }
            }
        } else {
            // No trip id means we have just boarded, ONLY depart again if at actual destination node
            for (Relationship outboundRelationship : outboundRelationships) {
                if (outboundRelationship.isType(TO_SERVICE)) {
                    results.add(outboundRelationship);
                } else {
                    if (serviceHeuristics.toEndStation(outboundRelationship)) {
                        return Collections.singleton(outboundRelationship);
                    }
                }
            }
        }
        return results;
    }

    private JourneyState updateStateToNodeTime(Node endNode) {
        LocalTime time = nodeOperations.getTime(endNode);
        String tripId = endNode.getProperty(GraphStaticKeys.TRIP_ID).toString();
        JourneyState newState = new JourneyState(time, tripId);
        return newState;
    }

    private Iterable<Relationship> hourOrdered(Iterable<Relationship> outboundRelationships) {
        SortedMap<Integer, Relationship> ordered = new TreeMap<>();
        for (Relationship outboundRelationship : outboundRelationships) {
            int hour = nodeOperations.getHour(outboundRelationship);
            ordered.put(hour,outboundRelationship);
        }
        return ordered.values();
    }

    private Iterable<Relationship> timeOrdered(Iterable<Relationship> outboundRelationships) {
        SortedMap<TramTime, Relationship> ordered = new TreeMap<>();
        for (Relationship outboundRelationship : outboundRelationships) {
            LocalTime time = nodeOperations.getTime(outboundRelationship);
            ordered.put(TramTime.of(time),outboundRelationship);
        }
        return ordered.values();
    }


    private Iterable<Relationship> costOrdered(Iterable<Relationship> outboundRelationships) {
        SortedMap<Integer, Relationship> ordered = new TreeMap<>();
        for (Relationship outboundRelationship : outboundRelationships) {
            int cost = (int) outboundRelationship.getProperty(GraphStaticKeys.COST);
            ordered.put(cost,outboundRelationship);
        }
        return ordered.values();
    }


    private Iterable<Relationship> fromPlatformOrderedDeparts(Relationship inbound,
                                                              Iterable<Relationship> outboundRelationships) {
        LinkedList<Relationship> result = new LinkedList<>();

        for (Relationship outboundRelationship : outboundRelationships) {
            if (outboundRelationship.isType(LEAVE_PLATFORM)) {
                if (serviceHeuristics.toEndStation(outboundRelationship)) {
                    // destination
                    return Collections.singleton(outboundRelationship);
                }
                //else
                if (!inbound.isType(ENTER_PLATFORM)) {
                    // dont immediately enter then leave a platform
                    result.addLast(outboundRelationship);
                }
            } else {
                result.addLast(outboundRelationship);
            }

        }
        return result;

    }

    @Override
    public PathExpander<JourneyState> reverse() {
        return null;
    }

    private LocalTime calculateElapsedTimeForPath(Path path) {

        Iterator<Relationship> relationshipIterator = path.reverseRelationships().iterator();
        List<Relationship> pathFragment = new ArrayList<>();

        LocalTime baseTime = queryTime;
        while(relationshipIterator.hasNext()) {
            Relationship relationship = relationshipIterator.next();
            pathFragment.add(relationship);

            if (relationship.isType(TRAM_GOES_TO)) {
                // TimeNode -> EndServiceNode
                Node timeNode = relationship.getStartNode();
                baseTime = nodeOperations.getTime(timeNode);
                break;
            }
        }
        int fragmentCost = pathFragment.stream().
                map(relationship -> nodeOperations.getCost(relationship)).
                collect(Collectors.summingInt(item->item));

        if (logger.isDebugEnabled()) {
            logger.debug(format("Time for %s is %s", path.toString(), queryTime.plusMinutes(fragmentCost)));
        }
        return baseTime.plusMinutes(fragmentCost);
    }

    private class Arrow {
        private final long relationshipId;
        private final long endNodeId;

        public Arrow(long relationshipId, long endNodeId) {

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

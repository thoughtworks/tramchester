package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.graph.search.stateMachine.UnexpectedNodeTypeException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static com.tramchester.graph.TransportRelationshipTypes.DEPART;
import static com.tramchester.graph.TransportRelationshipTypes.INTERCHANGE_DEPART;
import static org.neo4j.graphdb.Direction.OUTGOING;

public abstract class TraversalState implements ImmuatableTraversalState {

    protected final TraversalStateFactory builders;
    protected final TraversalOps traversalOps;
    private final Iterable<Relationship> outbounds;
    private final int costForLastEdge;
    private final int parentCost;
    private final TraversalState parent;

    private TraversalState child;

    // initial only
    protected TraversalState(TraversalOps traversalOps, TraversalStateFactory traversalStateFactory) {
        this.traversalOps = traversalOps;
        this.builders = traversalStateFactory;

        this.costForLastEdge = 0;
        this.parentCost = 0;
        this.parent = null;
        this.outbounds = new ArrayList<>();
    }

    protected TraversalState(TraversalState parent, Stream<Relationship> outbounds, int costForLastEdge) {
        this(parent, outbounds::iterator, costForLastEdge);
    }

    protected TraversalState(TraversalState parent, Iterable<Relationship> outbounds, int costForLastEdge) {
        this.traversalOps = parent.traversalOps;
        this.builders = parent.builders;
        this.parent = parent;

        this.outbounds = outbounds;
        this.costForLastEdge = costForLastEdge;
        this.parentCost = parent.getTotalCost();
    }

    public TraversalState nextState(Set<GraphBuilder.Labels> nodeLabels, Node node,
                                    JourneyStateUpdate journeyState, int cost) {
        long nodeId = node.getId();

        boolean isInterchange = nodeLabels.contains(GraphBuilder.Labels.INTERCHANGE);

        GraphBuilder.Labels nodeLabel;
        if (nodeLabels.size()==1) {
            nodeLabel = nodeLabels.iterator().next();
        } else if (nodeLabels.size()==2 && isInterchange && nodeLabels.contains(GraphBuilder.Labels.ROUTE_STATION)) {
            nodeLabel = GraphBuilder.Labels.ROUTE_STATION;
        } else {
            long stations = nodeLabels.stream().filter(GraphBuilder.Labels::isNoPlatformStation).count();
            if (stations==nodeLabels.size()) {
                // multi-mode station, all same case, so pick first one
                nodeLabel = nodeLabels.iterator().next();
            } else {
                throw new RuntimeException("Unexpected multi-label condition: " + nodeLabels);
            }
        }

        final Class<? extends TraversalState> from = this.getClass();
        switch (nodeLabel) {
            case MINUTE -> { return toMinute(builders.getTowardsMinute(from), node, cost, journeyState); }
            case HOUR -> { return toHour(builders.getTowardsHour(from), node, cost); }
            case GROUPED -> { return toGrouped(node, cost, journeyState, nodeId); }
            case BUS_STATION, TRAIN_STATION, SUBWAY_STATION, FERRY_STATION -> { return toStation(node, journeyState, cost, nodeId); }
            case TRAM_STATION -> { return toTramStation(node, journeyState, cost, nodeId); }
            case SERVICE -> { return toService(builders.getTowardsService(from), node, cost); }
            case PLATFORM -> { return toPlatform(builders.getTowardsPlatform(from), node, cost, journeyState); }
            case QUERY_NODE -> { return toWalk(builders.getTowardsWalk(from), node, cost, journeyState);}
            case ROUTE_STATION -> { return toRouteStation(from, node, cost, journeyState, isInterchange); }
            default -> throw new UnexpectedNodeTypeException(node, "Unexpected at " + this + " label:" + nodeLabel);
        }
    }

    protected JustBoardedState toJustBoarded(JustBoardedState.Builder towardsJustBoarded, Node node, int cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected TraversalState toWalk(WalkingState.Builder towardsWalk, Node node, int cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, Node node, int cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected TraversalState toService(ServiceState.Builder towardsService, Node node, int cost) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsNoPlatformStation, Node node, int cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, Node node, int cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected TraversalState toMinute(MinuteState.Builder towardsMinute, Node node, int cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected TramStationState toTramStation(TramStationState.Builder towardsStation, Node node, int cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected DestinationState toDestination(DestinationState.Builder towardsDestination, Node node, int cost, JourneyStateUpdate journeyStateUpdate) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected HourState toHour(HourState.Builder towardsHour, Node node, int cost) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected RouteStationStateOnTrip toRouteStationOnTrip(RouteStationStateOnTrip.Builder towardsRouteStation, Node node, int cost, boolean isInterchange) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected RouteStationStateEndTrip toRouteStationEndTrip(RouteStationStateEndTrip.Builder towardsRouteStation, Node node, int cost, boolean isInterchange) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected RouteStationState toRouteStationTowardsDest(RouteStationStateEndTrip.Builder towardsRouteStation, Node node, int cost) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    private TraversalState toTramStation(Node node, JourneyStateUpdate journeyState, int cost, long nodeId) {
        if (traversalOps.isDestination(nodeId)) {
            return toDestination(builders.getTowardsDestination(this.getClass()), node, cost, journeyState);
        } else {
            return toTramStation(builders.getTowardsStation(this.getClass()), node, cost, journeyState);
        }
    }

    private TraversalState toStation(Node node, JourneyStateUpdate journeyState, int cost, long nodeId) {
        if (traversalOps.isDestination(nodeId)) {
            return toDestination(builders.getTowardsDestination(this.getClass()), node, cost, journeyState);
        } else {
            return toNoPlatformStation(builders.getTowardsNoPlatformStation(this.getClass()), node, cost, journeyState);
        }
    }

    private TraversalState toGrouped(Node node, int cost, JourneyStateUpdate journeyState, long nodeId) {
        if (traversalOps.isDestination(nodeId)) {
            return toDestination(builders.getTowardsDestination(this.getClass()), node, cost, journeyState);
        } else {
            return toGrouped(builders.getTowardsGroup(this.getClass()), node, cost, journeyState);
        }
    }

    private RouteStationState toRouteStation(Class<? extends TraversalState> from, Node node, int cost, JourneyStateUpdate journeyState,
                                             boolean isInterchange) {

        if (from.equals(PlatformState.class) || from.equals(NoPlatformStationState.class)) {
            return toJustBoarded(builders.getTowardsJustBoarded(from), node, cost, journeyState);
        }

        if (from.equals(MinuteState.class)) {
            // Ignore whether interchanges only if leads to our destination
            Iterable<Relationship> allDeparts = node.getRelationships(OUTGOING, DEPART, INTERCHANGE_DEPART);
            List<Relationship> towardsDestination = traversalOps.getTowardsDestination(allDeparts);
            if (!towardsDestination.isEmpty()) {
                // we've nearly arrived
                return toRouteStationTowardsDest(builders.getTowardsRouteStationEndTrip(from),node, cost);
            }

            MinuteState minuteState = (MinuteState) this;
            if (traversalOps.hasOutboundFor(node, minuteState.getServiceId())) {
                return toRouteStationOnTrip(builders.getTowardsRouteStationOnTrip(from), node, cost, isInterchange);
            } else {
                return toRouteStationEndTrip(builders.getTowardsRouteStationEndTrip(from), node, cost, isInterchange);
            }
        } else {
            throw new RuntimeException("Unexpected from state " + from);
        }
    }

    public void dispose() {
        if (child!=null) {
            child.dispose();
            child = null;
        }
    }

    public Iterable<Relationship> getOutbounds() {
        return outbounds;
    }

    protected static Stream<Relationship> filterExcludingEndNode(Iterable<Relationship> relationships, NodeId hasNodeId) {
        long nodeId = hasNodeId.nodeId();
        return Streams.stream(relationships).
                filter(relationship -> relationship.getEndNode().getId() != nodeId);
    }

    public int getTotalCost() {
        return parentCost + getCurrentCost();
    }

    private int getCurrentCost() {
        return costForLastEdge;
    }

    @Override
    public String toString() {
        return "TraversalState{" +
                "costForLastEdge=" + costForLastEdge +
                ", parentCost=" + parentCost + System.lineSeparator() +
                ", parent=" + parent +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent);
    }

}

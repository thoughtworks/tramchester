package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import com.tramchester.graph.search.stateMachine.UnexpectedNodeTypeException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public abstract class TraversalState implements ImmuatableTraversalState {

    protected final TraversalStateFactory builders;
    protected final TraversalOps traversalOps;

    // TODO switch to Stream
    private final Iterable<Relationship> outbounds;
    private final Duration costForLastEdge;
    private final Duration parentCost;
    private final TraversalState parent;

    private TraversalState child;

    // initial only
    protected TraversalState(TraversalOps traversalOps, TraversalStateFactory traversalStateFactory) {
        this.traversalOps = traversalOps;
        this.builders = traversalStateFactory;

        this.costForLastEdge = Duration.ZERO;
        this.parentCost = Duration.ZERO;
        this.parent = null;
        this.outbounds = new ArrayList<>();
    }

    protected TraversalState(TraversalState parent, Stream<Relationship> outbounds, Duration costForLastEdge) {
        this(parent, outbounds::iterator, costForLastEdge);
    }

    protected TraversalState(TraversalState parent, Iterable<Relationship> outbounds, Duration costForLastEdge) {
        this.traversalOps = parent.traversalOps;
        this.builders = parent.builders;
        this.parent = parent;

        this.outbounds = outbounds;
        this.costForLastEdge = costForLastEdge;
        this.parentCost = parent.getTotalDuration();
    }

    public TraversalState nextState(Set<GraphLabel> nodeLabels, Node node,
                                    JourneyStateUpdate journeyState, Duration cost, boolean alreadyOnDiversion) {

        boolean isInterchange = nodeLabels.contains(GraphLabel.INTERCHANGE);
        boolean hasPlatforms = nodeLabels.contains(GraphLabel.HAS_PLATFORMS);

        GraphLabel actualNodeType;
        final int numLabels = nodeLabels.size();
        if (numLabels==1) {
            actualNodeType = nodeLabels.iterator().next();
        } else if (numLabels==2 && isInterchange && nodeLabels.contains(GraphLabel.ROUTE_STATION)) {
            actualNodeType = GraphLabel.ROUTE_STATION;
        } else if (nodeLabels.contains(GraphLabel.STATION)) {
            actualNodeType = GraphLabel.STATION;
        } else if (nodeLabels.contains(GraphLabel.HOUR)) {
            actualNodeType = GraphLabel.HOUR;
        } else {
            throw new RuntimeException("Not a station, unexpected multi-label condition: " + nodeLabels);
        }

        final Class<? extends TraversalState> from = this.getClass();
        switch (actualNodeType) {
            case MINUTE -> { return toMinute(builders.getTowardsMinute(from), node, cost, journeyState); }
            case HOUR -> { return toHour(builders.getTowardsHour(from), node, cost); }
            case GROUPED -> { return toGrouped(node, cost, journeyState); }
            case STATION -> { return toStation(node, journeyState, cost, hasPlatforms, alreadyOnDiversion); }
            case SERVICE -> { return toService(builders.getTowardsService(from), node, cost); }
            case PLATFORM -> { return toPlatform(builders.getTowardsPlatform(from), node, cost, journeyState); }
            case QUERY_NODE -> { return toWalk(builders.getTowardsWalk(from), node, cost, journeyState);}
            case ROUTE_STATION -> { return toRouteStation(from, node, cost, journeyState, isInterchange); }
            default -> throw new UnexpectedNodeTypeException(node, "Unexpected at " + this + " label:" + actualNodeType);
        }
    }

    public void toDestination(TraversalState from, Node finalNode, Duration cost, JourneyStateUpdate journeyState) {
        toDestination(builders.getTowardsDestination(from.getClass()), finalNode, cost, journeyState);
    }

    protected JustBoardedState toJustBoarded(JustBoardedState.Builder towardsJustBoarded, Node node, Duration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected TraversalState toWalk(WalkingState.Builder towardsWalk, Node node, Duration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected TraversalState toPlatform(PlatformState.Builder towardsPlatform, Node node, Duration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected TraversalState toService(ServiceState.Builder towardsService, Node node, Duration cost) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected TraversalState toNoPlatformStation(NoPlatformStationState.Builder towardsNoPlatformStation, Node node, Duration cost, JourneyStateUpdate journeyState, boolean onDiversion) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected TraversalState toGrouped(GroupedStationState.Builder towardsGroup, Node node, Duration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected TraversalState toMinute(MinuteState.Builder towardsMinute, Node node, Duration cost, JourneyStateUpdate journeyState) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected PlatformStationState toTramStation(PlatformStationState.Builder towardsStation, Node node, Duration cost, JourneyStateUpdate journeyState, boolean onDiversion) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected void toDestination(DestinationState.Builder towardsDestination, Node node, Duration cost, JourneyStateUpdate journeyStateUpdate) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected HourState toHour(HourState.Builder towardsHour, Node node, Duration cost) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected RouteStationStateOnTrip toRouteStationOnTrip(RouteStationStateOnTrip.Builder towardsRouteStation,
                                                           Node node, Duration cost, boolean isInterchange) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    protected RouteStationStateEndTrip toRouteStationEndTrip(RouteStationStateEndTrip.Builder towardsRouteStation,
                                                             Node node, Duration cost, boolean isInterchange) {
        throw new RuntimeException("No such transition at " + this.getClass());
    }

    private TraversalState toPlatformedStation(Node node, JourneyStateUpdate journeyState, Duration cost, boolean onDiversion) {
        return toTramStation(builders.getTowardsStation(this.getClass()), node, cost, journeyState, onDiversion);
    }

    private TraversalState toStation(Node node, JourneyStateUpdate journeyState, Duration cost, boolean hasPlatforms, boolean onDiversion) {
        if (hasPlatforms) {
            return toPlatformedStation(node, journeyState, cost, onDiversion);
        } else {
            return toNoPlatformStation(builders.getTowardsNoPlatformStation(this.getClass()), node, cost, journeyState, onDiversion);
        }
    }

    private TraversalState toGrouped(Node node, Duration cost, JourneyStateUpdate journeyState) {
        return toGrouped(builders.getTowardsGroup(this.getClass()), node, cost, journeyState);
    }

    private RouteStationState toRouteStation(Class<? extends TraversalState> from, Node node, Duration cost, JourneyStateUpdate journeyState,
                                             boolean isInterchange) {

        if (from.equals(PlatformState.class) || from.equals(NoPlatformStationState.class)) {
            return toJustBoarded(builders.getTowardsJustBoarded(from), node, cost, journeyState);
        }

        if (from.equals(MinuteState.class)) {

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
        return filterExcludingEndNode(Streams.stream(relationships), hasNodeId);
    }

    protected static Stream<Relationship> filterExcludingEndNode(Stream<Relationship> relationships, NodeId hasNodeId) {
        long nodeId = hasNodeId.nodeId();
        return relationships.
                filter(relationship -> relationship.getEndNode().getId() != nodeId);
    }

    /***
     * Use getTotalDuration()
     */
    @Deprecated
    public Duration getTotalCost() {
        return getTotalDuration();
    }

    /***
     * Use getCurrentDuration()
     */
    @Deprecated
    private Duration getCurrentCost() {
        return getCurrentDuration();
    }

    public Duration getTotalDuration() {
        return parentCost.plus(getCurrentCost());
    }

    public Duration getCurrentDuration() {
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

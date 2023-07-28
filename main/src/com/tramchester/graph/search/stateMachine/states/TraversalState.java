package com.tramchester.graph.search.stateMachine.states;

import com.google.common.collect.Streams;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;
import com.tramchester.graph.search.stateMachine.TraversalOps;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.impl.StandardExpander;
import org.neo4j.internal.helpers.collection.Iterables;

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

public abstract class TraversalState extends EmptyTraversalState implements ImmuatableTraversalState {

    protected final TraversalStateFactory builders;
    protected final TraversalOps traversalOps;

    // TODO switch to Stream
    private final ResourceIterable<Relationship> outbounds;
    private final Duration costForLastEdge;
    private final Duration parentCost;
    private final TraversalState parent;

    // only follow GOES_TO links for requested transport modes
    private final TransportRelationshipTypes[] requestedRelationshipTypes;

    private TraversalState child;

    // initial only
    protected TraversalState(TraversalOps traversalOps, TraversalStateFactory traversalStateFactory, Set<TransportMode> requestedModes, TraversalStateType stateType) {
        super(stateType);
        this.traversalOps = traversalOps;
        this.builders = traversalStateFactory;
        this.requestedRelationshipTypes = TransportRelationshipTypes.forModes(requestedModes);

        this.costForLastEdge = Duration.ZERO;
        this.parentCost = Duration.ZERO;
        this.parent = null;
        this.outbounds = Iterables.emptyResourceIterable();
        if (stateType!=TraversalStateType.NotStartedState) {
            throw new RuntimeException("Attempt to create for incorrect initial state " + stateType);
        }
    }

    protected TraversalState(TraversalState parent, Stream<Relationship> outbounds, Duration costForLastEdge, TraversalStateType stateType) {
        this(parent, new WrapStream(outbounds), costForLastEdge, stateType);
    }

    protected TraversalState(TraversalState parent, ResourceIterable<Relationship> outbounds, Duration costForLastEdge, TraversalStateType stateType) {
        super(stateType);
        this.traversalOps = parent.traversalOps;
        this.builders = parent.builders;
        this.parent = parent;

        this.outbounds = outbounds;
        this.costForLastEdge = costForLastEdge;
        this.parentCost = parent.getTotalDuration();

        this.requestedRelationshipTypes = parent.requestedRelationshipTypes;
    }

    @Override
    public TraversalStateType getStateType() {
        return stateType;
    }

    public static Stream<Relationship> getRelationships(Node node, Direction direction, TransportRelationshipTypes types) {
        return Streams.stream(node.getRelationships(direction, types));
    }

    public TraversalState nextState(final EnumSet<GraphLabel> nodeLabels, final Node node,
                                    final JourneyStateUpdate journeyState, final Duration cost, final boolean alreadyOnDiversion) {

        final boolean isInterchange = nodeLabels.contains(GraphLabel.INTERCHANGE);
        final boolean hasPlatforms = nodeLabels.contains(GraphLabel.HAS_PLATFORMS);

        final GraphLabel actualNodeType;
        final int numLabels = nodeLabels.size();
        if (numLabels==1) {
            actualNodeType = nodeLabels.iterator().next();
        } else if (numLabels==3 && isInterchange && nodeLabels.contains(GraphLabel.ROUTE_STATION)) {
            actualNodeType = GraphLabel.ROUTE_STATION;
        } else if (numLabels==2 && nodeLabels.contains(GraphLabel.ROUTE_STATION)) {
            actualNodeType = GraphLabel.ROUTE_STATION;
        } else if (nodeLabels.contains(GraphLabel.STATION)) {
            actualNodeType = GraphLabel.STATION;
        } else if (nodeLabels.contains(GraphLabel.HOUR)) {
            actualNodeType = GraphLabel.HOUR;
        } else {
            throw new RuntimeException("Not a station, unexpected multi-label condition: " + nodeLabels);
        }

        TraversalStateType nextType = getNextStateType(stateType, actualNodeType, hasPlatforms, node);

        return getTraversalState(nextType, node, journeyState, cost, alreadyOnDiversion, isInterchange);

    }

    private TraversalState getTraversalState(final TraversalStateType nextType, Node node, JourneyStateUpdate journeyState,
                                             Duration cost, final boolean alreadyOnDiversion, final boolean isInterchange) {
        switch (nextType) {
            case MinuteState -> {
                return toMinute(builders.getTowardsMinute(stateType), node, cost, journeyState, requestedRelationshipTypes);
            }
            case HourState -> {
                return toHour(builders.getTowardsHour(stateType), node, cost);
            }
            case GroupedStationState -> {
                return toGrouped(builders.getTowardsGroup(stateType), node, cost, journeyState);
            }
            case PlatformStationState -> {
                return toPlatformStation(builders.getTowardsStation(stateType), node, cost, journeyState, alreadyOnDiversion);
            }
            case NoPlatformStationState -> {
                return toNoPlatformStation(builders.getTowardsNoPlatformStation(stateType), node, cost, journeyState, alreadyOnDiversion);
            }
            case ServiceState -> {
                return toService(builders.getTowardsService(stateType), node, cost);
            }
            case PlatformState -> {
                return toPlatform(builders.getTowardsPlatform(stateType), node, cost, journeyState);
            }
            case WalkingState -> {
                return toWalk(builders.getTowardsWalk(stateType), node, cost, journeyState);
            }
            case RouteStationStateOnTrip -> {
                return toRouteStationOnTrip(builders.getTowardsRouteStationOnTrip(stateType), node, cost, isInterchange);
            }
            case RouteStationStateEndTrip -> {
                return toRouteStationEndTrip(builders.getTowardsRouteStationEndTrip(stateType), node, cost, isInterchange);
            }
            case JustBoardedState -> {
                return toJustBoarded(builders.getTowardsJustBoarded(stateType), node, cost, journeyState);
            }
            default -> throw new RuntimeException("Unexpected next state " + nextType + " at " + this);
        }
    }

    private TraversalStateType getNextStateType(final TraversalStateType currentStateType, final GraphLabel graphLabel,
                                                final boolean hasPlatforms, final Node node) {
        switch (graphLabel) {
            case MINUTE -> { return TraversalStateType.MinuteState; }
            case HOUR -> { return TraversalStateType.HourState; }
            case GROUPED -> { return TraversalStateType.GroupedStationState; }
            case STATION -> { return hasPlatforms ? TraversalStateType.PlatformStationState : TraversalStateType.NoPlatformStationState; }
            case SERVICE -> { return TraversalStateType.ServiceState; }
            case PLATFORM -> { return TraversalStateType.PlatformState; }
            case QUERY_NODE -> { return TraversalStateType.WalkingState; }
            case ROUTE_STATION -> { return getRouteStationStateFor(currentStateType, node); }
            default -> throw new RuntimeException("Unexpected at " + this + " label:" + graphLabel);
        }
    }

    private TraversalStateType getRouteStationStateFor(final TraversalStateType currentStateType, final Node node) {
        if (currentStateType==TraversalStateType.PlatformState || currentStateType==TraversalStateType.NoPlatformStationState) {
            return TraversalStateType.JustBoardedState;
        }
        if (currentStateType==TraversalStateType.MinuteState) {
            MinuteState minuteState = (MinuteState) this;
            if (traversalOps.hasOutboundFor(node, minuteState.getServiceId())) {
                return TraversalStateType.RouteStationStateOnTrip;
            } else {
                return TraversalStateType.RouteStationStateEndTrip;
            }
        } else {
            throw new RuntimeException("Unexpected from state " + currentStateType);
        }
    }

    public void toDestination(TraversalState from, Node finalNode, Duration cost, JourneyStateUpdate journeyState) {
        toDestination(builders.getTowardsDestination(from.getStateType()), finalNode, cost, journeyState);
    }

    public void dispose() {
        if (child!=null) {
            child.dispose();
            child = null;
        }
    }

    public ResourceIterable<Relationship> getOutbounds() {
        return outbounds;
    }

    protected static Stream<Relationship> filterExcludingEndNode(ResourceIterable<Relationship> relationships, NodeId hasNodeId) {
        return filterExcludingEndNode(Streams.stream(relationships), hasNodeId);
    }

    protected static Stream<Relationship> filterExcludingEndNode(Stream<Relationship> relationships, NodeId hasNodeId) {
        long nodeId = hasNodeId.nodeId();
        return relationships.
                filter(relationship -> relationship.getEndNode().getId() != nodeId);
    }

    public Duration getTotalDuration() {
        return parentCost.plus(getCurrentDuration());
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

    @Deprecated
    private static class WrapStream implements ResourceIterable<Relationship> {
        private final Stream<Relationship> stream;

        public WrapStream(Stream<Relationship> stream) {
            this.stream = stream;
        }

        @Override
        public ResourceIterator<Relationship> iterator() {
            return new ResourceIterator<>() {
                private final Iterator<Relationship> iterator = stream.iterator();

                @Override
                public void close() {
                    // noop
                }

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Relationship next() {
                    return iterator.next();
                }
            };
        }

        @Override
        public void close() {
            stream.close();
        }
    }
}

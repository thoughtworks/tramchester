package com.tramchester.graph.search.states;

import com.google.common.collect.Streams;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyState;
import com.tramchester.repository.TripRepository;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public abstract class TraversalState implements ImmuatableTraversalState {

    @Deprecated
    protected final Builders builders;
    protected final TraversalOps traversalOps;
    private final Iterable<Relationship> outbounds;
    private final int costForLastEdge;
    private final int parentCost;
    private final TraversalState parent;

    private TraversalState child;

    // initial only
    protected TraversalState(TraversalOps traversalOps) {
        this.traversalOps = traversalOps;
        this.builders = traversalOps.getBuilders();

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
        this.builders = traversalOps.getBuilders();
        this.parent = parent;

        this.outbounds = outbounds;
        this.costForLastEdge = costForLastEdge;
        this.parentCost = parent.getTotalCost();
    }

    protected abstract TraversalState createNextState(GraphBuilder.Labels nodeLabel, Node node,
                                                      JourneyState journeyState, int cost);

    protected TraversalState createNextState(Set<GraphBuilder.Labels> nodeLabels, Node node,
                                             JourneyState journeyState, int cost) {
        throw new RuntimeException(format("Multi label Not implemented at %s for %s labels were %s",
                this, journeyState, nodeLabels));
    }

    public TraversalState nextState(Set<GraphBuilder.Labels> nodeLabels, Node node,
                                    JourneyState journeyState, int cost) {
        if (nodeLabels.size()==1) {
            GraphBuilder.Labels nodeLabel = nodeLabels.iterator().next();
            child = createNextState(nodeLabel, node, journeyState, cost);
        } else {
            child = createNextState(nodeLabels, node, journeyState, cost);
        }

        return child;
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

    protected static class Builders {

        protected final TramStationState.Builder tramStation;
        protected final ServiceState.Builder service;
        protected final PlatformState.Builder platform;
        protected final WalkingState.Builder walking;
        protected final MinuteState.Builder minute;
        protected final DestinationState.Builder destination;
        protected final GroupedStationState.Builder groupedStation;
        private final RegistersStates registersStates;

        public Builders(SortsPositions sortsPositions, LatLong destinationLatLon, TramchesterConfig config) {
            registersStates = new RegistersStates();

            registersStates.addBuilder(new RouteStationStateOnTrip.Builder());
            registersStates.addBuilder(new RouteStationStateEndTrip.Builder());
            registersStates.addBuilder(new HourState.Builder());
            registersStates.addBuilder(new RouteStationStateJustBoarded.Builder(sortsPositions, destinationLatLon));
            registersStates.addBuilder(new NoPlatformStationState.Builder());

            service = new ServiceState.Builder();
            platform = new PlatformState.Builder();
            walking = new WalkingState.Builder();
            minute = new MinuteState.Builder(config);
            tramStation = new TramStationState.Builder();
            destination = new DestinationState.Builder();
            groupedStation = new GroupedStationState.Builder();
        }


        public RouteStationStateEndTrip.Builder towardsRouteStateEndTrip(MinuteState from, Class<RouteStationStateEndTrip> towards) {
            return (RouteStationStateEndTrip.Builder) registersStates.getBuilderFor(from.getClass(), towards);
        }

        public RouteStationStateOnTrip.Builder towardsRouteStateOnTrip(MinuteState from, Class<RouteStationStateOnTrip> towards) {
            return (RouteStationStateOnTrip.Builder) registersStates.getBuilderFor(from.getClass(), towards);
        }

        public HourState.Builder towardsHour(ServiceState from, Class<HourState> towards) {
            return (HourState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
        }

        public RouteStationStateJustBoarded.Builder towardsRouteStationJustBoarded(PlatformState from, Class<RouteStationStateJustBoarded> towards) {
            return (RouteStationStateJustBoarded.Builder) registersStates.getBuilderFor(from.getClass(), towards);
        }

        public RouteStationStateJustBoarded.Builder towardsRouteStationJustBoarded(NoPlatformStationState from, Class<RouteStationStateJustBoarded> towards) {
            return (RouteStationStateJustBoarded.Builder) registersStates.getBuilderFor(from.getClass(), towards);
        }

        public NoPlatformStationState.Builder towardsNeighbour(NoPlatformStationState from, Class<NoPlatformStationState> towards) {
            return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
        }

        public NoPlatformStationState.Builder towardsNeighbour(GroupedStationState from, Class<NoPlatformStationState> towards) {
            return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
        }

        public NoPlatformStationState.Builder towardsNeighbour(WalkingState from, Class<NoPlatformStationState> towards) {
            return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
        }

        public NoPlatformStationState.Builder towardsNeighbour(NotStartedState from, Class<NoPlatformStationState> towards) {
            return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
        }

        public NoPlatformStationState.Builder towardsNeighbourFromTramStation(TramStationState from, Class<NoPlatformStationState> towards) {
            return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
        }

        public NoPlatformStationState.Builder towardsNoPlatformStation(RouteStationStateEndTrip from, Class<NoPlatformStationState> towards) {
            return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
        }

        public NoPlatformStationState.Builder towardsNoPlatformStation(RouteStationStateOnTrip from, Class<NoPlatformStationState> towards) {
            return (NoPlatformStationState.Builder) registersStates.getBuilderFor(from.getClass(), towards);
        }
    }


}

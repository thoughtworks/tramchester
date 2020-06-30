package com.tramchester.graph.search.states;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.geo.SortsPositions;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.NodeContentsRepository;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class TraversalState implements ImmuatableTraversalState {

    private final Iterable<Relationship> outbounds;
    private final int costForLastEdge;
    private final int parentCost;
    private TraversalState child;
    private final TraversalState parent;

    protected final NodeContentsRepository nodeOperations;
    protected final long destinationNodeId;
    protected final List<String> destinationStationIds;
    protected final Builders builders;

    @Override
    public int hashCode() {
        return Objects.hash(parent);
    }

    // initial only
    protected TraversalState(SortsPositions sortsPositions, NodeContentsRepository nodeOperations,
                             long destinationNodeId, List<String> destinationStationdIds,
                             TramchesterConfig config) {
        this.nodeOperations = nodeOperations;
        this.destinationNodeId = destinationNodeId;
        this.destinationStationIds = destinationStationdIds;

        this.costForLastEdge = 0;
        this.parentCost = 0;
        this.parent = null;
        this.outbounds = new ArrayList<>();

        this.builders = new Builders(sortsPositions, destinationStationIds, config);
    }

    protected TraversalState(TraversalState parent, Iterable<Relationship> outbounds, int costForLastEdge) {
        this.nodeOperations = parent.nodeOperations;
        this.destinationNodeId = parent.destinationNodeId;
        this.destinationStationIds = parent.destinationStationIds;
        this.builders = parent.builders;

        this.parent = parent;
        this.outbounds = outbounds;
        this.costForLastEdge = costForLastEdge;
        this.parentCost = parent.getTotalCost();
    }

    protected abstract TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node node,
                                                      JourneyState journeyState, int cost);

    public TraversalState nextState(Path path, GraphBuilder.Labels nodeLabel, Node node,
                             JourneyState journeyState, int cost) {
        child = createNextState(path, nodeLabel, node, journeyState, cost);
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

    // TODO Return iterable instead
    protected static List<Relationship> filterExcludingEndNode(Iterable<Relationship> relationships, NodeId hasNodeId) {
        long nodeId = hasNodeId.nodeId();
        ArrayList<Relationship> results = new ArrayList<>();
        for (Relationship relationship: relationships) {
            if (relationship.getEndNode().getId() != nodeId) {
                results.add(relationship);
            }
        }
        return results;

//        return  StreamSupport.stream(relationships.spliterator(), false).
//                filter(relationship -> relationship.getEndNode().getId()!= nodeIdToSkip).
//                collect(Collectors.toList());
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
                ", parentCost=" + parentCost +
                ", parent=" + parent +
                '}';
    }

    public static class Builders {

        protected final RouteStationState.Builder routeStation;
        protected final RouteStationStateEndTrip.Builder routeStationEndTrip;
        protected final RouteStationStateJustBoarded.Builder routeStationJustBoarded;
        protected final BusStationState.Builder busStation;
        protected final TramStationState.Builder tramStation;
        protected final ServiceState.Builder service;
        protected final PlatformState.Builder platform;
        protected final WalkingState.Builder walking;
        protected final MinuteState.Builder minute;
        protected final HourState.Builder hour;

        public Builders(SortsPositions sortsPositions, List<String> destinationStationIds, TramchesterConfig config) {
            routeStation = new RouteStationState.Builder(config);
            routeStationEndTrip = new RouteStationStateEndTrip.Builder(config);
            routeStationJustBoarded = new RouteStationStateJustBoarded.Builder(sortsPositions, destinationStationIds);
            busStation = new BusStationState.Builder();
            service = new ServiceState.Builder();
            platform = new PlatformState.Builder();
            walking = new WalkingState.Builder();
            minute = new MinuteState.Builder(config);
            tramStation = new TramStationState.Builder();
            hour = new HourState.Builder();
        }
    }

}

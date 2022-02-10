package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.JourneyStateUpdate;
import com.tramchester.graph.search.stateMachine.NodeId;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.util.stream.Stream;

public abstract class StationState extends TraversalState implements NodeId {

    protected final Node stationNode;

    protected StationState(TraversalState parent, Stream<Relationship> outbounds, Duration costForLastEdge, Node stationNode, JourneyStateUpdate journeyState) {
        super(parent, outbounds, costForLastEdge);
        this.stationNode = stationNode;
        journeyState.seenStation(GraphProps.getStationId(stationNode));
    }

    protected StationState(TraversalState parent, Iterable<Relationship> outbounds, Duration costForLastEdge, Node stationNode, JourneyStateUpdate journeyState) {
        super(parent, outbounds, costForLastEdge);
        this.stationNode = stationNode;
        journeyState.seenStation(GraphProps.getStationId(stationNode));
    }
}

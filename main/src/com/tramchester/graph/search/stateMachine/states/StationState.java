package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.search.stateMachine.NodeId;
import org.neo4j.graphdb.Relationship;

import java.util.stream.Stream;

public abstract class StationState extends TraversalState implements NodeId {
    protected StationState(TraversalState parent, Stream<Relationship> outbounds, int costForLastEdge) {
        super(parent, outbounds, costForLastEdge);
    }

    protected StationState(TraversalState parent, Iterable<Relationship> outbounds, int costForLastEdge) {
        super(parent, outbounds, costForLastEdge);
    }
}

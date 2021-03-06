package com.tramchester.graph.search.stateMachine.states;

import org.neo4j.graphdb.Relationship;

import java.util.stream.Stream;

public abstract class RouteStationState extends TraversalState {

    protected RouteStationState(TraversalState parent, Iterable<Relationship> outbounds, int costForLastEdge) {
        super(parent, outbounds, costForLastEdge);
    }

    protected RouteStationState(TraversalState parent, Stream<Relationship> outbounds, int costForLastEdge) {
        super(parent, outbounds, costForLastEdge);
    }
}

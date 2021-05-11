package com.tramchester.graph.search.stateMachine.states;

import org.neo4j.graphdb.Relationship;

public abstract class RouteStationTripState extends TraversalState {

    protected RouteStationTripState(TraversalState parent, Iterable<Relationship> outbounds, int costForLastEdge) {
        super(parent, outbounds, costForLastEdge);
    }
}

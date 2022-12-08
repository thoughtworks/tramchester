package com.tramchester.graph.search.stateMachine.states;

import com.tramchester.graph.search.stateMachine.TowardsRouteStation;
import org.neo4j.graphdb.Relationship;

import java.time.Duration;
import java.util.stream.Stream;

public abstract class RouteStationState extends TraversalState {

    protected RouteStationState(TraversalState parent, Stream<Relationship> outbounds, Duration costForLastEdge, TowardsRouteStation<?> builder) {
        super(parent, outbounds, costForLastEdge, builder.getDestination());
    }
}

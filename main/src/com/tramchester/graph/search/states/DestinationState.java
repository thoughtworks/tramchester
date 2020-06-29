package com.tramchester.graph.search.states;

import com.tramchester.graph.build.GraphBuilder;
import com.tramchester.graph.search.JourneyState;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

import java.util.LinkedList;

public class DestinationState extends TraversalState
{
    public DestinationState(TraversalState parent, int cost) {
        super(parent, new LinkedList<>(), cost);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DestinationState)) return false;
        TraversalState that = (TraversalState) o;
        return that.destinationNodeId == this.destinationNodeId;
    }

    @Override
    public TraversalState createNextState(Path path, GraphBuilder.Labels nodeLabel, Node node, JourneyState journeyState, int cost) {
        throw new RuntimeException("Already at destination, id is " + destinationNodeId);
    }

    @Override
    public String toString() {
        return "DestinationState{" +
                "destinationNodeId=" + destinationNodeId +
                ", parent=" + parent +
                '}';
    }
}

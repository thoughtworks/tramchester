package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import org.neo4j.graphdb.Relationship;

public class WalksToRelationship extends TransportCostRelationship {
    public WalksToRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    @Override
    public boolean isWalk() {
        return true;
    }

    @Override
    public String getId() {
        return "noId";
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Walk;
    }
}

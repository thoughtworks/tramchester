package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import org.neo4j.graphdb.Relationship;

public class LeavePlatformRelationship extends TransportCostRelationship {
    public LeavePlatformRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    @Override
    public boolean isLeavePlatform() {
        return true;
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Walk;
    }
}

package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import org.neo4j.graphdb.Relationship;

public class EnterPlatformRelationship extends TransportCostRelationship  {

    public EnterPlatformRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    @Override
    public boolean isEnterPlatform() {
        return true;
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Walk;
    }
}

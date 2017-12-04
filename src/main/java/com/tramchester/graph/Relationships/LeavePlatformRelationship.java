package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.neo4j.graphdb.Relationship;

public class LeavePlatformRelationship extends TransportCostRelationship {
    public LeavePlatformRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    private LeavePlatformRelationship(int cost, String id, TramNode startNode, TramNode endNode) {
        super(cost,id, startNode, endNode);
    }

    @Override
    public boolean isLeavePlatform() {
        return true;
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Walk;
    }

    public static TransportRelationship TestOnly(int cost, String id, TramNode firstNode, TramNode secondNode) {
        return new LeavePlatformRelationship(cost, id, firstNode, secondNode);
    }
}

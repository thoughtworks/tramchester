package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
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

    private EnterPlatformRelationship(int cost, String id, TramNode startNode, TramNode endNode) {
        super(cost,id, startNode, endNode);
    }

    public static EnterPlatformRelationship TestOnly(int cost, String id, TramNode startNode, TramNode endNode) {
        return new EnterPlatformRelationship(cost,id,startNode,endNode);
    }
}

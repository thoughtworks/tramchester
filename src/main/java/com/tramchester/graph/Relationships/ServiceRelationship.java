package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.neo4j.graphdb.Relationship;

public class ServiceRelationship extends TransportCostRelationship  {

    public ServiceRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
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

    private ServiceRelationship(int cost, String id, TramNode startNode, TramNode endNode) {
        super(cost,id, startNode, endNode);
    }

    public static ServiceRelationship TestOnly(int cost, String id, TramNode startNode, TramNode endNode) {
        return new ServiceRelationship(cost,id,startNode,endNode);
    }
}

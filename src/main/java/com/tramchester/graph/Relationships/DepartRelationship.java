package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Relationship;

public class DepartRelationship extends TransportCostRelationship {
    public DepartRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    public static DepartRelationship TestOnly(String id, TramNode firstNode, TramNode endNode) {
        return new DepartRelationship(id, firstNode, endNode);
    }

    private DepartRelationship(String id, TramNode startNode, TramNode endNode) {
        super(TransportGraphBuilder.DEPARTS_COST,id,startNode, endNode);
    }

    @Override
    public int getCost() {
        return TransportGraphBuilder.DEPARTS_COST;
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Depart;
    }

    @Override
    public boolean isDepartTram() {
        return true;
    }

    @Override
    public String toString() {
        return "DepartRelationship{cost:"+ super.getCost() +", id:" + super.getId() + "}";
    }
}

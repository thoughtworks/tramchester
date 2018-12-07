package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Relationship;

public class InterchangeDepartsRelationship extends TransportCostRelationship {
    public InterchangeDepartsRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    public static InterchangeDepartsRelationship TestOnly(String id, TramNode firstNode, TramNode endNode) {
        return new InterchangeDepartsRelationship(id, firstNode, endNode);
    }

    private InterchangeDepartsRelationship(String id, TramNode startNode, TramNode endNode) {
        super(TransportGraphBuilder.INTERCHANGE_DEPART_COST ,id,startNode, endNode);
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
    public boolean isInterchange() { return true; }

    @Override
    public int getCost() {
        return TransportGraphBuilder.INTERCHANGE_DEPART_COST;
    }


    @Override
    public String toString() {
        return "InterchangeDepartsRelationship{cost:"+ super.getCost() +", id:" + super.getId() + "}";
    }
}

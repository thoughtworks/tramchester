package com.tramchester.graph.Relationships;

import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Relationship;

public class InterchangeBoardsRelationship extends BoardRelationship {

    public InterchangeBoardsRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    @Override
    public boolean isInterchange() {
        return true;
    }

    @Override
    public int getCost() {
        return TransportGraphBuilder.INTERCHANGE_BOARD_COST;
    }

    @Override
    public String toString() {
        return "InterchangeBoardsRelationship{cost:"+ super.getCost() +", id:" + super.getId() + "}";
    }
}

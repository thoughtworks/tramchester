package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Relationship;

public class BoardRelationship extends TransportCostRelationship {

    public BoardRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    public static BoardRelationship TestOnly(String id, TramNode firstNode, TramNode endNode) {
        return new BoardRelationship(id, firstNode, endNode);
    }

    private BoardRelationship(String id, TramNode startNode, TramNode endNode) {
        super(TransportGraphBuilder.BOARDING_COST,id,startNode, endNode);
    }

    @Override
    public int getCost() {
        return TransportGraphBuilder.BOARDING_COST;
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Board;
    }

    @Override
    public boolean isBoarding() {
        return true;
    }

    @Override
    public String toString() {
        return "BoardRelationship{cost:"+ super.getCost() +", id:" + super.getId() + "}";
    }
}

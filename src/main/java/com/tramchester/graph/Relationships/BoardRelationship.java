package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.neo4j.graphdb.Relationship;

public class BoardRelationship extends TransportCostRelationship {

    public BoardRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    public static BoardRelationship TestOnly(int cost, String id, TramNode firstNode, TramNode endNode) {
        return new BoardRelationship(cost,id, firstNode, endNode);
    }

    private BoardRelationship(int cost, String id, TramNode startNode, TramNode endNode) {
        super(cost,id,startNode, endNode);
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Board;
    }

    @Override
    public boolean isGoesTo() {
        return false;
    }

    @Override
    public boolean isBoarding() {
        return true;
    }

    @Override
    public boolean isDepartTram() {
        return false;
    }

    @Override
    public boolean isInterchange() {
        return false;
    }

    @Override
    public boolean isWalk() {
        return false;
    }

    @Override
    public String toString() {
        return "BoardRelationship{cost:"+ super.getCost() +", id:" + super.getId() + "}";
    }
}

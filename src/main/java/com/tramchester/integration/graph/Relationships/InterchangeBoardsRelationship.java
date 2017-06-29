package com.tramchester.integration.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.integration.graph.Nodes.NodeFactory;
import org.neo4j.graphdb.Relationship;

public class InterchangeBoardsRelationship extends TransportCostRelationship {
    public InterchangeBoardsRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
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
        return true;
    }

    @Override
    public boolean isWalk() {
        return false;
    }

    @Override
    public String toString() {
        return "InterchangeBoardsRelationship{cost:"+ super.getCost() +", id:" + super.getId() + "}";
    }
}

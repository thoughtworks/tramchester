package com.tramchester.graph.Relationships;

import org.neo4j.graphdb.Relationship;

public class InterchangeBoardsRelationship extends TramCostRelationship {
    public InterchangeBoardsRelationship(Relationship graphRelationship) {
        super(graphRelationship);
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
    public String toString() {
        return "InterchangeBoardsRelationship{cost:"+ super.getCost() +", id:" + super.getId() + "}";
    }
}

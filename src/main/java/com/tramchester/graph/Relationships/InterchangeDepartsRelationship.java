package com.tramchester.graph.Relationships;

import org.neo4j.graphdb.Relationship;

public class InterchangeDepartsRelationship extends TramCostRelationship {
    public InterchangeDepartsRelationship(Relationship graphRelationship) {
        super(graphRelationship);
    }

    @Override
    public boolean isGoesTo() {
        return false;
    }

    @Override
    public boolean isBoarding() {
        return false;
    }

    @Override
    public boolean isDepartTram() {
        return true;
    }

    @Override
    public boolean isInterchange() { return true; }

    @Override
    public String toString() {
        return "InterchangeDepartsRelationship{cost:"+ super.getCost() +", id:" + super.getId() + "}";
    }
}

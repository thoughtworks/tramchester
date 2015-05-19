package com.tramchester.graph.Relationships;

import org.neo4j.graphdb.Relationship;

public class DepartRelationship extends TramCostRelationship {
    public DepartRelationship(Relationship graphRelationship) {
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
    public boolean isInterchange() {
        return false;
    }

    @Override
    public String toString() {
        return "DepartRelationship{cost:"+ super.getCost() +", id:" + super.getId() + "}";
    }
}

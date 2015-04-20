package com.tramchester.graph.Relationships;

import org.neo4j.graphdb.Relationship;

public class InterchangeRelationship extends TramCostRelationship {
    public InterchangeRelationship(Relationship graphRelationship) {
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
        return false;
    }

    @Override
    public boolean isInterchange() {
        return true;
    }

    @Override
    public String toString() {
        return "InterchangeRelationship{cost:"+ super.cost +"}";
    }
}

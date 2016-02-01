package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import org.neo4j.graphdb.Relationship;

public class InterchangeDepartsRelationship extends TransportCostRelationship {
    public InterchangeDepartsRelationship(Relationship graphRelationship) {
        super(graphRelationship);
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Depart;
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
    public boolean isWalk() {
        return false;
    }

    @Override
    public String toString() {
        return "InterchangeDepartsRelationship{cost:"+ super.getCost() +", id:" + super.getId() + "}";
    }
}

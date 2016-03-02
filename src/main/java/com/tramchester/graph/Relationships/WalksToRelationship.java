package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import org.neo4j.graphdb.Relationship;

public class WalksToRelationship extends TransportCostRelationship {
    public WalksToRelationship(Relationship graphRelationship) {
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
        return false;
    }

    @Override
    public boolean isWalk() {
        return true;
    }

    @Override
    public String getId() {
        return "noId";
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Walk;
    }
}

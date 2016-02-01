package com.tramchester.graph.Relationships;


import com.tramchester.domain.TransportMode;
import org.neo4j.graphdb.Relationship;

public class BusGoesToRelationship extends GoesToRelationship {
    public BusGoesToRelationship(Relationship graphRelationship) {
        super(graphRelationship);
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Bus;
    }
}

package com.tramchester.graph.Relationships;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TransportMode;
import org.neo4j.graphdb.Relationship;

public class TramGoesToRelationship extends GoesToRelationship {
    public TramGoesToRelationship(String service, int cost, boolean[] daysRunning, int[] timesRunning, String id,
                                  TramServiceDate startDate, TramServiceDate endDate, String dest) {
        super(service, cost, daysRunning, timesRunning, id, startDate, endDate, dest);
    }

    public TramGoesToRelationship(Relationship graphRelationship) {
        super(graphRelationship);
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Tram;
    }
}

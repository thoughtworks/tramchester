package com.tramchester.graph.Relationships;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.neo4j.graphdb.Relationship;

import java.time.LocalTime;

public class TramGoesToRelationship extends GoesToRelationship {

    private TramGoesToRelationship(String service, int cost, boolean[] daysRunning, LocalTime[] timesRunning, String id,
                                   TramServiceDate startDate, TramServiceDate endDate, String dest,
                                   TramNode startNode, TramNode endNode, String tripId) {
        super(service, cost, daysRunning, timesRunning, id, startDate, endDate, dest, startNode, endNode, tripId);
    }

    public static TramGoesToRelationship TestOnly(String service, int cost, boolean[] daysRunning, LocalTime[] timesRunning, String id,
                                                  TramServiceDate startDate, TramServiceDate endDate, String dest,
                                                  TramNode startNode, TramNode endNode, String tripId) {
        return new TramGoesToRelationship(service,  cost,  daysRunning, timesRunning,  id,
                 startDate,  endDate,  dest,
                 startNode,  endNode, tripId);
    }

    public TramGoesToRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Tram;
    }

}

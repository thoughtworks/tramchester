package com.tramchester.graph.Relationships;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.neo4j.graphdb.Relationship;

public class TramGoesToRelationship extends GoesToRelationship {
    private TramGoesToRelationship(String service, int cost, boolean[] daysRunning, int[] timesRunning, String id,
                                  TramServiceDate startDate, TramServiceDate endDate, String dest,
                                  TramNode startNode, TramNode endNode) {
        super(service, cost, daysRunning, timesRunning, id, startDate, endDate, dest, startNode, endNode);
    }

    public static TramGoesToRelationship TestOnly(String service, int cost, boolean[] daysRunning, int[] timesRunning, String id,
                                                  TramServiceDate startDate, TramServiceDate endDate, String dest,
                                                  TramNode startNode, TramNode endNode) {
        return new TramGoesToRelationship(service,  cost,  daysRunning, timesRunning,  id,
                 startDate,  endDate,  dest,
                 startNode,  endNode);
    }

    public TramGoesToRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Tram;
    }
}

package com.tramchester.graph.Relationships;


import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import org.neo4j.graphdb.Relationship;

public class BusGoesToRelationship extends GoesToRelationship {
    public BusGoesToRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Bus;
    }
}

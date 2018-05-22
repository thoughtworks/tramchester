package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Nodes.NodeFactory;
import org.neo4j.graphdb.Relationship;

public class BusGoesToRelationship extends GoesToRelationship {
    private String routeName;

    public BusGoesToRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    public String getRouteName() {
        if (routeName==null) {
            routeName = graphRelationship.getProperty(GraphStaticKeys.RouteStation.ROUTE_NAME).toString();
        }
        return routeName;
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Bus;
    }

}

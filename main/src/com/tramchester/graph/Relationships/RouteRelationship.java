package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.neo4j.graphdb.Relationship;

import static com.tramchester.graph.GraphStaticKeys.ROUTE_ID;

public class RouteRelationship extends TransportCostRelationship {


    public RouteRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    @Override
    public TransportMode getMode() {
        return TransportMode.Tram;
    }

    private RouteRelationship(int cost, String id, TramNode startNode, TramNode endNode) {
        super(cost,id, startNode, endNode);
    }

    public static RouteRelationship TestOnly(int cost, String id, TramNode startNode, TramNode endNode) {
        return new RouteRelationship(cost,id,startNode,endNode);
    }

    @Override
    public boolean isRoute() {
        return true;
    }

    public String getRouteId() {
        return graphRelationship.getProperty(ROUTE_ID).toString();
    }
}

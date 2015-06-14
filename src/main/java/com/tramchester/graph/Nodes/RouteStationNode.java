package com.tramchester.graph.Nodes;

import com.google.common.collect.Lists;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.neo4j.graphdb.DynamicRelationshipType.withName;

public class RouteStationNode implements TramNode {
    private final String id;
    private final String routeName;
    private final String routeId;
    private final Node node;

    public RouteStationNode(Node node) {
        this.node = node;
        this.id = node.getProperty(GraphStaticKeys.ID).toString();
        this.routeName = node.getProperty(GraphStaticKeys.RouteStation.ROUTE_NAME).toString();
        this.routeId = node.getProperty(GraphStaticKeys.RouteStation.ROUTE_ID).toString();
    }

    @Override
    public boolean isStation() {
        return false;
    }

    @Override
    public boolean isRouteStation() {
        return true;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "RouteStationNode{" +
                "id='" + id + '\'' +
                ", routeName='" + routeName + '\'' +
                ", routeId='" + routeId + '\'' +
                '}';
    }

    public String getRouteName() {
        return routeName;
    }

    public String getRouteId() {
        return routeId;
    }

}

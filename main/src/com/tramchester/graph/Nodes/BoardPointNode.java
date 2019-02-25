package com.tramchester.graph.Nodes;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

public class BoardPointNode implements TramNode {
    private final String id;
    private final String routeName;
    private final String routeId;
    private final String name;

    private BoardPointNode(String id, String routeName, String routeId, String name) {
        this.id = id;
        this.routeName = routeName;
        this.routeId = routeId;
        this.name = name;
    }

    public static BoardPointNode TestOnly(String id, String routeName, String routeId, String name) {
        return new BoardPointNode(id, routeName, routeId, name);
    }

    public BoardPointNode(Node node) {
        this.id = node.getProperty(GraphStaticKeys.ID).toString();
        this.routeName = node.getProperty(GraphStaticKeys.RouteStation.ROUTE_NAME).toString();
        this.routeId = node.getProperty(GraphStaticKeys.RouteStation.ROUTE_ID).toString();
        this.name = node.getProperty(GraphStaticKeys.RouteStation.STATION_NAME).toString();
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
    public boolean isQuery() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isPlatform() {
        return false;
    }

    @Override
    public String toString() {
        return "BoardPointNode{" +
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

    @Override
    public boolean isService() {
        return false;
    }

    @Override
    public boolean isHour() {
        return false;
    }

    @Override
    public boolean isMinute() {
        return false;
    }

}

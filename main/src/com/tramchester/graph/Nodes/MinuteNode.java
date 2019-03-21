package com.tramchester.graph.Nodes;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

import java.time.LocalTime;

public class MinuteNode implements TramNode {
    private final String id;
    private final String name;
    private final Node node;

    public MinuteNode(Node node) {
        this.node = node;
        this.name = node.getProperty(GraphStaticKeys.TIME).toString();
        this.id = node.getProperty(GraphStaticKeys.ID).toString();
    }

    @Override
    public boolean isStation() {
        return false;
    }

    @Override
    public boolean isRouteStation() {
        return false;
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
    public boolean isService() {
        return false;
    }

    @Override
    public boolean isHour() {
        return false;
    }

    @Override
    public boolean isMinute() {
        return true;
    }

    public LocalTime getTime() {
        return (LocalTime) node.getProperty(GraphStaticKeys.TIME);
    }
}

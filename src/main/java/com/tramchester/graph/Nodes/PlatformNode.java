package com.tramchester.graph.Nodes;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

public class PlatformNode implements TramNode {
    private final String id;
    private final String name;

    private PlatformNode(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static PlatformNode TestOnly(String id, String name) {
        return new PlatformNode(id,name);
    }

    public PlatformNode(Node node) {
        this.name = node.getProperty(GraphStaticKeys.Station.NAME).toString();
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
        return true;
    }

    @Override
    public String toString() {
        return "PlatformNode{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
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

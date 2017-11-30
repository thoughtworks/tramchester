package com.tramchester.graph.Nodes;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

public class PlatformNode implements TramNode {
    private final String id;
    private final String name;

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
}

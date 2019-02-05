package com.tramchester.graph.Nodes;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

public class HourNode implements TramNode {
    private final String id;
    private final String name;
    private final Node node;

    public HourNode(Node node) {
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

}

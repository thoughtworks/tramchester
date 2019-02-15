package com.tramchester.graph.Nodes;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

public class HourNode implements TramNode {
    private final String id;
    private final Node node;
    private final String name;

    public HourNode(Node node) {
        this.name = node.getProperty(GraphStaticKeys.HOUR).toString();
        this.id = node.getProperty(GraphStaticKeys.ID).toString();
        this.node = node;
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
        return true;
    }

    @Override
    public boolean isMinute() {
        return false;
    }
}

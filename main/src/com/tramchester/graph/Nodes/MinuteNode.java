package com.tramchester.graph.Nodes;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

import java.time.LocalTime;

public class MinuteNode extends TramNode {
    private final String id;
    private final String name;
    private final Node node;

    public MinuteNode(Node node) {
        this.node = node;
        this.name = node.getProperty(GraphStaticKeys.TIME).toString();
        this.id = node.getProperty(GraphStaticKeys.ID).toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isMinute() {
        return true;
    }

    public LocalTime getTime() {
        return (LocalTime) node.getProperty(GraphStaticKeys.TIME);
    }
}

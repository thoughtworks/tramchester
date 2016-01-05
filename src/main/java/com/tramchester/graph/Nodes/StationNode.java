package com.tramchester.graph.Nodes;


import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

public class StationNode implements TramNode {
    private final String id;
    private final String name;

    public StationNode(Node node) {
        this.id = node.getProperty(GraphStaticKeys.ID).toString();
        this.name = node.getProperty(GraphStaticKeys.Station.NAME).toString();
    }

    @Override
    public String toString() {
        return "StationNode{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean isStation() {
        return true;
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

    public String getName() {
        return name;
    }

}

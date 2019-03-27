package com.tramchester.graph.Nodes;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

import java.time.LocalTime;

public class ServiceEndNode extends TramNode {
    private final String id;

    public ServiceEndNode(Node node) {
        this.id = node.getProperty(GraphStaticKeys.ID).toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return id;
    }

    @Override
    public boolean isServiceEnd() {
        return true;
    }


}

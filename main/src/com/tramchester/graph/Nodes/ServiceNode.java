package com.tramchester.graph.Nodes;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

public class ServiceNode extends TramNode {
    private final String id;
    private final String name;
    private final Node node;
    private boolean[] daysRunning;

    public ServiceNode(Node node) {
        this.node = node;
        this.name = node.getProperty(GraphStaticKeys.SERVICE_ID).toString();
        this.id = node.getProperty(GraphStaticKeys.ID).toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isService() {
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isPlatform() {
        return false;
    }

    public String getServiceId() {
        return name;
    }

}

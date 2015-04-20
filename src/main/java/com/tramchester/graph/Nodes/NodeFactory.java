package com.tramchester.graph.Nodes;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

public class NodeFactory {
    public TramNode getNode(Node node) {
        String type = node.getProperty(GraphStaticKeys.STATION_TYPE).toString();

        if (type.equals(GraphStaticKeys.STATION)) {
            return new StationNode(node);
        } else if(type.equals(GraphStaticKeys.ROUTE_STATION)) {
            return new RouteStationNode(node);
        }
        return null;
    }
}

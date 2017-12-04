package com.tramchester.graph.Nodes;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

public class NodeFactory {

    public TramNode getNode(Node node) {

        String type = node.getProperty(GraphStaticKeys.STATION_TYPE).toString();

        TramNode result = null;
        if (type.equals(GraphStaticKeys.STATION)) {
            result = new StationNode(node);
        } else if(type.equals(GraphStaticKeys.ROUTE_STATION)) {
            result = new BoardPointNode(node);
        } else if(type.equals(GraphStaticKeys.QUERY)) {
            result = new QueryNode(node);
        } else if(type.equals(GraphStaticKeys.PLATFORM)) {
            result = new PlatformNode(node);
        }
        return result;
    }
}

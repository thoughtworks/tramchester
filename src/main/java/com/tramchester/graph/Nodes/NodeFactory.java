package com.tramchester.graph.Nodes;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

import java.util.HashMap;
import java.util.Map;

public class NodeFactory {
    private Map<Long, TramNode> theCache;

    public NodeFactory() {
        theCache = new HashMap<>();
    }

    public TramNode getNode(Node node) {
        long id = node.getId();
        if (theCache.containsKey(id)) {
            return theCache.get(id);
        }
        String type = node.getProperty(GraphStaticKeys.STATION_TYPE).toString();

        TramNode result = null;
        if (type.equals(GraphStaticKeys.STATION)) {
            result = new StationNode(node);
        } else if(type.equals(GraphStaticKeys.ROUTE_STATION)) {
            result = new RouteStationNode(node);
        }
        theCache.put(id,result);
        return result;
    }
}

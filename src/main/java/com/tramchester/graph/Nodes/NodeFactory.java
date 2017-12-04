package com.tramchester.graph.Nodes;

import com.google.common.collect.Lists;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.util.List;

public class NodeFactory {

    public TramNode getNode(Node node) throws TramchesterException {
        Iterable<Label> iterable = node.getLabels();
        List<Label> labels = Lists.newLinkedList(iterable);

        String labelName = labels.get(0).name();
        TransportGraphBuilder.Labels type = TransportGraphBuilder.Labels.valueOf(labelName);

        switch (type) {
            case STATION:
                return new StationNode(node);
            case ROUTE_STATION:
                return new BoardPointNode(node);
            case PLATFORM:
                return new PlatformNode(node);
            case QUERY_NODE:
                return new QueryNode(node);
            default:
                throw new TramchesterException("Unknown node label " + labelName);
        }

//        TramNode result = null;
//        if (type.equals(STATION)) {
//            result = new StationNode(node);
//        } else if(type.equals(GraphStaticKeys.DESTINATION)) {
//            result = new BoardPointNode(node);
//        } else if(type.equals(GraphStaticKeys.QUERY)) {
//            result = new QueryNode(node);
//        } else if(type.equals(GraphStaticKeys.PLATFORM)) {
//            result = new PlatformNode(node);
//        }
//        return result;
    }
}

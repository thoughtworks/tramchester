package com.tramchester.graph.Nodes;

import com.google.common.collect.Lists;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.util.List;

public class NodeFactory {

    public TramNode getNode(Node node) {
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
            case SERVICE:
                return new ServiceNode(node);
            case TIME:
                return new HourNode(node);
            default:
                throw new RuntimeException("Unknown node label " + labelName);
        }

    }
}

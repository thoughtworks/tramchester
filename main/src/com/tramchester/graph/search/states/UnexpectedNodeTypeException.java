package com.tramchester.graph.search.states;

import org.neo4j.graphdb.Node;

public class UnexpectedNodeTypeException extends RuntimeException {
    public UnexpectedNodeTypeException(Node node, String message) {
        super(message + createNodeMessage(node));
    }

    private static String createNodeMessage(Node node) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" Node:").append(node.getId());
        node.getAllProperties().forEach((key, value) -> stringBuilder.append(" key:").append(key).append("-> value: ").append(value));
        return stringBuilder.toString();
    }

}

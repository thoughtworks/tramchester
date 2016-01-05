package com.tramchester.graph.Nodes;

import org.neo4j.graphdb.Node;

public class QueryNode implements TramNode {
    public QueryNode() {
    }

    @Override
    public boolean isStation() {
        return false;
    }

    @Override
    public boolean isRouteStation() {
        return false;
    }

    @Override
    public String getId() {
        return "Query";
    }

    @Override
    public boolean isQuery() {
        return true;
    }
}

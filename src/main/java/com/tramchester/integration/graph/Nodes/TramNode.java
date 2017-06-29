package com.tramchester.integration.graph.Nodes;

public interface TramNode {
    boolean isStation();
    boolean isRouteStation();
    String getId();
    boolean isQuery();
    String getName();
}

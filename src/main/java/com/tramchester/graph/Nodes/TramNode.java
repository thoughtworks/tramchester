package com.tramchester.graph.Nodes;

public interface TramNode {
    boolean isStation();
    boolean isRouteStation();
    String getId();
    boolean isQuery();
}

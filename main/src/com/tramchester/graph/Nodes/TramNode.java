package com.tramchester.graph.Nodes;

public interface TramNode {
    String getId();
    String getName();

    boolean isStation();
    boolean isRouteStation();
    boolean isPlatform();
    boolean isQuery();
    boolean isService();
    boolean isHour();
    boolean isMinute();
}

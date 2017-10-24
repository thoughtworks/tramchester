package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.graph.Nodes.TramNode;

public interface TransportRelationship {
    boolean isGoesTo();
    boolean isBoarding();
    boolean isDepartTram();
    boolean isInterchange();
    boolean isWalk();

    int getCost();
    String getId();
    TransportMode getMode();
    TramNode getStartNode();
    TramNode getEndNode();
}

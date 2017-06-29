package com.tramchester.integration.graph.Relationships;

import com.tramchester.domain.TransportMode;
import com.tramchester.integration.graph.Nodes.TramNode;

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

package com.tramchester.graph.Relationships;

import com.tramchester.domain.TransportMode;

public interface TransportRelationship {
    boolean isTramGoesTo();
    boolean isBoarding();
    boolean isDepartTram();
    boolean isInterchange();
    boolean isWalk();
    int getCost();
    String getId();
    TransportMode getMode();
}

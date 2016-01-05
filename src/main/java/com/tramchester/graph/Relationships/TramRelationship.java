package com.tramchester.graph.Relationships;

public interface  TramRelationship {
    boolean isTramGoesTo();
    boolean isBoarding();
    boolean isDepartTram();
    boolean isInterchange();
    boolean isWalk();
    int getCost();
    String getId();
    String getMode();
}

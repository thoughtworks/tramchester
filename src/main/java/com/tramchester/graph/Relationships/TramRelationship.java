package com.tramchester.graph.Relationships;

public interface  TramRelationship {
    boolean isTramGoesTo();
    boolean isBoarding();
    boolean isDepartTram();
    boolean isInterchange();
    int getCost();
    String getId();
}

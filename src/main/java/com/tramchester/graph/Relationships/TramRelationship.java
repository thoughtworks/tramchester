package com.tramchester.graph.Relationships;

public interface  TramRelationship {
    public boolean isGoesTo();
    public boolean isBoarding();
    public boolean isDepartTram();
    public boolean isInterchange();
    int getCost();

}

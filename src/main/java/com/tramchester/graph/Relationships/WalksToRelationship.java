package com.tramchester.graph.Relationships;

public class WalksToRelationship implements TramRelationship {
    @Override
    public boolean isTramGoesTo() {
        return false;
    }

    @Override
    public boolean isBoarding() {
        return false;
    }

    @Override
    public boolean isDepartTram() {
        return false;
    }

    @Override
    public boolean isInterchange() {
        return false;
    }

    @Override
    public boolean isWalk() {
        return true;
    }

    @Override
    public int getCost() {
        return 0;
    }

    @Override
    public String getId() {
        return "noId";
    }

    @Override
    public String getMode() {
        return "walk";
    }
}

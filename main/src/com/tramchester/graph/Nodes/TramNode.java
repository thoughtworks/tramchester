package com.tramchester.graph.Nodes;

public abstract class TramNode {
    public abstract String getId();
    public abstract String getName();

    public boolean isStation() {
        return false;
    }

    public boolean isRouteStation() {
        return false;
    }

    public boolean isPlatform(){
        return false;
    }

    public boolean isQuery(){
        return false;
    }

    public boolean isService(){
        return false;
    }

    public boolean isHour(){
        return false;
    }

    public boolean isMinute(){
        return false;
    }

    public boolean isServiceEnd(){
        return false;
    }

}

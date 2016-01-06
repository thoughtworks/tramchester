package com.tramchester.resources;

public class RouteCodeToClassMapper {
    public String map(String routeId) {
        return routeId.substring(4, 8);
    }
}

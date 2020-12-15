package com.tramchester.mappers;

import com.tramchester.repository.DueTramsRepository;

public class DueTramsByRouteStation {
    private final DueTramsRepository dueTramsRepository;
    private final RouteToLineMapper routeToLineMapper;


    public DueTramsByRouteStation(DueTramsRepository dueTramsRepository, RouteToLineMapper routeToLineMapper) {
        this.dueTramsRepository = dueTramsRepository;
        this.routeToLineMapper = routeToLineMapper;
    }
}

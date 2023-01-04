package com.tramchester.repository;

import com.google.inject.ImplementedBy;

@ImplementedBy(RouteRepository.class)
public interface NumberOfRoutes {
    int numberOfRoutes();
}

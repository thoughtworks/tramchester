package com.tramchester.integration.testSupport;

import com.tramchester.domain.RouteReadOnly;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.repository.InterchangeRepository;

import java.util.Set;
import java.util.stream.Collectors;

public class InterchangeRepositoryTestSupport {


    public static Set<RouteReadOnly> RoutesWithInterchanges(InterchangeRepository interchangeRepository,
                                                            TransportMode mode) {
        return interchangeRepository.getAllInterchanges().stream().
                flatMap(interchange -> interchange.getDestinationRoutes().stream()).
                filter(route -> route.getTransportMode().equals(mode)).
                collect(Collectors.toSet());
    }
}

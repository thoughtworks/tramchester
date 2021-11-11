package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.RouteReadOnly;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@LazySingleton
public class RouteCallingStations {
    private static final Logger logger = LoggerFactory.getLogger(RouteCallingStations.class);

    private final TransportData transportData;
    private final Map<RouteReadOnly, List<Station>> stations;

    @Inject
    public RouteCallingStations(TransportData transportData) {
        this.transportData = transportData;
        stations = new HashMap<>();
    }

    public List<Station> getStationsFor(RouteReadOnly route) {
        return stations.get(route);
    }

    @PreDestroy
    public void dispose() {
        logger.info("stopping");
        stations.values().forEach(List::clear);
        stations.clear();
        logger.info("stopped");
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        Collection<RouteReadOnly> routes = transportData.getRoutes();
        logger.info("Populating for " + routes.size() + " routes");
        routes.forEach(this::populateFromServices);
        logger.info("ready");
    }

    private void populateFromServices(RouteReadOnly route) {
        logger.debug("Populate calling stations for route " + HasId.asId(route));

        Set<Trip> allTrips = route.getTrips();

        // ASSUME: longest trips correspond to full end to end journeys on the whole route
        Optional<Trip> longest = allTrips.stream().max(Comparator.comparingInt(a -> a.getStopCalls().numberOfCallingPoints()));

        longest.ifPresent(longestTrip -> {
            StopCalls stops = longestTrip.getStopCalls();
            List<Station> inOrderStations = stops.stream().map(StopCall::getStation).collect(Collectors.toList());
            int size = inOrderStations.size();
            logger.debug("Adding " + size + " stations for route " + HasId.asId(route) +
                    " From:" + inOrderStations.get(0).getName() + " To:"+inOrderStations.get(size -1).getName());
            stations.put(route, inOrderStations);
        });

        if (longest.isEmpty()) {
            logger.warn("Did not find longest trip for route " + route);
            stations.put(route, Collections.emptyList());
        }
    }
}

package com.tramchester.repository;

import com.tramchester.domain.HasId;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class RouteCallingStations implements Startable, Disposable {
    private static final Logger logger = LoggerFactory.getLogger(RouteCallingStations.class);

    private final TransportData transportData;
    private final Map<Route, List<Station>> stations;

    public RouteCallingStations(TransportData transportData) {
        this.transportData = transportData;
        stations = new HashMap<>();
    }

    public List<Station> getStationsFor(Route route) {
        return stations.get(route);
    }

    @Override
    public void dispose() {
        stations.clear();
    }

    @Override
    public void start() {
        Collection<Route> routes = transportData.getRoutes();
        routes.forEach(route -> populateFromServices(route, route.getServices()));
    }

    private void populateFromServices(Route route, Set<Service> services) {
        Set<Trip> trips = services.stream().map(service -> service.getTripsFor(route)).flatMap(Collection::stream).collect(Collectors.toSet());

        // ASSUME: longest trips correspond to full end to end journeys on the whole route
        Optional<Trip> longest = trips.stream().max(Comparator.comparingInt(a -> a.getStops().numberOfCallingPoints()));

        longest.ifPresent(longestTrip -> {
            StopCalls stops = longestTrip.getStops();
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

    @Override
    public void stop() {
        // noop
    }
}

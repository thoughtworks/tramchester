package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
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
    private final Map<Route, List<Station>> stationsOnRoute;
    private final Map<RouteStation, Costs> costs;

    @Inject
    public RouteCallingStations(TransportData transportData) {
        this.transportData = transportData;
        stationsOnRoute = new HashMap<>();
        costs = new HashMap<>();
    }

    /**
     * @param route the route we want stations for
     * @return ordered list of stations, ordered by position along the route from start to finish
     */
    public List<Station> getStationsFor(Route route) {
        return stationsOnRoute.get(route);
    }

    @PreDestroy
    public void dispose() {
        logger.info("stopping");
        stationsOnRoute.clear();
        costs.clear();
        logger.info("stopped");
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        Collection<Route> routes = transportData.getRoutes();
        logger.info("Populating for " + routes.size() + " routes");
        routes.forEach(route -> {
            populateStationsOnRouteFor(route);
            populateCostToNextStation(route);
        });
        logger.info("ready");
    }

    private void populateStationsOnRouteFor(Route route) {

        Set<Trip> tripsForRoute = route.getTrips();

        // ASSUME: longest trips correspond to full end-to-end journey for the whole route
        // TODO should we use EndOfRoutes here instead?
        Optional<Trip> longest = tripsForRoute.stream().max(Comparator.comparingInt(a -> a.getStopCalls().numberOfCallingPoints()));

        longest.ifPresent(longestTrip -> {
            StopCalls stops = longestTrip.getStopCalls();
            List<Station> results = stops.stream().map(StopCall::getStation).collect(Collectors.toList());

            stationsOnRoute.put(route, results);
        });

        if (longest.isEmpty()) {
            logger.warn("Did not find longest trip for route " + route);
            stationsOnRoute.put(route, Collections.emptyList());
        }
    }

    private void populateCostToNextStation(Route route) {
        route.getTrips().forEach(trip -> {
            StopCalls stopCalls = trip.getStopCalls();
            List<StopCalls.StopLeg> legs = stopCalls.getLegs();
            if (!legs.isEmpty()) {
                populateCostsFor(route, legs);
            }
        });
    }

    private void populateCostsFor(Route route, List<StopCalls.StopLeg> legs) {
        legs.forEach(leg -> {
            RouteStation routeStation = new RouteStation(leg.getFirstStation(), route);
            if (!costs.containsKey(routeStation)) {
                costs.put(routeStation, new Costs());
            }
            costs.get(routeStation).addCost(leg.getCost());
        });
    }

    public Costs costToNextFor(RouteStation routeStation) {
        if (!costs.containsKey(routeStation)) {
            final String message = "No cost recorded for routestation " + routeStation.getId();
            logger.error(message);
            throw new RuntimeException(message);
        }
        return costs.get(routeStation);
    }

    public Costs costToNextFor(Route route, Station station) {
        return costToNextFor(new RouteStation(station, route));
    }

    public static class Costs {

        private static Costs ZeroCost() {
            Costs result = new Costs();
            result.addCost(0);
            return result;
        }

        private final List<Integer> theCosts;

        public Costs() {
            theCosts = new LinkedList<>();
        }

        public void addCost(int cost) {
            theCosts.add(cost);
        }

        public int getMin() {
            return theCosts.stream().mapToInt(Integer::valueOf).min().orElse(Integer.MAX_VALUE);
        }

        @Override
        public String toString() {
            return "Costs{" +
                    "theCosts=" + theCosts +
                    '}';
        }
    }

}

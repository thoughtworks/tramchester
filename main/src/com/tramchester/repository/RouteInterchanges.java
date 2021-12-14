package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.InterchangeStation;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@LazySingleton
public class RouteInterchanges {
    private static final Logger logger = LoggerFactory.getLogger(RouteInterchanges.class);

    private final RouteRepository routeRepository;
    private final StationRepository stationRepository;
    private final InterchangeRepository interchangeRepository;
    private final RouteCallingStations routeCallingStations;

    private final Map<Route, Set<InterchangeStation>> routeInterchanges;
    private Map<RouteStation, Integer> routeStationToInterchangeCost;

    @Inject
    public RouteInterchanges(RouteRepository routeRepository, StationRepository stationRepository, InterchangeRepository interchangeRepository, RouteCallingStations routeCallingStations) {
        this.routeRepository = routeRepository;
        this.stationRepository = stationRepository;
        this.interchangeRepository = interchangeRepository;
        this.routeCallingStations = routeCallingStations;
        routeInterchanges = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        populateRouteToInterchangeMap();
        populateRouteStationToFirstExchange();
        logger.info("started");
    }

    private void populateRouteStationToFirstExchange() {
        final Set<RouteStation> routeStations = stationRepository.getRouteStations();
        logger.info("Populate for " + routeStations.size() + " route stations");
        routeStationToInterchangeCost = routeStations.stream().
                collect(Collectors.toMap(routeStation->routeStation,
                        routeStation -> lowestCostBetween(routeStation, routeInterchanges.get(routeStation.getRoute()))));
    }

    private int lowestCostBetween(RouteStation routeStation, Set<InterchangeStation> interchangeStations) {
        Route currentRoute = routeStation.getRoute();
        List<Station> stationsAlongRoute = routeCallingStations.getStationsFor(currentRoute);
        IdSet<Station> interchangeStationIds = interchangeStations.stream().
                map(InterchangeStation::getStationId).collect(IdSet.idCollector());

        if (interchangeStationIds.contains(routeStation.getStationId())) {
            return 0; // already at an interchange
        }

        int indexOfCurrentRouteStation = 0;
        for (int i = 0; i < stationsAlongRoute.size(); i++) {
            indexOfCurrentRouteStation = i;
            if (stationsAlongRoute.get(i).equals(routeStation.getStation())) {
                break;
            }
        }

        if (indexOfCurrentRouteStation==stationsAlongRoute.size()) {
            throw new RuntimeException("Did not find " + routeStation + " in the list of stations for it's route");
        }

        if (indexOfCurrentRouteStation==stationsAlongRoute.size()-1) {
            // end of the route, not at interchange and cannot reach one from here
            return Integer.MAX_VALUE;
        }

        // return first interchange found between current index and end of the route
        for (int i = indexOfCurrentRouteStation; i < stationsAlongRoute.size(); i++) {
            final Station stationToCheck = stationsAlongRoute.get(i);
            if (interchangeStationIds.contains(stationToCheck.getId())) {
                final int costOnRoute = costOnRoute(currentRoute, stationsAlongRoute, indexOfCurrentRouteStation, i);
                logger.debug("Found interchange for " + routeStation + " cost " + costOnRoute + " at " +
                        stationsAlongRoute.get(i).getId());
                return costOnRoute;
            }
        }
        // quite possible no interchange between current station and the end of the current route
        return Integer.MAX_VALUE;
    }

    private int costOnRoute(Route currentRoute, List<Station> stations, int beginIndex, int endIndex) {
        int result = 0;
        for (int i = beginIndex; i <endIndex; i++) {
            result = result + routeCallingStations.costToNextFor(currentRoute, stations.get(i)).getMin();
        }
        return result;
    }

    private void populateRouteToInterchangeMap() {
        routeRepository.getRoutes().forEach(route -> routeInterchanges.put(route, new HashSet<>()));
        Set<InterchangeStation> allInterchanges = interchangeRepository.getAllInterchanges();
        allInterchanges.stream().
                flatMap(inter -> inter.getDropoffRoutes().stream().map(route -> Pair.of(route, inter))).
                forEach(pair -> routeInterchanges.get(pair.getLeft()).add(pair.getRight()));
    }

    public Set<InterchangeStation> getFor(Route route) {
        return routeInterchanges.get(route);
    }

    public int costToInterchange(RouteStation routeStation) {
        return routeStationToInterchangeCost.get(routeStation);
    }

}

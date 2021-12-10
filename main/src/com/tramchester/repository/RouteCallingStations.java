package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;

@LazySingleton
public class RouteCallingStations {
    private static final Logger logger = LoggerFactory.getLogger(RouteCallingStations.class);

    private final TransportData transportData;
    private final Map<Route, List<StationWithCost>> stations;

    @Inject
    public RouteCallingStations(TransportData transportData) {
        this.transportData = transportData;
        stations = new HashMap<>();
    }

    /**
     * @param route the route we want stations for
     * @return ordered list of stations, ordered by position along the route from start to finish
     */
    public List<StationWithCost> getStationsFor(Route route) {
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
        Collection<Route> routes = transportData.getRoutes();
        logger.info("Populating for " + routes.size() + " routes");
        routes.forEach(this::populateFromServices);
        logger.info("ready");
    }

    private void populateFromServices(Route route) {
        logger.debug("Populate calling stations for route " + HasId.asId(route));

        Set<Trip> allTrips = route.getTrips();

        // ASSUME: longest trips correspond to full end-to-end journey for the whole route
        // TODO should we use EndOfRoutes here instead?
        Optional<Trip> longest = allTrips.stream().max(Comparator.comparingInt(a -> a.getStopCalls().numberOfCallingPoints()));

        longest.ifPresent(longestTrip -> {
            StopCalls stops = longestTrip.getStopCalls();
            List<StopCalls.StopLeg> legs = stops.getLegs();

            List<StationWithCost> results = new ArrayList<>(legs.size());
            for (int i = 0; i < legs.size(); i++) {
                final StopCalls.StopLeg currentLeg = legs.get(i);
                results.add(new StationWithCost(currentLeg.getFirstStation(), currentLeg.getCost()));
            }
            StopCalls.StopLeg lastLeg = legs.get(legs.size() - 1);
            results.add(new StationWithCost(lastLeg.getSecondStation(), 0));
//            List<Station> inOrderStations = stops.stream().map(StopCall::getStation).collect(Collectors.toList());
            int size = results.size();
            logger.debug("Adding " + size + " stations for route " + HasId.asId(route) +
                    " From:" + results.get(0).getId() + " To:"+results.get(size -1).getId());
            stations.put(route, results);
        });

        if (longest.isEmpty()) {
            logger.warn("Did not find longest trip for route " + route);
            stations.put(route, Collections.emptyList());
        }
    }

    public static class StationWithCost {
        private final Station theStation;
        private final int costToNextStationOnRoute;


        public StationWithCost(Station theStation, int costToNextStationOnRoute) {
            this.theStation = theStation;
            this.costToNextStationOnRoute = costToNextStationOnRoute;
        }

        public Station getStation() {
            return theStation;
        }

        public int getCostToNextStation() {
            return costToNextStationOnRoute;
        }

        public IdFor<Station> getId() {
            return theStation.getId();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StationWithCost that = (StationWithCost) o;

            return theStation.equals(that.theStation);
        }

        @Override
        public int hashCode() {
            return theStation.hashCode();
        }

        @Override
        public String toString() {
            return "StationWithCost{" +
                    "theStation=" + theStation.getId() +
                    ", costToNextStationOnRoute=" + costToNextStationOnRoute +
                    '}';
        }
    }
}

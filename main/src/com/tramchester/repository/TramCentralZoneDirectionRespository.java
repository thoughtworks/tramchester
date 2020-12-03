package com.tramchester.repository;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.CentralZoneStation;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tramchester.domain.HasId.asId;
import static java.lang.String.format;

public class TramCentralZoneDirectionRespository implements Startable, Disposable {
    private static final Logger logger = LoggerFactory.getLogger(TramCentralZoneDirectionRespository.class);

    private final Map<IdFor<Route>, Integer> entered;
    private final Map<IdFor<Route>, Integer> left;

    private final RouteRepository routeRepository;
    private final RouteCallingStations routeCallingStations;

    public enum Place {
        towards,
        away,
        within
    }

    public TramCentralZoneDirectionRespository(RouteRepository routeRepository, RouteCallingStations routeCallingStations) {
        this.routeRepository = routeRepository;
        this.routeCallingStations = routeCallingStations;
        entered = new HashMap<>();
        left = new HashMap<>();
    }

    private int getRouteIndexOfStation(RouteStation routeStation) {
        List<Station> callingStations = routeCallingStations.getStationsFor(routeStation.getRoute());
        return callingStations.indexOf(routeStation.getStation());
    }

    public Place getStationPlacement(RouteStation routeStation) {
        int index = getRouteIndexOfStation(routeStation);
        IdFor<Route> routeId = routeStation.getRoute().getId();

        int enteredIndex = entered.get(routeId);
        if (index < enteredIndex) {
            return Place.towards;
        }

        if (left.containsKey(routeId)) {
            int leaveIndex = left.get(routeId);
            if (index > leaveIndex) {
                return Place.away;
            }
        }
        return Place.within;
    }

    @Override
    public void dispose() {
        entered.clear();
        left.clear();
    }

    @Override
    public void start() {
        logger.info("create central zone entry/exit indexs");
        Set<Route> allRoutes = routeRepository.getRoutes();
        allRoutes.forEach(route -> {
            List<Station> callingAt = routeCallingStations.getStationsFor(route);
            int enteredCityZone = -1;
            int leftCityZone = -1;
            for (int i = 0; i < callingAt.size(); i++) {
                Station current = callingAt.get(i);
                if (enteredCityZone==-1 && CentralZoneStation.contains(current)) {
                    enteredCityZone = i;
                }
                if (leftCityZone==-1 && enteredCityZone>=0 && !CentralZoneStation.contains(current)) {
                    leftCityZone = i-1;
                }
            }
            IdFor<Route> routeId = route.getId();
            if (enteredCityZone>=0) {
                logger.info(format("Route %s entered central zone at %s(%s)", routeId,
                        asId(callingAt.get(enteredCityZone)), enteredCityZone));
                entered.put(routeId, enteredCityZone);
            } else {
                logger.warn(format("Route %s never entered central zone", routeId));
            }
            if (leftCityZone>=0) {
                logger.info(format("Route %s left central zone at %s(%s)", routeId,
                        asId(callingAt.get(leftCityZone)), leftCityZone));
                left.put(routeId, leftCityZone);
            } else {
                logger.info(format("Route %s ended in central zone", routeId));
            }

        });
        logger.info("Ready");
    }

    @Override
    public void stop() {

    }

}

package com.tramchester.repository;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.Station;
import com.tramchester.domain.input.TramInterchanges;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class InterchangeRepository {
    private static final Logger logger = LoggerFactory.getLogger(InterchangeRepository.class);

    private static final int NUMBER_INTERCHANGES = 6;
    private final TransportDataSource dataSource;
    private final TramchesterConfig config;

    // id -> Station
    private final Map<String, Station> busInterchanges;

    public InterchangeRepository(TransportDataSource dataSource, TramchesterConfig config) {
        this.dataSource = dataSource;
        this.config = config;
        if (config.getBus()) {
            // potentially expensive
            busInterchanges = createBusInterchangeList(NUMBER_INTERCHANGES);
            logger.info(format("Added %s bus interchanges", busInterchanges.size()));
        } else {
            busInterchanges = Collections.emptyMap();
        }
    }

    private Map<String, Station> createBusInterchangeList(int numberAgencies) {
        logger.info("Finding bus interchanges bused on agency overlap of " + numberAgencies);

        Set<Station> allStations = dataSource.getStations();

        return allStations.stream().
                filter(station -> !station.isTram()).
                filter(station -> station.getAgencies().size()>=numberAgencies).
                collect(Collectors.toMap(Station::getId, (station -> station)));
    }

    public Collection<Station> getBusInterchanges() {
        return busInterchanges.values();
    }

    public boolean isInterchange(Station station) {
        if (station.isTram()) {
            return TramInterchanges.has(station);
        }
        return busInterchanges.containsValue(station);
    }

    public boolean isInterchange(String stationId) {
        if (TramInterchanges.has(stationId)) {
            return true;
        }
        if (config.getBus()) {
            return busInterchanges.containsKey(stationId);
        }
        return false;
    }

//    // remove, too approximate to be useful
//    @Deprecated
//    public Set<Route> findRoutesViaInterchangeFor(String targetBusStationId) {
//        Set<Route> results = new HashSet<>();
//        Station target = dataSource.getStation(targetBusStationId);
//
//        Set<Route> routesAtTarget = target.getRoutes();
//        for (Station interchange:busInterchanges.values()) {
//            Set<Route> overlaps = interchange.getRoutes().stream().filter(routesAtTarget::contains).collect(Collectors.toSet());
//            results.addAll(overlaps);
//        }
//        return results;
//    }
}

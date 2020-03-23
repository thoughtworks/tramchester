package com.tramchester.repository;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Station;
import com.tramchester.domain.input.TramInterchanges;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class InterchangeRepository {
    private static final Logger logger = LoggerFactory.getLogger(InterchangeRepository.class);

    private static final int NUMBER_AGENCIES = 5;
    private final TransportDataSource dataSource;
    private final TramchesterConfig config;

    // id -> Station
    private final Map<String, Station> busInterchanges;

    public InterchangeRepository(TransportDataSource dataSource, TramchesterConfig config) {
        this.dataSource = dataSource;
        this.config = config;
        if (config.getBus()) {
            // potentially expensive
            busInterchanges = createBusInterchangeList(NUMBER_AGENCIES);
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
                filter(station -> checkForInterchange(station.getName())).
                collect(Collectors.toMap(Station::getId, (station -> station)));


//        return allStations.stream().
//                filter(station -> !station.isTram()).
//                filter(station -> station.getAgencies().size()>=numberAgencies).
//                collect(Collectors.toMap(Station::getId, (station -> station)));
    }

    private boolean checkForInterchange(String name) {
        String lower = name.toLowerCase();

        if (lower.contains("interchange")) {
            return true;
        }

        if ( lower.contains("bus station") && (!lower.contains("adj bus station")) ) {
            return true;
        }

        return false;
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

}

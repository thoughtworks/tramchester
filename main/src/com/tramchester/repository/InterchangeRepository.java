package com.tramchester.repository;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.input.TramInterchanges;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class InterchangeRepository implements Disposable, Startable {
    private static final Logger logger = LoggerFactory.getLogger(InterchangeRepository.class);

    private final TransportDataSource dataSource;
    private final TramchesterConfig config;

    // id -> Station
    private Map<String, Station> busInterchanges;
    private Set<Station> multiAgency;

    public InterchangeRepository(TransportDataSource dataSource, TramchesterConfig config) {
        this.dataSource = dataSource;
        this.config = config;
        // both of these empty for trams
        busInterchanges = Collections.emptyMap();
        multiAgency = Collections.emptySet();
    }

    @Override
    public void dispose() {
        busInterchanges.clear();
    }

    @Override
    public void start() {
        if (config.getBus()) {
            Set<Station> allStations = dataSource.getStations();
            busInterchanges = createBusInterchangeList(allStations);
            logger.info(format("Added %s bus interchanges", busInterchanges.size()));
            multiAgency = createMultiAgency(allStations);
            logger.info(format("Added %s stations to multiagency list", multiAgency.size()));
        } else {
            logger.info("Buses disabled");
        }
    }

    private Set<Station> createMultiAgency(Set<Station> allStations) {
        return allStations.stream().
            filter(station -> !station.isTram()).
            filter(station -> station.getAgencies().size()>=2).
            collect(Collectors.toSet());
    }

    @Override
    public void stop() {
        // no op
    }

    private Map<String, Station> createBusInterchangeList(Set<Station> allStations) {
        logger.info("Finding bus interchanges based on names");

        return allStations.stream().
                filter(station -> !station.isTram()).
                filter(station -> checkForBusInterchange(station.getName())).
                collect(Collectors.toMap(Station::getId, (station -> station)));
    }

    // TODO WIP
    private boolean checkForBusInterchange(String name) {
        String lower = name.toLowerCase();

        if (lower.contains("interchange")) {
            return true;
        }

        return lower.contains("bus station") && (!lower.contains("adj bus station"));
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


    public Set<Station> getMultiAgencyStations() {
        return multiAgency;
    }
}

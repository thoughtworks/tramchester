package com.tramchester.repository;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.Station;
import com.tramchester.domain.input.TramInterchanges;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class InterchangeRepository {
    private static final Logger logger = LoggerFactory.getLogger(InterchangeRepository.class);

    private static final int NUMBER_INTERCHANGES = 6;
    private final TransportDataSource dataSource;
    private final List<Station> busInterchanges;

    public InterchangeRepository(TransportDataSource dataSource, TramchesterConfig config) {
        this.dataSource = dataSource;
        if (config.getBus()) {
            // potentially expensive
            busInterchanges = createBusInterchangeList(NUMBER_INTERCHANGES);
            logger.info(format("Added %s bus interchanges", busInterchanges.size()));
        } else {
            busInterchanges = Collections.emptyList();
        }
    }

    // sorted by agency numbers
    private List<Station> createBusInterchangeList(int numberAgencies) {
        logger.info("Finding bus interchanges bused on agency overlap of " + numberAgencies);

        Set<Station> allStations = dataSource.getStations();
        return allStations.stream().
                filter(station -> !station.isTram()).
                filter(station -> station.getAgencies().size()>=numberAgencies).
                sorted(Comparator.comparingInt(a -> a.getAgencies().size())).
                collect(Collectors.toList());
    }

    public List<Station> getBusInterchanges() {
        return busInterchanges;
    }

    public boolean isInterchange(Station station) {
        if (station.isTram()) {
            return TramInterchanges.has(station);
        }
        return busInterchanges.contains(station);
    }

    public Set<Route> findRoutesViaInterchangeFor(String targetBusStationId) {
        Set<Route> results = new HashSet<>();
        Station target = dataSource.getStation(targetBusStationId);

        Set<Route> routesAtTarget = target.getRoutes();
        for (Station interchange:busInterchanges) {
            Set<Route> overlaps = interchange.getRoutes().stream().filter(routesAtTarget::contains).collect(Collectors.toSet());
            results.addAll(overlaps);
        }
        return results;

    }
}

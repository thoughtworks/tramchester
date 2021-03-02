package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberConnections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.isBus;

@LazySingleton
public class EndsOfLinesRepository {
    private static final Logger logger = LoggerFactory.getLogger(EndsOfLinesRepository.class);

    private final FindStationsByNumberConnections findStationsByNumberConnections;
    private final TramchesterConfig config;
    private final Map<TransportMode,IdSet<Station>> endsOfRoutes;

    @Inject
    public EndsOfLinesRepository(FindStationsByNumberConnections findStationsByNumberConnections, TramchesterConfig config) {
        this.findStationsByNumberConnections = findStationsByNumberConnections;
        this.config = config;
        endsOfRoutes = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        Set<TransportMode> enabled = config.getTransportModes();
        for(TransportMode mode : enabled) {
            int threshhold = getThreshholdFor(mode);
            IdSet<Station> found = findStationsByNumberConnections.findFor(mode, threshhold, true);
            endsOfRoutes.put(mode, found);
            logger.info("Found " + found.size() + " ends of routes for " + mode);
        }

        logger.info("started");
    }

    private int getThreshholdFor(TransportMode mode) {
        // TODO Into config? Per data source and/or mode?
        if (isBus(mode)) {
            return 0;
        }
        return 1; // stations always on both in- and out- bound routes, so ends of line have 1 outbound link
    }

    public IdSet<Station> getStations(TransportMode mode) {
        return endsOfRoutes.get(mode);
    }
}

package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.IdSet;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.DiscoverInterchangeStations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Tram;

@LazySingleton
public class InterchangeRepository  {
    private static final Logger logger = LoggerFactory.getLogger(InterchangeRepository.class);

    // an interchange has at least this many links
    private static final int LINK_THRESHHOLD = 3;

    private final DiscoverInterchangeStations discoverInterchangeStations;
    private final TransportData dataSource;
    private final Set<TransportMode> enabledModes;

    private final Map<TransportMode, IdSet<Station>> interchanges;

    @Inject
    public InterchangeRepository(DiscoverInterchangeStations discoverInterchangeStations, TransportData dataSource, TramchesterConfig config) {
        this.discoverInterchangeStations = discoverInterchangeStations;
        this.dataSource = dataSource;
        enabledModes = config.getTransportModes();
        interchanges = new HashMap<>();
    }

    @PreDestroy
    public void dispose() {
        interchanges.clear();
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        enabledModes.forEach(mode -> {
            IdSet<Station> found = discoverInterchangeStations.findFor(mode, LINK_THRESHHOLD, false);
            interchanges.put(mode, found);
        });
        addAdditionalTramInterchanges();
        addMultiModeStations();
        logger.info("started");
    }

    private void addAdditionalTramInterchanges() {
        IdSet<Station> tramInterchanges = interchanges.get(Tram);
        TramInterchanges.stations().forEach(tramInterchanges::add);
    }


    private void addMultiModeStations() {
        dataSource.getStations().stream().
                filter(station -> station.getTransportModes().size()>1).
                forEach(station -> station.getTransportModes().forEach(mode -> interchanges.get(mode).add(station.getId())));
    }

    public boolean isInterchange(Station station) {

        // only checking stations modes is small optimisation as station id's are unique
        Set<TransportMode> stationModes = station.getTransportModes();

        for (TransportMode mode : stationModes) {
            if (interchanges.get(mode).contains(station.getId())) {
                return true;
            }
        }

        return false;
    }

    public boolean isInterchange(IdFor<Station> stationId) {
        for (TransportMode enabledMode : enabledModes) {
            if (interchanges.get(enabledMode).contains(stationId)) {
                return true;
            }
        }
        return false;
    }

    public IdSet<Station> getInterchangesFor(TransportMode mode) {
        return interchanges.get(mode);
    }
}

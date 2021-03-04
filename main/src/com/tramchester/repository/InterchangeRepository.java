package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberLinks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.domain.reference.TransportMode.isBus;
import static java.lang.String.format;

@LazySingleton
public class InterchangeRepository  {
    private static final Logger logger = LoggerFactory.getLogger(InterchangeRepository.class);

    private final FindStationsByNumberLinks findStationsByNumberConnections;
    private final TransportData dataSource;
    private final Set<TransportMode> enabledModes;

    private final Map<TransportMode, IdSet<Station>> interchanges;

    @Inject
    public InterchangeRepository(FindStationsByNumberLinks findStationsByNumberConnections, TransportData transportData,
                                 TramchesterConfig config) {
        this.findStationsByNumberConnections = findStationsByNumberConnections;
        this.dataSource = transportData;
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
            logger.debug("Finding interchanges for " + mode);
            int linkThreshhold = getLinkThreshhold(mode);
            IdSet<Station> found = findStationsByNumberConnections.findAtLeastNConnectionsFrom(mode, linkThreshhold);
            interchanges.put(mode, found);
            logger.info(format("Added %s interchanges for %s and link threshold %s", found.size(), mode, linkThreshhold));
        });
        addAdditionalTramInterchanges();
        addMultiModeStations();
        logger.info("started");
    }

    private int getLinkThreshhold(TransportMode mode) {
        // todo into config? Per datasource & transport mode?
        if (isBus(mode)) {
            return 2;
        }
        return 3;
    }

    private void addAdditionalTramInterchanges() {
        // TODO should really check data source as well
        if (enabledModes.contains(Tram)) {
            IdSet<Station> tramInterchanges = interchanges.get(Tram);
            TramInterchanges.stations().forEach(tramInterchanges::add);
        }
    }

    private void addMultiModeStations() {
        Set<Station> multimodeStations = dataSource.getStations().stream().
                filter(station -> station.getTransportModes().size() > 1).collect(Collectors.toSet());
        logger.info("Adding " + multimodeStations.size() + " multimode stations");
        multimodeStations.forEach(station -> station.getTransportModes().forEach(mode -> interchanges.get(mode).add(station.getId())));
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

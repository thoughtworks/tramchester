package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberLinks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.Collection;
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
    private final StationRepository stationRepository;
    private final CompositeStationRepository compositeStationRepository;
    private final Set<TransportMode> enabledModes;

    private final Map<TransportMode, IdSet<Station>> interchanges;

    @Inject
    public InterchangeRepository(FindStationsByNumberLinks findStationsByNumberConnections, StationRepository stationRepository,
                                 CompositeStationRepository compositeStationRepository,
                                 TramchesterConfig config) {
        this.findStationsByNumberConnections = findStationsByNumberConnections;
        this.stationRepository = stationRepository;
        this.compositeStationRepository = compositeStationRepository;
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
        enabledModes.forEach(this::populateInterchangesFor);
        addAdditionalTramInterchanges();
        addMultiModeStations();
        logger.info("started");
    }

    private void populateInterchangesFor(TransportMode mode) {
        logger.info("Finding interchanges for " + mode);
        int linkThreshhold = getLinkThreshhold(mode);
        IdSet<Station> found = findStationsByNumberConnections.findAtLeastNConnectionsFrom(mode, linkThreshhold);
        logger.info(format("Added %s interchanges for %s and link threshold %s", found.size(), mode, linkThreshhold));

        Set<CompositeStation> composites = compositeStationRepository.getCompositesFor(mode);
        IdSet<Station> allContainedStations = composites.stream().
                map(CompositeStation::getContained).
                filter(contained -> contained.size() > linkThreshhold).
                flatMap(Collection::stream).
                map(Station::getId).
                collect(IdSet.idCollector());
        logger.info(format("Added %s interchanges for %s composites", allContainedStations.size(), composites.size()));
        found.addAll(allContainedStations);

        interchanges.put(mode, found);
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
            IdSet<Station> addToSet = interchanges.get(Tram);
            TramInterchanges.stations().forEach(addToSet::add);
        }
    }

    private void addMultiModeStations() {
        // NOTE: by default the train data set contains mixed mode due to replacement buses, bus links, subways etc
        Set<Station> multimodeStations = stationRepository.getStations().stream().
                filter(station -> station.getTransportModes().size() > 1).
                collect(Collectors.toSet());
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

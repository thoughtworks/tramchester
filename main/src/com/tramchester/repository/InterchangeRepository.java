package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.ModeIdsMap;
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
    private final NeighboursRepository neighboursRepository;
    private final CompositeStationRepository compositeStationRepository;
    private final TramchesterConfig config;

    private final ModeIdsMap<Station> interchanges;

    @Inject
    public InterchangeRepository(FindStationsByNumberLinks findStationsByNumberConnections, StationRepository stationRepository,
                                 NeighboursRepository neighboursRepository, CompositeStationRepository compositeStationRepository,
                                 TramchesterConfig config) {
        this.findStationsByNumberConnections = findStationsByNumberConnections;
        this.stationRepository = stationRepository;
        this.neighboursRepository = neighboursRepository;
        this.compositeStationRepository = compositeStationRepository;
        this.config = config;
        interchanges = new ModeIdsMap<>();
    }

    @PreDestroy
    public void dispose() {
        interchanges.clear();
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        Set<TransportMode> enabledModes = config.getTransportModes();

        enabledModes.forEach(this::populateInterchangesFor);
        addAdditionalTramInterchanges();
        addMultiModeStations();
        enabledModes.forEach(this::addStationsWithNeighbours);
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

        interchanges.addAll(mode, found);
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
        if (config.getTransportModes().contains(Tram)) {
            TramInterchanges.stations().forEach(tramInterchange -> interchanges.add(Tram, tramInterchange));
        }
    }

    private void addMultiModeStations() {
        // NOTE: by default the train data set contains mixed mode due to replacement buses, bus links, subways etc
        Set<Station> multimodeStations = stationRepository.getStationStream().
                filter(station -> station.getTransportModes().size() > 1).
                collect(Collectors.toSet());
        logger.info("Adding " + multimodeStations.size() + " multimode stations");
        multimodeStations.forEach(station -> station.getTransportModes().forEach(mode -> interchanges.add(mode, station.getId())));
    }

    private void addStationsWithNeighbours(TransportMode mode) {
        if (config.getCreateNeighbours()) {
            neighboursRepository.getStationsWithNeighbours(mode).forEach(stationId -> {
                interchanges.add(mode, stationId);
            });
        }
    }

    public boolean isInterchange(TransportMode mode, IdFor<Station> stationId) {
        return interchanges.containsFor(mode, stationId);
    }

    public boolean isInterchange(Station station) {
        for(TransportMode mode : station.getTransportModes()) {
            if (isInterchange(mode, station.getId())) {
                return true;
            }
        }
        return false;
    }

    public boolean isInterchange(IdFor<Station> stationId) {
        for (TransportMode enabledMode : config.getTransportModes()) {
            if (isInterchange(enabledMode, stationId)) {
                return true;
            }
        }
        return false;
    }

    public IdSet<Station> getInterchangesFor(TransportMode mode) {
        return interchanges.get(mode);
    }
}

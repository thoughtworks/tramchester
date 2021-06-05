package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.*;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberLinks;
import com.tramchester.graph.filters.GraphFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.isBus;
import static java.lang.String.format;

// TODO Split out I/F here

@LazySingleton
public class InterchangeRepository  {
    private static final Logger logger = LoggerFactory.getLogger(InterchangeRepository.class);

    private final FindStationsByNumberLinks findStationsByNumberConnections;
    private final StationRepository stationRepository;
    private final NeighboursRepository neighboursRepository;
    private final CompositeStationRepository compositeStationRepository;
    private final TramchesterConfig config;
    private final GraphFilter graphFilter;

    private final Set<Station> interchanges;

    @Inject
    public InterchangeRepository(FindStationsByNumberLinks findStationsByNumberConnections, StationRepository stationRepository,
                                 NeighboursRepository neighboursRepository, CompositeStationRepository compositeStationRepository,
                                 TramchesterConfig config, GraphFilter graphFilter) {
        this.findStationsByNumberConnections = findStationsByNumberConnections;
        this.stationRepository = stationRepository;
        this.neighboursRepository = neighboursRepository;
        this.compositeStationRepository = compositeStationRepository;
        this.config = config;
        this.graphFilter = graphFilter;
        interchanges = new HashSet<>();
    }

    @PreDestroy
    public void dispose() {
        interchanges.clear();
    }

    @PostConstruct
    public void start() {
        logger.info("Starting");
        Set<TransportMode> enabledModes = config.getTransportModes();

        enabledModes.forEach(mode -> {
            int linkThreshhold = getLinkThreshhold(mode);
            populateInterchangesFor(mode, linkThreshhold);
            addCompositeStations(mode, linkThreshhold);
        });

        addAdditionalInterchanges(config.getGTFSDataSource());
        addMultiModeStations();
        enabledModes.forEach(this::addStationsWithNeighbours);

        logger.info("started");
    }

    private void populateInterchangesFor(TransportMode mode, int linkThreshhold) {
        logger.info("Finding interchanges for " + mode + " and threshhold " + linkThreshhold);

        IdSet<Station> foundIds = findStationsByNumberConnections.findAtLeastNConnectionsFrom(mode, linkThreshhold);
        logger.info(format("Added %s interchanges for %s and link threshold %s", foundIds.size(), mode, linkThreshhold));

        Set<Station> found = foundIds.stream().map(stationRepository::getStationById).collect(Collectors.toSet());
        interchanges.addAll(found);
    }

    private void addCompositeStations(TransportMode mode, int linkThreshhold) {
        logger.info("Adding composite stations as interchanges");

        Set<CompositeStation> composites = compositeStationRepository.getCompositesFor(mode);
        if (composites.isEmpty()) {
            logger.info("No composites to add");
            return;
        }

        Set<Station> expandedMatchingComposites = composites.stream().
                map(CompositeStation::getContained).
                filter(contained -> contained.size() >= linkThreshhold).
                flatMap(Collection::stream).
                collect(Collectors.toSet());

        if (expandedMatchingComposites.isEmpty()) {
            logger.warn(format("Did not find any composites matching threshold %s out of %s composites", linkThreshhold, composites.size()));
        } else {
            logger.info(format("Adding %s interchanges for %s composites", expandedMatchingComposites.size(), composites.size()));
        }
        interchanges.addAll(expandedMatchingComposites);
    }

    private int getLinkThreshhold(TransportMode mode) {
        // todo into config? Per datasource & transport mode?
        if (isBus(mode)) {
            return 2;
        }
        return 3;
    }

    private IdFor<Station> formStationId(String raw) {
        return StringIdFor.createId(raw);
    }

    private void addAdditionalInterchanges(List<GTFSSourceConfig> gtfsDataSource) {
        long countBefore = interchanges.size();

        gtfsDataSource.stream().
            filter(dataSource -> !dataSource.getAdditionalInterchanges().isEmpty()).
            forEach(this::addAdditionalInterchangesForSource);

        if (countBefore==interchanges.size()) {
            logger.info("No additional interchanges to add from any data sources");
        }
    }

    private void addAdditionalInterchangesForSource(GTFSSourceConfig dataSource) {
        final String name = dataSource.getName();
        final Set<String> additionalInterchanges = dataSource.getAdditionalInterchanges();
        logger.info("For source " + name + " attempt to add " + additionalInterchanges.size() + " interchange stations");

        Set<Station> validStationsFromConfig = additionalInterchanges.stream().
                map(this::formStationId).
                filter(graphFilter::shouldInclude).
                filter(stationRepository::hasStationId).
                map(stationRepository::getStationById).
                collect(Collectors.toSet());

        if (validStationsFromConfig.isEmpty()) {
            final String msg = "For " + name + " no valid interchange station id's found to add from " + additionalInterchanges;
            if (graphFilter.isFiltered()) {
                logger.warn(msg);
            } else {
                logger.error(msg);
            }
            return;
        }

        if (validStationsFromConfig.size() != additionalInterchanges.size()) {
            IdSet<Station> invalidIds = additionalInterchanges.stream().
                    map(this::formStationId).
                    filter(id -> !stationRepository.hasStationId(id)).collect(IdSet.idCollector());
            logger.error("For " + name + " additional interchange station ids invalid:" + invalidIds);
        }

        addInterchangesWhereModesMatch(dataSource.getTransportModes(), validStationsFromConfig);
    }

    private void addInterchangesWhereModesMatch(Set<TransportMode> enabledModes, Set<Station> stations) {
        long countBefore = interchanges.size();
        stations.forEach(station -> enabledModes.forEach(enabledMode -> {
            if (station.getTransportModes().contains(enabledMode)) {
                interchanges.add(station);
                logger.info("Added interchange " + station.getId() + " for mode "+ enabledMode);
            }
        }));
        if (countBefore==interchanges.size()) {
            logger.warn("Added no interchanges (mode mismatches?) from " + HasId.asIds(stations));
        }
    }

    private void addMultiModeStations() {
        // NOTE: by default the train data set contains mixed mode due to replacement buses, bus links, subways etc
        Set<Station> multimodeStations = stationRepository.getStationStream().
                filter(station -> station.getTransportModes().size() > 1).
                collect(Collectors.toSet());
        logger.info("Adding " + multimodeStations.size() + " multimode stations");
        multimodeStations.forEach(station -> station.getTransportModes().forEach(mode -> interchanges.add(station)));
    }

    @Deprecated
    private void addStationsWithNeighbours(TransportMode mode) {
        if (config.getCreateNeighbours()) {
            neighboursRepository.getStationsWithNeighbours(mode).stream().
                    map(stationRepository::getStationById).
                    forEach(interchanges::add);
        }
    }

    public boolean isInterchange(Station station) {
        return interchanges.contains(station);
    }

    /***
     * Composites - only the linked stations returned
     * @return interchanges
     */
    public IdSet<Station> getInterchangesFor(TransportMode mode) {
        return interchanges.stream().
                filter(station -> station.getTransportModes().contains(mode)).
                collect(IdSet.collector());
    }

    /***
     * Composites - only the linked stations returned
     * @return interchanges
     */
    public IdSet<Station> getAllInterchanges() {
        return interchanges.stream().collect(IdSet.collector());
    }

    public Set<Station> getInterchangesOn(Route route) {
        return interchanges.stream().
                filter(station -> station.servesRoute(route)).
                collect(Collectors.toSet());
    }
}

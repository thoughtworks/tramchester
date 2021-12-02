package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.InterchangeStation;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
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

import static java.lang.String.format;

@LazySingleton
public class Interchanges implements InterchangeRepository {
    private static final Logger logger = LoggerFactory.getLogger(Interchanges.class);

    private final FindStationsByNumberLinks findStationsByNumberConnections;
    private final StationRepository stationRepository;
    private final NeighboursRepository neighboursRepository;
    private final CompositeStationRepository compositeStationRepository;
    private final TramchesterConfig config;
    private final GraphFilter graphFilter;

    private final Map<IdFor<Station>, InterchangeStation> interchanges;

    @Inject
    public Interchanges(FindStationsByNumberLinks findStationsByNumberConnections, StationRepository stationRepository,
                        NeighboursRepository neighboursRepository, CompositeStationRepository compositeStationRepository,
                        TramchesterConfig config, GraphFilter graphFilter) {
        this.findStationsByNumberConnections = findStationsByNumberConnections;
        this.stationRepository = stationRepository;
        this.neighboursRepository = neighboursRepository;
        this.compositeStationRepository = compositeStationRepository;
        this.config = config;
        this.graphFilter = graphFilter;

        interchanges = new HashMap<>();
    }

    @PreDestroy
    public void dispose() {
        interchanges.clear();
    }

    @PostConstruct
    public void start() {
        Set<TransportMode> enabledModes = config.getTransportModes();

        logger.info("Starting for " + enabledModes);

        enabledModes.stream().filter(this::discoveryEnabled).forEach(mode -> {
            int linkThreshhold = getLinkThreshhold(mode);
            populateInterchangesFor(mode, linkThreshhold);
            addCompositeStations(mode); // 3 otherwise all 'pairs' of stations on single routes included
        });

        addAdditionalInterchanges(config.getGTFSDataSource());
        addMultiModeStations();

        if (config.getCreateNeighbours()) {
            addNeighboursAsInterchangesBetweenModes();
        }

        logger.info("Total number of interchanges: " + interchanges.size());
        logger.info("started");
    }

    private void addNeighboursAsInterchangesBetweenModes() {
        if (!neighboursRepository.differentModesOnly()) {
            // TODO
            throw new RuntimeException("TODO - not defined yet when neighbours can be same transport mode");
        }
        Set<StationLink> neighbours = neighboursRepository.getAll();
        int before = interchanges.size();
        neighbours.forEach(stationLink -> {
            final IdFor<Station> beginId = stationLink.getBegin().getId();

            final Set<Route> dropoffRoutesAtEnd = stationLink.getEnd().getDropoffRoutes();

            if (interchanges.containsKey(beginId)) {
                // already flagged as an interchange, add the additional routes from the other station
                InterchangeStation existing = interchanges.get(beginId);
                existing.addLinkedRoutes(dropoffRoutesAtEnd);
            } else {
                // not an interchange yet, so only add the routes from the linked station
                interchanges.put(beginId, new InterchangeStation(stationLink.getBegin(), dropoffRoutesAtEnd));
            }
        });
        final int count = interchanges.size() - before;
        final String msg = "Added " + count + " interchanges from multi-modal neighbours";
        if (count == 0) {
            logger.warn(msg);
        } else {
            logger.info(msg);
        }
    }

    private boolean discoveryEnabled(TransportMode mode) {
        return switch (mode) {
            case Tram, Bus, Train, Ferry, Subway -> true;
            case RailReplacementBus, Connect, NotSet, Walk -> false;
        };
    }

    private void populateInterchangesFor(TransportMode mode, int linkThreshhold) {
        logger.info("Adding stations marked as interchanges");
        Set<Station> markedAsInterchange = stationRepository.getStationsForMode(mode).
                stream().filter(Station::isMarkedInterchange).collect(Collectors.toSet());
        addStations(markedAsInterchange);
        logger.info(format("Added %s %s stations marked as as interchange", markedAsInterchange.size(), mode));

        logger.info("Finding interchanges for " + mode + " and threshhold " + linkThreshhold);
        IdSet<Station> foundIdsViaLinks = findStationsByNumberConnections.findAtLeastNConnectionsFrom(mode, linkThreshhold);

        // filter out any station already marked as interchange, or if data source only uses marked interchanges
        Set<Station> foundViaLinks = foundIdsViaLinks.stream().map(stationRepository::getStationById).
                filter(station -> !station.isMarkedInterchange()).
                filter(station -> !config.onlyMarkedInterchange(station)).
                collect(Collectors.toSet());
        logger.info(format("Added %s interchanges for %s and link threshold %s", foundViaLinks.size(), mode, linkThreshhold));
        addStations(foundViaLinks);
    }

    private void addCompositeStations(TransportMode mode) {
        logger.info("Adding composite stations as interchanges");

        Set<CompositeStation> composites = compositeStationRepository.getCompositesFor(mode);
        if (composites.isEmpty()) {
            logger.info("No composites to add");
            return;
        }

        Set<Station> expandedMatchingComposites = composites.stream().
                map(CompositeStation::getContained).
                filter(contained -> contained.size() >= 3).
                flatMap(Collection::stream).
                collect(Collectors.toSet());

        if (expandedMatchingComposites.isEmpty()) {
            logger.warn(format("Did not find any composites matching threshold %s out of %s composites", 3, composites.size()));
        } else {
            logger.info(format("Adding %s interchanges for %s composites", expandedMatchingComposites.size(), composites.size()));
        }
        addStations(expandedMatchingComposites);
    }

    private void addStations(Set<Station> stations) {
        Map<IdFor<Station>, InterchangeStation> toAdd = stations.stream().
                map(InterchangeStation::new).
                collect(Collectors.toMap(InterchangeStation::getStationId, item -> item));
        interchanges.putAll(toAdd);
    }

    private void addStationToInterchanges(Station station) {
        interchanges.put(station.getId(), new InterchangeStation(station));
    }

    private int getLinkThreshhold(TransportMode mode) {
        // todo into config? Per datasource & transport mode? Bus 2??
        return switch (mode) {
            case Bus, Ferry -> 2;
            case Tram, Subway, Train -> 3;
            default -> throw new RuntimeException("Todo for " + mode);
        };
    }

    private IdFor<Station> formStationId(String raw) {
        return StringIdFor.createId(raw);
    }

    private void addAdditionalInterchanges(List<GTFSSourceConfig> gtfsDataSource) {
        long countBefore = interchanges.size();

        gtfsDataSource.stream().
                filter(dataSource -> !dataSource.getAdditionalInterchanges().isEmpty()).
                forEach(this::addAdditionalInterchangesForSource);

        if (countBefore == interchanges.size()) {
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
            if (station.serves(enabledMode)) {
                addStationToInterchanges(station);
                logger.info("Added interchange " + station.getId() + " for mode " + enabledMode);
            }
        }));
        if (countBefore == interchanges.size()) {
            logger.warn("Added no interchanges (mode mismatches?) from " + HasId.asIds(stations));
        }
    }

    private void addMultiModeStations() {
        // NOTE: by default the train data set contains mixed mode due to replacement buses, bus links, subways etc
        Set<Station> multimodeStations = stationRepository.getStationStream().
                filter(station -> station.getTransportModes().size() > 1).
                collect(Collectors.toSet());
        logger.info("Adding " + multimodeStations.size() + " multimode stations");
        multimodeStations.forEach(station -> station.getTransportModes().forEach(mode -> addStationToInterchanges(station)));
    }

    @Override
    public boolean isInterchange(Station station) {
        return interchanges.containsKey(station.getId());
    }

    /***
     * Composites - only the linked stations returned
     * @return interchanges
     */
    @Override
    public Set<InterchangeStation> getAllInterchanges() {
        return new HashSet<>(interchanges.values());
    }

    @Override
    public int size() {
        return interchanges.size();
    }

}

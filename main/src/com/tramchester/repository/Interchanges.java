package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.FindStationsByNumberLinks;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.graph.graphbuild.StationsAndLinksGraphBuilder;
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
    private final TramchesterConfig config;
    private final GraphFilter graphFilter;

    private final Map<IdFor<Station>, InterchangeStation> interchanges;

    @Inject
    public Interchanges(FindStationsByNumberLinks findStationsByNumberConnections, StationRepository stationRepository,
                        NeighboursRepository neighboursRepository,
                        TramchesterConfig config, GraphFilter graphFilter, StationsAndLinksGraphBuilder.Ready ready) {
        this.findStationsByNumberConnections = findStationsByNumberConnections;
        this.stationRepository = stationRepository;
        this.neighboursRepository = neighboursRepository;
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

        enabledModes.forEach(this::addMarkedStations);

        enabledModes.stream().filter(this::discoveryEnabled).forEach(mode -> {
            int linkThreshhold = getLinkThreshhold(mode);
            populateInterchangesFor(mode, linkThreshhold);
        });

        addAdditionalInterchanges(config.getGTFSDataSource());
        addMultiModeStations();

        // Need to do this last, as checking if one of the neighbours is an interchange is required
        if (config.hasNeighbourConfig()) {
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
        neighbours.
                forEach(stationLink -> {
                    final IdFor<Station> beginId = stationLink.getBegin().getId();

                    final Set<Route> dropoffRoutesAtEnd = stationLink.getEnd().getDropoffRoutes();

                    if (interchanges.containsKey(beginId)) {
                        // already flagged as an interchange, add the additional routes from the other station
                        InterchangeStation existing = interchanges.get(beginId);
                        existing.addPickupRoutes(dropoffRoutesAtEnd);
                    } else {
                        // not an interchange yet, so only add the routes from the linked station
                        InterchangeStation interchangeStation = new InterchangeStation(stationLink.getBegin(), dropoffRoutesAtEnd,
                                InterchangeStation.InterchangeType.NeighbourLinks);
                        interchanges.put(beginId, interchangeStation);
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
            case RailReplacementBus, Connect, NotSet, Walk, Ship, Unknown -> false;
        };
    }

    private void populateInterchangesFor(TransportMode mode, int linkThreshhold) {

        logger.info("Finding interchanges for " + mode + " and threshhold " + linkThreshhold);
        IdSet<Station> foundIdsViaLinks = findStationsByNumberConnections.atLeastNLinkedStations(mode, linkThreshhold);

        // filter out any station already marked as interchange, or if data source only uses marked interchanges
        Set<Station> foundViaLinks = foundIdsViaLinks.stream().map(stationRepository::getStationById).
                filter(station -> !station.isMarkedInterchange()).
                filter(station -> !config.onlyMarkedInterchange(station)).
                collect(Collectors.toSet());
        logger.info(format("Added %s interchanges for %s and link threshold %s", foundViaLinks.size(), mode, linkThreshhold));
        addStations(foundViaLinks, InterchangeStation.InterchangeType.NumberOfLinks);
    }

    private void addMarkedStations(TransportMode mode) {
        logger.info("Adding stations marked as interchanges");
        Set<Station> markedAsInterchange = stationRepository.getStationsServing(mode).
                stream().filter(Station::isMarkedInterchange).collect(Collectors.toSet());
        addStations(markedAsInterchange, InterchangeStation.InterchangeType.FromSourceData);
        logger.info(format("Added %s %s stations marked as as interchange", markedAsInterchange.size(), mode));
    }

    private void addStations(Set<Station> stations, InterchangeStation.InterchangeType type) {
        Map<IdFor<Station>, InterchangeStation> toAdd = stations.stream().
                map(station -> new InterchangeStation(station, type)).
                collect(Collectors.toMap(InterchangeStation::getStationId, item -> item));
        interchanges.putAll(toAdd);
    }

    private void addStationToInterchanges(Station station, InterchangeStation.InterchangeType type) {
        interchanges.put(station.getId(), new InterchangeStation(station, type));
    }

    public static int getLinkThreshhold(TransportMode mode) {
        // todo into config? Per datasource & transport mode? Bus 2??
        return switch (mode) {
            case Ferry -> 2;
            case Bus -> 3;
            case Tram, Subway, Train -> 3;
            default -> throw new RuntimeException("Todo for " + mode);
        };
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
        IdSet<Station> additionalInterchanges = dataSource.getAdditionalInterchanges();
        logger.info("For source " + name + " attempt to add " + additionalInterchanges.size() + " interchange stations");

        Set<Station> validStationsFromConfig = additionalInterchanges.stream().
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
                    filter(id -> !stationRepository.hasStationId(id)).collect(IdSet.idCollector());
            logger.error("For " + name + " additional interchange station ids invalid:" + invalidIds);
        }

        addInterchangesWhereModesMatch(dataSource.getTransportModes(), validStationsFromConfig, InterchangeStation.InterchangeType.FromConfig);
    }

    private void addInterchangesWhereModesMatch(Set<TransportMode> enabledModes, Set<Station> stations,
                                                InterchangeStation.InterchangeType type) {
        long countBefore = interchanges.size();
        stations.forEach(station -> enabledModes.forEach(enabledMode -> {
            if (station.servesMode(enabledMode)) {
                addStationToInterchanges(station, type);
                logger.info("Added interchange " + station.getId() + " for mode " + enabledMode);
            }
        }));
        if (countBefore == interchanges.size()) {
            logger.warn("Added no interchanges (mode mismatches?) from " + HasId.asIds(stations));
        }
    }

    private void addMultiModeStations() {
        // NOTE: by default the train data set contains mixed mode due to replacement buses, bus links, subways etc
        Set<Station> multimodeStations = stationRepository.getActiveStationStream().
                filter(station -> station.getTransportModes().size() > 1).
                collect(Collectors.toSet());
        logger.info("Adding " + multimodeStations.size() + " multimode stations");
        multimodeStations.forEach(station -> station.getTransportModes().forEach(mode -> addStationToInterchanges(station,
                InterchangeStation.InterchangeType.Multimodal)));
    }

    @Override
    public boolean isInterchange(Location<?> location) {
        if (location.getLocationType() == LocationType.Station) {
            IdFor<Station> stationId = StringIdFor.convert(location.getId());
            return interchanges.containsKey(stationId);
        } else {
            return false;
        }
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

    @Override
    public InterchangeStation getInterchange(Location<?> location) {
        if (location.getLocationType() == LocationType.Station) {
            IdFor<Station> stationId = StringIdFor.convert(location.getId());
            return interchanges.get(stationId);
        }
        throw new RuntimeException(location + " is not a station");
    }

}

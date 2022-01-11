package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.id.CompositeId;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.CompositeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.repository.naptan.NaptanRespository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static java.lang.String.format;

/***
 * Wrap stations with duplicate names inside of a composite so at API/UI level see unique list of station names
 */
@LazySingleton
public class CompositeStationRepository implements StationRepositoryPublic {
    private static final Logger logger = LoggerFactory.getLogger(CompositeStationRepository.class);

    private final StationRepository stationRepository;
    private final TramchesterConfig config;
    private final NaptanRespository naptanRespository;
    private final GraphFilter graphFilter;

    private final IdSet<Station> isUnderlyingStationComposite;

    // TODO use IdMap<CompositeStation>
    private final Map<IdFor<Station>, CompositeStation> compositeStations;
    private final Map<String, CompositeStation> compositeStationsByName;

    @Inject
    public CompositeStationRepository(StationRepository stationRepository, TramchesterConfig config,
                                      NaptanRespository naptanRespository, GraphFilter graphFilter) {
        this.stationRepository = stationRepository;
        this.config = config;
        this.naptanRespository = naptanRespository;
        this.graphFilter = graphFilter;
        isUnderlyingStationComposite = new IdSet<>();
        compositeStations = new HashMap<>();
        compositeStationsByName = new HashMap<>();
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        compositeStations.clear();
        compositeStationsByName.clear();
        logger.info("Stopped");
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        List<GTFSSourceConfig> dataSources = config.getGTFSDataSource();
        dataSources.forEach(dataSource -> {
            final Set<TransportMode> compositeStationModes = dataSource.compositeStationModes();
            if (!compositeStationModes.isEmpty()) {
                populateFor(dataSource, compositeStationModes);
            } else {
                logger.info("Not adding " + dataSource.getName() + " since no composite station modes");
            }
        });
        logger.info("started");
    }

    private void populateFor(GTFSSourceConfig dataSource, Set<TransportMode> modes) {
        if (graphFilter.isFiltered()) {
            logger.warn("Filtering is enabled");
        }

        if (dataSource.getDataSourceId()==DataSourceID.tfgm && modes.contains(Bus)) {
            if (!naptanRespository.isEnabled()) {
                String msg = "Naptan config not present in remoteSources, it is required when Bus is enabled for TFGM " + dataSource;
                logger.error(msg);
                throw new RuntimeException(msg);
            }
        }

        logger.info("Populating for source:" + dataSource.getDataSourceId() + " modes:" + modes);
        modes.forEach(mode -> capture(dataSource.getDataSourceId(), mode) );
    }

    private void capture(DataSourceID dataSourceID, TransportMode mode) {

        Set<String> duplicatedNames = getDuplicatedNamesFor(dataSourceID, mode);

        if (duplicatedNames.isEmpty()) {
            logger.info("Not creating any composite stations for " + mode);
            return;
        }

        logger.info("Found " + duplicatedNames.size() + " duplicated names for " + mode +
                " out of " + stationRepository.getNumberOfStations());

        Map<String, Set<Station>> groupedByName = stationRepository.getStationsFromSource(dataSourceID).
                filter(graphFilter::shouldInclude).
                filter(station -> station.servesMode(mode)).
                filter(station -> !station.getArea().isBlank()).
                filter(station -> duplicatedNames.contains(station.getName())).
                collect(Collectors.groupingBy(Station::getName, Collectors.toSet()));
        groupedByName.forEach((name, stations) -> groupByAreaAndAdd(mode, name, stations));

        logger.info("Created " + compositeStations.size() + " composite stations");
    }

    @NotNull
    private Set<String> getDuplicatedNamesFor(DataSourceID dataSourceID, TransportMode mode) {
        return stationRepository.getStationsFromSource(dataSourceID).
                filter(graphFilter::shouldInclude).
                filter(station -> station.servesMode(mode)).
                map(Station::getName).
                collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).
                entrySet().stream().
                filter(item -> item.getValue() > 1).
                map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private void groupByAreaAndAdd(TransportMode mode, String nonUniqueName, Set<Station> stationsWithSameName) {
        Map<String, Set<Station>> groupdedByArea = stationsWithSameName.stream().
                collect(Collectors.groupingBy(Station::getArea, Collectors.toSet()));
        groupdedByArea.forEach((area, stations) -> addComposite(mode, nonUniqueName, area, stations));
    }

    private void addComposite(TransportMode mode, String nonUniqueName, String area, Set<Station> stationsToGroup) {
        if (stationsToGroup.size()==1) {
            Station single = stationsToGroup.iterator().next();
            logger.debug(format("Not grouping for area:%s name:%s as single station matched id:%s",
                    area, nonUniqueName, single.getId()));
            return;
        }

        logger.debug(format("Create for ids:%s name:%s mode:%s area:%s", HasId.asIds(stationsToGroup), nonUniqueName, mode, area));

        String compositeName = attemptUnqiueName(nonUniqueName, area, stationsToGroup);
        int minChangeCost = computeMinChangeCost(stationsToGroup);
        CompositeStation compositeStation = new CompositeStation(stationsToGroup, area, compositeName, minChangeCost);

        compositeStations.put(compositeStation.getId(), compositeStation);
        compositeStationsByName.putIfAbsent(compositeName, compositeStation); // see attemptUnqiueName, might fail to get unique name

        stationsToGroup.stream().map(Station::getId).collect(IdSet.idCollector()).forEach(isUnderlyingStationComposite::add);
    }

    private int computeMinChangeCost(Set<Station> stationsToGroup) {
        // find greatest distance between the stations, then convert to a cost
        Set<StationPair> allPairs = stationsToGroup.stream().
                flatMap(stationA -> stationsToGroup.stream().map(stationB -> StationPair.of(stationA, stationB))).
                filter(pair -> !pair.getBegin().equals(pair.getEnd())).
                collect(Collectors.toSet());
        OptionalLong furthestQuery = allPairs.stream().
                mapToLong(pair -> CoordinateTransforms.calcCostInMinutes(pair.getBegin(), pair.getEnd(), config.getWalkingMPH())).
                max();
        if (furthestQuery.isEmpty()) {
            return 1;
        }

        return (int) furthestQuery.getAsLong();

    }

    private String attemptUnqiueName(String nonUniqueName, String area, Set<Station> stations) {
        String compositeName = nonUniqueName;
        if (compositeStationsByName.containsKey(compositeName)) {
            compositeName = compositeName + ", " + area;
            if (compositeStationsByName.containsKey(compositeName)) {
                logger.warn(format("Unable to create unqiue name for %s, tried %s and stations %s ",
                        HasId.asIds(stations), nonUniqueName, compositeName));
            }
        }
        return compositeName;
    }

    /***
     * Provides composites instead of the stations contained in that composite
     * @param mode transport mode
     * @return stations for transport mode
     */
    @Override
    public Set<Station> getStationsServing(TransportMode mode) {
        Set<Station> result = stationRepository.getStationsServing(mode).stream().
                filter(station -> !isUnderlyingStationComposite.contains(station.getId()))
                .collect(Collectors.toSet());
        result.addAll(getCompositesServing(mode));
        return result;
    }

    /***
     * Provides composites instead of the stations contained in that composite
     * @return stations
     */
    @Override
    public Stream<Station> getActiveStationStream() {
        Stream<Station> stationStream = stationRepository.getActiveStationStream().
                filter(station -> !isUnderlyingStationComposite.contains(station.getId()));
        return Stream.concat(stationStream, compositeStations.values().stream());
    }

    @Override
    public Station getStationById(IdFor<Station> stationId) {
        if (compositeStations.containsKey(stationId)) {
            return compositeStations.get(stationId);
        }
        return stationRepository.getStationById(stationId);
    }

    @Override
    public boolean hasStationId(IdFor<Station> stationId) {
        if (compositeStations.containsKey(stationId)) {
            return true;
        }
        return stationRepository.hasStationId(stationId);
    }

    public IdSet<Station> resolve(IdFor<Station> id) {
        if (!compositeStations.containsKey(id)) {
            logger.warn(id + " was not a composite station");
            return IdSet.singleton(id);
        }
        CompositeId<Station> compositeId = CompositeId.parse(id.forDTO());
        return compositeId.getIds();
    }

    public long getNumberOfComposites() {
        return compositeStations.size();
    }

    public Set<CompositeStation> getCompositesServing(TransportMode mode) {
        return compositeStations.values().stream().
                filter(station -> station.servesMode(mode)).
                collect(Collectors.toSet());
    }

    public CompositeStation findByName(String name) {
        return compositeStationsByName.get(name);
    }

    public Set<CompositeStation> getAllComposites() {
        return new HashSet<>(compositeStations.values());
    }
}

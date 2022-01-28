package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.GroupedStations;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.graph.filters.GraphFilter;
import com.tramchester.repository.naptan.NaptanRespository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Bus;
import static java.lang.String.format;

/***
 * Wrap stations with same area id inside a group so at API/UI level see unique list of station names
 */
@LazySingleton
public class StationGroupsRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationGroupsRepository.class);

    private final TramchesterConfig config;
    private final NaptanRespository naptanRespository;
    private final StationRepository stationRepository;
    private final GraphFilter graphFilter;

    private final boolean enabled;

    private final Map<IdFor<NaptanArea>, GroupedStations> stationGroups;
    private final Map<String, GroupedStations> stationGroupsByName;

    @Inject
    public StationGroupsRepository(StationRepository stationRepository, TramchesterConfig config,
                                   NaptanRespository naptanRespository, GraphFilter graphFilter) {
        this.config = config;
        this.enabled = naptanRespository.isEnabled();

        this.stationRepository = stationRepository;
        this.naptanRespository = naptanRespository;
        this.graphFilter = graphFilter;

        stationGroups = new HashMap<>();
        stationGroupsByName = new HashMap<>();
    }

    @PreDestroy
    public void stop() {
        logger.info("Stopping");
        stationGroups.clear();
        stationGroupsByName.clear();
        logger.info("Stopped");
    }

    @PostConstruct
    public void start() {
        if (!enabled) {
            logger.warn("Naptan is disabled, cannot find grouped stations, need areaId and areaName from naptan");
            return;
        }

        logger.info("starting");
        List<GTFSSourceConfig> dataSources = config.getGTFSDataSource();
        dataSources.forEach(dataSource -> {
            final Set<TransportMode> compositeStationModes = dataSource.compositeStationModes();
            if (!compositeStationModes.isEmpty()) {
                populateFor(dataSource, compositeStationModes);
            } else {
                logger.warn("Not adding " + dataSource.getName() + " since no composite station modes");
            }
        });
        String message = format("Loaded %s groups and %s names", stationGroupsByName.size(), stationGroups.size());
        if (stationGroups.isEmpty() || stationGroupsByName.isEmpty()) {
            logger.warn(message);
        } else {
            logger.info(message);
        }
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

        Map<IdFor<NaptanArea>, Set<Station>> groupedByName = stationRepository.getStationsFromSource(dataSourceID).
                filter(graphFilter::shouldInclude).
                filter(station -> station.servesMode(mode)).
                filter(station -> station.getAreaId().isValid()).
                collect(Collectors.groupingBy(Station::getAreaId, Collectors.toSet()));

        groupedByName.entrySet().stream().
                filter(item -> item.getValue().size() > 1).
                forEach(item -> groupByAreaAndAdd(item.getKey(), item.getValue()));

        logger.info("Created " + stationGroups.size() + " composite stations from " + groupedByName.size());
    }

    private void groupByAreaAndAdd(IdFor<NaptanArea> areaId, Set<Station> stationsInArea) {
        stationsInArea.forEach(station ->  addComposite(areaId, stationsInArea));
    }

    private void addComposite(IdFor<NaptanArea> areaId, Set<Station> stationsToGroup) {

        int minChangeCost = computeMinChangeCost(stationsToGroup);

        String areaName  = areaId.toString();
        if (naptanRespository.containsArea(areaId)) {
            NaptanArea area = naptanRespository.getAreaFor(areaId);
            areaName = area.getName();
        } else {
            logger.error(format("Using %s as name, missing area code %s for station group %s", areaName, areaId, HasId.asIds(stationsToGroup)));
        }

        GroupedStations groupedStations = new GroupedStations(stationsToGroup, areaId, areaName, minChangeCost);

        stationGroups.put(areaId, groupedStations);
        stationGroupsByName.put(areaName, groupedStations);
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

    private void guardIsEnabled() {
        if (enabled) {
            return;
        }
        String msg = "Repository is disabled";
        logger.error(msg);
        throw new RuntimeException(msg);
    }

//    public long getNumberOfGroups() {
//        guardIsEnabled();
//        return stationGroups.size();
//    }

    public Set<GroupedStations> getStationGroupsFor(TransportMode mode) {
        guardIsEnabled();
        return stationGroups.values().stream().
                filter(station -> station.servesMode(mode)).
                collect(Collectors.toSet());
    }

    public GroupedStations findByName(String name) {
        guardIsEnabled();
        return stationGroupsByName.get(name);
    }

    public Set<GroupedStations> getAllGroups() {
        guardIsEnabled();
        return new HashSet<>(stationGroups.values());
    }

    public GroupedStations getStationGroup(IdFor<NaptanArea> areaId) {
        guardIsEnabled();
        return stationGroups.get(areaId);
    }

    public boolean isEnabled() {
        return enabled;
    }
}

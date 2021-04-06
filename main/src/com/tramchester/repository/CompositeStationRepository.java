package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.id.CompositeId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdMap;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

/***
 * Wrap stations with duplicate names inside of a composite so at API/UI level see unique list of station names
 */
@LazySingleton
public class CompositeStationRepository implements StationRepositoryPublic {
    private static final Logger logger = LoggerFactory.getLogger(CompositeStationRepository.class);

    private final StationRepository stationRepository;
    private final TramchesterConfig config;
    private final IdSet<Station> isUnderlyingStationComposite;
    private final IdMap<Station> compositeStations;

    @Inject
    public CompositeStationRepository(StationRepository stationRepository, TramchesterConfig config) {
        this.stationRepository = stationRepository;
        this.config = config;
        isUnderlyingStationComposite = new IdSet<>();
        compositeStations = new IdMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        Set<TransportMode> modes = config.getTransportModes();
        modes.forEach(this::capture);
        logger.info("started");
    }

    private void capture(TransportMode mode) {

        Set<String> duplicatedNames = getDuplicatedNamesFor(mode);

        logger.warn("Found " + duplicatedNames.size() + " duplicated names for " + mode +
                " out of " + stationRepository.getNumberOfStations());

        if (duplicatedNames.isEmpty()) {
            logger.info("Not creating any composite stations for " + mode);
            return;
        }

        Map<String, Set<Station>> groupedByName = stationRepository.getStationsForMode(mode).stream().
                filter(station -> duplicatedNames.contains(station.getName())).
                collect(Collectors.groupingBy(Station::getName, Collectors.toSet()));
        groupedByName.forEach((name, stations) -> addCompositeStation(mode, name, stations));
    }

    @NotNull
    private Set<String> getDuplicatedNamesFor(TransportMode mode) {
        return stationRepository.getStationsForModeStream(mode).
                filter(station -> !station.hasPlatforms()).
                map(Station::getName).
                collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).
                entrySet().stream().
                filter(item -> item.getValue() > 1).
                map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private void addCompositeStation(TransportMode mode, String nonUniqueName, Set<Station> stationsWithSameName) {
        IdFor<Station> newId = createIdFor(stationsWithSameName);
        logger.info(format("Create id:%s name:%s mode:%s", newId, nonUniqueName, mode));
        String area = "undefined";
        LatLong latLong = findLocationFor(stationsWithSameName);
        GridPosition gridPosition = CoordinateTransforms.getGridPosition(latLong);

        Station compositeStation = new Station(newId, area, nonUniqueName, latLong, gridPosition);
        stationsWithSameName.stream().flatMap(station -> station.getRoutes().stream()).forEach(compositeStation::addRoute);
        compositeStations.add(compositeStation);

        stationsWithSameName.stream().map(Station::getId).collect(IdSet.idCollector()).forEach(isUnderlyingStationComposite::add);
    }

    private LatLong findLocationFor(Set<Station> stations) {
        double lat = stations.stream().mapToDouble(station -> station.getLatLong().getLat()).
                average().orElse(Double.NaN);
        double lon = stations.stream().mapToDouble(station -> station.getLatLong().getLon()).
                average().orElse(Double.NaN);
        return new LatLong(lat, lon);
    }

    private IdFor<Station> createIdFor(Set<Station> stations) {
        IdSet<Station> ids = stations.stream().map(Station::getId).collect(IdSet.idCollector());
        return new CompositeId<>(ids);
    }

    @Override
    public Set<Station> getStationsForMode(TransportMode mode) {
        Set<Station> result = stationRepository.getStationsForMode(mode).stream().
                filter(station -> !isUnderlyingStationComposite.contains(station.getId()))
                .collect(Collectors.toSet());
        result.addAll(getCompositesFor(mode));
        return result;
    }

    @Override
    public Station getStationById(IdFor<Station> stationId) {
        if (compositeStations.hasId(stationId)) {
            return compositeStations.get(stationId);
        }
        return stationRepository.getStationById(stationId);
    }

    @Override
    public boolean hasStationId(IdFor<Station> stationId) {
        if (compositeStations.hasId(stationId)) {
            return true;
        }
        return stationRepository.hasStationId(stationId);
    }

    public IdSet<Station> resolve(IdFor<Station> id) {
        if (!compositeStations.hasId(id)) {
            logger.warn(id + " was not a composite station");
            return IdSet.singleton(id);
        }
        CompositeId<Station> compositeId = CompositeId.parse(id.forDTO());
        return compositeId.getIds();
    }

    public long getNumberOfComposites() {
        return compositeStations.size();
    }

    public Set<Station> getCompositesFor(TransportMode mode) {
        return compositeStations.filterStream(item -> item.getTransportModes().contains(mode)).collect(Collectors.toSet());
    }
}

package com.tramchester.dataimport.rail.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.ProvidesRailStationRecords;
import com.tramchester.dataimport.rail.records.PhysicalStationRecord;
import com.tramchester.dataimport.rail.records.RailLocationRecord;
import com.tramchester.dataimport.rail.records.reference.RailInterchangeType;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/***
 * Supports loading of rail station data only, use StationRepository in all other cases
 */
@LazySingleton
public class RailStationRecordsRepository {
    private static final Logger logger = LoggerFactory.getLogger(RailStationRecordsRepository.class);

    private final IdSet<Station> inUseStations;
    private final Map<String, MutableStation> tiplocMap;
    private final Set<String> missing;
    private final ProvidesRailStationRecords providesRailStationRecords;
    private final RailStationCRSRepository crsRepository;
    private final NaptanRepository naptanRepository;
    private final boolean enabled;

    @Inject
    public RailStationRecordsRepository(ProvidesRailStationRecords providesRailStationRecords, RailStationCRSRepository crsRepository,
                                        NaptanRepository naptanRepository, TramchesterConfig config) {
        this.providesRailStationRecords = providesRailStationRecords;
        this.crsRepository = crsRepository;
        this.naptanRepository = naptanRepository;
        inUseStations = new IdSet<>();
        tiplocMap = new HashMap<>();
        missing = new HashSet<>();
        enabled = config.hasRailConfig();
    }

    @PostConstruct
    public void start() {
        if (enabled) {
            logger.info("start");
            loadStations(providesRailStationRecords.load());
            logger.info("started");
        }
    }

    @PreDestroy
    private void close() {
        if (enabled) {
            if (!missing.isEmpty()) {
                logger.warn("Missing station locations that were referenced in timetable " + missing);
            }
            missing.clear();
            inUseStations.clear();
            tiplocMap.clear();
        }
    }

    private void loadStations(Stream<PhysicalStationRecord> physicalRecords) {

        Stream<Pair<MutableStation, PhysicalStationRecord>> railStations = physicalRecords.
                filter(this::validRecord).
                map(record -> Pair.of(createStationFor(record), record));

        railStations.forEach(railStationPair -> {
            final MutableStation mutableStation = railStationPair.getLeft();
            PhysicalStationRecord physicalStationRecord = railStationPair.getValue();
            addStation(mutableStation, physicalStationRecord.getTiplocCode());
            crsRepository.putCRS(mutableStation, physicalStationRecord.getCRS());
        });

        logger.info("Initially loaded " + tiplocMap.size() + " stations" );

    }

    private boolean validRecord(PhysicalStationRecord physicalStationRecord) {
        if (physicalStationRecord.getName().isEmpty()) {
            logger.warn("Invalid record " + physicalStationRecord);
            return false;
        }
        if (physicalStationRecord.getNorthing()==Integer.MAX_VALUE) {
            logger.warn("Invalid record " + physicalStationRecord);
            return false;
        }
        if (physicalStationRecord.getEasting()==Integer.MAX_VALUE) {
            logger.warn("Invalid record " + physicalStationRecord);
            return false;
        }
        return true;
    }

    private MutableStation createStationFor(PhysicalStationRecord record) {
        IdFor<Station> id = Station.createId(record.getTiplocCode());

        String name = record.getName();
        GridPosition grid = GridPosition.Invalid;
        IdFor<NaptanArea> areaId = NaptanArea.invalidId();
        boolean isInterchange = (record.getRailInterchangeType()!= RailInterchangeType.None);

        if (naptanRepository.containsTiploc(id)) {
            // prefer naptan data if available
            NaptanRecord stopsData = naptanRepository.getForTiploc(id);
            grid = stopsData.getGridPosition();
            name = stopsData.getName();
            areaId = TransportEntityFactory.chooseArea(naptanRepository, stopsData.getAreaCodes());
        }

        if (!grid.isValid()) {
            // not from naptan, try to get from rail data
            if (record.getEasting() == Integer.MIN_VALUE || record.getNorthing() == Integer.MIN_VALUE) {
                // have missing grid for this station, was 00000
                grid = GridPosition.Invalid;
            } else {
                grid = convertToOsGrid(record.getEasting(), record.getNorthing());
            }
        }

        LatLong latLong = grid.isValid() ?  CoordinateTransforms.getLatLong(grid) : LatLong.Invalid;

        Duration minChangeTime = Duration.ofMinutes(record.getMinChangeTime());

        return new MutableStation(id, areaId, name, latLong, grid, DataSourceID.rail, isInterchange, minChangeTime);
    }

    private GridPosition convertToOsGrid(int easting, int northing) {
        return new GridPosition(easting* 100L, northing* 100L);
    }

    private void addStation(MutableStation mutableStation, String tipLoc) {
        tiplocMap.put(tipLoc, mutableStation);
    }

    public Set<MutableStation> getInUse() {
        return tiplocMap.values().stream().
                filter(station -> inUseStations.contains(station.getId())).
                collect(Collectors.toSet());
    }

    public void markAsInUse(Station station) {
        inUseStations.add(station.getId());
    }

    public int count() {
        return tiplocMap.size();
    }

    public int countNeeded() {
        return inUseStations.size();
    }

    public boolean hasStationRecord(RailLocationRecord record) {
        final String tiplocCode = record.getTiplocCode();
        boolean found = tiplocMap.containsKey(tiplocCode);
        if (!found) {
            missing.add(record.getTiplocCode());
        }
        return found;
    }

    public MutableStation getMutableStationFor(RailLocationRecord record) {
        final String tiplocCode = record.getTiplocCode();
        return tiplocMap.get(tiplocCode);
    }

    /***
     * diagnostic support only
     * @param tiploc must be a valid tiploc
     * @return the matching station
     */
    public Station getMutableStationForTiploc(IdFor<Station> tiploc) {
        final String tiplocAsText = tiploc.getGraphId();
        return tiplocMap.get(tiplocAsText);
    }
}

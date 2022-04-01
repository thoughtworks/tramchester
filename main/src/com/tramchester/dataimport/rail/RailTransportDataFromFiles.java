package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RailConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.RemoteDataRefreshed;
import com.tramchester.dataimport.UnzipFetchedData;
import com.tramchester.dataimport.loader.DirectDataSourceFactory;
import com.tramchester.dataimport.rail.records.PhysicalStationRecord;
import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.records.reference.RailInterchangeType;
import com.tramchester.dataimport.rail.repository.RailStationCRSRepository;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.factory.TransportEntityFactory;
import com.tramchester.domain.id.CompositeIdMap;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.NaptanRecord;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.repository.WriteableTransportData;
import com.tramchester.repository.naptan.NaptanRespository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class RailTransportDataFromFiles implements DirectDataSourceFactory.PopulatesContainer {
    private static final Logger logger = LoggerFactory.getLogger(RailTransportDataFromFiles.class);

    private final LoadRailStationRecords loadRailStationRecords;
    private final LoadRailTimetableRecords loadRailTimetableRecords;
    private final RailConfig railConfig;
    private final BoundingBox bounds;
    private final NaptanRespository naptanRespository;
    private final GraphFilterActive graphFilterActive;
    private final RemoteDataRefreshed remoteDataRefreshed;
    private final RailStationCRSRepository crsRepository;

    private final boolean enabled;

    @Inject
    public RailTransportDataFromFiles(RailDataRecordFactory factory, TramchesterConfig config,
                                      NaptanRespository naptanRespository,
                                      GraphFilterActive graphFilterActive, RemoteDataRefreshed remoteDataRefreshed,
                                      UnzipFetchedData.Ready ready, RailStationCRSRepository crsRepository) {
        bounds = config.getBounds();
        railConfig = config.getRailConfig();
        this.naptanRespository = naptanRespository;
        this.graphFilterActive = graphFilterActive;
        this.remoteDataRefreshed = remoteDataRefreshed;
        this.crsRepository = crsRepository;
        enabled = (railConfig!=null);
        if (enabled) {
            final Path dataPath = railConfig.getDataPath();
            Path stationsPath = dataPath.resolve(railConfig.getStations());
            Path timetablePath = dataPath.resolve(railConfig.getTimetable());
            loadRailStationRecords = new LoadRailStationRecords(stationsPath);
            loadRailTimetableRecords = new LoadRailTimetableRecords(timetablePath, factory);
        } else {
            loadRailStationRecords = null;
            loadRailTimetableRecords = null;
        }
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        if (enabled) {
            logger.info("Enabled");
        } else {
            logger.info("Disabled");
        }
        logger.info("started");
    }

    @Override
    public void loadInto(TransportDataContainer dataContainer) {
        logger.info("Load stations");
        Stream<PhysicalStationRecord> physicalRecords = loadRailStationRecords.load();

        StationsTemporary stationsTemporary =  loadStations(physicalRecords);
        logger.info("Initially loaded " + stationsTemporary.count() + " stations" );

        logger.info("Load timetable");
        Stream<RailTimetableRecord> timetableRecords = loadRailTimetableRecords.load();
        processTimetableRecords(stationsTemporary, dataContainer, timetableRecords);

        stationsTemporary.getShouldInclude().forEach(dataContainer::addStation);

        crsRepository.keepOnly(dataContainer.getStations());
        logger.info("Retained " + stationsTemporary.countNeeded() + " stations of " + stationsTemporary.count());

        stationsTemporary.clear();
    }

    @Override
    public DataSourceInfo getDataSourceInfo() {
        if (!enabled) {
            throw new RuntimeException("Not enabled");
        }

        if (!remoteDataRefreshed.hasFileFor(DataSourceID.rail)) {
            String message = "Missing data source file for " + DataSourceID.rail;
            logger.error(message);
            throw new RuntimeException(message);
        }
        Path downloadedZip = remoteDataRefreshed.fileFor(DataSourceID.rail);

        FetchFileModTime fileModTime = new FetchFileModTime();
        LocalDateTime modTime = fileModTime.getFor(downloadedZip);
        final DataSourceInfo dataSourceInfo = new DataSourceInfo(railConfig.getDataSourceId(),
                railConfig.getVersion(), modTime, railConfig.getModes());
        logger.info("Generated  " + dataSourceInfo);
        return dataSourceInfo;
    }

    private void processTimetableRecords(StationsTemporary stationsTemporary, WriteableTransportData dataContainer,
                                         Stream<RailTimetableRecord> recordStream) {
        logger.info("Process timetable stream");
        RailTimetableMapper mapper = new RailTimetableMapper(stationsTemporary, dataContainer, railConfig, graphFilterActive);
        recordStream.forEach(mapper::seen);
        mapper.reportDiagnostics();
    }

    private StationsTemporary loadStations(Stream<PhysicalStationRecord> physicalRecords) {

        Stream<PhysicalStationRecord> validRecords = physicalRecords.filter(this::validRecord);

        Set<String> outOfBounds = new HashSet<>();

        // bit of a hack to avoid processing the stream twice
        Stream<PhysicalStationRecord> withinBounds = validRecords.
                filter(physicalStationRecord -> {
                    if (locationWithinBounds(physicalStationRecord)) {
                        return true;
                    } else {
                        outOfBounds.add(physicalStationRecord.getTiplocCode());
                        return false;
                    }
                });

        StationsTemporary stationsTemporary = new StationsTemporary(outOfBounds);

        withinBounds.
                map(this::createStationAndRecordCRS).
                forEach(stationsTemporary::addStation);

        return stationsTemporary;
    }

    private boolean locationWithinBounds(PhysicalStationRecord record) {
        GridPosition gridPosition = convertToOsGrid(record.getEasting(), record.getNorthing());
        if (gridPosition.isValid()) {
            return bounds.contained(gridPosition);
        }
        logger.debug("station out of bounds " + record.getName() + " " + record.getTiplocCode());
        return false;
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

    private MutableStation createStationAndRecordCRS(PhysicalStationRecord record) {
        IdFor<Station> id = StringIdFor.createId(record.getTiplocCode());

        String name = record.getName();
        GridPosition grid = GridPosition.Invalid;
        IdFor<NaptanArea> areaId = IdFor.invalid();
        boolean isInterchange = (record.getRailInterchangeType()!= RailInterchangeType.None);

        if (naptanRespository.containsTiploc(id)) {
            // prefer naptan data if available
            NaptanRecord stopsData = naptanRespository.getForTiploc(id);
            grid = stopsData.getGridPosition();
            name = stopsData.getName();
            areaId = TransportEntityFactory.chooseArea(naptanRespository, stopsData.getAreaCodes());
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

        final MutableStation mutableStation = new MutableStation(id, areaId, name, latLong, grid, DataSourceID.rail, isInterchange, minChangeTime);

        crsRepository.putCRS(mutableStation, record.getCRS());
        return mutableStation;
    }

    private GridPosition convertToOsGrid(int easting, int northing) {
        return new GridPosition(easting* 100L, northing* 100L);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static class StationsTemporary {
        private final CompositeIdMap<Station, MutableStation> stations;
        private final IdSet<Station> include;
        private final Set<String> outOfBounds;

        private StationsTemporary(Set<String> outOfBounds) {
            this.outOfBounds = outOfBounds;
            stations = new CompositeIdMap<>();
            include = new IdSet<>();
        }

        public void addStation(MutableStation mutableStation) {
            stations.add(mutableStation);
        }

        public Set<MutableStation> getShouldInclude() {
            return stations.getValues().stream().
                    filter(station -> include.contains(station.getId())).
                    collect(Collectors.toSet());
        }

        public boolean hasStationId(IdFor<Station> stationId) {
            return stations.hasId(stationId);
        }

        public MutableStation getMutableStation(IdFor<Station> stationId) {
            return stations.get(stationId);
        }

        public void markAsNeeded(Station station) {
            include.add(station.getId());
        }

        public void clear() {
            stations.clear();
            include.clear();
            outOfBounds.clear();
        }

        public int count() {
            return stations.size();
        }

        public int countNeeded() {
            return include.size();
        }

        public boolean wasOutOfBounds(String tiplocCode) {
            return outOfBounds.contains(tiplocCode);
        }
    }

}

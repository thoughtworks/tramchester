package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RailConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.RemoteDataRefreshed;
import com.tramchester.dataimport.loader.DirectDataSourceFactory;
import com.tramchester.dataimport.rail.records.PhysicalStationRecord;
import com.tramchester.dataimport.rail.records.RailLocationRecord;
import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.records.reference.RailInterchangeType;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
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
import com.tramchester.repository.naptan.NaptanRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class RailTransportDataFromFiles implements DirectDataSourceFactory.PopulatesContainer {
    private static final Logger logger = LoggerFactory.getLogger(RailTransportDataFromFiles.class);

    private final RailConfig railConfig;
    private final RemoteDataRefreshed remoteDataRefreshed;

    private final boolean enabled;
    private final BoundingBox bounds;
    private final Loader loader;

    @Inject
    public RailTransportDataFromFiles(ProvidesRailStationRecords providesRailStationRecords,
                                      LoadRailTimetableRecords loadRailTimetableRecords,
                                      TramchesterConfig config, NaptanRepository naptanRepository,
                                      GraphFilterActive graphFilterActive, RemoteDataRefreshed remoteDataRefreshed,
                                      RailStationCRSRepository crsRepository, RailRouteIdRepository railRouteRepository) {

        this.remoteDataRefreshed = remoteDataRefreshed;

        enabled = config.hasRailConfig();

        railConfig = config.getRailConfig();
        bounds = config.getBounds();

        loader = new Loader(providesRailStationRecords, loadRailTimetableRecords, railRouteRepository, crsRepository,
                naptanRepository, railConfig, graphFilterActive);
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
        if (!enabled) {
            logger.info("Disabled");
            return;
        }

        loader.loadInto(dataContainer, bounds);
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

    public boolean isEnabled() {
        return enabled;
    }

    public static class Loader {

        private final ProvidesRailStationRecords providesRailStationRecords;
        private final ProvidesRailTimetableRecords providesRailTimetableRecords;
        private final RailRouteIdRepository railRouteRepository;
        private final RailStationCRSRepository crsRepository;
        private final NaptanRepository naptanRepository;
        private final RailConfig railConfig;
        private final GraphFilterActive graphFilterActive;

        public Loader(ProvidesRailStationRecords providesRailStationRecords, ProvidesRailTimetableRecords providesRailTimetableRecords,
                      RailRouteIdRepository railRouteRepository, RailStationCRSRepository crsRepository,
                      NaptanRepository naptanRepository, RailConfig railConfig,
                      GraphFilterActive graphFilterActive) {
            this.providesRailStationRecords = providesRailStationRecords;
            this.providesRailTimetableRecords = providesRailTimetableRecords;
            this.railRouteRepository = railRouteRepository;
            this.crsRepository = crsRepository;
            this.naptanRepository = naptanRepository;
            this.railConfig = railConfig;
            this.graphFilterActive = graphFilterActive;
        }

        public void loadInto(TransportDataContainer dataContainer, BoundingBox bounds) {

            logger.info("Load stations");
            Stream<PhysicalStationRecord> physicalRecords = providesRailStationRecords.load();

            StationsTemporary stationsTemporary =  loadStations(physicalRecords);
            logger.info("Initially loaded " + stationsTemporary.count() + " stations" );

            logger.info("Load timetable");

            Stream<RailTimetableRecord> timetableRecords = providesRailTimetableRecords.load();
            processTimetableRecords(stationsTemporary, dataContainer, timetableRecords, bounds, railRouteRepository);

            stationsTemporary.getShouldInclude().forEach(dataContainer::addStation);

            logger.info("Retained " + stationsTemporary.countNeeded() + " stations of " + stationsTemporary.count());

            stationsTemporary.clear();
        }

        private StationsTemporary loadStations(Stream<PhysicalStationRecord> physicalRecords) {

            Stream<Pair<MutableStation, String>> railStations = physicalRecords.
                    filter(this::validRecord).
                    map(record -> Pair.of(createStationFor(record), record.getCRS()));

            StationsTemporary stationsTemporary = new StationsTemporary();

            railStations.forEach(railStation -> {
                final MutableStation mutableStation = railStation.getKey();
                stationsTemporary.addStation(mutableStation);
                crsRepository.putCRS(mutableStation, railStation.getValue());
            });

            return stationsTemporary;
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

        private void processTimetableRecords(StationsTemporary stationsTemporary, WriteableTransportData dataContainer,
                                             Stream<RailTimetableRecord> recordStream, BoundingBox bounds, RailRouteIdRepository railRouteRepository) {
            logger.info("Process timetable stream");
            RailTimetableMapper mapper = new RailTimetableMapper(stationsTemporary, dataContainer,
                    railConfig, graphFilterActive, bounds, railRouteRepository);
            recordStream.forEach(mapper::seen);
            mapper.reportDiagnostics();
            railRouteRepository.dispose();
        }

        private MutableStation createStationFor(PhysicalStationRecord record) {
            IdFor<Station> id = StringIdFor.createId(record.getTiplocCode());

            String name = record.getName();
            GridPosition grid = GridPosition.Invalid;
            IdFor<NaptanArea> areaId = IdFor.invalid();
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
    }

    /***
     * Temp holding for stations, so the final transport container only has the stations actually required
     */
    public static class StationsTemporary {
        private final CompositeIdMap<Station, MutableStation> stations;
        private final IdSet<Station> include;

        private StationsTemporary() {
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

        private boolean hasStationId(IdFor<Station> stationId) {
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
        }

        public int count() {
            return stations.size();
        }

        public int countNeeded() {
            return include.size();
        }

        public boolean isLoadedFor(RailLocationRecord record) {
            final String tiplocCode = record.getTiplocCode();
            IdFor<Station> stationId = StringIdFor.createId(tiplocCode);
            return hasStationId(stationId);
        }

        public MutableStation getMutableStationFor(RailLocationRecord record) {
            final String tiplocCode = record.getTiplocCode();
            IdFor<Station> stationId = StringIdFor.createId(tiplocCode);
            return getMutableStation(stationId);
        }

    }

}

package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RailConfig;
import com.tramchester.config.RemoteDataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.NaPTAN.StopsData;
import com.tramchester.dataimport.loader.DirectDataSourceFactory;
import com.tramchester.dataimport.rail.records.PhysicalStationRecord;
import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.records.reference.RailInterchangeType;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.repository.WriteableTransportData;
import com.tramchester.repository.naptan.NaptanRespository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@LazySingleton
public class LoadRailTransportData implements DirectDataSourceFactory.PopulatesContainer {
    private static final Logger logger = LoggerFactory.getLogger(LoadRailTransportData.class);

    private final RailStationDataFromFile railStationDataFromFile;
    private final RailTimetableDataFromFile railTimetableDataFromFile;
    private final boolean enabled;
    private final RailConfig railConfig;
    private final RemoteDataSourceConfig railRemoteSourceConfig;
    private final BoundingBox bounds;
    private final NaptanRespository naptanRespository;

    @Inject
    public LoadRailTransportData(RailDataRecordFactory factory, TramchesterConfig config, NaptanRespository naptanRespository) {
        bounds = config.getBounds();
        railConfig = config.getRailConfig();
        this.naptanRespository = naptanRespository;
        enabled = (railConfig!=null);
        if (enabled) {
            railRemoteSourceConfig = config.getDataRemoteSourceConfig(railConfig.getDataSourceId());
            final Path dataPath = railConfig.getDataPath();
            Path stationsPath = dataPath.resolve(railConfig.getStations());
            Path timetablePath = dataPath.resolve(railConfig.getTimetable());
            railStationDataFromFile = new RailStationDataFromFile(stationsPath);
            railTimetableDataFromFile = new RailTimetableDataFromFile(timetablePath, factory);
        } else {
            railStationDataFromFile = null;
            railTimetableDataFromFile = null;
            railRemoteSourceConfig = null;
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
        Stream<PhysicalStationRecord> physicalRecords = railStationDataFromFile.load();
        addStations(dataContainer, physicalRecords);

        logger.info("Load timetable");
        Stream<RailTimetableRecord> timetableRecords = railTimetableDataFromFile.load();
        processTimetableRecords(dataContainer, timetableRecords);

        logger.info("Remove stations no transport mode");
        // use association with the timetable data to populate station transport modes, so need to remove invalid ones
        // afterwards
        Set<Station> missingTransportModes = dataContainer.getStationStream().filter(station -> station.getTransportModes().isEmpty()).
                collect(Collectors.toSet());
        if (!missingTransportModes.isEmpty()) {
            logger.info("Removing " + missingTransportModes.size() +" stations");
            dataContainer.removeStations(missingTransportModes);
        }

        // this case ought not to happen as filter by mode within timetable load
        Set<Station> wrongTransportModes = dataContainer.getStationStream().filter(station -> station.getTransportModes().isEmpty()).
                collect(Collectors.toSet());
        if (!wrongTransportModes.isEmpty()) {
            logger.error("Unexpected " + wrongTransportModes.size() + " stations with incorrect modes");
            dataContainer.removeStations(wrongTransportModes);
        }

    }

    @Override
    public DataSourceInfo getDataSourceInfo() {
        if (!enabled) {
            throw new RuntimeException("Not enabled");
        }
        String zipFilename = railRemoteSourceConfig.getDownloadFilename();
        Path downloadedZip = railRemoteSourceConfig.getDataPath().resolve(zipFilename);
        FetchFileModTime fileModTime = new FetchFileModTime();
        LocalDateTime modTime = fileModTime.getFor(downloadedZip);
        final DataSourceInfo dataSourceInfo = new DataSourceInfo(railConfig.getDataSourceId(), zipFilename, modTime, railConfig.getModes());
        logger.info("Generated  " + dataSourceInfo);
        return dataSourceInfo;
    }

    private void processTimetableRecords(WriteableTransportData dataContainer, Stream<RailTimetableRecord> recordStream) {
        logger.info("Process timetable stream");
        RailTimetableMapper mapper = new RailTimetableMapper(dataContainer, railConfig);
        recordStream.forEach(mapper::seen);
        mapper.reportDiagnostics();
    }

    private void addStations(WriteableTransportData dataContainer, Stream<PhysicalStationRecord> physicalRecords) {
        physicalRecords.
                filter(this::validRecord).
                filter(this::locationWithinBounds).
                map(this::createStationFor).
                forEach(dataContainer::addStation);
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

    private MutableStation createStationFor(PhysicalStationRecord record) {
        IdFor<Station> id = StringIdFor.createId(record.getTiplocCode());

        String name = record.getName();
        String area = "";
        GridPosition grid = GridPosition.Invalid;
        boolean isInterchange = (record.getRailInterchangeType()!= RailInterchangeType.None);

        if (naptanRespository.containsTiploc(id)) {
            // prefer naptan data if available
            StopsData stopsData = naptanRespository.getForTiploc(id);
            grid = stopsData.getGridPosition();
            name = stopsData.getCommonName();
            area = stopsData.getLocalityName();
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

        return new MutableStation(id, area, name, latLong, grid, DataSourceID.rail, isInterchange);
    }

    private GridPosition convertToOsGrid(int easting, int northing) {
        return new GridPosition(easting* 100L, northing* 100L);
    }

    public boolean isEnabled() {
        return enabled;
    }

}

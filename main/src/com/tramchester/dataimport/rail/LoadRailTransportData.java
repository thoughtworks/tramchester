package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.config.RailConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.loader.DirectDataSourceFactory;
import com.tramchester.dataimport.rail.records.PhysicalStationRecord;
import com.tramchester.dataimport.rail.records.RailTimetableRecord;
import com.tramchester.dataimport.rail.records.reference.RailInterchangeType;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.TransportDataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.nio.file.Path;
import java.util.stream.Stream;

@LazySingleton
public class LoadRailTransportData implements DirectDataSourceFactory.PopulatesContainer {
    private static final Logger logger = LoggerFactory.getLogger(LoadRailTransportData.class);

    private final RailStationDataFromFile railStationDataFromFile;
    private final RailTimetableDataFromFile railTimetableDataFromFile;
    private final boolean enabled;

    @Inject
    public LoadRailTransportData(RailDataRecordFactory factory, TramchesterConfig config) {
        final RailConfig railConfig = config.getRailConfig();
        enabled = (railConfig!=null);
        if (enabled) {
            final Path dataPath = railConfig.getDataPath();
            Path stationsPath = dataPath.resolve(railConfig.getStations());
            Path timetablePath = dataPath.resolve(railConfig.getTimetable());
            railStationDataFromFile = new RailStationDataFromFile(stationsPath);
            railTimetableDataFromFile = new RailTimetableDataFromFile(timetablePath, factory);
        } else {
            railStationDataFromFile = null;
            railTimetableDataFromFile = null;
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
    }

    private void processTimetableRecords(TransportDataContainer dataContainer, Stream<RailTimetableRecord> recordStream) {
        logger.info("Process timetable stream");
        RailTimetableMapper mapper = new RailTimetableMapper(dataContainer);
        recordStream.forEach(mapper::seen);
        dataContainer.reportNumbers();
        mapper.reportDiagnostics();
    }

    private void addStations(TransportDataContainer dataContainer, Stream<PhysicalStationRecord> physicalRecords) {
        physicalRecords.
                filter(this::validRecord).
                map(this::createStationFor).
                forEach(dataContainer::addStation);
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

        GridPosition grid;
        LatLong latLong;
        if (record.getEasting()==Integer.MIN_VALUE || record.getNorthing()==Integer.MIN_VALUE) {
            // have missing grid for this station, was 00000
            grid = GridPosition.Invalid;
            latLong = LatLong.Invalid;
        }
        else {
            grid = convertToOsGrid(record.getEasting(), record.getNorthing());
            latLong = CoordinateTransforms.getLatLong(grid);
        }

        boolean isInterchange = (record.getRailInterchangeType()!= RailInterchangeType.None);

        return new MutableStation(id, area, name, latLong, grid, DataSourceID.rail, isInterchange);
    }

    private GridPosition convertToOsGrid(int easting, int northing) {
        return new GridPosition(easting* 100L, northing* 100L);
    }

    public boolean isEnabled() {
        return enabled;
    }

}

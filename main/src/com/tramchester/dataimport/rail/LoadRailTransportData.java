package com.tramchester.dataimport.rail;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.loader.TransportDataFactory;
import com.tramchester.dataimport.rail.records.PhysicalStationRecord;
import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.nio.file.Path;
import java.util.stream.Stream;

@LazySingleton
public class LoadRailTransportData implements TransportDataFactory {
    private static final Logger logger = LoggerFactory.getLogger(LoadRailTransportData.class);

    private final TransportDataContainer dataContainer;
    final private RailStationDataFromFile railStationDataFromFile;

    @Inject
    public LoadRailTransportData(ProvidesNow providesNow) {
        // TODO
        Path railStationFile = Path.of("data", "rail", "ttisf187.msn");

        this.railStationDataFromFile = new RailStationDataFromFile(railStationFile);
        String sourceName = "rail";
        dataContainer = new TransportDataContainer(providesNow, sourceName);
    }

    @PostConstruct
    public void start() {
        Stream<PhysicalStationRecord> physicalRecords = railStationDataFromFile.load();
        addStations(physicalRecords);
    }

    private void addStations(Stream<PhysicalStationRecord> physicalRecords) {
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

    private Station createStationFor(PhysicalStationRecord record) {
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

        return new MutableStation(id, area, name, latLong, grid, DataSourceID.rail);
    }

    private GridPosition convertToOsGrid(int easting, int northing) {
        return new GridPosition(easting* 100L, northing* 100L);
    }

    @Override
    public TransportData getData() {
        return dataContainer;
    }
}

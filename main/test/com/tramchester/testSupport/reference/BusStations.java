package com.tramchester.testSupport.reference;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestStation;
import com.tramchester.testSupport.TestStations;
import org.jetbrains.annotations.NotNull;
import org.opengis.referencing.operation.TransformException;

public enum BusStations implements TestStations {

    AltrinchamInterchange("1800AMIC001", "Altrincham", "Altrincham, Altrincham Interchange", new LatLong(53.38745, -2.34771)),
    StockportBusStation("1800STBS001", "Stockport", "Stockport, Stockport Bus Station", new LatLong(53.4091,-2.16293)),
    ShudehillInterchange("1800SHIC001", "Shudehill", "Shudehill, Shudehill Interchange",  new LatLong(53.48557, -2.23827)),
    ManchesterAirportStation("1800MABS001", "Manchester Airport", "Manchester Airport, Manchester Airport The Station",
            new LatLong(53.3656, -2.27242)),
    KnutsfordStationStand3("0600MA6022", "Knutsford", "Knutsford, Bus Station (Stand 3)",  new LatLong(53.30245, -2.37551)),
    BuryInterchange("1800BYIC001", "Bury", "Bury, at Bury Interchange",  new LatLong(53.59134, -2.29706)),
    PiccadilyStationStopA("1800EB01201", "Manchester City Centre", "Manchester City Centre, Piccadilly Station (Stop A)",
            new LatLong(53.47683, -2.23146)),
    PiccadillyGardensStopH("1800SB05001", "Piccadilly Gardens", "Piccadilly Gardens, Piccadilly Gardens (Stop H)",
            new LatLong(53.48063,-2.23825)),
    PiccadillyGardensStopN("1800SB04721", "Piccadilly Gardens", "Piccadilly Gardens, Piccadilly Gardens (Stop N)",
            new LatLong(53.48017, -2.23723)),
    MacclefieldBusStationBay1("0600MA6154", "Macclesfield", "Macclesfield, Bus Station (Bay 1)",
            new LatLong(53.25831, -2.12502));

    private final TestStation station;

    BusStations(String id, String area, String name, LatLong latlong) {
        try {
            @NotNull GridPosition grid = CoordinateTransforms.getGridPosition(latlong);
            this.station = new TestStation(id, area, name, latlong, grid, TransportMode.Bus);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }
    }

    public static Station of(BusStations enumValue) {
        return enumValue.station;
    }

    public static Station real(StationRepository stationRepository, BusStations station) {
        return stationRepository.getStationById(station.getId());
    }

    @Override
    public IdFor<Station> getId() {
        return station.getId();
    }

    @Override
    public GraphPropertyKey getProp() {
        return station.getProp();
    }

    public String forDTO() {
        return getId().forDTO();
    }
}

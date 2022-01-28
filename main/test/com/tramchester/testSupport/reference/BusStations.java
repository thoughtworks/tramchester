package com.tramchester.testSupport.reference;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestStation;
import com.tramchester.testSupport.TestStations;
import org.jetbrains.annotations.NotNull;

// TODO

@Deprecated
public enum BusStations implements TestStations {

    StopAtAltrinchamInterchange("1800AMIC0C1", "Altrincham Interchange",
            new LatLong(53.38745, -2.34771)),
    // stockport bus stations is no more, at least for now
//    StopAtStockportBusStation("1800SGQ0021", "Stockport", "Stockport Bus Station",
//            new LatLong(53.4091,-2.16443890806)),
    StopAtHeatonLaneStockportBusStation("1800STIC011", "Stockport Heaton Lane Bus Station",
            new LatLong(53.41036253703,-2.16501729098)),
    StopAtShudehillInterchange("1800SHIC0C1", "Shudehill Interchange",
            new LatLong(53.48557, -2.23827)),
    ManchesterAirportStation("1800MABS0E1", "Manchester Airport The Station",
            new LatLong(53.3656, -2.27242)),
    KnutsfordStationStand3("0600MA6022", "Bus Station",
            new LatLong(53.30245, -2.37551)),
    BuryInterchange("1800BYIC0C1", "Bury Interchange",
            new LatLong(53.59134, -2.29706)),
    PiccadilyStationStopA("1800EB01201", "Manchester Piccadilly Rail Station",
            new LatLong(53.47683, -2.23146)),
    PiccadillyGardensStopH("1800SB05001", "Piccadilly Gardens",
            new LatLong(53.48063,-2.23825)),
    PiccadillyGardensStopN("1800SB04721", "Piccadilly Gardens",
            new LatLong(53.48017, -2.23723)),
    // No longer in the data?
//    MacclefieldBusStationBay1("0600MA6154", "Macclesfield", "Macclesfield, Bus Station (Bay 1)",
//            new LatLong(53.25831, -2.12502)),
    StockportAtAldi("1800SG15721", "Aldi",
            new LatLong(53.41115, -2.15221)),
    StockportNewbridgeLane("1800SG15561", "Newbridge Lane",
            new LatLong(53.41149, -2.15438));

    private final TestStation station;

    BusStations(String id, String name, LatLong latlong) {
        @NotNull GridPosition grid = CoordinateTransforms.getGridPosition(latlong);
        IdFor<NaptanArea> areaId = IdFor.invalid();
        this.station = new TestStation(id, areaId, name, latlong, grid, TransportMode.Bus, DataSourceID.tfgm);
    }

    @Deprecated
    public static Station of(BusStations enumValue) {
        return enumValue.station;
    }

    public Station from(StationRepository repository) {
        return repository.getStationById(getId());
    }

    @Override
    public IdFor<Station> getId() {
        return station.getId();
    }
    
    public String getName() {
        return station.getName();
    }

    @Deprecated
    public String forDTO() {
        return getId().forDTO();
    }

    public enum Composites {
        // closed for rebuilding....
        //StockportBusStation("Stockport Bus Station"),
        StockportTempBusStation("Stockport Heaton Lane Bus Station"),
        AltrinchamInterchange("Altrincham Interchange");

        private final String compositeName;

        Composites(String compositeName) {
            this.compositeName = compositeName;
        }

        public String getName() {
            return compositeName;
        }
    }

    public static final IdFor<NaptanArea> KnutfordStationAreaId = StringIdFor.createId("060GMA294");
}

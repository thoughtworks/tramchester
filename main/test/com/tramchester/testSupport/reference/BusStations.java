package com.tramchester.testSupport.reference;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import org.jetbrains.annotations.NotNull;

public enum BusStations implements FakeStation {

    StopAtAltrinchamInterchange("1800AMIC0C1", "Altrincham Interchange",
            new LatLong(53.38745, -2.34771)),
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
    StockportAtAldi("1800SG15721", "Aldi",
            new LatLong(53.41115, -2.15221)),
    StockportNewbridgeLane("1800SG15561", "Newbridge Lane",
            new LatLong(53.41149, -2.15438));

    // stockport bus stations is no more, at least for now
//    StopAtStockportBusStation("1800SGQ0021", "Stockport", "Stockport Bus Station",
//            new LatLong(53.4091,-2.16443890806)),

    // No longer in the data?
//    MacclefieldBusStationBay1("0600MA6154", "Macclesfield", "Macclesfield, Bus Station (Bay 1)",
//            new LatLong(53.25831, -2.12502)),

    private final String id;
    private final String name;
    private final LatLong latlong;

    BusStations(String id, String name, LatLong latlong) {
        this.id = id;
        this.name = name;
        this.latlong = latlong;
    }

    @Deprecated
    public static Station of(BusStations enumValue) {
        return enumValue.fake();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LatLong getLatLong() {
        return latlong;
    }

    @Override
    public String getRawId() {
        return id;
    }

    @Override
    public Station fake() {
        return createMutable();
    }

    @Override
    public IdForDTO getIdForDTO() {
        return new IdForDTO(id);
    }

    @NotNull
    private MutableStation createMutable() {
        GridPosition grid = CoordinateTransforms.getGridPosition(latlong);
        return new MutableStation(getId(), NaptanArea.invalidId(), name, latlong, grid, DataSourceID.tfgm);
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

    public static final IdFor<NaptanArea> KnutfordStationAreaId = NaptanArea.createId("060GMA294");
}

package com.tramchester.testSupport;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import org.jetbrains.annotations.NotNull;
import org.opengis.referencing.operation.TransformException;

public enum TramStations {

    Altrincham("9400ZZMAALT", "Altrincham", "Altrincham", new LatLong(-1, -1)),
    Ashton("9400ZZMAAUL", "Ashton-under-Lyne", "Ashton-Under-Lyne", new LatLong(-1, -1)),
    ManAirport("9400ZZMAAIR", "Manchester Airport", "Manchester Airport", new LatLong(-1, -1)),
    TraffordBar("9400ZZMATRA", "Trafford Bar", "Trafford Bar", new LatLong(-1, -1)),
    VeloPark("9400ZZMAVPK", "Sportcity", "Velopark", new LatLong(-1, -1)),
    Cornbrook("9400ZZMACRN", "Cornbrook", "Cornbrook", new LatLong(-1, -1)),
    Etihad("9400ZZMAECS", "Sportcity", "Etihad Campus", new LatLong(-1, -1)),
    Piccadilly("9400ZZMAPIC", "Manchester City Centre", "Piccadilly", new LatLong(-1, -1)),
    HoltTown("9400ZZMAHTN", "Holt Town", "Holt Town", new LatLong(-1, -1)),
    Eccles("9400ZZMAECC", "Eccles", "Eccles", new LatLong(-1, -1)),
    Bury("9400ZZMABUR", "Bury", "Bury", new LatLong(-1, -1)),
    EastDidsbury("9400ZZMAEDY", "East Didsbury", "East Didsbury", new LatLong(-1, -1)),
    Rochdale("9400ZZMARIN", "Rochdale", "Rochdale Town Centre", new LatLong(-1, -1)),
    Pomona("9400ZZMAPOM", "Salford Quays", "Pomona", new LatLong(-1, -1)),
    Deansgate("9400ZZMAGMX", "Manchester City Centre", "Deansgate-Castlefield", new LatLong(-1, -1)),
    Broadway("9400ZZMABWY", "Broadway", "Broadway", new LatLong(-1, -1)),
    PiccadillyGardens("9400ZZMAPGD", "Piccadilly Gardens", "Piccadilly Gardens", new LatLong(-1, -1)),
    ExchangeSquare("9400ZZMAEXS", "Manchester City Centre", "Exchange Square", new LatLong(-1, -1)),
    Victoria("9400ZZMAVIC", "Manchester City Centre", "Victoria", new LatLong(-1, -1)),
    NavigationRoad("9400ZZMANAV", "Navigation Road", "Navigation Road", new LatLong(-1, -1)),
    ShawAndCrompton("9400ZZMASHA", "Shaw", "Shaw and Crompton", new LatLong(-1, -1)),
    HarbourCity("9400ZZMAHCY", "The Lowry", "Harbour City", new LatLong(-1, -1)),
    StPetersSquare("9400ZZMASTP", "Manchester City Centre", "St Peter's Square", new LatLong(-1, -1)),
    MarketStreet("9400ZZMAMKT", "Manchester City Centre", "Market Street", new LatLong(-1, -1)),
    MediaCityUK("9400ZZMAMCU", "MediaCityUK", "MediaCityUK", new LatLong(-1, -1)),
    StWerburghsRoad("9400ZZMASTW", "Chorlton", "St Werburgh's Road", new LatLong(-1, -1)),
    Shudehill("9400ZZMASHU","Manchester City Centre", "Shudehill", new LatLong(-1, -1)),
    Monsall("9400ZZMAMON", "Monsall", "Monsall", new LatLong(-1, -1)),
    ExchangeQuay("9400ZZMAEXC", "Salford Quays", "Exchange Quay", new LatLong(-1, -1)),
    SalfordQuay("9400ZZMASQY", "Salford Quays", "Salford Quays", new LatLong(-1, -1)),
    Anchorage("9400ZZMAANC", "Salford Quays", "Anchorage", new LatLong(-1, -1)),
    HeatonPark("9400ZZMAHEA", "Heaton Park", "Heaton Park", new LatLong(-1, -1)),
    BurtonRoad("9400ZZMABNR", "West Didsbury", "Burton Road", new LatLong(-1, -1)),
    RochdaleRail("9400ZZMARRS", "Rochdale", "Rochdale Railway Station", new LatLong(-1, -1)),
    Intu("9400ZZMATRC", "The Trafford Centre", "intu Trafford Centre", new LatLong(-1, -1));

    public static Station of(TramStations enumValue) {
        return enumValue.station;
    }

    private final Station station;

    TramStations(String id, String area, String name, LatLong latlong) {
        try {
            @NotNull GridPosition grid = CoordinateTransforms.getGridPosition(latlong);
            this.station = new TestStation(id, area, name, latlong, grid, TransportMode.Tram);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }
    }

    private static class TestStation extends Station {

        private final TransportMode mode;

        public TestStation(String id, String area, String stationName, LatLong latLong, GridPosition gridPosition, TransportMode mode) {
            super(IdFor.createId(id), area, stationName, latLong, gridPosition);
            this.mode = mode;
        }

        @Override
        public TransportMode getTransportMode() {
            return mode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;

            if (!(o instanceof Station)) {
                return false;
            }

            Station station = (Station) o;

            return getId().equals(station.getId());
        }



    }
}

package com.tramchester.testSupport.reference;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.testSupport.TestStation;
import com.tramchester.testSupport.TestStations;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum TramStations implements TestStations {

    Altrincham("9400ZZMAALT", "Altrincham", "Altrincham", pos(53.38726, -2.34755)),
    Ashton("9400ZZMAAUL", "Ashton-under-Lyne", "Ashton-Under-Lyne", pos(53.49035, -2.09798)),
    ManAirport("9400ZZMAAIR", "Manchester Airport", "Manchester Airport", pos(53.36541, -2.27222)),
    TraffordBar("9400ZZMATRA", "Trafford Bar", "Trafford Bar", pos(53.46163, -2.27762)),
    VeloPark("9400ZZMAVPK", "Sportcity", "Velopark", pos(53.48224, -2.1933)),
    Cornbrook("9400ZZMACRN", "Cornbrook", "Cornbrook", pos(53.46996, -2.26768)),
    Etihad("9400ZZMAECS", "Sportcity", "Etihad Campus", pos(53.48535, -2.20221)),
    Piccadilly("9400ZZMAPIC", "Manchester City Centre", "Piccadilly", pos(53.47731, -2.23121)),
    HoltTown("9400ZZMAHTN", "Holt Town", "Holt Town", pos(53.4832, -2.21228)),
    Bury("9400ZZMABUR", "Bury", "Bury", pos(53.59082, -2.29726)),
    EastDidsbury("9400ZZMAEDY", "East Didsbury", "East Didsbury", pos(53.41208, -2.21739)),
    Rochdale("9400ZZMARIN", "Rochdale", "Rochdale Town Centre", pos(53.61736, -2.15509)),
    Pomona("9400ZZMAPOM", "Salford Quays", "Pomona", pos(53.46521, -2.27791)),
    Deansgate("9400ZZMAGMX", "Manchester City Centre", "Deansgate-Castlefield", pos(53.47476, -2.25018)),
    Broadway("9400ZZMABWY", "Broadway", "Broadway", pos(53.47478, -2.29506)),
    PiccadillyGardens("9400ZZMAPGD", "Piccadilly Gardens", "Piccadilly Gardens", pos(53.48029, -2.23705)),
    ExchangeSquare("9400ZZMAEXS", "Manchester City Centre", "Exchange Square", pos(53.48439, -2.2427)),
    Victoria("9400ZZMAVIC", "Manchester City Centre", "Victoria", pos(53.48787, -2.24187)),
    NavigationRoad("9400ZZMANAV", "Navigation Road", "Navigation Road", pos(53.39589, -2.34331)),
    ShawAndCrompton("9400ZZMASHA", "Shaw", "Shaw and Crompton", pos(53.5763, -2.08963)),
    HarbourCity("9400ZZMAHCY", "The Lowry", "Harbour City", pos(53.47401, -2.29174)),
    StPetersSquare("9400ZZMASTP", "Manchester City Centre", "St Peter's Square", pos(53.47825, -2.24314)),
    MarketStreet("9400ZZMAMKT", "Manchester City Centre", "Market Street", pos(53.48192, -2.23883)),
    MediaCityUK("9400ZZMAMCU", "MediaCityUK", "MediaCityUK", pos(53.47214, -2.29733)),
    StWerburghsRoad("9400ZZMASTW", "Chorlton", "St Werburgh's Road", pos(53.4387, -2.26547)),
    Chorlton("9400ZZMACHO","Chorlton", "Chorlton", pos(53.44262, -2.27335) ),
    Shudehill("9400ZZMASHU","Manchester City Centre", "Shudehill", pos(53.48524, -2.23918)),
    Monsall("9400ZZMAMON", "Monsall", "Monsall", pos(53.50111, -2.21061)),
    ExchangeQuay("9400ZZMAEXC", "Salford Quays", "Exchange Quay", pos(53.46769, -2.28242)),
    SalfordQuay("9400ZZMASQY", "Salford Quays", "Salford Quays", pos(53.4703, -2.28407)),
    Anchorage("9400ZZMAANC", "Salford Quays", "Anchorage", pos(53.47425, -2.28607)),
    HeatonPark("9400ZZMAHEA", "Heaton Park", "Heaton Park", pos(53.53036, -2.26699)),
    BurtonRoad("9400ZZMABNR", "West Didsbury", "Burton Road", pos(53.42908, -2.24064)),
    RochdaleRail("9400ZZMARRS", "Rochdale", "Rochdale Railway Station", pos(53.61102, -2.15449)),
    OldTrafford("9400ZZMAOLD", "Old Trafford", "Old Trafford", pos(53.45634, -2.28496)),
    Wharfside("9400ZZMAWFS", "Old Trafford", "Wharfside", pos(53.46625, -2.28748)),
    PeelHall("9400ZZMAPLL","Peel Hall", "Peel Hall", pos(53.37373, -2.25038)),
    TraffordCentre("9400ZZMATRC", "The Trafford Centre", "The Trafford Centre", pos(53.46782, -2.34751)),
    ImperialWarMuseum("9400ZZMAIWM", "Imperial War Museum", "Imperial War Museum", pos(53.46862272157,-2.29682786715)),
    Eccles("9400ZZMAECC", "Eccles", "Eccles", pos(53.48307, -2.33454));

    public static final Set<TramStations> EndOfTheLine = new HashSet<>(Arrays.asList(Altrincham,
            ManAirport,
            Eccles,
            EastDidsbury,
            Ashton,
            Rochdale,
            Bury,
            TraffordCentre));

    public static boolean isEndOfLine(HasId<Station> station) {
        return containedIn(station, EndOfTheLine);
    }

    private static boolean containedIn(HasId<Station> station, Set<TramStations> theSet) {
        Set<IdFor<Station>> ids = theSet.stream().map(TramStations::getId).collect(Collectors.toSet());
        return ids.contains(station.getId());
    }

    public static MutableStation of(TramStations enumValue) {
        return enumValue.station;
    }

    private static LatLong pos(double lat, double lon) {
        return new LatLong(lat, lon);
    }

    private final MutableStation station;

    TramStations(String id, String area, String name, LatLong latlong) {
        @NotNull GridPosition grid = CoordinateTransforms.getGridPosition(latlong);
        this.station = new TestStation(id, area, name, latlong, grid, TransportMode.Tram, DataSourceID.tfgm);
    }

    @Override
    public IdFor<Station> getId() {
        return station.getId();
    }

    @Override
    public GraphPropertyKey getProp() {
        return station.getProp();
    }

    public String getName() {
        return station.getName();
    }

    public LatLong getLatLong() {
        return station.getLatLong();
    }

    public String forDTO() {
        return station.forDTO();
    }

    public String getArea() {
        return station.getArea();
    }

}

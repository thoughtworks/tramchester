package com.tramchester.testSupport.reference;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum TramStations implements FakeStation {

    Altrincham("9400ZZMAALT", "Altrincham", pos(53.38726, -2.34755)),
    Ashton("9400ZZMAAUL", "Ashton-Under-Lyne", pos(53.49035, -2.09798)),
    ManAirport("9400ZZMAAIR", "Manchester Airport", pos(53.36541, -2.27222)),
    TraffordBar("9400ZZMATRA", "Trafford Bar", pos(53.46163, -2.27762)),
    VeloPark("9400ZZMAVPK", "Velopark", pos(53.48224, -2.1933)),
    Cornbrook("9400ZZMACRN", "Cornbrook", pos(53.46996, -2.26768)),
    Etihad("9400ZZMAECS", "Etihad Campus", pos(53.48535, -2.20221)),
    Piccadilly("9400ZZMAPIC", "Piccadilly", pos(53.47731, -2.23121)),
    HoltTown("9400ZZMAHTN", "Holt Town", pos(53.4832, -2.21228)),
    Bury("9400ZZMABUR", "Bury", pos(53.59082, -2.29726)),
    EastDidsbury("9400ZZMAEDY", "East Didsbury", pos(53.41208, -2.21739)),
    Rochdale("9400ZZMARIN", "Rochdale Town Centre", pos(53.61736, -2.15509)),
    Pomona("9400ZZMAPOM", "Pomona", pos(53.46521, -2.27791)),
    Deansgate("9400ZZMAGMX", "Deansgate-Castlefield", pos(53.47476, -2.25018)),
    Broadway("9400ZZMABWY", "Broadway", pos(53.47478, -2.29506)),
    PiccadillyGardens("9400ZZMAPGD", "Piccadilly Gardens", pos(53.48029, -2.23705)),
    ExchangeSquare("9400ZZMAEXS", "Exchange Square", pos(53.48439, -2.2427)),
    Victoria("9400ZZMAVIC", "Victoria", pos(53.48787, -2.24187)),
    NavigationRoad("9400ZZMANAV", "Navigation Road", pos(53.39589, -2.34331)),
    ShawAndCrompton("9400ZZMASHA", "Shaw and Crompton", pos(53.5763, -2.08963)),
    HarbourCity("9400ZZMAHCY", "Harbour City", pos(53.47401, -2.29174)),
    StPetersSquare("9400ZZMASTP", "St Peter's Square", pos(53.47825, -2.24314)),
    MarketStreet("9400ZZMAMKT", "Market Street", pos(53.48192, -2.23883)),
    MediaCityUK("9400ZZMAMCU", "MediaCityUK", pos(53.47214, -2.29733)),
    StWerburghsRoad("9400ZZMASTW", "St Werburgh's Road", pos(53.4387, -2.26547)),
    Chorlton("9400ZZMACHO", "Chorlton", pos(53.44262, -2.27335) ),
    Shudehill("9400ZZMASHU", "Shudehill", pos(53.48524, -2.23918)),
    Monsall("9400ZZMAMON", "Monsall", pos(53.50111, -2.21061)),
    ExchangeQuay("9400ZZMAEXC", "Exchange Quay", pos(53.46769, -2.28242)),
    SalfordQuay("9400ZZMASQY", "Salford Quays", pos(53.4703, -2.28407)),
    Anchorage("9400ZZMAANC", "Anchorage", pos(53.47425, -2.28607)),
    HeatonPark("9400ZZMAHEA", "Heaton Park", pos(53.53036, -2.26699)),
    BurtonRoad("9400ZZMABNR", "Burton Road", pos(53.42908, -2.24064)),
    RochdaleRail("9400ZZMARRS", "Rochdale Railway Station", pos(53.61102, -2.15449)),
    OldTrafford("9400ZZMAOLD", "Old Trafford", pos(53.45634, -2.28496)),
    Wharfside("9400ZZMAWFS", "Wharfside", pos(53.46625, -2.28748)),
    PeelHall("9400ZZMAPLL", "Peel Hall", pos(53.37373, -2.25038)),
    TraffordCentre("9400ZZMATRC", "The Trafford Centre", pos(53.46782, -2.34751)),
    ImperialWarMuseum("9400ZZMAIWM", "Imperial War Museum", pos(53.46862272157,-2.29682786715)),
    Eccles("9400ZZMAECC", "Eccles", pos(53.48307, -2.33454)),
    NewIslington("9400ZZMANIS", "New Islington", pos(53.48108550908, -2.21985483562)),
    Timperley("9400ZZMATIM", "Timperley", pos(53.40429833013,-2.33826968737)),
    Whitefield("9400ZZMAWFD", "Whitefield", pos(53.55113165424,-2.2951414371));

    public static final Set<TramStations> EndOfTheLine = new HashSet<>(Arrays.asList(Altrincham,
            ManAirport,
            Eccles,
            EastDidsbury,
            Ashton,
            Rochdale,
            Bury,
            TraffordCentre));

    private final String id;
    private final String name;
    private final LatLong latlong;

    TramStations(String id, String name, LatLong latlong) {
        this.id = id;
        this.name = name;
        this.latlong = latlong;
    }

    public static boolean isEndOfLine(Station station) {
        return containedIn(station.getId(), EndOfTheLine);
    }

    private static boolean containedIn(IdFor<Station> stationId, Set<TramStations> theSet) {
        IdSet<Station> ids = theSet.stream().map(TramStations::getId).collect(IdSet.idCollector());
        return ids.contains(stationId);
    }

    private static LatLong pos(double lat, double lon) {
        return new LatLong(lat, lon);
    }

//    public static Set<Station> allFrom(StationRepository stationRepository, TramStations... tramStations) {
//        return Arrays.stream(tramStations).
//                map(tramStation -> stationRepository.getStationById(tramStation.getId()))
//                .collect(Collectors.toSet());
//    }

    @Override
    public String getRawId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LatLong getLatLong() {
        return latlong;
    }

    public IdFor<Platform> createIdFor(String platform) {
        return PlatformId.createId(getId(), platform);
        //return Platform.createId(getRawId()+platform);
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

    public Station fakeWithPlatform(String platformNumber, LatLong latLong, DataSourceID dataSourceID,
                                    IdFor<NaptanArea> naptanAreaId) {
        MutableStation station = createMutable();
        PlatformId platformId = PlatformId.createId(station.getId(), platformNumber);
        final Platform platform = MutablePlatform.buildForTFGMTram(platformId, station,
                latLong, dataSourceID, naptanAreaId);
        station.addPlatform(platform);
        return station;

    }
}

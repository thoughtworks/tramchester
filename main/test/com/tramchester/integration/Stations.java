package com.tramchester.integration;

import com.tramchester.domain.Platform;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.LatLong;
import org.apache.commons.collections4.ListUtils;

import java.util.Arrays;
import java.util.List;

public class Stations {
    public static int NumberOf = 93;
    // TEST DATA For stations

    private static LatLong position = new LatLong(1,1);

    public static Station Altrincham = createStation("9400ZZMAALT", "Altrincham area", "Altrincham");
    public static Station Ashton = createStation("9400ZZMAAUL", "Ashton area", "Ashton-Under-Lyne");
    public static Station ManAirport = createStation("9400ZZMAAIR", "Airport", "Manchester Airport");
    public static Station TraffordBar = createStation("9400ZZMATRA", "Trafford", "Trafford Bar");
    public static Station VeloPark = createStation("9400ZZMAVPK", "Velopark area", "Velopark");
    public static Station Cornbrook = createStation("9400ZZMACRN", "Cornbrook area", "Cornbrook");
    public static Station Etihad = createStation("9400ZZMAECS", "Etihad area", "Etihad");
    public static Station Piccadilly = createStation("9400ZZMAPIC", "Piccadilly area", "Piccadilly");
    public static Station HoltTown = createStation("9400ZZMAHTN", "Holt Area", "Holt Town");
    public static Station Eccles = createStation("9400ZZMAECC", "Eccles Area", "Eccles");
    public static Station Bury = createStation("9400ZZMABUR", "Bury Area", "Bury");
    public static Station EastDidsbury = createStation("9400ZZMAEDY", "Didsbury Area", "East Didsbury");
    public static Station Rochdale = createStation("9400ZZMARIN", "Rochdale", "Rochdale Interchange");
    public static Station Pomona = createStation("9400ZZMAPOM", "Pomona", "Pomona");
    public static Station Deansgate = createStation("9400ZZMAGMX", "Deansgate area", "Deansgate-Castlefield");
    public static Station Broadway = createStation("9400ZZMABWY", "Broadway area", "Broadway");
    public static Station PiccadillyGardens = createStation("9400ZZMAPGD", "Manchester", "Piccadilly Gardens");
    public static Station ExchangeSquare = createStation("9400ZZMAEXS", "Manchester", "Exchange Square");
    public static Station Victoria = createStation("9400ZZMAVIC", "Manchester", "Victoria");
    public static Station NavigationRoad = createStation("9400ZZMANAV", "Altrincham", "Navigation Road");
    public static Station ShawAndCrompton = createStation("9400ZZMASHA", "Shaw and Crompton Area", "Shaw and Crompton");
    public static Station HarbourCity = createStation("9400ZZMAHCY", "Harbour City area", "Harbour City");
    public static Station StPetersSquare = createStation("9400ZZMASTP", "Manchester City Centre", "St Peter's Square");
    public static Station MarketStreet = createStation("9400ZZMAMKT", "Market Street Area", "Market Street");
    public static Station MediaCityUK = createStation("9400ZZMAMCU", "Media City Area", "MediaCityUK");
    public static Station StWerburghsRoad = createStation("9400ZZMASTW", "Chorlton", "St Werburgh's Road");
    public static Station Shudehill = createStation("9400ZZMASHU","Manchester City Centre", "Shudehill");
    public static Station Monsall = createStation("9400ZZMAMON", "Manchester City Centre", "Monsall");
    public static Station RochdaleRail = createStation("9400ZZMARRS", "Rochsdale Town Centr", "Rochsdale Railway Station");

    private static List<Station> EndOfTheLineWest = Arrays.asList(Altrincham,
            ManAirport,
            Eccles,
            EastDidsbury);

    private static List<Station> EndOfTheLineEast = Arrays.asList(Ashton,
            Rochdale,
            Bury,
            ExchangeSquare);

    public static List<Station> EndOfTheLine = ListUtils.union(EndOfTheLineEast, EndOfTheLineWest);

    public static List<Station> Interchanges = Arrays.asList(Cornbrook, StPetersSquare, PiccadillyGardens,
            TraffordBar, StWerburghsRoad, Victoria, Deansgate, Piccadilly, HarbourCity, ShawAndCrompton);

    public static Station createStation(String id, String area, String name) {
        Station station = new Station(id, area, name, position, true);
//        station.addPlatform(new Platform(id+"1", name));
//        station.addPlatform(new Platform(id+"2", name));
        return station;
    }

}

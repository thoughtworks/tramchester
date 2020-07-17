package com.tramchester.testSupport;

import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;

import java.util.Arrays;
import java.util.List;

public class Stations {
    public static int NumberOf = 99;

    // TEST DATA For stations
    private static final LatLong position = new LatLong(1,1);

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
    public static Station ExchangeQuay = createStation("9400ZZMAEXC", "media city area", "Exchange Quay");
    public static Station SalfordQuay = createStation("9400ZZMASQY", "media city area", "Salford Quay");
    public static Station Anchorage = createStation("9400ZZMAANC", "media city area", "Anchorage");
    public static Station HeatonPark = createStation("9400ZZMAHEA", "Heaton", "Heaton Park");
    public static Station BurtonRoad = createStation("9400ZZMABNR", "Heaton", "Burton Road");
    public static Station RochdaleRail = createStation("9400ZZMARRS", "Rochsdale Town Centr", "Rochsdale Railway Station");
    public static Station Intu = createStation("9400ZZMATRC", "The Trafford Centre", "intu Trafford Centre");

    public static List<Station> EndOfTheLine = Arrays.asList(Altrincham,
            ManAirport,
            Eccles,
            EastDidsbury,
            Ashton,
            Rochdale,
            Bury,
            ExchangeSquare,
            Intu);


    public static List<Station> Interchanges = Arrays.asList(Cornbrook, StPetersSquare, PiccadillyGardens,
            TraffordBar, StWerburghsRoad, Victoria, Deansgate, Piccadilly, HarbourCity, ShawAndCrompton);

    public static Station createStation(String id, String area, String name) {
        return Station.forTest(id, area, name, position, TransportMode.Tram);
    }

}

package com.tramchester;

import com.tramchester.domain.Interchanges;
import com.tramchester.domain.Location;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.LatLong;
import org.apache.commons.collections.ListUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Stations {

    public static LatLong position = new LatLong(1,1);

    public static Location Altrincham = createStation("9400ZZMAALT", "Altrincham area", "Altrincham");
    public static Location Ashton = createStation("9400ZZMAAUL", "Ashton area", "Ashton");
    public static Location ManAirport = createStation("9400ZZMAAIR", "Airport", "Manchester Airport");
    public static Location TraffordBar = createStation("9400ZZMATRA", "Trafford", "Trafford Bar");
    public static Location VeloPark = createStation("9400ZZMAVPK", "Velopark area", "Velopark");
    public static Location Cornbrook = createStation("9400ZZMACRN", "Cornbrook area", "Cornbrook");
    public static Location Etihad = createStation("9400ZZMAECS", "Etihad area", "Etihad");
    public static Location Piccadily = createStation("9400ZZMAPIC", "Piccadily area", "Piccadily");
    public static Location HoltTown = createStation("9400ZZMAHTN", "Holt Area", "Hold Town");
    public static Location Eccles = createStation("9400ZZMAECC", "Eccles Area", "Eccles");
    public static Location Bury = createStation("9400ZZMABUR", "Bury Area", "Bury");
    public static Location EastDidsbury = createStation("9400ZZMAEDY", "Didsbury Area", "East Didsbury");
    public static Location Rochdale = createStation("9400ZZMARIN", "Rochdale area", "Rochdale");
    public static Location Pomona = createStation("9400ZZMAPOM", "Pomona", "Pomona");
    public static Location Deansgate = createStation("9400ZZMAGMX", "Deansgate area", "Deansgate-Castlefield");
    public static Location Broadway = createStation("9400ZZMABWY", "Broadway area", "Broadway");
    public static Location PiccadilyGardens = createStation("9400ZZMAPGD", "Manchester", "Piccadily Gardens");
    public static Location ExchangeSquare = createStation("9400ZZMAEXS", "Manchester", "Exchange Square");
    public static Location Victoria = createStation("9400ZZMAVIC", "Manchester", "Victoria");
    public static Location NavigationRoad = createStation("9400ZZMANAV", "Altrincham", "Navigation Road");
    public static Location ShawAndCrompton = createStation("9400ZZMASHA", "Shaw and Crompton Area", "Shaw And Crompton");

    public static String MarketStreet = "9400ZZMAMKT";
    public static String MediaCityUK = "9400ZZMAMCU";
    public static String HarbourCity = "9400ZZMAHCY";
    public static String StPetersSquare = "9400ZZMASTP";


    public static List<Location> EndOfTheLineWest = Arrays.asList(new Location[]{
            Altrincham,
            ManAirport,
            Eccles,
            EastDidsbury});

    public static List<Location> EndOfTheLineEast = Arrays.asList(new Location[]{
            Ashton,
            Rochdale,
            Bury,
            ExchangeSquare});

    public static List<Location> EndOfTheLine = ListUtils.union(EndOfTheLineEast, EndOfTheLineWest);

    public static List<Location> getInterchanges() {
        Set<String> ids = Interchanges.stations();
        return ids.stream().map(id -> createStation(id, "area", "name")).collect(Collectors.toList());
    }

    private static Station createStation(String id, String area, String name) {
        return new Station(id, area, name, position, true);
    }
}

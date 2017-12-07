package com.tramchester.integration;

import com.tramchester.domain.input.Interchanges;
import com.tramchester.domain.Location;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.LatLong;
import org.apache.commons.collections4.ListUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Stations {

    public static LatLong position = new LatLong(1,1);

    public static Location Altrincham = createStation("9400ZZMAALT", "Altrincham area", "Altrincham");
    public static Location Ashton = createStation("9400ZZMAAUL", "Ashton area", "Ashton-Under-Lyne");
    public static Location ManAirport = createStation("9400ZZMAAIR", "Airport", "Manchester Airport");
    public static Location TraffordBar = createStation("9400ZZMATRA", "Trafford", "Trafford Bar");
    public static Location VeloPark = createStation("9400ZZMAVPK", "Velopark area", "Velopark");
    public static Location Cornbrook = createStation("9400ZZMACRN", "Cornbrook area", "Cornbrook");
    public static Location Etihad = createStation("9400ZZMAECS", "Etihad area", "Etihad");
    public static Location Piccadilly = createStation("9400ZZMAPIC", "Piccadilly area", "Piccadilly");
    public static Location HoltTown = createStation("9400ZZMAHTN", "Holt Area", "Hold Town");
    public static Location Eccles = createStation("9400ZZMAECC", "Eccles Area", "Eccles");
    public static Location Bury = createStation("9400ZZMABUR", "Bury Area", "Bury");
    public static Location EastDidsbury = createStation("9400ZZMAEDY", "Didsbury Area", "East Didsbury");
    public static Location Rochdale = createStation("9400ZZMARIN", "Rochdale", "Rochdale Interchange");
    public static Location Pomona = createStation("9400ZZMAPOM", "Pomona", "Pomona");
    public static Location Deansgate = createStation("9400ZZMAGMX", "Deansgate area", "Deansgate-Castlefield");
    public static Location Broadway = createStation("9400ZZMABWY", "Broadway area", "Broadway");
    public static Location PiccadillyGardens = createStation("9400ZZMAPGD", "Manchester", "Piccadilly Gardens");
    public static Location ExchangeSquare = createStation("9400ZZMAEXS", "Manchester", "Exchange Square");
    public static Location Victoria = createStation("9400ZZMAVIC", "Manchester", "Victoria");
    public static Location NavigationRoad = createStation("9400ZZMANAV", "Altrincham", "Navigation Road");
    public static Location ShawAndCrompton = createStation("9400ZZMASHA", "Shaw and Crompton Area", "Shaw And Crompton");
    public static Location HarbourCity = createStation("9400ZZMAHCY", "Harbour City area", "Harbour City");
    public static Location StPetersSquare = createStation("9400ZZMASTP", "Manchester City Centre", "St Peters Square");
    public static Location MarketStreet = createStation("9400ZZMAMKT", "Market Street Area", "Market Street");

    public static Location MediaCityUK = createStation("9400ZZMAMCU", "Media City", "Media City");

    public static List<Location> EndOfTheLineWest = Arrays.asList(new Location[]{
            Altrincham,
            ManAirport,
            Eccles,   // Eccles is closed during summer 2016 refurb
            EastDidsbury});

    public static List<Location> EndOfTheLineEast = Arrays.asList(new Location[]{
            Ashton,
            Rochdale,
            Bury,
            ExchangeSquare});

    public static List<Location> EndOfTheLine = ListUtils.union(EndOfTheLineEast, EndOfTheLineWest);

    public static List<Location> getInterchanges() {
        return toLocationList(Interchanges.stations());
    }

    private static List<Location> toLocationList(Set<String> ids) {
        return ids.stream().map(id -> createStation(id, "area", "name")).collect(Collectors.toList());
    }

    public static Station createStation(String id, String area, String name) {
        return new Station(id, area, name, position, true);
    }

}

package com.tramchester;

import com.tramchester.domain.Location;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.LatLong;
import org.apache.commons.collections.ListUtils;

import java.util.Arrays;
import java.util.List;

public class Stations {

    public static LatLong position = new LatLong(1,1);

    public static Location Altrincham = new Station("9400ZZMAALT", "Altrincham area", "Altrincham", position, true);
    public static Location Ashton = new Station("9400ZZMAAUL", "Ashton area", "Ashton", position, true);
    public static Location ManAirport = new Station("9400ZZMAAIR", "Airport", "Manchester Airport", position, true);
    public static Location TraffordBar = new Station("9400ZZMATRA", "Trafford", "Trafford Bar", position, true);
    public static Location VeloPark = new Station("9400ZZMAVPK", "Velopark area", "Velopark", position, true);
    public static String MediaCityUK = "9400ZZMAMCU";
    public static String HarbourCity = "9400ZZMAHCY";
    public static Location Cornbrook = new Station("9400ZZMACRN", "Cornbrook area", "Cornbrook", position, true);
    public static String Etihad = "9400ZZMAECS";
    public static Location Piccadily = new Station("9400ZZMAPIC", "Piccadily area", "Piccadily", position, true);
    public static String HoltTown = "9400ZZMAHTN";
    public static String Eccles = "9400ZZMAECC";
    public static Location Bury = new Station("9400ZZMABUR", "Bury Area", "Bury", position, true);
    public static String EastDidsbury = "9400ZZMAEDY";
    public static Location Rochdale = new Station("9400ZZMARIN", "Rochdale area", "Rochdale", position, true);
    public static String Pomona = "9400ZZMAPOM";
    public static Location Deansgate = new Station("9400ZZMAGMX", "Deansgate area", "Deansgate-Castlefield", position, true);
    public static String Broadway = "9400ZZMABWY";
    public static Location PiccadilyGardens = new Station("9400ZZMAPGD", "Manchester", "Piccadily Gardens", position, true);
    public static String StPetersSquare = "9400ZZMASTP";
    public static Location ExchangeSquare = new Station("9400ZZMAEXS", "Manchester", "Exchange Square", position, true);
    public static Location Victoria = new Station("9400ZZMAVIC", "Manchester", "Victoria", position, true);
    public static Location NavigationRoad = new Station("9400ZZMANAV", "Altrincham", "Navigation Road", position, true);
    public static String ShawAndCrompton = "9400ZZMASHA";
    public static String MarketStreet = "9400ZZMAMKT";

    public static List<String> EndOfTheLineWest = Arrays.asList(new String[]{
            Altrincham.getId(),
            ManAirport.getId(),
            Eccles,
            EastDidsbury});

    public static List<String> EndOfTheLineEast = Arrays.asList(new String[]{
            Ashton.getId(),
            Rochdale.getId(),
            Bury.getId(),
            ExchangeSquare.getId()});

    public static List<String> EndOfTheLine = ListUtils.union(EndOfTheLineEast, EndOfTheLineWest);
}

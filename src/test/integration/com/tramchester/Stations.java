package com.tramchester;

import com.tramchester.domain.Location;
import com.tramchester.domain.Station;

public class Stations {

    public static Location Altrincham = new Station("9400ZZMAALT", "Altrincham area", "Altrincham", 1,1, true);
    public static Location ManAirport = new Station("9400ZZMAAIR", "Airport", "Manchester Airport", 1,1, true);
    public static String TraffordBar = "9400ZZMATRA";
    public static Location VeloPark = new Station("9400ZZMAVPK", "Velopark area", "Velopark", 1,1, true);
    public static String MediaCityUK = "9400ZZMAMCU";
    public static String HarbourCity = "9400ZZMAHCY";
    public static Location Cornbrook = new Station("9400ZZMACRN", "Cornbrook area", "Cornbrook", 1,1, true);
    public static String Etihad = "9400ZZMAECS";
    public static Location Piccadily = new Station("9400ZZMAPIC", "Piccadily area", "Piccadily", 1,1, true);
    public static String HoltTown = "9400ZZMAHTN";
    public static Location Ashton = new Station("9400ZZMAAUL", "Ashton area", "Ashton", 1,1, true);
    public static String NewIslington = "9400ZZMANIS";
    public static String Eccles = "9400ZZMAECC";
    public static Location Bury = new Station("9400ZZMABUR", "Bury Area", "Bury", 1,1, true);
    public static String EastDidsbury = "9400ZZMAEDY";
    public static Location Rochdale = new Station("9400ZZMARIN", "Rochdale area", "Rochdale", 1,1, true);
    public static String Pomona = "9400ZZMAPOM";
    public static Location Deansgate = new Station("9400ZZMAGMX", "Deansgate area", "Deansgate-Castlefield", 1,1, true);
    public static String Broadway = "9400ZZMABWY";
    public static Location PiccadilyGardens = new Station("9400ZZMAPGD", "Manchester", "Piccadily Gardens", 1,1, true);
    public static String StPetersSquare = "9400ZZMASTP";
    public static Location ExchangeSquare = new Station("9400ZZMAEXS", "Manchester", "Exchange Square", 1, 1, true);
    public static Location Victoria = new Station("9400ZZMAVIC", "Manchester", "Victoria", 1,1, true);
    public static String ShawAndCrompton = "9400ZZMASHA";
    public static String MarketStreet = "9400ZZMAMKT";

    public static String[] EndOfTheLine = new String[]{Altrincham.getId(),
            ManAirport.getId(),
            Eccles,
            EastDidsbury,
            Ashton.getId(),
            Rochdale.getId(),
            Bury.getId(),
            ExchangeSquare.getId()};
}

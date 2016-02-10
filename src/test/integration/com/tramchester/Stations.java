package com.tramchester;

import com.tramchester.domain.Station;

public class Stations {

    public static Station Altrincham = new Station("9400ZZMAALT", "Altrincham area", "Altrincham", 1,1, true);
    public static Station ManAirport = new Station("9400ZZMAAIR", "Airport", "Manchester Airport", 1,1, true);
    public static String TraffordBar = "9400ZZMATRA";
    public static Station VeloPark = new Station("9400ZZMAVPK", "Velopark area", "Velopark", 1,1, true);
    public static String MediaCityUK = "9400ZZMAMCU";
    public static String HarbourCity = "9400ZZMAHCY";
    public static Station Cornbrook = new Station("9400ZZMACRN", "Cornbrook area", "Cornbrook", 1,1, true);
    public static String Etihad = "9400ZZMAECS";
    public static Station Piccadily = new Station("9400ZZMAPIC", "Piccadily area", "Piccadily", 1,1, true);
    public static String HoltTown = "9400ZZMAHTN";
    public static Station Ashton = new Station("9400ZZMAAUL", "Ashton area", "Ashton", 1,1, true);
    public static String NewIslington = "9400ZZMANIS";
    public static String Eccles = "9400ZZMAECC";
    public static String Bury = "9400ZZMABUR";
    public static String EastDidsbury = "9400ZZMAEDY";
    public static Station Rochdale = new Station("9400ZZMARIN", "Rochdale area", "Rochdale", 1,1, true);
    public static String Pomona = "9400ZZMAPOM";
    public static Station Deansgate = new Station("9400ZZMAGMX", "Deansgate area", "Deansgate", 1,1, true);
    public static String Broadway = "9400ZZMABWY";
    public static Station PiccadilyGardens = new Station("9400ZZMAPGD", "Manchester", "Piccadily Gardens", 1,1, true);
    public static String StPetersSquare = "9400ZZMASTP";
    public static Station ExchangeSquare = new Station("9400ZZMAEXS", "Manchester", "Exchange Square", 1, 1, true);
    public static Station Victoria = new Station("9400ZZMAVIC", "Manchester", "Victoria", 1,1, true);
    public static String ShawAndCrompton = "9400ZZMASHA";
    public static String MarketStreet = "9400ZZMAMKT";

    public static String[] EndOfTheLine = new String[]{Altrincham.getId(),
            ManAirport.getId(),
            Eccles,
            EastDidsbury,
            Ashton.getId(),
            Rochdale.getId(),
            Bury,
            ExchangeSquare.getId()};
}

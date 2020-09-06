package com.tramchester.testSupport;

import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import org.opengis.referencing.operation.TransformException;

import java.util.Arrays;
import java.util.List;

@Deprecated
public class Stations {
    public static int NumberOf = 99;

    // TEST DATA For stations
    private static final LatLong position = new LatLong(1,1);

    public static Station Altrincham = TramStations.of(TramStations.Altrincham);
    public static Station Ashton = TramStations.of(TramStations.Ashton);
    public static Station ManAirport = TramStations.of(TramStations.ManAirport);
    public static Station TraffordBar = TramStations.of(TramStations.TraffordBar);
    public static Station VeloPark = TramStations.of(TramStations.VeloPark);
    public static Station Cornbrook = TramStations.of(TramStations.Cornbrook);
    public static Station Piccadilly = TramStations.of(TramStations.Piccadilly);
    public static Station Eccles = TramStations.of(TramStations.Eccles);
    public static Station Bury = TramStations.of(TramStations.Bury);
    private static Station EastDidsbury = TramStations.of(TramStations.EastDidsbury);
    public static Station Rochdale = TramStations.of(TramStations.Rochdale);
    public static Station Pomona = TramStations.of(TramStations.Pomona);
    public static Station Deansgate = TramStations.of(TramStations.Deansgate);
    public static Station Broadway = TramStations.of(TramStations.Broadway);
    public static Station PiccadillyGardens = TramStations.of(TramStations.PiccadillyGardens);
    public static Station ExchangeSquare = TramStations.of(TramStations.ExchangeSquare);
    public static Station Victoria = TramStations.of(TramStations.Victoria);
    public static Station NavigationRoad = TramStations.of(TramStations.NavigationRoad);
    public static Station ShawAndCrompton = TramStations.of(TramStations.ShawAndCrompton);
    public static Station HarbourCity = TramStations.of(TramStations.HarbourCity);
    public static Station StPetersSquare = TramStations.of(TramStations.StPetersSquare);
    public static Station MarketStreet = TramStations.of(TramStations.MarketStreet);
    public static Station MediaCityUK = TramStations.of(TramStations.MediaCityUK);
    public static Station StWerburghsRoad = TramStations.of(TramStations.StWerburghsRoad);
    public static Station Shudehill = TramStations.of(TramStations.Shudehill);
    public static Station Monsall = TramStations.of(TramStations.Monsall);
    public static Station RochdaleRail = TramStations.of(TramStations.RochdaleRail);
    public static Station Intu = TramStations.of(TramStations.Intu);

    @Deprecated
    public static List<Station> EndOfTheLine = Arrays.asList(Altrincham,
            ManAirport,
            Eccles,
            EastDidsbury,
            Ashton,
            Rochdale,
            Bury,
            ExchangeSquare,
            Intu);

    @Deprecated
    public static List<Station> Interchanges = Arrays.asList(Cornbrook, StPetersSquare, PiccadillyGardens,
            TraffordBar, StWerburghsRoad, Victoria, Deansgate, Piccadilly, HarbourCity, ShawAndCrompton);

    public static Station createStation(String id, String area, String name) {
        try {
            return Station.forTest(id, area, name, position, TransportMode.Tram);
        } catch (TransformException exception) {
            throw new RuntimeException(exception);
        }
    }

}

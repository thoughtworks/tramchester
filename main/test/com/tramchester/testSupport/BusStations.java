package com.tramchester.testSupport;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;

public class BusStations {
    private static LatLong position = new LatLong(1,1);

//    public static final String ALTRINCHAM_INTERCHANGE = "1800AMIC001";
//    public static final String STOCKPORT_BUSSTATION = "1800STBS001";
//    public static final String SHUDEHILL_INTERCHANGE = "1800SHIC001";

    public static Station AltrinchamInterchange = createStation("1800AMIC001", "Altrincham", "Altrincham Interchange");
    public static Station StockportBusStation = createStation("1800STBS001", "Stockport", "Stockport Bus Station");
    public static Station ShudehillInterchange = createStation("1800SHIC001", "Shudehill", "Shudehill Interchange");
    public static Station ManchesterAirportStation = createStation("1800MABS001", "Manchester Airport", "Manchester Airport The Station");

    private static Station createStation(String id, String area, String name) {
        return new Station(id, area, name, position, false);
    }
}

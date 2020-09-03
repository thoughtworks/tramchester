package com.tramchester.testSupport;

import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import org.opengis.referencing.operation.TransformException;

public class BusStations {
    private static final LatLong position = new LatLong(1,1);

    public static Station AltrinchamInterchange = createStation("1800AMIC001", "Altrincham", "Altrincham Interchange");
    public static Station StockportBusStation = createStation("1800STBS001", "Stockport", "Stockport Bus Station");
    public static Station ShudehillInterchange = createStation("1800SHIC001", "Shudehill", "Shudehill Interchange");
    public static Station ManchesterAirportStation = createStation("1800MABS001", "Manchester Airport", "Manchester Airport The Station");
    public static Station KnutsfordStationStand3 = createStation("0600MA6022", "Knutsford", "Knutsford,Bus Station (Stand 3)");
    public static Station BuryInterchange = createStation("1800BYIC001", "Bury", "Bury Interchange");
    public static Station PiccadilyStationStopA = createStation("1800EB01201", "Manchester City Centre", "Piccadilly Station (Stop A)");

    private static Station createStation(String id, String area, String name) {
        try {
            return Station.forTest(id, area, name, position, TransportMode.Bus);
        } catch (TransformException exception) {
            throw new RuntimeException(exception);
        }
    }
}

package com.tramchester.domain.reference;

import com.tramchester.domain.places.Station;

public enum MultilineStations {
    Cornbrook("9400ZZMACRN"),
    StPetersSquare("9400ZZMASTP"),
    PiccadillyGardens("9400ZZMAPGD"),
    TraffordBar("9400ZZMATRA"),
    StWerbergsRoad("9400ZZMASTW"),
    Victoria("9400ZZMAVIC"),
    Deansgate("9400ZZMAGMX"),
    Piccadilly("9400ZZMAPIC"),
    MarketStreet("9400ZZMAMKT"),
    Firswood("9400ZZMAFIR"),
    Shudehill("9400ZZMASHU"),
    Pomona("9400ZZMAPOM"),
    Chorlton("9400ZZMACHO");

    private final String stationId;

    MultilineStations(String stationId) {
        this.stationId = stationId;
    }

    public boolean matches(Station station) {
        return stationId.equals(station.getId().forDTO());
    }

    public String getId() {
        return stationId;
    }
}

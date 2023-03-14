package com.tramchester.domain.reference;

import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.places.Station;

import java.util.Arrays;

public enum CentralZoneStation {
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
    Chorlton("9400ZZMACHO"),
    ExchangeSquare("9400ZZMAEXS");

    //private final IdFor<Station> stationId;

    private final String stationId;

    CentralZoneStation(String stationId) {
        this.stationId = stationId;
    }

    public IdFor<Station> getId() {
        return Station.createId(stationId);
    }

    public static final IdSet<Station> ids;

    static {
        ids = Arrays.stream(CentralZoneStation.values()).map(CentralZoneStation::getId).collect(IdSet.idCollector());
    }

    public IdFor<Platform> getPlatformId(String platform) {
        return PlatformId.createId(getId(), platform);
    }
}

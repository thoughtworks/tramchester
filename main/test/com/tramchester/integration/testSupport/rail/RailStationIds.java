package com.tramchester.integration.testSupport.rail;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;

import static com.tramchester.domain.id.StringIdFor.createId;

public enum RailStationIds {
    Stockport (createId("STKP")),
    ManchesterPiccadilly(createId("MNCRPIC")),
    Altrincham(createId("ALTRNHM")),
    LondonEuston(createId("EUSTON")),
    Derby(createId("DRBY")),
    Wimbledon(createId("WDON")),
    LondonWaterloo(createId("WATRLMN")),
    LondonStPancras(createId("STPX")),
    Macclesfield(createId("MACLSFD")),
    MiltonKeynesCentral(createId("MKNSCEN")),
    StokeOnTrent(createId("STOKEOT"));

    private final IdFor<Station> id;

    RailStationIds(IdFor<Station> id) {
        this.id = id;
    }

    public IdFor<Station> getId() {
        return id;
    }
}

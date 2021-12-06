package com.tramchester.integration.testSupport.rail;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.StationRepository;

import static com.tramchester.domain.id.StringIdFor.createId;

public enum RailStationIds {
    Stockport (createId("STKP")),
    ManchesterPiccadilly(createId("MNCRPIC")),
    Altrincham(createId("ALTRNHM")),
    Crewe(createId("CREWE")),
    LondonEuston(createId("EUSTON")),
    Derby(createId("DRBY")),
    Wimbledon(createId("WDON")),
    LondonWaterloo(createId("WATRLMN")),
    LondonStPancras(createId("STPX")),
    Macclesfield(createId("MACLSFD")),
    MiltonKeynesCentral(createId("MKNSCEN")),
    Hale(createId("HALE")),
    Knutsford(createId("KNUTSFD")),
    Ashley(createId("ASHLEY")),
    Mobberley(createId("MOBERLY")),
    StokeOnTrent(createId("STOKEOT")),
    Wilmslow(createId(("WLMSL")));

    private final IdFor<Station> id;

    RailStationIds(IdFor<Station> id) {
        this.id = id;
    }

    public IdFor<Station> getId() {
        return id;
    }

    public Station getFrom(StationRepository repository) {
        return repository.getStationById(getId());
    }
}

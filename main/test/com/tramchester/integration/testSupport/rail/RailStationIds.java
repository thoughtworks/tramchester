package com.tramchester.integration.testSupport.rail;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.Station;
import com.tramchester.repository.StationRepository;


public enum RailStationIds implements HasId<Station> {
    Stockport(createId("STKP"), true),
    ManchesterPiccadilly(createId("MNCRPIC"), true),
    ManchesterVictoria(createId("MNCRVIC"), true),
    ManchesterDeansgate(createId("MNCRDGT"), true),
    ManchesterOxfordRoad(createId("MNCROXR"), true),
    ManchesterAirport(createId("MNCRIAP"), true),
    SalfordCentral(createId("SLFDORD"), true),
    Altrincham(createId("ALTRNHM"), true),
    NavigationRaod(createId("NAVGTNR"), true),
    Crewe(createId("CREWE"), false),
    LondonEuston(createId("EUSTON"), false),
    Derby(createId("DRBY"), false),
    Belper(createId("BELPER"), false),
    Duffield(createId("DUFIELD"), false),
    Dover(createId("DOVERP"), false),
    Wimbledon(createId("WDON"), false),
    LondonWaterloo(createId("WATRLMN"), false),
    LondonStPancras(createId("STPX"), false),
    Macclesfield(createId("MACLSFD"), false),
    MiltonKeynesCentral(createId("MKNSCEN"), false),
    Hale(createId("HALE"), true),
    Knutsford(createId("KNUTSFD"), false),
    Ashley(createId("ASHLEY"), true),
    Mobberley(createId("MOBERLY"), true),
    StokeOnTrent(createId("STOKEOT"), false),
    Delamere(createId("DELAMER"), false),
    Wilmslow(createId("WLMSL"), true),
    Chester(createId("CHST"), false),
    EastDidsbury(createId("EDIDBRY"), true),
    Eccles(createId("ECCLES"), true),
    Inverness(createId("IVRNESS"), false),
    LiverpoolLimeStreet(createId("LVRPLSH"), false),
    Huddersfield(createId("HDRSFLD"), false),
    Ashton(createId(("ASHONUL")), true);

    private static IdFor<Station> createId(String text) {
        return Station.createId(text);
    }

    private final IdFor<Station> id;
    private final boolean isGreaterManchester;

    RailStationIds(IdFor<Station> id, boolean isGreaterManchester) {
        this.id = id;
        this.isGreaterManchester = isGreaterManchester;
    }

    public IdFor<Station> getId() {
        return id;
    }

    public Station from(StationRepository repository) {
        return repository.getStationById(getId());
    }

    public IdForDTO getIdDTO() {
        return new IdForDTO(id);
    }

    public boolean isGreaterManchester() {
        return isGreaterManchester;
    }
}

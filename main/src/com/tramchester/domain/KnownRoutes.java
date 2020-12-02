package com.tramchester.domain;

public enum KnownRoutes {
    AltrinchamPiccadilly("MET:2:I:"),
    PiccadillyAltrincham("MET:2:O:"),
    intuTraffordCentreCornbrook("MET:7:I:"),
    CornbrookintuTraffordCentre("MET:7:O:"),
    AshtonunderLyneManchesterEccles("MET:3:I:"),
    EcclesManchesterAshtonunderLyne("MET:3:O:"),
    BuryPiccadilly("MET:4:I:"),
    PiccadillyBury("MET:4:O:"),
    EDidsburyManchesterRochdale("MET:5:I:"),
    RochdaleManchesterEDidsbury("MET:5:O:"),
    VictoriaManchesterAirport("MET:6:I:"),
    ManchesterAirportVictoria("MET:6:O:");

    private final String id;

    KnownRoutes(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}

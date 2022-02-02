package com.tramchester.repository.naptan;

import java.util.HashMap;

public enum NaptanStopAreaType {
    AirportBuilding("GAIR"),
    FerryTerminalOrDockBuilding("GFTD"),
    RailStation("GRLS"),
    TramMetroUndergroundStation("GTMU"),
    BusCoachStation("GBCS"),
    CoachServiceCoverage("GCCH"),
    OnStreetBusCoachTrolleyStopsCluster("GCLS"),
    LiftOrCablecarStation("GLCB"),
    OnStreetBusCoachTrolleyStopPair("GPBS"),
    Unknown("Unknown");

    // These two marked DEPRECATED in naptan docs
    //(GMLT) Multimode Interchange
    //(GOTH) Other Interchange.

    private static final HashMap<String, NaptanStopAreaType> map;

    static {
        map = new HashMap<>();
        for(NaptanStopAreaType type : NaptanStopAreaType.values()) {
            map.put(type.code, type);
        }
    }

    private final String code;

    NaptanStopAreaType(String code) {
        this.code = code;
    }

    public static NaptanStopAreaType parse(String text) {
        if (map.containsKey(text)) {
            return map.get(text);
        }
        return Unknown;
    }
}

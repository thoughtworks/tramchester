package com.tramchester.repository.naptan;

import java.util.HashMap;
import java.util.Map;

public enum NaptanStopType {

     	busCoachTrolleyStopOnStreet("BCT"),
     	taxiRank("TXR"),
     	sharedTaxiRank("STR"),
     	carSetDownPickUpArea("SDA"),
     	ferryOrPortAccess("FER"),
     	ferryOrPortBerth("FBT"),
     	railStationEntrance("RSE"),
     	railAccess("RLY"),
     	railPlatform("RPL"),
     	tramMetroUndergroundEntrance("TMU"),
     	tramMetroUndergroundAccess("MET"),
     	tramMetroUndergroundPlatform("PLT"),
     	liftOrCableCarStationEntrance("LCE"),
     	liftOrCableCarSetDownPickUpArea("LPL"),
     	busCoachStationEntrance("BCE"),
     	busCoachStationAccess("BST"),
     	busCoachTrolleyStationBay("BCS"),
     	busCoachTrolleyStationVariableBay("BCQ"),
        unknown("UNKNOWN");

    private final String code;

    private static final Map<String, NaptanStopType> map;

    static {
        map = new HashMap<>();
        for(NaptanStopType type :NaptanStopType.values()) {
            map.put(type.code, type);
        }
    }

    NaptanStopType(String code) {
        this.code = code;
    }

    public static NaptanStopType parse(String text) {
        if (map.containsKey(text)) {
            return map.get(text);
        }
        return NaptanStopType.unknown;
    }

    public static boolean isInterchance(NaptanStopType stopType) {
        return switch (stopType) {
            case busCoachTrolleyStationBay, busCoachTrolleyStationVariableBay, busCoachStationEntrance,
                    busCoachStationAccess-> true;
            default -> false;
        };
    }
}

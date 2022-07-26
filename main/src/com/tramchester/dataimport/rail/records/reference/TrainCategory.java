package com.tramchester.dataimport.rail.records.reference;


public enum TrainCategory implements EnumMap.HasCodes {
    LondonUndergroundOrMetroService("OL"),
    UnadvertisedOrdinaryPassenger("OU"),
    OrdinaryPassenger("OO"),
    StaffTrain("OS"),
    Mixed("OW"),
    ChannelTunnel("XC"),
    SleeperEuropeNightServices("XD"),
    International("XI"),
    Motorail("XR"),
    UnadvertisedExpress("XU"),
    ExpressPassenger("XX"),
    SleeperDomestic("XZ"),
    BusReplacement("BR"),
    BusService("BS"),
    Ship("SS"),
    Unknown("unknown");

    private static final EnumMap<TrainCategory> codes = new EnumMap<>(TrainCategory.values());

    private final String code;

    TrainCategory(String code) {
        this.code = code;
    }

    public static TrainCategory getFor(String code) {
        TrainCategory result = codes.get(code);
        if (result==null) {
            return Unknown;
        }
        return result;
    }

    @Override
    public String getCode() {
        return code;
    }
}

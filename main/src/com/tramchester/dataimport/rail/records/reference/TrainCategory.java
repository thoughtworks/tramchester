package com.tramchester.dataimport.rail.records.reference;


public enum TrainCategory implements EnumMap.HasCodes<TrainCategory> {
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
    Ship("SS");

    private static final EnumMap<TrainCategory> codes = new EnumMap<>(TrainCategory.values());

    private final String code;

    TrainCategory(String code) {
        this.code = code;
    }

    public static TrainCategory getFor(String code) {
        return codes.get(code);
    }

    @Override
    public String getCode() {
        return code;
    }
}
